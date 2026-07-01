/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.validate

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlJsonArrayItem
import com.kotlinorm.syntax.expr.SqlJsonObjectItem
import com.kotlinorm.syntax.expr.SqlJsonPassing
import com.kotlinorm.syntax.expr.SqlJsonQueryEmptyBehavior
import com.kotlinorm.syntax.expr.SqlJsonQueryErrorBehavior
import com.kotlinorm.syntax.expr.SqlJsonValueEmptyBehavior
import com.kotlinorm.syntax.expr.SqlJsonValueErrorBehavior
import com.kotlinorm.syntax.expr.SqlListAggOnOverflow
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.expr.SqlWindow
import com.kotlinorm.syntax.expr.SqlWindowFrame
import com.kotlinorm.syntax.expr.SqlWindowFrameBound
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlIndexDefinition
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlPrimaryKeyMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlGraphPatternTerm
import com.kotlinorm.syntax.table.SqlGraphQuantifier
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlJsonColumn
import com.kotlinorm.syntax.table.SqlMatchRecognize
import com.kotlinorm.syntax.table.SqlRowPattern
import com.kotlinorm.syntax.table.SqlRowPatternQuantifier
import com.kotlinorm.syntax.table.SqlRowPatternSkipMode
import com.kotlinorm.syntax.table.SqlRowPatternSubsetItem
import com.kotlinorm.syntax.table.SqlRowPatternTerm
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.token.SqlUnsafeToken

object SqlSyntaxValidator {
    fun validate(node: SqlNode): List<SqlValidationDiagnostic> {
        val diagnostics = mutableListOf<SqlValidationDiagnostic>()
        validateNode(node, diagnostics)
        return diagnostics
    }

    fun validateOrThrow(node: SqlNode) {
        val errors = validate(node).filter { it.severity == SqlValidationSeverity.Error }
        if (errors.isNotEmpty()) {
            throw SqlValidationException(errors)
        }
    }

    private fun validateNode(node: SqlNode, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (node) {
            is SqlQuery -> validateQuery(node, diagnostics)
            is SqlStatement -> validateStatement(node, diagnostics)
            is SqlTable -> validateTable(node, diagnostics)
            is SqlExpr -> validateExpr(node, diagnostics)
        }
    }

    private fun validateStatement(statement: SqlStatement, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (statement) {
            is SqlQuery -> validateQuery(statement, diagnostics)
            is SqlDmlStatement -> validateDml(statement, diagnostics)
            is SqlDdlStatement -> validateDdl(statement, diagnostics)
        }
    }

    private fun validateDml(statement: SqlDmlStatement, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (statement) {
            is SqlDmlStatement.Insert -> {
                validateTable(statement.table, diagnostics)
                validateDuplicateNames("insert.columns.duplicate", "INSERT column", statement.columns, diagnostics)
                validateInsertMode(statement.columns, statement.mode, diagnostics)
            }
            is SqlDmlStatement.Update -> {
                validateTable(statement.table, diagnostics)
                validateDuplicateNames("update.set.duplicate", "UPDATE SET column", statement.setPairs.map { it.column }, diagnostics)
                statement.setPairs.forEach { validateExpr(it.value, diagnostics) }
                statement.where?.let { validateExpr(it, diagnostics) }
            }
            is SqlDmlStatement.Delete -> {
                validateTable(statement.table, diagnostics)
                statement.where?.let { validateExpr(it, diagnostics) }
            }
            is SqlDmlStatement.Truncate -> validateTable(statement.table, diagnostics)
            is SqlDmlStatement.Upsert -> validateUpsert(statement, diagnostics)
        }
    }

