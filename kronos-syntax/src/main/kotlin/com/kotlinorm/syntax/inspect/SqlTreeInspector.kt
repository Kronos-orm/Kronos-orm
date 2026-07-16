/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.inspect

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.expr.SqlWindow
import com.kotlinorm.syntax.expr.SqlWindowFrame
import com.kotlinorm.syntax.expr.SqlWindowFrameBound
import com.kotlinorm.syntax.expr.SqlWindowItem
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlReturning
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.statement.SqlTableConstraint
import com.kotlinorm.syntax.statement.SqlUpsertAction
import com.kotlinorm.syntax.table.SqlGraphPatternTerm
import com.kotlinorm.syntax.table.SqlGraphQuantifier
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlJsonColumn
import com.kotlinorm.syntax.table.SqlMatchRecognize
import com.kotlinorm.syntax.table.SqlRowPattern
import com.kotlinorm.syntax.table.SqlRowPatternQuantifier
import com.kotlinorm.syntax.table.SqlRowPatternTerm
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.token.SqlUnsafeToken

fun interface SqlNodeVisitor {
    fun visit(node: SqlNode)
}

object SqlNodeWalker {
    fun walk(node: SqlNode, visitor: SqlNodeVisitor) {
        visitor.visit(node)
        when (node) {
            is SqlStatement -> walkStatement(node, visitor)
            is SqlTable -> walkTable(node, visitor)
            is SqlExpr -> walkExpr(node, visitor)
            is SqlGroup -> node.items.forEach { walkGroupingItem(it, visitor) }
            is SqlLimit -> {
                node.offset?.let { walk(it, visitor) }
                node.fetch?.let { walk(it.limit, visitor) }
            }
            is SqlOrderingItem -> walk(node.expr, visitor)
            is SqlSelectItem -> walkSelectItem(node, visitor)
            is SqlColumnDefinition -> node.defaultValue?.let { walk(it, visitor) }
            is SqlTableConstraint -> walkConstraint(node, visitor)
            is SqlWindow -> walkWindow(node, visitor)
            is SqlWindowItem -> walkWindow(node.window, visitor)
        }
    }

    private fun walkStatement(statement: SqlStatement, visitor: SqlNodeVisitor) {
        when (statement) {
            is SqlQuery -> walkQuery(statement, visitor)
            is SqlDmlStatement -> walkDml(statement, visitor)
            is SqlDdlStatement -> walkDdl(statement, visitor)
        }
    }

    private fun walkQuery(query: SqlQuery, visitor: SqlNodeVisitor) {
        when (query) {
            is SqlQuery.Select -> {
                query.select.forEach { walkSelectItem(it, visitor) }
                query.from.forEach { walk(it, visitor) }
                query.where?.let { walk(it, visitor) }
                query.groupBy?.let { walk(it, visitor) }
                query.having?.let { walk(it, visitor) }
                query.window.forEach { walkWindowItem(it, visitor) }
                query.qualify?.let { walk(it, visitor) }
                query.orderBy.forEach { walk(it, visitor) }
                query.limit?.let { walk(it, visitor) }
            }
            is SqlQuery.Set -> {
                walk(query.left, visitor)
                walk(query.right, visitor)
                query.orderBy.forEach { walk(it, visitor) }
                query.limit?.let { walk(it, visitor) }
            }
            is SqlQuery.Values -> query.values.flatten().forEach { walk(it, visitor) }
            is SqlQuery.With -> {
                query.withItems.forEach { walk(it.query, visitor) }
                walk(query.query, visitor)
            }
        }
    }

