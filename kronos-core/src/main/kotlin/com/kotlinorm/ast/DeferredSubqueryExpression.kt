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

/**
 * Deferred subquery expressions are builder-stage nodes. They must be lowered to concrete
 * [SubqueryExpression] or [SpecialExpression] nodes before SQL rendering.
 */
sealed class DeferredSubqueryExpression : Expression {
    data class Scalar(val query: SelectQueryRef) : DeferredSubqueryExpression()

    data class Exists(
        val query: SelectQueryRef,
        val not: Boolean = false
    ) : DeferredSubqueryExpression()

    data class In(
        val value: Expression,
        val query: SelectQueryRef,
        val not: Boolean = false
    ) : DeferredSubqueryExpression()

    data class QuantifiedComparison(
        val expression: Expression,
        val operator: SqlOperator,
        val quantifier: SubqueryExpression.Quantifier,
        val query: SelectQueryRef
    ) : DeferredSubqueryExpression()
}
