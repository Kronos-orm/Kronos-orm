/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlTimeType
import com.kotlinorm.syntax.expr.SqlTimeZoneMode
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.inspect.SqlParameterCollector
import com.kotlinorm.syntax.limit.SqlFetchMode
import com.kotlinorm.syntax.limit.SqlFetchUnit
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlNullsOrdering
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlPrimaryKeyMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlServerExtendedPropertyOperation
import com.kotlinorm.syntax.statement.SqlUpsertAction
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.syntax.token.SqlUnsafeToken

open class H2SqlRenderer : StandardSqlRenderer(SqlDialect.H2) {
    override fun shouldQuoteSelectItemAlias(metadata: SqlSelectItemAliasMetadata?): Boolean = true

    override fun renderDdl(statement: SqlDdlStatement): String = when (statement) {
        is SqlDdlStatement.AlterTable.ModifyColumn -> {
            val column = statement.column.copy(primaryKey = SqlPrimaryKeyMode.NotPrimary)
            "ALTER TABLE ${renderIdentifier(statement.tableName)} ALTER COLUMN ${renderColumnDefinition(column)}"
        }
        else -> super.renderDdl(statement)
    }

    override fun renderUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append("MERGE INTO ")
        append(renderTable(statement.table.copy(alias = SqlTableAlias("t1"))))
        append(" USING (VALUES (")
        append(statement.values.joinToString(", ") { renderExpr(it) })
        append(")) ")
        append(renderTableAlias(SqlTableAlias("t2", statement.columns.map { it.canonical })))
        append(" ON (")
        append(statement.primaryKeys.joinToString(" AND ") {
            "${quoteIdent("t1")}.${renderIdentifier(it)} = ${quoteIdent("t2")}.${renderIdentifier(it)}"
        })
        append(")")
        when (val action = statement.action) {
            SqlUpsertAction.DoNothing -> {}
            is SqlUpsertAction.Update -> {
                append(" WHEN MATCHED THEN UPDATE SET ")
                append(action.setPairs.joinToString(", ") {
                    "${renderAssignmentTarget(qualifyAssignmentTarget(it.target, SqlIdentifier.of("t1")))} = ${renderMergeSourceExpr(it.value, statement.table.identifier)}"
                })
                action.where?.let { append(" WHERE ${renderPredicate(it)}") }
            }
        }
        append(" WHEN NOT MATCHED THEN INSERT (")
        append(statement.columns.joinToString(", ") { renderIdentifier(it) })
        append(") VALUES (")
        append(statement.columns.joinToString(", ") { "${quoteIdent("t2")}.${renderIdentifier(it)}" })
        append(")")
        statement.returning?.let { append(" ${renderReturning(it)}") }
    }
}

