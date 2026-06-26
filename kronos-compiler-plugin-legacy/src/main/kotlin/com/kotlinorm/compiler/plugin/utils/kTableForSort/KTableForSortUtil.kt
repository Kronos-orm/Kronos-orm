/**
 * Copyright 2022-2025 kronos-orm
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

import com.kotlinorm.compiler.helpers.dispatchReceiverArgument
import com.kotlinorm.compiler.helpers.extensionReceiver
import com.kotlinorm.compiler.helpers.extensionReceiverArgument
import com.kotlinorm.compiler.helpers.invoke
import com.kotlinorm.compiler.helpers.irCast
import com.kotlinorm.compiler.helpers.valueArguments
import com.kotlinorm.compiler.plugin.utils.getColumnName
import com.kotlinorm.compiler.plugin.utils.kTableForCondition.funcName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
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
context(_: IrPluginContext, builder: IrBlockBuilder)
fun addFieldSortsIr(
    irFunction: IrFunction,
    irReturn: IrReturn
) = getSortFields(irFunction, irReturn).map {
    addSortFieldSymbol(
        builder.irGet(irFunction.parameters.extensionReceiver!!),
        it
    )
}

/**
 * Recursively traverses the given IrElement and extracts the sort fields.
 *
 * @param element The IrElement to traverse.
 * @return A mutable list of IrExpressions representing the sort fields.
 */
context(_: IrPluginContext, builder: IrBlockBuilder)
fun getSortFields(irFunction: IrFunction, element: IrElement): MutableList<IrExpression> {
    val variables = mutableListOf<IrExpression>()
    when (element) {
        is IrBlockBody -> {
            // 处理块体
            element.statements.forEach { statement ->
                variables.addAll(getSortFields(irFunction, statement))
            }
        }

        is IrCall -> {
            val extensionReceiver = element.extensionReceiverArgument
            if (element.origin == IrStatementOrigin.GET_PROPERTY) {
                val field = getColumnName(element)
                variables.add(
                    createAscSymbol(
                        builder.irGet(
                            irFunction.parameters.extensionReceiver!!
                        ),
                        field
                    )
                )
            } else {
                val irCall = element.irCast<IrCall>()
                when (irCall.funcName()) {
                    "plus" -> {
                        variables.addAll(getSortFields(irFunction, extensionReceiver!!))
                        variables.addAll(getSortFields(irFunction, element.valueArguments.first()!!))
                    }

                    "desc" -> {
                        val field = getColumnName(extensionReceiver!!)
                        variables.add(
                            createDescSymbol(
                                builder.irGet(
                                    irFunction.parameters.extensionReceiver!!,
                                ),
                                field
                            )
                        )
                    }

                    "asc" -> {
                        val field = getColumnName(extensionReceiver!!)

                        variables.add(
                            createAscSymbol(
                                builder.irGet(
                                    irFunction.parameters.extensionReceiver!!
                                ),
                                field
                            )
                        )
                    }
                }
            }
        }

        is IrGetValueImpl, is IrConst -> {
            variables.add(
                createAscSymbol(
                    builder.irGet(
                        irFunction.parameters.extensionReceiver!!
                    ),
                    element
                )
            )
        }

        is IrReturn -> {
            return getSortFields(irFunction, element.value)
        }
    }
    return variables
}