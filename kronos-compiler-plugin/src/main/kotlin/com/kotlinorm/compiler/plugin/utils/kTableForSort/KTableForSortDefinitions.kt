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

const val KTABLE_FOR_SORT_CLASS = "com.kotlinorm.beans.dsl.KTableForSort"

private val IrPluginContext.sortableClassSymbol
    get() = referenceClass(KTABLE_FOR_SORT_CLASS)!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrPluginContext.addSortFieldSymbol
    get() = sortableClassSymbol.getSimpleFunction("addSortField")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrPluginContext.createAscSymbol
    get() = sortableClassSymbol.getSimpleFunction("asc")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrPluginContext.createDescSymbol
    get() = sortableClassSymbol.getSimpleFunction("desc")!!