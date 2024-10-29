/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.compiler.helpers

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.name.FqName

// A helpers class for specifying the receiver of an IR function call
// applyIrCall的辅助类，用于指定IR函数调用的接收器
class Receivers(
    // The dispatch receiver expression
    // 分派接收器表达式
    var dispatchReceiver: IrExpression? = null,
    // The extension receiver expression
    // 扩展接收器表达式
    var extensionReceiver: IrExpression? = null
)

/**
 * Creates a new instance of the Receivers class with the specified dispatch receiver.
 *
 * @param dispatchReceiver The dispatch receiver expression.
 * @return A new instance of the `Receivers` class with the specified dispatch receiver.
 * @author OUSC
 */
internal fun Receivers.dispatchBy(dispatchReceiver: IrExpression?) {
    this.dispatchReceiver = dispatchReceiver
}

/**
 * Creates a new instance of the Receivers class with the specified extension receiver.
 *
 * @param extensionReceiver The extension receiver expression.
 * @return A new instance of the `Receivers` class with the specified extension receiver.
 * @author OUSC
 */
internal fun Receivers.extensionBy(extensionReceiver: IrExpression?) {
    this.extensionReceiver = extensionReceiver
}

/**
 * Applies an IR function call with the given function symbol, values, and receivers.
 *
 * @param irCall The function symbol of the IR call.
 * @param values The vararg array of expression values for the IR call.
 * @param setReceivers The lambda function that to set the receivers for the IR call. Defaults to a lambda that returns an empty Receivers instance.
 * @return The `IrFunctionAccessExpression` representing the applied IR function call.
 * @author OUSC
 */
context(IrBuilderWithScope)
internal fun applyIrCall(
    irCall: IrFunctionSymbol,
    vararg values: IrExpression?,
    typeArguments: Array<IrType> = emptyArray(),
    setReceivers: Receivers.() -> Unit = { }
): IrFunctionAccessExpression {
    val receiver = Receivers().apply(setReceivers)
    return irCall(irCall).apply {
        dispatchReceiver = receiver.dispatchReceiver
        extensionReceiver = receiver.extensionReceiver
        values.forEachIndexed { index, value ->
            putValueArgument(index, value)
        }
        typeArguments.forEachIndexed { index, value ->
            putTypeArgument(index, value)
        }
    }
}

context(IrBuilderWithScope)
internal fun IrSimpleFunctionSymbol.invoke(
    vararg values: IrExpression?,
    typeArguments: Array<IrType> = emptyArray(),
    setReceivers: Receivers.() -> Unit = { }
): IrFunctionAccessExpression {
    return applyIrCall(this, *values, typeArguments = typeArguments, setReceivers = setReceivers)
}

/**
 * Casts the given IrExpression to an IrCall.
 *
 * @return The `IrCall` representation of the IrExpression.
 */
internal fun IrExpression.asIrCall(): IrCall {
    return this as IrCall
}

/**
 * Finds the first IrConstructorCall in the iterable that has a containing descriptor with the given fqName.
 *
 * @param fqName The fully qualified name of the containing descriptor to search for.
 * @return The first IrConstructorCall that matches the given fqName, or null if none is found.
 */
internal fun <T : IrFunctionAccessExpression> Iterable<T>.findByFqName(fqName: FqName): T? =
    find { it.type.classFqName == fqName }

/**
 * Finds the first IrConstructorCall in the iterable that has a containing descriptor with the given fqName.
 *
 * @param fqName The fully qualified name of the containing descriptor to search for.
 * @return The first IrConstructorCall that matches the given fqName, or null if none is found.
 */
internal fun <T : IrFunctionAccessExpression> Iterable<T>.filterByFqName(fqName: FqName): List<T> =
    filter { it.type.classFqName == fqName }