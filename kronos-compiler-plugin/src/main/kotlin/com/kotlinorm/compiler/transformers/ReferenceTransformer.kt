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
import com.kotlinorm.compiler.core.addRefFieldSymbol
import com.kotlinorm.compiler.core.buildFieldFromProperty
import com.kotlinorm.compiler.core.buildFieldFromPropertyRef
import com.kotlinorm.compiler.utils.extensionReceiver
import com.kotlinorm.compiler.utils.extensionReceiverArgument
import com.kotlinorm.compiler.utils.funcName
import com.kotlinorm.compiler.utils.getValueArgumentSafe
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Reference Transformer
 *
 * A Kotlin compiler plugin transformer that manipulates IR elements related to table fields.
 * @author: OUSC
 *
 * Roughly speaking, the transform will turn the following:
 *
 *     // file: Foo.kt
 *     ```kotlin
 *     fun <T: KPojo> T.foo() {
 *          val action: (KTableForReference<T>.(T) -> Unit) = { it: T ->
 *              it::prop1 + Entity::prop2 + it::prop3
 *          }
 *          KTable<T>().action(this)
 *     }
 *     ```
 *
 * into the following equivalent representation:
 *
 *    // file: Foo.kt
 *    ```kotlin
 *     fun <T: KPojo> foo() {
 *          val action: (KTableForReference<T>.(T) -> Unit) = { it: T ->
 *              addField(Field("prop1",...))
 *              addField(Field("prop2",...))
 *              addField(Field("prop3",...))
 *              it::prop1 + Entity::prop2 + it::prop3
 *          }
 *          KTable<T>().action(this)
 *    }
 *    ```
 */
class ReferenceTransformer(
    private val pluginContext: IrPluginContext,
    irFunction: IrFunction,
    errorReporter: ErrorReporter
) : KTableTransformer(irFunction, errorReporter) {

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitReturn(expression: IrReturn): IrExpression {
        if (!shouldProcessReturn(expression)) {
            return super.visitReturn(expression)
        }

        return with(pluginContext) {
            DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlock {
                val receiver = irFunction.parameters.extensionReceiver
                    ?: return@irBlock run { +expression }
                collectReferences(expression.value).forEach { fieldExpr ->
                    +irCall(addRefFieldSymbol).apply {
                        dispatchReceiver = irGet(receiver)
                        arguments[1] = fieldExpr
                    }
                }
                +expression
            }
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    context(context: IrPluginContext, builder: IrBlockBuilder)
    private fun collectReferences(expression: IrExpression): List<IrExpression> {
        val result = mutableListOf<IrExpression>()
        when {
            expression is IrPropertyReference -> {
                result += buildFieldFromPropertyRef(expression, errorReporter)
            }
            expression is IrCall && expression.origin == IrStatementOrigin.PLUS -> {
                val left = expression.extensionReceiverArgument ?: expression.dispatchReceiver
                val right = expression.getValueArgumentSafe(0)
                if (left != null) result += collectReferences(left)
                if (right != null) result += collectReferences(right)
            }
            expression is IrCall && expression.funcName() == "unaryPlus" -> {
                val recv = expression.extensionReceiverArgument ?: expression.dispatchReceiver ?: return result
                result += collectReferences(recv)
            }
            expression is IrTypeOperatorCall -> {
                result += collectReferences(expression.argument)
            }
        }
        return result
    }
}
