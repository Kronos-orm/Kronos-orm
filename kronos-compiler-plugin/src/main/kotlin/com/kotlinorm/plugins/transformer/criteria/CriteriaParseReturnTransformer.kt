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

package com.kotlinorm.plugins.transformer.criteria

import com.kotlinorm.plugins.utils.kTableConditional.setCriteriaIr
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn

/**
 * Criteria Parse Return Transformer
 *
 * Transforms the return statement of a criteria function into a setCriteria call
 * @author: Jieyao Lu
 * @create: 2024/4/23 15:10
 **/
class CriteriaParseReturnTransformer(
    private val pluginContext: IrPluginContext,
    private val irFunction: IrFunction
) : IrElementTransformerVoidWithContext() {

    /**
     * Overrides the visitReturn method to transform the return statement of a criteria function into a setCriteria call.
     *
     * @param expression the [IrReturn] expression to be visited
     * @return the transformed IrExpression
     */
    override fun visitReturn(expression: IrReturn): IrExpression {
        if (expression.returnTargetSymbol != irFunction.symbol) {
            return super.visitReturn(expression)
        }
        with(pluginContext) {
            with(irFunction) {
                return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlock {
                    +setCriteriaIr()
                }
            }
        }
    }
}