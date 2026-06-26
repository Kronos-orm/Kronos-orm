/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *     http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kotlinorm.ast

import com.kotlinorm.enums.SortType

/**
 * AbstractSqlRenderer
 *
 * Abstract base class for SQL renderers implementing the visitor pattern. Provides default
 * implementations for common AST node rendering and defines abstract methods for database-specific
 * logic.
 *
 * @author OUSC
 */
abstract class AbstractSqlRenderer : SqlRenderer {
    override fun render(statement: Statement, context: RenderContext): RenderedSql {
        val sql =
                when (statement) {
                    is SelectStatement -> renderSelectStatement(statement, context)
                    is InsertStatement -> renderInsertStatement(statement, context)
                    is UpdateStatement -> renderUpdateStatement(statement, context)
                    is DeleteStatement -> renderDeleteStatement(statement, context)
                    is DdlStatement -> renderDdlStatement(statement, context)
                    is UnionStatement -> renderUnionStatement(statement, context)
                }
        return RenderedSql(sql, context.boundParameters.toMap())
    }

    override fun renderExpression(expression: Expression, context: RenderContext): String {
        return when (expression) {
            is ColumnReference -> renderColumnReference(expression, context)
            is Literal -> renderLiteral(expression, context)
            is Parameter -> renderParameter(expression, context)
            is BinaryExpression -> renderBinaryExpression(expression, context)
            is UnaryExpression -> renderUnaryExpression(expression, context)
            is FunctionCall -> renderFunctionCall(expression, context)
            is CaseExpression -> renderCaseExpression(expression, context)
            is SubqueryExpression -> renderSubqueryExpression(expression, context)
            is SpecialExpression -> renderSpecialExpression(expression, context)
        }
    }

    override fun renderTableReference(table: TableReference, context: RenderContext): String {
        return when (table) {
            is TableName -> renderTableName(table, context)
            is SubqueryTable -> renderSubqueryTable(table, context)
            is JoinTable -> renderJoinTable(table, context)
            is TableReferenceImpl.SimpleTableReference -> renderSimpleTableReference(table, context)
            is TableReferenceImpl.SubqueryTableReference ->
                    renderSubqueryTableReference(table, context)
            is TableReferenceImpl.JoinedTableReference -> renderJoinedTableReference(table, context)
        }
    }

    // Default implementations for common rendering

    override fun renderColumnReference(column: ColumnReference, context: RenderContext): String {
        return when {
            column.database != null && column.tableAlias != null -> {
                "${context.quote(column.database)}.${context.quote(column.tableAlias)}.${context.quote(column.columnName)}"
            }
            column.tableAlias != null -> {
                "${context.quote(column.tableAlias)}.${context.quote(column.columnName)}"
            }
            else -> {
                context.quote(column.columnName)
            }
        }
    }

    override fun renderLiteral(literal: Literal, context: RenderContext): String {
        return when (literal) {
            is Literal.StringLiteral -> renderStringLiteral(literal.value)
            is Literal.NumberLiteral -> literal.value
            is Literal.BooleanLiteral -> if (literal.value) "true" else "false"
            is Literal.NullLiteral -> "NULL"
            is Literal.DateLiteral -> renderDateLiteral(literal.value)
            is Literal.TimeLiteral -> renderTimeLiteral(literal.value)
            is Literal.TimestampLiteral -> renderTimestampLiteral(literal.value)
        }
    }

    override fun renderParameter(parameter: Parameter, context: RenderContext): String {
        return when (parameter) {
            is Parameter.NamedParameter -> {
                "${context.parameterPrefix}${parameter.name}"
            }
            is Parameter.PositionalParameter -> {
                context.positionalParameterPlaceholder
            }
        }
    }

    override fun renderBinaryExpression(expr: BinaryExpression, context: RenderContext): String {
        // Determine if left operand needs parentheses
        val leftNeedsParens = when {
            expr.left !is BinaryExpression -> false
            // If current operator is AND and left is OR, left needs parentheses
            expr.operator == SqlOperator.AND && 
            (expr.left.operator == SqlOperator.OR) -> true
            // If current operator is OR and left is AND, left needs parentheses
            expr.operator == SqlOperator.OR && 
            (expr.left.operator == SqlOperator.AND) -> true
            else -> false
        }
        
        // Determine if right operand needs parentheses
        val rightNeedsParens = when {
            expr.right !is BinaryExpression -> false
            // If current operator is AND and right is OR, right needs parentheses
            expr.operator == SqlOperator.AND && 
            (expr.right.operator == SqlOperator.OR) -> true
            // If current operator is OR and right is AND, right needs parentheses
            expr.operator == SqlOperator.OR && 
            (expr.right.operator == SqlOperator.AND) -> true
            else -> false
        }
        
        val left = renderExpression(expr.left, context)
        val right = renderExpression(expr.right, context)
        val operator = expr.operator.symbol
        
        val leftStr = if (leftNeedsParens) "($left)" else left
        val rightStr = if (rightNeedsParens) "($right)" else right
        
        return "$leftStr $operator $rightStr"
    }

