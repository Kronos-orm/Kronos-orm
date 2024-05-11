package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTable
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.toMap
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.setCommonStrategy

class UpsertClause<T : KPojo>(
    private val t: T,
    private var isExcept: Boolean = false,
    setUpsertFields: (KTable<T>.() -> Unit)? = null
) {

    internal lateinit var tableName: String
    internal lateinit var createTimeStrategy: KronosCommonStrategy
    internal lateinit var updateTimeStrategy: KronosCommonStrategy
    internal lateinit var logicDeleteStrategy: KronosCommonStrategy
    internal var allFields: LinkedHashSet<Field> = linkedSetOf()
    private var onDuplicateKey: Boolean = false
    private var toInsertFields: LinkedHashSet<Field> = linkedSetOf()
    private var toUpdateFields: LinkedHashSet<Field> = linkedSetOf()
    private var duplicateFeilds: LinkedHashSet<Field> = linkedSetOf()
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()

    init {
        paramMap.putAll(t.toMap())
        if (setUpsertFields != null) {
            t.tableRun {
                setUpsertFields()
                toUpdateFields += fields
            }
        }
    }

    fun on(someFields: KTableField<T, Unit>): UpsertClause<T> {
        if (null == someFields) throw NeedFieldsException()
        t.tableRun {
            someFields()
            duplicateFeilds += fields.toSet()
        }
        return this
    }

    fun onDuplicateKey(): UpsertClause<T> {
        onDuplicateKey = true
        return this
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build(wrapper).execute(wrapper)
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): KronosAtomicTask {

        val dbType = wrapper.orDefault().dbType

        if (isExcept) {
            toUpdateFields = (allFields - toUpdateFields.toSet()) as LinkedHashSet<Field>
        }

        if (toInsertFields.isEmpty()) {
            toInsertFields = linkedSetOf<Field>().apply {
                addAll(allFields.filter { null != paramMap[it.name] })
            }
        }

        if (toUpdateFields.isEmpty()) {
            toUpdateFields = linkedSetOf<Field>().apply {
                addAll(allFields)
            }
        }

        setCommonStrategy(createTimeStrategy, true) { field, value ->
            toInsertFields += field
            toUpdateFields -= field
            paramMap[field.name] = value
        }

        setCommonStrategy(updateTimeStrategy, true) { field, value ->
            toInsertFields += field
            toUpdateFields += field
            paramMap[field.name] = value
        }

        setCommonStrategy(logicDeleteStrategy) { field, value ->
            toInsertFields += field
            toUpdateFields -= field
            duplicateFeilds += field
            paramMap[field.name] = value
        }

        val sql = when (dbType) {
            DBType.Mysql -> {
                if (onDuplicateKey) {
                    listOfNotNull(
                        "INSERT INTO",
                        "`${tableName}`",
                        "(" + toInsertFields.joinToString { it.quotedColumnName() } + ")",
                        "VALUES",
                        "(" + toInsertFields.joinToString { ":${it.name}" } + ")",
                        "ON DUPLICATE KEY UPDATE",
                        toUpdateFields.joinToString(", ") { "${it.quotedColumnName()} = :${it.name}" }
                    ).joinToString(" ")
                } else if (duplicateFeilds.isNotEmpty()) {
                    listOfNotNull(
                        "INSERT INTO",
                        "`${tableName}`",
                        "(" + toInsertFields.joinToString { it.quotedColumnName() }+ ")",
                        "SELECT",
                        toInsertFields.joinToString { ":${it.name}" },
                        "FROM DUAL WHERE NOT EXISTS ( SELECT 1 FROM",
                        "`${tableName}`",
                        "WHERE",
                        duplicateFeilds.joinToString(" AND ") { "${it.quotedColumnName()} = :${it.name}" },
                        ");\n",
                        "UPDATE",
                        "`${tableName}`",
                        "SET",
                        toUpdateFields.joinToString { "${it.quotedColumnName()} = :${it.name}" },
                        "WHERE",
                        duplicateFeilds.joinToString(" AND ") { "${it.quotedColumnName()} = :${it.name}" }
                    ).joinToString(" ")

                } else {
                    listOfNotNull(
                        "INSERT INTO",
                        "`${tableName}`",
                        "(" + toInsertFields.joinToString { it.quotedColumnName() } + ")",
                        "VALUES",
                        "(" + toInsertFields.joinToString { ":${it.name}" } + ")"
                    ).joinToString(" ")
                }
            }

            else -> {
                TODO()
            }

        }

        return KronosAtomicTask(
            sql,
            paramMap,
            operationType = KOperationType.UPSERT
        )
    }
}