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
import com.kotlinorm.compiler.core.enumFactorySymbol
import com.kotlinorm.compiler.core.generatedTypeProviderSymbol
import com.kotlinorm.compiler.core.generatedTypeRegistrarSymbol
import com.kotlinorm.compiler.core.kPojoClassSymbol
import com.kotlinorm.compiler.core.kPojoFactorySymbol
import com.kotlinorm.compiler.core.listOfFunctionSymbol
import com.kotlinorm.compiler.core.typeOfFunctionSymbol
import com.kotlinorm.compiler.plugin.GeneratedTypeProviderConfiguration
import com.kotlinorm.compiler.utils.CascadeAnnotationFqName
import com.kotlinorm.compiler.utils.ErrorMessages
import com.kotlinorm.compiler.utils.GeneratedFactoryPackageFqName
import com.kotlinorm.compiler.utils.SerializeAnnotationFqName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.IrValueParameterBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
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
import org.jetbrains.kotlin.ir.builders.irSamConversion
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * Generates one public, no-argument metadata provider for a compiler module.
 *
 * The provider contributes directly callable KPojo factories and enum-name factories keyed
 * by exact `KType` values. Contributions are emitted in stable declaration-name order and
 * contain no reflective construction or enum lookup. Only declarations visible from the
 * generated package are referenced; unsupported KPojo constructors are reported and omitted.
 *
 * Enum discovery includes concrete enum leaves nested in generic property types. Properties
 * ignored in all mapping directions and cascade-only properties do not contribute enum metadata,
 * while serialized properties may contribute optional metadata for their decoded logical types.
 * Direct non-serialized enum properties and statically typed enum results require metadata and
 * produce a compiler error when the generated package cannot reference the enum declaration.
 */
