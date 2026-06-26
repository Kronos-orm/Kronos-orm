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
import com.kotlinorm.compiler.core.addFieldMethodSymbol
import com.kotlinorm.compiler.core.analyzeAndBuildFields
import com.kotlinorm.compiler.utils.extensionReceiver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Select Transformer
 *
 * A Kotlin compiler plugin transformer that manipulates IR elements related to table fields.
 * @author: OUSC
 *
 * Roughly speaking, the transform will turn the following:
 *
 *     // file: Foo.kt
 *     ```kotlin
 *     fun <T: KPojo> T.foo() {
 *          val action: (KTableForSelect<T>.(T) -> Unit) = { it: T ->
 *              it.username + it.password + it.createTime.as_("time")
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
 *          val action: (KTableForSelect<T>.(T) -> Unit) = { it: T ->
 *              addField(Field("username",...))
 *              addField(Field("password",...))
 *              addField(Field("createTime",...).setAlias("time"))
 *              it.username + it.password + it.createTime.as_("time")
 *          }
 *          KTable<T>().action(this)
 *    }
 *    ```
 */
class SelectTransformer(
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
                val fields = analyzeAndBuildFields(irFunction, expression.value, errorReporter)
                val receiver = irFunction.parameters.extensionReceiver
                    ?: return@irBlock run { +expression }

                fields.forEach { field ->
                    +irCall(addFieldMethodSymbol).apply {
                        dispatchReceiver = irGet(receiver)
                        arguments[1] = field
                    }
                }
                +expression
            }
        }
    }
}