    private fun validateDdl(statement: SqlDdlStatement, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (statement) {
            is SqlDdlStatement.CreateTable -> {
                val columnNames = statement.columns.map { it.name }
                validateDuplicateNames("ddl.column.duplicate", "CREATE TABLE column", columnNames, diagnostics)
                statement.columns.forEach { validateColumnDefinition(it, diagnostics) }
                statement.indexes.forEach { validateIndexDefinition(it, columnNames.toSet(), diagnostics) }
                val primaryColumns = statement.columns.filter { it.primaryKey != SqlPrimaryKeyMode.NotPrimary }
                if (primaryColumns.size > 1) {
                    diagnostics.error(
                        "ddl.primary.multiple",
                        "CREATE TABLE has multiple primary-key columns; composite primary keys are not modelled by this node."
                    )
                }
            }
            is SqlDdlStatement.CreateTableAsSelect -> validateQuery(statement.query, diagnostics)
            is SqlDdlStatement.AlterTable.AddColumn -> validateColumnDefinition(statement.column, diagnostics)
            is SqlDdlStatement.AlterTable.ModifyColumn -> validateColumnDefinition(statement.column, diagnostics)
            is SqlDdlStatement.AlterTable.DropColumn -> {}
            is SqlDdlStatement.DropTable -> {}
            is SqlDdlStatement.CreateIndex -> validateDuplicateNames(
                "ddl.index.column.duplicate",
                "CREATE INDEX column",
                statement.columns,
                diagnostics
            )
            is SqlDdlStatement.DropIndex -> {}
        }
    }

    private fun validateQuery(query: SqlQuery, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (query) {
            is SqlQuery.Select -> {
                query.select.forEach {
                    if (it is SqlSelectItem.Expr) validateExpr(it.expr, diagnostics)
                }
                query.from.forEach { validateTable(it, diagnostics) }
                query.where?.let { validateExpr(it, diagnostics) }
                query.groupBy?.let { validateGroup(it, diagnostics) }
                query.having?.let {
                    validateExpr(it, diagnostics)
                    if (query.groupBy == null) {
                        diagnostics.warning("select.having.without.group", "HAVING is present without GROUP BY.")
                    }
                }
                validateOrdering(query.orderBy, diagnostics)
                query.limit?.let { validateLimit(it, "select", diagnostics) }
                if (query.limit?.fetch?.mode == com.kotlinorm.syntax.limit.SqlFetchMode.WithTies && query.orderBy.isEmpty()) {
                    diagnostics.error("select.fetch.with.ties.without.order", "FETCH WITH TIES requires ORDER BY for deterministic ties.")
                }
            }
            is SqlQuery.Set -> {
                validateQuery(query.left, diagnostics)
                validateQuery(query.right, diagnostics)
                validateOrdering(query.orderBy, diagnostics)
                query.limit?.let { validateLimit(it, "set", diagnostics) }
                if (query.limit?.fetch?.mode == com.kotlinorm.syntax.limit.SqlFetchMode.WithTies && query.orderBy.isEmpty()) {
                    diagnostics.error("set.fetch.with.ties.without.order", "FETCH WITH TIES on a set query requires ORDER BY.")
                }
            }
            is SqlQuery.Values -> {
                validateRowsArity("values.row.arity", "VALUES query", query.values, diagnostics)
                query.values.flatten().forEach { validateExpr(it, diagnostics) }
            }
            is SqlQuery.With -> {
                validateDuplicateNames("with.duplicate.name", "CTE", query.withItems.map { it.name }, diagnostics)
                query.withItems.forEach { item ->
                    validateDuplicateNames("with.column.duplicate", "CTE column", item.columnNames, diagnostics)
                    validateQuery(item.query, diagnostics)
                }
                validateQuery(query.query, diagnostics)
            }
        }
    }

    private fun validateInsertMode(
        columns: List<String>,
        mode: SqlInsertMode,
        diagnostics: MutableList<SqlValidationDiagnostic>
    ) {
        when (mode) {
            is SqlInsertMode.Values -> {
                if (columns.isNotEmpty()) {
                    mode.values.forEachIndexed { index, row ->
                        if (row.size != columns.size) {
                            diagnostics.error(
                                "insert.values.arity",
                                "INSERT row $index has ${row.size} values but ${columns.size} columns."
                            )
                        }
                    }
                } else {
                    validateRowsArity("insert.values.row.arity", "INSERT VALUES", mode.values, diagnostics)
                }
                mode.values.flatten().forEach { validateExpr(it, diagnostics) }
            }
            is SqlInsertMode.Subquery -> validateQuery(mode.query, diagnostics)
        }
    }

