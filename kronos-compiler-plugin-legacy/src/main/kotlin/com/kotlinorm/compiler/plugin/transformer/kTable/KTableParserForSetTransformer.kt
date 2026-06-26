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

package com.kotlinorm.compiler.plugin.transformer.kTable

import com.kotlinorm.compiler.plugin.utils.kTableForSet.putFieldParamMap
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression

/**
 * KTable For Set Parser Transformer
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
class KTableParserForSetTransformer(
    private val pluginContext: IrPluginContext,
    private val irFunction: IrFunction
) : IrElementTransformerVoidWithContext() {

    /**
     * Overrides the visitBlock function to add field-parameter mappings to the block.
     *
     * @param expression the [IrBlock] expression to be visited
     * @return the transformed block expression
     */
    override fun visitBlock(expression: IrBlock): IrExpression {
        return if(expression.origin == null) {
            DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlock {
                +expression.statements
                +with(pluginContext) { putFieldParamMap(irFunction) }
                super.visitBlock(expression)
            }
        } else {
            super.visitBlock(expression)
        }
    }
}