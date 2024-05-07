package com.kotlinorm.plugins.utils

import com.kotlinorm.plugins.utils.kTable.getColumnName
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
internal val globalUpdateTimeSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.utils.getUpdateTimeStrategy")).first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
internal val globalLogicDeleteSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.utils.getLogicDeleteStrategy")).first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
internal val globalCreateTimeSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.utils.getCreateTimeStrategy"))
        .first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
internal val commonStrategySymbol
    get() = referenceClass(FqName("com.kotlinorm.beans.config.KronosCommonStrategy"))!!.constructors.first()

val UpdateTimeFqName = FqName("com.kotlinorm.beans.config.UpdateTimeStrategy")

val LogicDeleteFqName = FqName("com.kotlinorm.beans.config.LogicDeleteStrategy")

val CreateTimeFqName = FqName("com.kotlinorm.beans.config.CreateTimeStrategy")

context(IrBuilderWithScope, IrPluginContext)
internal fun getValidStrategy(irClass: IrClass, globalSymbol: IrFunctionSymbol, fqName: FqName): IrExpression? {
    var strategy: IrExpression? = applyIrCall(globalSymbol).asIrCall()
    val tableSetting = irClass.annotations.findByFqName(fqName)?.asIrCall()?.getValueArgument(1)
    if (tableSetting == null || (tableSetting is IrConst<*> && tableSetting.value == true)) {
        var annotation: IrConstructorCall?
        var config: IrConst<*>? = null
        var enabled: IrConst<*>?
        val column = irClass.properties.find {
            annotation = it.annotations.findByFqName(fqName)
            enabled = annotation?.getValueArgument(Name.identifier("enabled")) as IrConst<*>?
            config = annotation?.getValueArgument(0) as IrConst<*>?
            annotation != null && enabled?.value != false
        }
        if (column != null) {
            strategy = applyIrCall(commonStrategySymbol, irBoolean(true), getColumnName(column), config)
        }
    }
    return strategy
}