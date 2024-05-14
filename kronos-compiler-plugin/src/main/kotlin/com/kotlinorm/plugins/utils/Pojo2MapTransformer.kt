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

package com.kotlinorm.plugins.utils

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.FqName

/**
 * Transformer for data class to map
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * NOTICE:
 * Limited by compiler syntax restrictions, you can only get the specific data class parameters of listOf(vararg) in batch situations. Therefore, in this case, use reflection to convert the data class into a map.
 * 受限于编译器语法限制，只能拿到，批量情况下只能拿到listOf(vararg)的具体data class参数，因此在这种情况下使用反射将data class转为map
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * @author: OUSC
 */

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val pairSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.utils.createPair")).first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val mutableMapOfSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.utils.createMutableMap")).first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val mutableListOfSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.utils.createMutableList")).first()

context(IrBuilderWithScope, IrPluginContext, IrFunction)
fun pojo2Map(irClass: IrClass, expression: IrCall): IrFunctionAccessExpression {
    lateinit var receiver: IrVariable
    body = irBlockBody {
        irTemporary(expression.extensionReceiver).apply {
            receiver = this
        }
        +body!!.statements
    }
    val pairs = irClass.declarations.filterIsInstance<IrProperty>().map {
        applyIrCall(
            pairSymbol, irString(it.name.asString()), irGetField(irGet(receiver), it.backingField!!)
        )
    }
    return applyIrCall(
        mutableMapOfSymbol, irVararg(
            pairSymbol.owner.returnType, pairs
        )
    )
}

context(IrBuilderWithScope, IrPluginContext, IrFunction)
fun pojoList2MapList(irClass: IrClass, expression: IrCall): IrFunctionAccessExpression {
    val calls = when (expression.extensionReceiver) {
        is IrCall -> when (expression.extensionReceiver!!.asIrCall().valueArguments.single()) {
            is IrVararg -> {
                val collection = expression.extensionReceiver!!.asIrCall().valueArguments.single() as IrVararg
                collection.elements.map { pojo ->
                    val pairs = irClass.declarations.filterIsInstance<IrProperty>().map {
                        applyIrCall(
                            pairSymbol, irString(it.name.asString()), when (pojo) {
                                is IrGetValue -> irGetField(pojo, it.backingField!!)
                                is IrCallImpl -> {
                                    if (pojo.origin == IrStatementOrigin.GET_PROPERTY) {
                                        irGetField(pojo, it.backingField!!)
                                    } else {
                                        throw RuntimeException(
                                            "Kronos don't support ${pojo.javaClass.simpleName} yet"
                                        )
                                    }
                                }

                                else -> throw RuntimeException(
                                    "Kronos don't support ${pojo.javaClass.simpleName} yet"
                                )
                            }
                        )
                    }
                    applyIrCall(
                        mutableMapOfSymbol, irVararg(
                            pairSymbol.owner.returnType, pairs
                        )
                    )
                }
            }

            else -> listOf<IrCall>() // Not support
        }

        else -> listOf<IrCall>() // Not support
    }

    return applyIrCall(
        mutableListOfSymbol, irVararg(
            mutableMapOfSymbol.owner.returnType, listOf()
        )
    )
}