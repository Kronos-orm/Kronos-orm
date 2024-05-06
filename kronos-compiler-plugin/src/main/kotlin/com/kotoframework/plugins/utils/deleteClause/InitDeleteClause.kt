package com.kotoframework.plugins.utils.deleteClause

import com.kotoframework.plugins.utils.applyIrCall
import com.kotoframework.plugins.utils.kTable.getColumnName
import com.kotoframework.plugins.utils.kTable.getTableName
import com.kotoframework.plugins.utils.subType
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
private val initDeleteClauseSymbol
    get() = referenceFunctions(FqName("com.kotoframework.orm.delete.initDeleteClause"))
        .first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val initDeleteClauseListSymbol
    get() = referenceFunctions(FqName("com.kotoframework.orm.delete.initDeleteClauseList"))
        .first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val fieldSymbol
    get() = referenceClass(FqName("com.kotoframework.beans.dsl.Field"))!!

context(IrBuilderWithScope, IrPluginContext)
fun initDeleteClause(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.type.subType().getClass()!!
    return applyIrCall(
        initDeleteClauseSymbol,
        expression,
        getTableName(irClass),
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().map { getColumnName(it) }
        )
    )
}

context(IrBuilderWithScope, IrPluginContext)
fun initDeleteClauseList(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.type.subType().subType().getClass()!!
    return applyIrCall(
        initDeleteClauseListSymbol,
        expression,
        getTableName(irClass),
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().map { getColumnName(it) }
        )
    )
}