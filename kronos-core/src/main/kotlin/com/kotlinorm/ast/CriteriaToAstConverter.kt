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

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.NoValueStrategyType

/**
 * CriteriaToAstConverter
 *
 * Converts Criteria tree to AST Expression tree.
 * Handles all condition types (AND, OR, comparison operations, etc.)
 * and parameter binding.
 *
 * @author OUSC
 */
object CriteriaToAstConverter {
    /**
     * Converts a Criteria tree to an AST Expression tree.
     *
     * @param criteria The criteria to convert
     * @param parameterValues Optional mutable map to collect parameter values during conversion
     * @param operationType Optional operation type for Auto NoValueStrategy resolution (defaults to SELECT)
     * @param databaseOfTable Optional map of table name to database name (for cross-database queries)
     * @param useTableAliases Whether to use table aliases in column references (true for JOIN queries, false for single-table queries)
     * @return The converted AST Expression, or null if the criteria is empty/meaningless
     */
    fun convert(
        criteria: Criteria, 
        parameterValues: MutableMap<String, Any?> = mutableMapOf(),
        operationType: KOperationType = KOperationType.SELECT,
        databaseOfTable: Map<String, String> = emptyMap(),
        useTableAliases: Boolean = false
    ): Expression? {
        // Track parameter name usage for deduplication
        val paramNameCounter = mutableMapOf<String, Int>()
        return convertCriteria(criteria, parameterValues, paramNameCounter, operationType, databaseOfTable, useTableAliases)
    }

