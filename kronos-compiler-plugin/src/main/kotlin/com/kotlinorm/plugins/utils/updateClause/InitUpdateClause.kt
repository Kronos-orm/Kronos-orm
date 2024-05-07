package com.kotlinorm.plugins.utils.updateClause

import com.kotlinorm.plugins.utils.*
import com.kotlinorm.plugins.utils.applyIrCall
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

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val initUpdateClauseSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.orm.update.initUpdateClause"))
        .first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val initUpdateClauseListSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.orm.update.initUpdateClauseList"))
        .first()


context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val fieldSymbol
    get() = referenceClass(FqName("com.kotlinorm.beans.dsl.Field"))!!

context(IrBuilderWithScope, IrPluginContext)
fun initUpdateClause(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.type.subType().getClass()!!
    val updateTimeStrategy =
        getValidStrategy(irClass, globalUpdateTimeSymbol, FqName("com.kotlinorm.annotations.UpdateTime"))
    val logicDeleteStrategy =
        getValidStrategy(irClass, globalLogicDeleteSymbol, FqName("com.kotlinorm.annotations.LogicDelete"))
    return applyIrCall(
        initUpdateClauseSymbol,
        expression,
        getTableName(irClass),
        updateTimeStrategy,
        logicDeleteStrategy,
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().map { getColumnName(it) }
        )
    )
}

context(IrBuilderWithScope, IrPluginContext)
fun initUpdateClauseList(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.type.subType().subType().getClass()!!
    return applyIrCall(
        initUpdateClauseListSymbol,
        expression,
        getTableName(irClass),
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().map { getColumnName(it) }
        )
    )
}