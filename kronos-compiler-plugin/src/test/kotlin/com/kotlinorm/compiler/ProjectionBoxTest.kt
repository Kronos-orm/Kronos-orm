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
     * Verifies generated and derived projections preserve source serialization metadata.
     */
    @Test
    fun serializedProjectionMetadata() = box("serializedProjectionMetadata")

    /**
     * Verifies single-source select forms keep Source as Selected and Context.
     */
    @Test
    fun identitySourceProjectionKeepsSourceType() = box("identitySourceProjectionKeepsSourceType")

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
     * Verifies non-root JOIN cascade projections keep hidden local keys and load their relation.
     */
    @Test
    fun joinNonRootCascadeProjection() = box("joinNonRootCascadeProjection")

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

    /**
     * Verifies filter exposes generated projection aliases through a derived-query condition receiver.
     */
    @Test
    fun generatedProjectionFilterReceiver() = box("generatedProjectionFilterReceiver")

    /**
     * Verifies filter exposes generated window aliases through a derived-query condition receiver.
     */
    @Test
    fun windowAliasFilter() = box("windowAliasFilter")

    /**
     * Verifies filter boundaries and Selected receivers across JOIN, UNION, and already-derived queries.
     */
    @Test
    fun filterSelectableBoundaryMatrix() = box("filterSelectableBoundaryMatrix")

    /**
     * Verifies aggregate, scalar, serialized, and custom projection aliases remain filterable.
     */
    @Test
    fun filterAliasReceiverMatrix() = box("filterAliasReceiverMatrix")

    /**
     * Verifies an opted-in same-name alias keeps the selected expression type through mapping.
     */
    @Test
    fun selectedAliasOverrideType() = box("selectedAliasOverrideType")

    /**
     * Verifies source-minus removes a shadowed source field before Context merging.
     */
    @Test
    fun sourceMinusAliasRestoresContext() = box("sourceMinusAliasRestoresContext")

    /**
     * Verifies nested generic source aliases retain their generated property type.
     */
    @Test
    fun genericSourceAliasProjectionType() = box("genericSourceAliasProjectionType")

    /**
     * Verifies duplicate output names stay unique through SQL, mapping, derived select, and union.
     */
    @Test
    fun duplicateProjectionOutputNames() = box("duplicateProjectionOutputNames")

    /**
     * Verifies projection return types across derived, JOIN, UNION, offset, and cursor layers.
     */
    @Test
    fun projectionQueryLayerCoverageMatrix() = box("projectionQueryLayerCoverageMatrix")

    /**
     * Verifies generic variance and star projections survive generated projection materialization.
     */
    @Test
    fun projectionGenericVarianceCoverage() = box("projectionGenericVarianceCoverage")

    /**
     * Verifies same-named non-Kronos APIs are left untouched by projection call refinement.
     */
    @Test
    fun projectionCallShapeIsolation() = box("projectionCallShapeIsolation")

    /**
     * Verifies JOIN and UNION Selected values retain their scalar and predicate subquery shapes.
     */
    @Test
    fun projectionSelectableLayerSubqueryMatrix() = box("projectionSelectableLayerSubqueryMatrix")

    /**
     * Verifies implicit source receivers and shadowed JOIN lambda names keep lexical ownership.
     */
    @Test
    fun projectionReceiverScopeMatrix() = box("projectionReceiverScopeMatrix")

    /**
     * Verifies captured non-source properties and constant aliases retain their concrete generated types.
     */
    @Test
    fun projectionCapturedValueTypes() = box("projectionCapturedValueTypes")

}
