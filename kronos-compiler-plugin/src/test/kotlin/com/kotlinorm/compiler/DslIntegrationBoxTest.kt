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
 * Covers behavior that spans multiple compiler-plugin transformations.
 *
 * These integration tests combine KPojo augmentation, select DSL transformation,
 * typed projection support, and multi-file KPojo discovery to catch bugs that do
 * not appear in isolated transformer tests.
 */
class DslIntegrationBoxTest : AbstractKronosJvmBoxSuite("dslIntegration") {
    /**
     * Verifies select clause SQL statement construction after compiler-plugin field transformation.
     */
    @Test
    fun selectClauseStatement() = box("selectClauseStatement")

    /**
     * Verifies typed DTO projection keeps the source DTO receiver while returning the projection DTO type.
     */
    @Test
    fun typedProjection() = box("typedProjection")

    /**
     * Verifies KPojo discovery and generated members across multiple source files in one box test.
     */
    @Test
    fun multiFileKPojo() = box("multiFileKPojo")

}