    private fun validateUpsert(
        statement: SqlDmlStatement.Upsert,
        diagnostics: MutableList<SqlValidationDiagnostic>
    ) {
        validateTable(statement.table, diagnostics)
        validateDuplicateNames("upsert.columns.duplicate", "UPSERT column", statement.columns, diagnostics)
        validateDuplicateNames("upsert.primary.duplicate", "UPSERT primary key", statement.primaryKeys, diagnostics)
        validateDuplicateNames("upsert.update.duplicate", "UPSERT update column", statement.updateColumns, diagnostics)
        if (statement.values.size != statement.columns.size) {
            diagnostics.error("upsert.values.arity", "UPSERT has ${statement.values.size} values but ${statement.columns.size} columns.")
        }
        if (statement.updateColumns.isEmpty()) {
            diagnostics.error("upsert.update.empty", "UPSERT requires at least one update column.")
        }
        val columnSet = statement.columns.toSet()
        (statement.primaryKeys + statement.updateColumns).forEach { column ->
            if (column !in columnSet) {
                diagnostics.error("upsert.column.unknown", "UPSERT references column `$column` that is not present in the insert columns.")
            }
        }
        statement.values.forEach { validateExpr(it, diagnostics) }
    }

    private fun validateTable(table: SqlTable, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (table) {
            is SqlTable.Ident -> table.matchRecognize?.let { validateMatchRecognize(it, diagnostics) }
            is SqlTable.Func -> {
                table.args.forEach { validateExpr(it, diagnostics) }
                table.matchRecognize?.let { validateMatchRecognize(it, diagnostics) }
            }
            is SqlTable.Subquery -> {
                validateQuery(table.query, diagnostics)
                table.matchRecognize?.let { validateMatchRecognize(it, diagnostics) }
            }
            is SqlTable.Join -> {
                validateTable(table.left, diagnostics)
                validateTable(table.right, diagnostics)
                if (table.joinType == SqlJoinType.Cross && table.condition != null) {
                    diagnostics.error("join.cross.condition", "CROSS JOIN must not have ON or USING condition.")
                }
                table.condition?.let {
                    if (it is SqlJoinCondition.On) validateExpr(it.condition, diagnostics)
                }
            }
            is SqlTable.Json -> {
                validateExpr(table.expr, diagnostics)
                validateExpr(table.path, diagnostics)
                table.passingItems.forEach { validateJsonPassing(it, diagnostics) }
                table.columns.forEach { validateJsonColumn(it, diagnostics) }
                table.matchRecognize?.let { validateMatchRecognize(it, diagnostics) }
            }
            is SqlTable.Graph -> {
                table.patterns.forEach { validateGraphPatternTerm(it.term, diagnostics) }
                table.where?.let { validateExpr(it, diagnostics) }
                table.columns.forEach {
                    if (it is SqlSelectItem.Expr) validateExpr(it.expr, diagnostics)
                }
                table.matchRecognize?.let { validateMatchRecognize(it, diagnostics) }
            }
        }
    }

