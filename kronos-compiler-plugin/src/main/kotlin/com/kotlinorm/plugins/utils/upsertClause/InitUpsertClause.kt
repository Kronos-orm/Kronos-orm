package com.kotlinorm.plugins.utils.upsertClause

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

const val UPSERT_CLAUSE_CLASS = "com.kotlinorm.orm.upsert.UpsertClause"
const val UPSERT_FUNCTION = "update"
const val UPSERT_EXCEPT_FUNCTION = "updateExcept"

context(IrPluginContext)
private val initUpsertClauseSymbol
    get() = referenceFunctions("com.kotlinorm.orm.upsert", "initUpsertClause")
        .first()

context(IrPluginContext)
private val initUpsertClauseListSymbol
    get() = referenceFunctions("com.kotlinorm.orm.upsert", "initUpsertClauseList")
        .first()

context(IrPluginContext)
private val fieldSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.Field")!!

context(IrBuilderWithScope, IrPluginContext)
fun initUpsertClause(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.subTypeClass()
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
    val irClass = expression.subTypeClass(2)
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