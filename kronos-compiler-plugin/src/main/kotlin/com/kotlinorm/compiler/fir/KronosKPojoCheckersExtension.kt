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

import com.kotlinorm.compiler.utils.KPojoClassId
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * Registers declaration diagnostics for unsupported KPojo shapes.
 */
class KronosKPojoCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val regularClassCheckers: Set<FirRegularClassChecker> = setOf(KronosGenericKPojoChecker)
    }
}

private object KronosGenericKPojoChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (declaration.typeParameters.isEmpty() || !declaration.isKPojoClass(context.session)) return
        reporter.reportOn(declaration.source, KronosKPojoDiagnostics.GENERIC_KPOJO_NOT_SUPPORTED)
    }
}

@OptIn(SymbolInternals::class)
private fun FirClass.isKPojoClass(session: FirSession): Boolean {
    val pending = ArrayDeque<FirClass>()
    val visited = mutableSetOf<ClassId>()
    pending.add(this)

    while (pending.isNotEmpty()) {
        val current = pending.removeFirst()
        current.superTypeRefs
            .mapNotNull { ref -> (ref as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType }
            .forEach { type ->
                val classId = type.lookupTag.classId
                if (classId == KPojoClassId) return true
                if (visited.add(classId)) {
                    type.toClassSymbol(session)?.fir?.let(pending::addLast)
                }
            }
    }

    return false
}