open class MysqlSqlRenderer(
    standardEscapeStrings: Boolean = false
) : StandardSqlRenderer(SqlDialect.MySql.copy(standardEscapeStrings = standardEscapeStrings)) {
    override fun renderDdl(statement: SqlDdlStatement): String = when (statement) {
        is SqlDdlStatement.CreateTable -> renderMysqlCreateTable(statement)
        is SqlDdlStatement.AlterTable.AddColumn -> buildString {
            append("ALTER TABLE ${renderIdentifier(statement.tableName)} ADD COLUMN ${renderColumnDefinition(statement.column)}")
            statement.position?.let { append(" ${renderColumnPosition(it)}") }
        }
        is SqlDdlStatement.AlterTable.ModifyColumn -> buildString {
            append("ALTER TABLE ${renderIdentifier(statement.tableName)} MODIFY COLUMN ${renderColumnDefinition(statement.column)}")
            statement.position?.let { append(" ${renderColumnPosition(it)}") }
        }
        is SqlDdlStatement.CreateIndex -> renderMysqlCreateIndex(statement)
        is SqlDdlStatement.CommentOnTable -> renderMysqlTableComment(statement.tableName, statement.comment)
        is SqlDdlStatement.AlterTable.SetTableComment -> renderMysqlTableComment(statement.tableName, statement.comment)
        else -> super.renderDdl(statement)
    }

    private fun renderMysqlCreateTable(statement: SqlDdlStatement.CreateTable): String = buildString {
        append("CREATE TABLE ")
        if (statement.ifNotExists) append("IF NOT EXISTS ")
        append(renderIdentifier(statement.tableName))
        val definitions = statement.columns.map { renderColumnDefinition(it) } +
            statement.indexes.map { renderIndexDefinition(it) }
        append(definitions.joinToString(", ", " (", ")"))
        statement.comment?.let { append(" COMMENT = ${renderCommentValue(it)}") }
    }

    private fun renderMysqlCreateIndex(statement: SqlDdlStatement.CreateIndex): String = buildString {
        append("CREATE")
        if (statement.unique) append(" UNIQUE")
        statement.type?.takeIf { it.isNotBlank() }?.let { append(" $it") }
        append(" INDEX")
        if (statement.ifNotExists) append(" IF NOT EXISTS")
        append(" ${renderIdentifier(statement.indexName)} ON ${renderIdentifier(statement.tableName)}")
        append(statement.columns.joinToString(", ", " (", ")") { renderIdentifier(it) })
        statement.method?.takeIf { it.isNotBlank() }?.let { append(" USING $it") }
    }

    private fun renderMysqlTableComment(tableName: SqlIdentifier, comment: String?): String =
        "ALTER TABLE ${renderIdentifier(tableName)} COMMENT = ${renderCommentValue(comment ?: "")}"

    override fun renderColumnDefinition(column: SqlColumnDefinition): String = buildString {
        append("${renderIdentifier(column.name)} ${renderType(column.type)}")
        if (!column.nullable) append(" NOT NULL")
        if (column.primaryKey != SqlPrimaryKeyMode.NotPrimary) append(" PRIMARY KEY")
        if (column.primaryKey == SqlPrimaryKeyMode.Identity) append(" AUTO_INCREMENT")
        column.defaultValue?.let { append(" DEFAULT ${renderExpr(it)}") }
    }

    override fun renderUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append("INSERT INTO ")
        append(renderTable(statement.table))
        append(statement.columns.joinToString(", ", " (", ")") { renderIdentifier(it) })
        append(" VALUES (")
        append(statement.values.joinToString(", ") { renderExpr(it) })
        append(") ON DUPLICATE KEY UPDATE ")
        when (val action = statement.action) {
            SqlUpsertAction.DoNothing -> {
                val column = statement.primaryKeys.firstOrNull() ?: statement.columns.first()
                append("${renderIdentifier(column)} = ${renderIdentifier(column)}")
            }
            is SqlUpsertAction.Update -> {
                append(action.setPairs.joinToString(", ") {
                    val value = when (val expr = it.value) {
                        is SqlExpr.ExcludedColumn -> "VALUES (${renderIdentifier(expr.identifier)})"
                        else -> renderExpr(expr)
                    }
                    "${renderAssignmentTarget(it.target)} = $value"
                })
            }
        }
        statement.returning?.let { append(" ${renderReturning(it)}") }
    }

    override fun renderLimit(limit: SqlLimit): String = buildString {
        limit.fetch?.let { append("LIMIT ${renderExpr(it.limit)}") }
        limit.offset?.let {
            if (isEmpty()) append("LIMIT ${Long.MAX_VALUE}")
            append(" OFFSET ${renderExpr(it)}")
        }
    }

    override fun renderExpr(expr: SqlExpr): String = when (expr) {
        is SqlExpr.BooleanLiteral -> expr.boolean.toString()
        else -> super.renderExpr(expr)
    }

    override fun renderValuesQuery(query: com.kotlinorm.syntax.statement.SqlQuery.Values): String =
        query.values.joinToString(", ", "VALUES ") { row ->
            row.joinToString(", ", "ROW(", ")") { renderExpr(it) }
        }

    override fun renderBinary(expr: SqlExpr.Binary): String = when (val op = expr.operator) {
        SqlBinaryOperator.Concat -> "CONCAT(${renderExpr(expr.left)}, ${renderExpr(expr.right)})"
        is SqlBinaryOperator.IsDistinctFrom -> {
            val comparison = "${renderExpr(expr.left)} <=> ${renderExpr(expr.right)}"
            if (op.withNot) comparison else "NOT($comparison)"
        }
        else -> super.renderBinary(expr)
    }

    override fun renderListAgg(expr: SqlExpr.ListAggFunc): String = buildString {
        append("GROUP_CONCAT(")
        expr.quantifier?.let { append("${renderQuantifier(it)} ") }
        append(renderExpr(expr.expr))
        if (expr.withinGroup.isNotEmpty()) {
            append(expr.withinGroup.joinToString(", ", " ORDER BY ") { renderOrderingItem(it) })
        }
        append(" SEPARATOR ")
        append(renderExpr(expr.separator))
        append(")")
        expr.filter?.let { append(" ${renderFilter(it)}") }
    }

    override fun renderOrderingItem(item: SqlOrderingItem): String {
        val order = item.ordering ?: SqlOrdering.Asc
        val orderSql = order.name.uppercase()
        return when (item.nullsOrdering) {
            null -> "${renderExpr(item.expr)} $orderSql"
            SqlNullsOrdering.First -> if (order == SqlOrdering.Asc) {
                "${renderExpr(item.expr)} $orderSql"
            } else {
                "CASE WHEN ${renderExpr(item.expr)} IS NULL THEN 1 ELSE 0 END $orderSql, ${renderExpr(item.expr)} $orderSql"
            }
            SqlNullsOrdering.Last -> if (order == SqlOrdering.Desc) {
                "${renderExpr(item.expr)} $orderSql"
            } else {
                "CASE WHEN ${renderExpr(item.expr)} IS NULL THEN 1 ELSE 0 END $orderSql, ${renderExpr(item.expr)} $orderSql"
            }
        }
    }

    override fun renderFunction(expr: SqlExpr.Function): String {
        if (expr.name.last.equals("TRUNC", ignoreCase = true)) {
            return renderFunction(expr.copy(name = SqlIdentifier.of("TRUNCATE")))
        }
        if (expr.name.last.equals("JOIN", ignoreCase = true)) {
            return renderFunction(expr.copy(name = SqlIdentifier.of("CONCAT_WS")))
        }
        if (expr.name.last.equals("CONCAT", ignoreCase = true)) {
            val args = flattenConcatArgs(expr.args)
            return super.renderFunction(expr.copy(args = args))
        }
        return super.renderFunction(expr)
    }

    private fun flattenConcatArgs(args: List<SqlExpr>): List<SqlExpr> =
        args.flatMap { arg ->
            if (arg is SqlExpr.Function && arg.name.last.equals("CONCAT", ignoreCase = true)) {
                flattenConcatArgs(arg.args)
            } else {
                listOf(arg)
            }
        }

    override fun renderType(type: SqlType): String = when (type) {
        is SqlType.Varchar -> "CHAR${type.maxLength?.let { "($it)" } ?: ""}"
        SqlType.Int,
        SqlType.Long -> "SIGNED"
        is SqlType.Timestamp -> when (type.mode) {
            null,
            SqlTimeZoneMode.WithoutTimeZone -> "DATETIME"
            SqlTimeZoneMode.WithTimeZone -> super.renderType(type)
        }
        else -> super.renderType(type)
    }
}

