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

import com.kotlinorm.compiler.utils.CompareToFunctionName
import com.kotlinorm.compiler.utils.ContainsFunctionName
import com.kotlinorm.compiler.utils.EqualsFunctionName
import com.kotlinorm.compiler.utils.GeneratedProjectionFieldIdentifierRegex
import com.kotlinorm.compiler.utils.InsertClauseClassId
import com.kotlinorm.compiler.utils.InsertFunctionName
import com.kotlinorm.compiler.utils.KSelectableClassId
import com.kotlinorm.compiler.utils.PlainAggregateFunctionNames
import com.kotlinorm.compiler.utils.PrimaryKeyAnnotationClassId
import com.kotlinorm.compiler.utils.SelectAliasFunctionName
import com.kotlinorm.compiler.utils.SelectAliasFunctionNameIdentifier
import com.kotlinorm.compiler.utils.SelectClauseClassId
import com.kotlinorm.compiler.utils.SelectFunctionName
import com.kotlinorm.compiler.utils.SelectGroupByFunctionName
import com.kotlinorm.compiler.utils.SelectLimitFunctionName
import com.kotlinorm.compiler.utils.SelectPackageFqName
import com.kotlinorm.compiler.utils.SortAscendingFunctionName
import com.kotlinorm.compiler.utils.SortDescendingFunctionName
import com.kotlinorm.compiler.utils.SubqueryQuantifierFunctionNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getBooleanArgument
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedErrorAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.name.Name

/**
 * Registers FIR checkers for projection DSL constraints that must be visible before IR.
 */
class KronosProjectionCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    /**
     * Installs only expression checkers because projection misuse is expressed through DSL calls.
     */
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(KronosSelectProjectionChecker)
    }
}

