package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.dsl.KTableConditional.Companion.conditionalRun
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.toLinkedSet

class DeleteClause<T : KPojo>(private val pojo: T) {
    private var paramMap = pojo.toDataMap()
    private var tableName = pojo.kronosTableName()
    private var updateTimeStrategy = pojo.kronosUpdateTime()
    private var logicDeleteStrategy = pojo.kronosLogicDelete()
    private var logic = false
    private var condition: Criteria? = null
    private var allFields = pojo.kronosColumns().toLinkedSet()

    fun logic(): DeleteClause<T> {
        this.logic = true
        // TODO：这里有问题
        // 这里逻辑是错的，若logicDeleteStrategy.enabled为false则抛出异常
        // 若updateTimeStrategy.enabled为false则不更新updateTime，而不是强制更新
        this.logicDeleteStrategy.enabled = true
        this.updateTimeStrategy.enabled = true
        return this
    }

    fun by(someFields: KTableField<T, Any?>): DeleteClause<T> {
        // TODO:someFields为空时抛出异常
        if (someFields == null) return this
        pojo.tableRun {
            someFields()
            if (fields.isEmpty()) {
                throw NeedFieldsException()
            }

            condition = fields.map { it.eq(paramMap[it.name]) }.toCriteria()
        }
        return this
    }

    fun where(deleteCondition: KTableConditionalField<T, Boolean?> = null): DeleteClause<T> {
        if (deleteCondition == null) {
            // 获取所有字段 且去除null
            condition = paramMap.keys.mapNotNull { propName ->
                allFields.first { it.name == propName }.eq(paramMap[propName]).takeIf { it.value != null }
            }.toCriteria()
            return this
        }
        pojo.conditionalRun {
            propParamMap = paramMap
            deleteCondition()
            condition = criteria
        }
        return this
    }

    fun build(): KronosAtomicTask {
        if (logic) {// 设置Where内的逻辑删除
            setCommonStrategy(logicDeleteStrategy) { field, value ->
                condition = listOfNotNull(
                    condition, "${field.quoted()} = $value".asSql()
                ).toCriteria()
            }
        }

        val (conditionSql, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())

        if (logic) {
            val toUpdateFields = mutableListOf<Field>()
            val updateInsertFields = { field: Field, value: Any? ->
                toUpdateFields += (field + "New")
                paramMap[field.name + "New"] = value
            }
            setCommonStrategy(updateTimeStrategy, true, callBack = updateInsertFields)
            setCommonStrategy(logicDeleteStrategy, deleted = true, callBack = updateInsertFields)

            val updateFields = toUpdateFields.joinToString(", ") { it.equation() }

            val sql = listOfNotNull("UPDATE",
                "`$tableName`",
                "SET",
                updateFields,
                "WHERE".takeIf { !conditionSql.isNullOrEmpty() },
                conditionSql?.ifEmpty { null }).joinToString(" ")
            return KronosAtomicTask(sql, paramMap, operationType = KOperationType.DELETE)
        } else {
            val sql = "DELETE FROM `$tableName` WHERE $conditionSql"
            return KronosAtomicTask(sql, paramMap, operationType = KOperationType.DELETE)
        }
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build().execute(wrapper)
    }

    companion object {

        /**
         * Applies the `by` operation to each update clause in the list based on the provided fields.
         *
         * @param someFields the fields to set the condition for
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> List<DeleteClause<T>>.by(someFields: KTableField<T, Any?>): List<DeleteClause<T>> {
            return map { it.by(someFields) }
        }

        /**
         * Applies the `where` operation to each update clause in the list based on the provided update condition.
         *
         * @param updateCondition the condition for the update clause. Defaults to null.
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> List<DeleteClause<T>>.where(updateCondition: KTableConditionalField<T, Boolean?> = null): List<DeleteClause<T>> {
            return map { it.where(updateCondition) }
        }

        /**
         * Builds a KronosAtomicBatchTask from a list of UpdateClause objects.
         *
         * @param T The type of KPojo objects in the list.
         * @return A KronosAtomicBatchTask object with the SQL and parameter map array from the UpdateClause objects.
         */
        fun <T : KPojo> List<DeleteClause<T>>.build(): KronosAtomicBatchTask {
            val tasks = this.map { it.build() }
            return KronosAtomicBatchTask(
                sql = tasks.first().sql,
                paramMapArr = tasks.map { it.paramMap }.toTypedArray(),
                operationType = KOperationType.DELETE
            )
        }

        /**
         * Executes a list of UpdateClause objects and returns the result of the execution.
         *
         * @param wrapper The KronosDataSourceWrapper to use for the execution. Defaults to null.
         * @return The KronosOperationResult of the execution.
         */
        fun <T : KPojo> List<DeleteClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }
    }
}