internal object GeneratedTypeProviderGenerator {
    /**
     * Adds the configured provider class to [moduleFragment].
     *
     * @param pluginContext compiler symbols and IR factory used to construct declarations
     * @param moduleFragment module receiving the generated provider
     * @param kPojoClasses discovered KPojo declarations, including declarations from dependencies
     * @param staticallyTypedEnums enum types used directly by generated mapping code
     * @param provider validated stable provider identity and fully qualified name
     * @param errorReporter compiler reporter for constructibility warnings
     * @return generated provider class, or `null` when the module has no files or already contains it
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun generate(
        pluginContext: IrPluginContext,
        moduleFragment: IrModuleFragment,
        kPojoClasses: Collection<IrClass>,
        staticallyTypedEnums: Set<IrClass>,
        provider: GeneratedTypeProviderConfiguration,
        errorReporter: ErrorReporter
    ): IrClass? {
        if (moduleFragment.files.isEmpty()) return null

        val localKPojoClasses = kPojoClasses
            .filter { it.isDeclaredIn(moduleFragment) }
            .filter { it.isDirectlyConstructibleByGeneratedProvider() }
            .filter { it.isAccessibleFromGeneratedProvider(allowLocalClass = true) }
            .distinctBy { it.fqNameWhenAvailable ?: it.symbol }
            .sortedBy { it.fqNameWhenAvailable?.asString() ?: it.name.asString() }
        val requiredEnumClasses = linkedSetOf<IrClass>().apply {
            addAll(staticallyTypedEnums)
            localKPojoClasses.forEach { kPojo ->
                kPojo.properties
                    .filter { it.requiresScalarEnumMetadata() }
                    .mapNotNullTo(this) { property -> property.declaredType()?.concreteEnumClassOrNull() }
            }
        }
        val discoveredEnumClasses = linkedSetOf<IrClass>().apply {
            addAll(requiredEnumClasses)
            localKPojoClasses.forEach { kPojo ->
                kPojo.properties
                    .filter { it.contributesEnumMetadata() }
                    .forEach { property -> property.declaredType()?.collectConcreteEnumLeaves(this) }
            }
        }
        requiredEnumClasses
            .filterNot { it.isAccessibleFromGeneratedProvider() }
            .forEach { enumClass ->
                errorReporter.reportError(
                    enumClass,
                    ErrorMessages.inaccessibleEnumMetadata(enumClass.fqNameWhenAvailable)
                )
            }
        val enumClasses = discoveredEnumClasses
            .filter { it.isAccessibleFromGeneratedProvider() }
            .sortedBy { it.fqNameWhenAvailable?.asString() ?: it.name.asString() }

        val file = createProviderFile(moduleFragment)
        val providerName = provider.fqName.shortName()
        if (file.declarations.any { (it as? IrClass)?.name == providerName }) return null

        val providerClass = pluginContext.irFactory.buildClass {
            origin = IrDeclarationOrigin.DEFINED
            name = providerName
            visibility = DescriptorVisibilities.PUBLIC
            kind = ClassKind.CLASS
            modality = Modality.FINAL
        }.apply {
            parent = file
            superTypes = listOf(with(pluginContext) { generatedTypeProviderSymbol.defaultType })
            createThisReceiverParameter()
        }

        file.declarations += providerClass
        providerClass.addNoArgConstructor(pluginContext)
        providerClass.addIdGetter(pluginContext, provider.id)
        providerClass.addContributeFunction(pluginContext, localKPojoClasses, enumClasses, errorReporter)
        return providerClass
    }

    private fun createProviderFile(moduleFragment: IrModuleFragment): IrFile {
        moduleFragment.files
            .firstOrNull { it.packageFqName == GeneratedFactoryPackageFqName }
            ?.let { return it }
        val sourceFile = moduleFragment.files.firstOrNull()
            ?: error("Kronos generated type provider requires at least one source file")
        val packageFragment = MutablePackageFragmentDescriptor(sourceFile.moduleDescriptor, GeneratedFactoryPackageFqName)
        return IrFileImpl(sourceFile.fileEntry, packageFragment, moduleFragment).also { moduleFragment.files += it }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.addIdGetter(pluginContext: IrPluginContext, providerId: String) {
        val providerProperty = with(pluginContext) {
            generatedTypeProviderSymbol.owner.properties.single { it.name.asString() == "id" }
        }
        val providerGetter = providerProperty.getter!!
        val property = addProperty {
            name = providerProperty.name
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            isVar = false
            origin = IrDeclarationOrigin.DEFINED
        }.apply {
            overriddenSymbols = listOf(providerProperty.symbol)
        }
        val getter = property.addGetter {
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            returnType = providerGetter.returnType
            origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
        }.apply {
            parameters = listOf(this@addIdGetter.thisReceiver!!.copyTo(this, kind = IrParameterKind.DispatchReceiver))
            overriddenSymbols = listOf(providerGetter.symbol)
        }
        getter.body = pluginContext.irBuiltIns.createIrBuilder(getter.symbol).irBlockBody {
            +irReturn(irString(providerId))
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.addContributeFunction(
        pluginContext: IrPluginContext,
        kPojoClasses: List<IrClass>,
        enumClasses: List<IrClass>,
        errorReporter: ErrorReporter
    ) {
        val providerContribute = with(pluginContext) {
            generatedTypeProviderSymbol.owner.functions.single { it.name.asString() == "contributeTo" }
        }
        val contribute = addFunction {
            name = providerContribute.name
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            returnType = providerContribute.returnType
            origin = IrDeclarationOrigin.DEFINED
        }.apply {
            val registrarParameter = providerContribute.parameters.single { it.kind == IrParameterKind.Regular }
            parameters = listOf(
                this@addContributeFunction.thisReceiver!!.copyTo(this, kind = IrParameterKind.DispatchReceiver),
                pluginContext.irFactory.buildValueParameter(
                    IrValueParameterBuilder().apply {
                        name = Name.identifier("registrar")
                        type = registrarParameter.type
                        kind = IrParameterKind.Regular
                    },
                    this
                )
            )
            overriddenSymbols = listOf(providerContribute.symbol)
        }

        val kPojoCandidates = kPojoClasses.mapNotNull { klass ->
            klass.factoryConstructor(errorReporter)?.let { klass to it }
        }
        contribute.body = pluginContext.irBuiltIns.createIrBuilder(contribute.symbol).irBlockBody {
            val registrar = contribute.parameters.single { it.kind == IrParameterKind.Regular }
            val registerKPojo = with(pluginContext) {
                generatedTypeRegistrarSymbol.owner.functions.single { it.name.asString() == "registerKPojo" }
            }
            val registerEnum = with(pluginContext) {
                generatedTypeRegistrarSymbol.owner.functions.single { it.name.asString() == "registerEnum" }
            }
            kPojoCandidates.forEach { (klass, constructor) ->
                val ownerId = klass.fqNameWhenAvailable?.asString() ?: klass.name.asString()
                +irCall(registerKPojo.symbol).apply {
                    arguments[0] = irGet(registrar)
                    arguments[1] = irCall(with(pluginContext) { typeOfFunctionSymbol }).apply {
                        typeArguments[0] = klass.defaultType
                    }
                    arguments[2] = irString(ownerId)
                    arguments[3] = irString(constructor.signature(klass))
                    arguments[4] = buildKPojoFactory(pluginContext, contribute, constructor)
                }
            }
            enumClasses.forEach { enumClass ->
                val entries = enumClass.declarations.filterIsInstance<IrEnumEntry>()
                +irCall(registerEnum.symbol).apply {
                    arguments[0] = irGet(registrar)
                    arguments[1] = irCall(with(pluginContext) { typeOfFunctionSymbol }).apply {
                        typeArguments[0] = enumClass.defaultType
                    }
                    arguments[2] = buildStringList(pluginContext, entries.map { it.name.asString() })
                    arguments[3] = buildEnumFactory(pluginContext, contribute, enumClass, entries)
                }
            }
        }
    }

    private fun IrBuilderWithScope.buildStringList(
        pluginContext: IrPluginContext,
        values: List<String>
    ): IrExpression = irCall(with(pluginContext) { listOfFunctionSymbol }).apply {
        typeArguments[0] = pluginContext.irBuiltIns.stringType
        arguments[0] = irVararg(pluginContext.irBuiltIns.stringType, values.map(::irString))
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBuilderWithScope.buildKPojoFactory(
        pluginContext: IrPluginContext,
        parent: IrFunction,
        constructor: IrConstructor
    ): IrExpression = with(pluginContext) {
        val factoryCreate = kPojoFactorySymbol.owner.functions.single { it.name.asString() == "create" }
        val typeParameter = factoryCreate.parameters.single { it.kind == IrParameterKind.Regular }
        val lambda = irFactory.buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = SpecialNames.ANONYMOUS
            visibility = DescriptorVisibilities.LOCAL
            returnType = kPojoClassSymbol.defaultType
            modality = Modality.FINAL
        }.apply {
            this.parent = parent
            parameters = parameters + irFactory.buildValueParameter(
                IrValueParameterBuilder().apply {
                    name = Name.identifier("type")
                    type = typeParameter.type
                    kind = IrParameterKind.Regular
                },
                this
            )
            body = irBuiltIns.createIrBuilder(symbol).irBlockBody {
                +irReturn(irCall(constructor.symbol))
            }
        }
        val functionType = irBuiltIns.functionN(1).typeWith(typeParameter.type, kPojoClassSymbol.defaultType)
        val functionExpression = IrFunctionExpressionImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            functionType,
            lambda,
            IrStatementOrigin.LAMBDA
        )
        irSamConversion(functionExpression, kPojoFactorySymbol.defaultType)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBuilderWithScope.buildEnumFactory(
        pluginContext: IrPluginContext,
        parent: IrFunction,
        enumClass: IrClass,
        entries: List<IrEnumEntry>
    ): IrExpression = with(pluginContext) {
        val factoryCreate = enumFactorySymbol.owner.functions.single { it.name.asString() == "create" }
        val nameParameter = factoryCreate.parameters.single { it.kind == IrParameterKind.Regular }
        val lambda = irFactory.buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = SpecialNames.ANONYMOUS
            visibility = DescriptorVisibilities.LOCAL
            returnType = factoryCreate.returnType
            modality = Modality.FINAL
        }.apply {
            this.parent = parent
            parameters = parameters + irFactory.buildValueParameter(
                IrValueParameterBuilder().apply {
                    name = Name.identifier("name")
                    type = nameParameter.type
                    kind = IrParameterKind.Regular
                },
                this
            )
            body = irBuiltIns.createIrBuilder(symbol).irBlockBody {
                val nameParameter = parameters.single { it.kind == IrParameterKind.Regular }
                val branches = entries.map { entry ->
                    irBranch(
                        irEquals(irGet(nameParameter), irString(entry.name.asString())),
                        IrGetEnumValueImpl(
                            startOffset,
                            endOffset,
                            enumClass.defaultType,
                            entry.symbol
                        )
                    )
                } + irElseBranch(irNull(factoryCreate.returnType))
                +irReturn(irWhen(factoryCreate.returnType, branches))
            }
        }
        val functionType = irBuiltIns.functionN(1).typeWith(nameParameter.type, factoryCreate.returnType)
        val functionExpression = IrFunctionExpressionImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            functionType,
            lambda,
            IrStatementOrigin.LAMBDA
        )
        irSamConversion(functionExpression, enumFactorySymbol.defaultType)
    }

    /**
     * Selects a constructor callable without runtime arguments from the generated package.
     *
     * Required regular parameters make a class ineligible; parameters with defaults are legal
     * because the generated IR call uses the compiler-resolved constructor defaults. Failure is
     * reported at compile time and never replaced with reflective construction.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.factoryConstructor(errorReporter: ErrorReporter): IrConstructor? {
        val constructors = try {
            constructors
        } catch (_: IllegalStateException) {
            errorReporter.reportWarning(this, ErrorMessages.kpojoNoNoArgConstructor(fqNameWhenAvailable))
            return null
        }
        return constructors.firstOrNull { constructor ->
            constructor.visibility.isAccessibleFromGeneratedProvider() &&
                constructor.parameters.none { it.kind == IrParameterKind.Regular && it.defaultValue == null }
        } ?: run {
            errorReporter.reportWarning(this, ErrorMessages.kpojoNoNoArgConstructor(fqNameWhenAvailable))
            null
        }
    }

    private fun IrConstructor.signature(owner: IrClass): String {
        val ownerName = owner.fqNameWhenAvailable?.asString() ?: owner.name.asString()
        val parameterTypes = parameters
            .filter { it.kind == IrParameterKind.Regular }
            .joinToString(",") { it.type.classFqName?.asString() ?: it.type.toString() }
        return "$ownerName($parameterTypes)"
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

    private fun IrClass.isDeclaredIn(moduleFragment: IrModuleFragment): Boolean =
        runCatching { file in moduleFragment.files }.getOrDefault(false)

    private fun IrClass.isAccessibleFromGeneratedProvider(allowLocalClass: Boolean = false): Boolean {
        var owner: IrDeclarationParent = this
        while (owner is IrClass) {
            val isLocalRoot = owner === this && owner.visibility == DescriptorVisibilities.LOCAL
            if (owner.name.isSpecial || (!isLocalRoot && !owner.visibility.isAccessibleFromGeneratedProvider())) return false
            if (isLocalRoot && !allowLocalClass) return false
            owner = owner.parent
        }
        return owner is IrFile || (allowLocalClass && owner is IrFunction && !capturesExternalValues())
    }

    private fun IrClass.capturesExternalValues(): Boolean {
        val declaredValues = mutableSetOf<IrValueSymbol>()
        acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                if (element is IrValueDeclaration) declaredValues += element.symbol
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) = Unit
        })

        var capturesExternalValue = false
        acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                if (!capturesExternalValue) element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) = Unit

            override fun visitGetValue(expression: IrGetValue) {
                if (expression.symbol !in declaredValues) capturesExternalValue = true
            }
        })
        return capturesExternalValue
    }

    private fun IrClass.isDirectlyConstructibleByGeneratedProvider(): Boolean =
        kind == ClassKind.CLASS &&
            modality != Modality.ABSTRACT &&
            modality != Modality.SEALED &&
            !isInner &&
            typeParameters.isEmpty()

    private fun org.jetbrains.kotlin.descriptors.DescriptorVisibility.isAccessibleFromGeneratedProvider(): Boolean =
        this == DescriptorVisibilities.PUBLIC || this == DescriptorVisibilities.INTERNAL

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrProperty.contributesEnumMetadata(): Boolean {
        if (isIgnoredForAll()) return false
        if (hasAnnotation(SerializeAnnotationFqName)) return true
        return !hasAnnotation(CascadeAnnotationFqName)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrProperty.requiresScalarEnumMetadata(): Boolean =
        !isIgnoredForAll() &&
            !hasAnnotation(SerializeAnnotationFqName) &&
            !hasAnnotation(CascadeAnnotationFqName)

    private fun IrProperty.declaredType(): IrType? = getter?.returnType ?: backingField?.type

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrType.concreteEnumClassOrNull(): IrClass? =
        (this as? IrSimpleType)
            ?.classOrNull
            ?.owner
            ?.takeIf { it.kind == ClassKind.ENUM_CLASS }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrType.collectConcreteEnumLeaves(target: MutableSet<IrClass>) {
        val simpleType = this as? IrSimpleType ?: return
        simpleType.classOrNull?.owner
            ?.takeIf { it.kind == ClassKind.ENUM_CLASS }
            ?.let(target::add)
        simpleType.arguments
            .filterIsInstance<IrTypeProjection>()
            .forEach { it.type.collectConcreteEnumLeaves(target) }
    }
}
