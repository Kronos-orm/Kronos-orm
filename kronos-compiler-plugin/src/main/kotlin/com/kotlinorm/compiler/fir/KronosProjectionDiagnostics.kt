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

package com.kotlinorm.compiler.fir

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

/**
 * FIR diagnostics emitted for projection DSL misuse that Kotlin's normal type system cannot express.
 */
object KronosProjectionDiagnostics : KtDiagnosticsContainer() {
    val SELECT_ITEM_REQUIRES_ALIAS: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_SELECT_ITEM_REQUIRES_ALIAS",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    val DUPLICATE_PROJECTION_FIELD: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_DUPLICATE_PROJECTION_FIELD",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    val SELECTED_FIELD_CONFLICTS_WITH_SOURCE: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_SELECTED_FIELD_CONFLICTS_WITH_SOURCE",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    val SCALAR_SUBQUERY_REQUIRES_LIMIT: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_SCALAR_SUBQUERY_REQUIRES_LIMIT",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    val SCALAR_SUBQUERY_REQUIRES_SINGLE_COLUMN: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_SCALAR_SUBQUERY_REQUIRES_SINGLE_COLUMN",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    val PREDICATE_SUBQUERY_COLUMN_COUNT_MISMATCH: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_PREDICATE_SUBQUERY_COLUMN_COUNT_MISMATCH",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    val ROW_VALUE_TUPLE_REQUIRES_MULTIPLE_FIELDS: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_ROW_VALUE_TUPLE_REQUIRES_MULTIPLE_FIELDS",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    val INSERT_SELECT_VALUE_COUNT_MISMATCH: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_INSERT_SELECT_VALUE_COUNT_MISMATCH",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    val INSERT_SELECT_VALUE_TYPE_MISMATCH: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_INSERT_SELECT_VALUE_TYPE_MISMATCH",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KronosProjectionDiagnosticMessages
}

/**
 * User-facing messages for Kronos projection diagnostics.
 */
object KronosProjectionDiagnosticMessages : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("KronosProjection") { map ->
        map.put(
            KronosProjectionDiagnostics.SELECT_ITEM_REQUIRES_ALIAS,
            "Non-field select item must declare .alias(\"name\")"
        )
        map.put(
            KronosProjectionDiagnostics.DUPLICATE_PROJECTION_FIELD,
            "Duplicate selected field name"
        )
        map.put(
            KronosProjectionDiagnostics.SELECTED_FIELD_CONFLICTS_WITH_SOURCE,
            "Selected alias conflicts with a source field name"
        )
        map.put(
            KronosProjectionDiagnostics.SCALAR_SUBQUERY_REQUIRES_LIMIT,
            "Scalar subquery must explicitly use limit(1)"
        )
        map.put(
            KronosProjectionDiagnostics.SCALAR_SUBQUERY_REQUIRES_SINGLE_COLUMN,
            "Scalar subquery must select exactly one column or expression"
        )
        map.put(
            KronosProjectionDiagnostics.PREDICATE_SUBQUERY_COLUMN_COUNT_MISMATCH,
            "Predicate subquery column count does not match the left expression"
        )
        map.put(
            KronosProjectionDiagnostics.ROW_VALUE_TUPLE_REQUIRES_MULTIPLE_FIELDS,
            "Row-value tuple IN requires at least two fields; use field in query for a single column"
        )
        map.put(
            KronosProjectionDiagnostics.INSERT_SELECT_VALUE_COUNT_MISMATCH,
            "Insert-select value count must match target insertable field count"
        )
        map.put(
            KronosProjectionDiagnostics.INSERT_SELECT_VALUE_TYPE_MISMATCH,
            "Insert-select value type must match the target insertable field type"
        )
    }
}
