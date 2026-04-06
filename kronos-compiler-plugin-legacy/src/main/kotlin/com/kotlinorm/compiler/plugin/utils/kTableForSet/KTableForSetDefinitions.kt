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

package com.kotlinorm.compiler.plugin.utils.kTableForSet

import com.kotlinorm.compiler.helpers.referenceClass
import com.kotlinorm.compiler.helpers.valueParameters
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getSimpleFunction

/**
 * The fully qualified name of the KTableForSet class.
 */
const val KTABLE_FOR_SET_CLASS = "com.kotlinorm.beans.dsl.KTableForSet"

/**
 * Gets the symbol for the KTableForSet class.
 */
context(_: IrPluginContext)
private val kTableForSetSymbol
    get() = referenceClass(KTABLE_FOR_SET_CLASS)!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val setValueSymbol
    get() = kTableForSetSymbol.functions.first { it.owner.name.toString() == "setValue" && it.owner.parameters.valueParameters.size == 2 }

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val setAssignSymbol
    get() = kTableForSetSymbol.getSimpleFunction("setAssign")!!