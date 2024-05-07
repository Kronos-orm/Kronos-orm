package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTable
import com.kotlinorm.beans.dsl.KTableConditional
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.AND
import com.kotlinorm.enums.Equal
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.NeedConditionException
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.Extensions.toMap
import com.kotlinorm.utils.execute
import kotlin.reflect.full.createInstance

class DeleteClause<T : KPojo>(private val pojo:  T) {
    internal lateinit var tableName: String
    private var condition: Criteria? = null
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()

    init {
        paramMap.putAll(pojo.toMap().filter { it.value != null })
    }

    fun logic(): DeleteClause<T> {
        TODO("not implemented")
    }

    fun by(someFields: KTableField<T, Any?>): DeleteClause<T> {
        if (someFields == null) return this
        with(KTable(pojo::class.createInstance())) {
            someFields()
            if (fields.isEmpty()) {
                throw NeedFieldsException()
            }
            condition = Criteria(
                type = AND,
            ).apply {
                children = fields.map {
                    Criteria(
                        type = Equal,
                        field = it,
                        value = paramMap[it.name]
                    )
                }.toMutableList()
            }
        }
        return this
    }

    fun where(deleteCondition: KTableConditionalField<T, Boolean?> = null): DeleteClause<T> {
        if (deleteCondition == null) return this
        with(KTableConditional(pojo::class.createInstance())) {
            propParamMap = paramMap
            deleteCondition()
            condition = criteria ?: Criteria(
                type = AND
            ).apply {
                children = paramMap.keys.map { propName ->
                    Criteria(
                        type = Equal,
                        field = fields.first { it.name == propName },
                        value = paramMap[propName]
                    )
                }.toMutableList()
            }
        }
        return this
    }

    fun build(): KronosAtomicTask {
        val (conditionSql, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        if (conditionSql == null) {
            throw NeedConditionException()
        }
        val sql = "DELETE FROM $tableName WHERE $conditionSql"
        return KronosAtomicTask(sql, paramMap, operationType = KOperationType.DELETE)
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build().execute(wrapper)
    }
}