    override fun renderUnaryExpression(expr: UnaryExpression, context: RenderContext): String {
        val operand = renderExpression(expr.operand, context)
        val operator = expr.operator.symbol
        return when (expr.operator) {
            UnaryOperator.NOT -> "$operator ($operand)"
            UnaryOperator.NEGATE, UnaryOperator.POSITIVE -> "$operator$operand"
            UnaryOperator.BITWISE_NOT -> "$operator$operand"
        }
    }

    override fun renderFunctionCall(function: FunctionCall, context: RenderContext): String {
        // Use FunctionManager for all function rendering
        val rendered = com.kotlinorm.functions.FunctionManager.renderFunctionCall(
            function, context, ::renderExpression
        )
        
        // If FunctionManager returns null, throw exception - all functions must be registered
        return rendered ?: throw com.kotlinorm.exceptions.UnSupportedFunctionException(
            context.dbType ?: com.kotlinorm.enums.DBType.Unknown,
            function.functionName
        )
    }

    override fun renderCaseExpression(case: CaseExpression, context: RenderContext): String {
        return when (case) {
            is CaseExpression.SimpleCaseExpression -> {
                val operand = renderExpression(case.operand, context)
                val whenClauses =
                        case.whenClauses.joinToString(" ") { clause ->
                            "WHEN ${renderExpression(clause.whenCondition, context)} THEN ${renderExpression(clause.thenResult, context)}"
                        }
                val elseClause =
                        case.elseResult?.let { " ELSE ${renderExpression(it, context)}" } ?: ""
                "CASE $operand $whenClauses$elseClause END"
            }
            is CaseExpression.SearchedCaseExpression -> {
                val whenClauses =
                        case.whenClauses.joinToString(" ") { clause ->
                            "WHEN ${renderExpression(clause.whenCondition, context)} THEN ${renderExpression(clause.thenResult, context)}"
                        }
                val elseClause =
                        case.elseResult?.let { " ELSE ${renderExpression(it, context)}" } ?: ""
                "CASE $whenClauses$elseClause END"
            }
        }
    }

    override fun renderSubqueryExpression(
            subquery: SubqueryExpression,
            context: RenderContext
    ): String {
        return when (subquery) {
            is SubqueryExpression.ExistsExpression -> {
                val not = if (subquery.not) "NOT " else ""
                val sql = renderSelectStatement(subquery.subquery, context)
                "${not}EXISTS ($sql)"
            }
            is SubqueryExpression.ScalarSubquery -> {
                val sql = renderSelectStatement(subquery.subquery, context)
                "($sql)"
            }
            is SubqueryExpression.QuantifiedComparison -> {
                val expr = renderExpression(subquery.expression, context)
                val operator = subquery.operator.symbol
                val quantifier = subquery.quantifier.name
                val sql = renderSelectStatement(subquery.subquery, context)
                "$expr $operator $quantifier ($sql)"
            }
        }
    }

    override fun renderSpecialExpression(expr: SpecialExpression, context: RenderContext): String {
        return when (expr) {
            is SpecialExpression.BetweenExpression -> {
                val value = renderExpression(expr.value, context)
                val low = renderExpression(expr.low, context)
                val high = renderExpression(expr.high, context)
                val not = if (expr.not) "NOT " else ""
                "$value ${not}BETWEEN $low AND $high"
            }
            is SpecialExpression.InExpression -> {
                val value = renderExpression(expr.value, context)
                val values = expr.values.joinToString(", ") { renderExpression(it, context) }
                val not = if (expr.not) "NOT " else ""
                "$value ${not}IN ($values)"
            }
            is SpecialExpression.InSubqueryExpression -> {
                val value = renderExpression(expr.value, context)
                val sql = renderSelectStatement(expr.subquery, context)
                val not = if (expr.not) "NOT " else ""
                "$value ${not}IN ($sql)"
            }
            is SpecialExpression.IsNullExpression -> {
                val exprStr = renderExpression(expr.expression, context)
                val not = if (expr.not) "NOT " else ""
                "$exprStr IS ${not}NULL"
            }
            is SpecialExpression.LikeExpression -> {
                val value = renderExpression(expr.value, context)
                val pattern = renderExpression(expr.pattern, context)
                val escape = expr.escape?.let { " ESCAPE ${renderExpression(it, context)}" } ?: ""
                val not = if (expr.not) "NOT " else ""
                val likeOp = if (expr.caseInsensitive) "ILIKE" else "LIKE"
                "$value $not$likeOp $pattern$escape"
            }
            is SpecialExpression.RawSqlExpression -> {
                // Render raw SQL as-is without any escaping or quoting
                expr.sql
            }
        }
    }

