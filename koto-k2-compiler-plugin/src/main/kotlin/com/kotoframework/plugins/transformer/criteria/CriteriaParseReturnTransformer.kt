package com.kotoframework.plugins.transformer.criteria

import com.kotoframework.plugins.scopes.KotoBuildScope.Companion.useKotoBuildScope
import com.kotoframework.plugins.transformer.CommonTransformer
import com.kotoframework.plugins.utils.kTable.addFieldList
import com.kotoframework.plugins.utils.kTableConditional.buildCritercia
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

/**
 *@program: kotoframework
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/4/23 15:10
 **/
class CriteriaParseReturnTransformer(
    // Context for the plugin, contains global information about the compilation
    // 插件的上下文，包含编译过程中的全球信息
    override val pluginContext: IrPluginContext,

    // The current IR function in the transformation process
    // 转换过程中的当前 IR 函数
    override val irFunction: org.jetbrains.kotlin.ir.declarations.IrFunction
): IrElementTransformerVoidWithContext() , CommonTransformer {

    override fun visitReturn(expression: IrReturn): IrExpression {
        if (expression.returnTargetSymbol != irFunction.symbol) {
            return super.visitReturn(expression)
        }
        val transformer = this
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlock {
            +useKotoBuildScope(transformer)
                .buildCritercia(expression)!!
        }
    }
}