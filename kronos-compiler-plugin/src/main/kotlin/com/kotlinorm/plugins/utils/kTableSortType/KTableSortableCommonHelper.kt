package com.kotlinorm.plugins.utils.kTableSortType

import com.kotlinorm.plugins.helpers.applyIrCall
import com.kotlinorm.plugins.helpers.referenceClass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction

const val KTABLE_SORTABLE_CLASS = "com.kotlinorm.beans.dsl.KTableSortable"

context(IrPluginContext)
private val sortableClassSymbol
    get() = referenceClass(KTABLE_SORTABLE_CLASS)!!

context(IrPluginContext)
internal val addSortFieldSymbol
    get() = sortableClassSymbol.getSimpleFunction("addSortField")!!