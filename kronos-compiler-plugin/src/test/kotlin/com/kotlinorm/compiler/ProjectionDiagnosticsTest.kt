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
     * Verifies projection diagnostics still work through the IDEA-active FIR bridge path.
     */
    @Test
    fun duplicateProjectionPropertyIdeActive() = withIdeProjectionMode {
        diagnostics("duplicateProjectionPropertyIdeActive")
    }

    /**
     * Verifies non-field select items must declare an alias.
     */
    @Test
    fun functionFieldRequiresAlias() = diagnostics("functionFieldRequiresAlias")

    /**
     * Verifies aggregate select items must declare an alias.
     */
    @Test
    fun aggregateFieldRequiresAlias() = diagnostics("aggregateFieldRequiresAlias")

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
     * Verifies insert-select explicit values match target insertable field count.
     */
    @Test
    fun insertSelectValueCount() = diagnostics("insertSelectValueCount")

    /**
     * Verifies insert-select explicit values match target insertable field types.
     */
    @Test
    fun insertSelectValueType() = diagnostics("insertSelectValueType")

    /**
     * Verifies insert-select diagnostics inspect arrayOf explicit value factories.
     */
    @Test
    fun insertSelectArrayFactoryValues() = diagnostics("insertSelectArrayFactoryValues")

    /**
     * Verifies projection diagnostics inspect listOf and arrayOf projection forms.
     */
    @Test
    fun collectionProjectionDiagnostics() = diagnostics("collectionProjectionDiagnostics")

    /**
     * Verifies insert-select target field filtering excludes non-insertable properties.
     */
    @Test
    fun insertSelectTargetFiltering() = diagnostics("insertSelectTargetFiltering")

    /**
     * Verifies insert-select target filtering handles serialized primary-key combinations.
     */
    @Test
    fun insertSelectSerializePrimaryKeyFiltering() = diagnostics("insertSelectSerializePrimaryKeyFiltering")

    /**
     * Verifies non-identity primary keys stay insertable while identity primary keys are filtered.
     */
    @Test
    fun insertSelectPrimaryKeyFiltering() = diagnostics("insertSelectPrimaryKeyFiltering")

    /**
     * Verifies projection diagnostics inspect block, list, and array projection forms.
     */
    @Test
    fun projectionBlockAndVarargForms() = diagnostics("projectionBlockAndVarargForms")

    /**
     * Verifies invalid projection boundary forms remain rejected.
     */
    @Test
    fun projectionNegativeBoundaryForms() = diagnostics("projectionNegativeBoundaryForms")

    /**
     * Verifies projection diagnostics inspect mutableListOf and setOf projection forms.
     */
    @Test
    fun projectionMutableSetForms() = diagnostics("projectionMutableSetForms")

    /**
     * Verifies valid projection checker boundary forms do not report diagnostics.
     */
    @Test
    fun projectionCheckerPositiveMatrix() = diagnostics("projectionCheckerPositiveMatrix")

    /**
     * Verifies scalar subquery diagnostics across sort candidate forms.
     */
    @Test
    fun scalarSubqueryCandidateMatrix() = diagnostics("scalarSubqueryCandidateMatrix")

    /**
     * Verifies scalar subquery limit diagnostics require limit(1), not just any limit call.
     */
    @Test
    fun scalarSubqueryLimitMustBeOne() = diagnostics("scalarSubqueryLimitMustBeOne")

    /**
     * Verifies predicate subquery tuple diagnostics inspect listOf tuple forms.
     */
    @Test
    fun predicateSubqueryTupleFactoryForms() = diagnostics("predicateSubqueryTupleFactoryForms")

    /**
     * Verifies repeated Selected names require the standard Kotlin opt-in marker.
     */
    @Test
    fun duplicateProjectionProperty() = diagnostics("duplicateProjectionProperty")

    /**
     * Verifies join projection fields use the same duplicate-name opt-in diagnostic.
     */
    @Test
    fun joinDuplicateProjection() = diagnostics("joinDuplicateProjection")

    /**
     * Verifies duplicate projection opt-in behavior across nested, derived, and union query layers.
     */
    @Test
    fun projectionQueryLayerOptIn() = diagnostics("projectionQueryLayerOptIn")

    /**
     * Verifies a source-name shadow reports only when same-layer Context reads it.
     */
    @Test
    fun selectedAliasConflictsWithSource() = diagnostics("selectedAliasConflictsWithSource")

    /**
     * Verifies standard Kotlin opt-in scopes and source-minus restoration.
     */
    @Test
    fun projectionAliasOptInScopes() = diagnostics("projectionAliasOptInScopes")

    /**
     * Verifies file-level standard Kotlin opt-in for projection alias replacement.
     */
    @Test
    fun projectionAliasFileOptIn() = diagnostics("projectionAliasFileOptIn")

    /**
     * Verifies compiler-wide opt-in accepts duplicate Selected names and shadowed Context reads.
     */
    @Test
    fun projectionCompilerWideOptIn() = diagnostics("projectionCompilerWideOptIn")

    /**
     * Verifies offset-page Selected shapes participate in scalar and predicate diagnostics.
     */
    @Test
    fun projectionOffsetPageSubqueryDiagnostics() = diagnostics("projectionOffsetPageSubqueryDiagnostics")

    /**
     * Verifies same-layer where cannot access current selected aliases.
     */
    @Test
    fun sameLayerWhereSelectedAlias() = diagnostics("sameLayerWhereSelectedAlias")

    /**
     * Verifies filter Selected receivers exclude source fields omitted by the projection.
     */
    @Test
    fun filterExcludesUnselectedSource() = diagnostics("filterExcludesUnselectedSource")

    /**
     * Verifies the public filter contract rejects a null predicate.
     */
    @Test
    fun filterRejectsNullPredicate() = diagnostics("filterRejectsNullPredicate")

    /**
     * Verifies same-layer where cannot access current window aliases.
     */
    @Test
    fun sameLayerWhereWindowAlias() = diagnostics("sameLayerWhereWindowAlias")

    /**
     * Verifies same-layer where cannot access current scalar subquery aliases.
     */
    @Test
    fun sameLayerWhereScalarAlias() = diagnostics("sameLayerWhereScalarAlias")

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

    private fun withIdeProjectionMode(block: () -> Unit) {
        val key = "com.kotlinorm.kronos.ide.active"
        val oldValue = System.getProperty(key)
        try {
            System.setProperty(key, "true")
            block()
        } finally {
            if (oldValue == null) System.clearProperty(key) else System.setProperty(key, oldValue)
        }
    }
}
