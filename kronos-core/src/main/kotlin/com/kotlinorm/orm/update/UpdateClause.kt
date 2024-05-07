package com.kotlinorm.orm.update

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTable
import com.kotlinorm.beans.dsl.KTableConditional
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.AND
import com.kotlinorm.enums.Equal
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.Extensions.toMap
import com.kotlinorm.utils.execute
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

    fun build(): KronosAtomicTask {
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