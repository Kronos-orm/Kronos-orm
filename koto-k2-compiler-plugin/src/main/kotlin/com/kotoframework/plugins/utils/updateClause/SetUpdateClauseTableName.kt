package com.kotoframework.plugins.utils.updateClause

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.name.FqName

@OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class)
fun setUpdateClauseTableName(pluginContext: IrPluginContext, expression: IrCall): IrExpression {
    val irBuilder = DeclarationIrBuilder(pluginContext, expression.symbol)
    return irBuilder.irCall(
        pluginContext.referenceFunctions(FqName("com.kotoframework.orm.update.setUpdateClauseTableName"))
            .first(),
    ).apply {
        putValueArgument(0, expression)
        putValueArgument(
            1, irBuilder.irCall(
                pluginContext.referenceFunctions(FqName("com.kotoframework.utils.tableK2db")).first()
            ).apply {
                val name = (expression.valueArguments.first() as IrFunctionExpression).type.toKotlinType().arguments.first().type.arguments.first().type.getKotlinTypeFqName(false)
                putValueArgument(0,
                    irBuilder.irString(
                        name
                    )
                )
            }
        )
    }
}