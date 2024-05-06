package com.kotoframework.plugins.transformer.criteria

import com.kotoframework.plugins.utils.kTableConditional.setCriteriaIr
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn

/**
 *@program: kotoframework
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/4/23 15:10
 **/
class CriteriaParseReturnTransformer(
    // Context for the plugin, contains global information about the compilation
    // 插件的上下文，包含编译过程中的全球信息
    private val pluginContext: IrPluginContext,
    // The current IR function in the transformation process
    // 转换过程中的当前 IR 函数
    private val irFunction: IrFunction
) : IrElementTransformerVoidWithContext() {

    override fun visitReturn(expression: IrReturn): IrExpression {
        if (expression.returnTargetSymbol != irFunction.symbol) {
            return super.visitReturn(expression)
        }
        with(pluginContext) {
            with(irFunction) {
                return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlock {
                    +setCriteriaIr()
                }
            }
        }
    }
}