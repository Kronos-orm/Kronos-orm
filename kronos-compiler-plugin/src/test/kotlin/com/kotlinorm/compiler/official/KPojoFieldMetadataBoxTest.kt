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

package com.kotlinorm.compiler.official

import org.junit.jupiter.api.Test

/**
 * Verifies metadata extracted from KPojo declarations and annotations.
 *
 * The suite covers table names, column annotations, common strategies, and ignore
 * rules as they appear in generated `kronosColumns()` and related KPojo members.
 */
class KPojoFieldMetadataBoxTest : AbstractKronosJvmBoxSuite("kpojoFieldMetadata") {
    /**
     * Verifies table name metadata from `@Table` and naming strategy fallback.
     */
    @Test
    fun tableMetadata() = box("tableMetadata")

    /**
     * Verifies generated column metadata from column-related annotations.
     */
    @Test
    fun columnMetadata() = box("columnMetadata")

    /**
     * Verifies generated common strategy metadata such as primary key and lifecycle fields.
     */
    @Test
    fun strategyMetadata() = box("strategyMetadata")

    /**
     * Verifies ignore rules that control generated column and map-conversion behavior.
     */
    @Test
    fun ignoreRules() = box("ignoreRules")
}
