package com.kotlinorm.orm.update

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTable
import com.kotlinorm.beans.dsl.KTableConditional
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
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
import kotlin.reflect.full.createInstance

class UpdateClause<T : KPojo>(
    private val pojo: T, private var isExcept: Boolean = false, setUpdateFields: KTableField<T, Any?> = null
) {
    internal lateinit var tableName: String
    internal lateinit var updateTimeStrategy: KronosCommonStrategy
    internal lateinit var logicDeleteStrategy: KronosCommonStrategy
    internal var allFields: MutableList<Field> = mutableListOf()
    private var toUpdateFields: MutableList<Field> = mutableListOf()
    private var condition: Criteria? = null
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private var paramMapNew: MutableMap<Field, Any?> = mutableMapOf()

    init {
        paramMap.putAll(pojo.toMap().filter { it.value != null })
        if (setUpdateFields != null) {
            with(KTable(pojo::class.createInstance())) {
                setUpdateFields()
                toUpdateFields.addAll(fields)
            }
            toUpdateFields.distinct().forEach {
                paramMapNew[it + "New"] = paramMap[it.name]
            }
        }
    }

    fun set(newValue: KTableField<T, Unit>): UpdateClause<T> {
        if (newValue == null) throw NeedFieldsException()
        with(KTable(pojo::class.createInstance())) {
            newValue()
            if (isExcept) {
                toUpdateFields.removeAll(fields)
            } else {
                toUpdateFields.addAll(fields)
            }
            paramMapNew.putAll(fieldParamMap.map { it.key + "New" to it.value })
        }
        return this
    }

    fun by(someFields: KTableField<T, Any?>): UpdateClause<T> {
        if (someFields == null) throw NeedFieldsException()
        with(KTable(pojo::class.createInstance())) {
            someFields()
            condition = fields.map { it.eq(paramMap[it.name]) }.toCriteria()
        }
        return this
    }

    fun where(updateCondition: KTableConditionalField<T, Boolean?> = null): UpdateClause<T> {
        if (updateCondition == null) return this
            .apply {
                // 获取所有字段 且去除null
                condition = paramMap.keys.mapNotNull { propName ->
                    allFields.first { it.name == propName }.eq(paramMap[propName]).takeIf { it.value != null }
                }.toCriteria()
            }
        with(KTableConditional(pojo::class.createInstance())) {
            propParamMap = paramMap
            updateCondition()
            condition = criteria
        }
        return this
    }

    fun build(): KronosAtomicTask {
        // 如果 isExcept 为 true，则将 toUpdateFields 中的字段从 allFields 中移除
        if (isExcept) {
            toUpdateFields = (allFields - toUpdateFields.toSet()).toMutableList()
            toUpdateFields.forEach {
                paramMapNew[it + "new"] = paramMap[it.name]
            }
        }

        if (toUpdateFields.isEmpty()) {
            // 全都更新
            toUpdateFields = allFields.toMutableList()
            toUpdateFields.forEach {
                paramMapNew[it + "new"] = paramMap[it.name]
            }
        }

        // 设置逻辑删除
        setCommonStrategy(logicDeleteStrategy) { field, value ->
            toUpdateFields.remove(field)
            paramMapNew.remove(field + "new")
            condition = listOfNotNull(
                condition, "${logicDeleteStrategy.field.quotedColumnName()} = $value".asSql()
            ).toCriteria()
        }

        // 设置更新时间
        setCommonStrategy(updateTimeStrategy, true) { field, value ->
            paramMapNew[field + "new"] = value
        }

        val updateFields = toUpdateFields.joinToString(", ") { "$it = :${it.name + "New"}" }

        val (conditionSql, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())

        val sql = listOfNotNull(
            "UPDATE",
            tableName,
            "SET",
            updateFields,
            "WHERE".takeIf { !conditionSql.isNullOrEmpty() },
            conditionSql?.ifEmpty { null }
        ).joinToString(" ")

        // 合并
        paramMap.putAll(paramMapNew.map { it.key.name to it.value }.toMap())
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
        fun <T : KPojo> List<UpdateClause<T>>.set(rowData: KTableField<T, Unit>): List<UpdateClause<T>> {
            return map { it.set(rowData) }
        }

        fun <T : KPojo> List<UpdateClause<T>>.by(someFields: KTableField<T, Any?>): List<UpdateClause<T>> {
            return map { it.by(someFields) }
        }

        fun <T : KPojo> List<UpdateClause<T>>.where(updateCondition: KTableConditionalField<T, Boolean?> = null): List<UpdateClause<T>> {
            return map { it.where(updateCondition) }
        }

        fun <T : KPojo> List<UpdateClause<T>>.build(): KronosAtomicBatchTask {
            val tasks = this.map { it.build() }
            return KronosAtomicBatchTask(
                sql = tasks.first().sql,
                paramMapArr = tasks.map { it.paramMap }.toTypedArray(),
                operationType = KOperationType.UPDATE
            )
        }

        fun <T : KPojo> List<UpdateClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }
    }
}