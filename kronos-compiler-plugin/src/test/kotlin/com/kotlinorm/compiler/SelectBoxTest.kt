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
 * Covers select-field DSL transformation.
 *
 * The suite checks that selected properties, collection-literal field lists, aliases,
 * and function calls are lowered into the projection structures used by select SQL
 * generation.
 */
class SelectBoxTest : AbstractKronosJvmBoxSuite("select") {
    /**
     * Verifies selected property lists are transformed into ordered Kronos field projections.
     */
    @Test
    fun projectionFields() = box("projectionFields")

    /**
     * Verifies select field metadata preserves annotation-derived field attributes.
     */
    @Test
    fun fieldMetadataProjection() = box("fieldMetadataProjection")

    /**
     * Verifies select functions and aliases are transformed into function projections.
     */
    @Test
    fun functionFields() = box("functionFields")

    /**
     * Verifies @KronosFunction overrides the SQL function identifier.
     */
    @Test
    fun kronosFunctionAnnotationName() = box("kronosFunctionAnnotationName")

    /**
     * Verifies KPojo minus projection excludes selected properties.
     */
    @Test
    fun minusProjection() = box("minusProjection")

    /**
     * Verifies KPojo minus projection accepts property references and reference collections.
     */
    @Test
    fun minusPropertyReferenceForms() = box("minusPropertyReferenceForms")

    /**
     * Verifies selecting the KPojo receiver expands to all column fields.
     */
    @Test
    fun getValueAllFields() = box("getValueAllFields")

    /**
     * Verifies Kotlin collection factory calls are accepted as projection lists.
     */
    @Test
    fun collectionFactoryFields() = box("collectionFactoryFields")

    /**
     * Verifies property references can be used as field expressions.
     */
    @Test
    fun propertyReferenceFields() = box("propertyReferenceFields")

    /**
     * Verifies binary operator expressions are lowered into function projections.
     */
    @Test
    fun operatorFunctionFields() = box("operatorFunctionFields")

    /**
     * Verifies arithmetic, concat, and nested function projection variants.
     */
    @Test
    fun functionAndOperatorProjectionMatrix() = box("functionAndOperatorProjectionMatrix")

    /**
     * Verifies collection literal, listOf, and arrayOf projection items preserve mixed projection forms.
     */
    @Test
    fun projectionCollectionVariantItems() = box("projectionCollectionVariantItems")

    /**
     * Verifies raw SQL and raw SQL alias projection forms are lowered into select items.
     */
    @Test
    fun rawSqlAndAliasProjectionForms() = box("rawSqlAndAliasProjectionForms")

    /**
     * Verifies mutableListOf and setOf projection items preserve mixed projection forms.
     */
    @Test
    fun mutableSetProjectionVariantItems() = box("mutableSetProjectionVariantItems")

    /**
     * Verifies window function OVER clauses are lowered into function metadata.
     */
    @Test
    fun windowFunctionOver() = box("windowFunctionOver")

    /**
     * Verifies window partition and order variants are lowered into syntax window expressions.
     */
    @Test
    fun windowFunctionPartitionOrderMatrix() = box("windowFunctionPartitionOrderMatrix")

    /**
     * Verifies aliased selectable expressions are lowered into scalar subquery select items.
     */
    @Test
    fun scalarSubquerySelectItem() = box("scalarSubquerySelectItem")

    /**
     * Verifies scalar subquery type-hint casts do not change select item lowering.
     */
    @Test
    fun scalarSubquerySelectItemTypeHint() = box("scalarSubquerySelectItemTypeHint")
}
