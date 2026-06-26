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

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.isVararg

/**
 * These helpers are a workaround for the lack of built-in support for certain collection
 * operations in the Kotlin IR. They provide a way to create IR expressions for common collection
 * functions like listOf, mutableMapOf, and Pair.
 */

/**
 * Gets the symbol for the `get` function of the `Map` class.
 *
 * @return The symbol for the `get` function.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext) val mapGetterSymbol
    get() = referenceClass("kotlin.collections.Map")!!.getSimpleFunction("get")!!

/**
 * Gets the symbol for the `listOf` function that takes a vararg parameter.
 *
 * @return The symbol for the `listOf` function.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext) val listOfSymbol
    get() = referenceFunctions(
        "kotlin.collections",
        "listOf"
    ).first { it.owner.parameters.single().isVararg }

/**
 * Gets the symbol for the `Pair` class constructor.
 *
 * @return The symbol for the `Pair` constructor.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext) val pairSymbol
    get() = referenceClass("kotlin.Pair")!!.constructors.first()

/**
 * Gets the symbol for the `mapOf` function that takes a vararg parameter.
 *
 * @return The symbol for the `mapOf` function.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext) val mutableMapOfSymbol
    get() = referenceFunctions(
        "kotlin.collections",
        "mapOf"
    ).first { it.owner.parameters.single().isVararg }

/**
 * Creates an IR expression representing a call to `listOf` with the given elements.
 *
 * @param type The type of the elements in the list.
 * @param elements The elements to include in the list.
 * @return An IR expression representing the list.
 */
context(builder: IrBuilderWithScope, _: IrPluginContext)
fun irListOf(type: IrType, elements: List<IrExpression>) =
    listOfSymbol(
        builder.irVararg(type, elements), types = listOf(type)
    )

/**
 * Creates an IR expression representing a call to `mutableMapOf` with the given key-value pairs.
 *
 *
 */
@UnsafeDuringIrConstructionAPI
context(builder: IrBuilderWithScope, _: IrPluginContext) fun irMutableMapOf(
    k: IrType,
    v: IrType,
    pairs: Map<IrExpression, IrExpression>
) = listOf(k, v).let { types ->
    mutableMapOfSymbol(
        builder.irVararg(
        pairSymbol.owner.returnType, pairs.map {
            pairSymbol(it.key, it.value, types = types)
        }), types = types
    )
}

/**
 * Creates an IR expression representing a `Pair` with the given first and second elements.
 */
@UnsafeDuringIrConstructionAPI
context(_: IrBuilderWithScope, _: IrPluginContext) fun irPairOf(
    first: IrType,
    second: IrType,
    pair: Pair<IrExpression?, IrExpression?>
) = pairSymbol(
        pair.first,
        pair.second, types = listOf(first, second)
    )