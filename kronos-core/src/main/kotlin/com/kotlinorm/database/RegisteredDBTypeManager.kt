package com.kotlinorm.database

import com.kotlinorm.database.mssql.MssqlSupport
import com.kotlinorm.database.mysql.MysqlSupport
import com.kotlinorm.database.oracle.OracleSupport
import com.kotlinorm.database.postgres.PostgresqlSupport
import com.kotlinorm.database.sqlite.SqliteSupport
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.DBType.Mysql
import com.kotlinorm.enums.DBType.Postgres
import com.kotlinorm.enums.DBType.SQLite
import com.kotlinorm.enums.DBType.Mssql
import com.kotlinorm.enums.DBType.Oracle
import com.kotlinorm.interfaces.DatabasesSupport

// Used to generate SQL that is independent of database type, including dialect differences.
object RegisteredDBTypeManager {
    private val dBTypeSupport: MutableMap<DBType, DatabasesSupport> = mutableMapOf(
        Mysql to MysqlSupport,
        Postgres to PostgresqlSupport,
        SQLite to SqliteSupport,
        Mssql to MssqlSupport,
        Oracle to OracleSupport
    )

    @Suppress("UNUSED")
    // Custom functions for generating SQL that is independent of database type, including dialect differences.
    fun registerDBTypeSupport(dbType: DBType, support: DatabasesSupport) {
        dBTypeSupport[dbType] = support
    }

    fun getDBSupport(dbType: DBType): DatabasesSupport? {
        return dBTypeSupport[dbType]
    }
}