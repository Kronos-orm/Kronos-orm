@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "TooManyFunctions")

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

import com.kotlinorm.compiler.utils.GeneratedProjectionClassPrefix
import com.kotlinorm.compiler.utils.GeneratedContextClassPrefix
import com.kotlinorm.compiler.utils.GeneratedProjectionFieldIdentifierRegex
import com.kotlinorm.compiler.utils.GeneratedProjectionPackageFqName
import com.kotlinorm.compiler.utils.JoinedSelectQueryClassId
import com.kotlinorm.compiler.utils.KSelectableClassId
import com.kotlinorm.compiler.utils.FirstFunctionName
import com.kotlinorm.compiler.utils.FirstOrNullFunctionName
import com.kotlinorm.compiler.utils.OffsetPageQueryClassId
import com.kotlinorm.compiler.utils.SelectAliasFunctionName
import com.kotlinorm.compiler.utils.SelectClauseClassId
import com.kotlinorm.compiler.utils.SelectFunctionName
import com.kotlinorm.compiler.utils.SelectPackageFqName
import com.kotlinorm.compiler.utils.SelectQueryFunctionNames
import com.kotlinorm.compiler.utils.ToListFunctionName
import com.kotlinorm.compiler.utils.UnionClauseClassId
import com.kotlinorm.compiler.utils.isJoinSourceClassId
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirFunctionCallRefinementExtension
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CallInfo
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import java.util.IdentityHashMap

/**
 * Refines bare select and query calls to compiler-generated projection result types.
 */
