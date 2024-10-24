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

package com.kotlinorm.compiler.fir.utils.kTableForSelect

import com.kotlinorm.compiler.fir.utils.*
import com.kotlinorm.compiler.fir.utils.fieldSymbol
import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.dispatchBy
import com.kotlinorm.compiler.helpers.extensionBy
import com.kotlinorm.compiler.fir.utils.kTableForCondition.analyzeMinusExpression
import com.kotlinorm.compiler.helpers.irEnum
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.properties

/**
 * Adds a list of fields to the given IrReturn by gathering field names and applying the `addField` operation to each name.
 *
 * @param irReturn the IrReturn to which the fields will be added
 * @return a list of IrExpressions representing the applied `addField` operations
 */
context(IrBuilderWithScope, IrPluginContext, IrFunction)
fun addFieldList(irReturn: IrReturn, functions: Array<String>): List<IrExpression> {
    return addFieldsNames(irReturn, functions).map {
        // Apply the `addField` operation to each field name gathered, passing the receiver.
        // 将 `addField` 操作应用于收集到的每个字段名，传递接收者。
        applyIrCall(addFieldSymbol, it) { dispatchBy(irGet(extensionReceiverParameter!!)) }
    }
}

/**
 * Recursively adds field names from the given IR element to a mutable list.
 *
 * @param element the [IrElement] to extract field names from
 * @return a mutable list of IR expressions representing the field names
 */
context(IrBuilderWithScope, IrPluginContext, IrFunction)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun addFieldsNames(element: IrElement, functions: Array<String>): MutableList<IrExpression> {
    // Initialize an empty list for field names.
    // 初始化字段名的空列表。
    val fieldNames = mutableListOf<IrExpression>()
    when (element) {
        is IrBlockBody -> {
            element.statements.forEach { statement ->
                // Recursively add field names from each statement in a block body.
                // 从块体中的每个声明递归添加字段名。
                fieldNames.addAll(addFieldsNames(statement, functions))
            }
        }

        is IrTypeOperatorCall -> {
            fieldNames.addAll(addFieldsNames(element.argument, functions))
        }

        is IrCall -> {
            when (element.origin) {
                IrStatementOrigin.MINUS -> {
                    val (irClass, _, excludes) = analyzeMinusExpression(element)
                    irClass.properties.forEach { prop ->
                        if (prop.isColumn() && prop.name.asString() !in excludes) {
                            fieldNames.add(
                                getColumnName(prop)
                            )
                        }
                    }
                }

                IrStatementOrigin.PLUS -> {
                    // Add field names from both the receiver and value arguments if the origin is a PLUS operation.
                    // 如果起源是 PLUS 操作，从接收器和值参数添加字段名。
                    fieldNames.addAll(addFieldsNames((element.extensionReceiver ?: element.dispatchReceiver)!!, functions))
                    val args = element.valueArguments.filterNotNull()
                    args.forEach {
                        fieldNames.addAll(addFieldsNames(it, functions))
                    }
                }

                IrStatementOrigin.GET_PROPERTY -> {
                    getColumnName(element).let { fieldNames.add(it) }
                }

                else -> {
                    when (element.funcName()) {
                        "as" -> {
                            fieldNames.add(applyIrCall(
                                aliasSymbol,
                                element.valueArguments.first()
                            ) {
                                dispatchBy(
                                    irGet(
                                        extensionReceiverParameter!!
                                    ),
                                )
                                extensionBy(
                                    addFieldsNames(element.extensionReceiver!!, functions).first()
                                )
                            })
                        }
                    }
                }
            }
        }

        is IrConst<*> -> {
            // Add constant values directly to the field names list.
            // 直接将常量值添加到字段名列表。
            fieldNames.add(
                applyIrCall(
                    fieldSymbol.constructors.first(),
                    element,
                    element,
                    irEnum(kColumnTypeSymbol, kotlinTypeToKColumnType("CUSTOM_CRITERIA_SQL"))
                )
            )
        }

        is IrReturn -> {
            // Handle return statements by recursively adding field names from the return value.
            // 通过递归从返回值添加字段名来处理返回语句。
            return addFieldsNames(element.value, functions)
        }
    }
    return fieldNames
}