open class PostgresqlSqlRenderer : StandardSqlRenderer(SqlDialect.PostgreSql) {
    override fun shouldQuoteSelectItemAlias(metadata: SqlSelectItemAliasMetadata?): Boolean =
        metadata?.userReferenceable == true || super.shouldQuoteSelectItemAlias(metadata)

    override fun renderDdl(statement: SqlDdlStatement): String = when (statement) {
        is SqlDdlStatement.AlterTable.ModifyColumn -> {
            "ALTER TABLE ${renderIdentifier(statement.tableName)} ALTER COLUMN ${renderIdentifier(statement.column.name)} TYPE ${renderType(statement.column.type)}"
        }
        is SqlDdlStatement.AlterTable.SetTableComment -> {
            super.renderDdl(SqlDdlStatement.CommentOnTable(statement.tableName, statement.comment))
        }
        else -> super.renderDdl(statement)
    }

    override fun renderUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append("INSERT INTO ")
        append(renderTable(statement.table))
        append(statement.columns.joinToString(", ", " (", ")") { renderIdentifier(it) })
        append(" VALUES (")
        append(statement.values.joinToString(", ") { renderExpr(it) })
        append(") ON CONFLICT ")
        val target = statement.conflictTarget
        if (target.constraintName != null) {
            append("ON CONSTRAINT ${renderIdentifier(target.constraintName)}")
        } else {
            val columns = if (target.columns.isNotEmpty()) target.columns else statement.primaryKeys
            append(columns.joinToString(", ", "(", ")") { renderIdentifier(it) })
            target.where?.let { append(" WHERE ${renderPredicate(it)}") }
        }
        when (val action = statement.action) {
            SqlUpsertAction.DoNothing -> append(" DO NOTHING")
            is SqlUpsertAction.Update -> {
                append(" DO UPDATE SET ")
                append(action.setPairs.joinToString(", ") { "${renderAssignmentTarget(it.target)} = ${renderExpr(it.value)}" })
                action.where?.let { append(" WHERE ${renderPredicate(it)}") }
            }
        }
        statement.returning?.let { append(" ${renderReturning(it)}") }
    }

    override fun renderLimit(limit: SqlLimit): String {
        val fetch = limit.fetch
        val standardMode = fetch != null && (fetch.unit != SqlFetchUnit.RowCount || fetch.mode != SqlFetchMode.Only)
        return if (standardMode) {
            buildString {
                limit.offset?.let { append("OFFSET ${renderExpr(it)} ROWS") }
                fetch.let {
                    if (isNotEmpty()) append(" ")
                    append("FETCH NEXT ${renderExpr(it.limit)} ")
                    append(
                        when (it.unit) {
                            SqlFetchUnit.RowCount -> "ROWS"
                            SqlFetchUnit.Percentage -> "PERCENT ROWS"
                        }
                    )
                    append(" ")
                    append(
                        when (it.mode) {
                            SqlFetchMode.Only -> "ONLY"
                            SqlFetchMode.WithTies -> "WITH TIES"
                        }
                    )
                }
            }
        } else {
            buildString {
                fetch?.let { append("LIMIT ${renderExpr(it.limit)}") }
                limit.offset?.let {
                    if (isNotEmpty()) append(" ")
                    append("OFFSET ${renderExpr(it)}")
                }
            }
        }
    }

    override fun renderListAgg(expr: SqlExpr.ListAggFunc): String {
        val function = SqlExpr.Function(
            quantifier = expr.quantifier,
            name = SqlIdentifier.of("STRING_AGG"),
            args = listOf(expr.expr, expr.separator),
            orderBy = expr.withinGroup,
            filter = expr.filter
        )
        return renderExpr(function)
    }

    override fun renderFunction(expr: SqlExpr.Function): String = when (expr.name.last.uppercase()) {
        "RAND" -> "RANDOM()"
        "SUBSTR" -> renderPostgresSubstring(expr.args)
        "LEFT" -> renderPostgresSubstring(listOfNotNull(expr.args.getOrNull(0), SqlExpr.NumberLiteral("1"), expr.args.getOrNull(1)))
        "RIGHT" -> {
            val source = expr.args.getOrNull(0)
            val length = expr.args.getOrNull(1)
            if (source != null && length != null) "SUBSTRING(${renderExpr(source)} FROM -${renderExpr(length)})" else super.renderFunction(expr)
        }
        else -> super.renderFunction(expr)
    }

    private fun renderPostgresSubstring(args: List<SqlExpr>): String {
        val source = args.getOrNull(0) ?: return "SUBSTRING()"
        val start = args.getOrNull(1)
        val length = args.getOrNull(2)
        return buildString {
            append("SUBSTRING(${renderExpr(source)}")
            start?.let { append(" FROM ${renderExpr(it)}") }
            length?.let { append(" FOR ${renderExpr(it)}") }
            append(")")
        }
    }
}

