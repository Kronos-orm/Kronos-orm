/**
 * Copyright 2022-2024 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.compiler.plugin.utils.kTableForSort

import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.asIrCall
import com.kotlinorm.compiler.helpers.dispatchBy
import com.kotlinorm.compiler.helpers.extensionBy
import com.kotlinorm.compiler.plugin.utils.context.KotlinBlockBuilderContext
import com.kotlinorm.compiler.plugin.utils.getColumnName
import org.jetbrains.kotlin.ir.IrElement
import com.kotlinorm.compiler.helpers.valueArguments
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl

/**
 * Recursively traverses the given IrElement and extracts the sort fields.
 *
 * @param irReturn The IrReturn to traverse.
 * @return A mutable list of IrExpressions representing the sort fields.
 */
fun KotlinBlockBuilderContext.addFieldSortsIr(
    irFunction: IrFunction,
    irReturn: IrReturn
): List<IrFunctionAccessExpression> {
    with(pluginContext) {
        with(builder) {
            return getSortFields(irFunction, irReturn).map {
                applyIrCall(
                    addSortFieldSymbol, it
                ) {
                    dispatchBy(irGet(irFunction.extensionReceiverParameter!!))
                }
            }
        }
    }
}

/**
 * Recursively traverses the given IrElement and extracts the sort fields.
 *
 * @param element The IrElement to traverse.
 * @return A mutable list of IrExpressions representing the sort fields.
 */
fun KotlinBlockBuilderContext.getSortFields(irFunction: IrFunction, element: IrElement): MutableList<IrExpression> {
    val variables = mutableListOf<IrExpression>()
    with(pluginContext) {
        with(builder) {
            when (element) {
                is IrBlockBody -> {
                    // 处理块体
                    element.statements.forEach { statement ->
                        variables.addAll(getSortFields(irFunction, statement))
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
                                        irFunction.extensionReceiverParameter!!
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
                                variables.addAll(getSortFields(irFunction, extensionReceiver))
                                variables.addAll(getSortFields(irFunction, element.valueArguments.first()!!))
                            }

                            "desc" -> {
                                val field = getColumnName(extensionReceiver)
                                variables.add(
                                    applyIrCall(
                                        createDescSymbol
                                    ) {
                                        dispatchBy(
                                            irGet(
                                                irFunction.extensionReceiverParameter!!
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
                                                irFunction.extensionReceiverParameter!!
                                            )
                                        )
                                        extensionBy(field)
                                    }
                                )
                            }
                        }
                    }
                }

                is IrGetValueImpl, is IrConst -> {
                    variables.add(
                        applyIrCall(
                            createAscSymbol
                        ) {
                            dispatchBy(
                                irGet(
                                    irFunction.extensionReceiverParameter!!
                                )
                            )
                            extensionBy(element)
                        }
                    )
                }

                is IrReturn -> {
                    getSortFields(irFunction, element.value)
                }
            }
            return variables
        }
    }
}