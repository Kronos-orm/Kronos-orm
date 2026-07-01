@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

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

import com.kotlinorm.compiler.utils.GeneratedProjectionPackageFqName
import com.kotlinorm.compiler.utils.KPojoClassId
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.ownerGenerator
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameterKind
import org.jetbrains.kotlin.fir.declarations.builder.buildPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.references.builder.buildPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.ConstantValueKind
import java.util.concurrent.ConcurrentHashMap

/**
 * Generates FIR declarations for compiler-created select projection classes.
 */
@OptIn(FirImplementationDetail::class, ExperimentalTopLevelDeclarationsGenerationApi::class)
class KronosProjectionDeclarationGenerationExtension(
    session: FirSession
) : FirDeclarationGenerationExtension(session) {
    init {
        KronosProjectionRegistry.registerDeclarationGenerator(session, this)
    }

    /**
     * Projection classes are created on demand from the call-refinement registry.
     */
    override fun getTopLevelClassIds(): Set<ClassId> = KronosProjectionRegistry.allTopLevelClassIds(session)

    /**
     * Makes the generated projection package visible to top-level class lookup and backend output.
     */
    override fun hasPackage(packageFqName: FqName): Boolean =
        packageFqName == GeneratedProjectionPackageFqName || super.hasPackage(packageFqName)

    /**
     * Resolves a generated top-level projection class from registry state.
     */
    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        val model = KronosProjectionRegistry.find(session, classId) ?: return null
        val symbol = model.symbolFor(classId)
        if (symbol.isBound) return symbol
        val fir = buildProjectionClass(session, symbol, model, classId)
        fir.ownerGenerator = this
        symbol.bind(fir)
        return symbol
    }

    /**
     * Exposes generated projection field names to FIR member scope lookup.
     */
    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext
    ): Set<Name> {
        val model = KronosProjectionRegistry.find(session, classSymbol.classId) ?: return emptySet()
        return model.fieldsFor(classSymbol.classId).mapTo(mutableSetOf(SpecialNames.INIT)) { it.name }
    }

    /**
     * Generates projection field symbols when FIR asks for members of the synthetic class.
     */
    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        val classSymbol = context?.owner as? FirRegularClassSymbol ?: return emptyList()
        val model = KronosProjectionRegistry.find(session, classSymbol.classId) ?: return emptyList()
        return projectionMembers(session, classSymbol, model).properties
            .filter { it.name == callableId.callableName }
            .map { it.symbol }
    }

    /**
     * Exposes the primary constructor of the generated data class to FIR scopes.
     */
    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val classSymbol = context.owner as? FirRegularClassSymbol ?: return emptyList()
        val model = KronosProjectionRegistry.find(session, classSymbol.classId) ?: return emptyList()
        return listOf(projectionMembers(session, classSymbol, model).constructor.symbol)
    }

    companion object {
        /**
         * Binds the projection class early so refined call return types can reference a concrete FIR symbol.
         */
        fun ensureProjectionClassBound(
            session: FirSession,
            model: KronosProjectionModel
        ) {
            val symbol = model.symbol
            if (symbol.isBound) return
            val generator = KronosProjectionRegistry.declarationGenerator(session)
                ?: error("Kronos projection declaration generator is not registered for this FIR session")
            val fir = buildProjectionClass(session, symbol, model, model.classId)
            fir.ownerGenerator = generator
            symbol.bind(fir)
        }

        /**
         * Binds the generated Context class early so SelectClause's third type argument resolves in FIR.
         */
        fun ensureContextClassBound(
            session: FirSession,
            model: KronosProjectionModel
        ) {
            val symbol = model.contextSymbol
            if (symbol.isBound) return
            val generator = KronosProjectionRegistry.declarationGenerator(session)
                ?: error("Kronos projection declaration generator is not registered for this FIR session")
            val fir = buildProjectionClass(session, symbol, model, model.contextClassId)
            fir.ownerGenerator = generator
            symbol.bind(fir)
        }

        /**
         * Drops generated member cache when a projection model is refined before
         * member lookup, for example after a scalar subquery alias type becomes known.
         */
        fun invalidateMemberCache(model: KronosProjectionModel) {
            projectionMemberCache.remove(model.symbol)
            projectionMemberCache.remove(model.contextSymbol)
        }

        /**
         * Builds the synthetic projection class and attaches generated projection fields directly to it.
         */
        fun buildProjectionClass(
            session: FirSession,
            symbol: FirRegularClassSymbol,
            model: KronosProjectionModel,
            classId: ClassId
        ) = buildRegularClass {
            source = model.anchor
            resolvePhase = FirResolvePhase.STATUS
            moduleData = session.moduleData
            origin = projectionOrigin()
            scopeProvider = FirKotlinScopeProvider()
            status = projectionStatus().apply {
                isData = true
            }
            classKind = ClassKind.CLASS
            name = model.nameFor(classId)
            this.symbol = symbol
            superTypeRefs += buildResolvedTypeRef {
                coneType = ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(KPojoClassId),
                    ConeTypeProjection.EMPTY_ARRAY,
                    false,
                    ConeAttributes.Empty
                )
            }
        }

        /**
         * Returns generated constructor and property declarations for a projection class.
         */
        fun projectionMembers(
            session: FirSession,
            classSymbol: FirRegularClassSymbol,
            model: KronosProjectionModel
        ): KronosProjectionGeneratedMembers = projectionMemberCache.computeIfAbsent(classSymbol) {
            val primaryConstructor = buildProjectionPrimaryConstructor(session, classSymbol, model)
            val properties = model.fieldsFor(classSymbol.classId).map { field ->
                val parameter = primaryConstructor.valueParameters.single { it.name == field.name }
                buildProjectionProperty(session, classSymbol, field.name, field.type, model.anchor, parameter)
            }
            KronosProjectionGeneratedMembers(primaryConstructor, properties)
        }

        /**
         * Builds a primary constructor whose parameters all default to null, giving KPojo a no-arg path.
         */
        private fun buildProjectionPrimaryConstructor(
            session: FirSession,
            classSymbol: FirRegularClassSymbol,
            model: KronosProjectionModel
        ) = buildPrimaryConstructor {
            source = model.anchor
            resolvePhase = FirResolvePhase.STATUS
            moduleData = session.moduleData
            origin = projectionOrigin()
            status = projectionStatus()
            isLocal = false
            returnTypeRef = buildResolvedTypeRef {
                coneType = ConeClassLikeTypeImpl(
                    classSymbol.toLookupTag(),
                    ConeTypeProjection.EMPTY_ARRAY,
                    false,
                    ConeAttributes.Empty
                )
            }
            symbol = FirConstructorSymbol(classSymbol.classId)
            valueParameters += model.fieldsFor(classSymbol.classId).map { field ->
                buildProjectionConstructorParameter(session, symbol, field, model.anchor)
            }
        }.apply {
            containingClassForStaticMemberAttr = classSymbol.toLookupTag()
        }

        /**
         * Builds one constructor parameter with a null default value for the generated data class.
         */
        private fun buildProjectionConstructorParameter(
            session: FirSession,
            constructorSymbol: FirConstructorSymbol,
            field: KronosProjectionField,
            anchor: KtSourceElement,
        ) = buildValueParameter {
            source = field.source ?: anchor
            resolvePhase = FirResolvePhase.STATUS
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(KronosProjectionGeneratedDeclarationKey)
            returnTypeRef = buildResolvedTypeRef { coneType = field.type }
            this.name = field.name
            this.symbol = FirValueParameterSymbol()
            containingDeclarationSymbol = constructorSymbol
            valueParameterKind = FirValueParameterKind.Regular
            defaultValue = buildLiteralExpression(
                source = anchor,
                kind = ConstantValueKind.Null,
                value = null,
                setType = true
            ).apply {
                replaceConeTypeOrNull(session.builtinTypes.nullableNothingType.coneType)
            }
        }

        /**
         * Builds one public mutable field on the generated projection class.
         */
        fun buildProjectionProperty(
            session: FirSession,
            classSymbol: FirRegularClassSymbol,
            name: Name,
            type: ConeKotlinType,
            anchor: KtSourceElement,
            constructorParameter: FirValueParameter?,
        ) = buildProperty {
            source = anchor
            resolvePhase = FirResolvePhase.STATUS
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(KronosProjectionGeneratedDeclarationKey)
            this.isLocal = false
            status = projectionStatus()
            this.dispatchReceiverType = ConeClassLikeTypeImpl(
                classSymbol.toLookupTag(),
                ConeTypeProjection.EMPTY_ARRAY,
                false,
                ConeAttributes.Empty
            )
            this.name = name
            this.symbol = FirRegularPropertySymbol(CallableId(classSymbol.classId, name))
            returnTypeRef = buildResolvedTypeRef { coneType = type }
            initializer = constructorParameter?.let { parameter ->
                buildPropertyAccessExpression {
                    source = parameter.source ?: anchor
                    coneTypeOrNull = type
                    calleeReference = buildPropertyFromParameterResolvedNamedReference {
                        source = parameter.source ?: anchor
                        this.name = name
                        resolvedSymbol = parameter.symbol
                    }
                }
            } ?: constructorParameter?.defaultValue
            isVar = true
        }.apply {
            if (constructorParameter != null) {
                constructorParameter.correspondingProperty = this
                fromPrimaryConstructor = true
            }
        }

        /**
         * Marks projection declarations as compiler-plugin generated.
         */
        private fun projectionOrigin(): FirDeclarationOrigin =
            FirDeclarationOrigin.Plugin(KronosProjectionGeneratedDeclarationKey)

        /**
         * Creates the public final status shared by generated projection declarations.
         */
        private fun projectionStatus(): FirResolvedDeclarationStatusImpl {
            return FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
        }
    }
}

data class KronosProjectionGeneratedMembers(
    val constructor: FirConstructor,
    val properties: List<FirProperty>,
)

/**
 * Marks FIR declarations synthesized for generated select projection classes.
 */
object KronosProjectionGeneratedDeclarationKey : GeneratedDeclarationKey()

/**
 * Caches generated constructor/property FIR per projection class symbol within the compiler session.
 */
private val projectionMemberCache = ConcurrentHashMap<FirRegularClassSymbol, KronosProjectionGeneratedMembers>()

private fun KronosProjectionModel.fieldsFor(classId: ClassId): List<KronosProjectionField> =
    when (classId) {
        this.classId -> fields
        contextClassId -> contextFields
        else -> emptyList()
    }

private fun KronosProjectionModel.nameFor(classId: ClassId): Name =
    when (classId) {
        this.classId -> name
        contextClassId -> contextName
        else -> classId.shortClassName
    }

private fun KronosProjectionModel.symbolFor(classId: ClassId): FirRegularClassSymbol =
    when (classId) {
        this.classId -> symbol
        contextClassId -> contextSymbol
        else -> error("Unknown Kronos projection class id: $classId")
    }
