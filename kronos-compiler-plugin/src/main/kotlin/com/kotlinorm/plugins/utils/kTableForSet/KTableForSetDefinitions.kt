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

package com.kotlinorm.plugins.utils.kTableForSet

import com.kotlinorm.plugins.helpers.*
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.*


const val KTABLE_FOR_SET_CLASS = "com.kotlinorm.beans.dsl.KTableForSet"

context(IrPluginContext)
private val kTableForSetSymbol
    get() = referenceClass(KTABLE_FOR_SET_CLASS)!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val setValueSymbol
    get() = kTableForSetSymbol.functions.first { it.owner.name.toString() == "setValue" && it.owner.valueParameters.size == 2 }

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val setAssignSymbol
    get() = kTableForSetSymbol.getSimpleFunction("setAssign")!!