/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.order.SqlNullsOrdering
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.syntax.table.SqlTable

class PrettySqlRenderer(
    dialect: SqlDialect = SqlDialect.Standard,
    private val indent: String = "  "
) : StandardSqlRenderer(dialect) {
    private val compactRenderer: SqlRenderer = sqlRenderer(dialect)

    override fun renderStatement(statement: SqlStatement): String = when (statement) {
        is SqlQuery -> renderQuery(statement)
        is SqlDmlStatement.Upsert -> renderPrettyUpsert(statement)
        is SqlDmlStatement.Insert -> renderPrettyInsert(statement)
        is SqlDmlStatement.Update -> renderPrettyUpdate(statement)
        is SqlDmlStatement.Delete -> renderPrettyDelete(statement)
        is SqlDmlStatement.Truncate -> super.renderStatement(statement)
        is SqlDdlStatement -> super.renderStatement(statement)
    }

    override fun renderQuery(query: SqlQuery): String = when (query) {
        is SqlQuery.Select -> renderPrettySelect(query)
        is SqlQuery.Set -> renderPrettySet(query)
        is SqlQuery.Values -> super.renderQuery(query)
        is SqlQuery.With -> renderPrettyWith(query)
    }.let { sql ->
        query.lock?.let { "$sql\n${renderLock(it)}" } ?: sql
    }

    private fun renderPrettySelect(query: SqlQuery.Select): String = buildString {
        append("SELECT")
        query.quantifier?.let { append(" ${renderQuantifier(it)}") }
        append("\n")
        val selectItems = if (query.select.isEmpty()) listOf("*") else query.select.map { renderSelectItemPretty(it) }
        append(selectItems.joinToString(",\n") { "${renderIndent(1)}$it" })
        if (query.from.isNotEmpty()) {
            append("\nFROM\n")
            append(query.from.joinToString(",\n") { "${renderIndent(1)}${renderTablePretty(it, 1)}" })
        }
        query.where?.let {
            append("\nWHERE\n")
            append("${renderIndent(1)}${renderExprPretty(it)}")
        }
        query.groupBy?.let {
            append("\n")
            append(renderGroupPretty(it))
        }
        query.having?.let {
            append("\nHAVING\n")
            append("${renderIndent(1)}${renderExprPretty(it)}")
        }
        if (query.orderBy.isNotEmpty()) {
            append("\nORDER BY\n")
            append(query.orderBy.joinToString(",\n") { "${renderIndent(1)}${renderOrderingItemPretty(it)}" })
        }
        query.limit?.let {
            append("\n")
            append(renderLimit(it))
        }
    }

    private fun renderPrettySet(query: SqlQuery.Set): String = buildString {
        append("(\n")
        append(renderQuery(query.left).prependIndent(indent))
        append("\n)\n")
        append(renderSetOperator(query.operator))
        append("\n(\n")
        append(renderQuery(query.right).prependIndent(indent))
        append("\n)")
        if (query.orderBy.isNotEmpty()) {
            append("\nORDER BY\n")
            append(query.orderBy.joinToString(",\n") { "${renderIndent(1)}${renderOrderingItemPretty(it)}" })
        }
        query.limit?.let {
            append("\n")
            append(renderLimit(it))
        }
    }

    private fun renderPrettyWith(query: SqlQuery.With): String = buildString {
        append("WITH")
        if (query.withRecursive) append(" RECURSIVE")
        append("\n")
        append(query.withItems.joinToString(",\n") { item ->
            "${renderIndent(1)}${quoteIdent(item.name)}" +
                item.columnNames.takeIf { it.isNotEmpty() }?.joinToString(", ", " (", ")") { quoteIdent(it) }.orEmpty() +
                " AS (\n${renderQuery(item.query).prependIndent(renderIndent(2))}\n${renderIndent(1)})"
        })
        append("\n")
        append(renderQuery(query.query))
    }

    private fun renderPrettyInsert(statement: SqlDmlStatement.Insert): String = buildString {
        append("INSERT INTO ")
        append(renderTablePretty(statement.table, 0))
        if (statement.columns.isNotEmpty()) {
            append(statement.columns.joinToString(", ", " (", ")") { quoteIdent(it) })
        }
        when (val mode = statement.mode) {
            is SqlInsertMode.Values -> {
                append("\nVALUES\n")
                append(mode.values.joinToString(",\n") { row ->
                    row.joinToString(", ", "${renderIndent(1)}(", ")") { renderExprPretty(it) }
                })
            }
            is SqlInsertMode.Subquery -> {
                append("\n")
                append(renderQuery(mode.query))
            }
        }
    }

    private fun renderPrettyUpdate(statement: SqlDmlStatement.Update): String = buildString {
        append("UPDATE ")
        append(renderTablePretty(statement.table, 0))
        append("\nSET\n")
        append(statement.setPairs.joinToString(",\n") {
            "${renderIndent(1)}${quoteIdent(it.column)} = ${renderExprPretty(it.value)}"
        })
        statement.where?.let {
            append("\nWHERE\n")
            append("${renderIndent(1)}${renderExprPretty(it)}")
        }
    }

    private fun renderPrettyDelete(statement: SqlDmlStatement.Delete): String = buildString {
        append("DELETE FROM ")
        append(renderTablePretty(statement.table, 0))
        statement.where?.let {
            append("\nWHERE\n")
            append("${renderIndent(1)}${renderExprPretty(it)}")
        }
    }

    private fun renderPrettyUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append(
            when (dialect.family) {
                SqlDialectFamily.MySql -> renderPrettyMysqlUpsert(statement)
                SqlDialectFamily.PostgreSql -> renderPrettyPostgresqlUpsert(statement)
                SqlDialectFamily.SQLite -> renderPrettySqliteUpsert(statement)
                SqlDialectFamily.Standard,
                SqlDialectFamily.Oracle,
                SqlDialectFamily.SqlServer -> renderPrettyMergeUpsert(statement)
            }
        )
    }

    private fun renderTablePretty(table: SqlTable, depth: Int): String = when (table) {
        is SqlTable.Subquery -> buildString {
            if (table.matchRecognize != null) {
                append(renderCompactTable(table))
                return@buildString
            }
            if (table.withLateral) append("LATERAL ")
            append("(\n")
            append(renderQuery(table.query).prependIndent(renderIndent(depth + 1)))
            append("\n${renderIndent(depth)})")
            table.alias?.let { append(" ${renderTableAlias(it)}") }
        }
        is SqlTable.Join -> buildString {
            append(renderTablePretty(table.left, depth))
            append("\n")
            append(renderIndent(depth))
            append("${renderJoinType(table.joinType)} JOIN ")
            append(if (table.right is SqlTable.Join) "(${renderTablePretty(table.right, depth + 1)})" else renderTablePretty(table.right, depth))
            table.condition?.let {
                append("\n")
                append(renderIndent(depth))
                append(renderJoinConditionPretty(it))
            }
        }
        else -> renderCompactTable(table)
    }

    private fun renderSelectItemPretty(item: SqlSelectItem): String = when (item) {
        is SqlSelectItem.Asterisk -> item.tableName?.let { "${quoteIdent(it)}.*" } ?: "*"
        is SqlSelectItem.Expr -> renderExprPretty(item.expr) + (item.alias?.let { " AS ${quoteIdent(it)}" } ?: "")
    }

    private fun renderGroupPretty(group: SqlGroup): String = buildString {
        append("GROUP BY")
        group.quantifier?.let { append(" ${renderQuantifier(it)}") }
        append("\n")
        append(group.items.joinToString(",\n") { "${renderIndent(1)}${renderGroupingItemPretty(it)}" })
    }

    private fun renderGroupingItemPretty(item: SqlGroupingItem): String = when (item) {
        SqlGroupingItem.EmptyGroup -> "()"
        is SqlGroupingItem.Expr -> renderExprPretty(item.item)
        is SqlGroupingItem.Cube -> item.items.joinToString(", ", "CUBE(", ")") { renderExprPretty(it) }
        is SqlGroupingItem.Rollup -> item.items.joinToString(", ", "ROLLUP(", ")") { renderExprPretty(it) }
        is SqlGroupingItem.GroupingSets -> item.items.joinToString(", ", "GROUPING SETS(", ")") { renderGroupingItemPretty(it) }
    }

    private fun renderOrderingItemPretty(item: SqlOrderingItem): String = buildString {
        append(renderExprPretty(item.expr))
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

    private fun renderJoinConditionPretty(condition: SqlJoinCondition): String = when (condition) {
        is SqlJoinCondition.On -> "ON ${renderExprPretty(condition.condition)}"
        is SqlJoinCondition.Using -> condition.columnNames.joinToString(", ", "USING (", ")") { quoteIdent(it) }
    }

    private fun renderPrettyMergeUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append("MERGE INTO ")
        append(renderCompactTable(statement.table.copy(alias = SqlTableAlias("t1"))))
        append("\nUSING (\n")
        append("SELECT\n")
        append(statement.values.zip(statement.columns).joinToString(",\n") { (value, column) ->
            "${renderIndent(1)}${renderExprPretty(value)} AS ${quoteIdent(column)}"
        })
        append("\n) ")
        append(renderTableAlias(SqlTableAlias("t2")))
        append("\nON (")
        append(statement.primaryKeys.joinToString(" AND ") {
            "${quoteIdent("t1")}.${quoteIdent(it)} = ${quoteIdent("t2")}.${quoteIdent(it)}"
        })
        append(")\nWHEN MATCHED THEN UPDATE SET\n")
        append(statement.updateColumns.joinToString(",\n") {
            "${renderIndent(1)}${quoteIdent("t1")}.${quoteIdent(it)} = ${quoteIdent("t2")}.${quoteIdent(it)}"
        })
        append("\nWHEN NOT MATCHED THEN INSERT ")
        append(renderColumnList(statement.columns))
        append("\nVALUES (")
        append(statement.values.joinToString(", ") { renderExprPretty(it) })
        append(")")
    }

    private fun renderPrettyMysqlUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append("INSERT INTO ")
        append(renderCompactTable(statement.table))
        append(" ")
        append(renderColumnList(statement.columns))
        append("\nVALUES (")
        append(statement.values.joinToString(", ") { renderExprPretty(it) })
        append(")\nON DUPLICATE KEY UPDATE\n")
        append(statement.updateColumns.joinToString(",\n") {
            "${renderIndent(1)}${quoteIdent(it)} = VALUES (${quoteIdent(it)})"
        })
    }

    private fun renderPrettyPostgresqlUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append("INSERT INTO ")
        append(renderCompactTable(statement.table))
        append(" ")
        append(renderColumnList(statement.columns))
        append("\nVALUES (")
        append(statement.values.joinToString(", ") { renderExprPretty(it) })
        append(")\nON CONFLICT (")
        append(statement.primaryKeys.joinToString(", ") { quoteIdent(it) })
        append(") DO UPDATE SET\n")
        append(statement.updateColumns.joinToString(",\n") {
            "${renderIndent(1)}${quoteIdent(it)} = EXCLUDED.${quoteIdent(it)}"
        })
    }

    private fun renderPrettySqliteUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append("INSERT OR REPLACE INTO ")
        append(renderCompactTable(statement.table))
        append(" ")
        append(renderColumnList(statement.columns))
        append("\nVALUES (")
        append(statement.values.joinToString(", ") { renderExprPretty(it) })
        append(")")
    }

    private fun renderColumnList(columns: List<String>): String =
        columns.joinToString(", ", "(", ")") { quoteIdent(it) }

    private fun renderExprPretty(expr: SqlExpr): String =
        compactRenderer.renderExpr(expr)

    private fun renderCompactTable(table: SqlTable): String =
        compactRenderer.renderTable(table)

    private fun renderIndent(depth: Int): String =
        indent.repeat(depth)
}

fun SqlStatement.toPrettySql(dialect: SqlDialect = SqlDialect.Standard): String =
    PrettySqlRenderer(dialect).renderStatement(this)

fun SqlQuery.toPrettySql(dialect: SqlDialect = SqlDialect.Standard): String =
    PrettySqlRenderer(dialect).renderQuery(this)
