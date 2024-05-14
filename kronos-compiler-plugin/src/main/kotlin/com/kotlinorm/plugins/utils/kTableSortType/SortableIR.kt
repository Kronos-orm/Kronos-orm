package com.kotlinorm.plugins.utils.kTableSortType

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/14 11:00
 **/
class SortableIR(private val fieldSorts: IrExpression? = null) {

    context(IrBlockBuilder, IrPluginContext)
    fun toIrVariable(): IrVariable {
        return createSortable(fieldSorts)
    }

}