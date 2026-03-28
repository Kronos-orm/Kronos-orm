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

package com.kotlinorm.compiler.plugin.utils.kTableForSelect

import com.kotlinorm.compiler.helpers.referenceClass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.getSimpleFunction

/**
 * The fully qualified name of the KTableForSelect class.
 */
const val KTABLE_FOR_SELECT_CLASS = "com.kotlinorm.beans.dsl.KTableForSelect"

/**
 * Gets the symbol for the KTableForSelect class.
 */
context(_: IrPluginContext)
private val kTableForSelectSymbol
    get() = referenceClass(KTABLE_FOR_SELECT_CLASS)!!

/**
 * Gets the symbol for the `addField` function of the KTableForSelect class.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val addSelectFieldSymbol
    get() = kTableForSelectSymbol.getSimpleFunction("addField")!!

/**
 * Gets the symbol for the `setAlias` function of the KTableForSelect class.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val aliasSymbol
    get() = kTableForSelectSymbol.getSimpleFunction("setAlias")!!