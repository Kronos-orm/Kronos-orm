package com.kotlinorm.plugins.utils.kTableSortType

import com.kotlinorm.plugins.helpers.applyIrCall
import com.kotlinorm.plugins.helpers.asIrCall
import com.kotlinorm.plugins.helpers.dispatchBy
import com.kotlinorm.plugins.helpers.extensionBy
import com.kotlinorm.plugins.utils.kTable.getColumnName
import com.kotlinorm.plugins.utils.kTableConditional.funcName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl

context(IrBlockBuilder, IrPluginContext, IrFunction)
fun addFieldSortsIr(irReturn: IrReturn) = getSortFields(irReturn).map {
    applyIrCall(
        addSortFieldSymbol, it
    ) {
        dispatchBy(irGet(extensionReceiverParameter!!))
    }
}

context(IrBlockBuilder, IrPluginContext, IrFunction)
fun getSortFields(element: IrElement): MutableList<IrExpression> {
    val variables = mutableListOf<IrExpression>()
    when (element) {
        is IrBlockBody -> {
            // 处理块体
            element.statements.forEach { statement ->
                variables.addAll(getSortFields(statement))
            }
        }

        is IrCall -> {
            if (element.origin == IrStatementOrigin.GET_PROPERTY) {
                val field = getColumnName(element)
                variables.add(
                    applyIrCall(
                        createAscSymbol
                    ) {
                        dispatchBy(irGet(
                            extensionReceiverParameter!!
                        ))
                        extensionBy(field)
                    }
                )
            } else {
                val irCall = element.asIrCall()
                val extensionReceiver = element.extensionReceiver!!
                when (irCall.funcName()) {
                    "plus" -> {
                        variables.addAll(getSortFields(extensionReceiver))
                        variables.addAll(getSortFields(element.valueArguments.first()!!))
                    }

                    "desc" -> {
                        val field = getColumnName(extensionReceiver)
                        variables.add(
                            applyIrCall(
                                createDescSymbol
                            ) {
                                dispatchBy(irGet(
                                    extensionReceiverParameter!!
                                ))
                                extensionBy(field)
                            }
                        )
                    }

                    "asc" -> {
                        val field = getColumnName(irCall.extensionReceiver!!)

                        variables.add(
                            applyIrCall(
                                createAscSymbol
                            ) {
                                dispatchBy(irGet(
                                    extensionReceiverParameter!!
                                ))
                                extensionBy(field)
                            }
                        )
                    }
                }
            }
        }

        is IrGetValueImpl, is IrConst<*> -> {
            variables.add(
                applyIrCall(
                    createAscSymbol
                ) {
                    dispatchBy(irGet(
                        extensionReceiverParameter!!
                    ))
                    extensionBy(element as IrExpression)
                }
            )
        }

        is IrReturn -> {
            return getSortFields(element.value)
        }
    }
    return variables
}