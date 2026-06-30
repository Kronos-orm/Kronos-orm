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
 * Internal structured values accepted by [CriteriaToAstConverter] for subquery predicates.
 * These are builder/compiler handoff objects, not user-facing DSL entry points.
 */
sealed class CriteriaSubqueryValue {
    data class Scalar(
        val query: SelectQueryRef
    ) : CriteriaSubqueryValue()

    data class Exists(
        val query: SelectQueryRef,
        val not: Boolean = false
    ) : CriteriaSubqueryValue()

    data class In(
        val query: SelectQueryRef,
        val value: Any? = null,
        val not: Boolean = false
    ) : CriteriaSubqueryValue()

    data class QuantifiedComparison(
        val query: SelectQueryRef,
        val quantifier: SubqueryExpression.Quantifier
    ) : CriteriaSubqueryValue()
}
