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

package com.kotlinorm.compiler.plugin.utils.kTableForSelect

import com.kotlinorm.compiler.plugin.utils.fieldSymbol
import com.kotlinorm.compiler.plugin.utils.getColumnName
import com.kotlinorm.compiler.plugin.utils.getFunctionName
import com.kotlinorm.compiler.plugin.utils.kColumnTypeSymbol
import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.dispatchBy
import com.kotlinorm.compiler.helpers.extensionBy
import com.kotlinorm.compiler.plugin.utils.kTableForCondition.analyzeMinusExpression
import com.kotlinorm.compiler.plugin.utils.kotlinTypeToKColumnType
import com.kotlinorm.compiler.helpers.irEnum
import org.jetbrains.kotlin.ir.IrElement
import com.kotlinorm.compiler.helpers.valueArguments
import com.kotlinorm.compiler.plugin.utils.context.KotlinBuilderContext
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
fun KotlinBuilderContext.addFieldList(irFunction: IrFunction, irReturn: IrReturn): List<IrExpression> {
    with(pluginContext){
        with(builder){
            return collectFields(irFunction, irReturn).map {
                applyIrCall(addFieldSymbol, it) { dispatchBy(irGet(irFunction.extensionReceiverParameter!!)) }
            }
        }
    }
}

/**
 * Recursively adds field names from the given IR element to a mutable list.
 *
 * @param element the [IrElement] to extract field names from
 * @return a mutable list of IR expressions representing the field names
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun KotlinBuilderContext.collectFields(
    irFunction: IrFunction, element: IrElement
): MutableList<IrExpression> {
    with(pluginContext){
        with(builder){
            // Initialize an empty list for field names.
            // 初始化字段名的空列表。
            val fields = mutableListOf<IrExpression>()
            when (element) {
                is IrBlockBody -> {
                    element.statements.forEach { statement ->
                        // Recursively add field names from each statement in a block body.
                        // 从块体中的每个声明递归添加字段名。
                        fields += collectFields(irFunction, statement)
                    }
                }

                is IrTypeOperatorCall -> {
                    fields += collectFields(irFunction, element.argument)
                }

                is IrCall -> {
                    when (element.origin) {
                        IrStatementOrigin.MINUS -> {
                            val (irClass, _, excludes) = analyzeMinusExpression(element)
                            irClass.properties.forEach { prop ->
                                if (prop.isColumn() && prop.name.asString() !in excludes) {
                                    fields.add(
                                        getColumnName(prop)
                                    )
                                }
                            }
                        }

                        IrStatementOrigin.PLUS -> {
                            // Add field names from both the receiver and value arguments if the origin is a PLUS operation.
                            // 如果起源是 PLUS 操作，从接收器和值参数添加字段名。
                            fields += collectFields(
                                irFunction,
                                (element.extensionReceiver ?: element.dispatchReceiver)!!
                            )
                            element.valueArguments.forEach {
                                if (it != null) fields += collectFields(irFunction, it)
                            }
                        }

                        IrStatementOrigin.GET_PROPERTY -> {
                            fields += getColumnName(element)
                        }

                        else -> {
                            if (element.isKronosFunction()) {
                                fields += getFunctionName(element)
                            }

                            when (element.funcName()) {
                                "unaryPlus"->{
                                    // Add field names from the receiver if the origin is a UPLUS operation.
                                    // 如果起源是 UPLUS 操作，从接收器添加字段名。
                                    fields += collectFields(
                                        irFunction,
                                        element.extensionReceiver ?: element.dispatchReceiver!!
                                    )
                                }

                                "as_" -> {
                                    fields += applyIrCall(
                                        aliasSymbol,
                                        element.valueArguments.first()
                                    ) {
                                        dispatchBy(
                                            irGet(
                                                irFunction.extensionReceiverParameter!!
                                            )
                                        )
                                        extensionBy(
                                            collectFields(irFunction, element.extensionReceiver!!).first()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is IrConst -> {
                    // Add constant values directly to the field names list.
                    // 直接将常量值添加到字段名列表。
                    fields += applyIrCall(
                        fieldSymbol.constructors.first(),
                        element,
                        element,
                        irEnum(kColumnTypeSymbol, kotlinTypeToKColumnType("CUSTOM_CRITERIA_SQL"))
                    )
                }

                is IrPropertyReference -> {
                    fields += getColumnName(element)
                }

                is IrReturn -> {
                    // Handle return statements by recursively adding field names from the return value.
                    // 通过递归从返回值添加字段名来处理返回语句。
                    return collectFields(irFunction, element.value)
                }
            }
            return fields
        }
    }
}