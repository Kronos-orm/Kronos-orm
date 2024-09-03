package com.kotlinorm.plugins.transformer.kTable

import com.kotlinorm.plugins.utils.kTableSortType.addFieldSortsIr
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/14 15:29
 *
 * Roughly speaking, the transform will turn the following:
 *
 *     // file: Foo.kt
 *     fun <T: KPojo> T.foo() {
 *          val action: (KTableSortable<T>.(T) -> Unit) = { it: T ->
 *              it.username.desc() + it.password.asc() + it.age
 *          }
 *          KTableSortable<T>().action(this)
 *     }
 *
 * into the following equivalent representation:
 *
 *    // file: Foo.kt
 *     fun <T: KPojo> foo() {
 *          val action: (KTableSortable<T>.(T) -> Unit) = { it: T ->
 *              addSortField(Field("username").desc())
 *              addSortField(Field("password").asc())
 *              addSortField(Field("age"))
 *              it.username.desc() + it.password.asc() + it.age
 *          }
 *          KTableSortable<T>().action(this)
 *    }
 **/
class KTableSortableParseReturnTransformer(
    private val pluginContext: IrPluginContext,
    private val irFunction: IrFunction
) : IrElementTransformerVoidWithContext() {
    override fun visitReturn(expression: IrReturn): IrExpression {
        with(pluginContext) {
            with(irFunction) {
                with(DeclarationIrBuilder(pluginContext, irFunction.symbol)) {
                    return irBlock {
                        +addFieldSortsIr(expression)
                        +expression
                    }
                }
            }
        }
    }
}