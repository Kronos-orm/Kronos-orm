/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.inspect

import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlWindow
import com.kotlinorm.syntax.expr.SqlWindowFrame
import com.kotlinorm.syntax.expr.SqlWindowFrameBound
import com.kotlinorm.syntax.expr.SqlWindowItem
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.statement.SqlUpdateSetPair
import com.kotlinorm.syntax.statement.SqlUpsertAction
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.token.SqlUnsafeToken

interface SqlNodeRewriter {
    fun rewriteStatement(statement: SqlStatement): SqlStatement =
        SqlTreeRewriter.rewriteStatement(statement, this)

    fun rewriteQuery(query: SqlQuery): SqlQuery =
        SqlTreeRewriter.rewriteQuery(query, this)

    fun rewriteTable(table: SqlTable): SqlTable =
        SqlTreeRewriter.rewriteTable(table, this)

    fun rewriteExpr(expr: SqlExpr): SqlExpr =
        SqlTreeRewriter.rewriteExpr(expr, this)
}

object SqlTreeRewriter {
    fun rewriteStatement(statement: SqlStatement, rewriter: SqlNodeRewriter): SqlStatement = when (statement) {
        is SqlQuery -> rewriteQuery(statement, rewriter)
        is SqlDmlStatement -> rewriteDml(statement, rewriter)
        else -> statement
    }

    fun rewriteQuery(query: SqlQuery, rewriter: SqlNodeRewriter): SqlQuery = when (query) {
        is SqlQuery.Select -> query.copy(
            from = query.from.map { rewriteTable(it, rewriter) },
            where = query.where?.let { rewriter.rewriteExpr(it) },
            groupBy = query.groupBy,
            having = query.having?.let { rewriter.rewriteExpr(it) },
            window = query.window.map { rewriteWindowItem(it, rewriter) },
            qualify = query.qualify?.let { rewriter.rewriteExpr(it) },
            orderBy = query.orderBy.map { it.copy(expr = rewriter.rewriteExpr(it.expr)) },
            limit = query.limit?.let { limit ->
                limit.copy(
                    offset = limit.offset?.let { rewriter.rewriteExpr(it) },
                    fetch = limit.fetch?.copy(limit = rewriter.rewriteExpr(limit.fetch.limit))
                )
            },
            select = query.select.map { item ->
                when (item) {
                    is com.kotlinorm.syntax.statement.SqlSelectItem.Expr -> item.copy(expr = rewriter.rewriteExpr(item.expr))
                    is com.kotlinorm.syntax.statement.SqlSelectItem.Asterisk -> item
                }
            }
        )
        is SqlQuery.Set -> query.copy(
            left = rewriter.rewriteQuery(query.left),
            right = rewriter.rewriteQuery(query.right),
            orderBy = query.orderBy.map { it.copy(expr = rewriter.rewriteExpr(it.expr)) },
            limit = query.limit?.let { limit ->
                limit.copy(
                    offset = limit.offset?.let { rewriter.rewriteExpr(it) },
                    fetch = limit.fetch?.copy(limit = rewriter.rewriteExpr(limit.fetch.limit))
                )
            }
        )
        is SqlQuery.Values -> query.copy(values = query.values.map { row -> row.map { rewriter.rewriteExpr(it) } })
        is SqlQuery.With -> query.copy(
            withItems = query.withItems.map { it.copy(query = rewriter.rewriteQuery(it.query)) },
            query = rewriter.rewriteQuery(query.query)
        )
    }

    fun rewriteTable(table: SqlTable, rewriter: SqlNodeRewriter): SqlTable = when (table) {
        is SqlTable.Func -> table.copy(args = table.args.map { rewriter.rewriteExpr(it) })
        is SqlTable.Subquery -> table.copy(query = rewriter.rewriteQuery(table.query))
        is SqlTable.Join -> table.copy(
            left = rewriter.rewriteTable(table.left),
            right = rewriter.rewriteTable(table.right),
            condition = when (val condition = table.condition) {
                is SqlJoinCondition.On -> condition.copy(condition = rewriter.rewriteExpr(condition.condition))
                is SqlJoinCondition.Using,
                null -> condition
            }
        )
        is SqlTable.Json -> table.copy(
            expr = rewriter.rewriteExpr(table.expr),
            path = rewriter.rewriteExpr(table.path),
            passingItems = table.passingItems.map { it.copy(expr = rewriter.rewriteExpr(it.expr)) }
        )
        is SqlTable.Graph -> table.copy(where = table.where?.let { rewriter.rewriteExpr(it) })
        is SqlTable.Ident -> table
    }

