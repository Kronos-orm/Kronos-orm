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


context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
val listOfSymbol
    get() = referenceFunctions(
        "kotlin.collections",
        "listOf"
    ).first { it.owner.valueParameters.size == 1 && it.owner.valueParameters.first().isVararg }

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
val pairSymbol
    get() = referenceClass(
        "kotlin.Pair"
    )!!.constructors.first()

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
val mutableMapOfSymbol
    get() = referenceFunctions(
        "kotlin.collections",
        "mapOf"
    ).first { it.owner.valueParameters.size == 1 && it.owner.valueParameters.first().isVararg }


context(IrBuilderWithScope, IrPluginContext)
fun irListOf(type: IrType, elements: List<IrExpression>) =
    applyIrCall(
        listOfSymbol,
        irVararg(type, elements),
        typeArguments = arrayOf(type)
    )

context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun irMutableMapOf(k: IrType, v: IrType, pairs: Map<IrExpression, IrExpression>) =
    applyIrCall(
        mutableMapOfSymbol,
        irVararg(
            pairSymbol.owner.returnType,
            pairs.map {
                applyIrCall(
                    pairSymbol,
                    it.key,
                    it.value,
                    typeArguments = arrayOf(k, v)
                )
            }),
        typeArguments = arrayOf(k, v)
    )

context(IrBuilderWithScope, IrPluginContext)
fun irPairOf(first: IrType, second: IrType, pair: Pair<IrExpression?, IrExpression?>) =
    applyIrCall(
        pairSymbol,
        pair.first,
        pair.second,
        typeArguments = arrayOf(first, second)
    )


context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
val mapGetterSymbol
    get() = referenceClass("kotlin.collections.Map")!!.getSimpleFunction("get")!!