    /**
     * Recursively converts a Criteria to an Expression.
     *
     * @param criteria The criteria to convert
     * @param parameterValues Mutable map to collect parameter values during conversion
     * @param paramNameCounter Mutable map to track parameter name usage for deduplication
     * @param operationType Operation type for Auto NoValueStrategy resolution
     * @param databaseOfTable Map of table name to database name (for cross-database queries)
     * @param useTableAliases Whether to use table aliases in column references
     * @return The converted Expression, or null if the criteria is empty/meaningless
     */
    private fun convertCriteria(
        criteria: Criteria, 
        parameterValues: MutableMap<String, Any?>, 
        paramNameCounter: MutableMap<String, Int>,
        operationType: KOperationType,
        databaseOfTable: Map<String, String> = emptyMap(),
        useTableAliases: Boolean = false
    ): Expression? {
        // Handle NoValueStrategy: if value is null and strategy is Ignore (or Auto for SELECT), skip this criteria
        if (criteria.value == null && criteria.valueAcceptable) {
            val strategy = when (criteria.noValueStrategyType) {
                NoValueStrategyType.Ignore -> NoValueStrategyType.Ignore
                NoValueStrategyType.Auto, null -> {
                    // For SELECT operations, Auto strategy is Ignore (skip null values)
                    // For UPDATE/INSERT operations, Auto strategy might be different
                    if (operationType == KOperationType.SELECT) {
                        NoValueStrategyType.Ignore
                    } else {
                        // For other operations, keep the default behavior
                        criteria.noValueStrategyType ?: NoValueStrategyType.Auto
                    }
                }
                else -> criteria.noValueStrategyType
            }
            if (strategy == NoValueStrategyType.Ignore) {
                return null  // Skip this criteria entirely
            }
        }
        
        // Handle empty collection for IN clause: treat as "no value"
        if (criteria.type == ConditionType.IN && criteria.valueAcceptable) {
            val isEmpty = when (val value = criteria.value) {
                is Collection<*> -> value.isEmpty()
                is Array<*> -> value.isEmpty()
                else -> false
            }
            if (isEmpty) {
                // Empty IN clause: return false for IN, true for NOT IN
                // criteria.not = false (IN) -> return false (nothing can be in empty list)
                // criteria.not = true (NOT IN) -> return true (everything is not in empty list)
                return Literal.BooleanLiteral(criteria.not)
            }
        }

        // Handle ROOT/AND/OR - combine children with logical operators
        if (criteria.type == ConditionType.ROOT || criteria.type == ConditionType.AND || criteria.type == ConditionType.OR) {
            return combineChildren(criteria, parameterValues, paramNameCounter, operationType, databaseOfTable, useTableAliases)
        }

        val columnRef = buildColumnRef(criteria, databaseOfTable, useTableAliases)

        // Handle different condition types
        val expression = when (criteria.type) {
            ConditionType.EQUAL -> {
                buildComparisonExpression(
                    columnRef,
                    SqlOperator.EQUAL,
                    criteria.value,
                    criteria.field.name,
                    parameterValues,
                    paramNameCounter,
                    databaseOfTable,
                    useTableAliases
                )
            }
            ConditionType.GT -> {
                buildComparisonExpression(
                    columnRef,
                    SqlOperator.GREATER_THAN,
                    criteria.value,
                    criteria.field.name + "Min",
                    parameterValues,
                    paramNameCounter,
                    databaseOfTable,
                    useTableAliases
                )
            }
            ConditionType.GE -> {
                buildComparisonExpression(
                    columnRef,
                    SqlOperator.GREATER_THAN_OR_EQUAL,
                    criteria.value,
                    criteria.field.name + "Min",
                    parameterValues,
                    paramNameCounter,
                    databaseOfTable,
                    useTableAliases
                )
            }
            ConditionType.LT -> {
                buildComparisonExpression(
                    columnRef,
                    SqlOperator.LESS_THAN,
                    criteria.value,
                    criteria.field.name + "Max",
                    parameterValues,
                    paramNameCounter,
                    databaseOfTable,
                    useTableAliases
                )
            }
            ConditionType.LE -> {
                buildComparisonExpression(
                    columnRef,
                    SqlOperator.LESS_THAN_OR_EQUAL,
                    criteria.value,
                    criteria.field.name + "Max",
                    parameterValues,
                    paramNameCounter,
                    databaseOfTable,
                    useTableAliases
                )
            }
            ConditionType.LIKE -> {
                val patternExpr = convertValue(criteria.value, criteria.field.name, parameterValues, paramNameCounter, databaseOfTable, useTableAliases)
                SpecialExpression.LikeExpression(
                    value = columnRef,
                    pattern = patternExpr,
                    not = criteria.not,
                    caseInsensitive = false
                )
            }
            ConditionType.IN -> {
                val value = criteria.value
                when (value) {
                    is CriteriaSubqueryValue.In -> {
                        val left = when (val subqueryValue = value.value) {
                            is Expression -> subqueryValue
                            is List<*> -> rowValueFromFields(subqueryValue, databaseOfTable, useTableAliases)
                            else -> columnRef
                        }
                        DeferredSubqueryExpression.In(left, value.query, not = value.not || criteria.not)
                    }
                    is SelectQueryRef -> {
                        DeferredSubqueryExpression.In(columnRef, value, not = criteria.not)
                    }
                    else -> {
                // Store the entire collection as a single parameter with "List" suffix
                // NamedParameterUtils will expand it into multiple ? placeholders for JDBC
                val paramName = getUniqueParamName("${criteria.field.name}List", parameterValues, paramNameCounter)
                val listValue = when (value) {
                    is Collection<*> -> value.toList()
                    is Array<*> -> value.toList()
                    else -> [value]
                }
                parameterValues[paramName] = listValue
                
                // Create a single parameter reference for the list
                // The expansion to multiple placeholders happens in NamedParameterUtils
                SpecialExpression.InExpression(
                    value = columnRef,
                    values = [Parameter.NamedParameter(paramName)],
                    not = criteria.not
                )
                    }
                }
            }
            ConditionType.ISNULL -> {
                SpecialExpression.IsNullExpression(
                    expression = columnRef,
                    not = criteria.not
                )
            }
            ConditionType.BETWEEN -> {
                val value = criteria.value
                when (value) {
                    is Pair<*, *> -> {
                        val low = convertValue(value.first, criteria.field.name + "Min", parameterValues, paramNameCounter, databaseOfTable, useTableAliases)
                        val high = convertValue(value.second, criteria.field.name + "Max", parameterValues, paramNameCounter, databaseOfTable, useTableAliases)
                        SpecialExpression.BetweenExpression(
                            value = columnRef,
                            low = low,
                            high = high,
                            not = criteria.not
                        )
                    }
                    is ClosedRange<*> -> {
                        // Handle IntRange, LongRange, etc.
                        val low = convertValue(value.start, criteria.field.name + "Min", parameterValues, paramNameCounter, databaseOfTable, useTableAliases)
                        val high = convertValue(value.endInclusive, criteria.field.name + "Max", parameterValues, paramNameCounter, databaseOfTable, useTableAliases)
                        SpecialExpression.BetweenExpression(
                            value = columnRef,
                            low = low,
                            high = high,
                            not = criteria.not
                        )
                    }
                    is List<*> -> {
                        if (value.size >= 2) {
                            val low = convertValue(value[0], criteria.field.name + "Min", parameterValues, paramNameCounter, databaseOfTable, useTableAliases)
                            val high = convertValue(value[1], criteria.field.name + "Max", parameterValues, paramNameCounter, databaseOfTable, useTableAliases)
                            SpecialExpression.BetweenExpression(
                                value = columnRef,
                                low = low,
                                high = high,
                                not = criteria.not
                            )
                        } else {
                            throw IllegalArgumentException("BETWEEN requires exactly 2 values")
                        }
                    }
                    else -> {
                        throw IllegalArgumentException("BETWEEN requires a Pair, ClosedRange, or List with 2 values")
                    }
                }
            }
            ConditionType.REGEXP -> {
                val patternExpr = convertValue(criteria.value, criteria.field.name + "Pattern", parameterValues, paramNameCounter, databaseOfTable, useTableAliases)
                BinaryExpression(
                    columnRef,
                    if (criteria.not) SqlOperator.NOT_REGEXP else SqlOperator.REGEXP,
                    patternExpr
                )
            }
            ConditionType.SQL -> {
                // For SQL type, we might need to parse the SQL string or handle it specially
                // For now, return the value as-is if it's already an Expression, or convert it
                val value = criteria.value
                when (value) {
                    is CriteriaSubqueryValue.Exists -> DeferredSubqueryExpression.Exists(value.query, value.not)
                    is Expression -> value
                    is String -> {
                        // Raw SQL string - use RawSqlExpression to render as-is without quoting
                        SpecialExpression.RawSqlExpression(value)
                    }
                    else -> convertValue(value, databaseOfTable = databaseOfTable, useTableAliases = useTableAliases)
                }
            }
            ConditionType.AND, ConditionType.OR, ConditionType.ROOT -> {
                // These are handled above, but included here for completeness
                Literal.BooleanLiteral(true)
            }
        }

        return applyNot(criteria, expression)
    }

