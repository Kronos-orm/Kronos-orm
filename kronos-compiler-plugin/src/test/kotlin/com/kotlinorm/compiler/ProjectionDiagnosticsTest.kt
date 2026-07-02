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
 * Covers FIR diagnostics for projection DSL misuse.
 */
class ProjectionDiagnosticsTest : AbstractKronosJvmDiagnosticsSuite("projection") {
    /**
     * Verifies non-field select items must declare an alias.
     */
    @Test
    fun functionFieldRequiresAlias() = diagnostics("functionFieldRequiresAlias")

    /**
     * Verifies scalar subquery select items must declare an alias.
     */
    @Test
    fun scalarSubqueryRequiresAlias() = diagnostics("scalarSubqueryRequiresAlias")

    /**
     * Verifies scalar subqueries used as values must declare limit(1).
     */
    @Test
    fun scalarSubqueryRequiresLimit() = diagnostics("scalarSubqueryRequiresLimit")

    /**
     * Verifies scalar subqueries used as values must project exactly one item.
     */
    @Test
    fun scalarSubqueryRequiresSingleColumn() = diagnostics("scalarSubqueryRequiresSingleColumn")

    /**
     * Verifies predicate subqueries match field and tuple arity.
     */
    @Test
    fun predicateSubqueryColumnCount() = diagnostics("predicateSubqueryColumnCount")

    /**
     * Verifies selected projection property names must be unique.
     */
    @Test
    fun duplicateProjectionProperty() = diagnostics("duplicateProjectionProperty")

    /**
     * Verifies new selected aliases cannot conflict with source field names.
     */
    @Test
    fun selectedAliasConflictsWithSource() = diagnostics("selectedAliasConflictsWithSource")

    /**
     * Verifies same-layer where cannot access current selected aliases.
     */
    @Test
    fun sameLayerWhereSelectedAlias() = diagnostics("sameLayerWhereSelectedAlias")

    /**
     * Verifies same-layer where cannot access current window aliases.
     */
    @Test
    fun sameLayerWhereWindowAlias() = diagnostics("sameLayerWhereWindowAlias")

    /**
     * Verifies window OVER is not available in where/groupBy/having-style scopes.
     */
    @Test
    fun windowFunctionInvalidClausePosition() = diagnostics("windowFunctionInvalidClausePosition")

    /**
     * Verifies same-layer having cannot access aggregate aliases.
     */
    @Test
    fun sameLayerHavingAggregateAlias() = diagnostics("sameLayerHavingAggregateAlias")
}
