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
 * SpecialExpression
 *
 * Sealed class representing special SQL expressions that don't fit the standard binary/unary
 * pattern. Includes BETWEEN, IN, and other multi-operand expressions.
 *
 * @author OUSC
 */
sealed class SpecialExpression : Expression {
    /**
     * BetweenExpression
     *
     * Represents a BETWEEN expression: value BETWEEN low AND high.
     *
     * @property value The value expression to test
     * @property low The lower bound expression
     * @property high The upper bound expression
     * @property not Whether to use NOT BETWEEN
     */
    data class BetweenExpression(
            val value: Expression,
            val low: Expression,
            val high: Expression,
            val not: Boolean = false
    ) : SpecialExpression()

    /**
     * InExpression
     *
     * Represents an IN expression: value IN (val1, val2, ...).
     *
     * @property value The value expression to test
     * @property values List of expressions to check against
     * @property not Whether to use NOT IN
     */
    data class InExpression(
            val value: Expression,
            val values: List<Expression>,
            val not: Boolean = false
    ) : SpecialExpression()

    /**
     * InSubqueryExpression
     *
     * Represents an IN expression with a subquery: value IN (SELECT ...).
     *
     * @property value The value expression to test
     * @property subquery The subquery SELECT statement
     * @property not Whether to use NOT IN
     */
    data class InSubqueryExpression(
            val value: Expression,
            val subquery: SelectStatement,
            val not: Boolean = false
    ) : SpecialExpression()

    /**
     * IsNullExpression
     *
     * Represents an IS NULL or IS NOT NULL expression.
     *
     * @property expression The expression to test
     * @property not Whether to use IS NOT NULL
     */
    data class IsNullExpression(val expression: Expression, val not: Boolean = false) :
            SpecialExpression()

    /**
     * LikeExpression
     *
     * Represents a LIKE expression: value LIKE pattern [ESCAPE escape].
     *
     * @property value The value expression to test
     * @property pattern The pattern expression
     * @property escape Optional escape character expression
     * @property not Whether to use NOT LIKE
     * @property caseInsensitive Whether to use ILIKE (case-insensitive LIKE)
     */
    data class LikeExpression(
            val value: Expression,
            val pattern: Expression,
            val escape: Expression? = null,
            val not: Boolean = false,
            val caseInsensitive: Boolean = false
    ) : SpecialExpression()

    /**
     * RawSqlExpression
     *
     * Represents a raw SQL fragment that should be rendered as-is without any escaping or quoting.
     * Used for custom SQL expressions that don't fit into the standard AST structure.
     *
     * @property sql The raw SQL string to render
     */
    data class RawSqlExpression(val sql: String) : SpecialExpression()
}