    fun rewriteExpr(expr: SqlExpr, rewriter: SqlNodeRewriter): SqlExpr = when (expr) {
        is SqlExpr.Tuple -> expr.copy(items = expr.items.map { rewriter.rewriteExpr(it) })
        is SqlExpr.Array -> expr.copy(items = expr.items.map { rewriter.rewriteExpr(it) })
        is SqlExpr.Unary -> expr.copy(expr = rewriter.rewriteExpr(expr.expr))
        is SqlExpr.Binary -> expr.copy(left = rewriter.rewriteExpr(expr.left), right = rewriter.rewriteExpr(expr.right))
        is SqlExpr.JsonTest -> expr.copy(expr = rewriter.rewriteExpr(expr.expr))
        is SqlExpr.In -> expr.copy(
            expr = rewriter.rewriteExpr(expr.expr),
            `in` = when (val operand = expr.`in`) {
                is SqlInRightOperand.Values -> operand.copy(items = operand.items.map { rewriter.rewriteExpr(it) })
                is SqlInRightOperand.Subquery -> operand.copy(query = rewriter.rewriteQuery(operand.query))
            }
        )
        is SqlExpr.Between -> expr.copy(
            expr = rewriter.rewriteExpr(expr.expr),
            start = rewriter.rewriteExpr(expr.start),
            end = rewriter.rewriteExpr(expr.end)
        )
        is SqlExpr.Like -> expr.copy(
            expr = rewriter.rewriteExpr(expr.expr),
            pattern = rewriter.rewriteExpr(expr.pattern),
            escape = expr.escape?.let { rewriter.rewriteExpr(it) }
        )
        is SqlExpr.SimilarTo -> expr.copy(
            expr = rewriter.rewriteExpr(expr.expr),
            pattern = rewriter.rewriteExpr(expr.pattern),
            escape = expr.escape?.let { rewriter.rewriteExpr(it) }
        )
        is SqlExpr.Case -> expr.copy(
            branches = expr.branches.map { it.copy(`when` = rewriter.rewriteExpr(it.`when`), then = rewriter.rewriteExpr(it.then)) },
            default = expr.default?.let { rewriter.rewriteExpr(it) }
        )
        is SqlExpr.SimpleCase -> expr.copy(
            expr = rewriter.rewriteExpr(expr.expr),
            branches = expr.branches.map { it.copy(`when` = rewriter.rewriteExpr(it.`when`), then = rewriter.rewriteExpr(it.then)) },
            default = expr.default?.let { rewriter.rewriteExpr(it) }
        )
        is SqlExpr.Coalesce -> expr.copy(items = expr.items.map { rewriter.rewriteExpr(it) })
        is SqlExpr.NullIf -> expr.copy(expr = rewriter.rewriteExpr(expr.expr), test = rewriter.rewriteExpr(expr.test))
        is SqlExpr.Cast -> expr.copy(expr = rewriter.rewriteExpr(expr.expr))
        is SqlExpr.Window -> expr.copy(expr = rewriter.rewriteExpr(expr.expr), window = rewriteWindow(expr.window, rewriter))
        is SqlExpr.Subquery -> expr.copy(query = rewriter.rewriteQuery(expr.query))
        is SqlExpr.ExistsPredicate -> expr.copy(query = rewriter.rewriteQuery(expr.query))
        is SqlExpr.QuantifiedComparisonPredicate -> expr.copy(expr = rewriter.rewriteExpr(expr.expr), query = rewriter.rewriteQuery(expr.query))
        is SqlExpr.Grouping -> expr.copy(items = expr.items.map { rewriter.rewriteExpr(it) })
        is SqlExpr.Function -> expr.copy(
            args = expr.args.map { rewriter.rewriteExpr(it) },
            orderBy = expr.orderBy.map { it.copy(expr = rewriter.rewriteExpr(it.expr)) },
            withinGroup = expr.withinGroup.map { it.copy(expr = rewriter.rewriteExpr(it.expr)) },
            filter = expr.filter?.let { rewriter.rewriteExpr(it) }
        )
        is SqlExpr.SubstringFunc -> expr.copy(
            expr = rewriter.rewriteExpr(expr.expr),
            from = rewriter.rewriteExpr(expr.from),
            `for` = expr.`for`?.let { rewriter.rewriteExpr(it) }
        )
        is SqlExpr.TrimFunc -> expr.copy(expr = rewriter.rewriteExpr(expr.expr), trim = expr.trim?.copy(value = expr.trim.value?.let { rewriter.rewriteExpr(it) }))
        is SqlExpr.OverlayFunc -> expr.copy(
            expr = rewriter.rewriteExpr(expr.expr),
            placing = rewriter.rewriteExpr(expr.placing),
            from = rewriter.rewriteExpr(expr.from),
            `for` = expr.`for`?.let { rewriter.rewriteExpr(it) }
        )
        is SqlExpr.PositionFunc -> expr.copy(expr = rewriter.rewriteExpr(expr.expr), inExpr = rewriter.rewriteExpr(expr.inExpr))
        is SqlExpr.ExtractFunc -> expr.copy(expr = rewriter.rewriteExpr(expr.expr))
        is SqlExpr.JsonSerializeFunc -> expr.copy(expr = rewriter.rewriteExpr(expr.expr))
        is SqlExpr.JsonParseFunc -> expr.copy(expr = rewriter.rewriteExpr(expr.expr))
        is SqlExpr.JsonQueryFunc -> expr.copy(expr = rewriter.rewriteExpr(expr.expr), path = rewriter.rewriteExpr(expr.path))
        is SqlExpr.JsonValueFunc -> expr.copy(expr = rewriter.rewriteExpr(expr.expr), path = rewriter.rewriteExpr(expr.path))
        is SqlExpr.JsonObjectFunc -> expr.copy(items = expr.items.map { it.copy(key = rewriter.rewriteExpr(it.key), value = rewriter.rewriteExpr(it.value)) })
        is SqlExpr.JsonArrayFunc -> expr.copy(items = expr.items.map { it.copy(value = rewriter.rewriteExpr(it.value)) })
        is SqlExpr.JsonExistsFunc -> expr.copy(expr = rewriter.rewriteExpr(expr.expr), path = rewriter.rewriteExpr(expr.path))
        is SqlExpr.CountAsteriskFunc -> expr.copy(filter = expr.filter?.let { rewriter.rewriteExpr(it) })
        is SqlExpr.JsonObjectAggFunc -> expr.copy(
            item = expr.item.copy(key = rewriter.rewriteExpr(expr.item.key), value = rewriter.rewriteExpr(expr.item.value)),
            filter = expr.filter?.let { rewriter.rewriteExpr(it) }
        )
        is SqlExpr.JsonArrayAggFunc -> expr.copy(
            item = expr.item.copy(value = rewriter.rewriteExpr(expr.item.value)),
            orderBy = expr.orderBy.map { it.copy(expr = rewriter.rewriteExpr(it.expr)) },
            filter = expr.filter?.let { rewriter.rewriteExpr(it) }
        )
        is SqlExpr.ListAggFunc -> expr.copy(
            expr = rewriter.rewriteExpr(expr.expr),
            separator = rewriter.rewriteExpr(expr.separator),
            withinGroup = expr.withinGroup.map { it.copy(expr = rewriter.rewriteExpr(it.expr)) },
            filter = expr.filter?.let { rewriter.rewriteExpr(it) }
        )
        is SqlExpr.NullsTreatmentFunc -> expr.copy(args = expr.args.map { rewriter.rewriteExpr(it) })
        is SqlExpr.NthValueFunc -> expr.copy(expr = rewriter.rewriteExpr(expr.expr), row = rewriter.rewriteExpr(expr.row))
        is SqlExpr.MatchPhase -> expr.copy(expr = rewriter.rewriteExpr(expr.expr))
        is SqlExpr.UnsafeCustom -> expr.copy(tokens = expr.tokens.map { if (it is SqlUnsafeToken.Expr) it.copy(value = rewriter.rewriteExpr(it.value)) else it })
        SqlExpr.NullLiteral,
        is SqlExpr.Column,
        is SqlExpr.StringLiteral,
        is SqlExpr.NumberLiteral,
        is SqlExpr.BooleanLiteral,
        is SqlExpr.TimeLiteral,
        is SqlExpr.IntervalLiteral,
        is SqlExpr.Parameter,
        is SqlExpr.IdentFunc,
        is SqlExpr.ExcludedColumn,
        is SqlExpr.SourceColumn,
        is SqlExpr.UnsafeRaw -> expr
    }

