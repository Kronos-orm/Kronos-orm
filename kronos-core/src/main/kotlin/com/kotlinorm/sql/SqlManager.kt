package com.kotlinorm.sql

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.sql.SqlManagerCustom.tryGetColumnCreateSqlCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetDBNameFromUrlCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetIndexCreateSqlCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetSqlColumnTypeCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetTableColumnsCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetTableCreateSqlListCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetTableDropSqlCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetTableExistenceSqlCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetTableIndexesCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetTableSyncSqlListCustom
import com.kotlinorm.sql.mssql.MssqlSupport
import com.kotlinorm.sql.mysql.MysqlSupport
import com.kotlinorm.sql.oracle.OracleSupport
import com.kotlinorm.sql.postgres.PostgesqlSupport
import com.kotlinorm.sql.sqlite.SqliteSupport

// Used to generate SQL that is independent of database type, including dialect differences.
object SqlManager {
    internal fun getDBNameFrom(wrapper: KronosDataSourceWrapper): String {
        return when (wrapper.dbType) {
            DBType.Mysql -> wrapper.url.split("?").first().split("//")[1].split("/").last()
            DBType.SQLite -> wrapper.url.split("//").last()
            DBType.Oracle -> wrapper.userName
            DBType.Mssql -> wrapper.url.split("//").last().split(";").first()
            DBType.Postgres -> wrapper.url.split("//").last().split("/").first()
            else -> tryGetDBNameFromUrlCustom(wrapper)
                ?: throw UnsupportedDatabaseTypeException("Unsupported database type: ${wrapper.dbType}")
        }
    }

    fun sqlColumnType(
        dbType: DBType,
        type: KColumnType,
        length: Int
    ): String {
        return when (dbType) {
            DBType.Mysql -> MysqlSupport.getColumnType(type, length)
            DBType.Oracle -> OracleSupport.getColumnType(type, length)
            DBType.Mssql -> MssqlSupport.getColumnType(type, length)
            DBType.Postgres -> PostgesqlSupport.getColumnType(type, length)
            DBType.SQLite -> SqliteSupport.getColumnType(type, length)
            // For other database types, use the custom function if it exists, otherwise throw an exception.
            else -> tryGetSqlColumnTypeCustom(dbType, type, length)
                ?: throw RuntimeException("Unsupported database type: $dbType")
        }
    }

    fun columnCreateDefSql(
        dbType: DBType,
        column: Field
    ): String {
        return when (dbType) {
            DBType.Mysql -> MysqlSupport.getColumnCreateSql(dbType, column)
            DBType.Postgres -> PostgesqlSupport.getColumnCreateSql(dbType, column)
            DBType.Oracle -> OracleSupport.getColumnCreateSql(dbType, column)
            DBType.SQLite -> SqliteSupport.getColumnCreateSql(dbType, column)
            DBType.Mssql -> MssqlSupport.getColumnCreateSql(dbType, column)
            else -> tryGetColumnCreateSqlCustom(dbType, column)
                ?: throw RuntimeException("Unsupported database type: $dbType")
        }
    }

    fun indexCreateDefSql(
        dbType: DBType,
        tableName: String,
        kTableIndex: KTableIndex
    ): String {
        return when (dbType) {
            DBType.Mysql -> MysqlSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
            DBType.Postgres -> PostgesqlSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
            DBType.Oracle -> OracleSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
            DBType.SQLite -> SqliteSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
            DBType.Mssql -> MssqlSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
            else -> tryGetIndexCreateSqlCustom(dbType, tableName, kTableIndex)
                ?: throw RuntimeException("Unsupported database type: $dbType")
        }
    }

    fun getTableCreateSqlList(
        dbType: DBType,
        tableName: String,
        columns: List<Field>,
        indexes: List<KTableIndex>
    ): List<String> {
        return when (dbType) {
            DBType.Mysql -> MysqlSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
            DBType.Postgres -> PostgesqlSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
            DBType.Oracle -> OracleSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
            DBType.SQLite -> SqliteSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
            DBType.Mssql -> MssqlSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
            else -> tryGetTableCreateSqlListCustom(dbType, tableName, columns, indexes)
                ?: throw RuntimeException("Unsupported database type: $dbType")
        }
    }

