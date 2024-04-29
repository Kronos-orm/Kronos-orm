package com.kotoframework.plugins.utils.updateClause

import com.kotoframework.plugins.utils.applyIrCall
import com.kotoframework.plugins.utils.asSimpleType
import com.kotoframework.plugins.utils.kTable.getColumnName
import com.kotoframework.plugins.utils.kTable.getTableName
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.name.FqName

context(IrBuilderWithScope, IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val initUpdateClauseSymbol
    get() = referenceFunctions(FqName("com.kotoframework.orm.update.initUpdateClause"))
        .first()

context(IrBuilderWithScope, IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val fieldSymbol
    get() = referenceClass(FqName("com.kotoframework.beans.dsl.Field"))!!

context(IrBuilderWithScope, IrPluginContext)
fun initUpdateClause(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.type.asSimpleType().arguments[0].typeOrFail.getClass()!!
    return applyIrCall(
        initUpdateClauseSymbol,
        expression,
        getTableName(irClass),
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().map { getColumnName(it) }
        )
    )
}