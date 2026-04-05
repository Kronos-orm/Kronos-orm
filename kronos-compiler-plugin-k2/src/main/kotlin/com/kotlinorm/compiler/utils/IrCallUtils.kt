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

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.getSimpleFunction

/**
 * IrCall utility functions
 *
 * Provides safe access to IrCall properties without using deprecated APIs
 */

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
 * Builds a string concatenation IR expression (left + right)
 *
 * @param left The left string expression
 * @param right The right string expression
 * @return String concatenation IR expression
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
fun buildStringConcat(
    left: IrExpression,
    right: IrExpression
): IrExpression {
    // Get String.plus(Any?) function
    val stringClass = context.referenceClass(StringClassId)
    val plusFunction = stringClass?.getSimpleFunction("plus")
    
    if (plusFunction != null) {
        return builder.irCall(plusFunction).apply {
            dispatchReceiver = left
            arguments[0] = right
        }
    }
    
    // Fallback: just return left if plus function not found
    return left
}
