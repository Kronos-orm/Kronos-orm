package com.kotlinorm.orm.delete

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTable.Companion.table
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.dsl.KTableConditional.Companion.conditionalRun
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.Extensions.toMap
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.setCommonStrategy

class DeleteClause<T : KPojo>(
    private val pojo: T, setDeleteFields: KTableField<T, Any?> = null
) {
    internal lateinit var tableName: String
    internal lateinit var updateTimeStrategy: KronosCommonStrategy
    internal lateinit var logicDeleteStrategy: KronosCommonStrategy
    private var logic: Boolean = false
    private var condition: Criteria? = null
    internal var allFields: LinkedHashSet<Field> = linkedSetOf()
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private var paramMapNew: MutableMap<String, Any?> = mutableMapOf()


    init {
        paramMap.putAll(pojo.toMap().filter { it.value != null })
        if (setDeleteFields != null) {
            pojo.table().setDeleteFields()
        }
    }

    fun logic(): DeleteClause<T> {
        this.logic = true
        this.logicDeleteStrategy.enabled = true
        this.updateTimeStrategy.enabled = true
        return this
    }

    fun by(someFields: KTableField<T, Any?>): DeleteClause<T> {
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
        if (deleteCondition == null) return this.apply {
            // 获取所有字段 且去除null
            condition = paramMap.keys.mapNotNull { propName ->
                allFields.first { it.name == propName }.eq(paramMap[propName]).takeIf { it.value != null }
            }.toCriteria()
        }
        pojo.conditionalRun {
            propParamMap = paramMap
            deleteCondition()
            condition = criteria
        }
        return this
    }

    fun build(): KronosAtomicTask {

        // 设置Where内的逻辑删除
        setCommonStrategy(logicDeleteStrategy) { field, value ->
            condition = listOfNotNull(
                condition, "${field.quotedColumnName()} = $value".asSql()
            ).toCriteria()
        }

        val (conditionSql, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())

        if (logic) {
            val toUpdateFields = mutableListOf<Field>()
            val updateInsertFields = { field: Field, value: Any? ->
                toUpdateFields += field
                paramMap[field.name.let {
                    if (toUpdateFields.map { it.name }.contains(it)) {
                        return@let it + "New"
                    }else it
                }] = value
            }
            setCommonStrategy(updateTimeStrategy, true, callBack = updateInsertFields)
            setCommonStrategy(logicDeleteStrategy, deleted = true, callBack = updateInsertFields)

            val updateFields = toUpdateFields.joinToString(", ") { "${it.quotedColumnName()} = :${it.name + "New"}" }

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
}