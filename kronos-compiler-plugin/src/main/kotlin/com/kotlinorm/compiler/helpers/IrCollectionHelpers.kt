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
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.isVararg
import com.kotlinorm.compiler.plugin.utils.context.KotlinBuilderContext
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression


@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrPluginContext.listOfSymbol
    get() = referenceFunctions(
        "kotlin.collections",
        "listOf"
    ).first { it.owner.valueParameters.size == 1 && it.owner.valueParameters.first().isVararg }

@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrPluginContext.pairSymbol
    get() = referenceClass(
        "kotlin.Pair"
    )!!.constructors.first()

@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrPluginContext.mutableMapOfSymbol
    get() = referenceFunctions(
        "kotlin.collections",
        "mapOf"
    ).first { it.owner.valueParameters.size == 1 && it.owner.valueParameters.first().isVararg }


fun KotlinBuilderContext.irListOf(type: IrType, elements: List<IrExpression>) =
    builder.applyIrCall(
        pluginContext.listOfSymbol,
        builder.irVararg(type, elements),
        typeArguments = arrayOf(type)
    )

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun KotlinBuilderContext.irMutableMapOf(k: IrType, v: IrType, pairs: Map<IrExpression, IrExpression>): IrFunctionAccessExpression {
    with(pluginContext){
        with(builder){
            return applyIrCall(
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
        }
    }
}

fun KotlinBuilderContext.irPairOf(first: IrType, second: IrType, pair: Pair<IrExpression?, IrExpression?>) =
    builder.applyIrCall(
        pluginContext.pairSymbol,
        pair.first,
        pair.second,
        typeArguments = arrayOf(first, second)
    )


@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrPluginContext.mapGetterSymbol
    get() = referenceClass("kotlin.collections.Map")!!.getSimpleFunction("get")!!