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

import com.kotlinorm.compiler.utils.ErrorMessages
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

/**
 * FIR diagnostics for unsupported KPojo declarations.
 */
object KronosKPojoDiagnostics : KtDiagnosticsContainer() {
    val GENERIC_KPOJO_NOT_SUPPORTED: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_GENERIC_KPOJO_NOT_SUPPORTED",
        Severity.ERROR,
        SourceElementPositioningStrategies.DECLARATION_NAME,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    val INACCESSIBLE_ENUM_METADATA: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_INACCESSIBLE_ENUM_METADATA",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    val INACCESSIBLE_KPOJO_FACTORY: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        "KRONOS_INACCESSIBLE_KPOJO_FACTORY",
        Severity.ERROR,
        SourceElementPositioningStrategies.DECLARATION_NAME,
        kronosDiagnosticPsiElementClass(),
        getRendererFactory()
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KronosKPojoDiagnosticMessages
}

/**
 * User-facing messages for KPojo declaration diagnostics.
 */
object KronosKPojoDiagnosticMessages : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("KronosKPojo") { map ->
        map.put(
            KronosKPojoDiagnostics.GENERIC_KPOJO_NOT_SUPPORTED,
            "Generic KPojo declarations are not supported; use a non-generic KPojo with concrete property types"
        )
        map.put(
            KronosKPojoDiagnostics.INACCESSIBLE_ENUM_METADATA,
            "Kronos cannot generate required enum metadata for a private or otherwise inaccessible enum; " +
                "make the enum and its containing declarations public or internal"
        )
        map.put(
            KronosKPojoDiagnostics.INACCESSIBLE_KPOJO_FACTORY,
            ErrorMessages.INACCESSIBLE_KPOJO_FACTORY
        )
    }
}
