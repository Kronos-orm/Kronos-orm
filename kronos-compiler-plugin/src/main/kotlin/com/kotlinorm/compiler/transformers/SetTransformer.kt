/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm.compiler.transformers

import com.kotlinorm.compiler.core.ErrorReporter
import com.kotlinorm.compiler.core.KTableTransformer
import com.kotlinorm.compiler.core.buildFieldFromPropertyAccess
import com.kotlinorm.compiler.core.setAssignMethodSymbol
import com.kotlinorm.compiler.core.setValueMethodSymbol
import com.kotlinorm.compiler.utils.extensionReceiver
import com.kotlinorm.compiler.utils.extensionReceiverArgument
import com.kotlinorm.compiler.utils.dispatchReceiverArgument
import com.kotlinorm.compiler.utils.getValueArgumentSafe
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Set Transformer
 *
 * Transform IR blocks in Kotlin code. It enriches the block by appending a field parameter mapping at the end.
 * @author: OUSC
 *
 * Roughly speaking, the transform will turn the following:
 *
 *     // file: Foo.kt
 *     ```kotlin
 *     fun <T: KPojo> T.foo() {
 *          val action: (KTableForSet<T>.(T) -> Unit) = { it: T ->
 *              it.username = "Hello World"
 *              it.password = "123456"
 *          }
 *          KTable<T>().action(this)
 *     }
 *     ```
 *
 * into the following equivalent representation:
 *
 *    // file: Foo.kt
 *     ```kotlin
 *     fun <T: KPojo> foo() {
 *          val action: (KTableForSet<T>.(T) -> Unit) = { it: T ->
 *              setValue(Field("username",...), "Hello World")
 *              setValue(Field("password",...), "123456")
 *              it.username = "Hello World"
 *              it.password = "123456"
 *          }
 *          KTable<T>().action(this)
 *    }
 *    ```
 */
class SetTransformer(
    private val pluginContext: IrPluginContext,
    irFunction: IrFunction,
    errorReporter: ErrorReporter
) : KTableTransformer(irFunction, errorReporter) {

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitBlock(expression: IrBlock): IrExpression {
        return if (expression.origin == null) {
            with(pluginContext) {
                DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlock {
                    +expression.statements
                    val receiver = irGet(irFunction.parameters.extensionReceiver!!)
                    val stmts = putSetParamStatements(irFunction, receiver, expression)
                    +stmts
                    super.visitBlock(expression)
                }
            }
        } else {
            super.visitBlock(expression)
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    context(context: IrPluginContext, builder: IrBlockBuilder)
    private fun putSetParamStatements(
        irFunction: IrFunction,
        receiver: IrExpression,
        element: IrElement
    ): List<IrExpression> {
        val statements = mutableListOf<IrExpression>()

        when (element) {
            is IrBlockBody -> {
                element.statements.forEach { statement ->
                    statements += putSetParamStatements(irFunction, receiver, statement)
                }
            }

            is IrBlock -> {
                if (element.origin == null) {
                    element.statements.forEach { statement ->
                        statements += putSetParamStatements(irFunction, receiver, statement)
                    }
                }
            }

            is IrTypeOperatorCall -> {
                statements += putSetParamStatements(irFunction, receiver, element.argument)
            }

            is IrReturn -> {
                statements += putSetParamStatements(irFunction, receiver, element.value)
            }

            is IrCall -> {
                val extReceiver = element.extensionReceiverArgument
                val dispReceiver = element.dispatchReceiverArgument

                when (element.origin) {
                    IrStatementOrigin.EQ -> {
                        val fieldExpr = buildFieldFromPropertyAccess(element, errorReporter)
                        val value = element.getValueArgumentSafe(0)
                        if (value != null) {
                            statements += builder.irCall(setValueMethodSymbol).apply {
                                dispatchReceiver = receiver
                                arguments[1] = fieldExpr
                                arguments[2] = value
                            }
                        }
                    }

                    IrStatementOrigin.PLUSEQ -> {
                        val target = extReceiver ?: dispReceiver!!
                        val fieldExpr = buildFieldFromPropertyAccess(target as IrCall, errorReporter)
                        val value = element.getValueArgumentSafe(0)
                        if (value != null) {
                            statements += builder.irCall(setAssignMethodSymbol).apply {
                                dispatchReceiver = receiver
                                arguments[1] = builder.irString("+")
                                arguments[2] = fieldExpr
                                arguments[3] = value
                            }
                        }
                    }

                    IrStatementOrigin.MINUSEQ -> {
                        val target = extReceiver ?: dispReceiver!!
                        val fieldExpr = buildFieldFromPropertyAccess(target as IrCall, errorReporter)
                        val value = element.getValueArgumentSafe(0)
                        if (value != null) {
                            statements += builder.irCall(setAssignMethodSymbol).apply {
                                dispatchReceiver = receiver
                                arguments[1] = builder.irString("-")
                                arguments[2] = fieldExpr
                                arguments[3] = value
                            }
                        }
                    }

                    else -> {}
                }
            }
        }

        return statements
    }
}