    override fun renderTableName(table: TableName, context: RenderContext): String {
        val parts =
                listOfNotNull(table.database, table.schema, table.table).joinToString(".") {
                    context.quote(it)
                }
        return table.alias?.let { "$parts AS ${context.quote(it)}" } ?: parts
    }

    override fun renderSubqueryTable(table: SubqueryTable, context: RenderContext): String {
        val sql = renderSelectStatement(table.subquery, context)
        return "($sql) AS ${context.quote(table.alias)}"
    }

    override fun renderJoinTable(join: JoinTable, context: RenderContext): String {
        val left = renderTableReference(join.left, context)
        val right = renderTableReference(join.right, context)
        val joinType = join.joinType.value
        val condition = join.condition?.let { " ON ${renderExpression(it, context)}" } ?: ""
        return "$left $joinType $right$condition"
    }

    override fun renderSelectStatement(select: SelectStatement, context: RenderContext): String {
        val selectList = select.selectList.joinToString(", ") { renderSelectItem(it, context) }
        val distinct = if (select.distinct) "DISTINCT " else ""
        val from = renderTableReference(select.from, context)
        val where = select.where?.let { " WHERE ${renderExpression(it, context)}" } ?: ""
        val groupBy =
                select.groupBy?.let {
                    " GROUP BY ${it.joinToString(", ") { renderExpression(it, context) }}"
                }
                        ?: ""
        val having = select.having?.let { " HAVING ${renderExpression(it, context)}" } ?: ""
        val orderBy =
                select.orderBy?.let {
                    " ORDER BY ${it.joinToString(", ") { renderOrderByItem(it, context) }}"
                }
                        ?: ""
        val limit = select.limit?.let { renderLimitClause(it, context) } ?: ""
        val lock = select.lock?.let { " ${renderLock(it)}" } ?: ""
        return "SELECT $distinct$selectList FROM $from$where$groupBy$having$orderBy$limit$lock"
    }

    override fun renderInsertStatement(insert: InsertStatement, context: RenderContext): String {
        val table = renderTableReference(insert.table, context)
        val columns = insert.columns.joinToString(", ") { renderColumnReference(it, context) }
        val values = insert.values.joinToString(", ") { renderExpression(it, context) }
        val conflict = insert.conflictResolver?.let { renderConflictResolver(it, context) } ?: ""
        return "INSERT INTO $table ($columns) VALUES ($values)$conflict"
    }

    override fun renderUpdateStatement(update: UpdateStatement, context: RenderContext): String {
        val table = renderTableReference(update.table, context)
        val assignments = update.assignments.joinToString(", ") { renderAssignment(it, context) }
        val where = update.where?.let { " WHERE ${renderExpression(it, context)}" } ?: ""
        return "UPDATE $table SET $assignments$where"
    }

    override fun renderDeleteStatement(delete: DeleteStatement, context: RenderContext): String {
        val table = renderTableReference(delete.table, context)
        val where = delete.where?.let { " WHERE ${renderExpression(it, context)}" } ?: ""
        return "DELETE FROM $table$where"
    }

    override fun renderDdlStatement(ddl: DdlStatement, context: RenderContext): String {
        return when (ddl) {
            is DdlStatement.CreateTableStatement -> renderCreateTable(ddl, context)
            is DdlStatement.AlterTableStatement -> renderAlterTable(ddl, context)
            is DdlStatement.DropTableStatement -> renderDropTable(ddl, context)
            is DdlStatement.CreateIndexStatement -> renderCreateIndex(ddl, context)
            is DdlStatement.DropIndexStatement -> renderDropIndex(ddl, context)
            is DdlStatement.TruncateTableStatement -> renderTruncateTable(ddl, context)
        }
    }

    // Helper methods

