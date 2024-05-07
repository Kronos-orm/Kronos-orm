package com.kotlinorm.plugins.transformer.kTable

import com.kotlinorm.plugins.utils.kTable.addFieldList
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

/**
 * `KTableAddFieldTransformer` class is a Kotlin compiler plugin transformer that manipulates IR elements related to table fields.
 * It overrides the `visitCall` method to transform specific IR calls within the Kotlin Intermediate Representation (IR).
 * `KTableAddFieldTransformer` 类是一个 Kotlin 编译器插件转换器，用于操作与表字段相关的 IR 元素。
 * 它重写了 `visitCall` 方法，以转换 Kotlin 中间表示（IR）中的特定 IR 调用。
 */
class KTableAddFieldTransformer(
    // Plugin context, includes necessary information for the compilation process
    // 插件上下文，包含编译过程所需的信息
    private val pluginContext: IrPluginContext,

    // The current IR function being transformed
    // 当前正在转换的 IR 函数
    private val irFunction: IrFunction
) : IrElementTransformerVoidWithContext() {

    /**
     * Overrides the visitCall method to apply transformations based on the origin of the IR call.
     * If the call originates from a PLUS operation, it modifies the IR structure by adding fields.
     * 重写 visitCall 方法，根据 IR 调用的来源应用转换。
     * 如果调用源自 PLUS 操作，它通过添加字段来修改 IR 结构。
     */
    override fun visitCall(expression: IrCall): IrExpression {
        with(pluginContext) {
            with(irFunction) {
                return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlock {
                    if (expression.origin in arrayOf(IrStatementOrigin.PLUS, IrStatementOrigin.GET_PROPERTY)) {
                        +addFieldList()
                    }
                }
            }
        }
    }
}