    private fun buildComparisonExpression(
        leftExpr: Expression,
        operator: SqlOperator,
        value: Any?,
        fieldName: String,
        parameterValues: MutableMap<String, Any?>,
        paramNameCounter: MutableMap<String, Int>,
        databaseOfTable: Map<String, String>,
        useTableAliases: Boolean
    ): Expression {
        return when (value) {
            is CriteriaSubqueryValue.Scalar -> {
                BinaryExpression(leftExpr, operator, DeferredSubqueryExpression.Scalar(value.query))
            }
            is CriteriaSubqueryValue.QuantifiedComparison -> {
                DeferredSubqueryExpression.QuantifiedComparison(
                    expression = leftExpr,
                    operator = operator,
                    quantifier = value.quantifier,
                    query = value.query
                )
            }
            is SelectQueryRef -> {
                BinaryExpression(leftExpr, operator, DeferredSubqueryExpression.Scalar(value))
            }
            else -> {
                val rightExpr = convertValue(
                    value,
                    fieldName,
                    parameterValues,
                    paramNameCounter,
                    databaseOfTable,
                    useTableAliases
                )
                BinaryExpression(leftExpr, operator, rightExpr)
            }
        }
    }

    /**
     * Applies NOT negation to an expression if the criteria requires it.
     * SpecialExpression types (LikeExpression, IsNullExpression, BetweenExpression, InExpression)
     * already handle NOT in their constructors, so they are not wrapped again.
     * REGEXP also handles NOT by selecting NOT_REGEXP operator.
     *
     * @param criteria The criteria containing the NOT flag and type
     * @param expression The expression to potentially negate
     * @return The expression, possibly wrapped with NOT
     */
    private fun applyNot(criteria: Criteria, expression: Expression): Expression {
        if (
            !criteria.not ||
            expression is SpecialExpression ||
            expression is DeferredSubqueryExpression.In ||
            criteria.type == ConditionType.REGEXP
        ) {
            return expression
        }
        return when (expression) {
            is DeferredSubqueryExpression.Exists -> expression.copy(not = !expression.not)
            is DeferredSubqueryExpression.In -> expression.copy(not = !expression.not)
            is SubqueryExpression.ExistsExpression -> expression.copy(not = !expression.not)
            is SpecialExpression.InSubqueryExpression -> expression.copy(not = !expression.not)
            is BinaryExpression -> {
                val notOperator = when (expression.operator) {
                    SqlOperator.EQUAL -> SqlOperator.NOT_EQUAL
                    SqlOperator.LIKE -> SqlOperator.NOT_LIKE
                    SqlOperator.ILIKE -> SqlOperator.NOT_ILIKE
                    else -> null
                }
                if (notOperator != null) {
                    BinaryExpression(expression.left, notOperator, expression.right)
                } else {
                    UnaryExpression(UnaryOperator.NOT, expression)
                }
            }
            else -> UnaryExpression(UnaryOperator.NOT, expression)
        }
    }

