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

package com.kotlinorm.plugins.transformer.kTable

import com.kotlinorm.plugins.utils.kTable.addFieldList
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn

/**
 * KTable Add Field Transformer
 *
 * A Kotlin compiler plugin transformer that manipulates IR elements related to table fields.
 * @author: OUSC
 */
class KTableAddFieldTransformer(
    private val pluginContext: IrPluginContext,
    private val irFunction: IrFunction
) : IrElementTransformerVoidWithContext() {

    /**
     * Visits a call expression and returns an IrExpression.
     *
     * @param expression the [IrCall] expression to visit
     * @return the transformed IrExpression
     */
    override fun visitReturn(expression: IrReturn): IrExpression {
        with(pluginContext) {
            with(irFunction) {
                with(DeclarationIrBuilder(pluginContext, irFunction.symbol)) {
                    return irBlock {
                        +addFieldList(expression)
                        +expression
                    }
                }
            }
        }
    }
}