@OptIn(SymbolInternals::class)
private object KronosSelectProjectionChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private val PrimaryKeyIdentityArgumentName = Name.identifier("identity")
    private val ScalarSubqueryCandidateFunctionNames = setOf(
        SelectAliasFunctionNameIdentifier,
        CompareToFunctionName,
        EqualsFunctionName,
        SortAscendingFunctionName,
        SortDescendingFunctionName
    )
    private val PredicateSubqueryFunctionNames = SubqueryQuantifierFunctionNames + ContainsFunctionName
    private val ScalarReceiverOnlyFunctionNames = setOf(
        SelectAliasFunctionNameIdentifier,
        SortAscendingFunctionName,
        SortDescendingFunctionName
    )
    private val ScalarReceiverAndArgumentFunctionNames = setOf(CompareToFunctionName, EqualsFunctionName)
    private val SelectedTypeArgumentIndexByClassId = mapOf(
        SelectClauseClassId to 1,
        KSelectableClassId to 0
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val name = expression.calleeReference.name
        when {
            name == InsertFunctionName -> checkInsertSelectValueCount(expression)
            name in ScalarSubqueryCandidateFunctionNames -> checkScalarSubqueryLimitUsage(expression)
            name in PredicateSubqueryFunctionNames -> checkPredicateSubqueryShape(expression)
        }
        if (!expression.isKronosSelectCall()) return
        checkSelectProjection(expression)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSelectProjection(expression: FirFunctionCall) {
        val lambda = expression.argumentList.arguments.lastOrNull() as? FirAnonymousFunctionExpression ?: return
        val returned = lambda.anonymousFunction.body?.lastExpression()?.projectionExpression() ?: return
        val items = returned.projectionItems()
        if (items.isEmpty()) return
        val sourceType = expression.selectSourceType()
        val sourceFieldNames = sourceType?.sourceFieldNames().orEmpty()
        val sourceValues = lambda.anonymousFunction.valueParameters.projectionSourceValueAccessors()

        val seenNames = linkedSetOf<String>()
        items.forEach { item ->
            if (item.hasResolutionError()) return@forEach
            item.projectionNamesOrDiagnostic(sourceType, sourceFieldNames, sourceValues).forEach { result ->
                when (result) {
                    ProjectionItemResult.RequiresAlias -> reporter.reportOn(
                        item.source,
                        KronosProjectionDiagnostics.SELECT_ITEM_REQUIRES_ALIAS
                    )

                    is ProjectionItemResult.Named -> {
                        if (!seenNames.add(result.name.asString())) {
                            reporter.reportOn(
                                item.source,
                                KronosProjectionDiagnostics.DUPLICATE_PROJECTION_FIELD
                            )
                        }
                        if (
                            result.origin == ProjectionItemOrigin.ExplicitAlias &&
                            result.name.asString() in sourceFieldNames
                        ) {
                            reporter.reportOn(
                                item.source,
                                KronosProjectionDiagnostics.SELECTED_FIELD_CONFLICTS_WITH_SOURCE
                            )
                        }
                    }
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkScalarSubqueryLimitUsage(call: FirFunctionCall) {
        call.scalarSubqueryCandidates().forEach { candidate ->
            if (candidate.hasResolutionError()) return@forEach
            if (!candidate.isKSelectableExpression()) return@forEach
            val projectionArity = candidate.scalarProjectionArity()
            if (projectionArity != null && projectionArity != 1) {
                reporter.reportOn(candidate.source, KronosProjectionDiagnostics.SCALAR_SUBQUERY_REQUIRES_SINGLE_COLUMN)
                return@forEach
            }
            if (!candidate.isAggregateScalarWithoutGroupBy() && !candidate.hasLimitOneInChain()) {
                reporter.reportOn(candidate.source, KronosProjectionDiagnostics.SCALAR_SUBQUERY_REQUIRES_LIMIT)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPredicateSubqueryShape(call: FirFunctionCall) {
        val candidate = call.predicateSubqueryCandidate() ?: return
        if (candidate.query.hasResolutionError()) return
        if (candidate.tupleArity == 1) {
            reporter.reportOn(
                candidate.left.source,
                KronosProjectionDiagnostics.ROW_VALUE_TUPLE_REQUIRES_MULTIPLE_FIELDS
            )
            return
        }

        val rightArity = candidate.query.scalarProjectionArity() ?: return
        if (candidate.leftArity != rightArity) {
            reporter.reportOn(
                candidate.query.source,
                KronosProjectionDiagnostics.PREDICATE_SUBQUERY_COLUMN_COUNT_MISMATCH
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInsertSelectValueCount(call: FirFunctionCall) {
        val targetFieldTypes = call.insertTargetFieldTypes() ?: return
        val lambda = call.argumentList.arguments.lastOrNull() as? FirAnonymousFunctionExpression ?: return
        val returned = lambda.anonymousFunction.body?.lastExpression()?.projectionExpression() ?: return
        if (returned.hasResolutionError()) return

        val values = returned.collectionLiteralItemsOrNull() ?: return
        val valueCount = values.size
        if (valueCount != targetFieldTypes.size) {
            reporter.reportOn(returned.source ?: call.source, KronosProjectionDiagnostics.INSERT_SELECT_VALUE_COUNT_MISMATCH)
            return
        }

        values.zip(targetFieldTypes).forEach { (value, targetType) ->
            if (value.isNullLiteral()) return@forEach
            val valueType = value.insertSelectValueType()
            if (valueType == null) return@forEach
            if (!valueType.hasSameClassId(targetType)) {
                reporter.reportOn(value.source ?: returned.source ?: call.source, KronosProjectionDiagnostics.INSERT_SELECT_VALUE_TYPE_MISMATCH)
            }
        }
    }

    context(context: CheckerContext)
    private fun FirFunctionCall.insertTargetFieldTypes(): List<ConeKotlinType>? {
        val returnType = safeResolvedType() as? ConeClassLikeType ?: return null
        if (returnType.lookupTag.classId != InsertClauseClassId) return null

        val targetArgument = returnType.typeArguments.firstOrNull() as? ConeKotlinTypeProjection ?: return null
        return targetArgument.type.targetInsertableFieldTypes()
    }

    context(context: CheckerContext)
    private fun ConeKotlinType.targetInsertableFieldTypes(): List<ConeKotlinType>? {
        val classSymbol = toClassSymbol(context.session) ?: return null
        val fieldTypes = mutableListOf<ConeKotlinType>()
        classSymbol.processAllDeclarations(context.session, FirResolvePhase.STATUS) { symbol ->
            val property = (symbol as? FirPropertySymbol)?.fir ?: return@processAllDeclarations
            if (property.isTargetInsertableColumn()) {
                val propertyType = (property.returnTypeRef as? FirResolvedTypeRef)?.coneType ?: return@processAllDeclarations
                fieldTypes += propertyType
            }
        }
        return fieldTypes
    }

    context(context: CheckerContext)
    private fun FirProperty.isTargetInsertableColumn(): Boolean {
        if (!isKronosColumn(context.session)) return false
        return !isIdentityPrimaryKey()
    }

    context(context: CheckerContext)
    private fun FirProperty.isIdentityPrimaryKey(): Boolean {
        val primaryKey = getAnnotationByClassId(PrimaryKeyAnnotationClassId, context.session) ?: return false
        return primaryKey.getBooleanArgument(PrimaryKeyIdentityArgumentName) == true
    }

    context(context: CheckerContext)
    private fun FirFunctionCall.scalarSubqueryCandidates(): List<FirExpression> {
        val name = calleeReference.name
        if (name in ScalarReceiverOnlyFunctionNames) {
            return receiverCandidates().filter { it.isKSelectableExpression() }
        }
        if (name in ScalarReceiverAndArgumentFunctionNames) {
            return receiverCandidates().filter { it.isKSelectableExpression() } +
                argumentList.arguments.filter { it.isKSelectableExpression() }
        }
        return emptyList()
    }

    context(context: CheckerContext)
    private fun FirFunctionCall.predicateSubqueryCandidate(): PredicateSubqueryCandidate? {
        val name = calleeReference.name
        if (name == ContainsFunctionName) {
            val query = receiverCandidates().firstOrNull { it.isKSelectableExpression() } ?: return null
            val left = argumentList.arguments.firstOrNull() ?: return null
            val tupleArity = left.rowValueTupleArity()
            return PredicateSubqueryCandidate(
                left = left,
                query = query,
                leftArity = tupleArity ?: 1,
                tupleArity = tupleArity,
            )
        }

        val query = argumentList.arguments.firstOrNull { it.isKSelectableExpression() } ?: return null
        return PredicateSubqueryCandidate(
            left = this,
            query = query,
            leftArity = 1,
            tupleArity = null,
        )
    }

    private fun FirFunctionCall.receiverCandidates(): List<FirExpression> {
        return listOfNotNull(explicitReceiver, dispatchReceiver, extensionReceiver).distinct()
    }

    private fun FirExpression.rowValueTupleArity(): Int? {
        return collectionLiteralItemsOrNull()?.size
    }

    context(context: CheckerContext)
    private fun FirExpression.isKSelectableExpression(): Boolean {
        val type = safeResolvedType() as? ConeClassLikeType ?: return false
        return type.lookupTag.classId == SelectClauseClassId || type.lookupTag.classId == KSelectableClassId
    }

    context(context: CheckerContext)
    private fun FirExpression.hasLimitOneInChain(): Boolean {
        val call = projectionStatementOrNull() as? FirFunctionCall ?: return false
        if (call.calleeReference.name == SelectLimitFunctionName &&
            call.receiverCandidates().any { it.isKSelectableExpression() } &&
            call.argumentList.arguments.firstOrNull()?.intLiteralValue() == 1
        ) {
            return true
        }

        return call.receiverCandidates().any { it.hasLimitOneInChain() }
    }

    context(context: CheckerContext)
    private fun FirExpression.scalarProjectionArity(): Int? {
        return (safeResolvedType() as? ConeClassLikeType)?.selectedFieldCount()
    }

    context(context: CheckerContext)
    private fun ConeClassLikeType.selectedFieldCount(): Int? {
        val selectedType = selectedTypeArgumentOrNull() ?: return null
        val selectedClassType = selectedType as? ConeClassLikeType ?: return null
        val projectionModel = KronosProjectionRegistry.find(context.session, selectedClassType.lookupTag.classId)
        return projectionModel?.fields?.size
    }

    private fun ConeClassLikeType.selectedTypeArgumentOrNull(): ConeKotlinType? {
        val selectedArgumentIndex = SelectedTypeArgumentIndexByClassId[lookupTag.classId] ?: return null
        return (typeArguments.getOrNull(selectedArgumentIndex) as? ConeKotlinTypeProjection)?.type
    }

    context(context: CheckerContext)
    private fun FirExpression.isAggregateScalarWithoutGroupBy(): Boolean {
        if (hasCallInReceiverChain(SelectGroupByFunctionName)) return false
        val selectCall = findSelectCallInChain() ?: return false
        val item = selectCall.selectProjectionItems()?.singleOrNull() ?: return false
        return item.containsPlainAggregateCall()
    }

    private fun FirExpression.findSelectCallInChain(): FirFunctionCall? {
        val call = projectionStatementOrNull() as? FirFunctionCall ?: return null
        if (call.calleeReference.name == SelectFunctionName) return call
        return call.receiverCandidates().firstNotNullOfOrNull { it.findSelectCallInChain() }
    }

    private fun FirExpression.hasCallInReceiverChain(name: Name): Boolean {
        val call = projectionStatementOrNull() as? FirFunctionCall ?: return false
        return call.calleeReference.name == name || call.receiverCandidates().any { it.hasCallInReceiverChain(name) }
    }

    private fun FirFunctionCall.selectProjectionItems(): List<FirStatement>? {
        val lambda = argumentList.arguments.lastOrNull() as? FirAnonymousFunctionExpression ?: return null
        val returned = lambda.anonymousFunction.body?.lastExpression()?.projectionExpression() ?: return null
        return returned.projectionItems()
    }

    private fun FirStatement.containsPlainAggregateCall(): Boolean {
        return when (val statement = projectionStatementOrNull()) {
            is FirCollectionLiteral -> statement.argumentList.arguments.any { it.containsPlainAggregateCall() }
            is FirVarargArgumentsExpression -> statement.arguments.any { it.containsPlainAggregateCall() }
            is FirFunctionCall -> {
                val functionName = statement.calleeReference.name.asString()
                functionName in PlainAggregateFunctionNames ||
                    statement.receiverCandidates().any { it.containsPlainAggregateCall() } ||
                    statement.argumentList.arguments.any { it.containsPlainAggregateCall() }
            }
            else -> false
        }
    }

    private fun FirStatement.hasResolutionError(): Boolean {
        val statement = projectionStatementOrNull() ?: return false
        if (statement is FirErrorExpression || statement is FirQualifiedErrorAccessExpression) return true
        val access = statement as? FirQualifiedAccessExpression ?: return false
        return access.calleeReference is FirErrorNamedReference ||
            access.calleeReference is FirResolvedErrorReference
    }

    private fun FirFunctionCall.isKronosSelectCall(): Boolean {
        if (calleeReference.name != SelectFunctionName) return false
        val symbol = (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol
        val callableId = symbol?.callableId ?: return false
        return callableId.packageName == SelectPackageFqName && callableId.callableName == SelectFunctionName
    }

    context(context: CheckerContext)
    private fun FirFunctionCall.selectSourceType(): ConeKotlinType? {
        val receiverType = explicitReceiver?.safeResolvedType() ?: return null
        val receiverClassType = receiverType as? ConeClassLikeType ?: return receiverType
        return receiverClassType.selectedTypeArgumentOrNull() ?: receiverType
    }

    private fun FirStatement.safeResolvedType(): ConeKotlinType? {
        val expression = this as? FirExpression ?: return null
        return runCatching { expression.resolvedType }.getOrNull()
    }

    context(context: CheckerContext)
    private fun FirStatement.insertSelectValueType(): ConeKotlinType? {
        val statement = projectionStatementOrNull() ?: return null
        return when (statement) {
            is FirPropertyAccessExpression,
            is FirLiteralExpression -> statement.safeResolvedType()
            else -> null
        }
    }

    context(context: CheckerContext)
    private fun ConeKotlinType.sourceFieldNames(): Set<String> {
        val classSymbol = toClassSymbol(context.session) ?: return emptySet()
        val names = linkedSetOf<String>()
        classSymbol.processAllDeclarations(context.session, FirResolvePhase.STATUS) { symbol ->
            val property = (symbol as? FirPropertySymbol)?.fir ?: return@processAllDeclarations
            if (property.isKronosColumn(context.session)) names += property.name.asString()
        }
        return names
    }

    private fun FirStatement.projectionExpression(): FirStatement {
        return projectionStatementOrNull() ?: this
    }

    context(context: CheckerContext)
    private fun FirStatement.projectionNamesOrDiagnostic(
        sourceType: ConeKotlinType?,
        sourceFieldNames: Set<String>,
        sourceValues: ProjectionSourceValueAccessors
    ): List<ProjectionItemResult> {
        val statement = projectionStatementOrNull() ?: return listOf(ProjectionItemResult.RequiresAlias)

        val propertyAccess = statement as? FirPropertyAccessExpression
        if (propertyAccess != null) {
            if (
                sourceType != null && propertyAccess.isProjectionSourceValueAccess(
                    sourceType,
                    sourceValues,
                    propertyAccess.safeResolvedType()
                )
            ) {
                return sourceFieldNames.map { fieldName ->
                    ProjectionItemResult.Named(Name.identifier(fieldName), ProjectionItemOrigin.SourceField)
                }
            }
            return listOf(
                ProjectionItemResult.Named(propertyAccess.calleeReference.name, ProjectionItemOrigin.SourceField)
            )
        }

        val call = statement as? FirFunctionCall
        if (call != null && sourceType != null) {
            call.sourceMinusExcludedProjectionFieldNames(
                sourceType,
                sourceFieldNames,
                sourceValues
            ) { sourceMinusStatement -> sourceMinusStatement.safeResolvedType() }?.let { excludedNames ->
                return sourceFieldNames
                    .filterNot { it in excludedNames }
                    .map { fieldName ->
                        ProjectionItemResult.Named(Name.identifier(fieldName), ProjectionItemOrigin.SourceField)
                    }
            }
        }
        if (call != null && call.calleeReference.name.asString() == SelectAliasFunctionName) {
            val alias = call.argumentList.arguments.firstOrNull()?.stringLiteralValue()
            if (alias != null && GeneratedProjectionFieldIdentifierRegex.matches(alias)) {
                return listOf(ProjectionItemResult.Named(Name.identifier(alias), ProjectionItemOrigin.ExplicitAlias))
            }
        }

        val aliasLiteral = stringLiteralValue()
        if (aliasLiteral != null && GeneratedProjectionFieldIdentifierRegex.matches(aliasLiteral)) {
            return listOf(
                ProjectionItemResult.Named(Name.identifier(aliasLiteral), ProjectionItemOrigin.ExplicitAlias)
            )
        }

        return listOf(ProjectionItemResult.RequiresAlias)
    }

    private fun FirStatement.intLiteralValue(): Int? {
        val literal = projectionStatementOrNull() as? FirLiteralExpression ?: return null
        return when (val value = literal.value) {
            is Int -> value
            is Number -> value.toInt()
            else -> null
        }
    }

    private fun FirStatement.isNullLiteral(): Boolean {
        val literal = projectionStatementOrNull() as? FirLiteralExpression ?: return false
        return literal.value == null
    }

    private fun ConeKotlinType.hasSameClassId(other: ConeKotlinType): Boolean {
        val left = this as? ConeClassLikeType ?: return true
        val right = other as? ConeClassLikeType ?: return true
        return left.lookupTag.classId == right.lookupTag.classId
    }

}

private sealed interface ProjectionItemResult {
    data object RequiresAlias : ProjectionItemResult

    data class Named(val name: Name, val origin: ProjectionItemOrigin) : ProjectionItemResult
}

private enum class ProjectionItemOrigin {
    SourceField,
    ExplicitAlias
}

private data class PredicateSubqueryCandidate(
    val left: FirExpression,
    val query: FirExpression,
    val leftArity: Int,
    val tupleArity: Int?,
)
