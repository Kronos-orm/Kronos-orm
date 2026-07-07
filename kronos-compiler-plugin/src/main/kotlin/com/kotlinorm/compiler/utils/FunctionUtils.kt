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

package com.kotlinorm.compiler.utils

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName

/**
 * Function-related utility functions
 */

/**
 * Gets the corresponding property name for an IrCall if it's a property getter
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrCall.correspondingName
    get() = symbol.owner.correspondingPropertySymbol?.owner?.name

/**
 * Gets the function name from an IrCall
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrCall.funcName() = correspondingName?.asString() ?: this.symbol.owner.name.asString()

/**
 * Gets the SQL function identifier for a FunctionHandler extension.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrFunction.kronosFunctionName(): String {
    val annotation = findAnnotation(KronosFunctionAnnotationFqName)
    return getAnnotationStringValue(annotation, 0)?.takeIf { it.isNotBlank() } ?: name.asString()
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrCall.kronosFunctionName(): String =
    correspondingName?.asString() ?: symbol.owner.kronosFunctionName()

/**
 * Checks if an IrExpression is a Kronos function call (FunctionHandler extension)
 * 
 * Example: f.rand(), f.trunc(it.score, 2)
 * where f is an instance of FunctionHandler
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrExpression?.isKronosFunction(): Boolean {
    val call = this as? IrCall ?: return false
    return call.extensionReceiverArgument?.type?.classFqName == FunctionHandlerFqName
}
