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

package com.kotlinorm.plugins.utils

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

// A helper class for specifying the receiver of an IR function call
// applyIrCall的辅助类，用于指定IR函数调用的接收器
class Receivers(
    // The dispatch receiver expression
    // 分派接收器表达式
    val dispatchReceiver: IrExpression? = null,
    // The extension receiver expression
    // 扩展接收器表达式
    val extensionReceiver: IrExpression? = null
)

/**
 * Creates a new instance of the Receivers class with the specified dispatch receiver.
 *
 * @param dispatchReceiver The dispatch receiver expression.
 * @return A new instance of the Receivers class with the specified dispatch receiver.
 * @author OUSC
 */
fun dispatchBy(dispatchReceiver: IrExpression?): Receivers {
    return Receivers(dispatchReceiver)
}

/**
 * Creates a new instance of the Receivers class with the specified extension receiver.
 *
 * @param extensionReceiver The extension receiver expression.
 * @return A new instance of the Receivers class with the specified extension receiver.
 * @author OUSC
 */
fun extensionBy(extensionReceiver: IrExpression?): Receivers {
    return Receivers(null, extensionReceiver)
}

/**
 * Creates a new instance of the Receivers class with the specified dispatch and extension receivers.
 *
 * @param dispatchReceiver The dispatch receiver expression.
 * @param extensionReceiver The extension receiver expression.
 * @return A new instance of the Receivers class with the specified dispatch and extension receivers.
 * @author OUSC
 */
fun dispatchAndExtensionBy(dispatchReceiver: IrExpression?, extensionReceiver: IrExpression?): Receivers {
    return Receivers(dispatchReceiver, extensionReceiver)
}

/**
 * Applies an IR function call with the given function symbol, values, and receivers.
 *
 * @param irCall The function symbol of the IR call.
 * @param values The vararg array of expression values for the IR call.
 * @param receivers The lambda function that returns the receivers for the IR call. Defaults to a lambda that returns an empty Receivers instance.
 * @return The IrFunctionAccessExpression representing the applied IR function call.
 * @author OUSC
 */
context(IrBuilderWithScope, IrPluginContext)
internal fun applyIrCall(
    irCall: IrFunctionSymbol,
    vararg values: IrExpression?,
    receivers: () -> Receivers = { Receivers() }
): IrFunctionAccessExpression {
    val receiver = receivers()
    return irCall(irCall).apply {
        dispatchReceiver = receiver.dispatchReceiver
        extensionReceiver = receiver.extensionReceiver
        values.forEachIndexed { index, value ->
            putValueArgument(index, value)
        }
    }
}

/**
 * Casts the given IrExpression to an IrCall.
 *
 * @return The IrCall representation of the IrExpression.
 */
internal fun IrExpression.asIrCall(): IrCall {
    return this as IrCall
}

/**
 * Casts the given IrType to an IrSimpleType.
 *
 * @return The IrSimpleType representation of the IrType.
 */
internal fun IrType.asSimpleType(): IrSimpleType = this as IrSimpleType

/**
 * Returns the first type argument of the given IrType as an IrSimpleType.
 * 返回给定IrType的第一个类型参数
 *
 * @return The first type argument of the given IrType as an IrSimpleType.
 */
internal fun IrType.subType(): IrSimpleType = this.asSimpleType().arguments[0].typeOrFail.asSimpleType()

/**
 * Finds the first IrConstructorCall in the iterable that has a containing descriptor with the given fqName.
 *
 * @param fqName The fully qualified name of the containing descriptor to search for.
 * @return The first IrConstructorCall that matches the given fqName, or null if none is found.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun Iterable<IrConstructorCall>.findByFqName(fqName: FqName): IrConstructorCall? =
    firstOrNull { it.symbol.descriptor.containingDeclaration.fqNameSafe == fqName }