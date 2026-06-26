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
 * SubqueryExpression
 *
 * Sealed class representing subquery expressions used in various SQL contexts. Supports EXISTS,
 * scalar subqueries, and quantified comparisons (ALL, ANY, SOME).
 *
 * @author OUSC
 */
sealed class SubqueryExpression : Expression {
    /**
     * ExistsExpression
     *
     * Represents an EXISTS subquery: EXISTS (SELECT ...).
     *
     * @property subquery The subquery SELECT statement
     * @property not Whether to use NOT EXISTS
     */
    data class ExistsExpression(val subquery: SelectStatement, val not: Boolean = false) :
            SubqueryExpression()

    /**
     * ScalarSubquery
     *
     * Represents a scalar subquery that returns a single value: (SELECT ...). Used in SELECT list,
     * WHERE clause, etc.
     *
     * @property subquery The subquery SELECT statement
     */
    data class ScalarSubquery(val subquery: SelectStatement) : SubqueryExpression()

    /**
     * QuantifiedComparison
     *
     * Represents a quantified comparison with a subquery: expression operator ALL/ANY/SOME (SELECT
     * ...).
     *
     * @property expression The expression to compare
     * @property operator The comparison operator
     * @property quantifier The quantifier (ALL, ANY, SOME)
     * @property subquery The subquery SELECT statement
     */
    data class QuantifiedComparison(
            val expression: Expression,
            val operator: SqlOperator,
            val quantifier: Quantifier,
            val subquery: SelectStatement
    ) : SubqueryExpression()

    /**
     * Quantifier
     *
     * Quantifier for subquery comparisons.
     */
    enum class Quantifier {
        ALL,
        ANY,
        SOME
    }
}
