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

object SubqueryLowering {
    fun lower(statement: Statement, context: QueryMaterializeContext = QueryMaterializeContext()): Statement {
        return when (statement) {
            is SelectStatement -> lower(statement, context)
            is InsertStatement -> lower(statement, context)
            is UpdateStatement -> lower(statement, context)
            is DeleteStatement -> lower(statement, context)
            is DdlStatement -> lower(statement, context)
            is UnionStatement -> lower(statement, context)
        }
    }

    fun lower(statement: SelectStatement, context: QueryMaterializeContext = QueryMaterializeContext()): SelectStatement {
        return SelectStatement(
            selectList = statement.selectList.map { lowerSelectItem(it, context) }.toMutableList(),
            from = lowerTableReference(statement.from, context),
            where = statement.where?.let { lowerExpression(it, context) },
            groupBy = statement.groupBy?.map { lowerExpression(it, context) }?.toMutableList(),
            having = statement.having?.let { lowerExpression(it, context) },
            orderBy = statement.orderBy?.map { lowerOrderByItem(it, context) }?.toMutableList(),
            limit = statement.limit,
            distinct = statement.distinct,
            lock = statement.lock
        )
    }

    fun lower(statement: InsertStatement, context: QueryMaterializeContext = QueryMaterializeContext()): InsertStatement {
        return statement.copy(
            table = lowerTableReference(statement.table, context),
            values = statement.values.map { lowerExpression(it, context) },
            source = statement.source?.let { lowerQueryStatement(it, context) },
            conflictAssignments = statement.conflictAssignments.map { lowerAssignment(it, context) }
        )
    }

    fun lower(statement: UpdateStatement, context: QueryMaterializeContext = QueryMaterializeContext()): UpdateStatement {
        return UpdateStatement(
            table = lowerTableReference(statement.table, context),
            assignments = statement.assignments.map { lowerAssignment(it, context) }.toMutableList(),
            where = statement.where?.let { lowerExpression(it, context) }
        )
    }

    fun lower(statement: DeleteStatement, context: QueryMaterializeContext = QueryMaterializeContext()): DeleteStatement {
        return statement.copy(
            table = lowerTableReference(statement.table, context),
            where = statement.where?.let { lowerExpression(it, context) }
        )
    }

    fun lower(statement: UnionStatement, context: QueryMaterializeContext = QueryMaterializeContext()): UnionStatement {
        return statement.copy(
            queries = statement.queries.map { lower(it, context) },
            orderBy = statement.orderBy?.map { lowerOrderByItem(it, context) },
        )
    }

    fun lower(statement: DdlStatement, context: QueryMaterializeContext = QueryMaterializeContext()): DdlStatement {
        return when (statement) {
            is DdlStatement.CreateTableStatement -> statement.copy(
                columns = statement.columns.map { column ->
                    column.copy(defaultValue = column.defaultValue?.let { lowerExpression(it, context) })
                }
            )
            is DdlStatement.CreateTableAsSelectStatement ->
                statement.copy(query = lowerQueryStatement(statement.query, context))
            is DdlStatement.AlterTableStatement.AddColumnStatement -> statement.copy(
                column = statement.column.copy(
                    defaultValue = statement.column.defaultValue?.let { lowerExpression(it, context) }
                )
            )
            is DdlStatement.AlterTableStatement.ModifyColumnStatement -> statement.copy(
                column = statement.column.copy(
                    defaultValue = statement.column.defaultValue?.let { lowerExpression(it, context) }
                )
            )
            is DdlStatement.AlterTableStatement.DropColumnStatement,
            is DdlStatement.DropTableStatement,
            is DdlStatement.CreateIndexStatement,
            is DdlStatement.DropIndexStatement,
            is DdlStatement.TruncateTableStatement -> statement
        }
    }

    private fun lowerQueryStatement(statement: Statement, context: QueryMaterializeContext): Statement {
        return when (statement) {
            is SelectStatement -> lower(statement, context)
            is UnionStatement -> lower(statement, context)
            else -> error("CTAS query source must be a SELECT or UNION statement, but was ${statement::class.simpleName}.")
        }
    }