    private fun walkDml(statement: SqlDmlStatement, visitor: SqlNodeVisitor) {
        when (statement) {
            is SqlDmlStatement.Delete -> {
                walk(statement.table, visitor)
                statement.where?.let { walk(it, visitor) }
                statement.returning?.let { walkReturning(it, visitor) }
            }
            is SqlDmlStatement.Insert -> {
                walk(statement.table, visitor)
                when (val mode = statement.mode) {
                    is SqlInsertMode.Values -> mode.values.flatten().forEach { walk(it, visitor) }
                    is SqlInsertMode.Subquery -> walk(mode.query, visitor)
                }
                statement.returning?.let { walkReturning(it, visitor) }
            }
            is SqlDmlStatement.Update -> {
                walk(statement.table, visitor)
                statement.setPairs.forEach { walk(it.value, visitor) }
                statement.where?.let { walk(it, visitor) }
                statement.returning?.let { walkReturning(it, visitor) }
            }
            is SqlDmlStatement.Truncate -> walk(statement.table, visitor)
            is SqlDmlStatement.Upsert -> {
                walk(statement.table, visitor)
                statement.values.forEach { walk(it, visitor) }
                statement.conflictTarget.where?.let { walk(it, visitor) }
                val update = statement.action as? com.kotlinorm.syntax.statement.SqlUpsertAction.Update
                update?.setPairs?.forEach { walk(it.value, visitor) }
                update?.where?.let { walk(it, visitor) }
                statement.returning?.let { walkReturning(it, visitor) }
            }
        }
    }

    private fun walkDdl(statement: SqlDdlStatement, visitor: SqlNodeVisitor) {
        when (statement) {
            is SqlDdlStatement.CreateTable -> {
                statement.columns.forEach { walk(it, visitor) }
                statement.indexes.forEach { index -> index.columns.forEach { visitor.visit(it) } }
            }
            is SqlDdlStatement.CreateTableAsSelect -> walk(statement.query, visitor)
            is SqlDdlStatement.AlterTable.AddColumn -> walk(statement.column, visitor)
            is SqlDdlStatement.AlterTable.ModifyColumn -> walk(statement.column, visitor)
            is SqlDdlStatement.AlterTable.AlterColumnDefault -> statement.defaultValue?.let { walk(it, visitor) }
            is SqlDdlStatement.AddConstraint -> walk(statement.constraint, visitor)
            is SqlDdlStatement.Vacuum -> statement.into?.let { walk(it, visitor) }
            is SqlDdlStatement.AlterTable.DropColumn,
            is SqlDdlStatement.AlterTable.RenameColumn,
            is SqlDdlStatement.AlterTable.RenameTable,
            is SqlDdlStatement.AlterTable.AlterColumnNullable,
            is SqlDdlStatement.AlterTable.SetTableComment,
            is SqlDdlStatement.DropTable,
            is SqlDdlStatement.CreateIndex,
            is SqlDdlStatement.DropIndex,
            is SqlDdlStatement.DropConstraint,
            is SqlDdlStatement.CommentOnTable,
            is SqlDdlStatement.CommentOnColumn,
            is SqlDdlStatement.SqlServerExtendedPropertyComment,
            is SqlDdlStatement.SqlServerDropDefaultConstraint -> {}
        }
    }

    private fun walkTable(table: SqlTable, visitor: SqlNodeVisitor) {
        when (table) {
            is SqlTable.Ident -> table.matchRecognize?.let { walkMatchRecognize(it, visitor) }
            is SqlTable.Func -> {
                table.args.forEach { walk(it, visitor) }
                table.matchRecognize?.let { walkMatchRecognize(it, visitor) }
            }
            is SqlTable.Subquery -> {
                walk(table.query, visitor)
                table.matchRecognize?.let { walkMatchRecognize(it, visitor) }
            }
            is SqlTable.Join -> {
                walk(table.left, visitor)
                walk(table.right, visitor)
                when (val condition = table.condition) {
                    is SqlJoinCondition.On -> walk(condition.condition, visitor)
                    is SqlJoinCondition.Using,
                    null -> {}
                }
            }
            is SqlTable.Json -> {
                walk(table.expr, visitor)
                walk(table.path, visitor)
                table.passingItems.forEach { walk(it.expr, visitor) }
                table.columns.forEach { walkJsonColumn(it, visitor) }
                table.matchRecognize?.let { walkMatchRecognize(it, visitor) }
            }
            is SqlTable.Graph -> {
                table.patterns.forEach { walkGraphPatternTerm(it.term, visitor) }
                table.where?.let { walk(it, visitor) }
                table.columns.forEach { walkSelectItem(it, visitor) }
                table.matchRecognize?.let { walkMatchRecognize(it, visitor) }
            }
        }
    }