open class SqliteSqlRenderer : StandardSqlRenderer(SqlDialect.SQLite) {
    override fun renderSetQuery(query: SqlQuery.Set): String = buildString {
        append(renderSqliteSetOperand(query.left))
        append(" ${renderSetOperator(query.operator)} ")
        append(renderSqliteSetOperand(query.right))
        if (query.orderBy.isNotEmpty()) {
            append(query.orderBy.joinToString(", ", " ORDER BY ") { renderOrderingItem(it) })
        }
        query.limit?.let { append(" ${renderLimit(it)}") }
    }

    private fun renderSqliteSetOperand(query: SqlQuery): String =
        when (query) {
            is SqlQuery.Set -> "(${renderSetQuery(query)})"
            else -> renderQuery(query)
        }

    override fun renderDdl(statement: SqlDdlStatement): String = when (statement) {
        is SqlDdlStatement.Vacuum -> buildString {
            append("VACUUM")
            statement.schemaName?.let { append(" ${renderIdentifier(it)}") }
            statement.into?.let { append(" INTO ${renderExpr(it)}") }
        }
        else -> super.renderDdl(statement)
    }

    override fun renderColumnDefinition(column: SqlColumnDefinition): String = buildString {
        append("${renderIdentifier(column.name)} ${renderType(column.type)}")
        if (!column.nullable) append(" NOT NULL")
        if (column.primaryKey != SqlPrimaryKeyMode.NotPrimary) append(" PRIMARY KEY")
        if (column.primaryKey == SqlPrimaryKeyMode.Identity) append(" AUTOINCREMENT")
        column.defaultValue?.let { append(" DEFAULT ${renderExpr(it)}") }
    }

    override fun renderUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append(if (statement.action == SqlUpsertAction.DoNothing) "INSERT OR IGNORE INTO " else "INSERT INTO ")
        append(renderTable(statement.table))
        append(statement.columns.joinToString(", ", " (", ")") { renderIdentifier(it) })
        append(" VALUES (")
        append(statement.values.joinToString(", ") { renderExpr(it) })
        append(")")
        when (val action = statement.action) {
            SqlUpsertAction.DoNothing -> {}
            is SqlUpsertAction.Update -> {
                val target = statement.conflictTarget
                val columns = if (target.columns.isNotEmpty()) target.columns else statement.primaryKeys
                append(columns.joinToString(", ", " ON CONFLICT (", ")") { renderIdentifier(it) })
                target.where?.let { append(" WHERE ${renderPredicate(it)}") }
                append(" DO UPDATE SET ")
                append(action.setPairs.joinToString(", ") { "${renderAssignmentTarget(it.target)} = ${renderExpr(it.value)}" })
                action.where?.let { append(" WHERE ${renderPredicate(it)}") }
            }
        }
        statement.returning?.let { append(" ${renderReturning(it)}") }
    }

    override fun renderLimit(limit: SqlLimit): String = buildString {
        append("LIMIT ")
        append(limit.fetch?.let { renderExpr(it.limit) } ?: Long.MAX_VALUE.toString())
        limit.offset?.let { append(" OFFSET ${renderExpr(it)}") }
    }

    override fun renderListAgg(expr: SqlExpr.ListAggFunc): String {
        val function = SqlExpr.Function(
            quantifier = expr.quantifier,
            name = SqlIdentifier.of("GROUP_CONCAT"),
            args = listOf(expr.expr, expr.separator),
            orderBy = expr.withinGroup,
            filter = expr.filter
        )
        return renderExpr(function)
    }

    override fun renderFunction(expr: SqlExpr.Function): String = when (expr.name.last.uppercase()) {
        "RAND" -> "RANDOM()"
        else -> super.renderFunction(expr)
    }
}

