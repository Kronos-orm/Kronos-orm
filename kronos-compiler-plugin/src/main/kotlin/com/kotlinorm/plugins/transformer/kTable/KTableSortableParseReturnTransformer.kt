package com.kotlinorm.plugins.transformer.kTable

import com.kotlinorm.plugins.utils.kTableSortType.addFieldSortsIr
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/14 15:29
 **/
class KTableSortableParseReturnTransformer(
    private val pluginContext: IrPluginContext,
    private val irFunction: IrFunction
) : IrElementTransformerVoidWithContext() {
    override fun visitReturn(expression: IrReturn): IrExpression {
        with(pluginContext) {
            with(irFunction) {
                with(DeclarationIrBuilder(pluginContext, irFunction.symbol)) {
                    return irBlock {
                        +addFieldSortsIr(expression)
                        +expression
                    }
                }
            }
        }
    }
}