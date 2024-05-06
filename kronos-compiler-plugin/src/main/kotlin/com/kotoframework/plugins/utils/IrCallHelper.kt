package com.kotoframework.plugins.utils

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

fun dispatchBy(dispatchReceiver: IrExpression?): Receivers {
    return Receivers(dispatchReceiver)
}

fun extensionBy(extensionReceiver: IrExpression?): Receivers {
    return Receivers(null, extensionReceiver)
}

fun dispatchAndExtensionBy(dispatchReceiver: IrExpression?, extensionReceiver: IrExpression?): Receivers {
    return Receivers(dispatchReceiver, extensionReceiver)
}

/**
 * Simplified method to apply an IR function call using only expressions as parameters.
 * IrCall.apply{ ... }的简化方法，只使用表达式作为参数来应用IrCall。
 */
context(IrBuilderWithScope, IrPluginContext)
internal fun applyIrCall(
    irCall: IrFunctionSymbol,
    vararg values: Pair<Int, IrExpression?>,
    receivers: () -> Receivers = { Receivers() }
): IrFunctionAccessExpression {
    val receiver = receivers()
    return irCall(irCall).apply {
        dispatchReceiver = receiver.dispatchReceiver
        extensionReceiver = receiver.extensionReceiver
        values.forEach { putValueArgument(it.first, it.second) }
    }
}

/**
 * Simplified method to apply an IR function call using only expressions as parameters.
 * IrCall.apply{ ... }的简化方法，只使用表达式作为参数来应用IrCall。
 */
context(IrBuilderWithScope, IrPluginContext)
internal fun applyIrCall(
    irCall: IrFunctionSymbol,
    vararg values: IrExpression?,
    receivers: () -> Receivers = { Receivers() }
): IrFunctionAccessExpression {
    return applyIrCall(
        irCall,
        *values.mapIndexed { index, value -> index to value }.toTypedArray(),
        receivers = receivers
    )
}

internal fun IrExpression.asIrCall(): IrCall {
    return this as IrCall
}

internal fun IrType.asSimpleType(): IrSimpleType = this as IrSimpleType
internal fun IrType.subType(): IrSimpleType = this.asSimpleType().arguments[0].typeOrFail.asSimpleType()

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun Iterable<IrConstructorCall>.findByFqName(fqName: FqName): IrConstructorCall? =
    firstOrNull { it.symbol.descriptor.containingDeclaration.fqNameSafe == fqName }

fun <T, R> withMultiple(obj1: T, obj2: T, block: (T, T) -> R): R {
    return block(obj1, obj2)
}