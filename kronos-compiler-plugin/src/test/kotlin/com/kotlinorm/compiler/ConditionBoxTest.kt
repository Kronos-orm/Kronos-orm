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

package com.kotlinorm.compiler

import org.junit.jupiter.api.Test

/**
 * Exercises condition DSL transformation through real compiler execution.
 *
 * These tests validate that boolean expressions, comparisons, collection membership,
 * negation, and advanced condition operators are transformed into Kronos `Criteria`
 * structures correctly after FIR resolution and IR rewriting.
 */
class ConditionBoxTest : AbstractKronosJvmBoxSuite("condition") {
    /**
     * Verifies comparison operators are transformed into the expected Kronos criteria.
     */
    @Test
    fun comparisonOperators() = box("comparisonOperators")

    /**
     * Verifies collection membership and boolean composition in condition DSL lambdas.
     */
    @Test
    fun collectionAndBoolean() = box("collectionAndBoolean")

    /**
     * Verifies advanced condition helpers such as like, null checks, and range-style operations.
     */
    @Test
    fun advancedOperators() = box("advancedOperators")

    /**
     * Verifies negated criteria and minus/exclusion forms in condition DSL transformation.
     */
    @Test
    fun negationAndMinus() = box("negationAndMinus")

    /**
     * Verifies no-argument string match properties use the current KPojo values.
     */
    @Test
    fun noArgStringMatch() = box("noArgStringMatch")

    /**
     * Verifies method-style condition helpers produce precise Criteria nodes.
     */
    @Test
    fun methodCriteria() = box("methodCriteria")

    /**
     * Verifies whole-KPojo equality expands to per-column criteria.
     */
    @Test
    fun kpojoEquality() = box("kpojoEquality")

    /**
     * Verifies no-argument comparison properties use current KPojo values.
     */
    @Test
    fun noArgComparison() = box("noArgComparison")

    /**
     * Verifies reversed comparisons normalize operators against the KPojo field.
     */
    @Test
    fun reversedComparison() = box("reversedComparison")

    /**
     * Verifies string contains and array membership criteria.
     */
    @Test
    fun containsAndArrayMembership() = box("containsAndArrayMembership")

    /**
     * Verifies not-equal and OR condition transformations.
     */
    @Test
    fun notEqualAndOr() = box("notEqualAndOr")

    /**
     * Verifies field-valued and raw value RHS condition expressions.
     */
    @Test
    fun fieldValueCriteria() = box("fieldValueCriteria")

    /**
     * Verifies operator function fields work inside comparisons and string match values.
     */
    @Test
    fun operatorFunctionCriteria() = box("operatorFunctionCriteria")

    /**
     * Verifies field-in-selectable conditions produce deferred subquery criteria values.
     */
    @Test
    fun fieldInSelectableSubquery() = box("fieldInSelectableSubquery")

    /**
     * Verifies negated field-in-selectable conditions keep deferred subquery criteria values.
     */
    @Test
    fun fieldNotInSelectableSubquery() = box("fieldNotInSelectableSubquery")

    /**
     * Verifies tuple field-in-selectable conditions produce row-value subquery criteria.
     */
    @Test
    fun tupleInSelectableSubquery() = box("tupleInSelectableSubquery")

    /**
     * Verifies negated tuple field-in-selectable conditions produce row-value subquery criteria.
     */
    @Test
    fun tupleNotInSelectableSubquery() = box("tupleNotInSelectableSubquery")

    /**
     * Verifies field comparisons against selectable queries produce scalar subquery criteria.
     */
    @Test
    fun scalarSubqueryComparison() = box("scalarSubqueryComparison")

    /**
     * Verifies quantified comparisons against selectable queries produce structured criteria.
     */
    @Test
    fun quantifiedSubqueryComparison() = box("quantifiedSubqueryComparison")

    /**
     * Verifies exists-selectable conditions produce deferred subquery criteria values.
     */
    @Test
    fun existsSelectableSubquery() = box("existsSelectableSubquery")

    /**
     * Verifies negated exists-selectable conditions produce NOT EXISTS criteria values.
     */
    @Test
    fun notExistsSelectableSubquery() = box("notExistsSelectableSubquery")
}