    protected fun renderSelectItem(item: SelectItem, context: RenderContext): String {
        return when (item) {
            is SelectItem.ColumnSelectItem -> {
                val column = renderColumnReference(item.column, context)
                item.alias?.let { "$column AS ${context.quote(it)}" } ?: column
            }
            is SelectItem.ExpressionSelectItem -> {
                val expr = renderExpression(item.expression, context)
                // For function call expressions, don't quote the alias (it's a simple identifier)
                // For other expressions, quote the alias
                val aliasStr = item.alias?.let { alias ->
                    if (item.expression is FunctionCall) {
                        " AS $alias"  // Don't quote function aliases
                    } else {
                        " AS ${context.quote(alias)}"  // Quote other aliases
                    }
                }
                "$expr${aliasStr ?: ""}"
            }
            is SelectItem.AllColumnsSelectItem -> {
                item.table?.let { "${context.quote(it)}.*" } ?: "*"
            }
        }
    }

    protected fun renderOrderByItem(item: OrderByItem, context: RenderContext): String {
        val expr = renderExpression(item.expression, context)
        val direction =
                when (item.direction) {
                    SortType.ASC -> "ASC"
                    SortType.DESC -> "DESC"
                }
        return "$expr $direction"
    }

    protected open fun renderLimitClause(limit: LimitClause, context: RenderContext): String {
        val offset = if (limit.offset != null) " OFFSET ${limit.offset}" else ""
        return " LIMIT ${limit.limit}$offset"
    }

    protected fun renderAssignment(assignment: Assignment, context: RenderContext): String {
        val column = renderColumnReference(assignment.column, context)
        val value = renderExpression(assignment.value, context)
        return "$column = $value"
    }

    protected fun renderWindowClause(window: WindowClause, context: RenderContext): String {
        val partitionBy =
                window.partitionBy?.let {
                    "PARTITION BY ${it.joinToString(", ") { renderExpression(it, context) }}"
                }
                        ?: ""
        val orderBy =
                window.orderBy?.let {
                    "ORDER BY ${it.joinToString(", ") { renderOrderByItem(it, context) }}"
                }
                        ?: ""
        val frame = window.frame?.let { renderWindowFrame(it, context) } ?: ""
        return listOfNotNull(partitionBy, orderBy, frame).joinToString(" ")
    }

    protected fun renderWindowFrame(frame: WindowFrame, context: RenderContext): String {
        return when (frame) {
            is WindowFrame.BetweenFrame -> {
                val type = frame.type.name
                val start = renderFrameBoundary(frame.start, context)
                val end = renderFrameBoundary(frame.end, context)
                val exclude = frame.exclude?.let { " EXCLUDE ${it.name}" } ?: ""
                "$type BETWEEN $start AND $end$exclude"
            }
            is WindowFrame.SingleBoundaryFrame -> {
                val type = frame.type.name
                val boundary = renderFrameBoundary(frame.boundary, context)
                "$type $boundary"
            }
        }
    }

    protected fun renderFrameBoundary(
            boundary: WindowFrame.FrameBoundary,
            context: RenderContext
    ): String {
        return when (boundary) {
            is WindowFrame.FrameBoundary.UnboundedPreceding -> "UNBOUNDED PRECEDING"
            is WindowFrame.FrameBoundary.Preceding ->
                    "${renderExpression(boundary.value, context)} PRECEDING"
            is WindowFrame.FrameBoundary.CurrentRow -> "CURRENT ROW"
            is WindowFrame.FrameBoundary.Following ->
                    "${renderExpression(boundary.value, context)} FOLLOWING"
            is WindowFrame.FrameBoundary.UnboundedFollowing -> "UNBOUNDED FOLLOWING"
        }
    }

    // Legacy table reference support
    protected fun renderSimpleTableReference(
            table: TableReferenceImpl.SimpleTableReference,
            context: RenderContext
    ): String {
        val name = context.quote(table.tableName)
        return table.alias?.let { "$name AS ${context.quote(it)}" } ?: name
    }

    protected fun renderSubqueryTableReference(
            table: TableReferenceImpl.SubqueryTableReference,
            context: RenderContext
    ): String {
        val sql = renderSelectStatement(table.subquery, context)
        return "($sql) AS ${context.quote(table.alias)}"
    }

    protected fun renderJoinedTableReference(
            table: TableReferenceImpl.JoinedTableReference,
            context: RenderContext
    ): String {
        val base = renderTableReference(table.baseTable, context)
        val joins = table.joins.joinToString(" ") { renderJoin(it, context) }
        return "$base $joins"
    }

    protected fun renderJoin(join: Join, context: RenderContext): String {
        val table = renderTableReference(join.table, context)
        val joinType = join.type.value
        val condition = join.on?.let { " ON ${renderExpression(it, context)}" } ?: ""
        return "$joinType $table$condition"
    }

    // Abstract methods for database-specific implementations

