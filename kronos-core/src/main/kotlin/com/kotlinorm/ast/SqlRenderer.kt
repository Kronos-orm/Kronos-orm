/**
 * Copyright 2022-2025 kronos-orm
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

/**
 * RenderedSql
 *
 * Represents the result of SQL rendering, containing the SQL string and parameter bindings.
 *
 * @property sql The rendered SQL string
 * @property parameters Map of parameter names to values for parameterized queries
 *
 * @author OUSC
 */
data class RenderedSql(val sql: String, val parameters: Map<String, Any?> = emptyMap())

/**
 * RenderContext
 *
 * Context for SQL rendering, containing configuration and state for the rendering process. Manages
 * parameter binding, quote handling, and other rendering options.
 *
 * @property parameterPrefix Prefix for named parameters (e.g., ":" for :paramName)
 * @property positionalParameterPlaceholder Placeholder for positional parameters (e.g., "?")
 * @property quotes Pair of quote characters for identifiers (start, end), e.g., Pair("\"", "\"")
 * @property parameterCounter Counter for generating unique parameter names
 * @property boundParameters Mutable map of bound parameter values
 * @property usePositionalParameters Whether to use positional parameters instead of named
 * parameters
 * @property dbType Database type for database-specific rendering (optional)
 *
 * @author OUSC
 */
class RenderContext(
        val parameterPrefix: String = ":",
        val positionalParameterPlaceholder: String = "?",
        val quotes: Pair<String, String> = Pair("\"", "\""),
        private var parameterCounter: Int = 0,
        val boundParameters: MutableMap<String, Any?> = mutableMapOf(),
        val usePositionalParameters: Boolean = false,
        val dbType: com.kotlinorm.enums.DBType? = null
) {
    /** Generate a unique parameter name. */
    fun generateParameterName(baseName: String? = null): String {
        val name = baseName ?: "param"
        val uniqueName =
                if (boundParameters.containsKey(name)) {
                    "${name}_${parameterCounter++}"
                } else {
                    name
                }
        return uniqueName
    }

    /** Bind a parameter value and return the parameter name. */
    fun bindParameter(name: String, value: Any?): String {
        val paramName = generateParameterName(name)
        boundParameters[paramName] = value
        return paramName
    }

    /** Quote an identifier (table name, column name, etc.). */
    fun quote(identifier: String): String {
        return "${quotes.first}$identifier${quotes.second}"
    }

    /** Create a copy of this context with modified properties. */
    fun copy(
            parameterPrefix: String = this.parameterPrefix,
            positionalParameterPlaceholder: String = this.positionalParameterPlaceholder,
            quotes: Pair<String, String> = this.quotes,
            usePositionalParameters: Boolean = this.usePositionalParameters,
            dbType: com.kotlinorm.enums.DBType? = this.dbType
    ): RenderContext {
        return RenderContext(
                parameterPrefix = parameterPrefix,
                positionalParameterPlaceholder = positionalParameterPlaceholder,
                quotes = quotes,
                parameterCounter = this.parameterCounter,
                boundParameters = this.boundParameters.toMutableMap(),
                usePositionalParameters = usePositionalParameters,
                dbType = dbType
        )
    }
}

/**
 * SqlRenderer
 *
 * Interface for rendering SQL AST nodes to SQL strings. Uses the visitor pattern to traverse and
 * render AST nodes.
 *
 * @author OUSC
 */
interface SqlRenderer {
    /**
     * Render a SQL statement to a RenderedSql object.
     *
     * @param statement The statement to render
     * @param context Optional rendering context (creates a new one if not provided)
     * @return RenderedSql containing the SQL string and parameters
     */
    fun render(statement: Statement, context: RenderContext = RenderContext()): RenderedSql

    // Expression rendering methods
    fun renderExpression(expression: Expression, context: RenderContext): String
    fun renderColumnReference(column: ColumnReference, context: RenderContext): String
    fun renderLiteral(literal: Literal, context: RenderContext): String
    fun renderParameter(parameter: Parameter, context: RenderContext): String
    fun renderBinaryExpression(expr: BinaryExpression, context: RenderContext): String
    fun renderUnaryExpression(expr: UnaryExpression, context: RenderContext): String
    fun renderFunctionCall(function: FunctionCall, context: RenderContext): String
    fun renderCaseExpression(case: CaseExpression, context: RenderContext): String
    fun renderSubqueryExpression(subquery: SubqueryExpression, context: RenderContext): String
    fun renderSpecialExpression(expr: SpecialExpression, context: RenderContext): String

    // Table reference rendering methods
    fun renderTableReference(table: TableReference, context: RenderContext): String
    fun renderTableName(table: TableName, context: RenderContext): String
    fun renderSubqueryTable(table: SubqueryTable, context: RenderContext): String
    fun renderJoinTable(join: JoinTable, context: RenderContext): String

    // Statement rendering methods
    fun renderSelectStatement(select: SelectStatement, context: RenderContext): String
    fun renderInsertStatement(insert: InsertStatement, context: RenderContext): String
    fun renderUpdateStatement(update: UpdateStatement, context: RenderContext): String
    fun renderDeleteStatement(delete: DeleteStatement, context: RenderContext): String
    fun renderDdlStatement(ddl: DdlStatement, context: RenderContext): String
    fun renderUnionStatement(union: UnionStatement, context: RenderContext): String
}
