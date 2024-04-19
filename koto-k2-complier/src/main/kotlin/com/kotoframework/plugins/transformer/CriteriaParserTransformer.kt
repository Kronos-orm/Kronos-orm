package com.kotoframework.plugins.transformer

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName

class CriteriaParserTransformer(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
    private val kTableClazz = "com.kotoframework.beans.dsl.kTable"

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        when(declaration.extensionReceiverParameter?.symbol?.descriptor?.type?.getJetTypeFqName(false)){
            kTableClazz -> {
                declaration.body = irSetCriteria(declaration, declaration.body!!)
            }
        }
        return super.visitFunctionNew(declaration)
    }

    private fun irSetCriteria(
        irFunction: IrFunction,
        irBody: IrBody,
    ): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlockBody {
            +irBlock(resultType = irFunction.returnType) {
                for (statement in irBody.statements) { //原有方法体中的表达式
                    +statement
                }
            }.transform(CriteriaParserReturnTransformer(pluginContext, irFunction), null)

        }
    }
}