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

package com.kotlinorm.compiler.official

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
}