    private fun rewriteDml(statement: SqlDmlStatement, rewriter: SqlNodeRewriter): SqlDmlStatement = when (statement) {
        is SqlDmlStatement.Delete -> statement.copy(where = statement.where?.let { rewriter.rewriteExpr(it) })
        is SqlDmlStatement.Insert -> statement.copy(mode = when (val mode = statement.mode) {
            is SqlInsertMode.Values -> mode.copy(values = mode.values.map { row -> row.map { rewriter.rewriteExpr(it) } })
            is SqlInsertMode.Subquery -> mode.copy(query = rewriter.rewriteQuery(mode.query))
        })
        is SqlDmlStatement.Update -> statement.copy(
            setPairs = statement.setPairs.map { SqlUpdateSetPair(it.target, rewriter.rewriteExpr(it.value)) },
            where = statement.where?.let { rewriter.rewriteExpr(it) }
        )
        is SqlDmlStatement.Upsert -> statement.copy(
            values = statement.values.map { rewriter.rewriteExpr(it) },
            conflictTarget = statement.conflictTarget.copy(where = statement.conflictTarget.where?.let { rewriter.rewriteExpr(it) }),
            action = when (val action = statement.action) {
                SqlUpsertAction.DoNothing -> action
                is SqlUpsertAction.Update -> action.copy(
                    setPairs = action.setPairs.map { SqlUpdateSetPair(it.target, rewriter.rewriteExpr(it.value)) },
                    where = action.where?.let { rewriter.rewriteExpr(it) }
                )
            }
        )
        is SqlDmlStatement.Truncate -> statement
    }

