package com.kotoframework.orm.update

import com.kotoframework.beans.dsl.Criteria
import com.kotoframework.beans.dsl.Field
import com.kotoframework.beans.dsl.KTable
import com.kotoframework.beans.dsl.KTableConditional
import com.kotoframework.beans.task.KotoAtomicBatchTask
import com.kotoframework.beans.task.KotoAtomicTask
import com.kotoframework.beans.task.KotoOperationResult
import com.kotoframework.enums.AND
import com.kotoframework.enums.Equal
import com.kotoframework.enums.KotoAtomicOperationType
import com.kotoframework.exceptions.NeedUpdateConditionException
import com.kotoframework.interfaces.KPojo
import com.kotoframework.interfaces.KotoDataSourceWrapper
import com.kotoframework.types.KTableConditionalField
import com.kotoframework.types.KTableField
import com.kotoframework.utils.ConditionSqlBuilder
import com.kotoframework.utils.Extensions.toMap
import com.kotoframework.utils.execute
import kotlin.reflect.full.createInstance

class UpdateClause<T : KPojo>(
    private val pojo: T, private var isExcept: Boolean = false, setUpdateFields: KTableField<T, Any?> = null
) {
    internal lateinit var tableName: String
    internal var allFields: MutableSet<Field> = mutableSetOf()
    private var toUpdateFields: MutableSet<Field> = mutableSetOf()
    private var condition: Criteria? = null
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private var paramMapNew: MutableMap<Field, Any?> = mutableMapOf()

    init {
        paramMap.putAll(pojo.toMap().filter { it.value != null })
        if (setUpdateFields != null) {
            with(KTable(pojo::class.createInstance())) {
                setUpdateFields()
                toUpdateFields.addAll(fields)
                toUpdateFields.forEach {
                    paramMapNew[
                        Field(
                            it.columnName,
                            it.name + "New"
                        )
                    ] = paramMap[it.name]
                }
            }
        }
    }

    fun set(rowData: KTableField<T, Unit>): UpdateClause<T> {
        if (rowData == null) return this
        with(KTable(pojo::class.createInstance())) {
            rowData()
            toUpdateFields.addAll(fields)
            paramMapNew.putAll(fieldParamMap)
        }
        return this
    }

    fun by(someFields: KTableField<T, Any?>): UpdateClause<T> {
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

    fun where(updateCondition: KTableConditionalField<T, Boolean?> = null): UpdateClause<T> {
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
                        field = toUpdateFields.first { it.name == propName },
                        value = paramMap[propName]
                    )
                }.toMutableList()
            }
        }
        return this
    }

    fun build(): KotoAtomicTask {
        // 如果 isExcept 为 true，则将 toUpdateFields 中的字段从 allFields 中移除
        if (isExcept) {
            toUpdateFields =
                allFields.filter { !toUpdateFields.any { f -> f.columnName == it.columnName } }.toMutableSet()
            toUpdateFields.forEach {
                paramMapNew[Field(
                    it.columnName, it.name + "New"
                )] = paramMap[it.name]
            }
        }

        if (toUpdateFields.isEmpty()) {
            // 全都更新
            toUpdateFields = allFields.toMutableSet()
            toUpdateFields.forEach {
                paramMapNew[Field(
                    it.columnName, it.name + "New"
                )] = paramMap[it.name]
            }
        }

        val updateFields = toUpdateFields.joinToString(", ") { "${it.name} = :${it.name + "New"}" }

        var (conditionSql, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        if (conditionSql != null) {
            conditionSql = "WHERE $conditionSql"
        }
        val sql = listOfNotNull("UPDATE", tableName, "SET", updateFields, conditionSql).joinToString(" ")
        // 合并
        paramMap.apply {
            putAll(paramMapNew.map { entry ->
                // 如果 key 不以 "New" 结尾，则添加 "New" 后缀
                val keyWithSuffix = if (!entry.key.name.endsWith("New")) entry.key.name + "New" else entry.key.name
                keyWithSuffix to entry.value
            })
        }
        return KotoAtomicTask(
            sql,
            paramMap,
            operationType = KotoAtomicOperationType.UPDATE
        )
    }

    fun execute(wrapper: KotoDataSourceWrapper? = null): KotoOperationResult {
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

        fun <T : KPojo> List<UpdateClause<T>>.build(): KotoAtomicBatchTask {
            val tasks = this.map { it.build() }
            return KotoAtomicBatchTask(
                sql = tasks.first().sql,
                paramMapArr = tasks.map { it.paramMap }.toTypedArray(),
                operationType = KotoAtomicOperationType.UPDATE
            )
        }

        fun <T : KPojo> List<UpdateClause<T>>.execute(wrapper: KotoDataSourceWrapper? = null): KotoOperationResult {
            return build().execute(wrapper)
        }
    }
}