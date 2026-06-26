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
import com.kotlinorm.compiler.core.analyzeAndBuildCriteria
import com.kotlinorm.compiler.core.buildCriteriaNode
import com.kotlinorm.compiler.core.setCriteriaMethodSymbol
import com.kotlinorm.compiler.utils.ErrorMessages
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
 * Condition Transformer
 *
 * Transforms the return statement of a criteria function into a setCriteria call.
 * @author: Jieyao Lu
 *
 * Roughly speaking, the transform will turn the following:
 *
 *     // file: Foo.kt
 *     fun <T: KPojo> T.foo() {
 *          val action: (KTableConditional<T>.(T) -> Unit) = { it: T ->
 *              it.name == "Hello World"
 *          }
 *          KTableConditional<T>().action(this)
 *     }
 *
 * into the following equivalent representation:
 *
 *    // file: Foo.kt
 *     fun <T: KPojo> T.foo() {
 *          val action: (KTableConditional<T>.(T) -> Unit) = { it: T ->
 *              var tmp0 = Criteria(Field(""), ROOT, false, null, smart, mutableListOf())
 *              var tmp1 = Criteria(Field("name",...), eq, false, "Hello World", t, smart, mutableListOf())
 *              tmp0.children.add(tmp1)
 *              setCriteria(tmp0)
 *              it.name == "Hello World"
 *          }
 *          KTableConditional<T>().action(this)
 *    }
 */
class ConditionTransformer(
    private val pluginContext: IrPluginContext,
    irFunction: IrFunction,
    errorReporter: ErrorReporter
) : KTableTransformer(irFunction, errorReporter) {

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitReturn(expression: IrReturn): IrExpression {
        if (!shouldProcessReturn(expression)) {
            return super.visitReturn(expression)
        }

        return try {
            with(pluginContext) {
                DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlock {
                    val inner = analyzeAndBuildCriteria(irFunction, expression.value, errorReporter)
                    val receiver = irFunction.parameters.extensionReceiver

                    if (inner != null && receiver != null) {
                        // Always wrap in a ROOT criteria so consumers can access rst.children.get(0)
                        val rootCriteria = buildCriteriaNode(type = "ROOT", not = false, children = listOf(inner))
                        +irCall(setCriteriaMethodSymbol).apply {
                            dispatchReceiver = irGet(receiver)
                            arguments[1] = rootCriteria
                        }
                    }
                    // Condition transformer does NOT preserve the original return
                }
            }
        } catch (e: RuntimeException) {
            errorReporter.reportError(
                expression,
                ErrorMessages.failedToTransformCondition(e.message),
                e
            )
            expression
        }
    }
}