    private fun validateExpr(expr: SqlExpr, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (expr) {
            is SqlExpr.Column,
            SqlExpr.NullLiteral,
            is SqlExpr.StringLiteral,
            is SqlExpr.NumberLiteral,
            is SqlExpr.BooleanLiteral,
            is SqlExpr.TimeLiteral,
            is SqlExpr.IntervalLiteral,
            is SqlExpr.Parameter,
            is SqlExpr.IdentFunc -> {}
            is SqlExpr.Tuple -> expr.items.forEach { validateExpr(it, diagnostics) }
            is SqlExpr.Array -> expr.items.forEach { validateExpr(it, diagnostics) }
            is SqlExpr.Unary -> validateExpr(expr.expr, diagnostics)
            is SqlExpr.Binary -> {
                validateExpr(expr.left, diagnostics)
                validateExpr(expr.right, diagnostics)
            }
            is SqlExpr.JsonTest -> validateExpr(expr.expr, diagnostics)
            is SqlExpr.In -> {
                validateExpr(expr.expr, diagnostics)
                when (val operand = expr.`in`) {
                    is SqlInRightOperand.Values -> operand.items.forEach { validateExpr(it, diagnostics) }
                    is SqlInRightOperand.Subquery -> validateQuery(operand.query, diagnostics)
                }
            }
            is SqlExpr.Between -> {
                validateExpr(expr.expr, diagnostics)
                validateExpr(expr.start, diagnostics)
                validateExpr(expr.end, diagnostics)
            }
            is SqlExpr.Like -> {
                validateExpr(expr.expr, diagnostics)
                validateExpr(expr.pattern, diagnostics)
                expr.escape?.let { validateExpr(it, diagnostics) }
            }
            is SqlExpr.SimilarTo -> {
                validateExpr(expr.expr, diagnostics)
                validateExpr(expr.pattern, diagnostics)
                expr.escape?.let { validateExpr(it, diagnostics) }
            }
            is SqlExpr.Case -> {
                expr.branches.forEach { branch ->
                    validateExpr(branch.`when`, diagnostics)
                    validateExpr(branch.then, diagnostics)
                }
                expr.default?.let { validateExpr(it, diagnostics) }
            }
            is SqlExpr.SimpleCase -> {
                validateExpr(expr.expr, diagnostics)
                expr.branches.forEach { branch ->
                    validateExpr(branch.`when`, diagnostics)
                    validateExpr(branch.then, diagnostics)
                }
                expr.default?.let { validateExpr(it, diagnostics) }
            }
            is SqlExpr.Coalesce -> expr.items.forEach { validateExpr(it, diagnostics) }
            is SqlExpr.NullIf -> {
                validateExpr(expr.expr, diagnostics)
                validateExpr(expr.test, diagnostics)
            }
            is SqlExpr.Cast -> {
                validateExpr(expr.expr, diagnostics)
                validateType(expr.type, diagnostics)
            }
            is SqlExpr.Window -> {
                validateExpr(expr.expr, diagnostics)
                validateWindow(expr.window, diagnostics)
            }
            is SqlExpr.Subquery -> validateQuery(expr.query, diagnostics)
            is SqlExpr.ExistsPredicate -> validateQuery(expr.query, diagnostics)
            is SqlExpr.QuantifiedComparisonPredicate -> {
                validateExpr(expr.expr, diagnostics)
                validateQuery(expr.query, diagnostics)
            }
            is SqlExpr.Grouping -> expr.items.forEach { validateExpr(it, diagnostics) }
            is SqlExpr.SubstringFunc -> {
                validateExpr(expr.expr, diagnostics)
                validateExpr(expr.from, diagnostics)
                expr.`for`?.let { validateExpr(it, diagnostics) }
            }
            is SqlExpr.TrimFunc -> {
                expr.trim?.value?.let { validateExpr(it, diagnostics) }
                validateExpr(expr.expr, diagnostics)
            }
            is SqlExpr.OverlayFunc -> {
                validateExpr(expr.expr, diagnostics)
                validateExpr(expr.placing, diagnostics)
                validateExpr(expr.from, diagnostics)
                expr.`for`?.let { validateExpr(it, diagnostics) }
            }
            is SqlExpr.PositionFunc -> {
                validateExpr(expr.expr, diagnostics)
                validateExpr(expr.inExpr, diagnostics)
            }
            is SqlExpr.ExtractFunc -> validateExpr(expr.expr, diagnostics)
            is SqlExpr.JsonSerializeFunc -> {
                validateExpr(expr.expr, diagnostics)
                expr.output?.let { validateType(it.type, diagnostics) }
            }
            is SqlExpr.JsonParseFunc -> validateExpr(expr.expr, diagnostics)
            is SqlExpr.JsonQueryFunc -> {
                validateExpr(expr.expr, diagnostics)
                validateExpr(expr.path, diagnostics)
                expr.passingItems.forEach { validateJsonPassing(it, diagnostics) }
                expr.output?.let { validateType(it.type, diagnostics) }
                expr.onEmpty?.let { validateJsonQueryEmptyBehavior(it, diagnostics) }
                expr.onError?.let { validateJsonQueryErrorBehavior(it, diagnostics) }
            }
            is SqlExpr.JsonValueFunc -> {
                validateExpr(expr.expr, diagnostics)
                validateExpr(expr.path, diagnostics)
                expr.passingItems.forEach { validateJsonPassing(it, diagnostics) }
                expr.output?.let { validateType(it.type, diagnostics) }
                expr.onEmpty?.let { validateJsonValueEmptyBehavior(it, diagnostics) }
                expr.onError?.let { validateJsonValueErrorBehavior(it, diagnostics) }
            }
            is SqlExpr.JsonObjectFunc -> {
                expr.items.forEach { validateJsonObjectItem(it, diagnostics) }
                expr.output?.let { validateType(it.type, diagnostics) }
            }
            is SqlExpr.JsonArrayFunc -> {
                expr.items.forEach { validateJsonArrayItem(it, diagnostics) }
                expr.output?.let { validateType(it.type, diagnostics) }
            }
            is SqlExpr.JsonExistsFunc -> {
                validateExpr(expr.expr, diagnostics)
                validateExpr(expr.path, diagnostics)
                expr.passingItems.forEach { validateJsonPassing(it, diagnostics) }
            }
            is SqlExpr.CountAsteriskFunc -> expr.filter?.let { validateExpr(it, diagnostics) }
            is SqlExpr.JsonObjectAggFunc -> {
                validateJsonObjectItem(expr.item, diagnostics)
                expr.output?.let { validateType(it.type, diagnostics) }
                expr.filter?.let { validateExpr(it, diagnostics) }
            }
            is SqlExpr.JsonArrayAggFunc -> {
                validateJsonArrayItem(expr.item, diagnostics)
                validateOrdering(expr.orderBy, diagnostics)
                expr.output?.let { validateType(it.type, diagnostics) }
                expr.filter?.let { validateExpr(it, diagnostics) }
            }
            is SqlExpr.ListAggFunc -> {
                validateExpr(expr.expr, diagnostics)
                validateExpr(expr.separator, diagnostics)
                expr.onOverflow?.let { validateListAggOverflow(it, diagnostics) }
                validateOrdering(expr.withinGroup, diagnostics)
                expr.filter?.let { validateExpr(it, diagnostics) }
            }
            is SqlExpr.NullsTreatmentFunc -> expr.args.forEach { validateExpr(it, diagnostics) }
            is SqlExpr.NthValueFunc -> {
                validateExpr(expr.expr, diagnostics)
                validateExpr(expr.row, diagnostics)
            }
            is SqlExpr.GeneralFunc -> {
                expr.args.forEach { validateExpr(it, diagnostics) }
                validateOrdering(expr.orderBy, diagnostics)
                validateOrdering(expr.withinGroup, diagnostics)
                expr.filter?.let { validateExpr(it, diagnostics) }
            }
            is SqlExpr.MatchPhase -> validateExpr(expr.expr, diagnostics)
            is SqlExpr.UnsafeCustom -> expr.tokens.forEach { validateUnsafeToken(it, diagnostics) }
        }
    }

