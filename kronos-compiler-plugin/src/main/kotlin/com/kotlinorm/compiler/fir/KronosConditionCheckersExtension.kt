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

import com.kotlinorm.compiler.utils.JoinPackageFqName
import com.kotlinorm.compiler.utils.KTableForConditionClassId
import com.kotlinorm.compiler.utils.KTableForInsertSelectClassId
import com.kotlinorm.compiler.utils.KTableForReferenceClassId
import com.kotlinorm.compiler.utils.KTableForSelectClassId
import com.kotlinorm.compiler.utils.KTableForSetClassId
import com.kotlinorm.compiler.utils.KTableForSortClassId
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * Registers condition diagnostics that must be visible in FIR and the IDE.
 */
class KronosConditionCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(KronosConditionSourceChecker)
    }
}

private object KronosConditionSourceChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private val ValuePropertyName = Name.identifier("value")
    private val DynamicGateFunctionNames = setOf(
        Name.identifier("takeIf"),
        Name.identifier("takeUnless"),
    )
    private val SourceDslClassIds = setOf(
        KTableForConditionClassId,
        KTableForInsertSelectClassId,
        KTableForReferenceClassId,
        KTableForSelectClassId,
        KTableForSetClassId,
        KTableForSortClassId,
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (!expression.isOrmConditionCall()) return
        expression.argumentList.arguments
            .filterIsInstance<FirAnonymousFunctionExpression>()
            .map { it.anonymousFunction }
            .filter { it.isConditionLambda() }
            .forEach { lambda -> checkConditionLambda(expression, lambda) }
    }

    private fun FirFunctionCall.isOrmConditionCall(): Boolean {
        val callableId = ((calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol)
            ?.callableId
            ?: return false
        return callableId.packageName.asString().startsWith("com.kotlinorm.orm.")
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkConditionLambda(call: FirFunctionCall, lambda: FirAnonymousFunction) {
        val sourceSymbols = linkedSetOf<FirVariableSymbol<*>>()
        sourceSymbols += lambda.valueParameters.map { it.symbol }
        sourceSymbols += context.containingDeclarations
            .asSequence()
            .filterIsInstance<FirAnonymousFunctionSymbol>()
            .map { it.fir }
            .filter { it.isSourceDslLambda() }
            .flatMap { it.valueParameters }
            .map { it.symbol }
            .toList()
        sourceSymbols += call.directJoinSourceSymbols()

        lambda.body?.accept(
            ConditionSourceVisitor(context.session, sourceSymbols) { access ->
                reporter.reportOn(
                    access.source,
                    KronosConditionDiagnostics.UNREGISTERED_CONDITION_SOURCE
                )
            }
        )
    }

    private fun FirAnonymousFunction.isConditionLambda(): Boolean =
        receiverClassId() == KTableForConditionClassId

    private fun FirAnonymousFunction.isSourceDslLambda(): Boolean {
        val receiverClassId = receiverClassId() ?: return false
        return receiverClassId in SourceDslClassIds || receiverClassId.isSelectFromClassId()
    }

    private fun FirAnonymousFunction.receiverClassId(): ClassId? =
        ((receiverParameter?.typeRef as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType)?.lookupTag?.classId

    private fun ClassId.isSelectFromClassId(): Boolean =
        packageFqName == JoinPackageFqName && relativeClassName.asString().startsWith("SelectFrom")

    context(context: CheckerContext)
    private fun FirFunctionCall.directJoinSourceSymbols(): Set<FirVariableSymbol<*>> {
        val callableId = ((calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol)
            ?.callableId
            ?: return emptySet()
        if (callableId.classId?.isSelectFromClassId() != true || !callableId.callableName.asString().endsWith("Join")) {
            return emptySet()
        }
        return argumentList.arguments
            .asSequence()
            .filter { it.safeResolvedType()?.isKPojoLikeType(context.session) == true }
            .mapNotNull { it.rootVariableSymbol() }
            .toSet()
    }

    private class ConditionSourceVisitor(
        private val session: FirSession,
        private val sourceSymbols: Set<FirVariableSymbol<*>>,
        private val report: (FirPropertyAccessExpression) -> Unit,
    ) : FirDefaultVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression) {
            if (anonymousFunctionExpression.anonymousFunction.isSourceDslLambda()) return
            anonymousFunctionExpression.acceptChildren(this)
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall) {
            if (!functionCall.isConditionDynamicGate()) {
                functionCall.acceptChildren(this)
                return
            }

            listOfNotNull(functionCall.explicitReceiver, functionCall.extensionReceiver)
                .distinct()
                .forEach { receiver -> receiver.accept(this) }
        }

        override fun visitWhenExpression(whenExpression: FirWhenExpression) {
            whenExpression.branches.forEach { branch -> branch.result.accept(this) }
        }

        @OptIn(SymbolInternals::class)
        override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
            if (propertyAccessExpression.isConditionValueAccess()) return

            val property = (propertyAccessExpression.calleeReference as? FirResolvedNamedReference)
                ?.resolvedSymbol as? FirPropertySymbol
            val ownerType = property?.fir?.dispatchReceiverType
            if (ownerType?.isKPojoLikeType(session) == true) {
                val root = propertyAccessExpression.receiverExpression()?.rootVariableSymbol()
                if (root == null || root !in sourceSymbols) {
                    report(propertyAccessExpression)
                    return
                }
            }

            propertyAccessExpression.acceptChildren(this)
        }

        private fun FirPropertyAccessExpression.isConditionValueAccess(): Boolean {
            val property = (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirPropertySymbol
                ?: return false
            return property.callableId?.classId == KTableForConditionClassId &&
                property.callableId!!.callableName == ValuePropertyName
        }

        private fun FirFunctionCall.isConditionDynamicGate(): Boolean {
            val callableId = ((calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol)
                ?.callableId
                ?: return false
            return callableId.classId == KTableForConditionClassId &&
                callableId.callableName in DynamicGateFunctionNames
        }
    }
}

private fun FirExpression.receiverExpression(): FirExpression? = when (this) {
    is FirPropertyAccessExpression -> explicitReceiver ?: extensionReceiver ?: dispatchReceiver
    else -> null
}

private fun FirExpression.safeResolvedType() = runCatching { resolvedType }.getOrNull()

private fun FirExpression.rootVariableSymbol(): FirVariableSymbol<*>? = when (this) {
    is FirPropertyAccessExpression -> receiverExpression()?.rootVariableSymbol()
        ?: ((calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirVariableSymbol<*>)
    is FirSmartCastExpression -> originalExpression.rootVariableSymbol()
    is FirWrappedExpression -> expression.rootVariableSymbol()
    is FirCheckedSafeCallSubject -> originalReceiverRef.value.rootVariableSymbol()
    else -> null
}
