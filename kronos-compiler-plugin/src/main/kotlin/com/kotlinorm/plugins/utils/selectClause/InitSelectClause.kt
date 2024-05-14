package com.kotlinorm.plugins.utils.selectClause

import com.kotlinorm.plugins.helpers.applyIrCall
import com.kotlinorm.plugins.helpers.referenceClass
import com.kotlinorm.plugins.helpers.referenceFunctions
import com.kotlinorm.plugins.utils.*
import com.kotlinorm.plugins.utils.kTable.getColumnName
import com.kotlinorm.plugins.utils.kTable.getTableName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.defaultType

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/8 11:38
 **/

const val SELECT_CLAUSE_CLASS = "com.kotlinorm.orm.select.SelectClause"
const val SELECT_FUNCTION = "select"

context(IrPluginContext)
private val initSelectClauseSymbol
    get() = referenceFunctions("com.kotlinorm.orm.select", "initSelectClause")
        .first()

context(IrPluginContext)
private val initSelectClauseListSymbol
    get() = referenceFunctions("com.kotlinorm.orm.select", "initSelectClauseList")
        .first()


context(IrPluginContext)
private val fieldSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.Field")!!

context(IrBuilderWithScope, IrPluginContext)
fun initSelectClause(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.subTypeClass()
    val logicDeleteStrategy =
        getValidStrategy(irClass, globalLogicDeleteSymbol, LogicDeleteFqName)
    return applyIrCall(
        initSelectClauseSymbol,
        expression,
        getTableName(irClass),
        logicDeleteStrategy,
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().map { getColumnName(it) }
        )
    )
}

context(IrBuilderWithScope, IrPluginContext)
fun initSelectClauseList(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.subTypeClass(2)
    val logicDeleteStrategy =
        getValidStrategy(irClass, globalLogicDeleteSymbol, LogicDeleteFqName)
    return applyIrCall(
        initSelectClauseListSymbol,
        expression,
        getTableName(irClass),
        logicDeleteStrategy,
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().map { getColumnName(it) }
        )
    )
}