    private fun rewriteWindow(window: SqlWindow, rewriter: SqlNodeRewriter): SqlWindow =
        window.copy(
            partitionBy = window.partitionBy.map { rewriter.rewriteExpr(it) },
            orderBy = window.orderBy.map { it.copy(expr = rewriter.rewriteExpr(it.expr)) },
            frame = window.frame?.let { rewriteWindowFrame(it, rewriter) }
        )

    private fun rewriteWindowItem(item: SqlWindowItem, rewriter: SqlNodeRewriter): SqlWindowItem =
        item.copy(window = rewriteWindow(item.window, rewriter))

    private fun rewriteWindowFrame(frame: SqlWindowFrame, rewriter: SqlNodeRewriter): SqlWindowFrame = when (frame) {
        is SqlWindowFrame.Start -> frame.copy(start = rewriteWindowFrameBound(frame.start, rewriter))
        is SqlWindowFrame.Between -> frame.copy(
            start = rewriteWindowFrameBound(frame.start, rewriter),
            end = rewriteWindowFrameBound(frame.end, rewriter)
        )
    }

    private fun rewriteWindowFrameBound(bound: SqlWindowFrameBound, rewriter: SqlNodeRewriter): SqlWindowFrameBound = when (bound) {
        is SqlWindowFrameBound.Preceding -> bound.copy(n = rewriter.rewriteExpr(bound.n))
        is SqlWindowFrameBound.Following -> bound.copy(n = rewriter.rewriteExpr(bound.n))
        SqlWindowFrameBound.CurrentRow,
        SqlWindowFrameBound.UnboundedFollowing,
        SqlWindowFrameBound.UnboundedPreceding -> bound
    }
}
