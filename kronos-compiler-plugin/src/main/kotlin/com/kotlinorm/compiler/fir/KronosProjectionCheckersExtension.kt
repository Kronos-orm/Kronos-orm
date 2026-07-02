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
import com.kotlinorm.compiler.utils.KotlinListOfFunctionName
import com.kotlinorm.compiler.utils.KSelectableClassId
import com.kotlinorm.compiler.utils.PlainAggregateFunctionNames
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
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
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

private object KronosSelectProjectionChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        checkScalarSubqueryLimitUsage(expression)
        checkPredicateSubqueryShape(expression)
        if (!expression.isKronosSelectCall()) return
        checkSelectProjection(expression)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSelectProjection(expression: FirFunctionCall) {
        val lambda = expression.argumentList.arguments.lastOrNull() as? FirAnonymousFunctionExpression ?: return
        val returned = lambda.anonymousFunction.body?.statements?.lastOrNull()?.projectionExpression() ?: return
        val items = returned.projectionItems()
        if (items.isEmpty()) return
        val sourceFieldNames = expression.selectSourceType()?.sourceFieldNames().orEmpty()

        val seenNames = linkedSetOf<String>()
        items.forEach { item ->
            if (item.hasResolutionError()) return@forEach
            when (val result = item.projectionNameOrDiagnostic()) {
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
                    if (result.origin == ProjectionItemOrigin.ExplicitAlias && result.name.asString() in sourceFieldNames) {
                        reporter.reportOn(
                            item.source,
                            KronosProjectionDiagnostics.SELECTED_FIELD_CONFLICTS_WITH_SOURCE
                        )
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
        call.predicateSubqueryCandidates().forEach { candidate ->
            if (candidate.query.hasResolutionError()) return@forEach
            if (candidate.tupleArity == 1) {
                reporter.reportOn(
                    candidate.left.source,
                    KronosProjectionDiagnostics.ROW_VALUE_TUPLE_REQUIRES_MULTIPLE_FIELDS
                )
                return@forEach
            }

            val rightArity = candidate.query.scalarProjectionArity() ?: return@forEach
            if (candidate.leftArity != rightArity) {
                reporter.reportOn(
                    candidate.query.source,
                    KronosProjectionDiagnostics.PREDICATE_SUBQUERY_COLUMN_COUNT_MISMATCH
                )
            }
        }
    }

    context(context: CheckerContext)
    private fun FirFunctionCall.scalarSubqueryCandidates(): List<FirExpression> {
        val name = calleeReference.name
        return when (name) {
            SelectAliasFunctionNameIdentifier -> receiverCandidates().filter { it.isKSelectableExpression() }
            CompareToFunctionName, EqualsFunctionName -> receiverCandidates().filter { it.isKSelectableExpression() } +
                argumentList.arguments.filter { it.isKSelectableExpression() }
            SortAscendingFunctionName, SortDescendingFunctionName -> receiverCandidates().filter { it.isKSelectableExpression() }
            else -> emptyList()
        }
    }

    context(context: CheckerContext)
    private fun FirFunctionCall.predicateSubqueryCandidates(): List<PredicateSubqueryCandidate> {
        val name = calleeReference.name
        if (name == ContainsFunctionName) {
            val query = receiverCandidates().firstOrNull { it.isKSelectableExpression() } ?: return emptyList()
            val left = argumentList.arguments.firstOrNull() ?: return emptyList()
            val tupleArity = left.rowValueTupleArity()
            return listOf(
                PredicateSubqueryCandidate(
                    left = left,
                    query = query,
                    leftArity = tupleArity ?: 1,
                    tupleArity = tupleArity,
                )
            )
        }

        if (name in SubqueryQuantifierFunctionNames) {
            val query = argumentList.arguments.firstOrNull { it.isKSelectableExpression() } ?: return emptyList()
            return listOf(
                PredicateSubqueryCandidate(
                    left = this,
                    query = query,
                    leftArity = 1,
                    tupleArity = null,
                )
            )
        }

        return emptyList()
    }

    private fun FirFunctionCall.receiverCandidates(): List<FirExpression> {
        return listOfNotNull(explicitReceiver, dispatchReceiver, extensionReceiver).distinct()
    }

    private fun FirExpression.rowValueTupleArity(): Int? {
        val wrapped = (this as? FirWrappedExpression)?.expression
        if (wrapped != null) return wrapped.rowValueTupleArity()

        val literal = this as? FirCollectionLiteral
        if (literal != null) return literal.argumentList.arguments.size

        val vararg = this as? FirVarargArgumentsExpression
        if (vararg != null) return vararg.arguments.size

        val call = this as? FirFunctionCall ?: return null
        if (call.calleeReference.name == KotlinListOfFunctionName) {
            val listVararg = call.argumentList.arguments.singleOrNull() as? FirVarargArgumentsExpression
            return (listVararg?.arguments ?: call.argumentList.arguments).size
        }

        return null
    }

    context(context: CheckerContext)
    private fun FirExpression.isKSelectableExpression(): Boolean {
        val type = safeResolvedType() as? ConeClassLikeType ?: return false
        return type.lookupTag.classId == SelectClauseClassId || type.lookupTag.classId == KSelectableClassId
    }

    context(context: CheckerContext)
    private fun FirExpression.hasLimitOneInChain(): Boolean {
        val wrapped = (this as? FirWrappedExpression)?.expression
        if (wrapped != null) return wrapped.hasLimitOneInChain()

        val call = this as? FirFunctionCall ?: return false
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
        val selectedTypeArity = (safeResolvedType() as? ConeClassLikeType)?.selectedFieldCount()
        if (selectedTypeArity != null) return selectedTypeArity

        val selectCall = findSelectCallInChain() ?: return null
        return selectCall.selectProjectionItems()?.size
    }

    context(context: CheckerContext)
    private fun ConeClassLikeType.selectedFieldCount(): Int? {
        val selectedArgumentIndex = when (lookupTag.classId) {
            SelectClauseClassId -> 1
            KSelectableClassId -> 0
            else -> return null
        }
        val selectedArgument = typeArguments.getOrNull(selectedArgumentIndex) as? ConeKotlinTypeProjection ?: return null
        val selectedType = selectedArgument.type
        val selectedClassType = selectedType as? ConeClassLikeType ?: return null
        val projectionModel = KronosProjectionRegistry.find(context.session, selectedClassType.lookupTag.classId)
        return projectionModel?.fields?.size ?: selectedType.sourceFieldNames().size.takeIf { it > 0 }
    }

    context(context: CheckerContext)
    private fun FirExpression.isAggregateScalarWithoutGroupBy(): Boolean {
        if (hasCallInReceiverChain(SelectGroupByFunctionName)) return false
        val selectCall = findSelectCallInChain() ?: return false
        val item = selectCall.selectProjectionItems()?.singleOrNull() ?: return false
        return item.containsPlainAggregateCall()
    }

    private fun FirExpression.findSelectCallInChain(): FirFunctionCall? {
        val wrapped = (this as? FirWrappedExpression)?.expression
        if (wrapped != null) return wrapped.findSelectCallInChain()

        val call = this as? FirFunctionCall ?: return null
        if (call.calleeReference.name == SelectFunctionName) return call
        return call.receiverCandidates().firstNotNullOfOrNull { it.findSelectCallInChain() }
    }

    private fun FirExpression.hasCallInReceiverChain(name: Name): Boolean {
        val wrapped = (this as? FirWrappedExpression)?.expression
        if (wrapped != null) return wrapped.hasCallInReceiverChain(name)

        val call = this as? FirFunctionCall ?: return false
        return call.calleeReference.name == name || call.receiverCandidates().any { it.hasCallInReceiverChain(name) }
    }

    private fun FirFunctionCall.selectProjectionItems(): List<FirStatement>? {
        val lambda = argumentList.arguments.lastOrNull() as? FirAnonymousFunctionExpression ?: return null
        val returned = lambda.anonymousFunction.body?.statements?.lastOrNull()?.projectionExpression() ?: return null
        return returned.projectionItems()
    }

    private fun FirStatement.containsPlainAggregateCall(): Boolean {
        val returned = this as? FirReturnExpression
        if (returned != null) return returned.result.containsPlainAggregateCall()

        val block = this as? FirBlock
        if (block != null) return block.lastExpression()?.containsPlainAggregateCall() == true

        val wrapped = this as? FirWrappedExpression
        if (wrapped != null) return wrapped.expression.containsPlainAggregateCall()

        val literal = this as? FirCollectionLiteral
        if (literal != null) return literal.argumentList.arguments.any { it.containsPlainAggregateCall() }

        val vararg = this as? FirVarargArgumentsExpression
        if (vararg != null) return vararg.arguments.any { it.containsPlainAggregateCall() }

        val call = this as? FirFunctionCall
        if (call != null) {
            val functionName = call.calleeReference.name.asString()
            if (functionName in PlainAggregateFunctionNames) return true
            return call.receiverCandidates().any { it.containsPlainAggregateCall() } ||
                call.argumentList.arguments.any { it.containsPlainAggregateCall() }
        }

        return false
    }

    private fun FirStatement.hasResolutionError(): Boolean {
        val returned = this as? FirReturnExpression
        if (returned != null) return returned.result.hasResolutionError()

        val block = this as? FirBlock
        if (block != null) return block.lastExpression()?.hasResolutionError() == true

        val wrapped = this as? FirWrappedExpression
        if (wrapped != null) return wrapped.expression.hasResolutionError()

        if (this is FirErrorExpression || this is FirQualifiedErrorAccessExpression) return true

        val literal = this as? FirCollectionLiteral
        if (literal != null) return literal.argumentList.arguments.any { it.hasResolutionError() }

        val vararg = this as? FirVarargArgumentsExpression
        if (vararg != null) return vararg.arguments.any { it.hasResolutionError() }

        val qualified = this as? FirQualifiedAccessExpression
        if (qualified != null) {
            if (qualified.calleeReference is FirErrorNamedReference) return true
            if (qualified.calleeReference is FirResolvedErrorReference) return true
            if (qualified.explicitReceiver?.hasResolutionError() == true) return true
            if (qualified.dispatchReceiver?.hasResolutionError() == true) return true
            if (qualified.extensionReceiver?.hasResolutionError() == true) return true
        }

        val call = this as? FirFunctionCall
        if (call != null) {
            if (call.argumentList.arguments.any { it.hasResolutionError() }) return true
        }

        return false
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
        val selectedArgumentIndex = when (receiverClassType.lookupTag.classId) {
            SelectClauseClassId -> 1
            KSelectableClassId -> 0
            else -> return receiverType
        }
        val selectedArgument = receiverClassType.typeArguments.getOrNull(selectedArgumentIndex) as? ConeKotlinTypeProjection
        return selectedArgument?.type ?: receiverType
    }

    private fun FirExpression.safeResolvedType(): ConeKotlinType? {
        return runCatching { resolvedType }.getOrNull()
    }

    context(context: CheckerContext)
    private fun ConeKotlinType.sourceFieldNames(): Set<String> {
        val classSymbol = toClassSymbol(context.session) ?: return emptySet()
        val names = linkedSetOf<String>()
        classSymbol.processAllDeclarations(context.session, FirResolvePhase.STATUS) { symbol ->
            val property = symbol as? FirPropertySymbol ?: return@processAllDeclarations
            names += property.name.asString()
        }
        return names
    }

    private fun FirStatement.projectionItems(): List<FirStatement> {
        val returned = this as? FirReturnExpression
        if (returned != null) return returned.result.projectionItems()

        val block = this as? FirBlock
        if (block != null) return block.lastExpression()?.projectionItems().orEmpty()

        val wrapped = this as? FirWrappedExpression
        if (wrapped != null) return wrapped.expression.projectionItems()

        val literal = this as? FirCollectionLiteral
        if (literal != null) {
            return literal.argumentList.arguments
        }

        val call = this as? FirFunctionCall
        if (call?.calleeReference?.name == KotlinListOfFunctionName) {
            val listVararg = call.argumentList.arguments.singleOrNull() as? FirVarargArgumentsExpression
            return listVararg?.arguments ?: call.argumentList.arguments
        }

        val vararg = call?.argumentList?.arguments?.singleOrNull() as? FirVarargArgumentsExpression
        if (vararg != null) {
            return vararg.arguments
        }

        return listOf(this)
    }

    private fun FirStatement.projectionExpression(): FirStatement {
        val returned = this as? FirReturnExpression
        if (returned != null) return returned.result
        val block = this as? FirBlock
        if (block != null) return block.lastExpression() ?: this
        return this
    }

    private fun FirStatement.projectionNameOrDiagnostic(): ProjectionItemResult {
        val returned = this as? FirReturnExpression
        if (returned != null) return returned.result.projectionNameOrDiagnostic()

        val block = this as? FirBlock
        if (block != null) return block.lastExpression()?.projectionNameOrDiagnostic()
            ?: ProjectionItemResult.RequiresAlias

        val wrapped = this as? FirWrappedExpression
        if (wrapped != null) return wrapped.expression.projectionNameOrDiagnostic()

        val propertyAccess = this as? FirPropertyAccessExpression
        if (propertyAccess != null) {
            return ProjectionItemResult.Named(propertyAccess.calleeReference.name, ProjectionItemOrigin.SourceField)
        }

        val call = this as? FirFunctionCall
        if (call != null && call.calleeReference.name.asString() == SelectAliasFunctionName) {
            val alias = call.argumentList.arguments.firstOrNull()?.stringLiteralValue()
            if (alias != null && GeneratedProjectionFieldIdentifierRegex.matches(alias)) {
                return ProjectionItemResult.Named(Name.identifier(alias), ProjectionItemOrigin.ExplicitAlias)
            }
        }

        val aliasLiteral = stringLiteralValue()
        if (aliasLiteral != null && GeneratedProjectionFieldIdentifierRegex.matches(aliasLiteral)) {
            return ProjectionItemResult.Named(Name.identifier(aliasLiteral), ProjectionItemOrigin.ExplicitAlias)
        }

        return ProjectionItemResult.RequiresAlias
    }

    private fun FirStatement.stringLiteralValue(): String? {
        val wrapped = this as? FirWrappedExpression
        if (wrapped != null) return wrapped.expression.stringLiteralValue()

        val literal = this as? FirLiteralExpression ?: return null
        return literal.value as? String
    }

    private fun FirStatement.intLiteralValue(): Int? {
        val wrapped = this as? FirWrappedExpression
        if (wrapped != null) return wrapped.expression.intLiteralValue()

        val literal = this as? FirLiteralExpression ?: return null
        return when (val value = literal.value) {
            is Int -> value
            is Number -> value.toInt()
            else -> null
        }
    }

    private fun FirBlock.lastExpression(): FirStatement? {
        val statement = statements.lastOrNull() ?: return null
        return (statement as? FirReturnExpression)?.result ?: statement
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