open class OracleSqlRenderer : StandardSqlRenderer(SqlDialect.Oracle) {
    override fun quoteIdent(identifier: String): String =
        super.quoteIdent(identifier.uppercase())

    override fun renderSelectItemAlias(alias: String, metadata: SqlSelectItemAliasMetadata?): String {
        val normalized = alias.uppercase()
        return if (shouldQuoteSelectItemAlias(metadata)) quoteIdent(normalized) else safeUnquotedIdent(normalized)
    }

    override fun renderDdl(statement: SqlDdlStatement): String = when (statement) {
        is SqlDdlStatement.CreateTableAsSelect -> renderOracleCreateTableAsSelect(statement)
        is SqlDdlStatement.AlterTable.AddColumn -> {
            "ALTER TABLE ${renderIdentifier(statement.tableName)} ADD ${renderColumnDefinition(statement.column)}"
        }
        is SqlDdlStatement.AlterTable.ModifyColumn -> {
            "ALTER TABLE ${renderIdentifier(statement.tableName)} MODIFY(${renderColumnDefinition(statement.column)})"
        }
        is SqlDdlStatement.AlterTable.SetTableComment -> {
            super.renderDdl(SqlDdlStatement.CommentOnTable(statement.tableName, statement.comment))
        }
        else -> super.renderDdl(statement)
    }

    private fun renderOracleCreateTableAsSelect(statement: SqlDdlStatement.CreateTableAsSelect): String {
        val createSql = "CREATE TABLE ${renderIdentifier(statement.tableName)} AS ${renderQuery(statement.query)}"
        val parameterNames = SqlParameterCollector.collectNamedParameters(statement.query)
        if (!statement.ifNotExists && parameterNames.isEmpty()) return createSql

        val executeImmediate = buildString {
            append("EXECUTE IMMEDIATE '")
            append(createSql.replace("'", "''"))
            append("'")
            if (parameterNames.isNotEmpty()) {
                append(parameterNames.joinToString(", ", " USING ") { name -> ":$name" })
            }
            append(";")
        }
        return if (statement.ifNotExists) {
            "BEGIN $executeImmediate EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;"
        } else {
            "BEGIN $executeImmediate END;"
        }
    }

    override fun renderExpr(expr: SqlExpr): String = when (expr) {
        is SqlExpr.TimeLiteral -> when (expr.type) {
            is SqlTimeType.Timestamp -> when (expr.type.mode) {
                SqlTimeZoneMode.WithTimeZone -> renderExpr(
                    SqlExpr.Function(
                        name = SqlIdentifier.of("TO_TIMESTAMP_TZ"),
                        args = listOf(
                            SqlExpr.StringLiteral(expr.time),
                            SqlExpr.StringLiteral("YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM")
                        )
                    )
                )
                null,
                SqlTimeZoneMode.WithoutTimeZone -> renderExpr(
                    SqlExpr.Function(
                        name = SqlIdentifier.of("TO_TIMESTAMP"),
                        args = listOf(
                            SqlExpr.StringLiteral(expr.time),
                            SqlExpr.StringLiteral("YYYY-MM-DD HH24:MI:SS.FF9")
                        )
                    )
                )
            }
            SqlTimeType.Date -> renderExpr(
                SqlExpr.Function(
                    name = SqlIdentifier.of("TO_DATE"),
                    args = listOf(
                        SqlExpr.StringLiteral(expr.time),
                        SqlExpr.StringLiteral("YYYY-MM-DD")
                    )
                )
            )
            is SqlTimeType.Time -> super.renderExpr(expr)
        }
        else -> super.renderExpr(expr)
    }

    override fun renderPredicateBooleanLiteral(value: Boolean): String =
        if (value) "1 = 1" else "1 = 0"

    override fun renderBinary(expr: SqlExpr.Binary): String = when (expr.operator) {
        SqlBinaryOperator.Mod -> "MOD(${renderExpr(expr.left)}, ${renderExpr(expr.right)})"
        else -> super.renderBinary(expr)
    }

    override fun requiresComparisonOperandParens(expr: SqlExpr): Boolean =
        !(expr is SqlExpr.Binary && expr.operator === SqlBinaryOperator.Mod) && super.requiresComparisonOperandParens(expr)

    override fun renderSelectItemExpr(expr: SqlExpr): String =
        if (expr is SqlExpr.Binary && expr.operator === SqlBinaryOperator.Mod) renderExpr(expr) else super.renderSelectItemExpr(expr)

