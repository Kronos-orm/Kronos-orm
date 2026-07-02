/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.ast

enum class SelectConditionLayer {
    INNER,
    OUTER,
    ORIGINAL
}

data class SelectConditionSourceAnalysis(
    val referencedOutputs: Set<SelectItemAliasMetadata>,
    val layer: SelectConditionLayer
) {
    val scopes: Set<SelectItemSourceScope>
        get() = referencedOutputs.mapTo(mutableSetOf()) { it.scope }
}

data class SelectConditionLayeringResult(
    val inner: Expression?,
    val outer: Expression?,
    val original: Expression?
)

fun SelectStatement.analyzeConditionSources(expression: Expression): SelectConditionSourceAnalysis {
    val outputs = expression.referencedSelectOutputs(this)
    val scopes = outputs.mapTo(mutableSetOf()) { it.scope }
    val layer = when {
        scopes.any {
            it == SelectItemSourceScope.SELECTED ||
                    it == SelectItemSourceScope.AGGREGATE ||
                    it == SelectItemSourceScope.WINDOW
        } -> SelectConditionLayer.OUTER
        scopes.isNotEmpty() && scopes.all { it == SelectItemSourceScope.SOURCE } -> SelectConditionLayer.INNER
        else -> SelectConditionLayer.ORIGINAL
    }
    return SelectConditionSourceAnalysis(outputs, layer)
}

fun SelectStatement.splitConditionBySource(expression: Expression?): SelectConditionLayeringResult {
    if (expression == null) {
        return SelectConditionLayeringResult(null, null, null)
    }
    val inner = mutableListOf<Expression>()
    val outer = mutableListOf<Expression>()
    val original = mutableListOf<Expression>()
    for (part in expression.flattenAndConditions()) {
        when (analyzeConditionSources(part).layer) {
            SelectConditionLayer.INNER -> inner.add(part)
            SelectConditionLayer.OUTER -> outer.add(part)
            SelectConditionLayer.ORIGINAL -> original.add(part)
        }
    }
    return SelectConditionLayeringResult(
        inner = inner.combineWithAnd(),
        outer = outer.combineWithAnd(),
        original = original.combineWithAnd()
    )
}

fun SelectStatement.applyAutomaticLayering(alias: String): SelectStatement {
    val whereParts = splitConditionBySource(where)
    val havingParts = splitConditionBySource(having)
    val outerWhere = listOfNotNull(
        whereParts.outer,
        havingParts.outer
    ).map { it.rewriteSelectOutputsForOuterAlias(this, alias) }.combineWithAnd()
    val outerOrderBy = orderBy
        ?.takeIf { items -> items.any { analyzeConditionSources(it.expression).layer == SelectConditionLayer.OUTER } }
        ?.map { item ->
            OrderByItem(item.expression.rewriteSelectOutputsForOuterAlias(this, alias), item.direction)
        }
        ?.toMutableList()

    val inner = SelectStatement(
        selectList = selectList,
        from = from,
        where = listOfNotNull(whereParts.inner, whereParts.original).combineWithAnd(),
        groupBy = groupBy,
        having = listOfNotNull(havingParts.inner, havingParts.original).combineWithAnd(),
        orderBy = if (outerOrderBy == null) orderBy else null,
        limit = limit,
        distinct = distinct,
        lock = lock
    )
    return if (outerWhere != null || outerOrderBy != null) {
        inner.wrapWithOuterFilter(alias = alias, outerWhere = outerWhere, outerOrderBy = outerOrderBy)
    } else {
        inner
    }
}

fun Expression.referencedSelectOutputs(statement: SelectStatement): Set<SelectItemAliasMetadata> {
    val refs = mutableSetOf<SelectItemAliasMetadata>()
    collectColumnReferences().forEach { column ->
        statement.findSelectOutput(column.columnName)?.let { refs.add(it) }
    }
    return refs
}

