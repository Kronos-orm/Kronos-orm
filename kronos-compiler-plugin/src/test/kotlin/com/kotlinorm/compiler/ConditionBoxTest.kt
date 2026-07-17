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
 * negation, and advanced condition operators are transformed into syntax `SqlExpr`
 * structures after FIR resolution and IR rewriting.
 */
class ConditionBoxTest : AbstractKronosJvmBoxSuite("condition") {
    /**
     * Verifies comparison operators are transformed into the expected syntax expressions.
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
     * Verifies negation and minus/exclusion forms in condition DSL transformation.
     */
    @Test
    fun negationAndMinus() = box("negationAndMinus")

    /**
     * Verifies no-argument string match properties use the current KPojo values.
     */
    @Test
    fun noArgStringMatch() = box("noArgStringMatch")

    /**
     * Verifies functions and operator expressions can be compared from either side.
     */
    @Test
    fun functionComparisonBranches() = box("functionComparisonBranches")

    /**
     * Verifies function and operator comparison variants cover every comparison direction.
     */
    @Test
    fun functionComparisonMatrix() = box("functionComparisonMatrix")

    /**
     * Verifies reversed equality and function/field RHS values lower to SQL expressions.
     */
    @Test
    fun reversedEqualityFunctionAndField() = box("reversedEqualityFunctionAndField")

    /**
     * Verifies no-argument negation and KPojo-minus equality.
     */
    @Test
    fun noArgNegatedAndKPojoMinus() = box("noArgNegatedAndKPojoMinus")

    /**
     * Verifies no-value strategies, takeIf/takeUnless, run-wrapped conditions, and negated OR lowering.
     */
    @Test
    fun noValueTakeIfBooleanMatrix() = box("noValueTakeIfBooleanMatrix")

    /**
     * Verifies if/when-shaped condition lambdas lower through IrWhen analysis.
     */
    @Test
    fun whenConditionBranches() = box("whenConditionBranches")

    /**
     * Verifies parameterized string match helpers and negation matrix.
     */
    @Test
    fun stringMatchParameterizedMatrix() = box("stringMatchParameterizedMatrix")

    /**
     * Verifies block-wrapped condition expressions lower into a combined AND tree.
     */
    @Test
    fun blockBodyMultipleCriteria() = box("blockBodyMultipleCriteria")

    /**
     * Verifies method-style condition helpers produce precise syntax expressions.
     */
    @Test
    fun methodCriteria() = box("methodCriteria")

    /**
     * Verifies whole-KPojo equality expands to per-column syntax expressions.
     */
    @Test
    fun kpojoEquality() = box("kpojoEquality")

    /**
     * Verifies KPojo equality with no column properties is ignored.
     */
    @Test
    fun kpojoEqualityNoColumns() = box("kpojoEqualityNoColumns")

    /**
     * Verifies no-argument comparison properties use current KPojo values.
     */
    @Test
    fun noArgComparison() = box("noArgComparison")

    /**
     * Verifies parameterized eq/gt calls are not recognized as Kronos condition DSL.
     */
    @Test
    fun parameterizedConditionFunctionsIgnored() = box("parameterizedConditionFunctionsIgnored")

    /**
     * Verifies reversed comparisons normalize operators against the KPojo field.
     */
    @Test
    fun reversedComparison() = box("reversedComparison")

    /**
     * Verifies string contains and array membership syntax expressions.
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
     * Verifies literal null predicates and relationship safe-call field RHS expressions.
     */
    @Test
    fun literalNullAndSafeCallFields() = box("literalNullAndSafeCallFields")

    /**
     * Verifies operator function fields work inside comparisons and string match values.
     */
    @Test
    fun operatorFunctionCriteria() = box("operatorFunctionCriteria")

    /**
     * Verifies field-in-selectable conditions produce syntax subquery operands.
     */
    @Test
    fun fieldInSelectableSubquery() = box("fieldInSelectableSubquery")

    /**
     * Verifies query.contains(field) conditions produce syntax subquery operands.
     */
    @Test
    fun selectableContainsFieldSubquery() = box("selectableContainsFieldSubquery")

    /**
     * Verifies negated field-in-selectable conditions keep syntax subquery operands.
     */
    @Test
    fun fieldNotInSelectableSubquery() = box("fieldNotInSelectableSubquery")

    /**
     * Verifies tuple field-in-selectable conditions produce row-value subquery expressions.
     */
    @Test
    fun tupleInSelectableSubquery() = box("tupleInSelectableSubquery")

    /**
     * Verifies negated tuple field-in-selectable conditions produce row-value subquery expressions.
     */
    @Test
    fun tupleNotInSelectableSubquery() = box("tupleNotInSelectableSubquery")

    /**
     * Verifies field comparisons against selectable queries produce scalar subquery expressions.
     */
    @Test
    fun scalarSubqueryComparison() = box("scalarSubqueryComparison")

    /**
     * Verifies scalar subquery type-hint casts do not change syntax lowering.
     */
    @Test
    fun scalarSubqueryTypeHint() = box("scalarSubqueryTypeHint")

    /**
     * Verifies quantified comparisons against selectable queries produce syntax predicates.
     */
    @Test
    fun quantifiedSubqueryComparison() = box("quantifiedSubqueryComparison")

    /**
     * Verifies exists-selectable conditions produce syntax EXISTS predicates.
     */
    @Test
    fun existsSelectableSubquery() = box("existsSelectableSubquery")

    /**
     * Verifies negated exists-selectable conditions produce NOT EXISTS predicates.
     */
    @Test
    fun notExistsSelectableSubquery() = box("notExistsSelectableSubquery")

    /**
     * Verifies boundary condition forms still lower through SQL expression transformation.
     */
    @Test
    fun conditionBoundaryMatrix() = box("conditionBoundaryMatrix")

    /**
     * Verifies type-operator wrapped fields still lower through SQL expression transformation.
     */
    @Test
    fun typeOperatorFieldConditions() = box("typeOperatorFieldConditions")

    /**
     * Verifies generated projection fields work in where functions and operators.
     */
    @Test
    fun generatedProjectionWhereFunctionAndOperator() = box("generatedProjectionWhereFunctionAndOperator")
}
