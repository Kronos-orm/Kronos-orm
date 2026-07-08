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
 * Covers typed query return metadata injection.
 *
 * These tests verify that query APIs such as `queryList<T>()` and `queryOneOrNull<T>()`
 * receive the compiler-injected KPojo/type-hierarchy metadata needed by runtime
 * result mapping.
 */
class TypeParameterFixerBoxTest : AbstractKronosJvmBoxSuite("typeParameterFixer") {
    /**
     * Verifies query return APIs receive compiler-injected KPojo/type hierarchy metadata.
     */
    @Test
    fun queryReturnTypes() = box("queryReturnTypes")

    /**
     * Verifies non-KPojo typed query APIs receive explicit non-KPojo metadata.
     */
    @Test
    fun nonKPojoQueryReturnTypes() = box("nonKPojoQueryReturnTypes")

    /**
     * Verifies the KPojo interface type itself is recognized as KPojo metadata.
     */
    @Test
    fun kpojoInterfaceQueryReturnType() = box("kpojoInterfaceQueryReturnType")
}
