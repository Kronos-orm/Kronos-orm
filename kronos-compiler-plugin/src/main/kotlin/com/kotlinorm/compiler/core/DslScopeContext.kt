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

package com.kotlinorm.compiler.core

import com.kotlinorm.compiler.utils.extensionReceiver
import com.kotlinorm.compiler.utils.extensionReceiverArgument
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Scope functions return Kotlin values by default. Only a call on a DSL callback's
 * result path delegates collection to another callback with the same DSL receiver.
 * Arguments, variable initializers, operator operands, loop bodies and local-function
 * returns are value boundaries; their scope callbacks must not collect DSL results.
 */
internal class DslScopeContext {
    private val valueCallbacks = mutableSetOf<IrFunction>()
    private val selectorCalls = mutableSetOf<IrCall>()

    fun observeCall(call: IrCall) {
        val result = call.standardScopeResult() ?: return
        // apply/also/takeIf return their original receiver, so their callback's last
        // expression is never the enclosing DSL selector. let returns a callback
        // result but has a value argument rather than an extension receiver.
        if (result == ScopeResult.RECEIVER || call !in selectorCalls || !call.hasReceiverCallback()) {
            call.arguments.filterIsInstance<IrFunctionExpression>().mapTo(valueCallbacks) { it.function }
        }
    }

    fun isValueCallback(function: IrFunction): Boolean = function in valueCallbacks

    fun registerSelector(function: IrFunction) {
        val body = function.body ?: return
        val result = when (body) {
            is IrBlockBody -> body.statements.lastOrNull() as? IrExpression
            is IrExpressionBody -> body.expression
            else -> null
        }
        selectorResultLeaves(result)
            .filterIsInstance<IrCall>().forEach { selectorCalls += it }
        body.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)
            override fun visitFunction(declaration: IrFunction) = Unit
            override fun visitReturn(expression: IrReturn) {
                if (expression.returnTargetSymbol == function.symbol) {
                    selectorResultLeaves(expression.value).filterIsInstance<IrCall>().forEach { selectorCalls += it }
                }
                expression.acceptChildrenVoid(this)
            }
        })
    }
}

/** Branches and statement blocks carry results; operators such as Elvis and && consume them. */
private fun selectorResultLeaves(expression: IrExpression?): Sequence<IrExpression> = when (expression) {
    is IrReturn -> selectorResultLeaves(expression.value)
    is IrTypeOperatorCall -> if (expression.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
        selectorResultLeaves(expression.argument)
    } else sequenceOf(expression)
    is IrWhen -> if (expression.isResultBranching()) expression.branches.asSequence()
        .flatMap { selectorResultLeaves(it.result) } else sequenceOf(expression)
    is IrContainerExpression -> selectorResultLeaves(expression.statements.lastOrNull() as? IrExpression)
    is IrTry -> selectorResultLeaves(expression.tryResult) +
        expression.catches.asSequence().flatMap { selectorResultLeaves(it.result) }
    null -> emptySequence()
    else -> sequenceOf(expression)
}

private fun IrWhen.isResultBranching(): Boolean =
    origin !in setOf(IrStatementOrigin.ELVIS, IrStatementOrigin.ANDAND, IrStatementOrigin.OROR)

private enum class ScopeResult { CALLBACK, RECEIVER }

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrCall.standardScopeResult(): ScopeResult? = when (symbol.owner.fqNameWhenAvailable?.asString()) {
    "kotlin.run", "kotlin.with", "kotlin.let" -> ScopeResult.CALLBACK
    "kotlin.apply", "kotlin.also", "kotlin.takeIf", "kotlin.takeUnless" -> ScopeResult.RECEIVER
    else -> null
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrCall.hasReceiverCallback(): Boolean = arguments.filterIsInstance<IrFunctionExpression>().any {
    it.function.parameters.extensionReceiver != null
}

/** Receiver-returning scopes on the DSL context are ordinary side effects, even at the tail. */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrExpression.isReturningDslReceiverScope(function: IrFunction): Boolean {
    val call = this as? IrCall ?: return false
    val receiver = function.parameters.extensionReceiver?.type?.classFqName ?: return false
    return call.standardScopeResult() == ScopeResult.RECEIVER &&
        call.extensionReceiverArgument?.type?.classFqName == receiver
}

/** These calls execute their own selector callback; collecting their neutral return would collect twice. */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrExpression.isDelegatingDslScopeCall(function: IrFunction): Boolean {
    val call = this as? IrCall ?: return false
    val receiver = function.parameters.extensionReceiver?.type?.classFqName ?: return false
    return call.standardScopeResult() == ScopeResult.CALLBACK && call.arguments.filterIsInstance<IrFunctionExpression>().any {
        it.function.parameters.extensionReceiver?.type?.classFqName == receiver
    }
}

/**
 * A conditional result containing a delegated scope must execute the selected branch,
 * including its side effects. Turn its result leaves into returns so every collector
 * handles both delegated and direct fields through the same ordinary return contract.
 */
internal fun normalizeDelegatingSelectorReturns(function: IrFunction, builder: DeclarationIrBuilder) {
    fun lower(value: IrExpression): IrExpression = when (value) {
        is IrWhen -> if (value.isResultBranching()) value.apply {
            type = builder.context.irBuiltIns.nothingType
            branches.forEach { it.result = lower(it.result) }
        } else builder.irReturn(value)
        is IrContainerExpression -> {
            val tail = value.statements.lastOrNull() as? IrExpression
            if (tail == null) builder.irReturn(value) else value.apply {
                statements[statements.lastIndex] = lower(tail)
                type = builder.context.irBuiltIns.nothingType
            }
        }
        is IrTypeOperatorCall -> if (value.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
            lower(value.argument)
        } else builder.irReturn(value)
        is IrTry -> value.apply {
            type = builder.context.irBuiltIns.nothingType
            tryResult = lower(tryResult)
            catches.forEach { it.result = lower(it.result) }
        }
        else -> builder.irReturn(value)
    }
    function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitFunction(declaration: IrFunction) = declaration
        override fun visitReturn(expression: IrReturn): IrExpression {
            if (expression.returnTargetSymbol != function.symbol || expression.value.isDelegatingDslScopeCall(function)) {
                return expression
            }
            return if (selectorResultLeaves(expression.value).any { it.isDelegatingDslScopeCall(function) }) {
                lower(expression.value)
            } else expression
        }
    })
}