    private fun walkExpr(expr: SqlExpr, visitor: SqlNodeVisitor) {
        when (expr) {
            is SqlExpr.Tuple -> expr.items.forEach { walk(it, visitor) }
            is SqlExpr.Array -> expr.items.forEach { walk(it, visitor) }
            is SqlExpr.Unary -> walk(expr.expr, visitor)
            is SqlExpr.Binary -> {
                walk(expr.left, visitor)
                walk(expr.right, visitor)
            }
            is SqlExpr.JsonTest -> walk(expr.expr, visitor)
            is SqlExpr.In -> {
                walk(expr.expr, visitor)
                when (val operand = expr.`in`) {
                    is SqlInRightOperand.Values -> operand.items.forEach { walk(it, visitor) }
                    is SqlInRightOperand.Subquery -> walk(operand.query, visitor)
                }
            }
            is SqlExpr.Between -> {
                walk(expr.expr, visitor)
                walk(expr.start, visitor)
                walk(expr.end, visitor)
            }
            is SqlExpr.Like -> {
                walk(expr.expr, visitor)
                walk(expr.pattern, visitor)
                expr.escape?.let { walk(it, visitor) }
            }
            is SqlExpr.SimilarTo -> {
                walk(expr.expr, visitor)
                walk(expr.pattern, visitor)
                expr.escape?.let { walk(it, visitor) }
            }
            is SqlExpr.Case -> {
                expr.branches.forEach {
                    walk(it.`when`, visitor)
                    walk(it.then, visitor)
                }
                expr.default?.let { walk(it, visitor) }
            }
            is SqlExpr.SimpleCase -> {
                walk(expr.expr, visitor)
                expr.branches.forEach {
                    walk(it.`when`, visitor)
                    walk(it.then, visitor)
                }
                expr.default?.let { walk(it, visitor) }
            }
            is SqlExpr.Coalesce -> expr.items.forEach { walk(it, visitor) }
            is SqlExpr.NullIf -> {
                walk(expr.expr, visitor)
                walk(expr.test, visitor)
            }
            is SqlExpr.Cast -> walk(expr.expr, visitor)
            is SqlExpr.Window -> {
                walk(expr.expr, visitor)
                walkWindow(expr.window, visitor)
            }
            is SqlExpr.Subquery -> walk(expr.query, visitor)
            is SqlExpr.ExistsPredicate -> walk(expr.query, visitor)
            is SqlExpr.QuantifiedComparisonPredicate -> {
                walk(expr.expr, visitor)
                walk(expr.query, visitor)
            }
            is SqlExpr.Grouping -> expr.items.forEach { walk(it, visitor) }
            is SqlExpr.Function -> {
                expr.args.forEach { walk(it, visitor) }
                expr.orderBy.forEach { walk(it, visitor) }
                expr.withinGroup.forEach { walk(it, visitor) }
                expr.filter?.let { walk(it, visitor) }
            }
            is SqlExpr.SubstringFunc -> {
                walk(expr.expr, visitor)
                walk(expr.from, visitor)
                expr.`for`?.let { walk(it, visitor) }
            }
            is SqlExpr.TrimFunc -> {
                walk(expr.expr, visitor)
                expr.trim?.value?.let { walk(it, visitor) }
            }
            is SqlExpr.OverlayFunc -> {
                walk(expr.expr, visitor)
                walk(expr.placing, visitor)
                walk(expr.from, visitor)
                expr.`for`?.let { walk(it, visitor) }
            }
            is SqlExpr.PositionFunc -> {
                walk(expr.expr, visitor)
                walk(expr.inExpr, visitor)
            }
            is SqlExpr.ExtractFunc -> walk(expr.expr, visitor)
            is SqlExpr.JsonSerializeFunc -> walk(expr.expr, visitor)
            is SqlExpr.JsonParseFunc -> walk(expr.expr, visitor)
            is SqlExpr.JsonQueryFunc -> {
                walk(expr.expr, visitor)
                walk(expr.path, visitor)
                expr.passingItems.forEach { walk(it.expr, visitor) }
            }
            is SqlExpr.JsonValueFunc -> {
                walk(expr.expr, visitor)
                walk(expr.path, visitor)
                expr.passingItems.forEach { walk(it.expr, visitor) }
            }
            is SqlExpr.JsonObjectFunc -> expr.items.forEach {
                walk(it.key, visitor)
                walk(it.value, visitor)
            }
            is SqlExpr.JsonArrayFunc -> expr.items.forEach { walk(it.value, visitor) }
            is SqlExpr.JsonExistsFunc -> {
                walk(expr.expr, visitor)
                walk(expr.path, visitor)
                expr.passingItems.forEach { walk(it.expr, visitor) }
            }
            is SqlExpr.CountAsteriskFunc -> expr.filter?.let { walk(it, visitor) }
            is SqlExpr.JsonObjectAggFunc -> {
                walk(expr.item.key, visitor)
                walk(expr.item.value, visitor)
                expr.filter?.let { walk(it, visitor) }
            }
            is SqlExpr.JsonArrayAggFunc -> {
                walk(expr.item.value, visitor)
                expr.orderBy.forEach { walk(it, visitor) }
                expr.filter?.let { walk(it, visitor) }
            }
            is SqlExpr.ListAggFunc -> {
                walk(expr.expr, visitor)
                walk(expr.separator, visitor)
                expr.withinGroup.forEach { walk(it, visitor) }
                expr.filter?.let { walk(it, visitor) }
            }
            is SqlExpr.NullsTreatmentFunc -> expr.args.forEach { walk(it, visitor) }
            is SqlExpr.NthValueFunc -> {
                walk(expr.expr, visitor)
                walk(expr.row, visitor)
            }
            is SqlExpr.MatchPhase -> walk(expr.expr, visitor)
            is SqlExpr.UnsafeCustom -> expr.tokens.forEach {
                if (it is SqlUnsafeToken.Expr) walk(it.value, visitor)
            }
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
            is SqlExpr.UnsafeRaw -> {}
        }
    }

