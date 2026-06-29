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

package com.kotlinorm.compiler.plugin.fir

import com.kotlinorm.compiler.utils.KPojoClassId
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.ownerGenerator
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameterKind
import org.jetbrains.kotlin.fir.declarations.builder.buildPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
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
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.ConstantValueKind

// Generates synthetic projection classes and their projection fields.
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
    override fun getTopLevelClassIds(): Set<ClassId> = KronosProjectionRegistry.allClassIds()

    /**
     * Makes the generated projection package visible to top-level class lookup and backend output.
     */
    override fun hasPackage(fqName: FqName): Boolean =
        fqName == PROJECTION_PACKAGE || super.hasPackage(fqName)

    /**
     * Resolves a generated top-level projection class from registry state.
     */
    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        val model = KronosProjectionRegistry.find(classId) ?: return null
        val symbol = FirRegularClassSymbol(classId)
        val fir = buildProjectionClass(session, symbol, model)
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
        val model = KronosProjectionRegistry.find(classSymbol.classId) ?: return emptySet()
        return model.fields.mapTo(mutableSetOf()) { it.name }
    }

    /**
     * Generates projection field symbols when FIR asks for members of the synthetic class.
     */
    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        val classId = callableId.classId ?: return emptyList()
        val model = KronosProjectionRegistry.find(classId) ?: return emptyList()
        val symbol = model.symbol
        return model.fields
            .filter { it.name == callableId.callableName }
            .map { field ->
                buildProjectionProperty(
                    session,
                    symbol,
                    field.name,
                    field.type,
                    model.anchor,
                    constructorParameter = null
                ).symbol
            }
    }

    companion object {
        /**
         * Builds the synthetic projection class and attaches generated projection fields directly to it.
         */
        fun buildProjectionClass(
            session: FirSession,
            symbol: FirRegularClassSymbol,
            model: KronosProjectionModel
        ) = buildRegularClass {
            source = model.anchor
            resolvePhase = FirResolvePhase.STATUS
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(KronosProjectionGeneratedDeclarationKey)
            scopeProvider = FirKotlinScopeProvider()
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            ).apply {
                isData = true
            }
            classKind = org.jetbrains.kotlin.descriptors.ClassKind.CLASS
            name = model.name
            this.symbol = symbol
            val primaryConstructor = buildProjectionPrimaryConstructor(session, symbol, model)
            superTypeRefs += buildResolvedTypeRef {
                coneType = ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(KPojoClassId),
                    ConeTypeProjection.EMPTY_ARRAY,
                    false,
                    ConeAttributes.Empty
                )
            }
            declarations += primaryConstructor
            declarations += model.fields.map { field ->
                val parameter = primaryConstructor.valueParameters.single { it.name == field.name }
                buildProjectionProperty(session, symbol, field.name, field.type, model.anchor, parameter)
            }
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
            origin = FirDeclarationOrigin.Plugin(KronosProjectionGeneratedDeclarationKey)
            status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public)
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
            valueParameters += model.fields.map { field ->
                buildProjectionConstructorParameter(session, symbol, field.name, field.type, model.anchor)
            }
        }

        /**
         * Builds one constructor parameter with a null default value for the generated data class.
         */
        private fun buildProjectionConstructorParameter(
            session: FirSession,
            constructorSymbol: FirConstructorSymbol,
            name: Name,
            type: org.jetbrains.kotlin.fir.types.ConeKotlinType,
            anchor: org.jetbrains.kotlin.KtSourceElement
        ) = buildValueParameter {
            source = anchor
            resolvePhase = FirResolvePhase.STATUS
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(KronosProjectionGeneratedDeclarationKey)
            returnTypeRef = buildResolvedTypeRef { coneType = type }
            this.name = name
            this.symbol = FirValueParameterSymbol()
            containingDeclarationSymbol = constructorSymbol
            valueParameterKind = FirValueParameterKind.Regular
            defaultValue = buildLiteralExpression(
                source = anchor,
                kind = ConstantValueKind.Null,
                value = null,
                setType = true
            )
        }

        /**
         * Builds one public mutable field on the generated projection class.
         */
        fun buildProjectionProperty(
            session: FirSession,
            classSymbol: FirRegularClassSymbol,
            name: Name,
            type: org.jetbrains.kotlin.fir.types.ConeKotlinType,
            anchor: org.jetbrains.kotlin.KtSourceElement,
            constructorParameter: org.jetbrains.kotlin.fir.declarations.FirValueParameter?
        ) = buildProperty {
            source = anchor
            resolvePhase = FirResolvePhase.STATUS
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(KronosProjectionGeneratedDeclarationKey)
            isLocal = false
            val propertyStatus =
                FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public)
            status = propertyStatus
            this.dispatchReceiverType = ConeClassLikeTypeImpl(
                classSymbol.toLookupTag(),
                ConeTypeProjection.EMPTY_ARRAY,
                false,
                ConeAttributes.Empty
            )
            this.name = name
            this.symbol = FirRegularPropertySymbol(CallableId(classSymbol.classId, name))
            returnTypeRef = buildResolvedTypeRef { coneType = type }
            initializer = constructorParameter?.defaultValue
            isVar = true
        }
    }
}

object KronosProjectionGeneratedDeclarationKey : org.jetbrains.kotlin.GeneratedDeclarationKey()

private val PROJECTION_PACKAGE: FqName = FqName("com.kotlinorm.generated.projection")
