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
 * Final migration-batch regression tests for utility-backed compiler behavior.
 *
 * Pure utility classes remain covered by ordinary unit tests; these cases keep
 * the parts that become observable only after the Kronos plugin runs through the
 * official compiler pipeline.
 */
class RegressionBoxTest : AbstractKronosJvmBoxSuite("regression") {
    /**
     * Verifies default annotation paths and Kotlin-type-to-column-type mapping.
     */
    @Test
    fun annotationDefaultsAndTypeMapping() = box("annotationDefaultsAndTypeMapping")

    /**
     * Verifies table KDoc extraction through generated `__tableComment`.
     */
    @Test
    fun tableCommentFromKDoc() = box("tableCommentFromKDoc")

    /**
     * Verifies symbol-backed generation remains stable with several KPojo classes.
     */
    @Test
    fun multiKPojoSymbolRegression() = box("multiKPojoSymbolRegression")
}
