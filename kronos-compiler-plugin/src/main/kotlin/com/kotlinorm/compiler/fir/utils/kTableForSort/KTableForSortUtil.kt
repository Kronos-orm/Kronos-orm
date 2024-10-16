package com.kotlinorm.compiler.fir.utils.kTableForSort

import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.asIrCall
import com.kotlinorm.compiler.helpers.dispatchBy
import com.kotlinorm.compiler.helpers.extensionBy
import com.kotlinorm.compiler.fir.utils.funcName
import com.kotlinorm.compiler.fir.utils.getColumnName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl

/**
 * Recursively traverses the given IrElement and extracts the sort fields.
 *
 * @param irReturn The IrReturn to traverse.
 * @return A mutable list of IrExpressions representing the sort fields.
 */
context(IrBlockBuilder, IrPluginContext, IrFunction)
fun addFieldSortsIr(irReturn: IrReturn) = getSortFields(irReturn).map {
    applyIrCall(
        addSortFieldSymbol, it
    ) {
        dispatchBy(irGet(extensionReceiverParameter!!))
    }
}

/**
 * Recursively traverses the given IrElement and extracts the sort fields.
 *
 * @param element The IrElement to traverse.
 * @return A mutable list of IrExpressions representing the sort fields.
 */
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
                        dispatchBy(
                            irGet(
                                extensionReceiverParameter!!
                            )
                        )
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
                                dispatchBy(
                                    irGet(
                                        extensionReceiverParameter!!
                                    )
                                )
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
                                dispatchBy(
                                    irGet(
                                        extensionReceiverParameter!!
                                    )
                                )
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
                    dispatchBy(
                        irGet(
                            extensionReceiverParameter!!
                        )
                    )
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