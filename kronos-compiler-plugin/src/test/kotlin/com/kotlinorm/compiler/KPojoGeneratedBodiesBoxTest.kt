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
 * Verifies executable bodies generated for KPojo interface members.
 *
 * These tests exercise generated `toDataMap`, `fromMapData`, and dynamic `get/set`
 * implementations. They are especially useful because the official runner's IR
 * verifier catches invalid generated IR such as reused expression nodes.
 */
class KPojoGeneratedBodiesBoxTest : AbstractKronosJvmBoxSuite("kpojoGeneratedBodies") {
    /**
     * Verifies that generated `toDataMap()` returns property values under Kronos field names.
     */
    @Test
    fun toDataMap() = box("toDataMap")

    /**
     * Verifies that generated `fromMapData()` creates a KPojo instance from map values.
     */
    @Test
    fun fromMapData() = box("fromMapData")

    /**
     * Verifies generated dynamic `get` and `set` operators, including immutable property handling.
     */
    @Test
    fun dynamicAccessors() = box("dynamicAccessors")

    /**
     * Verifies that source `val` KPojo properties are still writable by generated mapping bodies.
     */
    @Test
    fun valPropertiesAreWritable() = box("valPropertiesAreWritable")
}
