package com.kotlinorm.plugins.utils.kTableSortType

import com.kotlinorm.plugins.utils.applyIrCall
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.name.FqName

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val sortableClassSymbol
    get() = referenceClass(FqName("com.kotlinorm.beans.dsl.KTableSortable"))!!

context(IrPluginContext)
internal val fieldSortsSetterSymbol
    get() = sortableClassSymbol.getPropertySetter("fieldSorts")!!

context(IrPluginContext)
internal val createDescSymbol
    get() = sortableClassSymbol.getPropertyGetter("desc")!!

context(IrPluginContext)
internal val createAscSymbol
    get() = sortableClassSymbol.getPropertyGetter("asc")!!

context(IrBlockBuilder , IrPluginContext)
fun createSortable(
    fieldSorts: IrExpression? = null
): IrVariable {
    return irTemporary(
        applyIrCall(
            sortableClassSymbol.constructors.first(),
            fieldSorts
        )
    )
}