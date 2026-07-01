/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlTable

interface SqlRenderer {
    fun renderStatement(statement: SqlStatement): String
    fun renderQuery(query: SqlQuery): String
    fun renderExpr(expr: SqlExpr): String
    fun renderTable(table: SqlTable): String
}

interface SqlPrinter : SqlRenderer

fun sqlRenderer(dialect: SqlDialect = SqlDialect.Standard): SqlRenderer = when (dialect.family) {
    SqlDialectFamily.Standard -> StandardSqlRenderer(dialect)
    SqlDialectFamily.MySql -> MysqlSqlRenderer(standardEscapeStrings = dialect.standardEscapeStrings)
    SqlDialectFamily.PostgreSql -> PostgresqlSqlRenderer()
    SqlDialectFamily.SQLite -> SqliteSqlRenderer()
    SqlDialectFamily.Oracle -> OracleSqlRenderer()
    SqlDialectFamily.SqlServer -> SqlServerSqlRenderer()
}

fun SqlStatement.toSql(dialect: SqlDialect = SqlDialect.Standard): String =
    sqlRenderer(dialect).renderStatement(this)

fun SqlQuery.toSql(dialect: SqlDialect = SqlDialect.Standard): String =
    sqlRenderer(dialect).renderQuery(this)

fun SqlExpr.toSql(dialect: SqlDialect = SqlDialect.Standard): String =
    sqlRenderer(dialect).renderExpr(this)

fun SqlTable.toSql(dialect: SqlDialect = SqlDialect.Standard): String =
    sqlRenderer(dialect).renderTable(this)