    private fun validateMatchRecognize(matchRecognize: SqlMatchRecognize, diagnostics: MutableList<SqlValidationDiagnostic>) {
        matchRecognize.partitionBy.forEach { validateExpr(it, diagnostics) }
        validateOrdering(matchRecognize.orderBy, diagnostics)
        matchRecognize.measures.forEach { validateExpr(it.expr, diagnostics) }
        validateRowPattern(matchRecognize.rowPattern, diagnostics)
    }

    private fun validateRowPattern(pattern: SqlRowPattern, diagnostics: MutableList<SqlValidationDiagnostic>) {
        val patternNames = collectPatternNames(pattern.pattern)
        val defineNames = pattern.define.map { it.name }
        validateDuplicateNames("match.define.duplicate", "MATCH_RECOGNIZE DEFINE item", defineNames, diagnostics)
        patternNames.forEach { name ->
            if (name !in defineNames.toSet()) {
                diagnostics.error("match.define.missing", "MATCH_RECOGNIZE pattern `$name` has no DEFINE item.")
            }
        }
        pattern.define.forEach { validateExpr(it.expr, diagnostics) }
        validateRowPatternTerm(pattern.pattern, diagnostics)
        pattern.subset.forEach { validateSubset(it, patternNames, diagnostics) }
        val skipName = when (val skip = pattern.afterMatchMode) {
            is SqlRowPatternSkipMode.To -> skip.name
            is SqlRowPatternSkipMode.ToFirst -> skip.name
            is SqlRowPatternSkipMode.ToLast -> skip.name
            SqlRowPatternSkipMode.PastLastRow,
            SqlRowPatternSkipMode.ToNextRow,
            null -> null
        }
        if (skipName != null && skipName !in patternNames) {
            diagnostics.error("match.skip.unknown", "AFTER MATCH SKIP references unknown pattern `$skipName`.")
        }
    }

    private fun validateSubset(
        subset: SqlRowPatternSubsetItem,
        patternNames: Set<String>,
        diagnostics: MutableList<SqlValidationDiagnostic>
    ) {
        subset.patternNames.forEach { name ->
            if (name !in patternNames) {
                diagnostics.error("match.subset.unknown", "SUBSET `${subset.name}` references unknown pattern `$name`.")
            }
        }
    }

