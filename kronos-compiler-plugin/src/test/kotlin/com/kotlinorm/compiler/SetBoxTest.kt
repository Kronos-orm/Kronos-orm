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
 * Covers update/set DSL assignment transformation.
 *
 * These tests ensure property assignments and arithmetic update operations become
 * Kronos `setValue` / `setAssign` calls without leaving invalid or duplicated IR
 * expression nodes in the transformed lambda body.
 */
class SetBoxTest : AbstractKronosJvmBoxSuite("set") {
    /**
     * Verifies simple property assignments in update/set DSL lambdas.
     */
    @Test
    fun assignments() = box("assignments")

    /**
     * Verifies mixed assignment forms, including arithmetic and typed values.
     */
    @Test
    fun diverseAssignments() = box("diverseAssignments")
}
