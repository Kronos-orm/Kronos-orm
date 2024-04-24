package com.kotoframework.plugins.utils.updateClause

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class)
fun setUpdateClauseTableName(pluginContext: IrPluginContext, expression: IrCall): IrExpression {
    val irBuilder = DeclarationIrBuilder(pluginContext, expression.symbol)
    return irBuilder.irCall(
        pluginContext.referenceFunctions(FqName("com.kotoframework.orm.update.setUpdateClauseTableName"))
            .first(),
    ).apply {
        val irClass = (expression.type as IrSimpleType).arguments[0].typeOrFail.getClass()!!
        val annotations = irClass.annotations
        val tableAnnotation =
            annotations.firstOrNull { it.symbol.descriptor.containingDeclaration.fqNameSafe == FqName("com.kotoframework.annotations.Table") }
        putValueArgument(0, expression)
        putValueArgument(
            1,
            tableAnnotation?.getValueArgument(0) ?: irBuilder.irCall(
                pluginContext.referenceFunctions(FqName("com.kotoframework.utils.tableK2db")).first()
            ).apply {
                putValueArgument(0,
                    irBuilder.irString(
                        irClass.name.asString()
                    )
                )
            }
        )
    }
}