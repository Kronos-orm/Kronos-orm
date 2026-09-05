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

import com.kotlinorm.compiler.utils.CascadeAnnotationClassId
import com.kotlinorm.compiler.utils.IgnoreAnnotationClassId
import com.kotlinorm.compiler.utils.KPojoClassId
import com.kotlinorm.compiler.utils.SerializeAnnotationClassId
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.extractEnumValueArgumentInfo
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.unwrapVarargValue
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * Registers declaration diagnostics for unsupported KPojo shapes.
 */
class KronosKPojoCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val regularClassCheckers: Set<FirRegularClassChecker> = setOf(
            KronosGenericKPojoChecker,
            KronosInaccessibleKPojoFactoryChecker,
        )
        override val propertyCheckers: Set<FirPropertyChecker> = setOf(KronosScalarEnumMetadataChecker)
    }

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(KronosTypedResultEnumMetadataChecker)
    }
}

private object KronosGenericKPojoChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (declaration.typeParameters.isEmpty() || !declaration.isKPojoClass(context.session)) return
        reporter.reportOn(declaration.source, KronosKPojoDiagnostics.GENERIC_KPOJO_NOT_SUPPORTED)
    }
}

/**
 * Rejects KPojo declarations whose otherwise eligible generated factory would
 * be silently omitted because the module-level provider cannot reference them.
 */
private object KronosInaccessibleKPojoFactoryChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (!declaration.requiresGeneratedKPojoFactory(context.session)) return
        if (declaration.isAccessibleFromGeneratedProvider(context.session)) return
        reporter.reportOn(declaration.source, KronosKPojoDiagnostics.INACCESSIBLE_KPOJO_FACTORY)
    }
}

@OptIn(SymbolInternals::class)
private object KronosScalarEnumMetadataChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (!declaration.requiresScalarEnumMetadata(context.session)) return
        val owner = declaration.dispatchReceiverType
            ?.toClassSymbol(context.session)
            ?.fir as? FirRegularClass
            ?: return
        if (!owner.isGeneratedProviderKPojoCandidate(context.session)) return
        val enumClass = ((declaration.returnTypeRef as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType)
            ?.toClassSymbol(context.session)
            ?.fir as? FirRegularClass
            ?: return
        if (
            enumClass.classKind != ClassKind.ENUM_CLASS ||
            enumClass.isAccessibleFromGeneratedProvider(context.session)
        ) {
            return
        }
        reporter.reportOn(declaration.source, KronosKPojoDiagnostics.INACCESSIBLE_ENUM_METADATA)
    }
}

private object KronosTypedResultEnumMetadataChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private val typedResultFunctionNames = setOf(
        "queryList",
        "queryOne",
        "queryOneOrNull",
        "toList",
        "first",
        "firstOrNull",
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (!expression.isKronosTypedResultCall()) return
        val resultType = runCatching { expression.resolvedType }.getOrNull() ?: return
        val enumClasses = linkedSetOf<FirRegularClass>()
        resultType.collectConcreteEnumLeaves(context.session, enumClasses)
        val hasInaccessibleEnum = enumClasses.any { enumClass ->
            !enumClass.isAccessibleFromGeneratedProvider(context.session)
        }
        if (!hasInaccessibleEnum) return
        reporter.reportOn(expression.source, KronosKPojoDiagnostics.INACCESSIBLE_ENUM_METADATA)
    }

    private fun FirFunctionCall.isKronosTypedResultCall(): Boolean {
        val callableId = ((calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol)
            ?.callableId
            ?: return false
        return callableId.callableName.asString() in typedResultFunctionNames &&
            callableId.packageName.asString().startsWith("com.kotlinorm.")
    }
}

private val IgnoreTargetsName = Name.identifier("targets")
private val IgnoreAllName = Name.identifier("ALL")

private fun FirProperty.requiresScalarEnumMetadata(session: FirSession): Boolean =
    !hasAnnotation(SerializeAnnotationClassId, session) &&
        !hasAnnotation(CascadeAnnotationClassId, session) &&
        !isIgnoredForAll(session)

private fun FirProperty.isIgnoredForAll(session: FirSession): Boolean {
    val ignore = getAnnotationByClassId(IgnoreAnnotationClassId, session) ?: return false
    val targets = ignore.findArgumentByName(IgnoreTargetsName) ?: return true
    return targets.unwrapVarargValue().any { target ->
        target.extractEnumValueArgumentInfo()?.enumEntryName == IgnoreAllName
    }
}

private fun FirRegularClass.isGeneratedProviderKPojoCandidate(session: FirSession): Boolean =
    classKind == ClassKind.CLASS &&
        status.modality != Modality.ABSTRACT &&
        status.modality != Modality.SEALED &&
        !status.isInner &&
        isKPojoClass(session) &&
        isAccessibleFromGeneratedProvider(session, allowLocalClass = true)

private fun FirRegularClass.requiresGeneratedKPojoFactory(session: FirSession): Boolean =
    classKind == ClassKind.CLASS &&
        status.modality != Modality.ABSTRACT &&
        status.modality != Modality.SEALED &&
        !status.isInner &&
        !isLocal &&
        typeParameters.isEmpty() &&
        isKPojoClass(session) &&
        hasGeneratedProviderConstructor(session)

@OptIn(SymbolInternals::class)
private fun FirRegularClass.hasGeneratedProviderConstructor(session: FirSession): Boolean {
    var found = false
    symbol.processAllDeclarations(session, FirResolvePhase.STATUS) { declarationSymbol ->
        val constructor = (declarationSymbol as? FirConstructorSymbol)?.fir
            ?: return@processAllDeclarations
        if (
            constructor.status.visibility.isAccessibleFromGeneratedProvider() &&
            constructor.valueParameters.all { parameter -> parameter.defaultValue != null }
        ) {
            found = true
        }
    }
    return found
}

private fun Visibility.isAccessibleFromGeneratedProvider(): Boolean =
    this == Visibilities.Public || this == Visibilities.Internal

@OptIn(SymbolInternals::class)
private fun FirRegularClass.isAccessibleFromGeneratedProvider(
    session: FirSession,
    allowLocalClass: Boolean = false,
): Boolean {
    if (isLocal) return allowLocalClass
    var current: FirRegularClass? = this
    while (current != null) {
        if (!current.status.visibility.isAccessibleFromGeneratedProvider()) {
            return false
        }
        val outerClassId = current.symbol.classId.outerClassId ?: return true
        current = session.symbolProvider.getClassLikeSymbolByClassId(outerClassId)?.fir as? FirRegularClass
            ?: return false
    }
    return true
}

@OptIn(SymbolInternals::class)
private fun ConeKotlinType.collectConcreteEnumLeaves(
    session: FirSession,
    target: MutableSet<FirRegularClass>,
) {
    val classLikeType = this as? ConeClassLikeType ?: return
    (classLikeType.toClassSymbol(session)?.fir as? FirRegularClass)
        ?.takeIf { it.classKind == ClassKind.ENUM_CLASS }
        ?.let(target::add)
    classLikeType.typeArguments
        .filterIsInstance<ConeKotlinTypeProjection>()
        .forEach { projection -> projection.type.collectConcreteEnumLeaves(session, target) }
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
