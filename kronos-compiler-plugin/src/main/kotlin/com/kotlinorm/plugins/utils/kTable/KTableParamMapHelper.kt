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
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

/**
 * Constructs a list of IR expressions by processing assignment statements and mapping them to field parameters.
 *
 * @return The list of IR expressions representing the parameter mapping.
 */
context(IrBuilderWithScope, IrPluginContext, IrFunction)
fun putFieldParamMap(): List<IrExpression> {
    return putParamMapStatements(irGet(extensionReceiverParameter!!), body!!)
}

/**
 * Constructs a list of IR expressions by processing assignment statements and mapping them to field parameters.
 *
 * @param receiver The receiver [IrExpression] for the parameter mapping.
 * @param element The [IrElement] to process for parameter mapping.
 * @return The list of IR expressions representing the parameter mapping.
 */
context(IrBuilderWithScope, IrPluginContext)
fun putParamMapStatements(receiver: IrExpression, element: IrElement): MutableList<IrExpression> {
    val statements = mutableListOf<IrExpression>()  // Initialize an empty list to hold the resulting IR expressions.
    // 初始化空列表，以保存结果 IR 表达式。
    when (element) {
        is IrBlockBody -> {
            // Recursively handle each statement within a block body.
            // 递归处理块体内的每个语句。
            element.statements.forEach { statement ->
                statements.addAll(putParamMapStatements(receiver, statement))
            }
        }

        is IrCall -> {
            // Handle assignment operations (EQ origin) to map field parameters.
            // 处理赋值操作（EQ 原点）以映射字段参数。
            if (element.origin == IrStatementOrigin.EQ) { // Assignment statement
                statements.add(
                    applyIrCall(
                        setValueSymbol,
                        getColumnName(element),
                        element.valueArguments[0]
                    ) {
                        dispatchBy(receiver)
                    }
                )
            }
        }
    }
    return statements
}