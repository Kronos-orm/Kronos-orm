package com.kotlinorm.plugins.utils.kTableSortType

import com.kotlinorm.plugins.utils.applyIrCall
import com.kotlinorm.plugins.utils.dispatchBy
import com.kotlinorm.plugins.utils.extensionBy
import com.kotlinorm.plugins.utils.kTable.getColumnName
import com.kotlinorm.plugins.utils.kTableConditional.funcName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall

context(IrBlockBuilder , IrPluginContext , IrFunction)
fun setFieldSortsIr() =
    applyIrCall(
        fieldSortsSetterSymbol, irGet(buildFieldSortsIr(body!!))
    ) {
        dispatchBy(irGet(extensionReceiverParameter!!))
    }

context(IrBlockBuilder , IrPluginContext , IrFunction)
fun buildFieldSortsIr(element: IrElement): IrVariable {

    val irCall = ((element as IrBlockBody).statements[0] as IrTypeOperatorCall).argument as IrCall
    val field = getColumnName(irCall.extensionReceiver!!)

    val fieldSorts = applyIrCall(
        createAscSymbol
    ){
        extensionBy(field)
    }.takeUnless { "desc" == irCall.funcName() } ?: applyIrCall(
        createDescSymbol,
    ){
        extensionBy(field)
    }

    return SortableIR(fieldSorts).toIrVariable()
}