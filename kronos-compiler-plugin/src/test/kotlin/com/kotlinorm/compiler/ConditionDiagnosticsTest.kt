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
 * Covers FIR diagnostics for condition DSL source registration.
 */
class ConditionDiagnosticsTest : AbstractKronosJvmDiagnosticsSuite("condition") {
    /**
     * Verifies every condition clause rejects KPojo fields from unregistered SQL sources.
     */
    @Test
    fun unregisteredKPojoProperty() = diagnostics("unregisteredKPojoProperty")

    /**
     * Verifies registered, correlated, joined, and explicit Kotlin-value sources stay valid.
     */
    @Test
    fun conditionSourcePositiveMatrix() = diagnostics("conditionSourcePositiveMatrix")
}