    fun lowerExpression(
        expression: Expression,
        context: QueryMaterializeContext = QueryMaterializeContext()
    ): Expression {
        return when (expression) {
            is DeferredSubqueryExpression.Scalar -> {
                val statement = lower(expression.query.materialize(context), context)
                SubqueryValidator.validateScalar(statement)
                SubqueryExpression.ScalarSubquery(statement)
            }
            is DeferredSubqueryExpression.Exists -> {
                val statement = lower(expression.query.materialize(context), context)
                SubqueryExpression.ExistsExpression(statement, expression.not)
            }
            is DeferredSubqueryExpression.In -> {
                val statement = lower(expression.query.materialize(context), context)
                val value = lowerExpression(expression.value, context)
                SubqueryValidator.validateInSubquery(value, statement)
                SpecialExpression.InSubqueryExpression(
                    value = value,
                    subquery = statement,
                    not = expression.not
                )
            }
            is DeferredSubqueryExpression.QuantifiedComparison -> {
                val statement = lower(expression.query.materialize(context), context)
                SubqueryExpression.QuantifiedComparison(
                    expression = lowerExpression(expression.expression, context),
                    operator = expression.operator,
                    quantifier = expression.quantifier,
                    subquery = statement
                )
            }
            is BinaryExpression -> expression.copy(
                left = lowerExpression(expression.left, context),
                right = lowerExpression(expression.right, context)
            )
            is UnaryExpression -> expression.copy(operand = lowerExpression(expression.operand, context))
            is FunctionCall -> expression.copy(
                arguments = expression.arguments.map { lowerExpression(it, context) },
                filter = expression.filter?.let { lowerExpression(it, context) },
                over = expression.over?.let { lowerWindowClause(it, context) }
            )
            is CaseExpression.SimpleCaseExpression -> expression.copy(
                operand = lowerExpression(expression.operand, context),
                whenClauses = expression.whenClauses.map { lowerWhenThenClause(it, context) },
                elseResult = expression.elseResult?.let { lowerExpression(it, context) }
            )
            is CaseExpression.SearchedCaseExpression -> expression.copy(
                whenClauses = expression.whenClauses.map { lowerWhenThenClause(it, context) },
                elseResult = expression.elseResult?.let { lowerExpression(it, context) }
            )
            is RowValueExpression -> RowValueExpression(expression.values.map { lowerExpression(it, context) })
            is SubqueryExpression.ExistsExpression ->
                expression.copy(subquery = lower(expression.subquery, context))
            is SubqueryExpression.ScalarSubquery -> {
                val statement = lower(expression.subquery, context)
                SubqueryValidator.validateScalar(statement)
                expression.copy(subquery = statement)
            }
            is SubqueryExpression.QuantifiedComparison -> expression.copy(
                expression = lowerExpression(expression.expression, context),
                subquery = lower(expression.subquery, context)
            )
            is SpecialExpression.BetweenExpression -> expression.copy(
                value = lowerExpression(expression.value, context),
                low = lowerExpression(expression.low, context),
                high = lowerExpression(expression.high, context)
            )
            is SpecialExpression.InExpression -> expression.copy(
                value = lowerExpression(expression.value, context),
                values = expression.values.map { lowerExpression(it, context) }
            )
            is SpecialExpression.InSubqueryExpression -> {
                val value = lowerExpression(expression.value, context)
                val statement = lower(expression.subquery, context)
                SubqueryValidator.validateInSubquery(value, statement)
                expression.copy(
                    value = value,
                    subquery = statement
                )
            }
            is SpecialExpression.IsNullExpression ->
                expression.copy(expression = lowerExpression(expression.expression, context))
            is SpecialExpression.LikeExpression -> expression.copy(
                value = lowerExpression(expression.value, context),
                pattern = lowerExpression(expression.pattern, context),
                escape = expression.escape?.let { lowerExpression(it, context) }
            )
            is ColumnReference,
            is Literal,
            is Parameter,
            is SpecialExpression.RawSqlExpression -> expression
        }
    }