    override fun renderFunction(expr: SqlExpr.Function): String = when (expr.name.last.uppercase()) {
        "RAND" -> "DBMS_RANDOM.VALUE"
        "CONCAT_WS",
        "JOIN" -> renderOracleConcatWs(expr.args)
        "LEFT" -> {
            val source = expr.args.getOrNull(0)
            val length = expr.args.getOrNull(1)
            if (source != null && length != null) "SUBSTR(${renderExpr(source)}, 1, ${renderExpr(length)})" else super.renderFunction(expr)
        }
        "RIGHT" -> {
            val source = expr.args.getOrNull(0)
            val length = expr.args.getOrNull(1)
            if (source != null && length != null) "SUBSTR(${renderExpr(source)}, -${renderExpr(length)})" else super.renderFunction(expr)
        }
        "REPEAT" -> {
            val source = expr.args.getOrNull(0)
            val times = expr.args.getOrNull(1)
            if (source != null && times != null) {
                val sourceSql = renderExpr(source)
                "RPAD($sourceSql, ${renderExpr(times)} * LENGTH($sourceSql), $sourceSql)"
            } else {
                super.renderFunction(expr)
            }
        }
        else -> super.renderFunction(expr)
    }

    private fun renderOracleConcatWs(args: List<SqlExpr>): String {
        if (args.size < 2) return "''"
        val separator = renderExpr(args.first())
        return args.drop(1).joinToString(" || $separator || ") { renderExpr(it) }
    }

    override fun renderType(type: SqlType): String = when (type) {
        is SqlType.Varchar -> if (type.maxLength == null) "VARCHAR(4000)" else super.renderType(type)
        SqlType.Long -> "INT"
        else -> super.renderType(type)
    }

    override fun renderColumnDefinition(column: SqlColumnDefinition): String = buildString {
        append("${renderIdentifier(column.name)} ${renderType(column.type)}")
        if (column.primaryKey == SqlPrimaryKeyMode.Identity) append(" GENERATED ALWAYS AS IDENTITY")
        if (!column.nullable) append(" NOT NULL")
        if (column.primaryKey != SqlPrimaryKeyMode.NotPrimary) append(" PRIMARY KEY")
        column.defaultValue?.let { append(" DEFAULT ${renderExpr(it)}") }
    }

    override fun renderTableAlias(alias: SqlTableAlias): String = buildString {
        append(renderIdentifier(alias.identifier))
        if (alias.columnIdentifiers.isNotEmpty()) {
            append(alias.columnIdentifiers.joinToString(", ", "(", ")") { renderIdentifier(it) })
        }
    }
}

open class SqlServerSqlRenderer : StandardSqlRenderer(SqlDialect.SqlServer) {
    override fun renderSelect(query: SqlQuery.Select): String {
        val limit = query.limit
        val fetch = limit?.fetch
        if (limit?.offset == null && fetch != null) {
            return renderSelectWithTop(query, fetch.limit)
        }
        return super.renderSelect(query)
    }

    private fun renderSelectWithTop(query: SqlQuery.Select, limit: SqlExpr): String = buildString {
        append("SELECT")
        query.quantifier?.let { append(" ${renderQuantifier(it)}") }
        append(" TOP (${renderExpr(limit)}) ")
        append(if (query.select.isEmpty()) "*" else query.select.joinToString(", ") { renderSelectItem(it) })
        if (query.from.isNotEmpty()) append(query.from.joinToString(", ", " FROM ") { renderTable(it) })
        query.where?.let { append(" WHERE ${renderPredicate(it)}") }
        query.groupBy?.let { append(" ${renderGroup(it)}") }
        query.having?.let { append(" HAVING ${renderPredicate(it)}") }
        if (query.window.isNotEmpty()) {
            append(query.window.joinToString(", ", " WINDOW ") { renderWindowItem(it) })
        }
        query.qualify?.let { append(" QUALIFY ${renderPredicate(it)}") }
        if (query.orderBy.isNotEmpty()) {
            append(query.orderBy.joinToString(", ", " ORDER BY ") { renderOrderingItem(it) })
        }
    }