    protected abstract fun renderStringLiteral(value: String): String
    protected abstract fun renderDateLiteral(value: String): String
    protected abstract fun renderTimeLiteral(value: String): String
    protected abstract fun renderTimestampLiteral(value: String): String
    protected abstract fun renderLock(lock: com.kotlinorm.enums.PessimisticLock): String
    protected abstract fun renderConflictResolver(
            resolver: com.kotlinorm.database.ConflictResolver,
            context: RenderContext
    ): String
    protected abstract fun renderCreateTable(
            create: DdlStatement.CreateTableStatement,
            context: RenderContext
    ): String
    protected abstract fun renderAlterTable(
            alter: DdlStatement.AlterTableStatement,
            context: RenderContext
    ): String
    protected abstract fun renderDropTable(
            drop: DdlStatement.DropTableStatement,
            context: RenderContext
    ): String
    protected abstract fun renderCreateIndex(
            create: DdlStatement.CreateIndexStatement,
            context: RenderContext
    ): String
    protected abstract fun renderDropIndex(
            drop: DdlStatement.DropIndexStatement,
            context: RenderContext
    ): String
    protected abstract fun renderTruncateTable(
            truncate: DdlStatement.TruncateTableStatement,
            context: RenderContext
    ): String
    
    /**
     * Renders a UNION statement.
     * Default implementation combines multiple SELECT statements with UNION or UNION ALL.
     * Parameters are pre-bound with unique names (e.g., id, id@1, id@2).
     * This method renames parameters in the rendered SQL to match the pre-bound names.
     */
    override fun renderUnionStatement(union: UnionStatement, context: RenderContext): String {
        val parts = mutableListOf<String>()
        val parameterCounter = mutableMapOf<String, Int>()
        
        // Initialize parameter counter from pre-bound parameters
        context.boundParameters.keys.forEach { paramName ->
            // Extract base name (before @N suffix)
            val baseName = paramName.substringBefore('@')
            val suffix = paramName.substringAfter('@', "")
            if (suffix.isNotEmpty()) {
                val count = suffix.toIntOrNull() ?: 0
                parameterCounter[baseName] = maxOf(parameterCounter[baseName] ?: 0, count)
            } else {
                parameterCounter[baseName] = 0
            }
        }
        
        // Render each SELECT statement
        union.queries.forEachIndexed { index, selectStatement ->
            if (index > 0) {
                parts.add(if (union.unionAll) "UNION ALL" else "UNION")
            }
            
            // Render the SELECT statement
            var querySql = renderSelectStatement(selectStatement, context)
            
            // For subsequent queries (index > 0), rename parameters in the SQL
            if (index > 0) {
                // Extract parameter names from the rendered SQL
                val parameterPattern = Regex(":([a-zA-Z_][a-zA-Z0-9_]*)")
                val matches = parameterPattern.findAll(querySql).toList()
                
                // Build a map of old parameter names to new parameter names
                val parameterRenameMap = mutableMapOf<String, String>()
                
                matches.forEach { match ->
                    val paramName = match.groupValues[1]
                    
                    // Skip if already processed
                    if (parameterRenameMap.containsKey(paramName)) {
                        return@forEach
                    }
                    
                    // Find the renamed parameter name in context.boundParameters
                    // For query index N, parameter "id" should be renamed to "id@N" (if N > 0)
                    val expectedName = if (index == 1) {
                        "$paramName@1"
                    } else {
                        "$paramName@$index"
                    }
                    
                    if (context.boundParameters.containsKey(expectedName)) {
                        parameterRenameMap[paramName] = expectedName
                    }
                }
                
                // Replace parameter names in the SQL string
                parameterRenameMap.forEach { (oldName, newName) ->
                    // Replace :oldName with :newName
                    // Use regex with word boundaries to avoid replacing partial matches
                    querySql = querySql.replace(Regex(":$oldName\\b"), ":$newName")
                }
            }
            
            // Wrap each SELECT in parentheses for clarity
            parts.add("($querySql)")
        }
        
        val sql = StringBuilder(parts.joinToString(" "))
        
        // Add ORDER BY if present (applies to entire union result)
        if (!union.orderBy.isNullOrEmpty()) {
            sql.append(" ORDER BY ")
            sql.append(union.orderBy.joinToString(", ") { orderByItem ->
                val expr = renderExpression(orderByItem.expression, context)
                val direction = when (orderByItem.direction) {
                    SortType.ASC -> "ASC"
                    SortType.DESC -> "DESC"
                }
                "$expr $direction"
            })
        }
        
        // Add LIMIT if present (applies to entire union result)
        if (union.limit != null) {
            sql.append(renderLimitClause(union.limit, context))
        }
        
        return sql.toString()
    }
}
