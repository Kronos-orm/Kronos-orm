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

package com.kotlinorm.compiler.plugin.transformer.kTable

import com.kotlinorm.compiler.plugin.utils.context.withBlock
import com.kotlinorm.compiler.plugin.utils.kTableForSort.addFieldSortsIr
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn

/**
 * KTable For Sort Parser Transformer
 *
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/14 15:29
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
 **/
class KTableParserForSortReturnTransformer(
    private val pluginContext: IrPluginContext,
    private val irFunction: IrFunction
) : IrElementTransformerVoidWithContext() {
    override fun visitReturn(expression: IrReturn): IrExpression {
        with(DeclarationIrBuilder(pluginContext, irFunction.symbol)) {
            return irBlock {
                +withBlock(pluginContext) { addFieldSortsIr(irFunction, expression) }
                +expression
            }
        }
    }
}