    override fun renderDdl(statement: SqlDdlStatement): String = when (statement) {
        is SqlDdlStatement.CreateTable -> renderSqlServerCreateTable(statement)
        is SqlDdlStatement.CreateTableAsSelect -> renderSqlServerCreateTableAsSelect(statement)
        is SqlDdlStatement.AlterTable.AddColumn -> {
            "ALTER TABLE ${renderIdentifier(statement.tableName)} ADD ${renderColumnDefinition(statement.column)}"
        }
        is SqlDdlStatement.AlterTable.ModifyColumn -> {
            "ALTER TABLE ${renderIdentifier(statement.tableName)} ALTER COLUMN ${renderColumnDefinition(statement.column)}"
        }
        is SqlDdlStatement.AlterTable.AlterColumnDefault -> {
            statement.defaultValue?.let {
                "ALTER TABLE ${renderIdentifier(statement.tableName)} ADD DEFAULT ${renderExpr(it)} FOR ${renderIdentifier(statement.columnName)}"
            } ?: "ALTER TABLE ${renderIdentifier(statement.tableName)} DROP DEFAULT FOR ${renderIdentifier(statement.columnName)}"
        }
        is SqlDdlStatement.CommentOnTable -> renderSqlServerExtendedPropertyComment(
            SqlDdlStatement.SqlServerExtendedPropertyComment(
                tableName = statement.tableName,
                comment = statement.comment,
                operation = commentOperation(statement.comment)
            )
        )
        is SqlDdlStatement.CommentOnColumn -> renderSqlServerExtendedPropertyComment(
            SqlDdlStatement.SqlServerExtendedPropertyComment(
                tableName = statement.tableName,
                columnName = statement.columnName,
                comment = statement.comment,
                operation = commentOperation(statement.comment)
            )
        )
        is SqlDdlStatement.AlterTable.SetTableComment -> renderSqlServerExtendedPropertyComment(
            SqlDdlStatement.SqlServerExtendedPropertyComment(
                tableName = statement.tableName,
                comment = statement.comment,
                operation = commentOperation(statement.comment)
            )
        )
        is SqlDdlStatement.SqlServerExtendedPropertyComment -> renderSqlServerExtendedPropertyComment(statement)
        is SqlDdlStatement.DropIndex -> {
            "DROP INDEX ${renderIdentifier(statement.indexName)} ON ${renderIdentifier(statement.tableName ?: statement.indexName)}"
        }
        is SqlDdlStatement.SqlServerDropDefaultConstraint -> renderSqlServerDropDefaultConstraint(statement)
        else -> super.renderDdl(statement)
    }

    override fun renderUpsert(statement: SqlDmlStatement.Upsert): String =
        "${super.renderUpsert(statement)};"

    private fun renderSqlServerCreateTable(statement: SqlDdlStatement.CreateTable): String {
        val createSql = super.renderDdl(statement.copy(ifNotExists = false))
        if (!statement.ifNotExists) return createSql
        val schemaName = statement.tableName.parts.dropLast(1).lastOrNull() ?: "dbo"
        val tableName = statement.tableName.last
        return "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'${quoteIdent(schemaName)}.${quoteIdent(tableName)}') AND type in (N'U')) BEGIN $createSql; END"
    }

    private fun renderSqlServerCreateTableAsSelect(statement: SqlDdlStatement.CreateTableAsSelect): String {
        val selectInto = when (val query = statement.query) {
            is SqlQuery.Select -> renderSqlServerSelectInto(query, statement.tableName)
            else -> "SELECT * INTO ${renderIdentifier(statement.tableName)} FROM (${renderQuery(query)}) AS ${quoteIdent("__kronos_ctas_source")}"
        }
        if (!statement.ifNotExists) return selectInto
        val schemaName = statement.tableName.parts.dropLast(1).lastOrNull() ?: "dbo"
        val tableName = statement.tableName.last
        return "IF OBJECT_ID(N'${quoteIdent(schemaName)}.${quoteIdent(tableName)}', N'U') IS NULL BEGIN $selectInto; END"
    }

    private fun renderSqlServerSelectInto(query: SqlQuery.Select, tableName: SqlIdentifier): String = buildString {
        val limit = query.limit
        val fetch = limit?.fetch
        append("SELECT")
        query.quantifier?.let { append(" ${renderQuantifier(it)}") }
        if (limit?.offset == null && fetch != null) append(" TOP (${renderExpr(fetch.limit)})")
        append(" ")
        append(if (query.select.isEmpty()) "*" else query.select.joinToString(", ") { renderSelectItem(it) })
        append(" INTO ${renderIdentifier(tableName)}")
        if (query.from.isNotEmpty()) append(query.from.joinToString(", ", " FROM ") { renderTable(it) })
        query.where?.let { append(" WHERE ${renderPredicate(it)}") }
        query.groupBy?.let { append(" ${renderGroup(it)}") }
        query.having?.let { append(" HAVING ${renderPredicate(it)}") }
        if (query.window.isNotEmpty()) {
            append(query.window.joinToString(", ", " WINDOW ") { renderWindowItem(it) })
        }
        query.qualify?.let { append(" QUALIFY ${renderPredicate(it)}") }
        if (query.orderBy.isNotEmpty()) {
            append(query.orderBy.joinToString(", ", " ORDER BY ") { renderOrderingItem(it) })
        }
        if (limit?.offset != null) append(" ${renderLimit(limit)}")
    }

    private fun commentOperation(comment: String?): SqlServerExtendedPropertyOperation =
        if (comment == null) SqlServerExtendedPropertyOperation.Drop else SqlServerExtendedPropertyOperation.Add