    private fun validateRowPatternTerm(term: SqlRowPatternTerm, diagnostics: MutableList<SqlValidationDiagnostic>) {
        term.quantifier?.let { validateRowPatternQuantifier(it, diagnostics) }
        when (term) {
            is SqlRowPatternTerm.Pattern,
            is SqlRowPatternTerm.Circumflex,
            is SqlRowPatternTerm.Dollar -> {}
            is SqlRowPatternTerm.Exclusion -> validateRowPatternTerm(term.term, diagnostics)
            is SqlRowPatternTerm.Permute -> term.terms.forEach { validateRowPatternTerm(it, diagnostics) }
            is SqlRowPatternTerm.Then -> {
                validateRowPatternTerm(term.left, diagnostics)
                validateRowPatternTerm(term.right, diagnostics)
            }
            is SqlRowPatternTerm.Or -> {
                validateRowPatternTerm(term.left, diagnostics)
                validateRowPatternTerm(term.right, diagnostics)
            }
        }
    }

    private fun validateRowPatternQuantifier(
        quantifier: SqlRowPatternQuantifier,
        diagnostics: MutableList<SqlValidationDiagnostic>
    ) {
        when (quantifier) {
            is SqlRowPatternQuantifier.Between -> {
                quantifier.start?.let { validateExpr(it, diagnostics) }
                quantifier.end?.let { validateExpr(it, diagnostics) }
            }
            is SqlRowPatternQuantifier.Quantity -> validateExpr(quantifier.quantity, diagnostics)
            is SqlRowPatternQuantifier.Asterisk,
            is SqlRowPatternQuantifier.Plus,
            is SqlRowPatternQuantifier.Question -> {}
        }
    }

    private fun validateGraphPatternTerm(term: SqlGraphPatternTerm, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (term) {
            is SqlGraphPatternTerm.Quantified -> {
                validateGraphPatternTerm(term.term, diagnostics)
                validateGraphQuantifier(term.quantifier, diagnostics)
            }
            is SqlGraphPatternTerm.Vertex -> term.where?.let { validateExpr(it, diagnostics) }
            is SqlGraphPatternTerm.Edge -> term.where?.let { validateExpr(it, diagnostics) }
            is SqlGraphPatternTerm.And -> {
                validateGraphPatternTerm(term.left, diagnostics)
                validateGraphPatternTerm(term.right, diagnostics)
            }
            is SqlGraphPatternTerm.Or -> {
                validateGraphPatternTerm(term.left, diagnostics)
                validateGraphPatternTerm(term.right, diagnostics)
            }
            is SqlGraphPatternTerm.Alternation -> {
                validateGraphPatternTerm(term.left, diagnostics)
                validateGraphPatternTerm(term.right, diagnostics)
            }
        }
    }

    private fun validateGraphQuantifier(quantifier: SqlGraphQuantifier, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (quantifier) {
            is SqlGraphQuantifier.Between -> {
                quantifier.start?.let { validateExpr(it, diagnostics) }
                quantifier.end?.let { validateExpr(it, diagnostics) }
            }
            is SqlGraphQuantifier.Quantity -> validateExpr(quantifier.quantity, diagnostics)
            SqlGraphQuantifier.Asterisk,
            SqlGraphQuantifier.Plus,
            SqlGraphQuantifier.Question -> {}
        }
    }

    private fun validateJsonColumn(column: SqlJsonColumn, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (column) {
            is SqlJsonColumn.Ordinality -> {}
            is SqlJsonColumn.Column -> {
                validateType(column.type, diagnostics)
                column.path?.let { validateExpr(it, diagnostics) }
                column.onEmpty?.let { validateJsonQueryEmptyBehavior(it, diagnostics) }
                column.onError?.let { validateJsonQueryErrorBehavior(it, diagnostics) }
            }
            is SqlJsonColumn.Exists -> {
                validateType(column.type, diagnostics)
                column.path?.let { validateExpr(it, diagnostics) }
            }
            is SqlJsonColumn.Nested -> {
                validateExpr(column.path, diagnostics)
                column.columns.forEach { validateJsonColumn(it, diagnostics) }
            }
        }
    }

    private fun validateColumnDefinition(column: SqlColumnDefinition, diagnostics: MutableList<SqlValidationDiagnostic>) {
        validateType(column.type, diagnostics)
        column.defaultValue?.let { validateExpr(it, diagnostics) }
    }

