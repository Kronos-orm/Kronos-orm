/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.inspect.SqlParameterCollector
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.validate.SqlValidationDiagnostic

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

fun SqlStatement.toSql(context: SqlRenderContext): String =
    toRenderedSql(context).sql

fun SqlStatement.toRenderedSql(context: SqlRenderContext = SqlRenderContext()): RenderedSql {
    val lowered = context.lowerer.lowerStatement(this, context.dialect)
    val plan = context.planner.plan(lowered, context.dialect)
    val diagnostics = plan.diagnostics + plan.statements.flatMap { context.validator.validate(it, context.dialect) }
    throwOnDialectErrors(context, diagnostics)
    if (plan.statements.size != 1) {
        throw MultiStatementSqlPlanException(plan.statements.size)
    }
    val statement = plan.statements.single()
    return renderedSql(
        sql = sqlRenderer(context.dialect).renderStatement(statement),
        context = context,
        astParameterNames = SqlParameterCollector.collectNamedParameters(statement)
    )
}

@ExperimentalKronosSyntaxApi
fun SqlStatement.toRenderedSqlBatch(context: SqlRenderContext = SqlRenderContext()): RenderedSqlBatch {
    val lowered = context.lowerer.lowerStatement(this, context.dialect)
    val plan = context.planner.plan(lowered, context.dialect)
    val diagnostics = plan.diagnostics + plan.statements.flatMap { context.validator.validate(it, context.dialect) }
    throwOnDialectErrors(context, diagnostics)
    val renderer = sqlRenderer(context.dialect)
    return RenderedSqlBatch(
        statements = plan.statements.map { statement ->
            renderedSql(
                sql = renderer.renderStatement(statement),
                context = context,
                astParameterNames = SqlParameterCollector.collectNamedParameters(statement)
            )
        },
        diagnostics = diagnostics
    )
}

fun SqlQuery.toSql(dialect: SqlDialect = SqlDialect.Standard): String =
    sqlRenderer(dialect).renderQuery(this)

fun SqlQuery.toSql(context: SqlRenderContext): String =
    toRenderedSql(context).sql

fun SqlQuery.toRenderedSql(context: SqlRenderContext = SqlRenderContext()): RenderedSql {
    val lowered = context.lowerer.lowerQuery(this, context.dialect)
    val plan = context.planner.plan(lowered, context.dialect)
    val diagnostics = plan.diagnostics + plan.statements.flatMap { context.validator.validate(it, context.dialect) }
    throwOnDialectErrors(context, diagnostics)
    if (plan.statements.size != 1) {
        throw MultiStatementSqlPlanException(plan.statements.size)
    }
    val statement = plan.statements.single()
    return renderedSql(
        sql = sqlRenderer(context.dialect).renderStatement(statement),
        context = context,
        astParameterNames = SqlParameterCollector.collectNamedParameters(statement)
    )
}

fun SqlExpr.toSql(dialect: SqlDialect = SqlDialect.Standard): String =
    sqlRenderer(dialect).renderExpr(this)

fun SqlExpr.toSql(context: SqlRenderContext): String =
    toRenderedSql(context).sql

fun SqlExpr.toRenderedSql(context: SqlRenderContext = SqlRenderContext()): RenderedSql {
    val lowered = context.lowerer.lowerExpr(this, context.dialect)
    val diagnostics = context.validator.validate(lowered, context.dialect)
    throwOnDialectErrors(context, diagnostics)
    return renderedSql(
        sql = sqlRenderer(context.dialect).renderExpr(lowered),
        context = context,
        astParameterNames = SqlParameterCollector.collectNamedParameters(lowered)
    )
}

fun SqlTable.toSql(dialect: SqlDialect = SqlDialect.Standard): String =
    sqlRenderer(dialect).renderTable(this)

fun SqlTable.toSql(context: SqlRenderContext): String =
    toRenderedSql(context).sql

fun SqlTable.toRenderedSql(context: SqlRenderContext = SqlRenderContext()): RenderedSql {
    val lowered = context.lowerer.lowerTable(this, context.dialect)
    val diagnostics = context.validator.validate(lowered, context.dialect)
    throwOnDialectErrors(context, diagnostics)
    return renderedSql(
        sql = sqlRenderer(context.dialect).renderTable(lowered),
        context = context,
        astParameterNames = SqlParameterCollector.collectNamedParameters(lowered)
    )
}

data class RenderedSqlBatch(
    val statements: List<RenderedSql>,
    val diagnostics: List<SqlValidationDiagnostic> = emptyList()
)

class MultiStatementSqlPlanException(
    val statementCount: Int
) : IllegalStateException("SQL plan produced $statementCount statements. Use toRenderedSqlBatch() for multi-statement plans.")
