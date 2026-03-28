/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm.compiler.transformers

import com.kotlinorm.compiler.core.ErrorReporter
import com.kotlinorm.compiler.core.kClassCreatorSetterSymbol
import com.kotlinorm.compiler.core.kClassSymbol
import com.kotlinorm.compiler.core.kPojoClassSymbol
import com.kotlinorm.compiler.utils.ErrorMessages
import com.kotlinorm.compiler.utils.KronosInitAnnotationFqName
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
import org.jetbrains.kotlin.ir.builders.irCall
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
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * KClassMapGenerator
 *
 * Generates a `kClassCreator` lambda for functions annotated with `@KronosInit`.
 * The generated lambda maps `KClass<KPojo>` to new instances of collected KPojo classes,
 * enabling runtime instantiation of KPojo types by their KClass reference.
 *
 * Roughly speaking, the transform will turn the following:
 *
 *     // file: App.kt
 *     ```kotlin
 *     @KronosInit
 *     fun init() {
 *         // user code
 *     }
 *     ```
 *
 * into the following equivalent representation:
 *
 *     // file: App.kt
 *     ```kotlin
 *     @KronosInit
 *     fun init() {
 *         kClassCreator = { kClass ->
 *             when (kClass) {
 *                 User::class -> User()
 *                 Role::class -> Role()
 *                 else -> null
 *             }
 *         }
 *         // user code
 *     }
 *     ```
 */
object KClassMapGenerator {

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun isKronosInitFunction(declaration: IrFunction): Boolean =
        declaration.hasAnnotation(KronosInitAnnotationFqName)

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun generate(
        pluginContext: IrPluginContext,
        declaration: IrFunction,
        kPojoClasses: Set<IrClass>,
        errorReporter: ErrorReporter
    ) {
        with(pluginContext) {
            val kPojoType = kPojoClassSymbol.defaultType
            val kPojoNullableType = kPojoType.makeNullable()
            val kClassOfKPojoType = kClassSymbol.typeWith(kPojoType)

            // Build the lambda: (KClass<out KPojo>) -> KPojo?
            val lambda = irFactory.buildFun {
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                name = SpecialNames.ANONYMOUS
                visibility = DescriptorVisibilities.LOCAL
                returnType = kPojoNullableType
                modality = Modality.FINAL
            }.apply {
                parent = declaration
                val kClassParam = irFactory.buildValueParameter(
                    IrValueParameterBuilder().apply {
                        name = Name.identifier("kClass")
                        type = kClassOfKPojoType
                        kind = IrParameterKind.Regular
                    },
                    this
                )
                parameters = parameters + kClassParam

                body = irBuiltIns.createIrBuilder(symbol).irBlockBody {
                    +irReturn(
                        irWhen(
                            kPojoNullableType,
                            kPojoClasses
                                .distinctBy { it.fqNameWhenAvailable }
                                .mapNotNull { klass ->
                                    // Only include classes with a no-arg constructor
                                    val ctor = klass.constructors.firstOrNull { ctor ->
                                        ctor.parameters.none { p ->
                                            p.kind == IrParameterKind.Regular && p.defaultValue == null
                                        }
                                    } ?: run {
                                        errorReporter.reportWarning(
                                            klass,
                                            ErrorMessages.kpojoNoNoArgConstructor(klass.fqNameWhenAvailable)
                                        )
                                        return@mapNotNull null
                                    }

                                    val classRef = IrClassReferenceImpl(
                                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                        kClassOfKPojoType,
                                        klass.symbol,
                                        klass.defaultType
                                    )

                                    irBranch(
                                        irEquals(irGet(kClassParam), classRef),
                                        irCall(ctor.symbol)
                                    )
                                } + irElseBranch(irNull())
                        )
                    )
                }
            }

            val kFunctionType = irBuiltIns.functionN(1).typeWith(kClassOfKPojoType, kPojoNullableType)
            val lambdaExpr = IrFunctionExpressionImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                kFunctionType,
                lambda,
                IrStatementOrigin.LAMBDA
            )

            // Prepend: kClassCreator = { kClass -> when(kClass) { ... } }
            val originalBody = declaration.body as? IrBlockBody ?: return
            declaration.body = irBuiltIns.createIrBuilder(declaration.symbol).irBlockBody {
                +irCall(kClassCreatorSetterSymbol).apply {
                    arguments[0] = lambdaExpr
                }
                originalBody.statements.forEach { +it }
            }
        }
    }
}
