package com.kotlinorm.database

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.database.RegisteredDBTypeManager.getDBSupport
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.orm.join.JoinClauseInfo
import com.kotlinorm.orm.select.SelectClauseInfo

// Used to generate SQL that is independent of database type, including dialect differences.
object SqlManager {
    private val DBType.dbSupport get() = getDBSupport(this)

    internal fun getDBNameFrom(wrapper: KronosDataSourceWrapper) =
        wrapper.dbType.dbSupport?.getDBNameFromUrl(wrapper) ?: throw UnsupportedDatabaseTypeException(wrapper.dbType)

    fun sqlColumnType(
        dbType: DBType, type: KColumnType, length: Int
    ) = dbType.dbSupport?.getColumnType(type, length) ?: throw UnsupportedDatabaseTypeException(dbType)

    fun getKotlinColumnType(
        dbType: DBType, sqlType: String, length: Int
    ) = dbType.dbSupport?.getKColumnType(sqlType, length) ?: throw UnsupportedDatabaseTypeException(dbType)

    fun columnCreateDefSql(
        dbType: DBType, column: Field
    ) = dbType.dbSupport?.getColumnCreateSql(dbType, column) ?: throw UnsupportedDatabaseTypeException(dbType)

    fun indexCreateDefSql(
        dbType: DBType, tableName: String, kTableIndex: KTableIndex
    ) = dbType.dbSupport?.getIndexCreateSql(dbType, tableName, kTableIndex) ?: throw UnsupportedDatabaseTypeException(
        dbType
    )

    fun getTableCreateSqlList(
        dbType: DBType, tableName: String, tableComment: String?, columns: List<Field>, indexes: List<KTableIndex>
    ) = dbType.dbSupport?.getTableCreateSqlList(dbType, tableName, tableComment, columns, indexes)
        ?: throw UnsupportedDatabaseTypeException(dbType)

    fun getTableExistenceSql(
        dbType: DBType
    ) = dbType.dbSupport?.getTableExistenceSql(dbType) ?: throw UnsupportedDatabaseTypeException(dbType)

    fun getTableTruncateSql(
        dbType: DBType, tableName: String, restartIdentity: Boolean
    ) = dbType.dbSupport?.getTableTruncateSql(dbType, tableName, restartIdentity)
        ?: throw UnsupportedDatabaseTypeException(dbType)

    fun getTableDropSql(
        dbType: DBType, tableName: String
    ) = dbType.dbSupport?.getTableDropSql(dbType, tableName) ?: throw UnsupportedDatabaseTypeException(dbType)

    fun getTableComment(
        dataSource: KronosDataSourceWrapper
    ) = dataSource.dbType.dbSupport?.getTableComment(dataSource.dbType) ?: throw UnsupportedDatabaseTypeException(
        dataSource.dbType
    )

    fun getTableColumns(
        dataSource: KronosDataSourceWrapper, tableName: String
    ) = dataSource.dbType.dbSupport?.getTableColumns(dataSource, tableName) ?: throw UnsupportedDatabaseTypeException(
        dataSource.dbType
    )

    fun getTableIndexes(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ) = dataSource.dbType.dbSupport?.getTableIndexes(dataSource, tableName) ?: throw UnsupportedDatabaseTypeException(
        dataSource.dbType
    )

    fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper, tableName: String, originalTableComment: String?, tableComment: String?, columns: TableColumnDiff, indexes: TableIndexDiff
    ) = dataSource.dbType.dbSupport?.getTableSyncSqlList(dataSource, tableName, originalTableComment,  tableComment, columns, indexes)
        ?: throw UnsupportedDatabaseTypeException(dataSource.dbType)

    fun getOnConflictSql(
        dataSource: KronosDataSourceWrapper, conflictResolver: ConflictResolver
    ) = dataSource.dbType.dbSupport?.getOnConflictSql(conflictResolver) ?: throw UnsupportedDatabaseTypeException(
        dataSource.dbType
    )

    fun Field.quoted(
        dataSource: KronosDataSourceWrapper, showTable: Boolean = false
    ) = dataSource.dbType.dbSupport?.quote(this, showTable) ?: throw UnsupportedDatabaseTypeException(
        dataSource.dbType
    )

    fun quote(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        showTable: Boolean = false,
        columnName: String? = null,
        map: Map<String, String> = emptyMap()
    ): String {
        val databaseName = map[tableName]
        val support = dataSource.dbType.dbSupport ?: throw UnsupportedDatabaseTypeException(dataSource.dbType)

        return listOfNotNull(
            if (databaseName.isNullOrBlank()) null else support.quote(databaseName),
            if (!showTable && databaseName.isNullOrBlank()) null else support.quote(tableName),
            columnName?.let { support.quote(it) }
        ).joinToString(".")
    }

    fun quote(
        dataSource: KronosDataSourceWrapper,
        field: Field,
        showTable: Boolean = false,
        map: Map<String, String> = emptyMap()
    ) = quote(dataSource, field.tableName, showTable, field.columnName, map)

    fun getInsertSql(
        dataSource: KronosDataSourceWrapper, tableName: String, columns: List<Field>
    ) = dataSource.dbType.dbSupport?.getInsertSql(dataSource, tableName, columns)
        ?: throw UnsupportedDatabaseTypeException(dataSource.dbType)

    fun getDeleteSql(
        dataSource: KronosDataSourceWrapper, tableName: String, whereClauseSql: String?
    ) = dataSource.dbType.dbSupport?.getDeleteSql(dataSource, tableName, whereClauseSql)
        ?: throw UnsupportedDatabaseTypeException(dataSource.dbType)

    fun getUpdateSql(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        toUpdateFields: List<Field>,
        whereClauseSql: String?,
        plusAssigns: MutableList<Pair<Field, String>>,
        minusAssigns: MutableList<Pair<Field, String>>
    ) = dataSource.dbType.dbSupport?.getUpdateSql(
        dataSource, tableName, toUpdateFields, whereClauseSql, plusAssigns, minusAssigns
    ) ?: throw UnsupportedDatabaseTypeException(dataSource.dbType)

    fun getSelectSql(
        dataSource: KronosDataSourceWrapper, selectClause: SelectClauseInfo
    ) = dataSource.dbType.dbSupport?.getSelectSql(dataSource, selectClause)
        ?: throw UnsupportedDatabaseTypeException(dataSource.dbType)

    fun getJoinSql(
        dataSource: KronosDataSourceWrapper, joinClause: JoinClauseInfo
    ) = dataSource.dbType.dbSupport?.getJoinSql(dataSource, joinClause)
        ?: throw UnsupportedDatabaseTypeException(dataSource.dbType)
}