    private fun validateIndexDefinition(
        index: SqlIndexDefinition,
        knownColumns: Set<String>,
        diagnostics: MutableList<SqlValidationDiagnostic>
    ) {
        validateDuplicateNames("ddl.index.column.duplicate", "index column", index.columns, diagnostics)
        index.columns.forEach { column ->
            if (column !in knownColumns) {
                diagnostics.error("ddl.index.column.unknown", "Index `${index.name}` references unknown column `$column`.")
            }
        }
    }

    private fun validateGroup(group: SqlGroup, diagnostics: MutableList<SqlValidationDiagnostic>) {
        group.items.forEach { validateGroupingItem(it, diagnostics) }
    }

    private fun validateGroupingItem(item: SqlGroupingItem, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (item) {
            SqlGroupingItem.EmptyGroup -> {}
            is SqlGroupingItem.Expr -> validateExpr(item.item, diagnostics)
            is SqlGroupingItem.Cube -> item.items.forEach { validateExpr(it, diagnostics) }
            is SqlGroupingItem.Rollup -> item.items.forEach { validateExpr(it, diagnostics) }
            is SqlGroupingItem.GroupingSets -> item.items.forEach { validateGroupingItem(it, diagnostics) }
        }
    }

    private fun validateOrdering(orderBy: List<SqlOrderingItem>, diagnostics: MutableList<SqlValidationDiagnostic>) {
        orderBy.forEach { validateExpr(it.expr, diagnostics) }
    }

    private fun validateLimit(limit: SqlLimit, scope: String, diagnostics: MutableList<SqlValidationDiagnostic>) {
        limit.offset?.let { validateExpr(it, diagnostics) }
        limit.fetch?.let { validateExpr(it.limit, diagnostics) }
        if (limit.offset == null && limit.fetch == null) {
            diagnostics.warning("$scope.limit.empty", "LIMIT/OFFSET clause is empty.")
        }
    }

    private fun validateWindow(window: SqlWindow, diagnostics: MutableList<SqlValidationDiagnostic>) {
        window.partitionBy.forEach { validateExpr(it, diagnostics) }
        validateOrdering(window.orderBy, diagnostics)
        when (val frame = window.frame) {
            is SqlWindowFrame.Start -> validateWindowFrameBound(frame.start, diagnostics)
            is SqlWindowFrame.Between -> {
                validateWindowFrameBound(frame.start, diagnostics)
                validateWindowFrameBound(frame.end, diagnostics)
            }
            null -> {}
        }
    }

    private fun validateWindowFrameBound(bound: SqlWindowFrameBound, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (bound) {
            is SqlWindowFrameBound.Preceding -> validateExpr(bound.n, diagnostics)
            is SqlWindowFrameBound.Following -> validateExpr(bound.n, diagnostics)
            SqlWindowFrameBound.CurrentRow,
            SqlWindowFrameBound.UnboundedFollowing,
            SqlWindowFrameBound.UnboundedPreceding -> {}
        }
    }

    private fun validateJsonPassing(passing: SqlJsonPassing, diagnostics: MutableList<SqlValidationDiagnostic>) {
        validateExpr(passing.expr, diagnostics)
    }

    private fun validateJsonObjectItem(item: SqlJsonObjectItem, diagnostics: MutableList<SqlValidationDiagnostic>) {
        validateExpr(item.key, diagnostics)
        validateExpr(item.value, diagnostics)
    }

    private fun validateJsonArrayItem(item: SqlJsonArrayItem, diagnostics: MutableList<SqlValidationDiagnostic>) {
        validateExpr(item.value, diagnostics)
    }

    private fun validateJsonQueryEmptyBehavior(
        behavior: SqlJsonQueryEmptyBehavior,
        diagnostics: MutableList<SqlValidationDiagnostic>
    ) {
        if (behavior is SqlJsonQueryEmptyBehavior.Default) validateExpr(behavior.expr, diagnostics)
    }

    private fun validateJsonQueryErrorBehavior(
        behavior: SqlJsonQueryErrorBehavior,
        diagnostics: MutableList<SqlValidationDiagnostic>
    ) {
        if (behavior is SqlJsonQueryErrorBehavior.Default) validateExpr(behavior.expr, diagnostics)
    }