@OptIn(
    FirExtensionApiInternals::class,
    UnresolvedExpressionTypeAccess::class,
    SymbolInternals::class
)
class KronosProjectionCallRefinementExtension(
    session: FirSession
) : FirFunctionCallRefinementExtension(session) {
    private val aliasedExpressionTypes = mutableListOf<AliasedExpressionType>()
    private val aliasScopeIds = IdentityHashMap<Any, Long>()
    private var nextAliasScopeId = 1L
    private val selectedTypeArgumentIndexByClassId = mapOf(
        SelectClauseClassId to 1,
        JoinedSelectQueryClassId to 1,
        KSelectableClassId to 0,
        UnionClauseClassId to 0,
        OffsetPageQueryClassId to 0
    )
    private val queryReturnTypeByName = mapOf<String, (KronosProjectionModel) -> ConeKotlinType?>(
        ToListFunctionName to { model -> listType(model) },
        FirstFunctionName to { model -> projectionType(model, isNullable = false) },
        FirstOrNullFunctionName to { model -> projectionType(model, isNullable = true) }
    )

    /**
     * Rewrites select/query return types once the projection fields are known.
     */
    override fun intercept(
        callInfo: CallInfo,
        symbol: FirNamedFunctionSymbol
    ): CallReturnType? {
        recordAliasedExpressionType(callInfo, symbol)

        if (isQueryCall(symbol, callInfo)) {
            val models = readQueryProjectionModels(callInfo)
            if (models.isEmpty()) return null
            val typeRef = buildResolvedTypeRef {
                source = callInfo.callSite.source
                coneType = refineQueryReturnType(symbol, models.first()) ?: return null
            }
            return CallReturnType(typeRef)
        }

        if (!isBareSelectCall(symbol, callInfo)) return null

        val model = buildProjectionModel(callInfo, isJoinSelectCall(callInfo, symbol)) ?: return null
        KronosProjectionRegistry.register(session, model)
        KronosProjectionDeclarationGenerationExtension.ensureProjectionClassBound(session, model)
        KronosProjectionDeclarationGenerationExtension.ensureContextClassBound(session, model)
        val typeRef = buildResolvedTypeRef {
            source = callInfo.callSite.source
            coneType = selectReturnType(callInfo, symbol, model.sourceType, model)
        }

        return CallReturnType(typeRef)
    }

    /**
     * Checks whether this call is a bare select lambda we can project.
     */
    private fun isBareSelectCall(symbol: FirNamedFunctionSymbol, callInfo: CallInfo): Boolean {
        val callableId = symbol.callableId
        if (callableId.callableName != SelectFunctionName) return false
        if (callInfo.arguments.size != 1) return false
        if (callInfo.arguments.lastOrNull() !is FirAnonymousFunctionExpression) return false
        return callableId.packageName == SelectPackageFqName || callableId.classId?.isJoinSourceClassId() == true
    }

    private fun isJoinSelectCall(callInfo: CallInfo, symbol: FirNamedFunctionSymbol): Boolean {
        val receiverType = callInfo.explicitReceiver?.coneTypeOrNull as? ConeClassLikeType
        return receiverType?.lookupTag?.classId?.isJoinSourceClassId() == true ||
            symbol.callableId.classId?.isJoinSourceClassId() == true
    }

    /**
     * Checks whether this is a query call on a generated projection receiver.
     */
    private fun isQueryCall(symbol: FirNamedFunctionSymbol, callInfo: CallInfo): Boolean {
        val callableId = symbol.callableId
        if (callableId.callableName.asString() !in SelectQueryFunctionNames) return false
        if (callableId.packageName != SelectPackageFqName && callableId.classId != JoinedSelectQueryClassId) {
            return false
        }
        if (callInfo.typeArguments.isNotEmpty()) return false
        return callInfo.explicitReceiver?.coneTypeOrNull != null
    }

    /**
     * Reads the generated projection model back from the SelectClause receiver.
     */
    private fun readQueryProjectionModels(callInfo: CallInfo): List<KronosProjectionModel> {
        val receiverType = callInfo.explicitReceiver?.coneTypeOrNull as? ConeClassLikeType ?: return emptyList()
        val projectionType = receiverType.selectedTypeArgumentOrNull(selectFromIndex = 1) as? ConeClassLikeType
            ?: return emptyList()
        return listOfNotNull(KronosProjectionRegistry.find(session, projectionType.lookupTag.classId))
    }

    /**
     * Keeps the original FIR call body. This extension only refines return types.
     */
    override fun transform(
        call: FirFunctionCall,
        originalSymbol: FirNamedFunctionSymbol
    ): FirFunctionCall {
        return call
    }

    override fun ownsSymbol(symbol: FirRegularClassSymbol): Boolean = false

    /**
     * This extension does not synthesize local classes; projection classes are top-level generated declarations.
     */
    override fun anchorElement(symbol: FirRegularClassSymbol) =
        error("Kronos projection call refinement no longer owns local symbols: $symbol")

    /**
     * Local symbol restoration is unused because projection classes are generated through FIR declaration generation.
     */
    override fun restoreSymbol(call: FirFunctionCall, name: Name): FirRegularClassSymbol? {
        return null
    }

    /**
     * Reads projection fields from a bare property access, collection literal, or vararg call.
     */
    private fun readProjectionFields(
        expression: FirStatement,
        sourceType: ConeKotlinType,
        sourceValues: ProjectionSourceValueAccessors,
        receiverBindings: ProjectionReceiverBindings,
        aliasScopeId: Long,
    ): List<KronosProjectionField> {
        val statement = expression.projectionStatementOrNull() ?: return emptyList()
        return statement.projectionItems().flatMap {
            it.toProjectionFields(sourceType, sourceValues, receiverBindings, aliasScopeId)
        }
    }

    /**
     * Converts one select item expression into one or more generated projection fields.
     */
    private fun FirStatement.toProjectionFields(
        sourceType: ConeKotlinType,
        sourceValues: ProjectionSourceValueAccessors,
        receiverBindings: ProjectionReceiverBindings,
        aliasScopeId: Long,
    ): List<KronosProjectionField> {
        val statement = projectionStatementOrNull() ?: return emptyList()

        val propertyAccess = statement as? FirPropertyAccessExpression
        if (propertyAccess != null) {
            if (
                propertyAccess.isProjectionSourceValueAccess(
                    sourceType,
                    sourceValues,
                    propertyAccess.coneTypeOrNull
                )
            ) {
                return readSourceFields(sourceType)
            }
            return listOfNotNull(propertyAccess.toPropertyProjectionField(sourceType, receiverBindings))
        }

        val call = statement as? FirFunctionCall
        if (call != null) {
            call.toSourceMinusProjectionFields(sourceType, sourceValues)?.let { return it }
            return listOfNotNull(
                call.toAliasProjectionField(sourceType, receiverBindings, aliasScopeId)
                    ?: call.toAliasCallProjectionField(aliasScopeId)
            )
        }

        return listOfNotNull(statement.toAliasLiteralProjectionField(aliasScopeId))
    }

    private fun FirFunctionCall.toSourceMinusProjectionFields(
        sourceType: ConeKotlinType,
        sourceValues: ProjectionSourceValueAccessors
    ): List<KronosProjectionField>? {
        val sourceFields = readSourceFields(sourceType)
        val sourceFieldNames = sourceFields.mapTo(linkedSetOf()) { it.name.asString() }
        val excludedNames = sourceMinusExcludedProjectionFieldNames(
            sourceType,
            sourceFieldNames,
            sourceValues
        ) { statement -> statement.resolvedConeType() } ?: return null
        return sourceFields.filterNot { it.name.asString() in excludedNames }
    }

    /**
     * Converts one source property access into a same-name generated projection field.
     */
    private fun FirPropertyAccessExpression.toPropertyProjectionField(
        sourceType: ConeKotlinType,
        receiverBindings: ProjectionReceiverBindings,
    ): KronosProjectionField? {
        val rootClassId = (sourceType as? ConeClassLikeType)?.lookupTag?.classId
        val candidates = receiverCandidates()
        val resolvedSymbol = (calleeReference as? FirResolvedNamedReference)
            ?.resolvedSymbol as? FirPropertySymbol
        val resolvedOwnerClassId = resolvedSymbol?.callableId?.classId
        val resolvedProperty = resolvedSymbol?.toResolvedProjectionProperty()
        val receiverTypes = candidates
            .asSequence()
            .mapNotNull { receiver ->
                (receiver as? FirStatement)?.projectionReceiverType(receiverBindings)
            }
            .toList()
        val regularReceiverTypes = receiverTypes.filterNot { it.recoveredFromBinding }
        val recoveredReceiverType = receiverTypes.firstOrNull { it.recoveredFromBinding }?.type
        val concreteReceiverClassId = regularReceiverTypes
            .map { it.type }
            .filterIsInstance<ConeClassLikeType>()
            .firstOrNull()
            ?.lookupTag
            ?.classId
        val concreteReceiverProperty = regularReceiverTypes.firstResolvedSourceProperty(calleeReference.name)
        val boundReceiverProperty = recoveredReceiverType.resolvedSourcePropertyOrNull(calleeReference.name)
        if (recoveredReceiverType != null && boundReceiverProperty == null) return null
        val knownOwnerClassId = boundReceiverProperty?.ownerClassId
            ?: concreteReceiverProperty?.ownerClassId
            ?: resolvedOwnerClassId
            ?: concreteReceiverClassId
        val rootProperty = if (
            knownOwnerClassId == rootClassId ||
            (knownOwnerClassId == null && candidates.all { it.isImplicitThisReceiver() })
        ) {
            resolveSourceProperty(sourceType, calleeReference.name)
        } else {
            null
        }
        val property = boundReceiverProperty ?: concreteReceiverProperty ?: resolvedProperty ?: rootProperty
        val type = property?.type ?: resolvedProjectionAccessType() ?: return null
        val ownerClassId = property?.ownerClassId ?: resolvedOwnerClassId ?: concreteReceiverClassId
        val isRootSourceProperty = ownerClassId == rootClassId
        return KronosProjectionField(
            name = calleeReference.name,
            type = type,
            source = property?.source ?: source,
            sourceClassId = ownerClassId,
            isSourceAlias = isRootSourceProperty,
            signature = "property:${ownerClassId ?: rootClassId}:${calleeReference.name.asString()}"
        )
    }

    /**
     * Converts `source.property.alias("alias")` into a generated projection field named by the alias.
     */
    private fun FirFunctionCall.toAliasProjectionField(
        sourceType: ConeKotlinType,
        receiverBindings: ProjectionReceiverBindings,
        aliasScopeId: Long,
    ): KronosProjectionField? {
        if (calleeReference.name.asString() != SelectAliasFunctionName) return null
        val alias = argumentList.arguments.first().stringLiteralValue() ?: return null
        val sourceField = firstAliasProjectionField(sourceType, receiverBindings) ?: return null
        return sourceField.copy(
            name = Name.identifier(alias),
            source = source ?: sourceField.source,
            sourceName = sourceField.name,
            requestedName = Name.identifier(alias),
            signature = "${sourceField.signature}:alias:$alias",
            aliasOccurrence = aliasOccurrence(aliasScopeId, source),
        )
    }

    private fun List<ProjectionReceiverType>.firstResolvedSourceProperty(name: Name): ResolvedSourceProperty? {
        for (receiver in this) {
            val property = resolveSourceProperty(receiver.type, name)
            if (property != null) return property
        }
        return null
    }

    private fun ConeKotlinType?.resolvedSourcePropertyOrNull(name: Name): ResolvedSourceProperty? =
        if (this == null) null else resolveSourceProperty(this, name)

    private fun FirFunctionCall.firstAliasProjectionField(
        sourceType: ConeKotlinType,
        receiverBindings: ProjectionReceiverBindings,
    ): KronosProjectionField? {
        val receivers = listOfNotNull(explicitReceiver, extensionReceiver, dispatchReceiver) + argumentList.arguments
        for (receiver in receivers) {
            val access = (receiver as? FirStatement)?.projectionStatementOrNull() as? FirPropertyAccessExpression
            val field = access?.toPropertyProjectionField(sourceType, receiverBindings)
            if (field != null) return field
        }
        return null
    }

    /**
     * Converts an `alias("alias")` call into a projection field when FIR has erased the call receiver.
     */
    private fun FirFunctionCall.toAliasCallProjectionField(aliasScopeId: Long): KronosProjectionField? {
        if (calleeReference.name.asString() != SelectAliasFunctionName) return null
        val alias = argumentList.arguments.first().stringLiteralValue() ?: return null
        if (!GeneratedProjectionFieldIdentifierRegex.matches(alias)) return null
        val type = aliasReceiverProjectionType()
            ?: aliasReceiverType()
            ?: aliasCallProjectionType()
            ?: aliasedExpressionType(alias, aliasOccurrence(aliasScopeId, source))
            ?: session.builtinTypes.nullableAnyType.coneType
        return KronosProjectionField(
            name = Name.identifier(alias),
            type = type,
            source = source,
            sourceName = Name.identifier(alias),
            signature = "alias:$alias",
            aliasOccurrence = aliasOccurrence(aliasScopeId, source),
        )
    }

    /**
     * Infers an alias field type from the receiver of `receiver.alias("alias")`.
     */
    private fun FirFunctionCall.aliasReceiverType(): ConeKotlinType? {
        return aliasReceiverStatement()?.resolvedConeType()
    }

    private fun FirFunctionCall.aliasReceiverStatement(): FirStatement? {
        return (extensionReceiver as? FirStatement) ?: (dispatchReceiver as? FirStatement)
    }

    private fun FirStatement.resolvedConeType(): ConeKotlinType? {
        return when (val statement = projectionStatementOrNull()) {
            is FirPropertyAccessExpression -> statement.resolvedProjectionAccessType()
            is FirFunctionCall -> statement.coneTypeOrNull
            else -> null
        }
    }

    private fun FirStatement.projectionReceiverType(
        receiverBindings: ProjectionReceiverBindings
    ): ProjectionReceiverType? {
        val access = projectionStatementOrNull() as? FirPropertyAccessExpression ?: return null
        access.resolvedProjectionAccessType()?.let {
            return ProjectionReceiverType(it, recoveredFromBinding = false)
        }
        if (!access.receiverCandidates().all { it.isImplicitThisReceiver() }) return null
        return receiverBindings.typeOf(access)?.let { type ->
            ProjectionReceiverType(type, recoveredFromBinding = true)
        }
    }

    /**
     * If the alias receiver is a SelectClause, use its generated Selected type. For scalar
     * subqueries the Selected projection must contain exactly one field, so that field is
     * the alias type used by later Context clauses.
     */
    private fun FirFunctionCall.aliasReceiverProjectionType(): ConeKotlinType? {
        val receiverType = aliasReceiverType() as? ConeClassLikeType ?: return null
        return receiverType.scalarSelectProjectionFieldType()
    }

    private fun FirFunctionCall.aliasCallProjectionType(): ConeKotlinType? {
        val callType = resolvedConeType() as? ConeClassLikeType ?: return null
        return callType.scalarSelectProjectionFieldType()
    }

    private fun ConeClassLikeType.scalarSelectProjectionFieldType(): ConeKotlinType? {
        val selectedType = selectedTypeArgumentOrNull(selectFromIndex = 1) as? ConeClassLikeType ?: return null
        val model = KronosProjectionRegistry.find(session, selectedType.lookupTag.classId) ?: return null
        return model.fields.singleOrNull()?.type
    }

    private fun ConeClassLikeType.selectedTypeArgumentOrNull(selectFromIndex: Int): ConeKotlinType? {
        val selectedArgumentIndex = selectedTypeArgumentIndexByClassId[lookupTag.classId]
            ?: if (lookupTag.classId.isJoinSourceClassId()) selectFromIndex else return null
        return (typeArguments.getOrNull(selectedArgumentIndex) as? ConeKotlinTypeProjection)?.type
    }

    /**
     * FIR can later expose `alias("alias")` inside a collection literal as only the
     * alias literal, without the original receiver. Record the resolved receiver
     * type while resolving the actual call so the outer projection can still be typed.
     */
    private fun recordAliasedExpressionType(callInfo: CallInfo, symbol: FirNamedFunctionSymbol) {
        if (symbol.callableId.callableName.asString() != SelectAliasFunctionName) return
        val alias = callInfo.arguments.first().stringLiteralValue() ?: return
        if (!GeneratedProjectionFieldIdentifierRegex.matches(alias)) return
        val receiverType = callInfo.explicitReceiver?.coneTypeOrNull ?: return
        val aliasType = (receiverType as? ConeClassLikeType)?.scalarSelectProjectionFieldType() ?: receiverType
        val scope = callInfo.containingDeclarations.asReversed()
            .filterIsInstance<FirAnonymousFunction>()
            .firstOrNull()
            ?: return
        val occurrence = aliasOccurrence(aliasScopeId(scope.symbol), callInfo.callSite.source) ?: return
        aliasedExpressionTypes.removeAll { cached ->
            cached.alias == alias && cached.occurrence == occurrence
        }
        aliasedExpressionTypes += AliasedExpressionType(alias, occurrence, aliasType)
        KronosProjectionRegistry.refineAliasFieldType(session, alias, occurrence, aliasType)
    }

    private fun aliasedExpressionType(
        alias: String,
        occurrence: ProjectionAliasOccurrence?,
    ): ConeKotlinType? {
        if (occurrence == null) return null
        val matching = aliasedExpressionTypes
            .filter { cached -> cached.alias == alias && cached.occurrence.overlaps(occurrence) }
            .minByOrNull { cached -> cached.occurrence.rangeSize }
        return matching?.type
    }

    /**
     * Converts a resolved alias literal from `alias()` into a projection field when FIR loses the receiver call.
     */
    private fun FirStatement.toAliasLiteralProjectionField(aliasScopeId: Long): KronosProjectionField? {
        val alias = stringLiteralValue() ?: return null
        if (!GeneratedProjectionFieldIdentifierRegex.matches(alias)) return null
        return KronosProjectionField(
            name = Name.identifier(alias),
            type = session.builtinTypes.nullableAnyType.coneType,
            source = source,
            sourceName = Name.identifier(alias),
            signature = "aliasLiteral:$alias",
            aliasOccurrence = aliasOccurrence(aliasScopeId, source),
        )
    }

    /**
     * Resolves the original DTO property type from the source receiver.
     */
    private fun resolveSourceProperty(sourceType: ConeKotlinType, name: Name): ResolvedSourceProperty? {
        val classSymbol = sourceType.toClassSymbol(session) ?: return null
        val scope = session.declaredMemberScope(classSymbol.fir, memberRequiredPhase = FirResolvePhase.STATUS)
        var result: ResolvedSourceProperty? = null
        scope.processPropertiesByName(name) { propertySymbol: FirVariableSymbol<*> ->
            val type = (propertySymbol.fir.returnTypeRef as? FirResolvedTypeRef)?.coneType
            if (type != null) {
                result = ResolvedSourceProperty(
                    ownerClassId = classSymbol.classId,
                    type = type,
                    source = propertySymbol.fir.source,
                )
            }
        }
        return result
    }

    private fun FirPropertySymbol.toResolvedProjectionProperty(): ResolvedSourceProperty? {
        val type = (fir.returnTypeRef as? FirResolvedTypeRef)?.coneType ?: return null
        return ResolvedSourceProperty(
            ownerClassId = callableId?.classId,
            type = type,
            source = fir.source,
        )
    }

    private data class ResolvedSourceProperty(
        val ownerClassId: ClassId?,
        val type: ConeKotlinType,
        val source: KtSourceElement?,
    )

    private data class ProjectionReceiverType(
        val type: ConeKotlinType,
        val recoveredFromBinding: Boolean,
    )

    private data class ProjectionReceiverBinding(
        val name: Name,
        val symbol: FirValueParameterSymbol?,
        val type: ConeKotlinType,
        val scope: KtSourceElement?,
        val lexicalPriority: Int,
    ) {
        val scopeSize: Int
            get() = scope?.let { it.endOffset - it.startOffset } ?: Int.MAX_VALUE

        fun contains(source: KtSourceElement?): Boolean =
            source == null || scope == null ||
                (scope.startOffset <= source.startOffset && source.endOffset <= scope.endOffset)

        fun matches(access: FirPropertyAccessExpression): Boolean {
            val accessSymbol = (access.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
            return when (accessSymbol) {
                is FirValueParameterSymbol -> symbol == accessSymbol
                null -> name == access.calleeReference.name
                else -> false
            }
        }
    }

    private class ProjectionReceiverBindings(
        private val bindings: List<ProjectionReceiverBinding>,
    ) {
        fun typeOf(access: FirPropertyAccessExpression): ConeKotlinType? = bindings
            .asSequence()
            .filter { binding -> binding.matches(access) && binding.contains(access.source) }
            .minWithOrNull(compareBy<ProjectionReceiverBinding> { it.scopeSize }.thenBy { it.lexicalPriority })
            ?.type
    }

    private data class AliasedExpressionType(
        val alias: String,
        val occurrence: ProjectionAliasOccurrence,
        val type: ConeKotlinType,
    )

    private fun aliasScopeId(scope: Any): Long =
        aliasScopeIds[scope] ?: nextAliasScopeId++.also { aliasScopeIds[scope] = it }

    private fun aliasOccurrence(scopeId: Long, source: KtSourceElement?): ProjectionAliasOccurrence? =
        source?.let { ProjectionAliasOccurrence(scopeId, it.startOffset, it.endOffset) }

    /**
     * Rewrites query return types to the generated projection class or list of it.
     */
    private fun refineQueryReturnType(symbol: FirNamedFunctionSymbol, model: KronosProjectionModel): ConeKotlinType? {
        return queryReturnTypeByName[symbol.callableId.callableName.asString()]?.invoke(model)
    }

    /**
     * Extracts one select projection model from the lambda body.
     */
    private fun buildProjectionModel(
        callInfo: CallInfo,
        isJoinSelect: Boolean,
    ): KronosProjectionModel? {
        val lambda = callInfo.arguments.lastOrNull() as? FirAnonymousFunctionExpression ?: return null
        val joinLambda = if (isJoinSelect) callInfo.nearestJoinSourceLambda() else null
        val sourceType = callInfo.selectSourceType(lambda.anonymousFunction, joinLambda) ?: return null
        val sourceDeclaration = sourceType.toClassSymbol(session)?.fir?.source
        val returned = lambda.anonymousFunction.body?.lastExpression() ?: return null
        val sourceValues = lambda.anonymousFunction.valueParameters.projectionSourceValueAccessors()
        if (returned.isIdentitySourceProjection(sourceType, sourceValues)) return null
        val aliasScopeId = aliasScopeId(lambda.anonymousFunction.symbol)
        val receiverBindings = projectionReceiverBindings(
            lambda.anonymousFunction,
            sourceType,
            sourceValues,
            joinLambda,
        )
        val fields = readProjectionFields(
            returned,
            sourceType,
            sourceValues,
            receiverBindings,
            aliasScopeId
        ).withUniqueProjectionNames()
        if (fields.isEmpty()) return null

        val name = Name.identifier("$GeneratedProjectionClassPrefix${mangleProjectionName(sourceType, fields)}")
        val effectiveSourceFields = effectiveSourceFields(returned, sourceType, sourceValues)
        val effectiveSourceNames = effectiveSourceFields.mapTo(linkedSetOf()) { it.name }
        val shadowedSourceNames = fields.mapNotNullTo(linkedSetOf()) { field ->
            field.name.takeIf { name ->
                name in effectiveSourceNames && !(field.isSourceAlias && field.sourceName == name)
            }
        }
        val contextFields = mergeContextFields(effectiveSourceFields, fields)
        val contextName = Name.identifier("$GeneratedContextClassPrefix${mangleProjectionName(sourceType, contextFields)}")
        val anchor = callInfo.callSite.source ?: lambda.source ?: return null
        val classId = ClassId(GeneratedProjectionPackageFqName, FqName.topLevel(name), isLocal = false)
        val contextClassId = ClassId(GeneratedProjectionPackageFqName, FqName.topLevel(contextName), isLocal = false)
        val symbol = FirRegularClassSymbol(classId)
        val contextSymbol = FirRegularClassSymbol(contextClassId)
        val model = KronosProjectionModel(
            classId = classId,
            name = name,
            symbol = symbol,
            contextClassId = contextClassId,
            contextName = contextName,
            contextSymbol = contextSymbol,
            sourceType = sourceType,
            fields = fields,
            contextFields = contextFields,
            shadowedSourceNames = shadowedSourceNames,
            anchor = anchor,
            sourceDeclaration = sourceDeclaration ?: anchor,
        )
        return model
    }

    /** Recovers unresolved simple receivers from the current select and, for JOINs, its nearest JOIN scope. */
    private fun projectionReceiverBindings(
        projectionLambda: FirAnonymousFunction,
        sourceType: ConeKotlinType,
        sourceValues: ProjectionSourceValueAccessors,
        joinLambda: FirAnonymousFunction?,
    ): ProjectionReceiverBindings {
        val bindings = mutableListOf<ProjectionReceiverBinding>()
        val projectionScope = projectionLambda.body?.source ?: projectionLambda.source
        sourceValues.names.forEach { name ->
            val parameter = projectionLambda.valueParameters.firstOrNull { it.name == name }
            bindings += ProjectionReceiverBinding(
                name = name,
                symbol = parameter?.symbol,
                type = sourceType,
                scope = projectionScope,
                lexicalPriority = 0,
            )
        }

        joinLambda ?: return ProjectionReceiverBindings(bindings)
        val joinReceiverType = joinLambda.joinSourceReceiverType() ?: return ProjectionReceiverBindings(bindings)
        val joinScope = joinLambda.body?.source ?: joinLambda.source
        val parameterTypes = joinReceiverType.typeArguments.map { argument ->
            (argument as? ConeKotlinTypeProjection)?.type
        }
        joinLambda.valueParameters.forEachIndexed { index, parameter ->
            val type = (parameter.returnTypeRef as? FirResolvedTypeRef)?.coneType
                ?: parameterTypes.getOrNull(index)
                ?: return@forEachIndexed
            bindings += ProjectionReceiverBinding(
                name = parameter.name,
                symbol = parameter.symbol,
                type = type,
                scope = joinScope,
                lexicalPriority = 1,
            )
        }
        return ProjectionReceiverBindings(bindings)
    }

    private fun CallInfo.nearestJoinSourceLambda(): FirAnonymousFunction? =
        containingDeclarations.asReversed()
            .filterIsInstance<FirAnonymousFunction>()
            .firstOrNull { it.joinSourceReceiverType() != null }

    private fun FirAnonymousFunction.joinSourceReceiverType(): ConeClassLikeType? =
        ((receiverParameter?.typeRef as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType)
            ?.takeIf { it.lookupTag.classId.isJoinSourceClassId() }

    /**
     * Keeps receiverless values with the Source type as `SelectClause<Source, Source, Source>`.
     */
    private fun FirStatement.isIdentitySourceProjection(
        sourceType: ConeKotlinType,
        sourceValues: ProjectionSourceValueAccessors
    ): Boolean {
        val item = projectionItems().singleOrNull() ?: return false
        val propertyAccess = item.projectionStatementOrNull() as? FirPropertyAccessExpression ?: return false
        if (propertyAccess.receiverCandidates().isNotEmpty()) return false
        val accessType = propertyAccess.resolvedProjectionAccessType()
        if (propertyAccess.isProjectionSourceValueParameter(sourceValues)) {
            return accessType?.hasSameClassIdAs(sourceType) != false
        }

        // Also allow `select { row -> it }` when `it` is an outer receiverless value with the Source type.
        return accessType.hasSameClassIdAs(sourceType)
    }

    private fun FirPropertyAccessExpression.receiverCandidates(): List<FirExpression> =
        listOfNotNull(explicitReceiver, dispatchReceiver, extensionReceiver).distinct()

    private fun FirExpression.isImplicitThisReceiver(): Boolean =
        (projectionStatementOrNull() as? FirThisReceiverExpression)?.isImplicit == true

    /**
     * Uses the KPojo receiver as Source for normal selects, and the receiver's Selected
     * type as Source when selecting from a selectable query layer.
     */
    private fun CallInfo.selectSourceType(
        projectionLambda: FirAnonymousFunction,
        joinLambda: FirAnonymousFunction?,
    ): ConeKotlinType? {
        val receiverResolvedType = (explicitReceiver as? FirPropertyAccessExpression)
            ?.resolvedProjectionAccessType()
        val projectionParameterType = projectionLambda.valueParameters
            .singleOrNull()
            ?.let { parameter -> (parameter.returnTypeRef as? FirResolvedTypeRef)?.coneType }
        val joinSourceType = joinLambda
            ?.joinSourceReceiverType()
            ?.selectedTypeArgumentOrNull(selectFromIndex = 0)
        return sequenceOf(
            explicitReceiver?.coneTypeOrNull,
            receiverResolvedType,
            projectionParameterType,
            joinSourceType,
        ).firstNotNullOfOrNull { candidate -> candidate?.sourceRowTypeOrNull() }
    }

    private fun ConeKotlinType.sourceRowTypeOrNull(): ConeKotlinType? {
        var current = this
        while (true) {
            val classType = current as? ConeClassLikeType ?: return null
            val selectedType = classType.selectedTypeArgumentOrNull(selectFromIndex = 0) ?: return classType
            if (selectedType == current) return null
            current = selectedType
        }
    }

    /**
     * Maps the refined projection class into SelectClause<Source, Projection, Context>.
     */
    private fun selectClauseType(sourceType: ConeKotlinType, model: KronosProjectionModel): ConeClassLikeTypeImpl {
        return ConeClassLikeTypeImpl(
            SelectClauseClassId.toLookupTag(),
            arrayOf(
                sourceType,
                projectionType(model, isNullable = false),
                contextType(model, isNullable = false)
            ),
            false,
            ConeAttributes.Empty
        )
    }

    /**
     * Maps the refined projection class back to the original query-layer type.
     */
    private fun selectReturnType(
        callInfo: CallInfo,
        symbol: FirNamedFunctionSymbol,
        sourceType: ConeKotlinType,
        model: KronosProjectionModel
    ): ConeClassLikeTypeImpl {
        if (isJoinSelectCall(callInfo, symbol)) {
            return ConeClassLikeTypeImpl(
                JoinedSelectQueryClassId.toLookupTag(),
                arrayOf(
                    sourceType,
                    projectionType(model, isNullable = false),
                    contextType(model, isNullable = false)
                ),
                false,
                ConeAttributes.Empty
            )
        }
        return selectClauseType(sourceType, model)
    }

    /**
     * Builds the concrete projection return type for first/firstOrNull.
     */
    private fun projectionType(model: KronosProjectionModel, isNullable: Boolean): ConeClassLikeTypeImpl {
        return ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagWithFixedSymbol(model.classId, model.symbol),
            ConeTypeProjection.EMPTY_ARRAY,
            isNullable,
            ConeAttributes.Empty
        )
    }

    /**
     * Builds the generated Context type used by orderBy after select.
     */
    private fun contextType(model: KronosProjectionModel, isNullable: Boolean): ConeClassLikeTypeImpl {
        return ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagWithFixedSymbol(model.contextClassId, model.contextSymbol),
            ConeTypeProjection.EMPTY_ARRAY,
            isNullable,
            ConeAttributes.Empty
        )
    }

    /**
     * Wraps the projection type in List for toList().
     */
    private fun listType(model: KronosProjectionModel): ConeClassLikeTypeImpl {
        return ConeClassLikeTypeImpl(
            StandardClassIds.List.toLookupTag(),
            arrayOf(projectionType(model, isNullable = false)),
            false,
            ConeAttributes.Empty
        )
    }

    /**
     * Builds the post-select Context shape: all source properties plus selected projection fields.
     */
    private fun mergeContextFields(
        sourceFields: List<KronosProjectionField>,
        selectedFields: List<KronosProjectionField>
    ): List<KronosProjectionField> {
        val result = linkedMapOf<Name, KronosProjectionField>()
        sourceFields.forEach { field -> result[field.name] = field }
        selectedFields.forEach { field -> result[field.name] = field }
        return result.values.toList()
    }

    /**
     * Applies all top-level source-minus exclusions before selected fields are merged into Context.
     */
    private fun effectiveSourceFields(
        expression: FirStatement,
        sourceType: ConeKotlinType,
        sourceValues: ProjectionSourceValueAccessors
    ): List<KronosProjectionField> {
        val sourceFields = readSourceFields(sourceType)
        val sourceNames = sourceFields.mapTo(linkedSetOf()) { it.name.asString() }
        val excluded = expression.projectionItems().asSequence()
            .mapNotNull { it.projectionStatementOrNull() as? FirFunctionCall }
            .filter { it.calleeReference.name.asString() == "minus" }
            .flatMap { minus ->
                minus.sourceMinusExcludedProjectionFieldNames(
                    sourceType,
                    sourceNames,
                    sourceValues
                ) { statement -> statement.resolvedConeType() }?.asSequence().orEmpty()
            }
            .toSet()
        return sourceFields.filterNot { it.name.asString() in excluded }
    }

    /**
     * Reads source KPojo properties as Context fields so post-select clauses can still filter source columns.
     */
    private fun readSourceFields(sourceType: ConeKotlinType): List<KronosProjectionField> {
        val classSymbol = sourceType.toClassSymbol(session) ?: return emptyList()
        val fields = mutableListOf<KronosProjectionField>()
        classSymbol.processAllDeclarations(session, FirResolvePhase.STATUS) { symbol ->
            val property = (symbol as? FirPropertySymbol)?.fir ?: return@processAllDeclarations
            if (!property.isKronosColumn(session)) return@processAllDeclarations
            val name = property.name
            val type = (property.returnTypeRef as? FirResolvedTypeRef)?.coneType ?: return@processAllDeclarations
            fields += KronosProjectionField(
                name = name,
                type = type,
                source = property.source,
                sourceName = name,
                sourceClassId = classSymbol.classId,
                isSourceAlias = true,
                signature = "property:${name.asString()}"
            )
        }
        return fields
    }

}
