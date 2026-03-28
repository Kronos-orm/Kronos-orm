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

package com.kotlinorm.compiler.helpers

import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Provides extension properties for IrFunctionAccessExpression to access its arguments based on their kinds.
 *
 * @return The dispatch receiver argument of the function access expression.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrFunctionAccessExpression.dispatchReceiverArgument
    get() =
        arguments.getOrNull(
            symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.DispatchReceiver }
        )

/**
 * Provides extension properties for IrFunctionAccessExpression to access its arguments based on their kinds.
 *
 * @return The extension receiver argument of the function access expression.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrFunctionAccessExpression.extensionReceiverArgument
    get() =
        arguments.getOrNull(
            symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.ExtensionReceiver }
        )

/**
 * **NOTICE**: Context Parameters are **NOT SUPPORTED** by Kronos ORM at the moment.
 * Provides extension properties for IrFunctionAccessExpression to access its arguments based on their kinds.
 *
 * @return Context parameter arguments of the function access expression.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrFunctionAccessExpression.contextArguments
    get() =
        arguments.filterIndexed { index, _ ->
            symbol.owner.parameters[index].kind == IrParameterKind.Context
        }

/**
 * Provides extension properties for IrFunctionAccessExpression to access its arguments based on their kinds.
 *
 * @return Regular value arguments of the function access expression.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrFunctionAccessExpression.valueArguments
    get() =
        arguments.filterIndexed { index, _ ->
            symbol.owner.parameters[index].kind == IrParameterKind.Regular
        }