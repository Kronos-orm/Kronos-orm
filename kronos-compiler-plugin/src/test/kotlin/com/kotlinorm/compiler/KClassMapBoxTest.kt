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
 * Verifies generated KClass creator map support.
 *
 * The suite checks that KPojo classes discovered by the compiler plugin are added
 * to the constructor map used by Kronos runtime mapping without requiring reflection.
 */
class KClassMapBoxTest : AbstractKronosJvmBoxSuite("kclassMap") {
    /**
     * Verifies compiler-generated KClass creator entries for reflection-free KPojo construction.
     */
    @Test
    fun kclassCreator() = box("kclassCreator")
}