fun Expression.rewriteSelectOutputsForOuterAlias(statement: SelectStatement, alias: String): Expression {
    return when (this) {
        is ColumnReference ->
            if (statement.findSelectOutput(columnName) != null) {
                copy(database = null, tableAlias = alias)
            } else {
                this
            }
        is BinaryExpression -> copy(
            left = left.rewriteSelectOutputsForOuterAlias(statement, alias),
            right = right.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        is UnaryExpression -> copy(operand = operand.rewriteSelectOutputsForOuterAlias(statement, alias))
        is FunctionCall -> copy(
            arguments = arguments.map { it.rewriteSelectOutputsForOuterAlias(statement, alias) },
            filter = filter?.rewriteSelectOutputsForOuterAlias(statement, alias),
            over = over?.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        is RowValueExpression -> copy(values = values.map { it.rewriteSelectOutputsForOuterAlias(statement, alias) })
        is CaseExpression.SimpleCaseExpression -> copy(
            operand = operand.rewriteSelectOutputsForOuterAlias(statement, alias),
            whenClauses = whenClauses.map {
                it.copy(
                    whenCondition = it.whenCondition.rewriteSelectOutputsForOuterAlias(statement, alias),
                    thenResult = it.thenResult.rewriteSelectOutputsForOuterAlias(statement, alias)
                )
            },
            elseResult = elseResult?.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        is CaseExpression.SearchedCaseExpression -> copy(
            whenClauses = whenClauses.map {
                it.copy(
                    whenCondition = it.whenCondition.rewriteSelectOutputsForOuterAlias(statement, alias),
                    thenResult = it.thenResult.rewriteSelectOutputsForOuterAlias(statement, alias)
                )
            },
            elseResult = elseResult?.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        is SpecialExpression.BetweenExpression -> copy(
            value = value.rewriteSelectOutputsForOuterAlias(statement, alias),
            low = low.rewriteSelectOutputsForOuterAlias(statement, alias),
            high = high.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        is SpecialExpression.InExpression -> copy(
            value = value.rewriteSelectOutputsForOuterAlias(statement, alias),
            values = values.map { it.rewriteSelectOutputsForOuterAlias(statement, alias) }
        )
        is SpecialExpression.InSubqueryExpression -> copy(
            value = value.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        is SpecialExpression.IsNullExpression -> copy(
            expression = expression.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        is SpecialExpression.LikeExpression -> copy(
            value = value.rewriteSelectOutputsForOuterAlias(statement, alias),
            pattern = pattern.rewriteSelectOutputsForOuterAlias(statement, alias),
            escape = escape?.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        is SubqueryExpression.QuantifiedComparison -> copy(
            expression = expression.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        is DeferredSubqueryExpression.In -> copy(value = value.rewriteSelectOutputsForOuterAlias(statement, alias))
        is DeferredSubqueryExpression.QuantifiedComparison -> copy(
            expression = expression.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        is Literal,
        is Parameter,
        is SpecialExpression.RawSqlExpression,
        is SubqueryExpression.ExistsExpression,
        is SubqueryExpression.ScalarSubquery,
        is DeferredSubqueryExpression.Scalar,
        is DeferredSubqueryExpression.Exists -> this
    }
}

fun Expression.qualifyUnqualifiedColumns(alias: String): Expression {
    return when (this) {
        is ColumnReference ->
            if (database == null && tableAlias == null) {
                copy(tableAlias = alias)
            } else {
                this
            }
        is BinaryExpression -> copy(
            left = left.qualifyUnqualifiedColumns(alias),
            right = right.qualifyUnqualifiedColumns(alias)
        )
        is UnaryExpression -> copy(operand = operand.qualifyUnqualifiedColumns(alias))
        is FunctionCall -> copy(
            arguments = arguments.map { it.qualifyUnqualifiedColumns(alias) },
            filter = filter?.qualifyUnqualifiedColumns(alias),
            over = over?.qualifyUnqualifiedColumns(alias)
        )
        is RowValueExpression -> copy(values = values.map { it.qualifyUnqualifiedColumns(alias) })
        is CaseExpression.SimpleCaseExpression -> copy(
            operand = operand.qualifyUnqualifiedColumns(alias),
            whenClauses = whenClauses.map {
                it.copy(
                    whenCondition = it.whenCondition.qualifyUnqualifiedColumns(alias),
                    thenResult = it.thenResult.qualifyUnqualifiedColumns(alias)
                )
            },
            elseResult = elseResult?.qualifyUnqualifiedColumns(alias)
        )
        is CaseExpression.SearchedCaseExpression -> copy(
            whenClauses = whenClauses.map {
                it.copy(
                    whenCondition = it.whenCondition.qualifyUnqualifiedColumns(alias),
                    thenResult = it.thenResult.qualifyUnqualifiedColumns(alias)
                )
            },
            elseResult = elseResult?.qualifyUnqualifiedColumns(alias)
        )
        is SpecialExpression.BetweenExpression -> copy(
            value = value.qualifyUnqualifiedColumns(alias),
            low = low.qualifyUnqualifiedColumns(alias),
            high = high.qualifyUnqualifiedColumns(alias)
        )
        is SpecialExpression.InExpression -> copy(
            value = value.qualifyUnqualifiedColumns(alias),
            values = values.map { it.qualifyUnqualifiedColumns(alias) }
        )
        is SpecialExpression.InSubqueryExpression -> copy(
            value = value.qualifyUnqualifiedColumns(alias)
        )
        is SpecialExpression.IsNullExpression -> copy(
            expression = expression.qualifyUnqualifiedColumns(alias)
        )
        is SpecialExpression.LikeExpression -> copy(
            value = value.qualifyUnqualifiedColumns(alias),
            pattern = pattern.qualifyUnqualifiedColumns(alias),
            escape = escape?.qualifyUnqualifiedColumns(alias)
        )
        is SubqueryExpression.QuantifiedComparison -> copy(
            expression = expression.qualifyUnqualifiedColumns(alias)
        )
        is DeferredSubqueryExpression.In -> copy(value = value.qualifyUnqualifiedColumns(alias))
        is DeferredSubqueryExpression.QuantifiedComparison -> copy(
            expression = expression.qualifyUnqualifiedColumns(alias)
        )
        is Literal,
        is Parameter,
        is SpecialExpression.RawSqlExpression,
        is SubqueryExpression.ExistsExpression,
        is SubqueryExpression.ScalarSubquery,
        is DeferredSubqueryExpression.Scalar,
        is DeferredSubqueryExpression.Exists -> this
    }
}

fun Expression.rewriteColumnTableAliases(replacements: Map<String, String>): Expression {
    if (replacements.isEmpty()) return this
    return when (this) {
        is ColumnReference ->
            tableAlias?.let { replacements[it] }?.let { alias ->
                copy(database = null, tableAlias = alias)
            } ?: this
        is BinaryExpression -> copy(
            left = left.rewriteColumnTableAliases(replacements),
            right = right.rewriteColumnTableAliases(replacements)
        )
        is UnaryExpression -> copy(operand = operand.rewriteColumnTableAliases(replacements))
        is FunctionCall -> copy(
            arguments = arguments.map { it.rewriteColumnTableAliases(replacements) },
            filter = filter?.rewriteColumnTableAliases(replacements),
            over = over?.rewriteColumnTableAliases(replacements)
        )
        is RowValueExpression -> copy(values = values.map { it.rewriteColumnTableAliases(replacements) })
        is CaseExpression.SimpleCaseExpression -> copy(
            operand = operand.rewriteColumnTableAliases(replacements),
            whenClauses = whenClauses.map {
                it.copy(
                    whenCondition = it.whenCondition.rewriteColumnTableAliases(replacements),
                    thenResult = it.thenResult.rewriteColumnTableAliases(replacements)
                )
            },
            elseResult = elseResult?.rewriteColumnTableAliases(replacements)
        )
        is CaseExpression.SearchedCaseExpression -> copy(
            whenClauses = whenClauses.map {
                it.copy(
                    whenCondition = it.whenCondition.rewriteColumnTableAliases(replacements),
                    thenResult = it.thenResult.rewriteColumnTableAliases(replacements)
                )
            },
            elseResult = elseResult?.rewriteColumnTableAliases(replacements)
        )
        is SpecialExpression.BetweenExpression -> copy(
            value = value.rewriteColumnTableAliases(replacements),
            low = low.rewriteColumnTableAliases(replacements),
            high = high.rewriteColumnTableAliases(replacements)
        )
        is SpecialExpression.InExpression -> copy(
            value = value.rewriteColumnTableAliases(replacements),
            values = values.map { it.rewriteColumnTableAliases(replacements) }
        )
        is SpecialExpression.InSubqueryExpression -> copy(
            value = value.rewriteColumnTableAliases(replacements)
        )
        is SpecialExpression.IsNullExpression -> copy(
            expression = expression.rewriteColumnTableAliases(replacements)
        )
        is SpecialExpression.LikeExpression -> copy(
            value = value.rewriteColumnTableAliases(replacements),
            pattern = pattern.rewriteColumnTableAliases(replacements),
            escape = escape?.rewriteColumnTableAliases(replacements)
        )
        is SubqueryExpression.QuantifiedComparison -> copy(
            expression = expression.rewriteColumnTableAliases(replacements)
        )
        is DeferredSubqueryExpression.In -> copy(value = value.rewriteColumnTableAliases(replacements))
        is DeferredSubqueryExpression.QuantifiedComparison -> copy(
            expression = expression.rewriteColumnTableAliases(replacements)
        )
        is Literal,
        is Parameter,
        is SpecialExpression.RawSqlExpression,
        is SubqueryExpression.ExistsExpression,
        is SubqueryExpression.ScalarSubquery,
        is DeferredSubqueryExpression.Scalar,
        is DeferredSubqueryExpression.Exists -> this
    }
}

private fun Expression.collectColumnReferences(): List<ColumnReference> {
    return when (this) {
        is ColumnReference -> listOf(this)
        is BinaryExpression -> left.collectColumnReferences() + right.collectColumnReferences()
        is UnaryExpression -> operand.collectColumnReferences()
        is FunctionCall ->
            arguments.flatMap { it.collectColumnReferences() } +
                    filter?.collectColumnReferences().orEmpty() +
                    over?.collectColumnReferences().orEmpty()
        is RowValueExpression -> values.flatMap { it.collectColumnReferences() }
        is CaseExpression.SimpleCaseExpression ->
            operand.collectColumnReferences() +
                    whenClauses.flatMap {
                        it.whenCondition.collectColumnReferences() + it.thenResult.collectColumnReferences()
                    } +
                    elseResult?.collectColumnReferences().orEmpty()
        is CaseExpression.SearchedCaseExpression ->
            whenClauses.flatMap {
                it.whenCondition.collectColumnReferences() + it.thenResult.collectColumnReferences()
            } + elseResult?.collectColumnReferences().orEmpty()
        is SpecialExpression.BetweenExpression ->
            value.collectColumnReferences() + low.collectColumnReferences() + high.collectColumnReferences()
        is SpecialExpression.InExpression ->
            value.collectColumnReferences() + values.flatMap { it.collectColumnReferences() }
        is SpecialExpression.InSubqueryExpression -> value.collectColumnReferences()
        is SpecialExpression.IsNullExpression -> expression.collectColumnReferences()
        is SpecialExpression.LikeExpression ->
            value.collectColumnReferences() + pattern.collectColumnReferences() + escape?.collectColumnReferences().orEmpty()
        is SubqueryExpression.QuantifiedComparison -> expression.collectColumnReferences()
        is DeferredSubqueryExpression.In -> value.collectColumnReferences()
        is DeferredSubqueryExpression.QuantifiedComparison -> expression.collectColumnReferences()
        is Literal,
        is Parameter,
        is SpecialExpression.RawSqlExpression,
        is SubqueryExpression.ExistsExpression,
        is SubqueryExpression.ScalarSubquery,
        is DeferredSubqueryExpression.Scalar,
        is DeferredSubqueryExpression.Exists -> emptyList()
    }
}

private fun Expression.flattenAndConditions(): List<Expression> {
    return if (this is BinaryExpression && operator == SqlOperator.AND) {
        left.flattenAndConditions() + right.flattenAndConditions()
    } else {
        listOf(this)
    }
}

private fun List<Expression>.combineWithAnd(): Expression? {
    return when (size) {
        0 -> null
        1 -> first()
        else -> reduce { acc, expression -> BinaryExpression(acc, SqlOperator.AND, expression) }
    }
}

private fun WindowClause.rewriteSelectOutputsForOuterAlias(
    statement: SelectStatement,
    alias: String
): WindowClause {
    return copy(
        partitionBy = partitionBy?.map { it.rewriteSelectOutputsForOuterAlias(statement, alias) },
        orderBy = orderBy?.map {
            OrderByItem(it.expression.rewriteSelectOutputsForOuterAlias(statement, alias), it.direction)
        },
        frame = frame?.rewriteSelectOutputsForOuterAlias(statement, alias)
    )
}

private fun WindowFrame.rewriteSelectOutputsForOuterAlias(
    statement: SelectStatement,
    alias: String
): WindowFrame {
    return when (this) {
        is WindowFrame.BetweenFrame -> copy(
            start = start.rewriteSelectOutputsForOuterAlias(statement, alias),
            end = end.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        is WindowFrame.SingleBoundaryFrame -> copy(
            boundary = boundary.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
    }
}

private fun WindowFrame.FrameBoundary.rewriteSelectOutputsForOuterAlias(
    statement: SelectStatement,
    alias: String
): WindowFrame.FrameBoundary {
    return when (this) {
        is WindowFrame.FrameBoundary.Preceding -> copy(
            value = value.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        is WindowFrame.FrameBoundary.Following -> copy(
            value = value.rewriteSelectOutputsForOuterAlias(statement, alias)
        )
        WindowFrame.FrameBoundary.CurrentRow,
        WindowFrame.FrameBoundary.UnboundedFollowing,
        WindowFrame.FrameBoundary.UnboundedPreceding -> this
    }
}

private fun WindowClause.qualifyUnqualifiedColumns(alias: String): WindowClause {
    return copy(
        partitionBy = partitionBy?.map { it.qualifyUnqualifiedColumns(alias) },
        orderBy = orderBy?.map {
            OrderByItem(it.expression.qualifyUnqualifiedColumns(alias), it.direction)
        },
        frame = frame?.qualifyUnqualifiedColumns(alias)
    )
}

private fun WindowFrame.qualifyUnqualifiedColumns(alias: String): WindowFrame {
    return when (this) {
        is WindowFrame.BetweenFrame -> copy(
            start = start.qualifyUnqualifiedColumns(alias),
            end = end.qualifyUnqualifiedColumns(alias)
        )
        is WindowFrame.SingleBoundaryFrame -> copy(
            boundary = boundary.qualifyUnqualifiedColumns(alias)
        )
    }
}

private fun WindowFrame.FrameBoundary.qualifyUnqualifiedColumns(alias: String): WindowFrame.FrameBoundary {
    return when (this) {
        is WindowFrame.FrameBoundary.Preceding -> copy(value = value.qualifyUnqualifiedColumns(alias))
        is WindowFrame.FrameBoundary.Following -> copy(value = value.qualifyUnqualifiedColumns(alias))
        WindowFrame.FrameBoundary.CurrentRow,
        WindowFrame.FrameBoundary.UnboundedFollowing,
        WindowFrame.FrameBoundary.UnboundedPreceding -> this
    }
}

private fun WindowClause.rewriteColumnTableAliases(replacements: Map<String, String>): WindowClause {
    return copy(
        partitionBy = partitionBy?.map { it.rewriteColumnTableAliases(replacements) },
        orderBy = orderBy?.map {
            OrderByItem(it.expression.rewriteColumnTableAliases(replacements), it.direction)
        },
        frame = frame?.rewriteColumnTableAliases(replacements)
    )
}

private fun WindowFrame.rewriteColumnTableAliases(replacements: Map<String, String>): WindowFrame {
    return when (this) {
        is WindowFrame.BetweenFrame -> copy(
            start = start.rewriteColumnTableAliases(replacements),
            end = end.rewriteColumnTableAliases(replacements)
        )
        is WindowFrame.SingleBoundaryFrame -> copy(
            boundary = boundary.rewriteColumnTableAliases(replacements)
        )
    }
}

private fun WindowFrame.FrameBoundary.rewriteColumnTableAliases(
    replacements: Map<String, String>
): WindowFrame.FrameBoundary {
    return when (this) {
        is WindowFrame.FrameBoundary.Preceding -> copy(value = value.rewriteColumnTableAliases(replacements))
        is WindowFrame.FrameBoundary.Following -> copy(value = value.rewriteColumnTableAliases(replacements))
        WindowFrame.FrameBoundary.CurrentRow,
        WindowFrame.FrameBoundary.UnboundedFollowing,
        WindowFrame.FrameBoundary.UnboundedPreceding -> this
    }
}

private fun WindowClause.collectColumnReferences(): List<ColumnReference> {
    return partitionBy.orEmpty().flatMap { it.collectColumnReferences() } +
            orderBy.orEmpty().flatMap { it.expression.collectColumnReferences() } +
            frame?.collectColumnReferences().orEmpty()
}

private fun WindowFrame.collectColumnReferences(): List<ColumnReference> {
    return when (this) {
        is WindowFrame.BetweenFrame -> start.collectColumnReferences() + end.collectColumnReferences()
        is WindowFrame.SingleBoundaryFrame -> boundary.collectColumnReferences()
    }
}

private fun WindowFrame.FrameBoundary.collectColumnReferences(): List<ColumnReference> {
    return when (this) {
        is WindowFrame.FrameBoundary.Preceding -> value.collectColumnReferences()
        is WindowFrame.FrameBoundary.Following -> value.collectColumnReferences()
        WindowFrame.FrameBoundary.CurrentRow,
        WindowFrame.FrameBoundary.UnboundedFollowing,
        WindowFrame.FrameBoundary.UnboundedPreceding -> emptyList()
    }
}
