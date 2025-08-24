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

import com.kotlinorm.compiler.plugin.utils.kTableForReference.addReferenceList
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn

/**
 * KTable For Select Parser Transformer
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
 *          val action: (KTableForSelect<T>.(T) -> Unit) = { it: T ->
 *              addField(Field("username",...))
 *              addField(Field("password",...))
 *              addField(Field("createTime",...))
 *              it::username + it::password + it::createTime
 *          }
 *          KTable<T>().action(this)
 *    }
 *    ```
 */
class KTableParserForReferenceTransformer(
    private val pluginContext: IrPluginContext,
    private val irFunction: IrFunction,
) : IrElementTransformerVoidWithContext() {

    /**
     * Visits a call expression and returns an IrExpression.
     *
     * @param expression the [IrReturn] expression to visit
     * @return the transformed IrExpression
     */
    override fun visitReturn(expression: IrReturn): IrExpression {
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlock {
                +with(pluginContext) { addReferenceList(irFunction, expression) }
                +expression
        }
    }
}