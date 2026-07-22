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
 * Covers FIR diagnostics for unsupported KPojo declarations.
 */
class KPojoDiagnosticsTest : AbstractKronosJvmDiagnosticsSuite("kpojo") {
    /**
     * Verifies generic KPojo classes are rejected before IR generation.
     */
    @Test
    fun genericKPojoNotSupported() = diagnostics("genericKPojoNotSupported")

    /**
     * Verifies required enum metadata rejects declarations hidden from the generated provider.
     */
    @Test
    fun inaccessibleEnumMetadata() = diagnostics("inaccessibleEnumMetadata")

    /**
     * Verifies factory-eligible KPojo declarations hidden from the generated provider fail in FIR.
     */
    @Test
    fun inaccessibleKPojoFactory() = diagnostics("inaccessibleKPojoFactory")
}
