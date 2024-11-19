package com.kotlinorm.compiler.fir.utils

import com.kotlinorm.compiler.helpers.kFunctionN
import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.createExprNew
import com.kotlinorm.compiler.helpers.createKClassExpr
import com.kotlinorm.compiler.helpers.irListOf
import com.kotlinorm.compiler.helpers.nType
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.IrValueParameterBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

object KClassCreatorUtil {
    val kPojoClasses = mutableSetOf<IrClass>()

    context(IrPluginContext)
    private val kClassCreatorSymbol
        get() = referenceProperties(
            CallableId(
                packageName = FqName("com.kotlinorm.utils"),
                callableName = Name.identifier("kClassCreator")
            )
        ).first()

    context(IrPluginContext)
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private val kClassCreatorSetterSymbol
        get() = kClassCreatorSymbol.owner.setter!!.symbol

    context(IrPluginContext)
    private fun lambdaArgument(
        lambda: IrSimpleFunction,
        type: IrType = kFunctionN(lambda.allParameters.size).typeWith(lambda.allParameters.map { it.type } + lambda.returnType),
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): IrFunctionExpression = IrFunctionExpressionImpl(
        startOffset,
        endOffset,
        type,
        lambda,
        IrStatementOrigin.LAMBDA
    )

    context(IrBuilderWithScope, IrPluginContext)
    fun buildKClassMapper(declaration: IrFunction) {
        declaration.body = irBlockBody {
            declaration.body?.statements?.forEach { +it }
            val lambda = irFactory.buildFun {
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                name = SpecialNames.ANONYMOUS
                visibility = DescriptorVisibilities.LOCAL
                returnType = KPojoSymbol.nType
                modality = Modality.FINAL
            }.apply {
                parent = declaration
                valueParameters += irFactory.buildValueParameter(IrValueParameterBuilder().apply {
                    name = Name.identifier("kClass")
                    type = createKClassExpr(KPojoSymbol).type // KClass<KPojo>
                    index = 0
                }, this)
                body = irBuiltIns.createIrBuilder(symbol).run {
                    irBlockBody {
                        +irReturn(
                            irWhen(
                                KPojoSymbol.nType,
                                kPojoClasses.mapNotNull {
                                    createExprNew(it.symbol)?.let { new ->
                                        irBranch(
                                            irEquals(
                                                irGet(valueParameters.first()),
                                                createKClassExpr(it.symbol)
                                            ),
                                            new
                                        )
                                    }
                                } + irElseBranch(
                                    irNull()
                                )
                            )
                        )
                    }
                }
            }

            +applyIrCall(
                kClassCreatorSetterSymbol,
                lambdaArgument(lambda),
                operator = IrStatementOrigin.EQ
            )
        }
    }
}