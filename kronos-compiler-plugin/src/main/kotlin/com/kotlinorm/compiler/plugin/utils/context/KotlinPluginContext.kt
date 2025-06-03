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

package com.kotlinorm.compiler.plugin.utils.context

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

class KotlinPluginContext(
    var pluginContext: IrPluginContext,
) {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    internal val IrCall.correspondingName
        get() = symbol.owner.correspondingPropertySymbol?.owner?.name

    /**
     * Returns a string representing the function name based on the IrExpression type and origin, with optional logic for setNot parameter.
     *
     * @param setNot a boolean value indicating whether to add the "not" prefix to the function name
     * @return a string representing the function name
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrExpression.funcName(setNot: Boolean = false): String {
        return when (this) {
            is IrCall -> when (origin) {
                IrStatementOrigin.EQEQ, IrStatementOrigin.EXCLEQ -> "equal"
                IrStatementOrigin.GT -> "gt"
                IrStatementOrigin.LT -> "lt"
                IrStatementOrigin.GTEQ -> "ge"
                IrStatementOrigin.LTEQ -> "le"
                else -> correspondingName?.asString() ?: symbol.owner.name.asString()
            }

            is IrWhen -> when {
                (origin == IrStatementOrigin.OROR && !setNot) || (origin == IrStatementOrigin.ANDAND && setNot) -> "OR"
                (origin == IrStatementOrigin.ANDAND && !setNot) || (origin == IrStatementOrigin.OROR && setNot) -> "AND"
                else -> origin.toString()
            }

            else -> ""
        }

    }
}

fun <T> IrPluginContext.withContext(action: KotlinPluginContext.() -> T) = action(
    KotlinPluginContext(this)
)
