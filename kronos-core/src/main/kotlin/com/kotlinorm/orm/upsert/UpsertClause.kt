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
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KPojo
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
    private var onFields: LinkedHashSet<Field> = linkedSetOf()
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()

    init {
        if (setUpsertFields != null) {
            pojo.tableRun {
                setUpsertFields()
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
            someFields()
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

}