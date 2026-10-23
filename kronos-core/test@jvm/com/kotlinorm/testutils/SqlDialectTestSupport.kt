package com.kotlinorm.testutils

import com.kotlinorm.Kronos
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KAtomicTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import com.kotlinorm.wrappers.SampleOracleJdbcWrapper
import com.kotlinorm.wrappers.SamplePostgresJdbcWrapper
import com.kotlinorm.wrappers.SampleSqlServerJdbcWrapper
import com.kotlinorm.wrappers.SampleSqliteJdbcWrapper
import kotlin.test.assertEquals

data class DialectCase(
    val dbType: DBType,
    val label: String,
    val wrapper: KronosDataSourceWrapper
)

data class ExpectedTask(
    val sql: String,
    val params: Map<String, Any?> = emptyMap()
)

val coreSqlDialects = listOf(
    DialectCase(DBType.Mysql, "mysql", SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper),
    DialectCase(DBType.Postgres, "postgres", SamplePostgresJdbcWrapper()),
    DialectCase(DBType.SQLite, "sqlite", SampleSqliteJdbcWrapper),
    DialectCase(DBType.Mssql, "mssql", SampleSqlServerJdbcWrapper),
    DialectCase(DBType.Oracle, "oracle", SampleOracleJdbcWrapper)
)

fun initializeCoreSqlTestDefaults() {
    Kronos.fieldNamingStrategy = Kronos.lineHumpNamingStrategy
    Kronos.tableNamingStrategy = Kronos.lineHumpNamingStrategy
}

fun assertTaskEquals(expected: ExpectedTask, actual: KAtomicTask, label: String) {
    assertEquals(expected.sql.normalizeSql(), actual.sql.normalizeSql(), "$label sql")
    assertEquals(expected.params, actual.paramMap.toMap(), "$label params")
}

fun String.normalizeSql(): String =
    trim().replace(Regex("\\s+"), " ")
