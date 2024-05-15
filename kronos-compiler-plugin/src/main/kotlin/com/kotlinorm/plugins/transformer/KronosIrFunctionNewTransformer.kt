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

package com.kotlinorm.plugins.transformer

import com.kotlinorm.plugins.helpers.applyIrCall
import com.kotlinorm.plugins.helpers.dispatchBy
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

/**
 * Kronos Parser Transformer
 *
 * @author OUSC, Jieyao Lu
 */
class KronosIrFunctionNewTransformer(
    private val pluginContext: IrPluginContext,
    private val irFunction: IrFunction
) : IrElementTransformerVoidWithContext() {
    val scope = Scope(irFunction.symbol)
    fun irTemporary(expression: IrExpression, nameHint: String): IrVariable {
        return scope.createTemporaryVariable(expression, nameHint)
    }

    context(IrBuilderWithScope, IrPluginContext)
    private fun irSafeCall(
        target: IrExpression,
        symbol: IrFunctionSymbol,
        vararg args: IrExpression
    ): IrExpression {
        val tmpVal = irTemporary(target, nameHint = "safe_receiver")
        return irBlock(
            origin = IrStatementOrigin.SAFE_CALL,
        ) {
            +tmpVal
            +irIfThenElse(
                type = target.type,
                condition = irEquals(irGet(tmpVal), irNull()),
                thenPart = irNull(),
                elsePart = applyIrCall(symbol, *args) {
                    dispatchBy(irGet(tmpVal))
                }
            )
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        return super.visitCall(expression)
    }
}