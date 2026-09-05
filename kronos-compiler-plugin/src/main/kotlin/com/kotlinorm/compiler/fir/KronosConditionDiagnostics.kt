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
 * FIR diagnostics for condition expressions that reference unavailable SQL sources.
 */
object KronosConditionDiagnostics : KtDiagnosticsContainer() {
    val UNREGISTERED_CONDITION_SOURCE: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_UNREGISTERED_CONDITION_SOURCE",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KronosConditionDiagnosticMessages
}

/**
 * User-facing messages for Kronos condition diagnostics.
 */
object KronosConditionDiagnosticMessages : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("KronosCondition") { map ->
        map.put(
            KronosConditionDiagnostics.UNREGISTERED_CONDITION_SOURCE,
            "KPojo property is not a registered SQL source in this condition; use .value for its Kotlin value"
        )
    }
}
