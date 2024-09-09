package com.kotlinorm.database

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.database.SqlManagerCustom.tryGetColumnCreateSqlCustom
import com.kotlinorm.database.SqlManagerCustom.tryGetDBNameFromUrlCustom
import com.kotlinorm.database.SqlManagerCustom.tryGetIndexCreateSqlCustom
import com.kotlinorm.database.SqlManagerCustom.tryGetOnConflictSqlCustom
import com.kotlinorm.database.SqlManagerCustom.tryGetSqlColumnTypeCustom
import com.kotlinorm.database.SqlManagerCustom.tryGetTableColumnsCustom
import com.kotlinorm.database.SqlManagerCustom.tryGetTableCreateSqlListCustom
import com.kotlinorm.database.SqlManagerCustom.tryGetTableDropSqlCustom
import com.kotlinorm.database.SqlManagerCustom.tryGetTableExistenceSqlCustom
import com.kotlinorm.database.SqlManagerCustom.tryGetTableIndexesCustom
import com.kotlinorm.database.SqlManagerCustom.tryGetTableSyncSqlListCustom
import com.kotlinorm.database.SqlManagerCustom.tryGetTableTruncateSqlCustom
import com.kotlinorm.database.mssql.MssqlSupport
import com.kotlinorm.database.mysql.MysqlSupport
import com.kotlinorm.database.oracle.OracleSupport
import com.kotlinorm.database.postgres.PostgresqlSupport
import com.kotlinorm.database.sqlite.SqliteSupport
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.DBType.*
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.orm.join.JoinClauseInfo
import com.kotlinorm.orm.select.SelectClauseInfo

// Used to generate SQL that is independent of database type, including dialect differences.
object SqlManager {
    internal fun getDBNameFrom(wrapper: KronosDataSourceWrapper) = when (wrapper.dbType) {
        Mysql -> wrapper.url.split("?").first().split("//")[1].split("/").last()
        SQLite -> wrapper.url.split("//").last()
        Oracle -> wrapper.userName
        Mssql -> wrapper.url.split("//").last().split(";").first()
        Postgres -> wrapper.url.split("//").last().split("/").first()
        else -> tryGetDBNameFromUrlCustom(wrapper)
            ?: throw UnsupportedDatabaseTypeException("Unsupported database type: ${wrapper.dbType}")
    }

    fun sqlColumnType(
        dbType: DBType,
        type: KColumnType,
        length: Int
    ) = when (dbType) {
        Mysql -> MysqlSupport.getColumnType(type, length)
        Oracle -> OracleSupport.getColumnType(type, length)
        Mssql -> MssqlSupport.getColumnType(type, length)
        Postgres -> PostgresqlSupport.getColumnType(type, length)
        SQLite -> SqliteSupport.getColumnType(type, length)
        // For other database types, use the custom function if it exists, otherwise throw an exception.
        else -> tryGetSqlColumnTypeCustom(dbType, type, length)
            ?: throw RuntimeException("Unsupported database type: $dbType")
    }

    fun getKotlinColumnType(
        dbType: DBType,
        sqlType: String,
        length: Int
    ) = when (dbType) {
        Mysql -> MysqlSupport.getKColumnType(sqlType, length)
        Postgres -> PostgresqlSupport.getKColumnType(sqlType, length)
        Oracle -> OracleSupport.getKColumnType(sqlType, length)
        SQLite -> SqliteSupport.getKColumnType(sqlType, length)
        Mssql -> MssqlSupport.getKColumnType(sqlType, length)
        else -> throw RuntimeException("Unsupported database type: $dbType")
    }

    fun columnCreateDefSql(
        dbType: DBType,
        column: Field
    ) = when (dbType) {
        Mysql -> MysqlSupport.getColumnCreateSql(dbType, column)
        Postgres -> PostgresqlSupport.getColumnCreateSql(dbType, column)
        Oracle -> OracleSupport.getColumnCreateSql(dbType, column)
        SQLite -> SqliteSupport.getColumnCreateSql(dbType, column)
        Mssql -> MssqlSupport.getColumnCreateSql(dbType, column)
        else -> tryGetColumnCreateSqlCustom(dbType, column)
            ?: throw RuntimeException("Unsupported database type: $dbType")
    }

