@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

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
import com.kotlinorm.compiler.core.firstTypeArgument
import com.kotlinorm.compiler.core.kPojoClassSymbol
import com.kotlinorm.compiler.fir.KronosProjectionField
import com.kotlinorm.compiler.fir.KronosProjectionModel
import com.kotlinorm.compiler.fir.KronosProjectionRegistry
import com.kotlinorm.compiler.utils.CascadeAnnotationFqName
import com.kotlinorm.compiler.utils.GeneratedProjectionPackageFqName
import com.kotlinorm.compiler.utils.JoinSelectGeneratedProjectionCallableId
import com.kotlinorm.compiler.utils.JoinSourceFqName
import com.kotlinorm.compiler.utils.KSelectableFqName
import com.kotlinorm.compiler.utils.KPojoTableCommentPropertyName
import com.kotlinorm.compiler.utils.KPojoTableNamePropertyName
import com.kotlinorm.compiler.utils.SelectFunctionFqName
import com.kotlinorm.compiler.utils.SelectGeneratedProjectionCallableId
import com.kotlinorm.compiler.utils.SerializeAnnotationFqName
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addBackingField
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultGetter
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultSetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.ir.util.addFakeOverrides
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ProjectionKind
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.types.Variance

/**
 * Materializes FIR-declared select projection classes as concrete backend IR classes.
 */