    fun getTableExistenceSql(
        dbType: DBType
    ): String {
        return when (dbType) {
            DBType.Mysql -> MysqlSupport.getTableExistenceSql(dbType)
            DBType.Postgres -> PostgesqlSupport.getTableExistenceSql(dbType)
            DBType.Oracle -> OracleSupport.getTableExistenceSql(dbType)
            DBType.SQLite -> SqliteSupport.getTableExistenceSql(dbType)
            DBType.Mssql -> MssqlSupport.getTableExistenceSql(dbType)
            else -> tryGetTableExistenceSqlCustom(dbType)
                ?: throw RuntimeException("Unsupported database type: $dbType")
        }
    }

    fun getTableDropSql(
        dbType: DBType,
        tableName: String
    ): String {
        return when (dbType) {
            DBType.Mysql -> MysqlSupport.getTableDropSql(dbType, tableName)
            DBType.Postgres -> PostgesqlSupport.getTableDropSql(dbType, tableName)
            DBType.Oracle -> OracleSupport.getTableDropSql(dbType, tableName)
            DBType.SQLite -> SqliteSupport.getTableDropSql(dbType, tableName)
            DBType.Mssql -> MssqlSupport.getTableDropSql(dbType, tableName)
            else -> tryGetTableDropSqlCustom(dbType, tableName)
                ?: throw RuntimeException("Unsupported database type: $dbType")
        }
    }

    fun getTableColumns(
        dataSource: KronosDataSourceWrapper,
        tableName: String
    ): List<Field> {
        return when (dataSource.dbType) {
            DBType.Mysql -> MysqlSupport.getTableColumns(dataSource, tableName)
            DBType.Postgres -> PostgesqlSupport.getTableColumns(dataSource, tableName)
            DBType.Oracle -> OracleSupport.getTableColumns(dataSource, tableName)
            DBType.SQLite -> MysqlSupport.getTableColumns(dataSource, tableName)
            DBType.Mssql -> MysqlSupport.getTableColumns(dataSource, tableName)
            else -> tryGetTableColumnsCustom(dataSource, tableName)
                ?: throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
        }
    }

    fun getTableIndexes(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ): List<KTableIndex> {
        return when (dataSource.dbType) {
            DBType.Mysql -> MysqlSupport.getTableIndexes(dataSource, tableName)
            DBType.Postgres -> PostgesqlSupport.getTableIndexes(dataSource, tableName)
            DBType.Oracle -> OracleSupport.getTableIndexes(dataSource, tableName)
            DBType.SQLite -> SqliteSupport.getTableIndexes(dataSource, tableName)
            DBType.Mssql -> MysqlSupport.getTableIndexes(dataSource, tableName)
            else -> tryGetTableIndexesCustom(dataSource, tableName)
                ?: throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
        }
    }

    fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        columns: TableColumnDiff,
        indexes: TableIndexDiff
    ): List<String> {
        return when (dataSource.dbType) {
            DBType.Mysql -> MysqlSupport.getTableSyncSqlList(dataSource, tableName, columns, indexes)
            DBType.Postgres -> PostgesqlSupport.getTableSyncSqlList(dataSource, tableName, columns, indexes)
            DBType.Oracle -> OracleSupport.getTableSyncSqlList(dataSource, tableName, columns, indexes)
            DBType.SQLite -> SqliteSupport.getTableSyncSqlList(dataSource, tableName, columns, indexes)
            DBType.Mssql -> MssqlSupport.getTableSyncSqlList(dataSource, tableName, columns, indexes)
            else -> tryGetTableSyncSqlListCustom(dataSource, tableName, columns, indexes)
                ?: throw RuntimeException("Unsupported database type: ${dataSource.dbType}")
        }
    }

}