/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.compiler.plugin.utils

import com.kotlinorm.compiler.helpers.instantiate
import com.kotlinorm.compiler.helpers.invoke
import com.kotlinorm.compiler.helpers.kFunctionN
import com.kotlinorm.compiler.helpers.nType
import com.kotlinorm.compiler.helpers.toKClass
import com.kotlinorm.compiler.helpers.valueParameters
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
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
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

object KClassCreatorUtil {
    val kPojoClasses = mutableSetOf<IrClass>()
    val initFunctions = mutableSetOf<Triple<IrPluginContext, IrBuilderWithScope, IrFunction>>()
    fun resetKClassCreator() {
        kPojoClasses.clear()
        initFunctions.clear()
    }

    context(context: IrPluginContext)
    private val kClassCreatorSymbol
        get() = context.referenceProperties(
            CallableId(
                packageName = FqName("com.kotlinorm.utils"),
                callableName = Name.identifier("kClassCreator")
            )
        ).first()

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    context(_: IrPluginContext)
    private val kClassCreatorSetterSymbol
        get() = kClassCreatorSymbol.owner.setter!!.symbol

    context(_: IrPluginContext)
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

    context(_: IrPluginContext, builder: IrBuilderWithScope)
    fun buildKClassMapper(declaration: IrFunction) {
        declaration.body = builder.irBlockBody {
            val lambda = context.irFactory.buildFun {
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                name = SpecialNames.ANONYMOUS
                visibility = DescriptorVisibilities.LOCAL
                returnType = KPojoSymbol.nType
                modality = Modality.FINAL
            }.apply {
                parent = declaration
                parameters = parameters + context.irFactory.buildValueParameter(IrValueParameterBuilder().apply {
                    name = Name.identifier("kClass")
                    type = KPojoSymbol.toKClass().type // KClass<KPojo>
                    kind = IrParameterKind.Regular
                }, this)
                body = context.irBuiltIns.createIrBuilder(symbol).run {
                    irBlockBody {
                        +irReturn(
                            irWhen(
                                KPojoSymbol.nType,
                                kPojoClasses.distinctBy { it.fqNameWhenAvailable }.mapNotNull {
                                    it.symbol.instantiate()?.let { new ->
                                        irBranch(
                                            irEquals(
                                                irGet(parameters.valueParameters.first()),
                                                it.symbol.toKClass()
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

            +kClassCreatorSetterSymbol(
                lambdaArgument(lambda),
                operator = IrStatementOrigin.EQ
            )
            declaration.body?.statements?.forEach { +it }
        }
    }
}