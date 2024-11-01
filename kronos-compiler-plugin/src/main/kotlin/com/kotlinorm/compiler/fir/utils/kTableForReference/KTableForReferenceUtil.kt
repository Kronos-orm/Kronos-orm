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

package com.kotlinorm.compiler.fir.utils.kTableForReference

import com.kotlinorm.compiler.fir.utils.*
import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.dispatchBy
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Adds a list of fields to the given IrReturn by gathering field names and applying the `addField` operation to each name.
 *
 * @param irReturn the IrReturn to which the fields will be added
 * @return a list of IrExpressions representing the applied `addField` operations
 */
context(IrBuilderWithScope, IrPluginContext, IrFunction)
fun addReferenceList(irReturn: IrReturn): List<IrExpression> {
    return collectReferences(irReturn).map {
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
fun collectReferences(
    element: IrElement
): MutableList<IrExpression> {
    // Initialize an empty list for field names.
    // 初始化字段名的空列表。
    val fields = mutableListOf<IrExpression>()
    when (element) {
        is IrBlockBody -> {
            element.statements.forEach { statement ->
                // Recursively add field names from each statement in a block body.
                // 从块体中的每个声明递归添加字段名。
                fields += collectReferences(statement)
            }
        }

        is IrTypeOperatorCall -> {
            fields += collectReferences(element.argument)
        }

        is IrCall -> {
            when {
                element.origin == IrStatementOrigin.PLUS -> {
                    // Add field names from both the receiver and value arguments if the origin is a PLUS operation.
                    // 如果起源是 PLUS 操作，从接收器和值参数添加字段名。
                    fields += collectReferences(
                        (element.extensionReceiver ?: element.dispatchReceiver)!!
                    )
                    element.valueArguments.forEach {
                        if (it != null) fields += collectReferences(it)
                    }
                }

                element.funcName() == "unaryPlus" -> {
                    // Add field names from the receiver if the origin is a UPLUS operation.
                    // 如果起源是 unaryPlus 操作，从接收器添加字段名。
                    fields += collectReferences(
                        element.extensionReceiver ?: element.dispatchReceiver!!
                    )
                }
            }
        }

        is IrPropertyReference -> {
            fields += getColumnName(element)
        }

        is IrReturn -> {
            // Handle return statements by recursively adding field names from the return value.
            // 通过递归从返回值添加字段名来处理返回语句。
            return collectReferences(element.value)
        }
    }
    return fields
}