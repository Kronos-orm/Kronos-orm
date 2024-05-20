package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.tableCache.TableCache.getTable
import com.kotlinorm.utils.toLinkedSet

/**
 * Update Clause
 *
 * Creates an update clause for the given pojo.
 *
 * @param T the type of the pojo
 *
 * @property pojo the pojo for the update
 * @property isExcept whether to exclude the fields from the update
 * @param setUpsertFields the fields to update
 * @author Jieyao Lu, OUSC
 */
class UpsertClause<T : KPojo>(
    private val pojo: T,
    private var isExcept: Boolean = false,
    private var setUpsertFields: KTableField<T, Any?> = null
) {
    private var paramMap = pojo.toDataMap()
    private var tableName = pojo.kronosTableName()
    private var createTimeStrategy = pojo.kronosCreateTime()
    private var updateTimeStrategy = pojo.kronosUpdateTime()
    private var logicDeleteStrategy = pojo.kronosLogicDelete()
    private var allFields = pojo.kronosColumns().toLinkedSet()
    private var onDuplicateKey = false
    private var toInsertFields = linkedSetOf<Field>()
    private var toUpdateFields = linkedSetOf<Field>()
    private var onFields = linkedSetOf<Field>()

    init {
        if (setUpsertFields != null) {
            pojo.tableRun {
                setUpsertFields!!(it)
                toUpdateFields += fields
            }
        }
    }

    private fun updateUpsertFields(updateOnFields: Boolean = false): (Field, Any?) -> Unit {
        return { field: Field, value: Any? ->
            toInsertFields += field
            toUpdateFields -= field
            if (updateOnFields) onFields += field
            paramMap[field.name] = value
        }
    }

    /**
     * Set the fields on which the update clause will be applied.
     *
     * @param someFields on which the update clause will be applied
     * @throws NeedFieldsException if the new value is null
     * @return the upsert UpdateClause object
     */
    fun on(someFields: KTableField<T, Unit>): UpsertClause<T> {
        if (null == someFields) throw NeedFieldsException()
        pojo.tableRun {
            someFields(it)
            onFields += fields.toSet()
        }
        return this
    }

    /**
     * On duplicate key update
     *
     * **Please define constraints before using onDuplicateKey**
     *
     * @return the upsert UpdateClause object
     */
    fun onDuplicateKey(): UpsertClause<T> {
        onDuplicateKey = true
        return this
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build(wrapper).execute(wrapper)
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): KronosAtomicTask {
        val dataSource = wrapper.orDefault()
        val dbType = dataSource.dbType

        if (isExcept) {
            toUpdateFields = (allFields - toUpdateFields.toSet()) as LinkedHashSet<Field>
        }

        if (toInsertFields.isEmpty()) {
            toInsertFields = allFields.filter { null != paramMap[it.name] }.toLinkedSet()
        }

        if (toUpdateFields.isEmpty()) {
            toUpdateFields = allFields.toLinkedSet()
        }

        setCommonStrategy(createTimeStrategy, true, callBack = updateUpsertFields())
        setCommonStrategy(updateTimeStrategy, true, callBack = updateUpsertFields())
        setCommonStrategy(logicDeleteStrategy, callBack = updateUpsertFields(true))

        paramMap = paramMap.filter { it ->
            it.key in (toUpdateFields + toInsertFields + onFields).map { it.name }
        }.toMutableMap()

        val sql = if (onDuplicateKey) {
            when (dbType) {
                DBType.Mysql, DBType.OceanBase -> mysqlOnDuplicateSql(
                    ConflictResolver(
                        tableName,
                        onFields,
                        toUpdateFields,
                        toInsertFields
                    )
                )

                else -> {
                    val pks = getTable(dataSource, tableName).columns.filter { it.primaryKey }.toLinkedSet()
                    val conflictResolver = ConflictResolver(tableName, pks, toUpdateFields, toInsertFields)
                    when (dbType) {
                        DBType.Postgres -> postgresOnExistSql(conflictResolver)
                        DBType.Oracle -> oracleOnConflictSql(conflictResolver)
                        DBType.SQLite -> sqliteOnConflictSql(conflictResolver)
                        DBType.Mssql -> sqlServerOnExistSql(conflictResolver)
                        else -> throw UnsupportedDatabaseTypeException()
                    }
                }
            }
        } else {
            generateOnExistSql(dataSource)
        }

        return KronosAtomicTask(
            sql,
            paramMap,
            operationType = KOperationType.UPSERT
        )
    }

    private fun generateOnExistSql(wrapper: KronosDataSourceWrapper): String {
        val conflictResolver = ConflictResolver(tableName, onFields, toUpdateFields, toInsertFields)
        return when (wrapper.dbType) {
            DBType.Mysql, DBType.OceanBase -> mysqlOnExistSql(conflictResolver)
            DBType.Postgres -> postgresOnExistSql(conflictResolver)
            DBType.Mssql -> sqlServerOnExistSql(conflictResolver)
            DBType.Oracle -> oracleOnExistSql(conflictResolver)
            DBType.SQLite -> sqliteOnConflictSql(conflictResolver)
            else -> throw UnsupportedDatabaseTypeException()
        }
    }

    companion object {
        fun <T : KPojo> List<UpsertClause<T>>.on(someFields: KTableField<T, Unit>): List<UpsertClause<T>> {
            return map { it.on(someFields) }
        }

        fun <T : KPojo> List<UpsertClause<T>>.onDuplicateKey(): List<UpsertClause<T>> {
            return map { it.onDuplicateKey() }
        }

        fun <T : KPojo> List<UpsertClause<T>>.build(): KronosAtomicBatchTask {
            val tasks = this.map { it.build() }
            return KronosAtomicBatchTask(
                sql = tasks.first().sql,
                paramMapArr = tasks.map { it.paramMap }.toTypedArray(),
                operationType = KOperationType.UPSERT
            )
        }

        fun <T : KPojo> List<UpsertClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }
    }

}