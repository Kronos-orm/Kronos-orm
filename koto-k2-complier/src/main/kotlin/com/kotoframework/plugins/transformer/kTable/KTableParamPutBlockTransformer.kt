package com.kotoframework.plugins.transformer.kTable

import com.kotoframework.plugins.scopes.KotoBuildScope.Companion.useKotoBuildScope
import com.kotoframework.plugins.utils.kTable.putFieldParamMap
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression

/**
 * `KTableParamPutBlockTransformer` is designed to manipulate block expressions in Kotlin's Intermediate Representation (IR) during the compilation process.
 * It focuses on enriching blocks by adding field parameter mappings, specifically tailored for transformations that require parameter context.
 * `KTableParamPutBlockTransformer` 旨在编译过程中操作 Kotlin 中间表示 (IR) 的块表达式。
 * 它专注于通过添加字段参数映射来丰富块，特别适用于需要参数上下文的转换。
 */
class KTableParamPutBlockTransformer(
    // Plugin context, includes essential information for the compilation process
    // 插件上下文，包含编译过程中的必要信息
    override val pluginContext: IrPluginContext,

    // The current IR function being transformed
    // 当前正在转换的 IR 函数
    override val irFunction: IrFunction
) : IrElementTransformerVoidWithContext(), CommonTransformer {

    /**
     * Overrides the visitBlock method to apply transformations to IR blocks.
     * This method enriches the block by appending a field parameter mapping at the end.
     * 重写 visitBlock 方法以对 IR 块应用转换。
     * 该方法通过在块的末尾附加一个字段参数映射来丰富块。
     */
    override fun visitBlock(expression: IrBlock): IrExpression {
        val transformer = this
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlock {
            // Preserve existing statements in the block
            // 保留块中的现有语句
            +expression.statements
            // Use a scoped builder to add custom transformations
            // 使用范围构建器添加自定义转换
            +useKotoBuildScope(transformer)
                // Append the custom transformation to add field-parameter mappings
                // 附加自定义转换以添加字段参数映射
                .putFieldParamMap()
        }
    }
}