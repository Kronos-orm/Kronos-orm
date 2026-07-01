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
import com.kotlinorm.compiler.utils.QueryListFunctionName
import com.kotlinorm.compiler.utils.QueryOneFunctionName
import com.kotlinorm.compiler.utils.QueryOneOrNullFunctionName
import com.kotlinorm.compiler.utils.SelectAliasFunctionName
import com.kotlinorm.compiler.utils.SelectClauseClassId
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
            return CallReturnType(typeRef)
        }

        if (!isBareSelectCall(symbol, callInfo)) return null

        val model = buildProjectionModel(callInfo) ?: return null
        KronosProjectionRegistry.register(session, model)
        KronosProjectionDeclarationGenerationExtension.ensureProjectionClassBound(session, model)
        KronosProjectionDeclarationGenerationExtension.ensureContextClassBound(session, model)
        val typeRef = buildResolvedTypeRef {
            source = callInfo.callSite.source
            coneType = selectClauseType(model.sourceType, model)
        }

        return CallReturnType(typeRef)
    }

    /**
     * Checks whether this call is a bare select lambda we can project.
     */
    private fun isBareSelectCall(symbol: FirNamedFunctionSymbol, callInfo: CallInfo): Boolean {
        val callableId = symbol.callableId
        if (callableId.packageName != SelectPackageFqName) return false
        if (callableId.callableName != SelectFunctionName) return false
        if (callInfo.arguments.size != 1) return false
        return callInfo.arguments.lastOrNull() is FirAnonymousFunctionExpression
    }

    /**
     * Checks whether this is a query call on a generated projection receiver.
     */
    private fun isQueryCall(symbol: FirNamedFunctionSymbol, callInfo: CallInfo): Boolean {
        val callableId = symbol.callableId
        if (callableId.packageName != SelectPackageFqName) return false
        if (callableId.callableName.asString() !in SelectQueryFunctionNames) return false
        return callInfo.explicitReceiver?.coneTypeOrNull != null
    }

    /**
     * Reads the generated projection model back from the SelectClause receiver.
     */
    private fun readQueryProjectionModels(callInfo: CallInfo): List<KronosProjectionModel> {
        val receiverType = callInfo.explicitReceiver?.coneTypeOrNull as? ConeClassLikeType ?: return emptyList()
        if (receiverType.lookupTag.classId != SelectClauseClassId) return emptyList()
        val projectionArgument = receiverType.typeArguments.getOrNull(1) as? ConeKotlinTypeProjection ?: return emptyList()
        val projectionType = projectionArgument.type as? ConeClassLikeType ?: return emptyList()
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
        expression.toProjectionField(sourceType)?.let { return listOf(it) }

        val literal = expression as? FirCollectionLiteral
        if (literal != null) {
            return literal.argumentList.arguments.mapNotNull { it.toProjectionField(sourceType) }
        }

        val call = expression as? FirFunctionCall ?: return emptyList()
        val vararg = call.argumentList.arguments.singleOrNull() as? FirVarargArgumentsExpression ?: return emptyList()
        return vararg.arguments.mapNotNull { it.toProjectionField(sourceType) }
    }

    /**
     * Converts one select item expression into a generated projection field.
     */
    private fun FirStatement.toProjectionField(sourceType: ConeKotlinType): KronosProjectionField? {
        val wrapped = this as? FirWrappedExpression
        if (wrapped != null) return wrapped.expression.toProjectionField(sourceType)

        val propertyAccess = this as? FirPropertyAccessExpression
        if (propertyAccess != null) return propertyAccess.toPropertyProjectionField(sourceType)

        val call = this as? FirFunctionCall
        if (call != null) return call.toAliasProjectionField(sourceType) ?: call.toAliasCallProjectionField()

        return toAliasLiteralProjectionField()
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
     * Converts `source.property.as_("alias")` into a generated projection field named by the alias.
     */
    private fun FirFunctionCall.toAliasProjectionField(sourceType: ConeKotlinType): KronosProjectionField? {
        if (calleeReference.name.asString() != SelectAliasFunctionName) return null
        val alias = argumentList.arguments.firstOrNull()?.stringLiteralValue() ?: return null
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
     * Converts an `as_("alias")` call into a projection field when FIR has erased the call receiver.
     */
    private fun FirFunctionCall.toAliasCallProjectionField(): KronosProjectionField? {
        if (calleeReference.name.asString() != SelectAliasFunctionName) return null
        val alias = argumentList.arguments.firstOrNull()?.stringLiteralValue() ?: return null
        if (!GeneratedProjectionFieldIdentifierRegex.matches(alias)) return null
        return KronosProjectionField(
            name = Name.identifier(alias),
            type = session.builtinTypes.stringType.coneType,
            source = source,
            sourceName = Name.identifier(alias),
            signature = "alias:$alias"
        )
    }

    /**
     * Converts a resolved alias literal from `as_()` into a projection field when FIR loses the receiver call.
     */
    private fun FirStatement.toAliasLiteralProjectionField(): KronosProjectionField? {
        val alias = stringLiteralValue() ?: return null
        if (!GeneratedProjectionFieldIdentifierRegex.matches(alias)) return null
        return KronosProjectionField(
            name = Name.identifier(alias),
            type = session.builtinTypes.stringType.coneType,
            source = source,
            sourceName = Name.identifier(alias),
            signature = "aliasLiteral:$alias"
        )
    }

    /**
     * Returns a constant string value when the expression is a resolved string literal.
     */
    private fun FirStatement.stringLiteralValue(): String? {
        val wrapped = this as? FirWrappedExpression
        if (wrapped != null) return wrapped.expression.stringLiteralValue()

        val literal = this as? FirLiteralExpression ?: return null
        return literal.value as? String
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

    private fun FirBlock.lastExpression(): FirStatement? = statements.lastOrNull()

    /**
     * Rewrites query return types to the generated projection class or list of it.
     */
    private fun refineQueryReturnType(symbol: FirNamedFunctionSymbol, model: KronosProjectionModel): ConeKotlinType? {
        return when (symbol.callableId.callableName.asString()) {
            QueryListFunctionName -> listType(model)
            QueryOneFunctionName -> projectionType(model, isNullable = false)
            QueryOneOrNullFunctionName -> projectionType(model, isNullable = true)
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
        )
        return model
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
     * Builds the generated Context type used by where/having/orderBy after select.
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
}
