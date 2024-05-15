package com.kotlinorm.orm.select

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.dsl.KTableConditional.Companion.conditionalRun
import com.kotlinorm.beans.dsl.KTableSortable.Companion.sortableRun
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.pagination.PagedClause
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.types.KTableSortableField
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.tableCache.TableCache.getTable
import com.kotlinorm.utils.toLinkedSet

class SelectClause<T : KPojo>(
    private val pojo: T, setSelectFields: KTableField<T, Any?> = null
) {

    private var tableName = pojo.kronosTableName()
    private var paramMap = pojo.transformToMap()
    private var logicDeleteStrategy = pojo.kronosLogicDelete()
    private var allFields: LinkedHashSet<Field> = pojo.kronosColumns().toLinkedSet()
    private var condition: Criteria? = null
    private var lastCondition: Criteria? = null

    init {
        if (setSelectFields != null) {
            pojo.tableRun {
                setSelectFields()
            }
        }
    }

    fun orderBy(someFields: KTableSortableField<T, Unit>): SelectClause<T> {
        pojo.sortableRun {
            this
        }
        return this
    }

    fun groupBy(lambda: KTableField<T, Unit>): SelectClause<T> {
        TODO()
    }

    fun distinct(): SelectClause<T> {
        TODO()
    }

    fun limit(num: Int): SelectClause<T> {
        TODO()
    }

    fun offset(num: Int): SelectClause<T> {
        TODO()
    }

    fun page(pi: Int, ps: Int): SelectClause<T> {
        // page(1, 10) limit 10 offset 0
        val offset = (pi - 1) * ps
        return this.apply {
            lastCondition = listOfNotNull(
                lastCondition, "limit ${ps} offset ${offset}".asSql()
            ).toCriteria()
        }
    }

    fun by(someFields: KTableField<T, Any?>): SelectClause<T> {
        if (someFields == null) throw NeedFieldsException()
        pojo.tableRun {
            someFields()
            condition = fields.map { it.eq(paramMap[it.name]) }.toCriteria()
        }
        return this
    }

    fun where(selectCondition: KTableConditionalField<T, Boolean?> = null): SelectClause<T> {
        if (selectCondition == null) return this.apply {
            // 获取所有字段
            condition = paramMap.keys.map { propName ->
                allFields.first { it.name == propName }.eq(paramMap[propName])
            }.toCriteria()
        }
        pojo.conditionalRun {
            propParamMap = paramMap
            selectCondition()
            condition = criteria
        }
        return this
    }

    fun having(lambda: KTableConditionalField<T, Boolean?> = null): SelectClause<T> {
        TODO()
    }

    fun withTotal(): PagedClause<SelectClause<T>> {
        TODO()
    }

    fun query(): List<Map<String, Any>> {
        TODO()
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): KronosAtomicTask {

        allFields = getTable(wrapper.orDefault(), tableName).columns.toLinkedSet()

        if (condition == null) {
            condition = paramMap.keys.filter {
                paramMap[it] != null
            }.map { propName ->
                allFields.first { it.name == propName }.eq(paramMap[propName])
            }.toCriteria()
        }

        logicDeleteStrategy.enabled = true

        // 设置逻辑删除
        setCommonStrategy(logicDeleteStrategy) { field, value ->
            condition = listOfNotNull(
                condition, "${logicDeleteStrategy.field.quoted()} = $value".asSql()
            ).toCriteria()
        }

        // 是否分页
        if (lastCondition != null) {
            condition = listOfNotNull(
                condition, lastCondition
            ).toCriteria()
        }

        val (conditionSql, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())

        val sql = listOfNotNull(
            "SELECT",
            if (allFields.isEmpty()) "*" else allFields.joinToString(",") {
                it.let {
                    // 加别名
                    if (it.name != it.columnName) {
                        "${it.quoted()} AS `$it`"
                    } else {
                        it.quoted()
                    }
                }
            },
            "FROM `$tableName`",
            "WHERE".takeIf { !conditionSql.isNullOrEmpty() },
            conditionSql?.ifEmpty { null }
        ).joinToString(" ")

        return KronosAtomicTask(
            sql,
            paramMap,
            operationType = KOperationType.UPDATE
        )
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build().execute(wrapper)
    }

    companion object {

        fun <T : KPojo> List<SelectClause<T>>.by(someFields: KTableField<T, Any?>): List<SelectClause<T>> {
            return map { it.by(someFields) }
        }

        /**
         * Applies the `where` operation to each update clause in the list based on the provided update condition.
         *
         * @param updateCondition the condition for the update clause. Defaults to null.
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> List<SelectClause<T>>.where(selectCondition: KTableConditionalField<T, Boolean?> = null): List<SelectClause<T>> {
            return map { it.where(selectCondition) }
        }

        /**
         * Builds a KronosAtomicBatchTask from a list of UpdateClause objects.
         *
         * @param T The type of KPojo objects in the list.
         * @return A KronosAtomicBatchTask object with the SQL and parameter map array from the UpdateClause objects.
         */
        fun <T : KPojo> List<SelectClause<T>>.build(): KronosAtomicBatchTask {
            val tasks = this.map { it.build() }
            return KronosAtomicBatchTask(
                sql = tasks.first().sql,
                paramMapArr = tasks.map { it.paramMap }.toTypedArray(),
                operationType = KOperationType.UPDATE
            )
        }

        /**
         * Executes a list of UpdateClause objects and returns the result of the execution.
         *
         * @param wrapper The KronosDataSourceWrapper to use for the execution. Defaults to null.
         * @return The KronosOperationResult of the execution.
         */
        fun <T : KPojo> List<SelectClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }
    }
}