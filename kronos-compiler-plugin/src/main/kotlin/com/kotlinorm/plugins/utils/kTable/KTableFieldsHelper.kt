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

package com.kotlinorm.plugins.utils.kTable

import com.kotlinorm.plugins.helpers.applyIrCall
import com.kotlinorm.plugins.helpers.dispatchBy
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*

/**
 * Creates a list of IR expressions that represent field additions, using a predefined symbol to generate `addField` calls.
 *
 * @return A list of IR expressions representing field additions.
 */
context(IrBuilderWithScope, IrPluginContext, IrFunction)
fun addFieldList(): List<IrExpression> {
    return addFieldsNames(body!!).map {
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
context(IrBuilderWithScope, IrPluginContext)
fun addFieldsNames(element: IrElement): MutableList<IrExpression> {
    // Initialize an empty list for field names.
    // 初始化字段名的空列表。
    val fieldNames = mutableListOf<IrExpression>()
    when (element) {
        is IrBlockBody -> {
            element.statements.forEach { statement ->
                // Recursively add field names from each statement in a block body.
                // 从块体中的每个声明递归添加字段名。
                fieldNames.addAll(addFieldsNames(statement))
            }
        }

        is IrTypeOperatorCall -> {
            fieldNames.addAll(addFieldsNames(element.argument))
        }

        is IrCall -> {
            when (element.origin) {
                IrStatementOrigin.PLUS -> {
                    // Add field names from both the receiver and value arguments if the origin is a PLUS operation.
                    // 如果起源是 PLUS 操作，从接收器和值参数添加字段名。
                    fieldNames.addAll(addFieldsNames(element.extensionReceiver!!))
                    val args = element.valueArguments.filterNotNull()
                    args.forEach {
                        fieldNames.addAll(addFieldsNames(it))
                    }
                }

                IrStatementOrigin.GET_PROPERTY -> {
                    getColumnName(element).let { fieldNames.add(it) }
                }
            }
        }

        is IrConst<*> -> {
            // Add constant values directly to the field names list.
            // 直接将常量值添加到字段名列表。
            fieldNames.add(element as IrExpression)
        }

        is IrReturn -> {
            // Handle return statements by recursively adding field names from the return value.
            // 通过递归从返回值添加字段名来处理返回语句。
            return addFieldsNames(element.value)
        }
    }
    return fieldNames
}