    private fun validateJsonValueEmptyBehavior(
        behavior: SqlJsonValueEmptyBehavior,
        diagnostics: MutableList<SqlValidationDiagnostic>
    ) {
        if (behavior is SqlJsonValueEmptyBehavior.Default) validateExpr(behavior.expr, diagnostics)
    }

    private fun validateJsonValueErrorBehavior(
        behavior: SqlJsonValueErrorBehavior,
        diagnostics: MutableList<SqlValidationDiagnostic>
    ) {
        if (behavior is SqlJsonValueErrorBehavior.Default) validateExpr(behavior.expr, diagnostics)
    }

    private fun validateListAggOverflow(onOverflow: SqlListAggOnOverflow, diagnostics: MutableList<SqlValidationDiagnostic>) {
        if (onOverflow is SqlListAggOnOverflow.Truncate) {
            onOverflow.filler?.let { validateExpr(it, diagnostics) }
        }
    }

    private fun validateType(type: SqlType, diagnostics: MutableList<SqlValidationDiagnostic>) {
        when (type) {
            is SqlType.Array -> validateType(type.type, diagnostics)
            is SqlType.UnsafeCustom -> type.tokens.forEach { validateUnsafeToken(it, diagnostics) }
            is SqlType.Varchar,
            SqlType.Int,
            SqlType.Long,
            SqlType.Float,
            SqlType.Double,
            is SqlType.Decimal,
            SqlType.Date,
            is SqlType.Timestamp,
            is SqlType.Time,
            SqlType.Json,
            SqlType.Boolean,
            SqlType.Interval,
            SqlType.Geometry,
            SqlType.Point,
            SqlType.LineString,
            SqlType.Polygon,
            SqlType.MultiPoint,
            SqlType.MultiLineString,
            SqlType.MultiPolygon,
            SqlType.GeometryCollection,
            is SqlType.Named -> {}
        }
    }

    private fun validateUnsafeToken(token: SqlUnsafeToken, diagnostics: MutableList<SqlValidationDiagnostic>) {
        if (token is SqlUnsafeToken.Expr) validateExpr(token.value, diagnostics)
    }

    private fun validateRowsArity(
        code: String,
        label: String,
        rows: List<List<SqlExpr>>,
        diagnostics: MutableList<SqlValidationDiagnostic>
    ) {
        val expected = rows.firstOrNull()?.size ?: return
        rows.forEachIndexed { index, row ->
            if (row.size != expected) {
                diagnostics.error(code, "$label row $index has ${row.size} values but row 0 has $expected values.")
            }
        }
    }

    private fun validateDuplicateNames(
        code: String,
        label: String,
        names: List<String>,
        diagnostics: MutableList<SqlValidationDiagnostic>
    ) {
        duplicates(names).forEach { name ->
            diagnostics.error(code, "Duplicate $label `$name`.")
        }
    }

    private fun duplicates(names: List<String>): Set<String> =
        names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

    private fun collectPatternNames(term: SqlRowPatternTerm): Set<String> = when (term) {
        is SqlRowPatternTerm.Pattern -> setOf(term.name)
        is SqlRowPatternTerm.Exclusion -> collectPatternNames(term.term)
        is SqlRowPatternTerm.Permute -> term.terms.flatMap { collectPatternNames(it) }.toSet()
        is SqlRowPatternTerm.Then -> collectPatternNames(term.left) + collectPatternNames(term.right)
        is SqlRowPatternTerm.Or -> collectPatternNames(term.left) + collectPatternNames(term.right)
        is SqlRowPatternTerm.Circumflex,
        is SqlRowPatternTerm.Dollar -> emptySet()
    }

    private fun MutableList<SqlValidationDiagnostic>.error(code: String, message: String) {
        add(SqlValidationDiagnostic(code, message, SqlValidationSeverity.Error))
    }

    private fun MutableList<SqlValidationDiagnostic>.warning(code: String, message: String) {
        add(SqlValidationDiagnostic(code, message, SqlValidationSeverity.Warning))
    }
}

data class SqlValidationDiagnostic(
    val code: String,
    val message: String,
    val severity: SqlValidationSeverity
)

enum class SqlValidationSeverity {
    Error,
    Warning
}

class SqlValidationException(
    val diagnostics: List<SqlValidationDiagnostic>
) : IllegalArgumentException(diagnostics.joinToString("; ") { "${it.code}: ${it.message}" })
