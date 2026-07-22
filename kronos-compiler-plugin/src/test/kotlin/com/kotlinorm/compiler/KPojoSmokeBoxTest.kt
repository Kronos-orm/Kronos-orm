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
 * Covers the smallest end-to-end KPojo augmentation scenario.
 *
 * The test data checks that a simple KPojo receives generated members such as table
 * metadata, data-map conversion, and column metadata when compiled through the real
 * FIR-to-IR pipeline.
 */
class KPojoSmokeBoxTest : AbstractKronosJvmBoxSuite("kpojoSmoke") {
    /**
     * Verifies the smallest complete KPojo transformation, including generated metadata and map conversion.
     */
    @Test
    fun generatedMembers() = box("generatedMembers")

    /**
     * Verifies that the generated provider matches exact KType keys and invokes a direct
     * no-argument/default-argument constructor without reflective fallback.
     */
    @Test
    fun generatedFactoryProvider() = box("generatedFactoryProvider")

    /**
     * Verifies an indirect KPojo interface remains an interface while its concrete implementation is generated.
     */
    @Test
    fun indirectKPojoInterface() = box("indirectKPojoInterface")

    /**
     * Verifies indirect KPojo fallback does not replace explicit user implementations.
     */
    @Test
    fun indirectKPojoExplicitOverrides() = box("indirectKPojoExplicitOverrides")

    /**
     * Verifies generated providers contribute visible declarations and omit KPojo shapes
     * that cannot be constructed without an enclosing instance or required arguments.
     */
    @Test
    fun generatedProviderVisibility() = box("generatedProviderVisibility")

    /**
     * Verifies automatic enum metadata discovery across scalar, nullable, collection, nested,
     * and serialized field types while honoring directional and all-direction ignore rules.
     */
    @Test
    fun generatedEnumMetadata() = box("generatedEnumMetadata")

    /**
     * Verifies enum metadata preserves declaration order and name lookup does not depend on
     * ordinal values or reflective enum lookup.
     */
    @Test
    fun generatedEnumDeclarationOrder() = box("generatedEnumDeclarationOrder")

    /**
     * Verifies module-unique providers coexist through service loading and do not re-contribute
     * dependency factories into the consuming module.
     */
    @Test
    fun moduleUniqueProviders() = box("moduleUniqueProviders")
}