    private fun walkReturning(returning: SqlReturning, visitor: SqlNodeVisitor) {
        returning.items.forEach { walkSelectItem(it, visitor) }
    }

    private fun walkSelectItem(item: SqlSelectItem, visitor: SqlNodeVisitor) {
        if (item is SqlSelectItem.Expr) walk(item.expr, visitor)
    }

    private fun walkGroupingItem(item: SqlGroupingItem, visitor: SqlNodeVisitor) {
        when (item) {
            SqlGroupingItem.EmptyGroup -> {}
            is SqlGroupingItem.Expr -> walk(item.item, visitor)
            is SqlGroupingItem.Cube -> item.items.forEach { walk(it, visitor) }
            is SqlGroupingItem.Rollup -> item.items.forEach { walk(it, visitor) }
            is SqlGroupingItem.GroupingSets -> item.items.forEach { walkGroupingItem(it, visitor) }
        }
    }

    private fun walkWindow(window: SqlWindow, visitor: SqlNodeVisitor) {
        window.partitionBy.forEach { walk(it, visitor) }
        window.orderBy.forEach { walk(it, visitor) }
        when (val frame = window.frame) {
            is SqlWindowFrame.Start -> walkWindowFrameBound(frame.start, visitor)
            is SqlWindowFrame.Between -> {
                walkWindowFrameBound(frame.start, visitor)
                walkWindowFrameBound(frame.end, visitor)
            }
            null -> {}
        }
    }

    private fun walkWindowItem(item: SqlWindowItem, visitor: SqlNodeVisitor) {
        visitor.visit(item)
        walkWindow(item.window, visitor)
    }

    private fun walkWindowFrameBound(bound: SqlWindowFrameBound, visitor: SqlNodeVisitor) {
        when (bound) {
            is SqlWindowFrameBound.Preceding -> walk(bound.n, visitor)
            is SqlWindowFrameBound.Following -> walk(bound.n, visitor)
            SqlWindowFrameBound.CurrentRow,
            SqlWindowFrameBound.UnboundedFollowing,
            SqlWindowFrameBound.UnboundedPreceding -> {}
        }
    }

    private fun walkJsonColumn(column: SqlJsonColumn, visitor: SqlNodeVisitor) {
        when (column) {
            is SqlJsonColumn.Column -> column.path?.let { walk(it, visitor) }
            is SqlJsonColumn.Exists -> column.path?.let { walk(it, visitor) }
            is SqlJsonColumn.Nested -> {
                walk(column.path, visitor)
                column.columns.forEach { walkJsonColumn(it, visitor) }
            }
            is SqlJsonColumn.Ordinality -> {}
        }
    }

