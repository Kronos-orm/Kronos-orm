/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlTimeType
import com.kotlinorm.syntax.expr.SqlTimeZoneMode
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.limit.SqlFetchMode
import com.kotlinorm.syntax.limit.SqlFetchUnit
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlNullsOrdering
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.syntax.token.SqlUnsafeToken

open class MysqlSqlRenderer(
    standardEscapeStrings: Boolean = false
) : StandardSqlRenderer(SqlDialect.MySql.copy(standardEscapeStrings = standardEscapeStrings)) {
    override fun renderUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append("INSERT INTO ")
        append(renderTable(statement.table))
        append(statement.columns.joinToString(", ", " (", ")") { quoteIdent(it) })
        append(" VALUES (")
        append(statement.values.joinToString(", ") { renderExpr(it) })
        append(") ON DUPLICATE KEY UPDATE ")
        append(statement.updateColumns.joinToString(", ") { "${quoteIdent(it)} = VALUES (${quoteIdent(it)})" })
    }

    override fun renderLimit(limit: SqlLimit): String = buildString {
        append("LIMIT ")
        append(limit.offset?.let { renderExpr(it) } ?: "0")
        append(", ")
        append(limit.fetch?.let { renderExpr(it.limit) } ?: Long.MAX_VALUE.toString())
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
            null,
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
    override fun renderUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append("INSERT INTO ")
        append(renderTable(statement.table))
        append(statement.columns.joinToString(", ", " (", ")") { quoteIdent(it) })
        append(" VALUES (")
        append(statement.values.joinToString(", ") { renderExpr(it) })
        append(") ON CONFLICT (")
        append(statement.primaryKeys.joinToString(", ") { quoteIdent(it) })
        append(") DO UPDATE SET ")
        append(statement.updateColumns.joinToString(", ") { "${quoteIdent(it)} = EXCLUDED.${quoteIdent(it)}" })
    }

    override fun renderLimit(limit: SqlLimit): String {
        val fetch = limit.fetch
        val standardMode = fetch != null && (fetch.unit != SqlFetchUnit.RowCount || fetch.mode != SqlFetchMode.Only)
        return if (standardMode) {
            super.renderLimit(limit)
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
        val function = SqlExpr.GeneralFunc(
            quantifier = expr.quantifier,
            name = "STRING_AGG",
            args = listOf(expr.expr, expr.separator),
            orderBy = expr.withinGroup,
            filter = expr.filter
        )
        return renderExpr(function)
    }
}

open class SqliteSqlRenderer : StandardSqlRenderer(SqlDialect.SQLite) {
    override fun renderUpsert(statement: SqlDmlStatement.Upsert): String = buildString {
        append("INSERT OR REPLACE INTO ")
        append(renderTable(statement.table))
        append(statement.columns.joinToString(", ", " (", ")") { quoteIdent(it) })
        append(" VALUES (")
        append(statement.values.joinToString(", ") { renderExpr(it) })
        append(")")
    }

    override fun renderLimit(limit: SqlLimit): String = buildString {
        append("LIMIT ")
        append(limit.fetch?.let { renderExpr(it.limit) } ?: Long.MAX_VALUE.toString())
        limit.offset?.let { append(" OFFSET ${renderExpr(it)}") }
    }

    override fun renderListAgg(expr: SqlExpr.ListAggFunc): String {
        val function = SqlExpr.GeneralFunc(
            quantifier = expr.quantifier,
            name = "GROUP_CONCAT",
            args = listOf(expr.expr, expr.separator),
            orderBy = expr.withinGroup,
            filter = expr.filter
        )
        return renderExpr(function)
    }
}

open class OracleSqlRenderer : StandardSqlRenderer(SqlDialect.Oracle) {
    override fun renderExpr(expr: SqlExpr): String = when (expr) {
        is SqlExpr.TimeLiteral -> when (expr.type) {
            is SqlTimeType.Timestamp -> when (expr.type.mode) {
                SqlTimeZoneMode.WithTimeZone -> renderExpr(
                    SqlExpr.GeneralFunc(
                        name = "TO_TIMESTAMP_TZ",
                        args = listOf(
                            SqlExpr.StringLiteral(expr.time),
                            SqlExpr.StringLiteral("YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM")
                        )
                    )
                )
                null,
                SqlTimeZoneMode.WithoutTimeZone -> renderExpr(
                    SqlExpr.GeneralFunc(
                        name = "TO_TIMESTAMP",
                        args = listOf(
                            SqlExpr.StringLiteral(expr.time),
                            SqlExpr.StringLiteral("YYYY-MM-DD HH24:MI:SS.FF9")
                        )
                    )
                )
            }
            SqlTimeType.Date -> renderExpr(
                SqlExpr.GeneralFunc(
                    name = "TO_DATE",
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

    override fun renderType(type: SqlType): String = when (type) {
        is SqlType.Varchar -> if (type.maxLength == null) "VARCHAR(4000)" else super.renderType(type)
        SqlType.Long -> "INT"
        else -> super.renderType(type)
    }

    override fun renderTableAlias(alias: SqlTableAlias): String = buildString {
        append(quoteIdent(alias.alias))
        if (alias.columnAliases.isNotEmpty()) {
            append(alias.columnAliases.joinToString(", ", "(", ")") { quoteIdent(it) })
        }
    }
}

open class SqlServerSqlRenderer : StandardSqlRenderer(SqlDialect.SqlServer) {
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
        val function = SqlExpr.GeneralFunc(
            quantifier = expr.quantifier,
            name = "STRING_AGG",
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
