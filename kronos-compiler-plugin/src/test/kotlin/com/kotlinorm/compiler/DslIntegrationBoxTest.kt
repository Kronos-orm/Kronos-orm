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
 * Covers behavior that spans multiple compiler-plugin transformations.
 *
 * These integration tests combine KPojo augmentation, select DSL transformation,
 * typed projection support, and multi-file KPojo discovery to catch bugs that do
 * not appear in isolated transformer tests.
 */
class DslIntegrationBoxTest : AbstractKronosJvmBoxSuite("dslIntegration") {
    /**
     * Verifies select clause SQL statement construction after compiler-plugin field transformation.
     */
    @Test
    fun selectClauseStatement() = box("selectClauseStatement")

    /**
     * Verifies typed DTO projection keeps the source DTO receiver while returning the projection DTO type.
     */
    @Test
    fun typedProjection() = box("typedProjection")

    /**
     * Verifies function-local DTO projections can be instantiated through query result mapping.
     */
    @Test
    fun localTypedProjection() = box("localTypedProjection")

    /**
     * Verifies KPojo.where is select().where() sugar and keeps Source as the query result type.
     */
    @Test
    fun kpojoWhereSugar() = box("kpojoWhereSugar")

    /**
     * Verifies select() without a projection exposes the original Source in the next query layer.
     */
    @Test
    fun selectNoProjectionNextSource() = box("selectNoProjectionNextSource")

    /**
     * Verifies join(KSelectable) exposes the right query's Selected type to the join lambda.
     */
    @Test
    fun joinSelectableSource() = box("joinSelectableSource")

    /**
     * Verifies join select generated projections flow into no-arg queryList.
     */
    @Test
    fun joinSelectGeneratedQueryReturn() = box("joinSelectGeneratedQueryReturn")

    /**
     * Verifies insert-select value lambdas expose the source query's generated Selected type.
     */
    @Test
    fun insertSelectableGeneratedReceiver() = box("insertSelectableGeneratedReceiver")

    /**
     * Verifies derived insert-select sources expose the derived query's generated Selected type.
     */
    @Test
    fun insertSelectableDerivedGeneratedReceiver() = box("insertSelectableDerivedGeneratedReceiver")

    /**
     * Verifies join insert-select value lambdas expose the join query's generated Selected type.
     */
    @Test
    fun joinInsertGeneratedReceiver() = box("joinInsertGeneratedReceiver")

    /**
     * Verifies union results expose the first branch Selected type as the next-layer source.
     */
    @Test
    fun unionSelectableGeneratedReceiver() = box("unionSelectableGeneratedReceiver")

    /**
     * Verifies union insert-select value lambdas expose the union source's generated Selected type.
     */
    @Test
    fun unionInsertGeneratedReceiver() = box("unionInsertGeneratedReceiver")

    /**
     * Verifies insert-select value lambdas treat scalar subquery casts as type hints.
     */
    @Test
    fun insertSelectableScalarTypeHint() = box("insertSelectableScalarTypeHint")

    /**
     * Verifies insert-select value lowering for fields, functions, and operators.
     */
    @Test
    fun insertSelectValueExpressionMatrix() = box("insertSelectValueExpressionMatrix")

    /**
     * Verifies insert-select target filtering is reflected in emitted target columns.
     */
    @Test
    fun insertSelectTargetFilteringStatement() = box("insertSelectTargetFilteringStatement")

    /**
     * Verifies KPojo discovery and generated members across multiple source files in one box test.
     */
    @Test
    fun multiFileKPojo() = box("multiFileKPojo")

}
