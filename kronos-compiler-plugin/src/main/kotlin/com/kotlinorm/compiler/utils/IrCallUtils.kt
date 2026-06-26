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

package com.kotlinorm.compiler.utils

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.copyTo

/**
 * Sets the dispatch or extension receiver parameter of a function.
 * Only supports IrParameterKind.DispatchReceiver and IrParameterKind.ExtensionReceiver.
 */
operator fun IrFunction.set(paramKind: IrParameterKind, param: IrValueParameter?) {
    if (param == null) throw IllegalArgumentException("Parameter must not be null")
    val pos = when (paramKind) {
        IrParameterKind.DispatchReceiver -> 0
        IrParameterKind.ExtensionReceiver -> 1
        else -> throw IllegalArgumentException("Unsupported parameter kind: $paramKind")
    }
    parameters = parameters.toMutableList().apply {
        set(pos, param.copyTo(this@set, kind = paramKind))
    }
}

/**
 * Filters and returns the regular value parameters from a list of IrValueParameters.
 */
val List<IrValueParameter>.valueParameters
    get() = filter { it.kind == IrParameterKind.Regular }

/**
 * Finds and returns the extension receiver parameter from a list of IrValueParameters.
 */
val List<IrValueParameter>.extensionReceiver
    get() = find { it.kind == IrParameterKind.ExtensionReceiver }

/**
 * Finds and returns the dispatch receiver parameter from a list of IrValueParameters.
 */
val List<IrValueParameter>.dispatchReceiver
    get() = find { it.kind == IrParameterKind.DispatchReceiver }

/**
 * Gets the extension receiver argument of an IrCall without using deprecated API
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrCall.extensionReceiverArgument: IrExpression?
    get() = arguments.getOrNull(
        symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.ExtensionReceiver }
    )

/**
 * Gets the dispatch receiver argument of an IrCall without using deprecated API
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrCall.dispatchReceiverArgument: IrExpression?
    get() = arguments.getOrNull(
        symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.DispatchReceiver }
    )

/**
 * Gets a value argument by index without using deprecated API
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrCall.getValueArgumentSafe(index: Int): IrExpression? {
    val regularParams = symbol.owner.parameters.valueParameters
    if (index >= regularParams.size) return null
    val paramIndex = symbol.owner.parameters.indexOf(regularParams[index])
    return arguments.getOrNull(paramIndex)
}

/**
 * Gets all value arguments without using deprecated API
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrCall.valueArguments: List<IrExpression?>
    get() {
        val regularParams = symbol.owner.parameters.valueParameters
        return regularParams.map { param ->
            val paramIndex = symbol.owner.parameters.indexOf(param)
            arguments.getOrNull(paramIndex)
        }
    }

/**
 * Creates an IR function call using the given function symbol and arguments.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal operator fun IrFunctionSymbol.invoke(
    context: IrBuilderWithScope,
    vararg args: IrExpression?
): IrFunctionAccessExpression {
    return context.irCall(this).apply {
        args.forEachIndexed { i, arg -> arguments[i] = arg }
    }
}
