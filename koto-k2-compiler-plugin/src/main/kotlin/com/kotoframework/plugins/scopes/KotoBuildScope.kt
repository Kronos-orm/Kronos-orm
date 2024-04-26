package com.kotoframework.plugins.scopes

import com.kotoframework.plugins.transformer.CommonTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

/**
 * Created by Ousc, on April 18, 2024 at 14:22.
 * The `KotoBuildScope` class is designed for constructing the IR (Intermediate Representation) scope.
 * This class contains the basic components necessary for building IR and several auxiliary methods.
 * 由Ousc创建，创建时间为2024年4月18日14点22分。
 * `KotoBuildScope` 类用于构建IR（Intermediate Representation，中间表示）的作用域。
 * 这个类包含了构建IR所需的基本组件和一些辅助方法。
 */
class KotoBuildScope {
    // IR builder
    // IR构建器
    lateinit var builder: IrBlockBuilder

    // The function within the current scope
    // 当前作用域的函数
    lateinit var function: IrFunction

    // Plugin context containing global information during the compilation process
    // 插件上下文，包含编译过程中的全局信息
    lateinit var pluginContext: IrPluginContext

    companion object {
        // Creating a scope which can be used to construct the IR
        // 创建KotoBuildScope，该scope用于构建IR
        fun IrBlockBuilder.useKotoBuildScope(
            context: CommonTransformer
        ): KotoBuildScope {
            return KotoBuildScope().apply {
                builder = this@useKotoBuildScope
                this.function = context.irFunction
                this.pluginContext = context.pluginContext
            }
        }

        fun dispatchBy(dispatchReceiver: IrExpression?): Receivers {
            return Receivers(dispatchReceiver)
        }

        fun extensionBy(extensionReceiver: IrExpression?): Receivers {
            return Receivers(null, extensionReceiver)
        }

        fun dispatchAndExtension(dispatchReceiver: IrExpression?, extensionReceiver: IrExpression?): Receivers {
            return Receivers(dispatchReceiver, extensionReceiver)
        }
    }

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
     * Simplified method to apply an IR function call using only expressions as parameters.
     * IrCall.apply{ ... }的简化方法，只使用表达式作为参数来应用IrCall。
     */
    internal fun applyIrCall(
        irCall: IrFunctionSymbol,
        vararg values: Pair<Int, IrExpression?>,
        receivers: ()->Receivers = { Receivers() }
    ): IrFunctionAccessExpression {
        val receiver = receivers()
        return builder.irCall(irCall).apply {
            dispatchReceiver = receiver.dispatchReceiver
            extensionReceiver = receiver.extensionReceiver
            values.forEach { putValueArgument(it.first, it.second) }
        }
    }

    /**
     * Simplified method to apply an IR function call using only expressions as parameters.
     * IrCall.apply{ ... }的简化方法，只使用表达式作为参数来应用IrCall。
     */
    internal fun applyIrCall(
        irCall: IrFunctionSymbol,
        vararg values: IrExpression?,
        receivers: ()->Receivers = { Receivers() }
    ): IrFunctionAccessExpression {
        return applyIrCall(
            irCall,
            *values.mapIndexed { index, value -> index to value }.toTypedArray(),
            receivers = receivers
        )
    }
}