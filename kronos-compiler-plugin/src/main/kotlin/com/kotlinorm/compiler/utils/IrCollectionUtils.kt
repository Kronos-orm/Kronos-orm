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

import com.kotlinorm.compiler.core.listOfFunctionSymbol
import com.kotlinorm.compiler.core.mutableMapOfFunctionSymbol
import com.kotlinorm.compiler.core.pairClassSymbol
import com.kotlinorm.compiler.core.pairConstructorSymbol
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith

/**
 * Collection-related IR utility functions
 * 
 * Provides helpers for creating IR expressions for collections like List and Pair
 */

/**
 * Creates an IR expression representing a call to `listOf` with the given elements
 *
 * @param type The type of the elements in the list
 * @param elements The elements to include in the list
 * @return An IR expression representing the list
 */
context(builder: IrBuilderWithScope, context: IrPluginContext)
fun irListOf(type: IrType, elements: List<IrExpression>): IrExpression {
    return builder.irCall(listOfFunctionSymbol).apply {
        typeArguments[0] = type
        arguments[0] = builder.irVararg(type, elements)
    }
}

/**
 * Creates an IR expression representing a `Pair` with the given first and second elements
 *
 * @param firstType The type of the first element
 * @param secondType The type of the second element
 * @param first The first element expression
 * @param second The second element expression
 * @return An IR expression representing the Pair
 */
context(builder: IrBuilderWithScope, context: IrPluginContext)
fun irPairOf(
    firstType: IrType,
    secondType: IrType,
    first: IrExpression?,
    second: IrExpression?
): IrExpression {
    return builder.irCall(pairConstructorSymbol).apply {
        typeArguments[0] = firstType
        typeArguments[1] = secondType
        arguments[0] = first
        arguments[1] = second
    }
}

/**
 * Creates an IR expression representing a call to `mutableMapOf` with the given key-value pairs
 *
 * @param keyType The type of the keys
 * @param valueType The type of the values
 * @param entries The key-value pairs to include in the map
 * @return An IR expression representing the mutable map
 */
context(builder: IrBuilderWithScope, context: IrPluginContext)
fun irMutableMapOf(keyType: IrType, valueType: IrType, entries: Map<IrExpression, IrExpression>): IrExpression {
    val pairType = pairClassSymbol.typeWith(keyType, valueType)
    val pairs = entries.map { (k, v) ->
        builder.irCall(pairConstructorSymbol).apply {
            typeArguments[0] = keyType
            typeArguments[1] = valueType
            arguments[0] = k
            arguments[1] = v
        }
    }
    return builder.irCall(mutableMapOfFunctionSymbol).apply {
        typeArguments[0] = keyType
        typeArguments[1] = valueType
        arguments[0] = builder.irVararg(pairType, pairs)
    }
}