    fun indexCreateDefSql(
        dbType: DBType,
        tableName: String,
        kTableIndex: KTableIndex
    ) = when (dbType) {
        Mysql -> MysqlSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
        Postgres -> PostgresqlSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
        Oracle -> OracleSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
        SQLite -> SqliteSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
        Mssql -> MssqlSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
        else -> tryGetIndexCreateSqlCustom(dbType, tableName, kTableIndex)
            ?: throw RuntimeException("Unsupported database type: $dbType")
    }

    fun getTableCreateSqlList(
        dbType: DBType,
        tableName: String,
        columns: List<Field>,
        indexes: List<KTableIndex>
    ) = when (dbType) {
        Mysql -> MysqlSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
        Postgres -> PostgresqlSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
        Oracle -> OracleSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
        SQLite -> SqliteSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
        Mssql -> MssqlSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
        else -> tryGetTableCreateSqlListCustom(dbType, tableName, columns, indexes)
            ?: throw RuntimeException("Unsupported database type: $dbType")
    }

    fun getTableExistenceSql(
        dbType: DBType
    ) = when (dbType) {
        Mysql -> MysqlSupport.getTableExistenceSql(dbType)
        Postgres -> PostgresqlSupport.getTableExistenceSql(dbType)
        Oracle -> OracleSupport.getTableExistenceSql(dbType)
        SQLite -> SqliteSupport.getTableExistenceSql(dbType)
        Mssql -> MssqlSupport.getTableExistenceSql(dbType)
        else -> tryGetTableExistenceSqlCustom(dbType)
            ?: throw RuntimeException("Unsupported database type: $dbType")
    }

    fun getTableTruncateSql(
        dbType: DBType,
        tableName: String,
        restartIdentity: Boolean
    ) = when (dbType) {
        Mysql -> MysqlSupport.getTableTruncateSql(dbType, tableName, restartIdentity)
        Postgres -> PostgresqlSupport.getTableTruncateSql(dbType, tableName, restartIdentity)
        Oracle -> OracleSupport.getTableTruncateSql(dbType, tableName, restartIdentity)
        SQLite -> SqliteSupport.getTableTruncateSql(dbType, tableName, restartIdentity)
        Mssql -> MssqlSupport.getTableTruncateSql(dbType, tableName, restartIdentity)
        else -> tryGetTableTruncateSqlCustom(dbType, tableName, restartIdentity)
            ?: throw RuntimeException("Unsupported database type: $dbType")
    }

    fun getTableDropSql(
        dbType: DBType,
        tableName: String
    ) = when (dbType) {
        Mysql -> MysqlSupport.getTableDropSql(dbType, tableName)
        Postgres -> PostgresqlSupport.getTableDropSql(dbType, tableName)
        Oracle -> OracleSupport.getTableDropSql(dbType, tableName)
        SQLite -> SqliteSupport.getTableDropSql(dbType, tableName)
        Mssql -> MssqlSupport.getTableDropSql(dbType, tableName)
        else -> tryGetTableDropSqlCustom(dbType, tableName)
            ?: throw RuntimeException("Unsupported database type: $dbType")
    }

    fun getTableColumns(
        dataSource: KronosDataSourceWrapper,
        tableName: String
    ) = when (dataSource.dbType) {
        Mysql -> MysqlSupport.getTableColumns(dataSource, tableName)
        Postgres -> PostgresqlSupport.getTableColumns(dataSource, tableName)
        Oracle -> OracleSupport.getTableColumns(dataSource, tableName)
        SQLite -> SqliteSupport.getTableColumns(dataSource, tableName)
        Mssql -> MssqlSupport.getTableColumns(dataSource, tableName)
        else -> tryGetTableColumnsCustom(dataSource, tableName)
            ?: throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
    }

