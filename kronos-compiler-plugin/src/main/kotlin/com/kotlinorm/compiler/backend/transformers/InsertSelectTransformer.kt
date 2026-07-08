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

package com.kotlinorm.compiler.backend.transformers

import com.kotlinorm.compiler.core.ErrorReporter
import com.kotlinorm.compiler.core.KTableTransformer
import com.kotlinorm.compiler.core.addInsertSelectValueMethodSymbol
import com.kotlinorm.compiler.core.analyzeAndBuildInsertSelectValues
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
 * Lowers INSERT SELECT source-value lambdas into KTableForInsertSelect.addValue calls.
 */
class InsertSelectTransformer(
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
                val values = analyzeAndBuildInsertSelectValues(irFunction, expression.value, errorReporter)
                val receiver = irFunction.parameters.extensionReceiver
                    ?: return@irBlock run { +expression }

                if (values.isEmpty()) {
                    +expression
                } else {
                    values.forEach { value ->
                        +irCall(addInsertSelectValueMethodSymbol).apply {
                            dispatchReceiver = irGet(receiver)
                            arguments[1] = value
                        }
                    }
                }
            }
        }
    }
}
