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
import com.kotlinorm.database.mssql.MssqlSupport
import com.kotlinorm.database.mysql.MysqlSupport
import com.kotlinorm.database.oracle.OracleSupport
import com.kotlinorm.database.postgres.PostgesqlSupport
import com.kotlinorm.database.sqlite.SqliteSupport
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.DBType.*
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff

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
        Postgres -> PostgesqlSupport.getColumnType(type, length)
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
        Postgres -> PostgesqlSupport.getKColumnType(sqlType, length)
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
        Postgres -> PostgesqlSupport.getColumnCreateSql(dbType, column)
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
        Postgres -> PostgesqlSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
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
        Postgres -> PostgesqlSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
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
        Postgres -> PostgesqlSupport.getTableExistenceSql(dbType)
        Oracle -> OracleSupport.getTableExistenceSql(dbType)
        SQLite -> SqliteSupport.getTableExistenceSql(dbType)
        Mssql -> MssqlSupport.getTableExistenceSql(dbType)
        else -> tryGetTableExistenceSqlCustom(dbType)
            ?: throw RuntimeException("Unsupported database type: $dbType")
    }

    fun getTableDropSql(
        dbType: DBType,
        tableName: String
    ) = when (dbType) {
        Mysql -> MysqlSupport.getTableDropSql(dbType, tableName)
        Postgres -> PostgesqlSupport.getTableDropSql(dbType, tableName)
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
        Postgres -> PostgesqlSupport.getTableColumns(dataSource, tableName)
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
        Postgres -> PostgesqlSupport.getTableIndexes(dataSource, tableName)
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
        Postgres -> PostgesqlSupport.getTableSyncSqlList(dataSource, tableName, columns, indexes)
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
        Postgres -> PostgesqlSupport.getOnConflictSql(conflictResolver)
        Oracle -> OracleSupport.getOnConflictSql(conflictResolver)
        SQLite -> SqliteSupport.getOnConflictSql(conflictResolver)
        Mssql -> MssqlSupport.getOnConflictSql(conflictResolver)
        else -> tryGetOnConflictSqlCustom(dataSource, conflictResolver)
            ?: throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
    }

}