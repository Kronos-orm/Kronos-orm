package com.kotlinorm.plugins.utils

import com.kotlinorm.plugins.utils.kTable.getColumnName
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName


context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
internal val globalUpdateTimeSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.utils.getUpdateTimeStrategy"))
        .first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
internal val globalLogicDeleteSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.utils.getLogicDeleteStrategy"))
        .first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
internal val commonStrategySymbol
    get() = referenceClass(FqName("com.kotlinorm.beans.config.KronosCommonStrategy"))!!.constructors.first()


context(IrBuilderWithScope, IrPluginContext)
internal fun getValidStrategy(irClass: IrClass, globalSymbol: IrFunctionSymbol, fqName: FqName): IrExpression? {
    var strategy: IrExpression? = applyIrCall(globalSymbol).asIrCall()
    val tableSetting = irClass.annotations.findByFqName(fqName)?.asIrCall()
        ?.getValueArgument(1)
    if (tableSetting == null || (tableSetting is IrConst<*> && tableSetting.value == true)) {
        val column = irClass.properties.find { it.annotations.findByFqName(fqName) != null }
        if (column != null) {
            strategy = applyIrCall(commonStrategySymbol, irBoolean(true), getColumnName(column))
        }
    }
    return strategy
}