/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.expr.SqlTimeType
import com.kotlinorm.syntax.inspect.SqlParameterCollector
import com.kotlinorm.syntax.inspect.SqlNodeRewriter
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.validate.SqlValidationDiagnostic
import java.math.BigDecimal
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

interface SqlRenderer {
    fun renderStatement(statement: SqlStatement): String
    fun renderQuery(query: SqlQuery): String
    fun renderExpr(expr: SqlExpr): String
    fun renderPredicate(expr: SqlExpr): String
    fun renderTable(table: SqlTable): String
}

interface SqlPrinter : SqlRenderer

fun sqlRenderer(dialect: SqlDialect = SqlDialect.Standard): SqlRenderer = when (dialect.family) {
    SqlDialectFamily.Standard -> StandardSqlRenderer(dialect)
    SqlDialectFamily.MySql -> MysqlSqlRenderer(standardEscapeStrings = dialect.standardEscapeStrings)
    SqlDialectFamily.PostgreSql -> PostgresqlSqlRenderer()
    SqlDialectFamily.SQLite -> SqliteSqlRenderer()
    SqlDialectFamily.H2 -> H2SqlRenderer()
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
    val statement = plan.statements.single().prepareForRendering(context)
    return renderedSql(
        sql = sqlRenderer(context.dialect).renderStatement(statement),
        context = context,
        astParameterNames = SqlParameterCollector.collectNamedParameters(statement),
        listParameterOccurrences = SqlParameterCollector.collectListExpansionOccurrences(statement)
    )
}

private fun SqlStatement.prepareForRendering(context: SqlRenderContext): SqlStatement {
    if (context.dialect.family != SqlDialectFamily.Oracle || this !is SqlDdlStatement.CreateTableAsSelect) {
        return this
    }
    val inliner = object : SqlNodeRewriter {
        override fun rewriteExpr(expr: SqlExpr): SqlExpr {
            val parameter = expr as? SqlExpr.Parameter ?: return super.rewriteExpr(expr)
            val name = (parameter.parameter as? SqlParameter.Named)?.name ?: return parameter
            require(context.parameterValues.containsKey(name)) {
                "Oracle CREATE TABLE AS SELECT parameter '$name' must be bound before rendering."
            }
            require(!parameter.expandAsList) {
                "Oracle CREATE TABLE AS SELECT does not support list parameter '$name'."
            }
            return context.parameterValues[name].toOracleDdlLiteral(name)
        }
    }
    return copy(query = inliner.rewriteQuery(query))
}

private fun Any?.toOracleDdlLiteral(parameterName: String): SqlExpr = when (this) {
    null -> SqlExpr.NullLiteral
    is String -> SqlExpr.StringLiteral(this)
    is Char -> SqlExpr.StringLiteral(toString())
    is Boolean -> SqlExpr.NumberLiteral(if (this) "1" else "0")
    is BigDecimal -> SqlExpr.NumberLiteral(toPlainString())
    is Number -> SqlExpr.NumberLiteral(toString())
    is Enum<*> -> SqlExpr.StringLiteral(name)
    is UUID -> SqlExpr.StringLiteral(toString())
    is LocalDate -> SqlExpr.TimeLiteral(SqlTimeType.Date, toString())
    is java.sql.Date -> SqlExpr.TimeLiteral(SqlTimeType.Date, toLocalDate().toString())
    is LocalTime -> SqlExpr.TimeLiteral(SqlTimeType.Time(), toString())
    is Time -> SqlExpr.TimeLiteral(SqlTimeType.Time(), toLocalTime().toString())
    is LocalDateTime -> SqlExpr.TimeLiteral(SqlTimeType.Timestamp(), formatOracleTimestamp())
    is Timestamp -> SqlExpr.TimeLiteral(SqlTimeType.Timestamp(), toLocalDateTime().formatOracleTimestamp())
    is OffsetDateTime -> SqlExpr.TimeLiteral(
        SqlTimeType.Timestamp(com.kotlinorm.syntax.expr.SqlTimeZoneMode.WithTimeZone),
        format(oracleOffsetTimestampFormatter)
    )
    is ZonedDateTime -> toOffsetDateTime().toOracleDdlLiteral(parameterName)
    is java.time.Instant -> atOffset(ZoneOffset.UTC).toOracleDdlLiteral(parameterName)
    is ByteArray -> SqlExpr.Function(
        name = SqlIdentifier.of("HEXTORAW"),
        args = listOf(SqlExpr.StringLiteral(joinToString("") { byte -> "%02x".format(byte) }))
    )
    else -> throw IllegalArgumentException(
        "Oracle CREATE TABLE AS SELECT parameter '$parameterName' has unsupported literal type ${this::class.qualifiedName}."
    )
}

private fun LocalDateTime.formatOracleTimestamp(): String =
    format(oracleLocalTimestampFormatter)

private val oracleLocalTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS")

private val oracleOffsetTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS xxx")

@ExperimentalKronosSyntaxApi
fun SqlStatement.toRenderedSqlBatch(context: SqlRenderContext = SqlRenderContext()): RenderedSqlBatch {
    val lowered = context.lowerer.lowerStatement(this, context.dialect)
    val plan = context.planner.plan(lowered, context.dialect)
    val diagnostics = plan.diagnostics + plan.statements.flatMap { context.validator.validate(it, context.dialect) }
    throwOnDialectErrors(context, diagnostics)
    val renderer = sqlRenderer(context.dialect)
    return RenderedSqlBatch(
        statements = plan.statements.map { plannedStatement ->
            val statement = plannedStatement.prepareForRendering(context)
            renderedSql(
                sql = renderer.renderStatement(statement),
                context = context,
                astParameterNames = SqlParameterCollector.collectNamedParameters(statement),
                listParameterOccurrences = SqlParameterCollector.collectListExpansionOccurrences(statement)
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
        astParameterNames = SqlParameterCollector.collectNamedParameters(statement),
        listParameterOccurrences = SqlParameterCollector.collectListExpansionOccurrences(statement)
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
        astParameterNames = SqlParameterCollector.collectNamedParameters(lowered),
        listParameterOccurrences = SqlParameterCollector.collectListExpansionOccurrences(lowered)
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
        astParameterNames = SqlParameterCollector.collectNamedParameters(lowered),
        listParameterOccurrences = SqlParameterCollector.collectListExpansionOccurrences(lowered)
    )
}

data class RenderedSqlBatch(
    val statements: List<RenderedSql>,
    val diagnostics: List<SqlValidationDiagnostic> = emptyList()
)

class MultiStatementSqlPlanException(
    val statementCount: Int
) : IllegalStateException("SQL plan produced $statementCount statements. Use toRenderedSqlBatch() for multi-statement plans.")