class KronosProjectionIrTransformer(
    private val pluginContext: IrPluginContext,
    private val errorReporter: ErrorReporter
) : IrElementTransformerVoidWithContext() {
    private val materializedProjectionClasses = linkedMapOf<FqName, IrClass>()
    private val projectionMetadataClasses = linkedMapOf<FqName, IrClass>()
    val projectionClasses: Collection<IrClass>
        get() = materializedProjectionClasses.values

    private var moduleFragment: IrModuleFragment? = null
    private var projectionFile: IrFile? = null

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        moduleFragment = declaration
        return super.visitModuleFragment(declaration)
    }

    /**
     * Rewrites bare select calls after their FIR-refined return type exposes a generated projection class.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        val call = super.visitCall(expression) as IrCall
        return buildGeneratedSelectCall(call)
            ?: redirectProjectionAccessorCall(call)
            ?: call
    }

    /**
     * Redirects property reads from FIR-lazy projection getters to the materialized backend class.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun redirectProjectionAccessorCall(call: IrCall): IrCall? {
        if (call.origin != IrStatementOrigin.GET_PROPERTY) return null
        val lazyProjectionClass = call.symbol.owner.parent as? IrClass ?: return null
        if (!lazyProjectionClass.isGeneratedProjectionClass()) return null
        val materializedProjectionClass = materializedProjectionClasses[lazyProjectionClass.kotlinFqName]
            ?: materializeProjectionForAccessor(lazyProjectionClass)
            ?: return null
        val propertyName = call.symbol.owner.correspondingPropertySymbol?.owner?.name ?: return null
        val materializedGetter = materializedProjectionClass.properties
            .firstOrNull { it.name == propertyName }
            ?.getter
            ?: return null

        call.symbol = materializedGetter.symbol
        call.type = materializedGetter.returnType
        call.superQualifierSymbol = null
        return call
    }

    /**
     * Materializes a projection before its originating select call is visited.
     *
     * Generated Selected values can cross function boundaries, so a caller's property read may
     * be traversed before the helper that contains the select call. Leaving the FIR-lazy getter in
     * that call path exposes a fake override without an overridden declaration to JVM lowering.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun materializeProjectionForAccessor(generatedClass: IrClass): IrClass? {
        val model = KronosProjectionRegistry.findAny(ClassId.topLevel(generatedClass.kotlinFqName)) ?: return null
        val sourceType = model.sourceType.toIrTypeOrNull() ?: return null
        return materializeGeneratedProjectionClass(generatedClass, sourceType)
    }

    /**
     * Rewrites bare select calls to pass the generated projection and context KClasses at runtime.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildGeneratedSelectCall(call: IrCall): IrCall? {
        val function = call.symbol.owner
        val receiverType = call.arguments.getOrNull(0)?.type
        val isJoinSelect = function.name.asString() == "select" && receiverType?.isJoinSourceReceiver() == true
        if (function.kotlinFqName != SelectFunctionFqName && !isJoinSelect) return null

        val selectType = call.type as? IrSimpleType ?: return null
        val sourceType = (selectType.arguments.getOrNull(0) as? IrTypeProjection)?.type ?: return null
        val projectionType = (selectType.arguments.getOrNull(1) as? IrTypeProjection)?.type ?: return null
        val contextType = (selectType.arguments.getOrNull(2) as? IrTypeProjection)?.type ?: return null
        val projectionClass = projectionType.classOrNull?.owner ?: return null
        val contextClass = contextType.classOrNull?.owner ?: return null
        if (!projectionClass.isGeneratedProjectionClass()) return null
        if (!contextClass.isGeneratedProjectionClass()) return null
        val materializedProjectionClass = materializeGeneratedProjectionClass(projectionClass, sourceType)
        val materializedContextClass = materializeGeneratedProjectionClass(contextClass, sourceType)
        val materializedProjectionType = materializedProjectionClass.symbol.defaultType
        val materializedContextType = materializedContextClass.symbol.defaultType

        val selectableReceiver = receiverType?.isKSelectableReceiver() == true
        val receiverClassFqName = if (isJoinSelect) JoinSourceFqName else KSelectableFqName
        val selectGeneratedCallableId = if (isJoinSelect) {
            JoinSelectGeneratedProjectionCallableId
        } else {
            SelectGeneratedProjectionCallableId
        }
        val selectGeneratedSymbol = pluginContext.referenceFunctions(selectGeneratedCallableId)
            .firstOrNull { symbol ->
                symbol.owner.typeParameters.size >= 3 &&
                    symbol.owner.parameters.size == 4 &&
                    if (isJoinSelect) {
                        symbol.owner.parameters[0].type.classFqName == receiverClassFqName
                    } else {
                        (symbol.owner.parameters[0].type.classFqName == receiverClassFqName) == selectableReceiver
                    }
            }
            ?: return null

        return DeclarationIrBuilder(pluginContext, currentScope!!.scope.scopeOwnerSymbol).irCall(selectGeneratedSymbol).takeIf {
            it.typeArguments.size >= 3 && it.arguments.size >= 4
        }?.apply {
            typeArguments[0] = sourceType
            typeArguments[1] = materializedProjectionType
            typeArguments[2] = materializedContextType
            arguments[0] = call.arguments.getOrNull(0)
            arguments[1] = IrClassReferenceImpl(
                call.startOffset,
                call.endOffset,
                pluginContext.irBuiltIns.kClassClass.typeWith(materializedProjectionType),
                materializedProjectionClass.symbol,
                materializedProjectionType
            )
            arguments[2] = IrClassReferenceImpl(
                call.startOffset,
                call.endOffset,
                pluginContext.irBuiltIns.kClassClass.typeWith(materializedContextType),
                materializedContextClass.symbol,
                materializedContextType
            )
            arguments[3] = call.arguments[1]
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun materializeGeneratedProjectionClass(generatedClass: IrClass, sourceType: IrType): IrClass {
        val fqName = generatedClass.kotlinFqName
        materializedProjectionClasses[fqName]?.let { return it }
        val classId = ClassId.topLevel(fqName)
        val model = KronosProjectionRegistry.findAny(classId) ?: return generatedClass

        val file = projectionFile ?: createProjectionFile().also { projectionFile = it }
        val irClass = pluginContext.irFactory.buildClass {
            origin = IrDeclarationOrigin.DEFINED
            name = generatedClass.name
            visibility = DescriptorVisibilities.PUBLIC
            kind = ClassKind.CLASS
            modality = Modality.FINAL
            isData = true
        }.apply {
            parent = file
            superTypes = listOf(with(pluginContext) { kPojoClassSymbol.defaultType })
            createThisReceiverParameter()
        }

        file.declarations += irClass
        materializedProjectionClasses[fqName] = irClass

        val rawSourceClass = sourceType.classOrNull?.owner
        val sourceClass = rawSourceClass?.materializedSourceClassOrNull() ?: irClass
        val metadataClass = rawSourceClass?.metadataSourceClassOrNull() ?: sourceClass
        projectionMetadataClasses[fqName] = metadataClass

        val sourceProperties = sourceClass.properties.associateBy { it.name }
        val projectionFields = model.fieldsFor(classId).withCascadeLocalKeyFields(sourceClass, sourceProperties)
        projectionFields.forEach { field ->
            val fieldSourceClass = field.sourceClassId
                ?.takeUnless { it.isLocal }
                ?.let(pluginContext::referenceClass)
                ?.owner
                ?: sourceClass
            val fieldSourceProperties = fieldSourceClass.properties.associateBy { it.name }
            val sourceProperty = fieldSourceProperties[field.sourceName] ?: sourceProperties[field.sourceName]
            val sourceType = sourceProperty?.getter?.returnType
                ?: sourceProperty?.backingField?.type
            // Cascade-local fields are synthesized from the relationship field in FIR,
            // but their runtime property must retain the actual local key type.
            val propertyType = if (field.signature.startsWith("cascadeLocal:")) {
                sourceType ?: field.type.toIrTypeOrNull()
            } else {
                field.type.toIrTypeOrNull()
                    ?: if (field.isSourceAlias) sourceType else null
            } ?: pluginContext.irBuiltIns.anyNType
            irClass.addProjectionProperty(field.name, propertyType, sourceProperty.takeIf { field.isSourceAlias })
        }
        irClass.addNoArgConstructor()
        irClass.addFakeOverrides(IrTypeSystemContextImpl(pluginContext.irBuiltIns))
        irClass.transform(KronosIrClassTransformer(pluginContext, irClass, errorReporter, metadataClass), null)
        return irClass
    }

    /**
     * Uses the concrete backend class for generated projection sources.
     *
     * A reselect or derived JOIN source can itself be a generated projection. Materialize that
     * source recursively instead of caching the outer projection with its own incomplete fields.
     * The outer class is registered before this lookup, so a cyclic source reference terminates
     * at the already-created class rather than recursing indefinitely.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.materializedSourceClassOrNull(): IrClass? {
        if (!isGeneratedProjectionClass()) return this
        return materializedProjectionClasses[kotlinFqName]
            ?: materializeProjectionForAccessor(this)
    }

    /**
     * Keeps generated projection table metadata tied to the original source class.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.metadataSourceClassOrNull(): IrClass? {
        if (!isGeneratedProjectionClass()) return this
        return projectionMetadataClasses[kotlinFqName]
    }

    /**
     * Selects the generated fields that belong to the materialized result or context class.
     */
    private fun KronosProjectionModel.fieldsFor(classId: ClassId): List<KronosProjectionField> =
        when (classId) {
            this.classId -> fields
            contextClassId -> contextFields
            else -> error("Projection model ${this.classId} does not contain fields for $classId")
        }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun List<KronosProjectionField>.withCascadeLocalKeyFields(
        sourceClass: IrClass,
        sourceProperties: Map<Name, IrProperty>
    ): List<KronosProjectionField> {
        val result = linkedMapOf<Name, KronosProjectionField>()
        fun add(field: KronosProjectionField) {
            result.putIfAbsent(field.name, field)
        }

        forEach { field ->
            add(field)
            val fieldSourceClass = field.sourceClassId
                ?.takeUnless { it.isLocal }
                ?.let(pluginContext::referenceClass)
                ?.owner
                ?: sourceClass
            val fieldSourceProperties = fieldSourceClass.properties.associateBy { it.name }
            val sourceProperty = fieldSourceProperties[field.sourceName] ?: sourceProperties[field.sourceName] ?: return@forEach
            val localKeys = sourceProperty.directCascadeLocalProperties() +
                sourceProperty.reverseCascadeLocalProperties(fieldSourceClass)

            localKeys.forEach { localName ->
                if (result.containsKey(localName)) return@forEach
                val localProperty = fieldSourceProperties[localName] ?: sourceProperties[localName] ?: return@forEach
                add(
                    field.copy(
                        name = localName,
                        source = field.source,
                        sourceName = localName,
                        sourceClassId = field.sourceClassId,
                        signature = "cascadeLocal:${sourceProperty.name.asString()}:${localName.asString()}"
                    )
                )
            }
        }

        return result.values.toList()
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrProperty.directCascadeLocalProperties(): List<Name> =
        cascadeStringArguments(index = 0)

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrProperty.reverseCascadeLocalProperties(sourceClass: IrClass): List<Name> {
        val targetClass = cascadeTargetClass() ?: return emptyList()
        return targetClass.properties
            .filter { property ->
                property.cascadeStringArguments(index = 0).isNotEmpty() &&
                    property.cascadeTargetClass() == sourceClass
            }
            .flatMap { property -> property.cascadeStringArguments(index = 1) }
            .toList()
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrProperty.cascadeStringArguments(index: Int): List<Name> {
        val cascade = annotations.firstOrNull {
            it.symbol.owner.returnType.classFqName == CascadeAnnotationFqName
        } ?: return emptyList()
        val vararg = cascade.arguments[index] as IrVararg
        return vararg.elements.filterIsInstance<IrConst>().map { element ->
            Name.identifier(element.value.toString())
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrProperty.cascadeTargetClass(): IrClass? {
        val type = getter?.returnType ?: backingField!!.type
        return type.firstTypeArgument()?.classOrNull?.owner ?: type.classOrNull?.owner
    }

    private fun createProjectionFile(): IrFile {
        val module = moduleFragment ?: error("Kronos projection materializer has no module fragment")
        val sourceFile = module.files.firstOrNull()
            ?: error("Kronos projection materializer requires at least one source file")
        val fileEntry = sourceFile.fileEntry
        val packageFragment = MutablePackageFragmentDescriptor(sourceFile.moduleDescriptor, GeneratedProjectionPackageFqName)
        return IrFileImpl(
            fileEntry = fileEntry,
            packageFragmentDescriptor = packageFragment,
            module = module
        ).also { module.files += it }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.addProjectionProperty(
        name: Name,
        type: IrType,
        sourceProperty: IrProperty?
    ): IrProperty {
        return addProperty {
            origin = IrDeclarationOrigin.DEFINED
            this.name = name
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            isVar = true
        }.also { property ->
            property.annotations += sourceProperty
                ?.annotations
                ?.filter { it.symbol.owner.returnType.classFqName == SerializeAnnotationFqName }
                ?.map { it.deepCopyWithSymbols() }
                .orEmpty()
            val backingField = property.addBackingField {
                this.type = type
            }
            property.addDefaultGetter(this, pluginContext.irBuiltIns)
            property.addDefaultSetter(this, pluginContext.irBuiltIns)
        }
    }

    /**
     * Converts the FIR-selected type to the backend type used by the materialized property.
     * This keeps a derived alias authoritative even when its logical name matches a source field.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun ConeKotlinType.toIrTypeOrNull(): IrType? {
        val classType = this as? ConeClassLikeType ?: return null
        val classSymbol = pluginContext.referenceClass(classType.lookupTag.classId) ?: return null
        val arguments = classType.typeArguments.map { projection ->
            when (projection) {
                is ConeStarProjection -> IrStarProjectionImpl
                is ConeKotlinTypeProjection -> {
                    val type = projection.type.toIrTypeOrNull() ?: return null
                    val variance = when (projection.kind) {
                        ProjectionKind.IN -> Variance.IN_VARIANCE
                        ProjectionKind.OUT -> Variance.OUT_VARIANCE
                        else -> Variance.INVARIANT
                    }
                    makeTypeProjection(type, variance)
                }
                else -> return null
            }
        }
        return classSymbol.createType(classType.isMarkedNullable, arguments)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.addNoArgConstructor() {
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

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.isGeneratedProjectionClass(): Boolean =
        kotlinFqName.parent() == GeneratedProjectionPackageFqName

    private fun IrType.isKSelectableReceiver(): Boolean {
        return classFqName == KSelectableFqName || superTypes().any { it.classFqName == KSelectableFqName }
    }

    private fun IrType.isJoinSourceReceiver(): Boolean {
        return classFqName == JoinSourceFqName || superTypes().any { it.classFqName == JoinSourceFqName }
    }
}
