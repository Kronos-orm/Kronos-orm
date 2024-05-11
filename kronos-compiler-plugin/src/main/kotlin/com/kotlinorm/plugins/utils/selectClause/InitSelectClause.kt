package com.kotlinorm.plugins.utils.selectClause

import com.kotlinorm.plugins.utils.*
import com.kotlinorm.plugins.utils.applyIrCall
import com.kotlinorm.plugins.utils.getValidStrategy
import com.kotlinorm.plugins.utils.globalLogicDeleteSymbol
import com.kotlinorm.plugins.utils.kTable.getColumnName
import com.kotlinorm.plugins.utils.kTable.getTableName
import com.kotlinorm.plugins.utils.subType
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.name.FqName

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/8 11:38
 **/
context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val initSelectClauseSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.orm.select.initSelectClause"))
        .first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val initSelectClauseListSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.orm.select.initSelectClauseList"))
        .first()


context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val fieldSymbol
    get() = referenceClass(FqName("com.kotlinorm.beans.dsl.Field"))!!

context(IrBuilderWithScope, IrPluginContext)
fun initSelectClause(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.type.subType().getClass()!!
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
    val irClass = expression.type.subType().subType().getClass()!!
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