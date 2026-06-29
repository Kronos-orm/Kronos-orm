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

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.ownerGenerator
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirFunctionCallRefinementExtension
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.builder.buildAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildTypeOperatorCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CallInfo
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import java.util.IdentityHashMap

// Refines select and query calls to generated projection types.
@OptIn(
    FirExtensionApiInternals::class,
    FirImplementationDetail::class,
    SymbolInternals::class,
    UnresolvedExpressionTypeAccess::class
)
class KronosProjectionCallRefinementExtension(
    session: FirSession
) : FirFunctionCallRefinementExtension(session) {
    private val projectionsByRefinedSymbol = IdentityHashMap<FirNamedFunctionSymbol, KronosProjectionModel>()
    private val projectionsByOriginalCall = IdentityHashMap<FirFunctionCall, KronosProjectionModel>()
    private val queryProjectionsByRefinedSymbol = IdentityHashMap<FirNamedFunctionSymbol, List<KronosProjectionModel>>()
    private val queryProjectionsByOriginalCall = IdentityHashMap<FirFunctionCall, List<KronosProjectionModel>>()
    private val ownedSymbols = mutableMapOf<FirRegularClassSymbol, org.jetbrains.kotlin.KtSourceElement>()

    /**
     * Rewrites select/query return types once the projection fields are known.
     */
    override fun intercept(
        callInfo: CallInfo,
        symbol: FirNamedFunctionSymbol
    ): CallReturnType? {
        if (isQueryCall(symbol, callInfo)) {
            val models = readQueryProjectionModels(callInfo)
            if (models.isEmpty()) return null
            val typeRef = buildResolvedTypeRef {
                source = callInfo.callSite.source
                coneType = refineQueryReturnType(symbol, models.first()) ?: return null
            }
            return CallReturnType(typeRef) { refinedSymbol ->
                queryProjectionsByRefinedSymbol[refinedSymbol] = models
            }
        }

        if (!isBareSelectCall(symbol, callInfo)) return null

        val model = buildProjectionModel(callInfo) ?: return null
        KronosProjectionRegistry.register(model)
        val typeRef = buildResolvedTypeRef {
            source = callInfo.callSite.source
            coneType = selectClauseType(model.sourceType, model.symbol)
        }

        return CallReturnType(typeRef) { refinedSymbol ->
            projectionsByRefinedSymbol[refinedSymbol] = model
        }
    }

    /**
     * Checks whether this call is a bare select lambda we can project.
     */
    private fun isBareSelectCall(symbol: FirNamedFunctionSymbol, callInfo: CallInfo): Boolean {
        val callableId = symbol.callableId
        if (callableId.packageName.asString() != "com.kotlinorm.orm.select") return false
        if (callableId.callableName.asString() != "select") return false
        if (callInfo.arguments.size != 1) return false
        return callInfo.arguments.lastOrNull() is FirAnonymousFunctionExpression
    }

    /**
     * Checks whether this is a query call on a generated projection receiver.
     */
    private fun isQueryCall(symbol: FirNamedFunctionSymbol, callInfo: CallInfo): Boolean {
        val callableId = symbol.callableId
        if (callableId.packageName.asString() != "com.kotlinorm.orm.select") return false
        if (callableId.callableName.asString() !in QUERY_NAMES) return false
        return callInfo.explicitReceiver?.coneTypeOrNull != null
    }

    /**
     * Reads the generated projection model back from the SelectClause receiver.
     */
    private fun readQueryProjectionModels(callInfo: CallInfo): List<KronosProjectionModel> {
        val receiverType = callInfo.explicitReceiver?.coneTypeOrNull as? ConeClassLikeType ?: return emptyList()
        if (receiverType.lookupTag.classId != SELECT_CLAUSE_CLASS_ID) return emptyList()
        val projectionArgument = receiverType.typeArguments.getOrNull(1) as? ConeKotlinTypeProjection ?: return emptyList()
        val projectionType = projectionArgument.type as? ConeClassLikeType ?: return emptyList()
        return listOfNotNull(KronosProjectionRegistry.find(projectionType.lookupTag.classId))
    }

    /**
     * Wraps the refined select call in a resolved `run` call that owns the generated local class.
     */
    @OptIn(FirSymbolProviderInternals::class)
    override fun transform(
        call: FirFunctionCall,
        originalSymbol: FirNamedFunctionSymbol
    ): FirFunctionCall {
        val refinedSymbol = (call.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol
        val queryModels = queryProjectionsByRefinedSymbol[refinedSymbol]
        if (queryModels != null) {
            queryProjectionsByOriginalCall[call] = queryModels
            queryModels.forEach { model -> ownedSymbols[model.symbol] = call.source ?: model.anchor }
            return call
        }

        val model = projectionsByRefinedSymbol[refinedSymbol] ?: return call
        projectionsByOriginalCall[call] = model
        ownedSymbols[model.symbol] = call.source ?: model.anchor
        return buildRunWithProjection(call, model) ?: call
    }

    /**
     * Creates the FIR shape expected by [FirFunctionCallRefinementExtension]:
     * `run { class Projection; selectCall as SelectClause<Source, Projection> }`.
     */
    @OptIn(FirSymbolProviderInternals::class)
    private fun buildRunWithProjection(call: FirFunctionCall, model: KronosProjectionModel): FirFunctionCall? {
        val runSymbol = session.symbolProvider
            .getTopLevelFunctionSymbols(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("run"))
            .firstOrNull { it.fir.valueParameters.size == 1 }
            ?: return null
        val typeRef = buildResolvedTypeRef {
            source = call.source
            coneType = call.coneTypeOrNull ?: return null
        }
        val projectionClass = KronosProjectionDeclarationGenerationExtension.buildProjectionClass(session, model.symbol, model).also {
            model.symbol.bind(it)
        }
        val castCall = buildTypeOperatorCall {
            source = call.source
            coneTypeOrNull = call.coneTypeOrNull
            argumentList = buildArgumentList {
                source = call.source
                arguments += call
            }
            operation = FirOperation.AS
            conversionTypeRef = typeRef
        }
        val lambda = buildAnonymousFunctionExpression {
            source = call.source
            isTrailingLambda = true
            anonymousFunction = buildAnonymousFunction {
                source = call.source
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Plugin(KronosProjectionGeneratedDeclarationKey)
                status = org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
                returnTypeRef = typeRef
                symbol = FirAnonymousFunctionSymbol()
                isLambda = true
                hasExplicitParameterList = false
                body = buildBlock {
                    source = call.source
                    coneTypeOrNull = call.coneTypeOrNull
                    statements += projectionClass
                    statements += castCall
                }
            }
        }
        return buildFunctionCall {
            source = call.source
            coneTypeOrNull = call.coneTypeOrNull
            argumentList = buildArgumentList {
                source = call.source
                arguments += lambda
            }
            calleeReference = buildResolvedNamedReference {
                source = call.calleeReference.source
                name = Name.identifier("run")
                resolvedSymbol = runSymbol
            }
        }
    }

    /**
     * Marks the generated projection class as owned by this extension.
     */
    override fun ownsSymbol(symbol: FirRegularClassSymbol): Boolean = symbol in ownedSymbols

    /**
     * Returns the anchor element used when FIR asks for the generated class origin.
     */
    override fun anchorElement(symbol: FirRegularClassSymbol) =
        ownedSymbols[symbol] ?: error("Kronos projection symbol is not owned by this extension: $symbol")

    /**
     * Restores the generated projection symbol after FIR serialization and deserialization steps.
     */
    override fun restoreSymbol(call: FirFunctionCall, name: Name): FirRegularClassSymbol? {
        queryProjectionsByOriginalCall[call]?.firstOrNull { it.name == name }?.let { return it.symbol }
        projectionsByOriginalCall[call]?.takeIf { it.name == name }?.let { return it.symbol }
        return ownedSymbols.keys.singleOrNull { it.fir.name == name }
    }

    /**
     * Reads projection fields from a bare property access, collection literal, or vararg call.
     */
    private fun readProjectionFields(expression: FirStatement, sourceType: ConeKotlinType): List<KronosProjectionField> {
        val property = expression as? FirPropertyAccessExpression
        if (property != null) return listOfNotNull(property.toProjectionField(sourceType))

        val literal = expression as? FirCollectionLiteral
        if (literal != null) {
            return literal.argumentList.arguments.mapNotNull { (it as? FirPropertyAccessExpression)?.toProjectionField(sourceType) }
        }

        val call = expression as? FirFunctionCall ?: return emptyList()
        val vararg = call.argumentList.arguments.singleOrNull() as? FirVarargArgumentsExpression ?: return emptyList()
        return vararg.arguments.mapNotNull { (it as? FirPropertyAccessExpression)?.toProjectionField(sourceType) }
    }

    /**
     * Converts one source property access into a generated projection field.
     */
    private fun FirPropertyAccessExpression.toProjectionField(sourceType: ConeKotlinType): KronosProjectionField? {
        val type = resolveSourcePropertyType(sourceType, calleeReference.name) ?: coneTypeOrNull ?: return null
        return KronosProjectionField(calleeReference.name, type)
    }

    /**
     * Resolves the original DTO property type from the source receiver.
     */
    private fun resolveSourcePropertyType(sourceType: ConeKotlinType, name: Name): ConeKotlinType? {
        val classSymbol = sourceType.toClassSymbol(session) ?: return null
        val scope = session.declaredMemberScope(classSymbol.fir, memberRequiredPhase = FirResolvePhase.STATUS)
        var result: ConeKotlinType? = null
        scope.processPropertiesByName(name) { propertySymbol: FirVariableSymbol<*> ->
            result = (propertySymbol.fir.returnTypeRef as? FirResolvedTypeRef)?.coneType
        }
        return result
    }

    private fun FirBlock.lastExpression(): FirStatement? = statements.lastOrNull()

    /**
     * Rewrites query return types to the generated projection class or list of it.
     */
    private fun refineQueryReturnType(symbol: FirNamedFunctionSymbol, model: KronosProjectionModel): ConeKotlinType? {
        return when (symbol.callableId.callableName.asString()) {
            "queryList" -> listType(model)
            "queryOne" -> projectionType(model, isNullable = false)
            "queryOneOrNull" -> projectionType(model, isNullable = true)
            else -> null
        }
    }

    /**
     * Extracts one select projection model from the lambda body.
     */
    private fun buildProjectionModel(callInfo: CallInfo): KronosProjectionModel? {
        val sourceType = callInfo.explicitReceiver?.coneTypeOrNull ?: return null
        val lambda = callInfo.arguments.lastOrNull() as? FirAnonymousFunctionExpression ?: return null
        val returned = lambda.anonymousFunction.body?.lastExpression() ?: return null
        val fields = readProjectionFields(returned, sourceType)
        if (fields.isEmpty()) return null

        val name = Name.identifier("KronosSelectResult_${projectionId(callInfo)}")
        val anchor = callInfo.callSite.source ?: lambda.source ?: return null
        val classId = ClassId.topLevel(PROJECTION_PACKAGE.child(name))
        val symbol = FirRegularClassSymbol(classId)
        val model = KronosProjectionModel(
            classId = classId,
            name = name,
            symbol = symbol,
            sourceType = sourceType,
            fields = fields,
            anchor = anchor,
        )
        return model
    }

    /**
     * Maps the refined projection class into SelectClause<User, Projection>.
     */
    private fun selectClauseType(sourceType: ConeKotlinType, projectionSymbol: FirRegularClassSymbol): ConeClassLikeTypeImpl {
        return ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(SELECT_CLAUSE_CLASS_ID),
            arrayOf(
                sourceType,
                ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(projectionSymbol.classId),
                    ConeTypeProjection.EMPTY_ARRAY,
                    false,
                    ConeAttributes.Empty
                )
            ),
            false,
            ConeAttributes.Empty
        )
    }

    /**
     * Builds the concrete projection return type for queryOne/queryOneOrNull.
     */
    private fun projectionType(model: KronosProjectionModel, isNullable: Boolean): ConeClassLikeTypeImpl {
        return ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(model.classId),
            ConeTypeProjection.EMPTY_ARRAY,
            isNullable,
            ConeAttributes.Empty
        )
    }

    /**
     * Wraps the projection type in List for queryList().
     */
    private fun listType(model: KronosProjectionModel): ConeClassLikeTypeImpl {
        return ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(StandardClassIds.List),
            arrayOf(projectionType(model, isNullable = false)),
            false,
            ConeAttributes.Empty
        )
    }

    /**
     * Generates a stable synthetic class suffix from the call site.
     */
    private fun projectionId(callInfo: CallInfo): String {
        val raw = listOf(
            callInfo.containingFile.name,
            callInfo.callSite.source?.toString(),
            callInfo.arguments.lastOrNull()?.source?.toString()
        ).joinToString("|")
        return raw.hashCode().toUInt().toString(16).padStart(8, '0').take(8)
    }

    private companion object {
        val PROJECTION_PACKAGE: FqName = FqName("com.kotlinorm.generated.projection")
        val SELECT_CLAUSE_CLASS_ID: ClassId = ClassId.topLevel(FqName("com.kotlinorm.orm.select.SelectClause"))
        val QUERY_NAMES: Set<String> = setOf("queryList", "queryOne", "queryOneOrNull")
    }
}
