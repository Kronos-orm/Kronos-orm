/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.*
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.limit.SqlFetch
import com.kotlinorm.syntax.limit.SqlFetchMode
import com.kotlinorm.syntax.limit.SqlFetchUnit
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlNullsOrdering
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.*
import com.kotlinorm.syntax.table.*
import com.kotlinorm.syntax.token.SqlUnsafeToken

open class StandardSqlRenderer(
    protected val dialect: SqlDialect = SqlDialect.Standard
) : SqlPrinter {
    override fun renderStatement(statement: SqlStatement): String = when (statement) {
        is SqlQuery -> renderQuery(statement)
        is SqlDmlStatement -> renderDml(statement)
        is SqlDdlStatement -> renderDdl(statement)
    }

    override fun renderQuery(query: SqlQuery): String {
        val sql = when (query) {
            is SqlQuery.Select -> renderSelect(query)
            is SqlQuery.Set -> renderSetQuery(query)
            is SqlQuery.Values -> renderValuesQuery(query)
            is SqlQuery.With -> renderWithQuery(query)
        }
        return query.lock?.let { "$sql ${renderLock(it)}" } ?: sql
    }

    override fun renderExpr(expr: SqlExpr): String = when (expr) {
        is SqlExpr.Column -> renderColumn(expr)
        SqlExpr.NullLiteral -> "NULL"
        is SqlExpr.StringLiteral -> quoteString(expr.string)
        is SqlExpr.NumberLiteral -> expr.number
        is SqlExpr.BooleanLiteral -> if (expr.boolean) "TRUE" else "FALSE"
        is SqlExpr.TimeLiteral -> "${renderTimeType(expr.type)} ${quoteString(expr.time)}"
        is SqlExpr.IntervalLiteral -> "INTERVAL ${quoteString(expr.value)} ${renderIntervalField(expr.field)}"
        is SqlExpr.Tuple -> expr.items.joinToString(", ", "(", ")") { renderExpr(it) }
        is SqlExpr.Array -> expr.items.joinToString(", ", "ARRAY[", "]") { renderExpr(it) }
        is SqlExpr.Parameter -> renderParameter(expr.parameter)
        is SqlExpr.Unary -> renderUnary(expr)
        is SqlExpr.Binary -> renderBinary(expr)
        is SqlExpr.JsonTest -> renderJsonTest(expr)
        is SqlExpr.In -> renderIn(expr)
        is SqlExpr.Between -> renderBetween(expr)
        is SqlExpr.Like -> renderLike(expr)
        is SqlExpr.SimilarTo -> renderSimilarTo(expr)
        is SqlExpr.Case -> renderCase(expr)
        is SqlExpr.SimpleCase -> renderSimpleCase(expr)
        is SqlExpr.Coalesce -> expr.items.joinToString(", ", "COALESCE(", ")") { renderExpr(it) }
        is SqlExpr.NullIf -> "NULLIF(${renderExpr(expr.expr)}, ${renderExpr(expr.test)})"
        is SqlExpr.Cast -> "CAST(${renderExpr(expr.expr)} AS ${renderType(expr.type)})"
        is SqlExpr.Window -> "${renderExpr(expr.expr)} OVER (${renderWindow(expr.window)})"
        is SqlExpr.Subquery -> "(${renderQuery(expr.query)})"
        is SqlExpr.ExistsPredicate -> "${if (expr.withNot) "NOT " else ""}EXISTS (${renderQuery(expr.query)})"
        is SqlExpr.QuantifiedComparisonPredicate -> {
            "${renderExpr(expr.expr)} ${renderQuantifiedComparisonOperator(expr.operator)} ${expr.quantifier.name.uppercase()} (${renderQuery(expr.query)})"
        }
        is SqlExpr.Grouping -> expr.items.joinToString(", ", "GROUPING(", ")") { renderExpr(it) }
        is SqlExpr.IdentFunc -> safeUnquotedIdent(expr.name)
        is SqlExpr.SubstringFunc -> renderSubstring(expr)
        is SqlExpr.TrimFunc -> renderTrim(expr)
        is SqlExpr.OverlayFunc -> renderOverlay(expr)
        is SqlExpr.PositionFunc -> "POSITION(${renderExpr(expr.expr)} IN ${renderExpr(expr.inExpr)})"
        is SqlExpr.ExtractFunc -> "EXTRACT(${expr.unit.name.uppercase()} FROM ${renderExpr(expr.expr)})"
        is SqlExpr.JsonSerializeFunc -> renderJsonSerialize(expr)
        is SqlExpr.JsonParseFunc -> renderJsonParse(expr)
        is SqlExpr.JsonQueryFunc -> renderJsonQuery(expr)
        is SqlExpr.JsonValueFunc -> renderJsonValue(expr)
        is SqlExpr.JsonObjectFunc -> renderJsonObject(expr)
        is SqlExpr.JsonArrayFunc -> renderJsonArray(expr)
        is SqlExpr.JsonExistsFunc -> renderJsonExists(expr)
        is SqlExpr.CountAsteriskFunc -> renderCountAsterisk(expr)
        is SqlExpr.JsonObjectAggFunc -> renderJsonObjectAgg(expr)
        is SqlExpr.JsonArrayAggFunc -> renderJsonArrayAgg(expr)
        is SqlExpr.ListAggFunc -> renderListAgg(expr)
        is SqlExpr.NullsTreatmentFunc -> renderNullsTreatmentFunc(expr)
        is SqlExpr.NthValueFunc -> renderNthValue(expr)
        is SqlExpr.GeneralFunc -> renderGeneralFunc(expr)
        is SqlExpr.MatchPhase -> "${expr.phase.name.uppercase()} ${renderExpr(expr.expr)}"
        is SqlExpr.UnsafeCustom -> expr.tokens.joinToString(" ", "(", ")") { renderUnsafeToken(it) }
    }

    override fun renderTable(table: SqlTable): String = when (table) {
        is SqlTable.Ident -> renderIdentTable(table)
        is SqlTable.Func -> renderFuncTable(table)
        is SqlTable.Subquery -> renderSubqueryTable(table)
        is SqlTable.Join -> renderJoinTable(table)
        is SqlTable.Json -> renderJsonTable(table)
        is SqlTable.Graph -> renderGraphTable(table)
    }

    private fun renderSelect(query: SqlQuery.Select): String = buildString {
        append("SELECT")
        query.quantifier?.let { append(" ${renderQuantifier(it)}") }
        append(" ")
        append(if (query.select.isEmpty()) "*" else query.select.joinToString(", ") { renderSelectItem(it) })
        if (query.from.isNotEmpty()) append(query.from.joinToString(", ", " FROM ") { renderTable(it) })
        query.where?.let { append(" WHERE ${renderExpr(it)}") }
        query.groupBy?.let { append(" ${renderGroup(it)}") }
        query.having?.let { append(" HAVING ${renderExpr(it)}") }
        if (query.orderBy.isNotEmpty()) {
            append(query.orderBy.joinToString(", ", " ORDER BY ") { renderOrderingItem(it) })
        }
        query.limit?.let { append(" ${renderLimit(it)}") }
    }

    private fun renderSetQuery(query: SqlQuery.Set): String = buildString {
        append("(${renderQuery(query.left)}) ${renderSetOperator(query.operator)} (${renderQuery(query.right)})")
        if (query.orderBy.isNotEmpty()) {
            append(query.orderBy.joinToString(", ", " ORDER BY ") { renderOrderingItem(it) })
        }
        query.limit?.let { append(" ${renderLimit(it)}") }
    }

    protected open fun renderValuesQuery(query: SqlQuery.Values): String =
        query.values.joinToString(", ", "VALUES ") { row ->
            row.joinToString(", ", "(", ")") { renderExpr(it) }
        }

    private fun renderWithQuery(query: SqlQuery.With): String = buildString {
        append("WITH")
        if (query.withRecursive) append(" RECURSIVE")
        append(query.withItems.joinToString(", ", " ") { renderWithItem(it) })
        append(" ${renderQuery(query.query)}")
    }

    protected open fun renderDml(statement: SqlDmlStatement): String = when (statement) {
        is SqlDmlStatement.Delete -> buildString {
            append("DELETE FROM ${renderTable(statement.table)}")
            statement.where?.let { append(" WHERE ${renderExpr(it)}") }
        }
        is SqlDmlStatement.Insert -> buildString {
            append("INSERT INTO ${renderTable(statement.table)}")
            if (statement.columns.isNotEmpty()) {
                append(statement.columns.joinToString(", ", " (", ")") { quoteIdent(it) })
            }
            append(" ${renderInsertMode(statement.mode)}")
        }
        is SqlDmlStatement.Update -> buildString {
            append("UPDATE ${renderTable(statement.table)} SET ")
            append(statement.setPairs.joinToString(", ") { "${quoteIdent(it.column)} = ${renderExpr(it.value)}" })
            statement.where?.let { append(" WHERE ${renderExpr(it)}") }
        }
        is SqlDmlStatement.Truncate -> "TRUNCATE TABLE ${renderTable(statement.table)}"
        is SqlDmlStatement.Upsert -> renderUpsert(statement)
    }

    private fun renderDdl(statement: SqlDdlStatement): String = when (statement) {
        is SqlDdlStatement.CreateTable -> buildString {
            append("CREATE TABLE ")
            if (statement.ifNotExists) append("IF NOT EXISTS ")
            append(quoteIdent(statement.tableName))
            append(" (")
            val definitions = statement.columns.map { renderColumnDefinition(it) } +
                statement.indexes.map { renderIndexDefinition(it) }
            append(definitions.joinToString(", "))
            append(")")
            statement.comment?.let { append(" COMMENT ${quoteString(it)}") }
        }
        is SqlDdlStatement.CreateTableAsSelect -> {
            "CREATE TABLE ${if (statement.ifNotExists) "IF NOT EXISTS " else ""}${quoteIdent(statement.tableName)} AS ${renderQuery(statement.query)}"
        }
        is SqlDdlStatement.AlterTable.AddColumn -> {
            "ALTER TABLE ${quoteIdent(statement.tableName)} ADD COLUMN ${renderColumnDefinition(statement.column)}"
        }
        is SqlDdlStatement.AlterTable.DropColumn -> {
            "ALTER TABLE ${quoteIdent(statement.tableName)} DROP COLUMN ${quoteIdent(statement.columnName)}"
        }
        is SqlDdlStatement.AlterTable.ModifyColumn -> {
            "ALTER TABLE ${quoteIdent(statement.tableName)} ALTER COLUMN ${renderColumnDefinition(statement.column)}"
        }
        is SqlDdlStatement.DropTable -> {
            "DROP TABLE ${if (statement.ifExists) "IF EXISTS " else ""}${quoteIdent(statement.tableName)}"
        }
        is SqlDdlStatement.CreateIndex -> {
            "${if (statement.unique) "CREATE UNIQUE INDEX" else "CREATE INDEX"} ${quoteIdent(statement.indexName)} ON ${quoteIdent(statement.tableName)} (${statement.columns.joinToString(", ") { quoteIdent(it) }})"
        }
        is SqlDdlStatement.DropIndex -> "DROP INDEX ${quoteIdent(statement.indexName)} ON ${quoteIdent(statement.tableName)}"
    }

    private fun renderInsertMode(mode: SqlInsertMode): String = when (mode) {
        is SqlInsertMode.Values -> mode.values.joinToString(", ", "VALUES ") { row ->
            row.joinToString(", ", "(", ")") { renderExpr(it) }
        }
        is SqlInsertMode.Subquery -> renderQuery(mode.query)
    }

    protected open fun renderUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append("MERGE INTO ")
        append(renderTable(statement.table.copy(alias = SqlTableAlias("t1"))))
        append(" USING (SELECT ")
        append(statement.values.zip(statement.columns).joinToString(", ") { (value, column) ->
            "${renderExpr(value)} AS ${quoteIdent(column)}"
        })
        append(") ${renderTableAlias(SqlTableAlias("t2"))}")
        append(" ON (")
        append(statement.primaryKeys.joinToString(" AND ") {
            "${quoteIdent("t1")}.${quoteIdent(it)} = ${quoteIdent("t2")}.${quoteIdent(it)}"
        })
        append(") WHEN MATCHED THEN UPDATE SET ")
        append(statement.updateColumns.joinToString(", ") {
            "${quoteIdent("t1")}.${quoteIdent(it)} = ${quoteIdent("t2")}.${quoteIdent(it)}"
        })
        append(" WHEN NOT MATCHED THEN INSERT (")
        append(statement.columns.joinToString(", ") { quoteIdent(it) })
        append(") VALUES (")
        append(statement.values.joinToString(", ") { renderExpr(it) })
        append(")")
    }

    private fun renderColumn(expr: SqlExpr.Column): String =
        listOfNotNull(expr.tableName, expr.columnName).joinToString(".") { quoteIdent(it) }

    private fun renderUnary(expr: SqlExpr.Unary): String = when (expr.operator) {
        SqlUnaryOperator.Positive -> "+${renderExpr(expr.expr)}"
        SqlUnaryOperator.Negative -> "-${renderExpr(expr.expr)}"
        SqlUnaryOperator.Not -> "NOT(${renderExpr(expr.expr)})"
        SqlUnaryOperator.BitwiseNot -> "~${renderExpr(expr.expr)}"
        is SqlUnaryOperator.UnsafeCustom -> "${renderUnaryOperator(expr.operator)}(${renderExpr(expr.expr)})"
    }

    protected open fun renderBinary(expr: SqlExpr.Binary): String {
        val left = renderBinaryOperand(expr.left, expr.operator, isRight = false)
        val right = renderBinaryOperand(expr.right, expr.operator, isRight = true)
        return "$left ${renderBinaryOperator(expr.operator)} $right"
    }

    private fun renderBinaryOperand(child: SqlExpr, parent: SqlBinaryOperator, isRight: Boolean): String {
        val rendered = renderExpr(child)
        val childPrecedence = exprPrecedence(child) ?: return rendered
        val needsParens = if (isRight) childPrecedence <= parent.precedence else childPrecedence < parent.precedence
        return if (needsParens) "($rendered)" else rendered
    }

    private fun exprPrecedence(expr: SqlExpr): Int? = when (expr) {
        is SqlExpr.Binary -> expr.operator.precedence
        is SqlExpr.In,
        is SqlExpr.Between,
        is SqlExpr.Like,
        is SqlExpr.SimilarTo,
        is SqlExpr.QuantifiedComparisonPredicate -> SqlBinaryOperator.Equal.precedence
        else -> null
    }

    private fun renderJsonTest(expr: SqlExpr.JsonTest): String = buildString {
        append(renderExpr(expr.expr))
        append(" IS ")
        if (expr.withNot) append("NOT ")
        append("JSON")
        expr.nodeType?.let { append(" ${it.name.uppercase()}") }
        expr.uniquenessMode?.let { append(" ${renderJsonUniquenessMode(it)}") }
    }

    private fun renderIn(expr: SqlExpr.In): String {
        val right = when (val operand = expr.`in`) {
            is SqlInRightOperand.Values -> operand.items.joinToString(", ", "(", ")") { renderExpr(it) }
            is SqlInRightOperand.Subquery -> "(${renderQuery(operand.query)})"
        }
        return "${renderExpr(expr.expr)} ${if (expr.withNot) "NOT " else ""}IN $right"
    }

    private fun renderBetween(expr: SqlExpr.Between): String =
        "${renderExpr(expr.expr)} ${if (expr.withNot) "NOT " else ""}BETWEEN ${renderExpr(expr.start)} AND ${renderExpr(expr.end)}"

    private fun renderLike(expr: SqlExpr.Like): String = buildString {
        append("${renderExpr(expr.expr)} ")
        if (expr.withNot) append("NOT ")
        append(if (expr.caseInsensitive) "ILIKE" else "LIKE")
        append(" ${renderExpr(expr.pattern)}")
        expr.escape?.let { append(" ESCAPE ${renderExpr(it)}") }
    }

    private fun renderSimilarTo(expr: SqlExpr.SimilarTo): String = buildString {
        append("${renderExpr(expr.expr)} ")
        if (expr.withNot) append("NOT ")
        append("SIMILAR TO ${renderExpr(expr.pattern)}")
        expr.escape?.let { append(" ESCAPE ${renderExpr(it)}") }
    }

    private fun renderCase(expr: SqlExpr.Case): String = buildString {
        append("CASE ")
        append(expr.branches.joinToString(" ") { "WHEN ${renderExpr(it.`when`)} THEN ${renderExpr(it.then)}" })
        expr.default?.let { append(" ELSE ${renderExpr(it)}") }
        append(" END")
    }

    private fun renderSimpleCase(expr: SqlExpr.SimpleCase): String = buildString {
        append("CASE ${renderExpr(expr.expr)} ")
        append(expr.branches.joinToString(" ") { "WHEN ${renderExpr(it.`when`)} THEN ${renderExpr(it.then)}" })
        expr.default?.let { append(" ELSE ${renderExpr(it)}") }
        append(" END")
    }

    private fun renderSubstring(expr: SqlExpr.SubstringFunc): String = buildString {
        append("SUBSTRING(${renderExpr(expr.expr)} FROM ${renderExpr(expr.from)}")
        expr.`for`?.let { append(" FOR ${renderExpr(it)}") }
        append(")")
    }

    private fun renderTrim(expr: SqlExpr.TrimFunc): String = buildString {
        append("TRIM(")
        expr.trim?.let { trim ->
            trim.mode?.let { append("${it.name.uppercase()} ") }
            trim.value?.let { append("${renderExpr(it)} FROM ") }
        }
        append(renderExpr(expr.expr))
        append(")")
    }

    private fun renderOverlay(expr: SqlExpr.OverlayFunc): String = buildString {
        append("OVERLAY(${renderExpr(expr.expr)} PLACING ${renderExpr(expr.placing)} FROM ${renderExpr(expr.from)}")
        expr.`for`?.let { append(" FOR ${renderExpr(it)}") }
        append(")")
    }

    private fun renderJsonSerialize(expr: SqlExpr.JsonSerializeFunc): String = buildString {
        append("JSON_SERIALIZE(${renderExpr(expr.expr)}")
        expr.output?.let { append(" ${renderJsonOutput(it)}") }
        append(")")
    }

    private fun renderJsonParse(expr: SqlExpr.JsonParseFunc): String = buildString {
        append("JSON(${renderExpr(expr.expr)}")
        expr.input?.let { append(" ${renderJsonInput(it)}") }
        expr.uniquenessMode?.let { append(" ${renderJsonUniquenessMode(it)}") }
        append(")")
    }

    private fun renderJsonQuery(expr: SqlExpr.JsonQueryFunc): String = buildString {
        append("JSON_QUERY(${renderExpr(expr.expr)}, ${renderExpr(expr.path)}")
        if (expr.passingItems.isNotEmpty()) {
            append(expr.passingItems.joinToString(", ", " PASSING ") { renderJsonPassing(it) })
        }
        expr.output?.let { append(" ${renderJsonOutput(it)}") }
        expr.wrapper?.let { append(" ${renderJsonQueryWrapperBehavior(it)}") }
        expr.quotes?.let { append(" ${renderJsonQueryQuotesBehavior(it)}") }
        expr.onEmpty?.let { append(" ${renderJsonQueryEmptyBehavior(it)}") }
        expr.onError?.let { append(" ${renderJsonQueryErrorBehavior(it)}") }
        append(")")
    }

    private fun renderJsonValue(expr: SqlExpr.JsonValueFunc): String = buildString {
        append("JSON_VALUE(${renderExpr(expr.expr)}, ${renderExpr(expr.path)}")
        if (expr.passingItems.isNotEmpty()) {
            append(expr.passingItems.joinToString(", ", " PASSING ") { renderJsonPassing(it) })
        }
        expr.output?.let { append(" ${renderJsonOutput(it)}") }
        expr.onEmpty?.let { append(" ${renderJsonValueEmptyBehavior(it)}") }
        expr.onError?.let { append(" ${renderJsonValueErrorBehavior(it)}") }
        append(")")
    }

    private fun renderJsonObject(expr: SqlExpr.JsonObjectFunc): String = buildString {
        append("JSON_OBJECT(")
        append(expr.items.joinToString(", ") { renderJsonObjectItem(it) })
        expr.nullConstructor?.let { appendSeparatedOption(this, expr.items.isNotEmpty(), renderJsonNullConstructor(it)) }
        expr.uniquenessMode?.let { appendSeparatedOption(this, isNotEmptyAfterOpen("JSON_OBJECT("), renderJsonUniquenessMode(it)) }
        expr.output?.let { appendSeparatedOption(this, isNotEmptyAfterOpen("JSON_OBJECT("), renderJsonOutput(it)) }
        append(")")
    }

    private fun renderJsonArray(expr: SqlExpr.JsonArrayFunc): String = buildString {
        append("JSON_ARRAY(")
        append(expr.items.joinToString(", ") { renderJsonArrayItem(it) })
        expr.nullConstructor?.let { appendSeparatedOption(this, expr.items.isNotEmpty(), renderJsonNullConstructor(it)) }
        expr.output?.let { appendSeparatedOption(this, isNotEmptyAfterOpen("JSON_ARRAY("), renderJsonOutput(it)) }
        append(")")
    }

    private fun renderJsonExists(expr: SqlExpr.JsonExistsFunc): String = buildString {
        append("JSON_EXISTS(${renderExpr(expr.expr)}, ${renderExpr(expr.path)}")
        if (expr.passingItems.isNotEmpty()) {
            append(expr.passingItems.joinToString(", ", " PASSING ") { renderJsonPassing(it) })
        }
        expr.onError?.let { append(" ${renderJsonExistsErrorBehavior(it)}") }
        append(")")
    }

    private fun renderCountAsterisk(expr: SqlExpr.CountAsteriskFunc): String = buildString {
        append("COUNT(")
        expr.tableName?.let { append("${quoteIdent(it)}.") }
        append("*)")
        expr.filter?.let { append(" ${renderFilter(it)}") }
    }

    private fun renderJsonObjectAgg(expr: SqlExpr.JsonObjectAggFunc): String = buildString {
        append("JSON_OBJECTAGG(${renderJsonObjectItem(expr.item)}")
        expr.nullConstructor?.let { append(" ${renderJsonNullConstructor(it)}") }
        expr.uniquenessMode?.let { append(" ${renderJsonUniquenessMode(it)}") }
        expr.output?.let { append(" ${renderJsonOutput(it)}") }
        append(")")
        expr.filter?.let { append(" ${renderFilter(it)}") }
    }

    private fun renderJsonArrayAgg(expr: SqlExpr.JsonArrayAggFunc): String = buildString {
        append("JSON_ARRAYAGG(${renderJsonArrayItem(expr.item)}")
        if (expr.orderBy.isNotEmpty()) {
            append(expr.orderBy.joinToString(", ", " ORDER BY ") { renderOrderingItem(it) })
        }
        expr.nullConstructor?.let { append(" ${renderJsonNullConstructor(it)}") }
        expr.output?.let { append(" ${renderJsonOutput(it)}") }
        append(")")
        expr.filter?.let { append(" ${renderFilter(it)}") }
    }

    protected open fun renderListAgg(expr: SqlExpr.ListAggFunc): String = buildString {
        append("LISTAGG(")
        expr.quantifier?.let { append("${renderQuantifier(it)} ") }
        append("${renderExpr(expr.expr)}, ${renderExpr(expr.separator)}")
        expr.onOverflow?.let { append(" ${renderListAggOnOverflow(it)}") }
        append(")")
        if (expr.withinGroup.isNotEmpty()) {
            append(expr.withinGroup.joinToString(", ", " WITHIN GROUP (ORDER BY ", ")") { renderOrderingItem(it) })
        }
        expr.filter?.let { append(" ${renderFilter(it)}") }
    }

    private fun renderNullsTreatmentFunc(expr: SqlExpr.NullsTreatmentFunc): String = buildString {
        append(safeUnquotedIdent(expr.name))
        append(expr.args.joinToString(", ", "(", ")") { renderExpr(it) })
        expr.nullsMode?.let { append(" ${renderWindowNullsMode(it)}") }
    }

    private fun renderNthValue(expr: SqlExpr.NthValueFunc): String = buildString {
        append("NTH_VALUE(${renderExpr(expr.expr)}, ${renderExpr(expr.row)})")
        expr.fromMode?.let { append(" FROM ${it.name.uppercase()}") }
        expr.nullsMode?.let { append(" ${renderWindowNullsMode(it)}") }
    }

    private fun renderGeneralFunc(expr: SqlExpr.GeneralFunc): String = buildString {
        append(safeUnquotedIdent(expr.name))
        append("(")
        expr.quantifier?.let {
            append(renderQuantifier(it))
            if (expr.args.isNotEmpty()) append(" ")
        }
        append(expr.args.joinToString(", ") { renderExpr(it) })
        if (expr.orderBy.isNotEmpty()) {
            append(expr.orderBy.joinToString(", ", " ORDER BY ") { renderOrderingItem(it) })
        }
        append(")")
        if (expr.withinGroup.isNotEmpty()) {
            append(expr.withinGroup.joinToString(", ", " WITHIN GROUP (ORDER BY ", ")") { renderOrderingItem(it) })
        }
        expr.filter?.let { append(" ${renderFilter(it)}") }
    }

    private fun StringBuilder.isNotEmptyAfterOpen(prefix: String): Boolean = length > prefix.length

    private fun appendSeparatedOption(builder: StringBuilder, hasPrevious: Boolean, option: String) {
        if (hasPrevious) builder.append(" ") else Unit
        builder.append(option)
    }

    private fun renderWindow(window: SqlWindow): String = listOfNotNull(
        window.partitionBy.takeIf { it.isNotEmpty() }?.joinToString(", ", "PARTITION BY ") { renderExpr(it) },
        window.orderBy.takeIf { it.isNotEmpty() }?.joinToString(", ", "ORDER BY ") { renderOrderingItem(it) },
        window.frame?.let { renderWindowFrame(it) }
    ).joinToString(" ")

    private fun renderWindowFrame(frame: SqlWindowFrame): String = when (frame) {
        is SqlWindowFrame.Start -> buildString {
            append("${frame.unit.name.uppercase()} ${renderFrameBound(frame.start)}")
            frame.excludeMode?.let { append(" EXCLUDE ${renderFrameExclude(it)}") }
        }
        is SqlWindowFrame.Between -> buildString {
            append("${frame.unit.name.uppercase()} BETWEEN ${renderFrameBound(frame.start)} AND ${renderFrameBound(frame.end)}")
            frame.excludeMode?.let { append(" EXCLUDE ${renderFrameExclude(it)}") }
        }
    }

    private fun renderFrameBound(bound: SqlWindowFrameBound): String = when (bound) {
        SqlWindowFrameBound.CurrentRow -> "CURRENT ROW"
        SqlWindowFrameBound.UnboundedPreceding -> "UNBOUNDED PRECEDING"
        is SqlWindowFrameBound.Preceding -> "${renderExpr(bound.n)} PRECEDING"
        SqlWindowFrameBound.UnboundedFollowing -> "UNBOUNDED FOLLOWING"
        is SqlWindowFrameBound.Following -> "${renderExpr(bound.n)} FOLLOWING"
    }

    private fun renderFrameExclude(mode: SqlWindowFrameExcludeMode): String = when (mode) {
        SqlWindowFrameExcludeMode.CurrentRow -> "CURRENT ROW"
        SqlWindowFrameExcludeMode.Group -> "GROUP"
        SqlWindowFrameExcludeMode.Ties -> "TIES"
        SqlWindowFrameExcludeMode.NoOthers -> "NO OTHERS"
    }

    private fun renderIdentTable(table: SqlTable.Ident): String = buildString {
        append(quoteIdent(table.name))
        table.periodForMode?.let { append(" ${renderTablePeriod(it)}") }
        table.alias?.let { append(" ${renderTableAlias(it)}") }
        table.matchRecognize?.let { append(" ${renderMatchRecognize(it)}") }
        table.sample?.let { append(" ${renderTableSample(it)}") }
    }

    private fun renderFuncTable(table: SqlTable.Func): String = buildString {
        if (table.withLateral) append("LATERAL ")
        append(safeUnquotedIdent(table.name))
        append(table.args.joinToString(", ", "(", ")") { renderExpr(it) })
        if (table.withOrdinality) append(" WITH ORDINALITY")
        table.alias?.let { append(" ${renderTableAlias(it)}") }
        table.matchRecognize?.let { append(" ${renderMatchRecognize(it)}") }
    }

    private fun renderSubqueryTable(table: SqlTable.Subquery): String = buildString {
        if (table.withLateral) append("LATERAL ")
        append("(${renderQuery(table.query)})")
        table.alias?.let { append(" ${renderTableAlias(it)}") }
        table.matchRecognize?.let { append(" ${renderMatchRecognize(it)}") }
    }

    private fun renderJoinTable(table: SqlTable.Join): String = buildString {
        append(renderTable(table.left))
        append(" ${renderJoinType(table.joinType)} JOIN ")
        append(if (table.right is SqlTable.Join) "(${renderTable(table.right)})" else renderTable(table.right))
        table.condition?.let { append(" ${renderJoinCondition(it)}") }
    }

    private fun renderJsonTable(table: SqlTable.Json): String = buildString {
        if (table.withLateral) append("LATERAL ")
        append("JSON_TABLE(${renderExpr(table.expr)}, ${renderExpr(table.path)}")
        table.pathAlias?.let { append(" AS ${quoteIdent(it)}") }
        if (table.passingItems.isNotEmpty()) {
            append(table.passingItems.joinToString(", ", " PASSING ") { renderJsonPassing(it) })
        }
        append(table.columns.joinToString(", ", " COLUMNS (", ")") { renderJsonColumn(it) })
        table.onError?.let { append(" ${renderJsonErrorBehavior(it)}") }
        append(")")
        table.alias?.let { append(" ${renderTableAlias(it)}") }
        table.matchRecognize?.let { append(" ${renderMatchRecognize(it)}") }
    }

    private fun renderGraphTable(table: SqlTable.Graph): String = buildString {
        if (table.withLateral) append("LATERAL ")
        append("GRAPH_TABLE(${quoteIdent(table.name)} MATCH")
        table.matchMode?.let { append(" ${renderGraphMatchMode(it)}") }
        append(" ")
        append(table.patterns.joinToString(", ") { renderGraphPattern(it) })
        table.where?.let { append(" WHERE ${renderExpr(it)}") }
        table.rowsMode?.let { append(" ${renderGraphRowsMode(it)}") }
        append(table.columns.joinToString(", ", " COLUMNS (", ")") { renderSelectItem(it) })
        table.exportMode?.let { append(" ${renderGraphExportMode(it)}") }
        append(")")
        table.alias?.let { append(" ${renderTableAlias(it)}") }
        table.matchRecognize?.let { append(" ${renderMatchRecognize(it)}") }
    }

    protected open fun renderSelectItem(item: SqlSelectItem): String = when (item) {
        is SqlSelectItem.Asterisk -> item.tableName?.let { "${quoteIdent(it)}.*" } ?: "*"
        is SqlSelectItem.Expr -> renderExpr(item.expr) + (item.alias?.let { " AS ${quoteIdent(it)}" } ?: "")
    }

    private fun renderWithItem(item: SqlWithItem): String = buildString {
        append(quoteIdent(item.name))
        if (item.columnNames.isNotEmpty()) {
            append(item.columnNames.joinToString(", ", " (", ")") { quoteIdent(it) })
        }
        append(" AS (${renderQuery(item.query)})")
    }

    protected open fun renderGroup(group: SqlGroup): String = buildString {
        append("GROUP BY")
        group.quantifier?.let { append(" ${renderQuantifier(it)}") }
        append(group.items.joinToString(", ", " ") { renderGroupingItem(it) })
    }

    protected open fun renderGroupingItem(item: SqlGroupingItem): String = when (item) {
        SqlGroupingItem.EmptyGroup -> "()"
        is SqlGroupingItem.Expr -> renderExpr(item.item)
        is SqlGroupingItem.Cube -> item.items.joinToString(", ", "CUBE(", ")") { renderExpr(it) }
        is SqlGroupingItem.Rollup -> item.items.joinToString(", ", "ROLLUP(", ")") { renderExpr(it) }
        is SqlGroupingItem.GroupingSets -> item.items.joinToString(", ", "GROUPING SETS(", ")") { renderGroupingItem(it) }
    }

    protected open fun renderOrderingItem(item: SqlOrderingItem): String = buildString {
        append(renderExpr(item.expr))
        append(" ")
        append((item.ordering ?: SqlOrdering.Asc).name.uppercase())
        item.nullsOrdering?.let {
            append(" NULLS ")
            append(when (it) {
                SqlNullsOrdering.First -> "FIRST"
                SqlNullsOrdering.Last -> "LAST"
            })
        }
    }

    protected open fun renderLimit(limit: SqlLimit): String = when (dialect.limitStyle) {
        SqlLimitStyle.Fetch -> buildString {
            limit.offset?.let { append("OFFSET ${renderExpr(it)} ROWS") }
            limit.fetch?.let {
                if (isNotEmpty()) append(" ")
                append(renderFetch(it))
            }
        }
        SqlLimitStyle.LimitOffset -> buildString {
            limit.fetch?.let { append("LIMIT ${renderExpr(it.limit)}") }
            limit.offset?.let {
                if (isEmpty()) append("LIMIT -1")
                append(" OFFSET ${renderExpr(it)}")
            }
        }
    }

    private fun renderFetch(fetch: SqlFetch): String = buildString {
        append("FETCH NEXT ${renderExpr(fetch.limit)} ")
        append(when (fetch.unit) {
            SqlFetchUnit.RowCount -> "ROWS"
            SqlFetchUnit.Percentage -> "PERCENT ROWS"
        })
        append(" ")
        append(when (fetch.mode) {
            SqlFetchMode.Only -> "ONLY"
            SqlFetchMode.WithTies -> "WITH TIES"
        })
    }

    protected open fun renderLock(lock: SqlLock): String = buildString {
        append(when (lock) {
            is SqlLock.Update -> "FOR UPDATE"
            is SqlLock.Share -> "FOR SHARE"
        })
        lock.waitMode?.let {
            append(" ")
            append(when (it) {
                SqlLockWaitMode.NoWait -> "NOWAIT"
                SqlLockWaitMode.SkipLocked -> "SKIP LOCKED"
            })
        }
    }

    protected open fun renderTableAlias(alias: SqlTableAlias): String = buildString {
        append("AS ${quoteIdent(alias.alias)}")
        if (alias.columnAliases.isNotEmpty()) {
            append(alias.columnAliases.joinToString(", ", " (", ")") { quoteIdent(it) })
        }
    }

    private fun renderTablePeriod(period: SqlTablePeriodForMode): String = when (period) {
        is SqlTablePeriodForMode.SystemTimeAsOf -> "FOR SYSTEM_TIME AS OF ${renderExpr(period.expr)}"
        is SqlTablePeriodForMode.SystemTimeBetween -> {
            val mode = period.mode?.name?.uppercase()?.let { "$it " } ?: ""
            "FOR SYSTEM_TIME BETWEEN $mode${renderExpr(period.start)} AND ${renderExpr(period.end)}"
        }
        is SqlTablePeriodForMode.SystemTimeFrom -> "FOR SYSTEM_TIME FROM ${renderExpr(period.from)} TO ${renderExpr(period.to)}"
    }

    private fun renderTableSample(sample: SqlTableSample): String = buildString {
        append("TABLESAMPLE ${sample.mode.name.uppercase()} (${renderExpr(sample.percentage)})")
        sample.repeatable?.let { append(" REPEATABLE (${renderExpr(it)})") }
    }

    protected open fun renderJoinType(type: SqlJoinType): String = when (type) {
        SqlJoinType.Inner -> "INNER"
        SqlJoinType.Left -> "LEFT"
        SqlJoinType.Right -> "RIGHT"
        SqlJoinType.Full -> "FULL"
        SqlJoinType.Cross -> "CROSS"
        is SqlJoinType.UnsafeCustom -> type.tokens.joinToString(" ") { renderUnsafeToken(it) }
    }

    protected open fun renderJoinCondition(condition: SqlJoinCondition): String = when (condition) {
        is SqlJoinCondition.On -> "ON ${renderExpr(condition.condition)}"
        is SqlJoinCondition.Using -> condition.columnNames.joinToString(", ", "USING (", ")") { quoteIdent(it) }
    }

    private fun renderJsonColumn(column: SqlJsonColumn): String = when (column) {
        is SqlJsonColumn.Ordinality -> "${quoteIdent(column.name)} FOR ORDINALITY"
        is SqlJsonColumn.Column -> buildString {
            append("${quoteIdent(column.name)} ${renderType(column.type)}")
            column.format?.let { append(" ${renderJsonOutputFormat(it)}") }
            column.path?.let { append(" PATH ${renderExpr(it)}") }
            column.wrapper?.let { append(" ${renderJsonQueryWrapperBehavior(it)}") }
            column.quotes?.let { append(" ${renderJsonQueryQuotesBehavior(it)}") }
            column.onEmpty?.let { append(" ${renderJsonQueryEmptyBehavior(it)}") }
            column.onError?.let { append(" ${renderJsonQueryErrorBehavior(it)}") }
        }
        is SqlJsonColumn.Exists -> buildString {
            append("${quoteIdent(column.name)} ${renderType(column.type)} EXISTS")
            column.path?.let { append(" PATH ${renderExpr(it)}") }
            column.onError?.let { append(" ${renderJsonExistsErrorBehavior(it)}") }
        }
        is SqlJsonColumn.Nested -> buildString {
            append("NESTED PATH ${renderExpr(column.path)}")
            column.pathAlias?.let { append(" AS ${quoteIdent(it)}") }
            append(column.columns.joinToString(", ", " COLUMNS (", ")") { renderJsonColumn(it) })
        }
    }

    private fun renderColumnDefinition(column: SqlColumnDefinition): String = buildString {
        append("${quoteIdent(column.name)} ${renderType(column.type)}")
        if (!column.nullable) append(" NOT NULL")
        when (column.primaryKey) {
            SqlPrimaryKeyMode.NotPrimary -> {}
            SqlPrimaryKeyMode.Primary -> append(" PRIMARY KEY")
            SqlPrimaryKeyMode.Identity -> append(" PRIMARY KEY GENERATED ALWAYS AS IDENTITY")
            SqlPrimaryKeyMode.Uuid,
            SqlPrimaryKeyMode.Snowflake -> append(" PRIMARY KEY")
        }
        column.defaultValue?.let { append(" DEFAULT ${renderExpr(it)}") }
    }

    private fun renderIndexDefinition(index: SqlIndexDefinition): String = buildString {
        if (index.unique) append("UNIQUE ")
        append("INDEX ${quoteIdent(index.name)}")
        index.method?.let { append(" USING $it") }
        append(index.columns.joinToString(", ", " (", ")") { quoteIdent(it) })
    }

    protected open fun renderType(type: SqlType): String = when (type) {
        is SqlType.Varchar -> "VARCHAR${type.maxLength?.let { "($it)" } ?: ""}"
        SqlType.Int -> "INTEGER"
        SqlType.Long -> "BIGINT"
        SqlType.Float -> "REAL"
        SqlType.Double -> "DOUBLE PRECISION"
        is SqlType.Decimal -> "DECIMAL${type.precision?.let { "(${it.first}, ${it.second})" } ?: ""}"
        SqlType.Date -> "DATE"
        is SqlType.Timestamp -> "TIMESTAMP${type.mode?.let { " ${renderTimeZoneMode(it)}" } ?: ""}"
        is SqlType.Time -> "TIME${type.mode?.let { " ${renderTimeZoneMode(it)}" } ?: ""}"
        SqlType.Json -> "JSON"
        SqlType.Boolean -> "BOOLEAN"
        SqlType.Interval -> "INTERVAL"
        SqlType.Geometry -> "GEOMETRY"
        SqlType.Point -> "POINT"
        SqlType.LineString -> "LINESTRING"
        SqlType.Polygon -> "POLYGON"
        SqlType.MultiPoint -> "MULTIPOINT"
        SqlType.MultiLineString -> "MULTILINESTRING"
        SqlType.MultiPolygon -> "MULTIPOLYGON"
        SqlType.GeometryCollection -> "GEOMETRYCOLLECTION"
        is SqlType.Array -> "${renderType(type.type)}[]"
        is SqlType.Named -> buildString {
            append(type.name)
            if (type.arguments.isNotEmpty()) append(type.arguments.joinToString(", ", "(", ")"))
        }
        is SqlType.UnsafeCustom -> type.tokens.joinToString(" ", "(", ")") { renderUnsafeToken(it) }
    }

    private fun renderJsonInput(input: SqlJsonInput): String = buildString {
        append("FORMAT JSON")
        input.encoding?.let { append(" ENCODING ${renderEncoding(it)}") }
    }

    private fun renderJsonOutputFormat(format: SqlJsonOutputFormat): String = buildString {
        append("FORMAT JSON")
        format.encoding?.let { append(" ENCODING ${renderEncoding(it)}") }
    }

    private fun renderJsonOutput(output: SqlJsonOutput): String = buildString {
        append("RETURNING ${renderType(output.type)}")
        output.format?.let { append(" ${renderJsonOutputFormat(it)}") }
    }

    private fun renderEncoding(encoding: SqlEncoding): String = when (encoding) {
        SqlEncoding.Utf8 -> "UTF8"
        SqlEncoding.Utf16 -> "UTF16"
        SqlEncoding.Utf32 -> "UTF32"
        is SqlEncoding.UnsafeCustom -> encoding.tokens.joinToString(" ") { renderUnsafeToken(it) }
    }

    private fun renderJsonPassing(passing: SqlJsonPassing): String =
        "${renderExpr(passing.expr)} AS ${quoteIdent(passing.alias)}"

    private fun renderJsonObjectItem(item: SqlJsonObjectItem): String =
        "${renderExpr(item.key)} VALUE ${renderExpr(item.value)}"

    private fun renderJsonArrayItem(item: SqlJsonArrayItem): String = buildString {
        append(renderExpr(item.value))
        item.input?.let { append(" ${renderJsonInput(it)}") }
    }

    private fun renderJsonNullConstructor(constructor: SqlJsonNullConstructor): String = when (constructor) {
        SqlJsonNullConstructor.Null -> "NULL ON NULL"
        SqlJsonNullConstructor.Absent -> "ABSENT ON NULL"
    }

    private fun renderJsonUniquenessMode(mode: SqlJsonUniquenessMode): String = when (mode) {
        SqlJsonUniquenessMode.With -> "WITH UNIQUE KEYS"
        SqlJsonUniquenessMode.Without -> "WITHOUT UNIQUE KEYS"
    }

    private fun renderJsonQueryWrapperBehavior(behavior: SqlJsonQueryWrapperBehavior): String = when (behavior) {
        is SqlJsonQueryWrapperBehavior.With -> buildString {
            append("WITH")
            behavior.mode?.let { append(" ${renderJsonQueryWrapperBehaviorMode(it)}") }
            if (behavior.withArray) append(" ARRAY")
            append(" WRAPPER")
        }
        is SqlJsonQueryWrapperBehavior.Without -> buildString {
            append("WITHOUT")
            if (behavior.withArray) append(" ARRAY")
            append(" WRAPPER")
        }
    }

    private fun renderJsonQueryWrapperBehaviorMode(mode: SqlJsonQueryWrapperBehaviorMode): String = when (mode) {
        SqlJsonQueryWrapperBehaviorMode.Conditional -> "CONDITIONAL"
        SqlJsonQueryWrapperBehaviorMode.Unconditional -> "UNCONDITIONAL"
    }

    private fun renderJsonQueryQuotesBehavior(behavior: SqlJsonQueryQuotesBehavior): String = buildString {
        append(when (behavior.mode) {
            SqlJsonQueryQuotesBehaviorMode.Keep -> "KEEP"
            SqlJsonQueryQuotesBehaviorMode.Omit -> "OMIT"
        })
        append(" QUOTES")
        if (behavior.withOnScalarString) append(" ON SCALAR STRING")
    }

    private fun renderJsonQueryEmptyBehavior(behavior: SqlJsonQueryEmptyBehavior): String =
        "${renderJsonQueryBehaviorValue(behavior)} ON EMPTY"

    private fun renderJsonQueryErrorBehavior(behavior: SqlJsonQueryErrorBehavior): String =
        "${renderJsonQueryBehaviorValue(behavior)} ON ERROR"

    private fun renderJsonQueryBehaviorValue(behavior: SqlJsonQueryEmptyBehavior): String = when (behavior) {
        SqlJsonQueryEmptyBehavior.Error -> "ERROR"
        SqlJsonQueryEmptyBehavior.Null -> "NULL"
        SqlJsonQueryEmptyBehavior.EmptyObject -> "EMPTY OBJECT"
        SqlJsonQueryEmptyBehavior.EmptyArray -> "EMPTY ARRAY"
        is SqlJsonQueryEmptyBehavior.Default -> "DEFAULT ${renderExpr(behavior.expr)}"
    }

    private fun renderJsonQueryBehaviorValue(behavior: SqlJsonQueryErrorBehavior): String = when (behavior) {
        SqlJsonQueryErrorBehavior.Error -> "ERROR"
        SqlJsonQueryErrorBehavior.Null -> "NULL"
        SqlJsonQueryErrorBehavior.EmptyObject -> "EMPTY OBJECT"
        SqlJsonQueryErrorBehavior.EmptyArray -> "EMPTY ARRAY"
        is SqlJsonQueryErrorBehavior.Default -> "DEFAULT ${renderExpr(behavior.expr)}"
    }

    private fun renderJsonValueEmptyBehavior(behavior: SqlJsonValueEmptyBehavior): String = when (behavior) {
        SqlJsonValueEmptyBehavior.Error -> "ERROR ON EMPTY"
        SqlJsonValueEmptyBehavior.Null -> "NULL ON EMPTY"
        is SqlJsonValueEmptyBehavior.Default -> "DEFAULT ${renderExpr(behavior.expr)} ON EMPTY"
    }

    private fun renderJsonValueErrorBehavior(behavior: SqlJsonValueErrorBehavior): String = when (behavior) {
        SqlJsonValueErrorBehavior.Error -> "ERROR ON ERROR"
        SqlJsonValueErrorBehavior.Null -> "NULL ON ERROR"
        is SqlJsonValueErrorBehavior.Default -> "DEFAULT ${renderExpr(behavior.expr)} ON ERROR"
    }

    private fun renderJsonExistsErrorBehavior(behavior: SqlJsonExistsErrorBehavior): String = when (behavior) {
        SqlJsonExistsErrorBehavior.Error -> "ERROR ON ERROR"
        SqlJsonExistsErrorBehavior.True -> "TRUE ON ERROR"
        SqlJsonExistsErrorBehavior.False -> "FALSE ON ERROR"
        SqlJsonExistsErrorBehavior.Unknown -> "UNKNOWN ON ERROR"
    }

    private fun renderJsonErrorBehavior(behavior: SqlJsonErrorBehavior): String = when (behavior) {
        SqlJsonErrorBehavior.Error -> "ERROR ON ERROR"
        SqlJsonErrorBehavior.Empty -> "EMPTY ON ERROR"
        SqlJsonErrorBehavior.EmptyArray -> "EMPTY ARRAY ON ERROR"
    }

    private fun renderGraphPattern(pattern: SqlGraphPattern): String =
        pattern.name?.let { "${quoteIdent(it)} = ${renderGraphPatternTerm(pattern.term)}" }
            ?: renderGraphPatternTerm(pattern.term)

    private fun renderGraphPatternTerm(term: SqlGraphPatternTerm): String = when (term) {
        is SqlGraphPatternTerm.Quantified -> {
            val inner = when (term.term) {
                is SqlGraphPatternTerm.And,
                is SqlGraphPatternTerm.Or,
                is SqlGraphPatternTerm.Alternation -> "(${renderGraphPatternTerm(term.term)})"
                else -> renderGraphPatternTerm(term.term)
            }
            inner + renderGraphQuantifier(term.quantifier)
        }
        is SqlGraphPatternTerm.Vertex -> buildString {
            append("(")
            val parts = buildList {
                term.name?.let { add(quoteIdent(it)) }
                term.label?.let { add("IS ${renderGraphLabel(it)}") }
                term.where?.let { add("WHERE ${renderExpr(it)}") }
            }
            append(parts.joinToString(" "))
            append(")")
        }
        is SqlGraphPatternTerm.Edge -> buildString {
            append(renderGraphSymbol(term.leftSymbol))
            append("[")
            val parts = buildList {
                term.name?.let { add(quoteIdent(it)) }
                term.label?.let { add("IS ${renderGraphLabel(it)}") }
                term.where?.let { add("WHERE ${renderExpr(it)}") }
            }
            append(parts.joinToString(" "))
            append("]")
            append(renderGraphSymbol(term.rightSymbol))
        }
        is SqlGraphPatternTerm.And -> {
            "${renderGraphPatternTermChild(term.left, wrapOr = true, wrapAlternation = true)} ${renderGraphPatternTermChild(term.right, wrapOr = true, wrapAlternation = true)}"
        }
        is SqlGraphPatternTerm.Or -> {
            "${renderGraphPatternTermChild(term.left, wrapAlternation = true)} | ${renderGraphPatternTermChild(term.right, wrapAlternation = true)}"
        }
        is SqlGraphPatternTerm.Alternation -> {
            "${renderGraphPatternTermChild(term.left, wrapOr = true)} |+| ${renderGraphPatternTermChild(term.right, wrapOr = true)}"
        }
    }

    private fun renderGraphPatternTermChild(
        term: SqlGraphPatternTerm,
        wrapOr: Boolean = false,
        wrapAlternation: Boolean = false
    ): String {
        val rendered = renderGraphPatternTerm(term)
        val shouldWrap = (wrapOr && term is SqlGraphPatternTerm.Or) ||
            (wrapAlternation && term is SqlGraphPatternTerm.Alternation)
        return if (shouldWrap) "($rendered)" else rendered
    }

    private fun renderGraphQuantifier(quantifier: SqlGraphQuantifier): String = when (quantifier) {
        SqlGraphQuantifier.Asterisk -> "*"
        SqlGraphQuantifier.Question -> "?"
        SqlGraphQuantifier.Plus -> "+"
        is SqlGraphQuantifier.Between -> "{${quantifier.start?.let { renderExpr(it) } ?: ""},${quantifier.end?.let { renderExpr(it) } ?: ""}}"
        is SqlGraphQuantifier.Quantity -> "{${renderExpr(quantifier.quantity)}}"
    }

    private fun renderGraphSymbol(symbol: SqlGraphSymbol): String = when (symbol) {
        SqlGraphSymbol.Dash -> "-"
        SqlGraphSymbol.Tilde -> "~"
        SqlGraphSymbol.LeftArrow -> "<-"
        SqlGraphSymbol.RightArrow -> "->"
        SqlGraphSymbol.LeftTildeArrow -> "<~"
        SqlGraphSymbol.RightTildeArrow -> "~>"
    }

    private fun renderGraphLabel(label: SqlGraphLabel): String = when (label) {
        is SqlGraphLabel.Label -> quoteIdent(label.name)
        SqlGraphLabel.Percent -> "%"
        is SqlGraphLabel.Not -> "!(${renderGraphLabel(label.label)})"
        is SqlGraphLabel.And -> {
            "${renderGraphLabelChild(label.left, wrapOr = true)} & ${renderGraphLabelChild(label.right, wrapOr = true)}"
        }
        is SqlGraphLabel.Or -> "${renderGraphLabel(label.left)} | ${renderGraphLabel(label.right)}"
    }

    private fun renderGraphLabelChild(label: SqlGraphLabel, wrapOr: Boolean = false): String {
        val rendered = renderGraphLabel(label)
        return if (wrapOr && label is SqlGraphLabel.Or) "($rendered)" else rendered
    }

    private fun renderGraphMatchMode(mode: SqlGraphMatchMode): String = when (mode) {
        is SqlGraphMatchMode.Repeatable -> "REPEATABLE ${renderGraphRepeatableMode(mode.mode)}"
        is SqlGraphMatchMode.Different -> "DIFFERENT ${renderGraphDifferentMode(mode.mode)}"
    }

    private fun renderGraphRepeatableMode(mode: SqlGraphRepeatableMode): String = when (mode) {
        SqlGraphRepeatableMode.Element -> "ELEMENT"
        SqlGraphRepeatableMode.ElementBindings -> "ELEMENT BINDINGS"
        SqlGraphRepeatableMode.Elements -> "ELEMENTS"
    }

    private fun renderGraphDifferentMode(mode: SqlGraphDifferentMode): String = when (mode) {
        SqlGraphDifferentMode.Edge -> "EDGE"
        SqlGraphDifferentMode.EdgeBindings -> "EDGE BINDINGS"
        SqlGraphDifferentMode.Edges -> "EDGES"
    }

    private fun renderGraphRowsMode(mode: SqlGraphRowsMode): String = when (mode) {
        SqlGraphRowsMode.Match -> "ONE ROW PER MATCH"
        is SqlGraphRowsMode.Vertex -> buildString {
            append("ONE ROW PER VERTEX (${quoteIdent(mode.name)})")
            if (mode.inPaths.isNotEmpty()) append(mode.inPaths.joinToString(", ", " IN (", ")") { quoteIdent(it) })
        }
        is SqlGraphRowsMode.Step -> buildString {
            append("ONE ROW PER STEP (${quoteIdent(mode.vertex1)}, ${quoteIdent(mode.edge)}, ${quoteIdent(mode.vertex2)})")
            if (mode.inPaths.isNotEmpty()) append(mode.inPaths.joinToString(", ", " IN (", ")") { quoteIdent(it) })
        }
    }

    private fun renderGraphExportMode(mode: SqlGraphExportMode): String = when (mode) {
        is SqlGraphExportMode.AllSingletons -> mode.exceptPatterns.joinToString(", ", "EXPORT ALL SINGLETONS EXCEPT (", ")") { quoteIdent(it) }
        is SqlGraphExportMode.Singletons -> mode.patterns.joinToString(", ", "EXPORT SINGLETONS (", ")") { quoteIdent(it) }
        SqlGraphExportMode.NoSingletons -> "EXPORT NO SINGLETONS"
    }

    private fun renderMatchRecognize(matchRecognize: SqlMatchRecognize): String = buildString {
        append("MATCH_RECOGNIZE(")
        val clauses = mutableListOf<String>()
        if (matchRecognize.partitionBy.isNotEmpty()) {
            clauses += matchRecognize.partitionBy.joinToString(", ", "PARTITION BY ") { renderExpr(it) }
        }
        if (matchRecognize.orderBy.isNotEmpty()) {
            clauses += matchRecognize.orderBy.joinToString(", ", "ORDER BY ") { renderOrderingItem(it) }
        }
        if (matchRecognize.measures.isNotEmpty()) {
            clauses += matchRecognize.measures.joinToString(", ", "MEASURES ") { renderRecognizeMeasureItem(it) }
        }
        matchRecognize.rowsMode?.let { clauses += renderRecognizeRowsMode(it) }
        clauses += renderRowPattern(matchRecognize.rowPattern)
        append(clauses.joinToString(" "))
        append(")")
        matchRecognize.alias?.let { append(" ${renderTableAlias(it)}") }
    }

    private fun renderRecognizeMeasureItem(item: SqlRecognizeMeasureItem): String =
        "${renderExpr(item.expr)} AS ${quoteIdent(item.alias)}"

    private fun renderRecognizeRowsMode(mode: SqlRecognizePatternRowsMode): String = when (mode) {
        SqlRecognizePatternRowsMode.OneRow -> "ONE ROW PER MATCH"
        is SqlRecognizePatternRowsMode.AllRows -> buildString {
            append("ALL ROWS PER MATCH")
            mode.emptyMatchMode?.let { append(" ${renderRecognizeEmptyMatchMode(it)}") }
        }
    }

    private fun renderRecognizeEmptyMatchMode(mode: SqlRecognizePatternEmptyMatchMode): String = when (mode) {
        SqlRecognizePatternEmptyMatchMode.ShowEmptyMatches -> "SHOW EMPTY MATCHES"
        SqlRecognizePatternEmptyMatchMode.OmitEmptyMatches -> "OMIT EMPTY MATCHES"
        SqlRecognizePatternEmptyMatchMode.WithUnmatchedRows -> "WITH UNMATCHED ROWS"
    }

    private fun renderRowPattern(pattern: SqlRowPattern): String = buildString {
        pattern.afterMatchMode?.let { append("AFTER MATCH ${renderRowPatternSkipMode(it)} ") }
        pattern.strategy?.let { append("${it.name.uppercase()} ") }
        append("PATTERN (${renderRowPatternTerm(pattern.pattern)})")
        if (pattern.subset.isNotEmpty()) {
            append(pattern.subset.joinToString(", ", " SUBSET ") { renderRowPatternSubsetItem(it) })
        }
        append(pattern.define.joinToString(", ", " DEFINE ") { renderRowPatternDefineItem(it) })
    }

    private fun renderRowPatternTerm(term: SqlRowPatternTerm): String {
        val core = when (term) {
            is SqlRowPatternTerm.Pattern -> quoteIdent(term.name)
            is SqlRowPatternTerm.Circumflex -> "^"
            is SqlRowPatternTerm.Dollar -> "\$"
            is SqlRowPatternTerm.Exclusion -> "{-${renderRowPatternTerm(term.term)}-}"
            is SqlRowPatternTerm.Permute -> term.terms.joinToString(", ", "PERMUTE(", ")") { renderRowPatternTerm(it) }
            is SqlRowPatternTerm.Then -> {
                "${renderRowPatternTermChild(term.left, wrapOr = true)} ${renderRowPatternTermChild(term.right, wrapOr = true)}"
            }
            is SqlRowPatternTerm.Or -> "${renderRowPatternTerm(term.left)} | ${renderRowPatternTerm(term.right)}"
        }
        val quantifiedCore = if (term.quantifier != null && (term is SqlRowPatternTerm.Then || term is SqlRowPatternTerm.Or)) {
            "($core)"
        } else {
            core
        }
        return quantifiedCore + (term.quantifier?.let { renderRowPatternQuantifier(it) } ?: "")
    }

    private fun renderRowPatternTermChild(term: SqlRowPatternTerm, wrapOr: Boolean = false): String {
        val rendered = renderRowPatternTerm(term)
        return if (wrapOr && term is SqlRowPatternTerm.Or) "($rendered)" else rendered
    }

    private fun renderRowPatternQuantifier(quantifier: SqlRowPatternQuantifier): String = when (quantifier) {
        is SqlRowPatternQuantifier.Asterisk -> "*" + if (quantifier.withQuestion) "?" else ""
        is SqlRowPatternQuantifier.Plus -> "+" + if (quantifier.withQuestion) "?" else ""
        is SqlRowPatternQuantifier.Question -> "?" + if (quantifier.withQuestion) "?" else ""
        is SqlRowPatternQuantifier.Between -> buildString {
            append("{")
            quantifier.start?.let { append(renderExpr(it)) }
            append(",")
            quantifier.end?.let { append(renderExpr(it)) }
            append("}")
            if (quantifier.withQuestion) append("?")
        }
        is SqlRowPatternQuantifier.Quantity -> "{${renderExpr(quantifier.quantity)}}"
    }

    private fun renderRowPatternSkipMode(mode: SqlRowPatternSkipMode): String = when (mode) {
        SqlRowPatternSkipMode.ToNextRow -> "SKIP TO NEXT ROW"
        SqlRowPatternSkipMode.PastLastRow -> "SKIP PAST LAST ROW"
        is SqlRowPatternSkipMode.ToFirst -> "SKIP TO FIRST ${quoteIdent(mode.name)}"
        is SqlRowPatternSkipMode.ToLast -> "SKIP TO LAST ${quoteIdent(mode.name)}"
        is SqlRowPatternSkipMode.To -> "SKIP TO ${quoteIdent(mode.name)}"
    }

    private fun renderRowPatternSubsetItem(item: SqlRowPatternSubsetItem): String =
        item.patternNames.joinToString(", ", "${quoteIdent(item.name)} = (", ")") { quoteIdent(it) }

    private fun renderRowPatternDefineItem(item: SqlRowPatternDefineItem): String =
        "${quoteIdent(item.name)} AS ${renderExpr(item.expr)}"

    protected fun renderQuantifier(quantifier: SqlQuantifier): String = when (quantifier) {
        SqlQuantifier.All -> "ALL"
        SqlQuantifier.Distinct -> "DISTINCT"
        is SqlQuantifier.UnsafeCustom -> quantifier.tokens.joinToString(" ") { renderUnsafeToken(it) }
    }

    protected open fun renderSetOperator(operator: SqlSetOperator): String = when (operator) {
        is SqlSetOperator.Union -> "UNION${operator.quantifier?.let { " ${renderQuantifier(it)}" } ?: ""}"
        is SqlSetOperator.Except -> "EXCEPT${operator.quantifier?.let { " ${renderQuantifier(it)}" } ?: ""}"
        is SqlSetOperator.Intersect -> "INTERSECT${operator.quantifier?.let { " ${renderQuantifier(it)}" } ?: ""}"
    }

    protected open fun renderBinaryOperator(operator: SqlBinaryOperator): String = when (operator) {
        SqlBinaryOperator.Times -> "*"
        SqlBinaryOperator.Div -> "/"
        SqlBinaryOperator.Mod -> "%"
        SqlBinaryOperator.Plus -> "+"
        SqlBinaryOperator.Minus -> "-"
        SqlBinaryOperator.Concat -> "||"
        SqlBinaryOperator.Equal -> "="
        SqlBinaryOperator.NotEqual -> "<>"
        is SqlBinaryOperator.IsDistinctFrom -> if (operator.withNot) "IS NOT DISTINCT FROM" else "IS DISTINCT FROM"
        is SqlBinaryOperator.Is -> if (operator.withNot) "IS NOT" else "IS"
        SqlBinaryOperator.GreaterThan -> ">"
        SqlBinaryOperator.GreaterThanEqual -> ">="
        SqlBinaryOperator.LessThan -> "<"
        SqlBinaryOperator.LessThanEqual -> "<="
        SqlBinaryOperator.Overlaps -> "OVERLAPS"
        SqlBinaryOperator.Regexp -> "REGEXP"
        SqlBinaryOperator.NotRegexp -> "NOT REGEXP"
        SqlBinaryOperator.BitwiseAnd -> "&"
        SqlBinaryOperator.BitwiseOr -> "|"
        SqlBinaryOperator.BitwiseXor -> "^"
        SqlBinaryOperator.BitwiseLeftShift -> "<<"
        SqlBinaryOperator.BitwiseRightShift -> ">>"
        SqlBinaryOperator.And -> "AND"
        SqlBinaryOperator.Or -> "OR"
        is SqlBinaryOperator.UnsafeCustom -> operator.tokens.joinToString(" ") { renderUnsafeToken(it) }
    }

    private fun renderUnaryOperator(operator: SqlUnaryOperator): String = when (operator) {
        SqlUnaryOperator.Positive -> "+"
        SqlUnaryOperator.Negative -> "-"
        SqlUnaryOperator.Not -> "NOT"
        SqlUnaryOperator.BitwiseNot -> "~"
        is SqlUnaryOperator.UnsafeCustom -> operator.tokens.joinToString(" ") { renderUnsafeToken(it) }
    }

    private fun renderQuantifiedComparisonOperator(operator: SqlQuantifiedComparisonOperator): String = when (operator) {
        SqlQuantifiedComparisonOperator.Equal -> "="
        SqlQuantifiedComparisonOperator.NotEqual -> "<>"
        SqlQuantifiedComparisonOperator.GreaterThan -> ">"
        SqlQuantifiedComparisonOperator.GreaterThanEqual -> ">="
        SqlQuantifiedComparisonOperator.LessThan -> "<"
        SqlQuantifiedComparisonOperator.LessThanEqual -> "<="
        is SqlQuantifiedComparisonOperator.UnsafeCustom -> operator.tokens.joinToString(" ") { renderUnsafeToken(it) }
    }

    private fun renderParameter(parameter: SqlParameter): String = when (parameter) {
        is SqlParameter.Named -> ":${parameter.name}"
        is SqlParameter.Positional -> "?"
    }

    private fun renderTimeType(type: SqlTimeType): String = when (type) {
        SqlTimeType.Date -> "DATE"
        is SqlTimeType.Time -> "TIME${type.mode?.let { " ${renderTimeZoneMode(it)}" } ?: ""}"
        is SqlTimeType.Timestamp -> "TIMESTAMP${type.mode?.let { " ${renderTimeZoneMode(it)}" } ?: ""}"
    }

    private fun renderTimeZoneMode(mode: SqlTimeZoneMode): String = when (mode) {
        SqlTimeZoneMode.WithTimeZone -> "WITH TIME ZONE"
        SqlTimeZoneMode.WithoutTimeZone -> "WITHOUT TIME ZONE"
    }

    private fun renderIntervalField(field: SqlIntervalField): String = when (field) {
        is SqlIntervalField.Single -> field.unit.name.uppercase()
        is SqlIntervalField.To -> "${field.start.name.uppercase()} TO ${field.end.name.uppercase()}"
    }

    protected fun renderListAggOnOverflow(onOverflow: SqlListAggOnOverflow): String = when (onOverflow) {
        SqlListAggOnOverflow.Error -> "ON OVERFLOW ERROR"
        is SqlListAggOnOverflow.Truncate -> buildString {
            append("ON OVERFLOW TRUNCATE")
            onOverflow.filler?.let { append(" ${renderExpr(it)}") }
            onOverflow.countMode?.let {
                append(" ")
                append(when (it) {
                    SqlListAggCountMode.With -> "WITH COUNT"
                    SqlListAggCountMode.Without -> "WITHOUT COUNT"
                })
            }
        }
    }

    private fun renderWindowNullsMode(mode: SqlWindowNullsMode): String = when (mode) {
        SqlWindowNullsMode.Respect -> "RESPECT NULLS"
        SqlWindowNullsMode.Ignore -> "IGNORE NULLS"
    }

    protected fun renderFilter(expr: SqlExpr): String = "FILTER (WHERE ${renderExpr(expr)})"

    protected fun renderUnsafeToken(token: SqlUnsafeToken): String = when (token) {
        is SqlUnsafeToken.Text -> token.value
        is SqlUnsafeToken.Identifier -> quoteIdent(token.value)
        is SqlUnsafeToken.Expr -> renderExpr(token.value)
    }

    protected fun quoteIdent(identifier: String): String =
        dialect.leftQuote + identifier.replace(dialect.rightQuote, dialect.rightQuote + dialect.rightQuote) + dialect.rightQuote

    private fun quoteString(value: String): String = buildString {
        append("'")
        value.forEach { char ->
            if (char == '\'') append('\'')
            if (char == '\\' && !dialect.standardEscapeStrings) append('\\')
            append(char)
        }
        append("'")
    }

    protected fun safeUnquotedIdent(identifier: String): String =
        identifier.filter { it.isLetterOrDigit() || it == '_' }
}
