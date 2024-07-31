package com.kotlinorm.plugins.utils.kTableSortType

import com.kotlinorm.plugins.helpers.referenceClass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.getSimpleFunction

const val KTABLE_SORTABLE_CLASS = "com.kotlinorm.beans.dsl.KTableSortable"

context(IrPluginContext)
private val sortableClassSymbol
    get() = referenceClass(KTABLE_SORTABLE_CLASS)!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val addSortFieldSymbol
    get() = sortableClassSymbol.getSimpleFunction("addSortField")!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val createAscSymbol
    get() = sortableClassSymbol.getSimpleFunction("asc")!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val createDescSymbol
    get() = sortableClassSymbol.getSimpleFunction("desc")!!