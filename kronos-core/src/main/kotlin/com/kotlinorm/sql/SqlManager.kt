package com.kotlinorm.sql

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.sql.SqlManagerCustom.tryGetColumnCreateSqlCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetDBNameFromUrlCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetIndexCreateSqlCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetSqlColumnTypeCustom
import com.kotlinorm.sql.SqlManagerCustom.tryGetTableCreateSqlListCustom
import com.kotlinorm.sql.mysql.MysqlSupport
import com.kotlinorm.sql.oracle.OracleSupport
import com.kotlinorm.sql.postgres.PostgesqlSupport

// Used to generate SQL that is independent of database type, including dialect differences.
object SqlManager {
    internal fun getDBNameFromUrl(wrapper: KronosDataSourceWrapper): String {
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
            DBType.Oracle -> MysqlSupport.getColumnType(type, length)
            DBType.Mssql -> MysqlSupport.getColumnType(type, length)
            DBType.Postgres -> MysqlSupport.getColumnType(type, length)
            DBType.SQLite -> MysqlSupport.getColumnType(type, length)
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
            DBType.SQLite -> MysqlSupport.getColumnCreateSql(dbType, column)
            DBType.Mssql -> MysqlSupport.getColumnCreateSql(dbType, column)
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
            DBType.SQLite -> MysqlSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
            DBType.Mssql -> MysqlSupport.getIndexCreateSql(dbType, tableName, kTableIndex)
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
            DBType.SQLite -> MysqlSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
            DBType.Mssql -> MysqlSupport.getTableCreateSqlList(dbType, tableName, columns, indexes)
            else -> tryGetTableCreateSqlListCustom(dbType, tableName, columns, indexes)
                ?: throw RuntimeException("Unsupported database type: $dbType")
        }
    }

}