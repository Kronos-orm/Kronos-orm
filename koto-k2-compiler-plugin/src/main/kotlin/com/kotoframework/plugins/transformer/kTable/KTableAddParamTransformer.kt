package com.kotoframework.plugins.transformer.kTable

import com.kotoframework.plugins.utils.kTable.putFieldParamMap
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression

/**
 * `KTableAddParamTransformer` is designed to manipulate block expressions in Kotlin's Intermediate Representation (IR) during the compilation process.
 * It focuses on enriching blocks by adding field parameter mappings, specifically tailored for transformations that require parameter context.
 * `KTableAddParamTransformer` 旨在编译过程中操作 Kotlin 中间表示 (IR) 的块表达式。
 * 它专注于通过添加字段参数映射来丰富块，特别适用于需要参数上下文的转换。
 */
class KTableAddParamTransformer(
    // Plugin context, includes essential information for the compilation process
    // 插件上下文，包含编译过程中的必要信息
    private val pluginContext: IrPluginContext,

    // The current IR function being transformed
    // 当前正在转换的 IR 函数
    private val irFunction: IrFunction
) : IrElementTransformerVoidWithContext() {

    /**
     * Overrides the visitBlock method to apply transformations to IR blocks.
     * This method enriches the block by appending a field parameter mapping at the end.
     * 重写 visitBlock 方法以对 IR 块应用转换。
     * 该方法通过在块的末尾附加一个字段参数映射来丰富块。
     */
    override fun visitBlock(expression: IrBlock): IrExpression {
        with(pluginContext) {
            with(irFunction) {
                return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlock {
                    // Preserve existing statements in the block
                    // 保留块中的现有语句
                    +expression.statements
                    // add field-parameter mappings
                    // 添加字段参数映射
                    +putFieldParamMap()

                    super.visitBlock(expression)
                }
            }
        }
    }
}