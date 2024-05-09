package com.kotlinorm.plugins.utils.upsertClause

import com.kotlinorm.plugins.utils.*
import com.kotlinorm.plugins.utils.kTable.getColumnName
import com.kotlinorm.plugins.utils.kTable.getTableName
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
private val initUpsertClauseSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.orm.upsert.initUpsertClause"))
        .first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val initUpsertClauseListSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.orm.upsert.initUpsertClauseList"))
        .first()


context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val fieldSymbol
    get() = referenceClass(FqName("com.kotlinorm.beans.dsl.Field"))!!

context(IrBuilderWithScope, IrPluginContext)
fun initUpsertClause(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.type.subType().getClass()!!
    val createTimeStrategy =
        getValidStrategy(irClass, globalCreateTimeSymbol, CreateTimeFqName)
    val updateTimeStrategy =
        getValidStrategy(irClass, globalUpdateTimeSymbol, UpdateTimeFqName)
    val logicDeleteStrategy =
        getValidStrategy(irClass, globalLogicDeleteSymbol, LogicDeleteFqName)
    return applyIrCall(
        initUpsertClauseSymbol,
        expression,
        getTableName(irClass),
        createTimeStrategy,
        updateTimeStrategy,
        logicDeleteStrategy,
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().sortedBy { it.name }.map { getColumnName(it) }
        )
    )
}

context(IrBuilderWithScope, IrPluginContext)
fun initUpsertClauseList(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.type.subType().subType().getClass()!!
    val createTimeStrategy =
        getValidStrategy(irClass, globalCreateTimeSymbol, CreateTimeFqName)
    val updateTimeStrategy =
        getValidStrategy(irClass, globalUpdateTimeSymbol, UpdateTimeFqName)
    val logicDeleteStrategy =
        getValidStrategy(irClass, globalLogicDeleteSymbol, LogicDeleteFqName)
    return applyIrCall(
        initUpsertClauseListSymbol,
        expression,
        getTableName(irClass),
        createTimeStrategy,
        updateTimeStrategy,
        logicDeleteStrategy,
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().sortedBy { it.name }.map { getColumnName(it) }
        )
    )
}