    private fun lowerSelectItem(item: SelectItem, context: QueryMaterializeContext): SelectItem {
        return when (item) {
            is SelectItem.ColumnSelectItem,
            is SelectItem.AllColumnsSelectItem -> item
            is SelectItem.ExpressionSelectItem ->
                item.copy(expression = lowerExpression(item.expression, context))
        }
    }

    private fun lowerTableReference(table: TableReference, context: QueryMaterializeContext): TableReference {
        return when (table) {
            is SubqueryTable -> table.copy(subquery = lower(table.subquery, context))
            is DeferredSubqueryTable ->
                SubqueryTable(lower(table.query.materialize(context), context), table.alias)
            is JoinTable -> table.copy(
                left = lowerTableReference(table.left, context),
                right = lowerTableReference(table.right, context),
                condition = table.condition?.let { lowerExpression(it, context) }
            )
            is TableReferenceImpl.SubqueryTableReference ->
                table.copy(subquery = lower(table.subquery, context))
            is TableReferenceImpl.JoinedTableReference -> table.copy(
                baseTable = lowerTableReference(table.baseTable, context),
                joins = table.joins.map { lowerJoin(it, context) }
            )
            is TableName,
            is TableReferenceImpl.SimpleTableReference -> table
        }
    }

    private fun lowerJoin(join: Join, context: QueryMaterializeContext): Join {
        return join.copy(
            table = lowerTableReference(join.table, context),
            on = join.on?.let { lowerExpression(it, context) }
        )
    }

    private fun lowerOrderByItem(item: OrderByItem, context: QueryMaterializeContext): OrderByItem {
        return item.copy(expression = lowerExpression(item.expression, context))
    }

    private fun lowerAssignment(assignment: Assignment, context: QueryMaterializeContext): Assignment {
        return assignment.copy(value = lowerExpression(assignment.value, context))
    }

    private fun lowerWhenThenClause(
        clause: CaseExpression.WhenThenClause,
        context: QueryMaterializeContext
    ): CaseExpression.WhenThenClause {
        return clause.copy(
            whenCondition = lowerExpression(clause.whenCondition, context),
            thenResult = lowerExpression(clause.thenResult, context)
        )
    }

    private fun lowerWindowClause(window: WindowClause, context: QueryMaterializeContext): WindowClause {
        return window.copy(
            partitionBy = window.partitionBy?.map { lowerExpression(it, context) },
            orderBy = window.orderBy?.map { lowerOrderByItem(it, context) },
            frame = window.frame?.let { lowerWindowFrame(it, context) }
        )
    }

    private fun lowerWindowFrame(frame: WindowFrame, context: QueryMaterializeContext): WindowFrame {
        return when (frame) {
            is WindowFrame.BetweenFrame -> frame.copy(
                start = lowerFrameBoundary(frame.start, context),
                end = lowerFrameBoundary(frame.end, context)
            )
            is WindowFrame.SingleBoundaryFrame ->
                frame.copy(boundary = lowerFrameBoundary(frame.boundary, context))
        }
    }

    private fun lowerFrameBoundary(
        boundary: WindowFrame.FrameBoundary,
        context: QueryMaterializeContext
    ): WindowFrame.FrameBoundary {
        return when (boundary) {
            is WindowFrame.FrameBoundary.Preceding ->
                boundary.copy(value = lowerExpression(boundary.value, context))
            is WindowFrame.FrameBoundary.Following ->
                boundary.copy(value = lowerExpression(boundary.value, context))
            is WindowFrame.FrameBoundary.UnboundedPreceding,
            is WindowFrame.FrameBoundary.CurrentRow,
            is WindowFrame.FrameBoundary.UnboundedFollowing -> boundary
        }
    }
}