    private fun walkMatchRecognize(matchRecognize: SqlMatchRecognize, visitor: SqlNodeVisitor) {
        matchRecognize.partitionBy.forEach { walk(it, visitor) }
        matchRecognize.orderBy.forEach { walk(it, visitor) }
        matchRecognize.measures.forEach { walk(it.expr, visitor) }
        walkRowPattern(matchRecognize.rowPattern, visitor)
    }

    private fun walkRowPattern(pattern: SqlRowPattern, visitor: SqlNodeVisitor) {
        walkRowPatternTerm(pattern.pattern, visitor)
        pattern.define.forEach { walk(it.expr, visitor) }
    }

    private fun walkRowPatternTerm(term: SqlRowPatternTerm, visitor: SqlNodeVisitor) {
        term.quantifier?.let { walkRowPatternQuantifier(it, visitor) }
        when (term) {
            is SqlRowPatternTerm.Exclusion -> walkRowPatternTerm(term.term, visitor)
            is SqlRowPatternTerm.Permute -> term.terms.forEach { walkRowPatternTerm(it, visitor) }
            is SqlRowPatternTerm.Then -> {
                walkRowPatternTerm(term.left, visitor)
                walkRowPatternTerm(term.right, visitor)
            }
            is SqlRowPatternTerm.Or -> {
                walkRowPatternTerm(term.left, visitor)
                walkRowPatternTerm(term.right, visitor)
            }
            is SqlRowPatternTerm.Pattern,
            is SqlRowPatternTerm.Circumflex,
            is SqlRowPatternTerm.Dollar -> {}
        }
    }

    private fun walkRowPatternQuantifier(quantifier: SqlRowPatternQuantifier, visitor: SqlNodeVisitor) {
        when (quantifier) {
            is SqlRowPatternQuantifier.Between -> {
                quantifier.start?.let { walk(it, visitor) }
                quantifier.end?.let { walk(it, visitor) }
            }
            is SqlRowPatternQuantifier.Quantity -> walk(quantifier.quantity, visitor)
            is SqlRowPatternQuantifier.Asterisk,
            is SqlRowPatternQuantifier.Plus,
            is SqlRowPatternQuantifier.Question -> {}
        }
    }

    private fun walkGraphPatternTerm(term: SqlGraphPatternTerm, visitor: SqlNodeVisitor) {
        when (term) {
            is SqlGraphPatternTerm.Quantified -> {
                walkGraphPatternTerm(term.term, visitor)
                walkGraphQuantifier(term.quantifier, visitor)
            }
            is SqlGraphPatternTerm.Vertex -> term.where?.let { walk(it, visitor) }
            is SqlGraphPatternTerm.Edge -> term.where?.let { walk(it, visitor) }
            is SqlGraphPatternTerm.And -> {
                walkGraphPatternTerm(term.left, visitor)
                walkGraphPatternTerm(term.right, visitor)
            }
            is SqlGraphPatternTerm.Or -> {
                walkGraphPatternTerm(term.left, visitor)
                walkGraphPatternTerm(term.right, visitor)
            }
            is SqlGraphPatternTerm.Alternation -> {
                walkGraphPatternTerm(term.left, visitor)
                walkGraphPatternTerm(term.right, visitor)
            }
        }
    }

    private fun walkGraphQuantifier(quantifier: SqlGraphQuantifier, visitor: SqlNodeVisitor) {
        when (quantifier) {
            is SqlGraphQuantifier.Between -> {
                quantifier.start?.let { walk(it, visitor) }
                quantifier.end?.let { walk(it, visitor) }
            }
            is SqlGraphQuantifier.Quantity -> walk(quantifier.quantity, visitor)
            SqlGraphQuantifier.Asterisk,
            SqlGraphQuantifier.Plus,
            SqlGraphQuantifier.Question -> {}
        }
    }

    private fun walkConstraint(constraint: SqlTableConstraint, visitor: SqlNodeVisitor) {
        if (constraint is SqlTableConstraint.Check) walk(constraint.condition, visitor)
    }
}

