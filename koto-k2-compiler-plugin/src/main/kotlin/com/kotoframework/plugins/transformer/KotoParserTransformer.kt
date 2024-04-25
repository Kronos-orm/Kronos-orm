package com.kotoframework.plugins.transformer

import com.kotoframework.plugins.transformer.criteria.CriteriaParseReturnTransformer
import com.kotoframework.plugins.transformer.kTable.KTableFieldAddReturnTransformer
import com.kotoframework.plugins.transformer.kTable.KTableParamPutBlockTransformer
import com.kotoframework.plugins.utils.kTableConditional.funcName
import com.kotoframework.plugins.utils.updateClause.setUpdateClauseTableName
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.name.FqName

/**
 * `KotoParserTransformer` manipulates function declarations in Kotlin's Intermediate Representation (IR) based on specific type conditions.
 * This transformer focuses on functions that have a `KTable` as their extension receiver, applying custom IR transformations to modify the function body.
 * `KotoParserTransformer` 根据特定类型条件操作 Kotlin 中间表示 (IR) 的函数声明。
 * 此转换器专注于那些以 `KTable` 作为扩展接收器的函数，应用自定义 IR 转换以修改函数体。
 */
class KotoParserTransformer(
    // Plugin context, includes essential information for IR transformation
    // 插件上下文，包含 IR 转换所需的必要信息
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
    private val kTableClass = "com.kotoframework.beans.dsl.KTable"
    private val updateClauseClass = "com.kotoframework.orm.update.UpdateClause"
    private val kTableConditionalClazz = "com.kotoframework.beans.dsl.KTableConditional"

    @OptIn(FirIncompatiblePluginAPI::class)
    fun IrPluginContext.printlnFunc(): IrSimpleFunctionSymbol = referenceFunctions(FqName("kotlin.io.println")).single {
        val parameters = it.owner.valueParameters
        parameters.size == 1 && parameters[0].type == irBuiltIns.anyNType
    }

    /**
     * Checks each function's extension receiver type during its creation.
     * If it matches `KTable`, applies a specific transformation to the function body.
     * 在函数创建时检查每个函数的扩展接收器类型。
     * 如果匹配 `KTable`，则对函数体应用特定的转换。
     */
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        when (declaration.extensionReceiverParameter?.symbol?.descriptor?.returnType?.getKotlinTypeFqName(false)) {
            kTableClass -> {
                declaration.body = transformKTable(declaration)
            }
            kTableConditionalClazz -> {
                declaration.body = transformKTableConditional(declaration)
            }
        }
        return super.visitFunctionNew(declaration)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        when {
            expression.symbol.descriptor.returnType?.getKotlinTypeFqName(false) == updateClauseClass && expression.funcName() == "update" -> {
                return setUpdateClauseTableName(pluginContext, super.visitCall(expression) as IrCall)
            }
        }
        return super.visitCall(expression)
    }

    /**
     * Creates a new IR block body for the function, transforming each statement with additional IR transformations focused on `KTable` manipulations.
     * 为函数创建一个新的 IR 块体，通过额外的针对 `KTable` 操作的 IR 转换来转换每个语句。
     */
    private fun transformKTable(
        irFunction: IrFunction
    ): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlockBody {
            +irBlock {
                +irFunction.body!!.statements
            }
                .transform(KTableFieldAddReturnTransformer(pluginContext, irFunction), null)
                .transform(KTableParamPutBlockTransformer(pluginContext, irFunction), null)
        }
    }

    private fun transformKTableConditional(
        irFunction: IrFunction
    ): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlockBody {
            +irBlock(resultType = irFunction.returnType) {
                +irFunction.body!!.statements
            }
                .transform(CriteriaParseReturnTransformer(pluginContext, irFunction), null)
        }
    }
}