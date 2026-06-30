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

object SubqueryValidator {
    private val aggregateFunctionNames = setOf("COUNT", "SUM", "AVG", "MIN", "MAX")

    fun validateScalar(statement: SelectStatement) {
        require(statement.selectList.size == 1) {
            "Scalar subquery must select exactly one column or expression."
        }

        if (isAggregateWithoutGroupBy(statement)) {
            return
        }

        require(statement.limit?.limit == 1) {
            "Non-aggregate scalar subquery must explicitly use limit(1)."
        }
    }

    fun validateInSubquery(value: Expression, statement: SelectStatement) {
        val leftArity = when (value) {
            is RowValueExpression -> value.values.size
            else -> 1
        }
        val rightArity = statement.selectList.size

        require(leftArity == rightArity) {
            "IN subquery column count mismatch: left side has $leftArity column(s), but subquery selects $rightArity column(s)."
        }
    }

    private fun isAggregateWithoutGroupBy(statement: SelectStatement): Boolean {
        if (!statement.groupBy.isNullOrEmpty()) return false
        val item = statement.selectList.singleOrNull() ?: return false
        val expression = when (item) {
            is SelectItem.ExpressionSelectItem -> item.expression
            else -> return false
        }
        return expression.containsPlainAggregate()
    }

    private fun Expression.containsPlainAggregate(): Boolean {
        return when (this) {
            is FunctionCall -> {
                over == null && functionName.uppercase() in aggregateFunctionNames ||
                    arguments.any { it.containsPlainAggregate() } ||
                    filter?.containsPlainAggregate() == true
            }
            is BinaryExpression -> left.containsPlainAggregate() || right.containsPlainAggregate()
            is UnaryExpression -> operand.containsPlainAggregate()
            is CaseExpression.SimpleCaseExpression ->
                operand.containsPlainAggregate() ||
                    whenClauses.any {
                        it.whenCondition.containsPlainAggregate() || it.thenResult.containsPlainAggregate()
                    } ||
                    elseResult?.containsPlainAggregate() == true
            is CaseExpression.SearchedCaseExpression ->
                whenClauses.any {
                    it.whenCondition.containsPlainAggregate() || it.thenResult.containsPlainAggregate()
                } ||
                    elseResult?.containsPlainAggregate() == true
            is RowValueExpression -> values.any { it.containsPlainAggregate() }
            is SpecialExpression.BetweenExpression ->
                value.containsPlainAggregate() ||
                    low.containsPlainAggregate() ||
                    high.containsPlainAggregate()
            is SpecialExpression.InExpression ->
                value.containsPlainAggregate() || values.any { it.containsPlainAggregate() }
            is SpecialExpression.InSubqueryExpression -> value.containsPlainAggregate()
            is SpecialExpression.IsNullExpression -> expression.containsPlainAggregate()
            is SpecialExpression.LikeExpression ->
                value.containsPlainAggregate() ||
                    pattern.containsPlainAggregate() ||
                    escape?.containsPlainAggregate() == true
            is SubqueryExpression.QuantifiedComparison -> expression.containsPlainAggregate()
            is DeferredSubqueryExpression.QuantifiedComparison -> expression.containsPlainAggregate()
            else -> false
        }
    }
}
