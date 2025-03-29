/**
 * Copyright 2022-2024 kronos-orm
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

import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.createExprNew
import com.kotlinorm.compiler.helpers.createKClassExpr
import com.kotlinorm.compiler.helpers.kFunctionN
import com.kotlinorm.compiler.helpers.nType
import com.kotlinorm.compiler.plugin.utils.context.KotlinBuilderContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
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
    val initFunctions = mutableSetOf<Pair<KotlinBuilderContext, IrFunction>>()
    fun resetKClassCreator() {
        kPojoClasses.clear()
        initFunctions.clear()
    }

    private val IrPluginContext.kClassCreatorSymbol
        get() = referenceProperties(
            CallableId(
                packageName = FqName("com.kotlinorm.utils"),
                callableName = Name.identifier("kClassCreator")
            )
        ).first()

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private val IrPluginContext.kClassCreatorSetterSymbol
        get() = kClassCreatorSymbol.owner.setter!!.symbol

    private fun IrPluginContext.lambdaArgument(
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

    fun KotlinBuilderContext.buildKClassMapper(declaration: IrFunction) {
        with(pluginContext){
            with(builder){
                declaration.body = irBlockBody {
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
                        }, this)
                        body = irBuiltIns.createIrBuilder(symbol).run {
                            irBlockBody {
                                +irReturn(
                                    irWhen(
                                        KPojoSymbol.nType,
                                        kPojoClasses.distinctBy { it.fqNameWhenAvailable }.mapNotNull {
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
                    declaration.body?.statements?.forEach { +it }
                }
            }
        }
    }
}