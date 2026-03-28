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

package com.kotlinorm.compiler.plugin.utils.kTableForSort

import com.kotlinorm.compiler.helpers.referenceClass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.getSimpleFunction

/**
 * KTableForSort class name.
 */
const val KTABLE_FOR_SORT_CLASS = "com.kotlinorm.beans.dsl.KTableForSort"

/**
 * KTableForSort class symbol.
 */
context(_: IrPluginContext)
private val sortableClassSymbol
    get() = referenceClass(KTABLE_FOR_SORT_CLASS)!!

/**
 * Gets the symbol for the addSortField function in the KTableForSort class.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val addSortFieldSymbol
    get() = sortableClassSymbol.getSimpleFunction("addSortField")!!

/**
 * Gets the symbol for the asc function in the KTableForSort class.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val createAscSymbol
    get() = sortableClassSymbol.getSimpleFunction("asc")!!

/**
 * Gets the symbol for the desc function in the KTableForSort class.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val createDescSymbol
    get() = sortableClassSymbol.getSimpleFunction("desc")!!