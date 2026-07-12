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
     * Verifies bare select lambdas refine toList() to generated projection row types.
     */
    @Test
    fun generatedSelectProjection() = box("generatedSelectProjection")

    /**
     * Verifies toList/first/firstOrNull refine to generated projection row types.
     */
    @Test
    fun queryReturnTypeVariants() = box("queryReturnTypeVariants")

    /**
     * Verifies supported collection constructors and literals feed generated receivers.
     */
    @Test
    fun projectionCollectionForms() = box("projectionCollectionForms")

    /**
     * Verifies whole-source and source-minus expressions generate matching projection rows.
     */
    @Test
    fun kpojoExpansionGeneratedProjection() = box("kpojoExpansionGeneratedProjection")

    /**
     * Verifies explicit KPojo metadata overrides are excluded from whole-source projection fields.
     */
    @Test
    fun metadataOverridesExcludedFromProjectionFields() = box("metadataOverridesExcludedFromProjectionFields")

    /**
     * Verifies collection literal and listOf projection forms feed generated receivers.
     */
    @Test
    fun collectionLiteralAndListProjectionReceivers() = box("collectionLiteralAndListProjectionReceivers")

    /**
     * Verifies generated projection properties can be selected again as field expressions.
     */
    @Test
    fun generatedProjectionReselectFields() = box("generatedProjectionReselectFields")

    /**
     * Verifies cascade projections keep hidden local key fields needed by after-query loading.
     */
    @Test
    fun cascadeProjectionKeepsLocalKey() = box("cascadeProjectionKeepsLocalKey")

    /**
     * Verifies direct single-object cascade projections keep hidden local key fields.
     */
    @Test
    fun directCascadeProjectionKeepsLocalKey() = box("directCascadeProjectionKeepsLocalKey")

    /**
     * Verifies select aliases are visible on post-select orderBy Context receivers.
     */
    @Test
    fun selectAliasContextOrderBy() = box("selectAliasContextOrderBy")

    /**
     * Verifies post-select orderBy Context receivers still expose Source fields.
     */
    @Test
    fun sourceFieldContextOrderBy() = box("sourceFieldContextOrderBy")

    /**
     * Verifies scalar subquery aliases are visible on generated Selected and orderBy Context receivers.
     */
    @Test
    fun scalarSubqueryAliasContextOrderBy() = box("scalarSubqueryAliasContextOrderBy")

    /**
     * Verifies scalar subquery alias receiver type variants refine generated projection types.
     */
    @Test
    fun scalarSubqueryAliasReceiverTypeVariants() = box("scalarSubqueryAliasReceiverTypeVariants")

    /**
     * Verifies function and aggregate aliases are visible on generated Selected and orderBy Context receivers.
     */
    @Test
    fun functionAliasContext() = box("functionAliasContext")

    /**
     * Verifies function projection aliases are generated as query-mapped projection properties.
     */
    @Test
    fun generatedFunctionAliasPropertyAccess() = box("generatedFunctionAliasPropertyAccess")

    /**
     * Verifies string literal projection aliases become generated projection properties.
     */
    @Test
    fun stringLiteralAliasProjectionField() = box("stringLiteralAliasProjectionField")

    /**
     * Verifies window aliases are visible on post-select orderBy Context receivers.
     */
    @Test
    fun windowAliasContextOrderBy() = box("windowAliasContextOrderBy")

    /**
     * Verifies generated window aliases remain available to derived query where receivers.
     */
    @Test
    fun windowAliasDerivedWhere() = box("windowAliasDerivedWhere")
}
