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

package com.kotlinorm.compiler.fir.utils.kTableForSelect

import com.kotlinorm.compiler.helpers.referenceClass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.getSimpleFunction


const val KTABLE_FOR_SELECT_CLASS = "com.kotlinorm.beans.dsl.KTableForSelect"
const val METHOD_MANAGER_CLASS = "com.kotlinorm.methods.MethodManager"

context(IrPluginContext)
private val kTableForSelectSymbol
    get() = referenceClass(KTABLE_FOR_SELECT_CLASS)!!

context(IrPluginContext)
internal val methodManagerSymbol
    get() = referenceClass(METHOD_MANAGER_CLASS)!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val addFieldSymbol
    get() = kTableForSelectSymbol.getSimpleFunction("addField")!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val aliasSymbol
    get() = kTableForSelectSymbol.getSimpleFunction("setAlias")!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val methodTransformSymbol
    get() = methodManagerSymbol.getSimpleFunction("getMethodTransformed")!!

internal val builtinFunctions =
    arrayOf("count")