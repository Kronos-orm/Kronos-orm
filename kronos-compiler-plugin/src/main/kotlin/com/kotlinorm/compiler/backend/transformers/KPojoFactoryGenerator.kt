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

package com.kotlinorm.compiler.backend.transformers

import com.kotlinorm.compiler.core.ErrorReporter
import com.kotlinorm.compiler.core.kClassSymbol
import com.kotlinorm.compiler.core.kPojoClassSymbol
import com.kotlinorm.compiler.core.kPojoFactoryProviderSymbol
import com.kotlinorm.compiler.core.registerKPojoFactorySymbol
import com.kotlinorm.compiler.utils.ErrorMessages
import com.kotlinorm.compiler.utils.GeneratedFactoryPackageFqName
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.IrValueParameterBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl

/**
 * Generates a module-local KPojo factory provider.
 *
 * The provider registers a single `when (kClass)` factory with the runtime registry.
 */
object KPojoFactoryGenerator {
    private const val ProviderName = "KronosGeneratedKPojoFactoryProvider"

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun generate(
        pluginContext: IrPluginContext,
        moduleFragment: IrModuleFragment,
        kPojoClasses: Set<IrClass>,
        errorReporter: ErrorReporter
    ): IrClass? {
        if (moduleFragment.files.isEmpty()) return null
        val file = createFactoryFile(moduleFragment)
        if (file.declarations.any { (it as? IrClass)?.name?.asString() == ProviderName }) return null

        val providerClass = pluginContext.irFactory.buildClass {
            origin = IrDeclarationOrigin.DEFINED
            name = Name.identifier(ProviderName)
            visibility = DescriptorVisibilities.PUBLIC
            kind = ClassKind.CLASS
            modality = Modality.FINAL
        }.apply {
            parent = file
            superTypes = listOf(with(pluginContext) { kPojoFactoryProviderSymbol.defaultType })
            createThisReceiverParameter()
        }

        file.declarations += providerClass
        providerClass.addNoArgConstructor(pluginContext)

        val registerFunction = providerClass.addFunction {
            name = Name.identifier("register")
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.OPEN
            returnType = pluginContext.irBuiltIns.unitType
            origin = IrDeclarationOrigin.DEFINED
        }.apply {
            parameters = listOf(providerClass.thisReceiver!!.copyTo(this, kind = IrParameterKind.DispatchReceiver)) + parameters
            overriddenSymbols = with(pluginContext) {
                listOfNotNull(kPojoFactoryProviderSymbol.owner.functions.firstOrNull { it.name.asString() == "register" }?.symbol)
            }
        }

        registerFunction.body = pluginContext.irBuiltIns.createIrBuilder(registerFunction.symbol).irBlockBody {
            if (kPojoClasses.isNotEmpty()) {
                +irCall(with(pluginContext) { registerKPojoFactorySymbol }).apply {
                arguments[0] = with(errorReporter) {
                    buildFactoryLambda(pluginContext, registerFunction, kPojoClasses)
                }
                }
            }
        }

        return providerClass
    }

    private fun createFactoryFile(moduleFragment: IrModuleFragment): IrFile {
        moduleFragment.files
            .firstOrNull { it.packageFqName == GeneratedFactoryPackageFqName }
            ?.let { return it }
        val sourceFile = moduleFragment.files.firstOrNull()
            ?: error("Kronos KPojo factory generator requires at least one source file")
        val fileEntry = sourceFile.fileEntry
        val packageFragment = MutablePackageFragmentDescriptor(sourceFile.moduleDescriptor, GeneratedFactoryPackageFqName)
        return IrFileImpl(
            fileEntry = fileEntry,
            packageFragmentDescriptor = packageFragment,
            module = moduleFragment
        ).also { moduleFragment.files += it }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    context(errorReporter: ErrorReporter)
    private fun buildFactoryLambda(
        pluginContext: IrPluginContext,
        parent: IrFunction,
        kPojoClasses: Set<IrClass>
    ) = with(pluginContext) {
        val kPojoType = kPojoClassSymbol.defaultType
        val kPojoNullableType = kPojoType.makeNullable()
        val kClassOfKPojoType = kClassSymbol.typeWith(kPojoType)

        val lambda = irFactory.buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = SpecialNames.ANONYMOUS
            visibility = DescriptorVisibilities.LOCAL
            returnType = kPojoNullableType
            modality = Modality.FINAL
        }.apply {
            this.parent = parent
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
                            .distinctBy { it.fqNameWhenAvailable ?: it.symbol }
                            .mapNotNull { klass ->
                                val constructors = try {
                                    klass.constructors
                                } catch (_: IllegalStateException) {
                                    errorReporter.reportWarning(
                                        klass,
                                        ErrorMessages.kpojoNoNoArgConstructor(klass.fqNameWhenAvailable)
                                    )
                                    return@mapNotNull null
                                }
                                val ctor = constructors.firstOrNull { ctor ->
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
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
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
        IrFunctionExpressionImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            kFunctionType,
            lambda,
            IrStatementOrigin.LAMBDA
        )
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.addNoArgConstructor(pluginContext: IrPluginContext) {
        addConstructor {
            origin = IrDeclarationOrigin.DEFINED
            visibility = DescriptorVisibilities.PUBLIC
            isPrimary = true
        }.also { constructor ->
            constructor.body = DeclarationIrBuilder(pluginContext, constructor.symbol).irBlockBody {
                +irDelegatingConstructorCall(pluginContext.irBuiltIns.anyClass.owner.constructors.single())
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    this@addNoArgConstructor.symbol,
                    pluginContext.irBuiltIns.unitType
                )
            }
        }
    }
}
