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
 * CaseExpression
 *
 * Represents a SQL CASE expression. Supports both simple CASE and searched CASE forms. CASE
 * expressions can be nested and used in various contexts (SELECT, WHERE, ORDER BY, etc.).
 *
 * @author OUSC
 */
sealed class CaseExpression : Expression {
    /**
     * SimpleCaseExpression
     *
     * Represents a simple CASE expression: CASE operand WHEN value1 THEN result1 WHEN value2 THEN
     * result2 ... ELSE elseResult END
     *
     * @property operand The operand expression to compare against
     * @property whenClauses List of WHEN-THEN pairs
     * @property elseResult Optional ELSE clause expression
     */
    data class SimpleCaseExpression(
            val operand: Expression,
            val whenClauses: List<WhenThenClause>,
            val elseResult: Expression? = null
    ) : CaseExpression()

    /**
     * SearchedCaseExpression
     *
     * Represents a searched CASE expression: CASE WHEN condition1 THEN result1 WHEN condition2 THEN
     * result2 ... ELSE elseResult END
     *
     * @property whenClauses List of WHEN-THEN pairs (condition -> result)
     * @property elseResult Optional ELSE clause expression
     */
    data class SearchedCaseExpression(
            val whenClauses: List<WhenThenClause>,
            val elseResult: Expression? = null
    ) : CaseExpression()

    /**
     * WhenThenClause
     *
     * Represents a WHEN-THEN clause in a CASE expression.
     *
     * @property whenCondition For simple CASE: the value to compare; For searched CASE: the
     * condition
     * @property thenResult The result expression when the condition matches
     */
    data class WhenThenClause(val whenCondition: Expression, val thenResult: Expression)
}
