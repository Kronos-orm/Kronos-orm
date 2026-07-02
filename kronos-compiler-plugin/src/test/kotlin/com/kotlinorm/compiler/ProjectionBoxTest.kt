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
 * Covers FIR-generated select projection result types.
 */
class ProjectionBoxTest : AbstractKronosJvmBoxSuite("projection") {
    /**
     * Verifies bare select lambdas refine queryList() to generated projection row types.
     */
    @Test
    fun generatedSelectProjection() = box("generatedSelectProjection")

    /**
     * Verifies cascade projections keep hidden local key fields needed by after-query loading.
     */
    @Test
    fun cascadeProjectionKeepsLocalKey() = box("cascadeProjectionKeepsLocalKey")

    /**
     * Verifies select aliases are visible on post-select orderBy Context receivers.
     */
    @Test
    fun selectAliasContextOrderBy() = box("selectAliasContextOrderBy")

    /**
     * Verifies scalar subquery aliases are visible on generated Selected and orderBy Context receivers.
     */
    @Test
    fun scalarSubqueryAliasContextOrderBy() = box("scalarSubqueryAliasContextOrderBy")

    /**
     * Verifies function and aggregate aliases are visible on generated Selected and orderBy Context receivers.
     */
    @Test
    fun functionAliasContext() = box("functionAliasContext")

    /**
     * Verifies window aliases are visible on post-select orderBy Context receivers.
     */
    @Test
    fun windowAliasContextOrderBy() = box("windowAliasContextOrderBy")
}