    /**
     * Combines children of a ROOT/AND/OR criteria into a single expression using the appropriate logical operator.
     *
     * @param criteria The criteria whose children to combine
     * @param parameterValues Mutable map to collect parameter values during conversion
     * @param paramNameCounter Mutable map to track parameter name usage for deduplication
     * @param operationType Operation type for Auto NoValueStrategy resolution
     * @param databaseOfTable Map of table name to database name (for cross-database queries)
     * @param useTableAliases Whether to use table aliases in column references
     * @return The combined expression, or null if no children produce expressions
     */
    private fun combineChildren(
        criteria: Criteria,
        parameterValues: MutableMap<String, Any?>,
        paramNameCounter: MutableMap<String, Int>,
        operationType: KOperationType,
        databaseOfTable: Map<String, String>,
        useTableAliases: Boolean
    ): Expression? {
        val operator = if (criteria.type == ConditionType.OR) SqlOperator.OR else SqlOperator.AND
        val expressions = criteria.children
            .filterNotNull()
            .mapNotNull { convertCriteria(it, parameterValues, paramNameCounter, operationType, databaseOfTable, useTableAliases) }
        return when {
            expressions.isEmpty() -> null
            expressions.size == 1 -> expressions.first()
            else -> expressions.reduce { acc, expr -> BinaryExpression(acc, operator, expr) }
        }
    }

    /**
     * Builds a column reference expression from a criteria's field.
     * Handles FunctionField conversion and table alias resolution for JOIN queries.
     *
     * @param criteria The criteria containing the field
     * @param databaseOfTable Map of table name to database name
     * @param useTableAliases Whether to use table aliases
     * @return The column reference expression
     */
    private fun buildColumnRef(
        criteria: Criteria,
        databaseOfTable: Map<String, String>,
        useTableAliases: Boolean
    ): Expression {
        if (criteria.field is FunctionField) {
            return FieldToExpressionConverter.fieldToExpression(criteria.field, useTableAlias = useTableAliases)
        }
        val shouldUseAlias = useTableAliases && !criteria.field.tableName.isNullOrEmpty()
        val database = if (shouldUseAlias) databaseOfTable[criteria.field.tableName] else null
        return ColumnReference(
            database = database,
            tableAlias = if (shouldUseAlias) criteria.field.tableName else null,
            columnName = criteria.field.columnName
        )
    }

    private fun rowValueFromFields(
        values: List<*>,
        databaseOfTable: Map<String, String>,
        useTableAliases: Boolean
    ): RowValueExpression {
        val fields = values.filterIsInstance<com.kotlinorm.beans.dsl.Field>()
        require(fields.size == values.size && fields.size > 1) {
            "Tuple IN requires at least two fields."
        }
        return RowValueExpression(
            fields.map { field ->
                val shouldUseAlias = useTableAliases && !field.tableName.isNullOrEmpty()
                ColumnReference(
                    database = if (shouldUseAlias) databaseOfTable[field.tableName] else null,
                    tableAlias = if (shouldUseAlias) field.tableName else null,
                    columnName = field.columnName
                )
            }
        )
    }