object SqlParameterCollector {
    fun collectParameters(node: SqlNode): List<SqlParameter> = buildList {
        SqlNodeWalker.walk(node) { visited ->
            if (visited is SqlExpr.Parameter) add(visited.parameter)
        }
    }

    fun collectNamedParameters(node: SqlNode): List<String> =
        collectParameters(node).mapNotNull { (it as? SqlParameter.Named)?.name }

    fun collectListExpansionOccurrences(node: SqlNode): Set<Int> = buildSet {
        var occurrence = 0
        SqlNodeWalker.walk(node) { visited ->
            val parameter = visited as? SqlExpr.Parameter ?: return@walk
            if (parameter.parameter is SqlParameter.Named) {
                if (parameter.expandAsList) add(occurrence)
                occurrence++
            }
        }
    }
}

data class SqlQueryOutput(
    val outputName: String?,
    val alias: String?,
    val sourceColumn: SqlIdentifier?,
    val expression: SqlExpr?
)

object SqlStatementInspector {
    fun tableNameOrNull(statement: SqlStatement): SqlIdentifier? = when (statement) {
        is SqlDmlStatement.Delete -> statement.table.identifier
        is SqlDmlStatement.Insert -> statement.table.identifier
        is SqlDmlStatement.Update -> statement.table.identifier
        is SqlDmlStatement.Truncate -> statement.table.identifier
        is SqlDmlStatement.Upsert -> statement.table.identifier
        is SqlDdlStatement.CreateTable -> statement.tableName
        is SqlDdlStatement.CreateTableAsSelect -> statement.tableName
        is SqlDdlStatement.AlterTable -> statement.tableName
        is SqlDdlStatement.DropTable -> statement.tableName
        is SqlDdlStatement.CreateIndex -> statement.tableName
        is SqlDdlStatement.DropIndex -> statement.tableName
        is SqlDdlStatement.AddConstraint -> statement.tableName
        is SqlDdlStatement.DropConstraint -> statement.tableName
        is SqlDdlStatement.CommentOnTable -> statement.tableName
        is SqlDdlStatement.CommentOnColumn -> statement.tableName
        is SqlDdlStatement.SqlServerExtendedPropertyComment -> statement.tableName
        is SqlDdlStatement.SqlServerDropDefaultConstraint -> statement.tableName
        is SqlDdlStatement.Vacuum -> null
        is SqlQuery -> null
    }

    fun whereOrNull(statement: SqlStatement): SqlExpr? = when (statement) {
        is SqlQuery.Select -> statement.where
        is SqlDmlStatement.Delete -> statement.where
        is SqlDmlStatement.Update -> statement.where
        is SqlDmlStatement.Upsert -> {
            val updateWhere = (statement.action as? SqlUpsertAction.Update)?.where
            when {
                statement.conflictTarget.where == null -> updateWhere
                updateWhere == null -> statement.conflictTarget.where
                else -> SqlExpr.Binary(
                    statement.conflictTarget.where,
                    SqlBinaryOperator.And,
                    updateWhere
                )
            }
        }
        else -> null
    }

    fun queryOutputs(query: SqlQuery): List<SqlQueryOutput> = when (query) {
        is SqlQuery.Select -> query.select.mapIndexed { index, item ->
            when (item) {
                is SqlSelectItem.Asterisk -> SqlQueryOutput(item.qualifier?.canonical, item.qualifier?.canonical, null, null)
                is SqlSelectItem.Expr -> {
                    val source = item.expr as? SqlExpr.Column
                    val alias = item.alias
                    SqlQueryOutput(
                        outputName = alias ?: source?.identifier?.last ?: "expr$index",
                        alias = alias,
                        sourceColumn = source?.identifier,
                        expression = item.expr
                    )
                }
            }
        }
        is SqlQuery.With -> queryOutputs(query.query)
        is SqlQuery.Set -> queryOutputs(query.left)
        is SqlQuery.Values -> emptyList()
    }

    fun selectAliasRegistry(query: SqlQuery): Map<String, SqlSelectItem.Expr> =
        (query as? SqlQuery.Select)?.select.orEmpty()
            .filterIsInstance<SqlSelectItem.Expr>()
            .mapNotNull { item -> item.alias?.let { it to item } }
            .toMap()
}
