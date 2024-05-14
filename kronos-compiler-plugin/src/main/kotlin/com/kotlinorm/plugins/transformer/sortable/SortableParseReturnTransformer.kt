package com.kotlinorm.plugins.transformer.sortable

import com.kotlinorm.plugins.utils.kTable.addFieldList
import com.kotlinorm.plugins.utils.kTableConditional.setCriteriaIr
import com.kotlinorm.plugins.utils.kTableSortType.setFieldSortsIr
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/14 15:29
 **/
class SortableParseReturnTransformer(
    private val pluginContext: IrPluginContext,
    private val irFunction: IrFunction
): IrElementTransformerVoidWithContext() {

    override fun visitCall(expression: IrCall): IrExpression {
        with(pluginContext) {
            with(irFunction) {
                return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlock {
                    if (expression.origin in arrayOf(IrStatementOrigin.PLUS, IrStatementOrigin.GET_PROPERTY)) {
                        +setFieldSortsIr()
                    }
                }
            }
        }
    }

}