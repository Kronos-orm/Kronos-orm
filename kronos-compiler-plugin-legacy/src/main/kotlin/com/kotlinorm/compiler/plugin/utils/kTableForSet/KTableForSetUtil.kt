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

package com.kotlinorm.compiler.plugin.utils.kTableForSet

import com.kotlinorm.compiler.helpers.dispatchReceiverArgument
import com.kotlinorm.compiler.helpers.extensionReceiver
import com.kotlinorm.compiler.helpers.extensionReceiverArgument
import com.kotlinorm.compiler.helpers.invoke
import com.kotlinorm.compiler.helpers.valueArguments
import com.kotlinorm.compiler.plugin.utils.getColumnName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

/**
 * Constructs a list of IR expressions by processing assignment statements and mapping them to field parameters.
 *
 * @return The list of IR expressions representing the parameter mapping.
 */
context(_: IrPluginContext, builder: IrBlockBuilder)
fun putFieldParamMap(irFunction: IrFunction) =
    putParamMapStatements(
        irFunction,
        builder.irGet(irFunction.parameters.extensionReceiver!!),
        irFunction.body!!
    )

/**
 * Constructs a list of IR expressions by processing assignment statements and mapping them to field parameters.
 *
 * @param irFunction The function to process for parameter mapping.
 * @param receiver The receiver [IrExpression] for the parameter mapping.
 * @param element The [IrElement] to process for parameter mapping.
 * @return The list of IR expressions representing the parameter mapping.
 */
context(_: IrPluginContext, builder: IrBlockBuilder)
fun putParamMapStatements(
    irFunction: IrFunction,
    receiver: IrExpression,
    element: IrElement
): MutableList<IrExpression> {
    val statements =
        mutableListOf<IrExpression>()  // Initialize an empty list to hold the resulting IR expressions.
    // 初始化空列表，以保存结果 IR 表达式。
    when (element) {
        is IrBlockBody -> {
            // Recursively handle each statement within a block body.
            // 递归处理块体内的每个语句。
            element.statements.forEach { statement ->
                statements.addAll(putParamMapStatements(irFunction, receiver, statement))
            }
        }

        is IrCall -> {
            val extensionReceiver = element.extensionReceiverArgument
            val dispatchReceiver = element.dispatchReceiverArgument

            // Handle assignment operations (EQ origin) to map field parameters.
            // 处理赋值操作（EQ 原点）以映射字段参数。
            when (element.origin) { // Assignment statement
                IrStatementOrigin.EQ -> statements.add(
                    setValueSymbol(
                        receiver,
                        getColumnName(element),
                        element.valueArguments[0]
                    )
                )

                IrStatementOrigin.PLUSEQ -> statements.add(
                    setAssignSymbol(
                        receiver,
                        builder.irString("+"),
                        getColumnName(extensionReceiver ?: dispatchReceiver!!),
                        element.valueArguments[0]
                    )
                )

                IrStatementOrigin.MINUSEQ -> statements.add(
                    setAssignSymbol(
                        receiver,
                        builder.irString("-"),
                        getColumnName(extensionReceiver ?: dispatchReceiver!!),
                        element.valueArguments[0]
                    )
                )
            }
        }
    }
    return statements
}