    fun getTableIndexes(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ) = when (dataSource.dbType) {
        Mysql -> MysqlSupport.getTableIndexes(dataSource, tableName)
        Postgres -> PostgresqlSupport.getTableIndexes(dataSource, tableName)
        Oracle -> OracleSupport.getTableIndexes(dataSource, tableName)
        SQLite -> SqliteSupport.getTableIndexes(dataSource, tableName)
        Mssql -> MssqlSupport.getTableIndexes(dataSource, tableName)
        else -> tryGetTableIndexesCustom(dataSource, tableName)
            ?: throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
    }

    fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        columns: TableColumnDiff,
        indexes: TableIndexDiff
    ) = when (dataSource.dbType) {
        Mysql -> MysqlSupport.getTableSyncSqlList(dataSource, tableName, columns, indexes)
        Postgres -> PostgresqlSupport.getTableSyncSqlList(dataSource, tableName, columns, indexes)
        Oracle -> OracleSupport.getTableSyncSqlList(dataSource, tableName, columns, indexes)
        SQLite -> SqliteSupport.getTableSyncSqlList(dataSource, tableName, columns, indexes)
        Mssql -> MssqlSupport.getTableSyncSqlList(dataSource, tableName, columns, indexes)
        else -> tryGetTableSyncSqlListCustom(dataSource, tableName, columns, indexes)
            ?: throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
    }

    fun getOnConflictSql(
        dataSource: KronosDataSourceWrapper,
        conflictResolver: ConflictResolver
    ) = when (dataSource.dbType) {
        Mysql -> MysqlSupport.getOnConflictSql(conflictResolver)
        Postgres -> PostgresqlSupport.getOnConflictSql(conflictResolver)
        Oracle -> OracleSupport.getOnConflictSql(conflictResolver)
        SQLite -> SqliteSupport.getOnConflictSql(conflictResolver)
        Mssql -> MssqlSupport.getOnConflictSql(conflictResolver)
        else -> tryGetOnConflictSqlCustom(dataSource, conflictResolver)
            ?: throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
    }

    fun Field.quoted(
        dataSource: KronosDataSourceWrapper,
        showTable: Boolean = false
    ) = when (dataSource.dbType) {
        Mysql -> MysqlSupport.quote(this, showTable)
        Postgres -> PostgresqlSupport.quote(this, showTable)
        Oracle -> OracleSupport.quote(this, showTable)
        SQLite -> SqliteSupport.quote(this, showTable)
        Mssql -> MssqlSupport.quote(this, showTable)
        else -> throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
    }

    fun quote(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        showTable: Boolean = false,
        columnName: String? = null,
        map: Map<String, String> = emptyMap()
    ): String {

        val databaseName = map[tableName]

        return when (dataSource.dbType) {
            Mysql -> listOfNotNull(
                if (databaseName.isNullOrBlank()) null else MysqlSupport.quote(databaseName),
                if (!showTable && databaseName.isNullOrBlank()) null else MysqlSupport.quote(tableName),
                columnName?.let { MysqlSupport.quote(it) }
            ).joinToString(".")

            Postgres -> listOfNotNull(
                if (databaseName.isNullOrBlank()) null else PostgresqlSupport.quote(databaseName),
                if (!showTable && databaseName.isNullOrBlank()) null else PostgresqlSupport.quote(tableName),
                columnName?.let { PostgresqlSupport.quote(it) }
            ).joinToString(".")

            Oracle -> listOfNotNull(
                if (databaseName.isNullOrBlank()) null else OracleSupport.quote(databaseName),
                if (!showTable && databaseName.isNullOrBlank()) null else OracleSupport.quote(tableName),
                columnName?.let { OracleSupport.quote(it) }
            ).joinToString(".")

            SQLite -> listOfNotNull(
                if (databaseName.isNullOrBlank()) null else SqliteSupport.quote(databaseName),
                if (!showTable && databaseName.isNullOrBlank()) null else SqliteSupport.quote(tableName),
                columnName?.let { SqliteSupport.quote(it) }
            ).joinToString(".")

            Mssql -> listOfNotNull(
                if (databaseName.isNullOrBlank()) null else MssqlSupport.quote(databaseName),
                if (!showTable && databaseName.isNullOrBlank()) null else MssqlSupport.quote(tableName),
                columnName?.let { MssqlSupport.quote(it) }
            ).joinToString(".")

            else -> throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
        }
    }

    fun quote(
        dataSource: KronosDataSourceWrapper,
        field: Field,
        showTable: Boolean = false,
        map: Map<String, String> = emptyMap()
    ) = quote(dataSource, field.tableName, showTable, field.columnName, map)

    fun getInsertSql(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        columns: List<Field>
    ) = when (dataSource.dbType) {
        Mysql -> MysqlSupport.getInsertSql(dataSource, tableName, columns)
        Postgres -> PostgresqlSupport.getInsertSql(dataSource, tableName, columns)
        Oracle -> OracleSupport.getInsertSql(dataSource, tableName, columns)
        SQLite -> SqliteSupport.getInsertSql(dataSource, tableName, columns)
        Mssql -> MssqlSupport.getInsertSql(dataSource, tableName, columns)
        else -> throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
    }

    fun getDeleteSql(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        whereClauseSql: String?
    ) = when (dataSource.dbType) {
        Mysql -> MysqlSupport.getDeleteSql(dataSource, tableName, whereClauseSql)
        Postgres -> PostgresqlSupport.getDeleteSql(dataSource, tableName, whereClauseSql)
        Oracle -> OracleSupport.getDeleteSql(dataSource, tableName, whereClauseSql)
        SQLite -> SqliteSupport.getDeleteSql(dataSource, tableName, whereClauseSql)
        Mssql -> MssqlSupport.getDeleteSql(dataSource, tableName, whereClauseSql)
        else -> throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
    }

    fun getUpdateSql(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        toUpdateFields: List<Field>,
        versionField: String?,
        whereClauseSql: String?
    ) = when (dataSource.dbType) {
        Mysql -> MysqlSupport.getUpdateSql(dataSource, tableName, toUpdateFields, versionField, whereClauseSql)
        Postgres -> PostgresqlSupport.getUpdateSql(
            dataSource,
            tableName,
            toUpdateFields,
            versionField,
            whereClauseSql
        )

        Oracle -> OracleSupport.getUpdateSql(dataSource, tableName, toUpdateFields, versionField, whereClauseSql)
        SQLite -> SqliteSupport.getUpdateSql(dataSource, tableName, toUpdateFields, versionField, whereClauseSql)
        Mssql -> MssqlSupport.getUpdateSql(dataSource, tableName, toUpdateFields, versionField, whereClauseSql)
        else -> throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
    }

    fun getSelectSql(
        dataSource: KronosDataSourceWrapper,
        selectClause: SelectClauseInfo
    ) = when (dataSource.dbType) {
        Mysql -> MysqlSupport.getSelectSql(dataSource, selectClause)
        Postgres -> PostgresqlSupport.getSelectSql(dataSource, selectClause)
        Oracle -> OracleSupport.getSelectSql(dataSource, selectClause)
        SQLite -> SqliteSupport.getSelectSql(dataSource, selectClause)
        Mssql -> MssqlSupport.getSelectSql(dataSource, selectClause)
        else -> throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
    }

    fun getJoinSql(
        dataSource: KronosDataSourceWrapper,
        joinClause: JoinClauseInfo
    ) = when (dataSource.dbType) {
        Mysql -> MysqlSupport.getJoinSql(dataSource, joinClause)
        Postgres -> PostgresqlSupport.getJoinSql(dataSource, joinClause)
        Oracle -> OracleSupport.getJoinSql(dataSource, joinClause)
        SQLite -> SqliteSupport.getJoinSql(dataSource, joinClause)
        Mssql -> MssqlSupport.getJoinSql(dataSource, joinClause)
        else -> throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
    }
}