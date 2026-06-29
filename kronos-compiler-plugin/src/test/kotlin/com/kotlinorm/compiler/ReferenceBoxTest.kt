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
 * Covers reference and cascade-reference DSL transformation.
 *
 * These tests validate property-reference extraction for normal reference lists and
 * cascade selections, including collection-literal reference syntax.
 */
class ReferenceBoxTest : AbstractKronosJvmBoxSuite("reference") {
    /**
     * Verifies ordinary property-reference extraction from reference DSL lambdas.
     */
    @Test
    fun referenceFields() = box("referenceFields")

    /**
     * Verifies cascade reference extraction, including collection relationship fields.
     */
    @Test
    fun cascadeReferences() = box("cascadeReferences")
}
