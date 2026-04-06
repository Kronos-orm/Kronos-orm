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
import com.kotlinorm.compiler.core.addSortFieldMethodSymbol
import com.kotlinorm.compiler.core.ascMethodSymbol
import com.kotlinorm.compiler.core.buildFieldFromProperty
import com.kotlinorm.compiler.core.buildFieldFromPropertyAccess
import com.kotlinorm.compiler.core.descMethodSymbol
import com.kotlinorm.compiler.core.isColumnType
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
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.properties

/**
 * Sort Transformer
 *
 * @author: Jieyao Lu
 *
 * Roughly speaking, the transform will turn the following:
 *
 *     // file: Foo.kt
 *     ```kotlin
 *     fun <T: KPojo> T.foo() {
 *          val action: (KTableSortable<T>.(T) -> Unit) = { it: T ->
 *              it.username.desc() + it.password.asc() + it.age
 *          }
 *          KTableSortable<T>().action(this)
 *     }
 *     ```
 *
 * into the following equivalent representation:
 *
 *    // file: Foo.kt
 *    ```kotlin
 *     fun <T: KPojo> foo() {
 *          val action: (KTableSortable<T>.(T) -> Unit) = { it: T ->
 *              addSortField(Field("username",...).desc())
 *              addSortField(Field("password",...).asc())
 *              addSortField(Field("age",...))
 *              it.username.desc() + it.password.asc() + it.age
 *          }
 *          KTableSortable<T>().action(this)
 *    }
 *    ```
 */
class SortTransformer(
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
                collectSortFields(irFunction, expression.value).forEach { (fieldExpr, asc) ->
                    val sortedField = irCall(if (asc) ascMethodSymbol else descMethodSymbol).apply {
                        dispatchReceiver = irGet(receiver)
                        arguments[1] = fieldExpr
                    }
                    +irCall(addSortFieldMethodSymbol).apply {
                        dispatchReceiver = irGet(receiver)
                        arguments[1] = sortedField
                    }
                }
                +expression
            }
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    context(context: IrPluginContext, builder: IrBlockBuilder)
    private fun collectSortFields(
        irFunction: IrFunction,
        expression: IrExpression
    ): List<Pair<IrExpression, Boolean>> {
        val result = mutableListOf<Pair<IrExpression, Boolean>>()
        when {
            expression is IrCall && expression.origin == IrStatementOrigin.PLUS -> {
                val left = expression.extensionReceiverArgument ?: expression.dispatchReceiver
                val right = expression.getValueArgumentSafe(0)
                if (left != null) result += collectSortFields(irFunction, left)
                if (right != null) result += collectSortFields(irFunction, right)
            }
            expression is IrCall && expression.funcName() == "desc" -> {
                val receiver = expression.extensionReceiverArgument ?: expression.dispatchReceiver ?: return result
                if (receiver is IrCall && receiver.origin == IrStatementOrigin.GET_PROPERTY) {
                    result += buildFieldFromPropertyAccess(receiver, errorReporter) to false
                }
            }
            expression is IrCall && expression.funcName() == "asc" -> {
                val receiver = expression.extensionReceiverArgument ?: expression.dispatchReceiver ?: return result
                if (receiver is IrCall && receiver.origin == IrStatementOrigin.GET_PROPERTY) {
                    result += buildFieldFromPropertyAccess(receiver, errorReporter) to true
                }
            }
            expression is IrCall && expression.origin == IrStatementOrigin.GET_PROPERTY -> {
                result += buildFieldFromPropertyAccess(expression, errorReporter) to true
            }
            expression is IrGetValue -> {
                // bare `it` -> all columns asc
                val irClass = expression.type.classOrNull?.owner ?: return result
                irClass.properties.filter { it.isColumnType() }.forEach { prop ->
                    result += buildFieldFromProperty(prop) to true
                }
            }
        }
        return result
    }
}
