package com.kotoframework.plugins.utils.updateClause

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class)
fun initUpdateClause(pluginContext: IrPluginContext, expression: IrCall): IrCall {
    val builder = DeclarationIrBuilder(pluginContext, expression.symbol)
    return builder.irCall(
        pluginContext.referenceFunctions(FqName("com.kotoframework.orm.update.initUpdateClause"))
            .first(),
    ).apply {
        val irClass = (expression.type as IrSimpleType).arguments[0].typeOrFail.getClass()!!
        val annotations = irClass.annotations
        val tableAnnotation =
            annotations.firstOrNull { it.symbol.descriptor.containingDeclaration.fqNameSafe == FqName("com.kotoframework.annotations.Table") }
        putValueArgument(0, expression)
        putValueArgument(
            1,
            tableAnnotation?.getValueArgument(0) ?: builder.irCall(
                pluginContext.referenceFunctions(FqName("com.kotoframework.utils.tableK2db")).first()
            ).apply {
                putValueArgument(0,
                    builder.irString(
                        irClass.name.asString()
                    )
                )
            }
        )
        putValueArgument(
            2,
            builder.irVararg(
                pluginContext.referenceClass(FqName("com.kotoframework.beans.dsl.Field"))!!.defaultType,
                getAllMemberProperties(irClass).map { it.toField(builder, pluginContext) }
            )
        )
    }
}

fun getAllMemberProperties(irClass: IrClass): List<IrProperty> {
    val memberProperties = mutableListOf<IrProperty>()

    irClass.declarations.forEach { declaration ->
        if (declaration is IrProperty) {
            memberProperties.add(declaration)
        }
    }

    return memberProperties
}

@OptIn(ObsoleteDescriptorBasedAPI::class, FirIncompatiblePluginAPI::class)
fun IrProperty.toField(builder: DeclarationIrBuilder, pluginContext: IrPluginContext): IrExpression {
    val annotations = annotations
    val columnAnnotation =
        annotations.firstOrNull { it.symbol.descriptor.containingDeclaration.fqNameSafe == FqName("com.kotoframework.annotations.Column") }
    val columnName = columnAnnotation?.getValueArgument(0) ?: builder.irCall(
        pluginContext.referenceFunctions(FqName("com.kotoframework.utils.fieldK2db")).first()
    ).apply {
        putValueArgument(0, builder.irString(name.asString()))
    }
    return builder.irCall(pluginContext.referenceClass(FqName("com.kotoframework.beans.dsl.Field"))!!.constructors.first())
        .apply {
            putValueArgument(0, columnName)
            putValueArgument(1, builder.irString(name.asString()))
    }
}