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
import org.jetbrains.kotlin.ir.declarations.IrValueParameter

/**
 * Finds and returns the dispatch receiver parameter from a list of IrValueParameters.
 *
 * @return The dispatch receiver parameter, or null if not found.
 */
val List<IrValueParameter>.dispatchReceiver
    get() =
        find { it.kind == IrParameterKind.DispatchReceiver }

/**
 * Finds and returns the extension receiver parameter from a list of IrValueParameters.
 *
 * @return The extension receiver parameter, or null if not found.
 */
val List<IrValueParameter>.extensionReceiver
    get() =
        find { it.kind == IrParameterKind.ExtensionReceiver }

/**
 * **NOTICE**: Context Parameters are **NOT SUPPORTED** by Kronos ORM at the moment.
 * Filters and returns the context receiver parameters from a list of IrValueParameters.
 *
 * @return A list of context receiver parameters.
 */
val List<IrValueParameter>.contextReceiver
    get() =
        filter { it.kind == IrParameterKind.Context }

/**
 * Filters and returns the regular value parameters from a list of IrValueParameters.
 *
 * @return A list of regular value parameters.
 */
val List<IrValueParameter>.valueParameters
    get() =
        filter { it.kind == IrParameterKind.Regular }