    override fun renderColumnDefinition(column: SqlColumnDefinition): String = buildString {
        append("${renderIdentifier(column.name)} ${renderType(column.type)}")
        if (column.primaryKey == SqlPrimaryKeyMode.Identity) append(" IDENTITY")
        if (!column.nullable) append(" NOT NULL")
        if (column.primaryKey != SqlPrimaryKeyMode.NotPrimary) append(" PRIMARY KEY")
        column.defaultValue?.let { append(" DEFAULT ${renderExpr(it)}") }
    }

    override fun renderLimit(limit: SqlLimit): String = buildString {
        append("OFFSET ${limit.offset?.let { renderExpr(it) } ?: "0"} ROWS")
        limit.fetch?.let {
            append(" FETCH NEXT ${renderExpr(it.limit)} ")
            append(
                when (it.unit) {
                    SqlFetchUnit.RowCount -> "ROWS"
                    SqlFetchUnit.Percentage -> "PERCENT ROWS"
                }
            )
            append(" ")
            append(
                when (it.mode) {
                    SqlFetchMode.Only -> "ONLY"
                    SqlFetchMode.WithTies -> "WITH TIES"
                }
            )
        }
    }

    override fun renderExpr(expr: SqlExpr): String = when (expr) {
        is SqlExpr.StringLiteral -> "N${super.renderExpr(expr)}"
        is SqlExpr.TimeLiteral -> when (expr.type) {
            is SqlTimeType.Timestamp -> renderExpr(
                SqlExpr.Cast(
                    SqlExpr.StringLiteral(expr.time),
                    SqlType.Timestamp(expr.type.mode)
                )
            )
            SqlTimeType.Date -> renderExpr(SqlExpr.Cast(SqlExpr.StringLiteral(expr.time), SqlType.Date))
            is SqlTimeType.Time -> super.renderExpr(expr)
        }
        else -> super.renderExpr(expr)
    }

    override fun renderPredicateBooleanLiteral(value: Boolean): String =
        if (value) "1 = 1" else "1 = 0"

    override fun renderFunction(expr: SqlExpr.Function): String = when (expr.name.last.uppercase()) {
        "CEIL" -> renderRenamedFunction("CEILING", expr.args)
        "LN" -> {
            val arg = expr.args.getOrNull(0)
            if (arg != null) "LOG(${renderExpr(arg)}, EXP(1))" else super.renderFunction(expr)
        }
        "LENGTH" -> renderRenamedFunction("LEN", expr.args)
        "REPEAT" -> "REPLICATE(${expr.args.joinToString(", ") { renderSqlServerFunctionArg(it) }})"
        "TRUNC" -> renderRenamedFunction("ROUND", expr.args)
        else -> super.renderFunction(expr)
    }

    private fun renderRenamedFunction(name: String, args: List<SqlExpr>): String =
        args.joinToString(", ", "$name(", ")") { renderExpr(it) }

    private fun renderSqlServerFunctionArg(expr: SqlExpr): String = when (expr) {
        is SqlExpr.StringLiteral -> plainString(expr.string)
        else -> renderExpr(expr)
    }

    private fun plainString(value: String): String =
        "'" + value.replace("'", "''") + "'"

    override fun renderType(type: SqlType): String = when (type) {
        is SqlType.Varchar -> "NVARCHAR(${type.maxLength?.toString() ?: "MAX"})"
        is SqlType.Timestamp -> when (type.mode) {
            SqlTimeZoneMode.WithTimeZone -> "DATETIMEOFFSET"
            null,
            SqlTimeZoneMode.WithoutTimeZone -> "DATETIME2"
        }
        else -> super.renderType(type)
    }

    override fun renderListAgg(expr: SqlExpr.ListAggFunc): String {
        val function = SqlExpr.Function(
            quantifier = expr.quantifier,
            name = SqlIdentifier.of("STRING_AGG"),
            args = listOf(expr.expr, expr.separator),
            withinGroup = expr.withinGroup,
            filter = expr.filter
        )
        return renderExpr(function)
    }

    override fun renderOrderingItem(item: SqlOrderingItem): String {
        val order = item.ordering ?: SqlOrdering.Asc
        val orderSql = order.name.uppercase()
        val exprSql = renderExpr(item.expr)
        val nullRank = "CASE WHEN $exprSql IS NULL THEN 1 ELSE 0 END"
        return when (item.nullsOrdering) {
            null -> "$exprSql $orderSql"
            SqlNullsOrdering.First -> if (order == SqlOrdering.Asc) {
                "$exprSql $orderSql"
            } else {
                "$nullRank $orderSql, $exprSql $orderSql"
            }
            SqlNullsOrdering.Last -> if (order == SqlOrdering.Desc) {
                "$exprSql $orderSql"
            } else {
                "$nullRank $orderSql, $exprSql $orderSql"
            }
        }
    }
}

class UnsafeTokenSqlRenderer(dialect: SqlDialect = SqlDialect.Standard) : StandardSqlRenderer(dialect) {
    fun renderToken(token: SqlUnsafeToken): String = renderUnsafeToken(token)
}
