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

import com.kotlinorm.compiler.utils.DslCollectionFunctionNames
import com.kotlinorm.compiler.utils.CascadeAnnotationClassId
import com.kotlinorm.compiler.utils.IgnoreAnnotationClassId
import com.kotlinorm.compiler.utils.KPojoClassId
import com.kotlinorm.compiler.utils.SerializeAnnotationClassId
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.name.Name

internal fun FirBlock.lastExpression(): FirStatement? {
    val statement = statements.lastOrNull() ?: return null
    return (statement as? FirReturnExpression)?.result ?: statement
}

internal fun FirStatement.projectionStatementOrNull(): FirStatement? {
    var statement = this
    while (true) {
        statement = when (statement) {
            is FirReturnExpression -> statement.result
            is FirBlock -> statement.lastExpression() ?: return null
            is FirWrappedExpression -> statement.expression
            else -> return statement
        }
    }
}

internal fun FirStatement.projectionItems(): List<FirStatement> {
    val statement = projectionStatementOrNull() ?: return emptyList()
    statement.projectionCollectionItemsOrNull()?.let { return it }
    return listOf(statement)
}

internal fun FirStatement.collectionLiteralItemsOrNull(): List<FirStatement>? {
    val statement = projectionStatementOrNull() ?: return null
    return statement.projectionCollectionItemsOrNull()
}

private fun FirStatement.projectionCollectionItemsOrNull(): List<FirStatement>? =
    when (this) {
        is FirCollectionLiteral -> argumentList.arguments
        is FirFunctionCall -> {
            if (calleeReference.name.asString() !in DslCollectionFunctionNames) return null
            val arguments = argumentList.arguments
            arguments.singleVarargArgumentsOrNull() ?: arguments
        }
        else -> null
    }

internal fun FirStatement.stringLiteralValue(): String? {
    val literal = projectionStatementOrNull() as? FirLiteralExpression ?: return null
    return literal.value as? String
}

private fun List<FirStatement>.singleVarargArgumentsOrNull(): List<FirStatement>? =
    (singleOrNull() as? FirVarargArgumentsExpression)?.arguments

private val ImplicitItName = Name.identifier("it")

internal data class ProjectionSourceValueAccessors(
    val names: Set<Name>,
    val symbols: Set<FirValueParameterSymbol>
)

internal fun List<FirValueParameter>.projectionSourceValueAccessors(): ProjectionSourceValueAccessors {
    val names = mapTo(linkedSetOf()) { it.name }.apply {
        if (isEmpty()) add(ImplicitItName)
    }
    val symbols = mapTo(linkedSetOf()) { it.symbol }
    return ProjectionSourceValueAccessors(names, symbols)
}

internal fun ConeKotlinType?.hasSameClassIdAs(other: ConeKotlinType): Boolean {
    val thisClass = this as? ConeClassLikeType ?: return false
    val otherClass = other as? ConeClassLikeType ?: return false
    return thisClass.lookupTag.classId == otherClass.lookupTag.classId
}

@OptIn(UnresolvedExpressionTypeAccess::class, SymbolInternals::class)
internal fun FirPropertyAccessExpression.resolvedProjectionAccessType(): ConeKotlinType? {
    coneTypeOrNull?.let { return it }
    val symbol = (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirVariableSymbol<*>
    return (symbol?.fir?.returnTypeRef as? FirResolvedTypeRef)?.coneType
}

internal fun FirPropertyAccessExpression.isProjectionSourceValueParameter(
    sourceValues: ProjectionSourceValueAccessors
): Boolean {
    val symbol = (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
    if (symbol == null) return calleeReference.name in sourceValues.names
    return symbol is FirValueParameterSymbol &&
        (sourceValues.symbols.isEmpty() || symbol in sourceValues.symbols)
}

internal fun FirPropertyAccessExpression.isProjectionSourceValueAccess(
    sourceType: ConeKotlinType,
    sourceValues: ProjectionSourceValueAccessors,
    resolvedType: ConeKotlinType?
): Boolean {
    if (!isProjectionSourceValueParameter(sourceValues)) return false
    return (resolvedType ?: resolvedProjectionAccessType())?.hasSameClassIdAs(sourceType) != false
}

internal fun FirStatement.excludedProjectionSourceFieldNames(sourceFieldNames: Set<String>): Set<String> {
    val statement = projectionStatementOrNull() ?: return emptySet()
    statement.collectionLiteralItemsOrNull()?.let { items ->
        return items.flatMapTo(linkedSetOf()) { it.excludedProjectionSourceFieldNames(sourceFieldNames) }
    }
    val propertyAccess = statement as? FirPropertyAccessExpression ?: return emptySet()
    val name = propertyAccess.calleeReference.name.asString()
    return if (name in sourceFieldNames) setOf(name) else emptySet()
}

internal fun FirFunctionCall.sourceMinusExcludedProjectionFieldNames(
    sourceType: ConeKotlinType,
    sourceFieldNames: Set<String>,
    sourceValues: ProjectionSourceValueAccessors,
    resolvedType: (FirStatement) -> ConeKotlinType?
): Set<String>? {
    if (calleeReference.name.asString() != "minus") return null
    val receiver = sourceMinusReceiver()?.projectionStatementOrNull() ?: return null
    val excludedNames = when (receiver) {
        is FirPropertyAccessExpression -> {
            if (!receiver.isProjectionSourceValueAccess(sourceType, sourceValues, resolvedType(receiver))) {
                return null
            }
            linkedSetOf<String>()
        }
        is FirFunctionCall -> receiver.sourceMinusExcludedProjectionFieldNames(
            sourceType,
            sourceFieldNames,
            sourceValues,
            resolvedType
        )?.toCollection(linkedSetOf()) ?: return null
        else -> return null
    }
    argumentList.arguments.flatMapTo(excludedNames) { argument ->
        argument.excludedProjectionSourceFieldNames(sourceFieldNames)
    }
    return excludedNames
}

private fun FirFunctionCall.sourceMinusReceiver(): FirStatement? =
    listOfNotNull(explicitReceiver, extensionReceiver, dispatchReceiver)
        .filterIsInstance<FirStatement>()
        .firstOrNull()

internal fun FirProperty.isKronosColumn(session: FirSession): Boolean {
    if (hasAnnotation(IgnoreAnnotationClassId, session)) return false
    if (hasAnnotation(CascadeAnnotationClassId, session)) return false
    if (hasAnnotation(SerializeAnnotationClassId, session)) return true
    val propertyType = (returnTypeRef as? FirResolvedTypeRef)?.coneType ?: return false
    if (propertyType.isKPojoLikeType(session)) return false
    return propertyType.typeArguments.none { projection ->
        (projection as? ConeKotlinTypeProjection)?.type?.isKPojoLikeType(session) == true
    }
}

@OptIn(SymbolInternals::class)
internal fun ConeKotlinType.isKPojoLikeType(session: FirSession): Boolean {
    val classLike = this as? ConeClassLikeType ?: return false
    if (classLike.lookupTag.classId == KPojoClassId) return true
    val symbol = classLike.toClassSymbol(session) ?: return false
    return symbol.fir.superTypeRefs
        .mapNotNull { ref -> (ref as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType }
        .any { superType -> superType.lookupTag.classId == KPojoClassId }
}
