package com.kotoframework.orm.delete

import com.kotoframework.beans.dsl.Criteria
import com.kotoframework.beans.dsl.Field
import com.kotoframework.beans.dsl.KTable
import com.kotoframework.beans.dsl.KTableConditional
import com.kotoframework.beans.task.KronosAtomicTask
import com.kotoframework.beans.task.KronosOperationResult
import com.kotoframework.enums.AND
import com.kotoframework.enums.Equal
import com.kotoframework.enums.KOperationType
import com.kotoframework.exceptions.NeedUpdateConditionException
import com.kotoframework.interfaces.KPojo
import com.kotoframework.interfaces.KronosDataSourceWrapper
import com.kotoframework.orm.update.UpdateClause
import com.kotoframework.types.KTableConditionalField
import com.kotoframework.types.KTableField
import com.kotoframework.utils.ConditionSqlBuilder
import com.kotoframework.utils.Extensions.toMap
import com.kotoframework.utils.execute
import kotlin.reflect.full.createInstance

class DeleteClause<T : KPojo>(private val pojo:  T) {

    internal lateinit var tableName: String
    private var condition: Criteria? = null
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()


    init {
        paramMap.putAll(pojo.toMap().filter { it.value != null })
    }

    fun logic(): DeleteClause<T> {
       return this
    }

    fun by(someFields: KTableField<T, Any?>): DeleteClause<T> {
        if (someFields == null) return this
        with(KTable(pojo::class.createInstance())) {
            someFields()
            if (fields.isEmpty()) {
                throw NeedUpdateConditionException()
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

    fun where(updateCondition: KTableConditionalField<T, Boolean?> = null): DeleteClause<T> {
        if (updateCondition == null) return this
        with(KTableConditional(pojo::class.createInstance())) {
            propParamMap = paramMap
            updateCondition()
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
            throw RuntimeException("Delete operation requires a condition.")
        }
        val sql = "DELETE FROM $tableName WHERE $conditionSql"
        return KronosAtomicTask(sql, paramMap, operationType = KOperationType.DELETE)
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build().execute(wrapper)
    }

    operator fun component1(): String {
        // 返回构建好的SQL语句
        val (conditionSql, _) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        return "DELETE FROM $tableName WHERE $conditionSql"
    }

    operator fun component2(): Map<String, Any?> {
        // 返回用于执行SQL的参数映射
        val (_, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        return paramMap
    }
}