    /**
     * Gets a unique parameter name by appending @N suffix if the name already exists.
     * For example: id, id@1, id@2, etc.
     *
     * @param baseName The base parameter name
     * @param parameterValues The map of existing parameter values
     * @param paramNameCounter The counter map for tracking parameter name usage
     * @return A unique parameter name
     */
    internal fun getUniqueParamName(baseName: String, parameterValues: MutableMap<String, Any?>, paramNameCounter: MutableMap<String, Int>): String {
        // Check if this is the first use of this parameter name
        if (!parameterValues.containsKey(baseName)) {
            return baseName
        }
        
        // Parameter name already exists, need to add suffix
        val count = paramNameCounter.getOrDefault(baseName, 0) + 1
        paramNameCounter[baseName] = count
        return "$baseName@$count"
    }

    /**
     * Converts a value to an Expression (Literal or Parameter).
     * Creates Parameter nodes with field names for proper parameter binding.
     * Stores the actual parameter value in the parameterValues map.
     *
     * @param value The value to convert
     * @param fieldName The field name to use for parameter naming (if applicable)
     * @param parameterValues Mutable map to store parameter values
     * @param paramNameCounter Mutable map to track parameter name usage for deduplication
     * @param databaseOfTable Map of table name to database name (for cross-database queries)
     * @param useTableAliases Whether to use table aliases in column references
     * @return The converted Expression
     */
    private fun convertValue(
        value: Any?, 
        fieldName: String? = null, 
        parameterValues: MutableMap<String, Any?> = mutableMapOf(), 
        paramNameCounter: MutableMap<String, Int> = mutableMapOf(), 
        databaseOfTable: Map<String, String> = emptyMap(),
        useTableAliases: Boolean = false
    ): Expression {
        return when (value) {
            null -> Literal.NullLiteral
            is Expression -> value
            is Parameter -> value
            is FunctionField -> {
                // Convert FunctionField to FunctionCall expression
                FieldToExpressionConverter.fieldToExpression(value, useTableAlias = useTableAliases)
            }
            is com.kotlinorm.beans.dsl.Field -> {
                // Convert Field to ColumnReference with table alias and database name for join conditions
                // Only use table alias when explicitly requested (for JOIN queries)
                val shouldUseAlias = useTableAliases && !value.tableName.isNullOrEmpty()
                val database = if (shouldUseAlias) {
                    databaseOfTable[value.tableName]
                } else null
                ColumnReference(
                    database = database,
                    tableAlias = if (shouldUseAlias) value.tableName else null,
                    columnName = value.columnName
                )
            }
            else -> {
                // Create a Parameter node with the field name for proper binding
                // Store the actual value in the parameterValues map
                if (fieldName != null) {
                    // Get unique parameter name to avoid conflicts
                    val uniqueParamName = getUniqueParamName(fieldName, parameterValues, paramNameCounter)
                    parameterValues[uniqueParamName] = value
                    Parameter.NamedParameter(uniqueParamName)
                } else {
                    // Fallback to literal if no field name is provided
                    when (value) {
                        is String -> Literal.StringLiteral(value)
                        is Boolean -> Literal.BooleanLiteral(value)
                        is Number -> Literal.NumberLiteral(value.toString())
                        is Char -> Literal.StringLiteral(value.toString())
                        is Collection<*> -> {
                            if (value.isEmpty()) {
                                Literal.NullLiteral
                            } else {
                                convertValue(value.first(), null, parameterValues, paramNameCounter, databaseOfTable, useTableAliases)
                            }
                        }
                        is Array<*> -> {
                            if (value.isEmpty()) {
                                Literal.NullLiteral
                            } else {
                                convertValue(value.first(), null, parameterValues, paramNameCounter, databaseOfTable, useTableAliases)
                            }
                        }
                        else -> Literal.StringLiteral(value.toString())
                    }
                }
            }
        }
    }
}

