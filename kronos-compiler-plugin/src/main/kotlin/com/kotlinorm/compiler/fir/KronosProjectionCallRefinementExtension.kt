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

import com.kotlinorm.compiler.utils.GeneratedProjectionClassPrefix
import com.kotlinorm.compiler.utils.GeneratedContextClassPrefix
import com.kotlinorm.compiler.utils.GeneratedProjectionFieldIdentifierRegex
import com.kotlinorm.compiler.utils.GeneratedProjectionPackageFqName
import com.kotlinorm.compiler.utils.JoinPackageFqName
import com.kotlinorm.compiler.utils.KotlinListOfFunctionName
import com.kotlinorm.compiler.utils.KSelectableClassId
import com.kotlinorm.compiler.utils.QueryListFunctionName
import com.kotlinorm.compiler.utils.QueryOneFunctionName
import com.kotlinorm.compiler.utils.QueryOneOrNullFunctionName
import com.kotlinorm.compiler.utils.SelectAliasFunctionName
import com.kotlinorm.compiler.utils.SelectClauseClassId
import com.kotlinorm.compiler.utils.SelectFromClassId
import com.kotlinorm.compiler.utils.SelectFunctionName
import com.kotlinorm.compiler.utils.SelectPackageFqName
import com.kotlinorm.compiler.utils.SelectQueryFunctionNames
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirFunctionCallRefinementExtension
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CallInfo
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
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
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import java.security.MessageDigest

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
    private val aliasedExpressionTypes = mutableMapOf<String, ConeKotlinType>()
    private val selectedTypeArgumentIndexByClassId = mapOf(
        SelectClauseClassId to 1,
        KSelectableClassId to 0
    )
    private val queryReturnTypeByName = mapOf<String, (KronosProjectionModel) -> ConeKotlinType?>(
        QueryListFunctionName to { model -> listType(model) },
        QueryOneFunctionName to { model -> projectionType(model, isNullable = false) },
        QueryOneOrNullFunctionName to { model -> projectionType(model, isNullable = true) }
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

        val model = buildProjectionModel(callInfo) ?: return null
        KronosProjectionRegistry.register(session, model)
        KronosProjectionDeclarationGenerationExtension.ensureProjectionClassBound(session, model)
        KronosProjectionDeclarationGenerationExtension.ensureContextClassBound(session, model)
        val typeRef = buildResolvedTypeRef {
            source = callInfo.callSite.source
            coneType = selectReturnType(callInfo, model.sourceType, model)
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
        return callableId.packageName == SelectPackageFqName || callableId.classId?.isSelectFromClassId() == true
    }

    /**
     * Checks whether this is a query call on a generated projection receiver.
     */
    private fun isQueryCall(symbol: FirNamedFunctionSymbol, callInfo: CallInfo): Boolean {
        val callableId = symbol.callableId
        if (callableId.callableName.asString() !in SelectQueryFunctionNames) return false
        if (callableId.packageName != SelectPackageFqName && callableId.classId?.isSelectFromClassId() != true) {
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
    private fun readProjectionFields(expression: FirStatement, sourceType: ConeKotlinType): List<KronosProjectionField> {
        val statement = expression.projectionStatementOrNull() ?: return emptyList()
        return statement.projectionItems(includeSingle = true).mapNotNull { it.toProjectionField(sourceType) }
    }

    /**
     * Converts one select item expression into a generated projection field.
     */
    private fun FirStatement.toProjectionField(sourceType: ConeKotlinType): KronosProjectionField? {
        val statement = projectionStatementOrNull() ?: return null

        val propertyAccess = statement as? FirPropertyAccessExpression
        if (propertyAccess != null) return propertyAccess.toPropertyProjectionField(sourceType)

        val call = statement as? FirFunctionCall
        if (call != null) return call.toAliasProjectionField(sourceType) ?: call.toAliasCallProjectionField()

        return statement.toAliasLiteralProjectionField()
    }

    /**
     * Converts one source property access into a same-name generated projection field.
     */
    private fun FirPropertyAccessExpression.toPropertyProjectionField(sourceType: ConeKotlinType): KronosProjectionField? {
        val property = resolveSourceProperty(sourceType, calleeReference.name)
        val type = property?.type ?: coneTypeOrNull ?: return null
        return KronosProjectionField(
            name = calleeReference.name,
            type = type,
            source = property?.source,
            signature = "property:${calleeReference.name.asString()}"
        )
    }

    /**
     * Converts `source.property.alias("alias")` into a generated projection field named by the alias.
     */
    private fun FirFunctionCall.toAliasProjectionField(sourceType: ConeKotlinType): KronosProjectionField? {
        if (calleeReference.name.asString() != SelectAliasFunctionName) return null
        val alias = argumentList.arguments.first().stringLiteralValue() ?: return null
        val sourceField = (extensionReceiver as? FirPropertyAccessExpression)?.toPropertyProjectionField(sourceType)
            ?: (dispatchReceiver as? FirPropertyAccessExpression)?.toPropertyProjectionField(sourceType)
            ?: return null
        return sourceField.copy(
            name = Name.identifier(alias),
            source = source ?: sourceField.source,
            sourceName = sourceField.name,
            signature = "${sourceField.signature}:alias:$alias"
        )
    }

    /**
     * Converts an `alias("alias")` call into a projection field when FIR has erased the call receiver.
     */
    private fun FirFunctionCall.toAliasCallProjectionField(): KronosProjectionField? {
        if (calleeReference.name.asString() != SelectAliasFunctionName) return null
        val alias = argumentList.arguments.first().stringLiteralValue() ?: return null
        if (!GeneratedProjectionFieldIdentifierRegex.matches(alias)) return null
        val type = aliasReceiverProjectionType()
            ?: aliasReceiverType()
            ?: aliasCallProjectionType()
            ?: aliasedExpressionTypes[alias]
            ?: session.builtinTypes.nullableAnyType.coneType
        return KronosProjectionField(
            name = Name.identifier(alias),
            type = type,
            source = source,
            sourceName = Name.identifier(alias),
            signature = "alias:$alias"
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
            is FirPropertyAccessExpression -> statement.coneTypeOrNull
            is FirFunctionCall -> statement.coneTypeOrNull
            else -> null
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
            ?: if (lookupTag.classId.isSelectFromClassId()) selectFromIndex else return null
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
        aliasedExpressionTypes[alias] = aliasType
        KronosProjectionRegistry.refineAliasFieldType(session, alias, aliasType)
    }

    /**
     * Converts a resolved alias literal from `alias()` into a projection field when FIR loses the receiver call.
     */
    private fun FirStatement.toAliasLiteralProjectionField(): KronosProjectionField? {
        val alias = stringLiteralValue() ?: return null
        if (!GeneratedProjectionFieldIdentifierRegex.matches(alias)) return null
        return KronosProjectionField(
            name = Name.identifier(alias),
            type = session.builtinTypes.nullableAnyType.coneType,
            source = source,
            sourceName = Name.identifier(alias),
            signature = "aliasLiteral:$alias"
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
                result = ResolvedSourceProperty(type, propertySymbol.fir.source)
            }
        }
        return result
    }

    private data class ResolvedSourceProperty(
        val type: ConeKotlinType,
        val source: KtSourceElement?,
    )

    /**
     * Rewrites query return types to the generated projection class or list of it.
     */
    private fun refineQueryReturnType(symbol: FirNamedFunctionSymbol, model: KronosProjectionModel): ConeKotlinType? {
        return queryReturnTypeByName[symbol.callableId.callableName.asString()]?.invoke(model)
    }

    /**
     * Extracts one select projection model from the lambda body.
     */
    private fun buildProjectionModel(callInfo: CallInfo): KronosProjectionModel? {
        val sourceType = callInfo.selectSourceType() ?: return null
        val sourceDeclaration = sourceType.toClassSymbol(session)?.fir?.source
        val lambda = callInfo.arguments.lastOrNull() as? FirAnonymousFunctionExpression ?: return null
        val returned = lambda.anonymousFunction.body?.lastExpression() ?: return null
        val fields = readProjectionFields(returned, sourceType)
        if (fields.isEmpty()) return null

        val name = Name.identifier("$GeneratedProjectionClassPrefix${mangleProjectionName(sourceType, fields)}")
        val contextFields = mergeContextFields(sourceType, fields)
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
            anchor = anchor,
            sourceDeclaration = sourceDeclaration ?: anchor,
        )
        return model
    }

    /**
     * Uses the KPojo receiver as Source for normal selects, and the receiver's Selected
     * type as Source when selecting from a selectable query layer.
     */
    private fun CallInfo.selectSourceType(): ConeKotlinType? {
        val receiverType = explicitReceiver?.coneTypeOrNull ?: return null
        val receiverClassType = receiverType as? ConeClassLikeType ?: return receiverType
        return receiverClassType.selectedTypeArgumentOrNull(selectFromIndex = 0) ?: receiverType
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
        sourceType: ConeKotlinType,
        model: KronosProjectionModel
    ): ConeClassLikeTypeImpl {
        val receiverType = callInfo.explicitReceiver?.coneTypeOrNull as? ConeClassLikeType
        if (receiverType?.lookupTag?.classId?.isSelectFromClassId() == true) {
            return ConeClassLikeTypeImpl(
                SelectFromClassId.toLookupTag(),
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
     * Builds the concrete projection return type for queryOne/queryOneOrNull.
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
     * Wraps the projection type in List for queryList().
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
     * Generates a stable synthetic class suffix from the source row type and selected field signatures.
     */
    private fun mangleProjectionName(sourceType: ConeKotlinType, fields: List<KronosProjectionField>): String {
        val sourceClass = (sourceType as? ConeClassLikeType)?.lookupTag?.classId?.asFqNameString()
            ?: sourceType.renderForMangle()
        val raw = buildString {
            append(sourceClass)
            fields.forEach { field ->
                append('|')
                append(field.signature)
                append(':')
                append(field.type.renderForMangle())
            }
        }
        return stableHash(raw)
    }

    /**
     * Builds the post-select Context shape: all source properties plus selected projection fields.
     */
    private fun mergeContextFields(
        sourceType: ConeKotlinType,
        selectedFields: List<KronosProjectionField>
    ): List<KronosProjectionField> {
        val result = linkedMapOf<Name, KronosProjectionField>()
        readSourceFields(sourceType).forEach { field -> result[field.name] = field }
        selectedFields.forEach { field -> result[field.name] = field }
        return result.values.toList()
    }

    /**
     * Reads source KPojo properties as Context fields so post-select clauses can still filter source columns.
     */
    private fun readSourceFields(sourceType: ConeKotlinType): List<KronosProjectionField> {
        val classSymbol = sourceType.toClassSymbol(session) ?: return emptyList()
        val fields = mutableListOf<KronosProjectionField>()
        classSymbol.processAllDeclarations(session, FirResolvePhase.STATUS) { symbol ->
            val property = (symbol as? FirPropertySymbol)?.fir ?: return@processAllDeclarations
            val name = property.name
            val type = (property.returnTypeRef as? FirResolvedTypeRef)?.coneType ?: return@processAllDeclarations
            fields += KronosProjectionField(
                name = name,
                type = type,
                source = property.source,
                sourceName = name,
                signature = "contextSource:${name.asString()}"
            )
        }
        return fields
    }

    /**
     * Produces a short deterministic hexadecimal hash for generated class names.
     */
    private fun stableHash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.take(8).joinToString("") { byte -> "%02x".format(byte) }
    }

    /**
     * Renders a type only for deterministic generated-name hashing.
     */
    private fun ConeKotlinType.renderForMangle(): String {
        val classLike = this as? ConeClassLikeType
        return classLike?.lookupTag?.classId?.asFqNameString() ?: toString()
    }

    private fun ClassId.isSelectFromClassId(): Boolean {
        if (this == SelectFromClassId) return true
        if (packageFqName != JoinPackageFqName) return false
        return shortClassName.asString().matches(Regex("SelectFrom\\d*"))
    }
}
