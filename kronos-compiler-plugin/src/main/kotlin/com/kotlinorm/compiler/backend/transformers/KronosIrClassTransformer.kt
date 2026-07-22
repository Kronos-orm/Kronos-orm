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
import com.kotlinorm.compiler.core.buildFieldFromProperty
import com.kotlinorm.compiler.core.isColumnType
import com.kotlinorm.compiler.core.kPojoClassSymbol
import com.kotlinorm.compiler.utils.AnnotationFqNames
import com.kotlinorm.compiler.utils.set
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addBackingField
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultGetter
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultSetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

/**
 * Kronos IR Class Transformer
 *
 * Transforms classes that implement the `KPojo` interface by replacing their
 * fake-override functions and properties with real, compiler-generated implementations.
 *
 * The following methods/properties are generated for each KPojo class:
 * - `__kType` — property holding the concrete KType for static KPojo classes.
 * - `__columns` — property holding the list of [Field] descriptors for all column properties.
 * - `__tableName` — property holding the table name (from `@Table` annotation or class name).
 * - `__tableComment` — property holding the table comment.
 * - `__tableIndexes` — property holding the list of table indexes.
 * - `__createTime` / `__updateTime` / `__logicDelete` / `__optimisticLock`
 *   — properties holding the special-purpose strategies annotated on the class.
 * - `toDataMap()` — serializes the instance to a `MutableMap<String, Any?>`.
 * - `get(name)` / `set(name, value)` — dynamic property access by column name.
 * - `fromMapData()` / `safeFromMapData()` — populates the instance from a map.
 */
class KronosIrClassTransformer(
    private val pluginContext: IrPluginContext,
    private val irClass: IrClass,
    @Suppress("unused") private val errorReporter: ErrorReporter,
    private val metadataClass: IrClass = irClass
) : IrElementTransformerVoidWithContext() {

    /**
     * FIR2IR represents members inherited through an intermediate KPojo interface as bridge
     * functions on the concrete class, without corresponding [IrProperty] declarations. Attach
     * those exact bridge symbols to real properties before normal body materialization runs.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun materializeMissingKPojoMembers() {
        val declaredPropertyNames = irClass.declarations
            .filterIsInstance<IrProperty>()
            .mapTo(mutableSetOf()) { it.name.asString() }
        if (declaredPropertyNames.containsAll(KPojoPropertyNames)) return

        val kPojoClass = with(pluginContext) { kPojoClassSymbol.owner }
        val propertyTemplates = kPojoClass.properties
            .filter { it.name.asString() in KPojoPropertyNames }
            .associateBy { it.name.asString() }

        propertyTemplates.forEach { (name, template) ->
            if (irClass.declarations.filterIsInstance<IrProperty>().any { it.name.asString() == name }) {
                return@forEach
            }
            materializePropertyBridge(template)
        }

        val functionTemplates = kPojoClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter { it.name.asString() in KPojoFunctionNames }
        functionTemplates.forEach(::materializeFunctionBridge)

        val missingProperties = KPojoPropertyNames - irClass.declarations
            .filterIsInstance<IrProperty>()
            .mapTo(mutableSetOf()) { it.name.asString() }
        check(missingProperties.isEmpty()) {
            "Failed to materialize KPojo properties on ${irClass.kotlinFqName}: $missingProperties"
        }
        val missingFunctions = functionTemplates
            .filter { template ->
                irClass.declarations.filterIsInstance<IrSimpleFunction>().none { candidate ->
                    candidate.correspondingPropertySymbol == null &&
                        candidate.matchesKPojoContract(template) &&
                        !candidate.isFakeOverride &&
                        candidate.body != null
                }
            }
            .mapTo(mutableSetOf()) { it.name.asString() }
        check(missingFunctions.isEmpty()) {
            "Failed to materialize KPojo functions on ${irClass.kotlinFqName}: $missingFunctions"
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun materializePropertyBridge(template: IrProperty) {
        val templateGetter = template.getter ?: return
        val getterBridge = irClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.isMaterializableKPojoBridge() && it.matchesKPojoContract(templateGetter) }
            ?: return
        val templateSetter = template.setter ?: return
        val setterBridge = irClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.isMaterializableKPojoBridge() && it.matchesKPojoContract(templateSetter) }
            ?: return

        irClass.declarations.remove(getterBridge)
        irClass.declarations.remove(setterBridge)

        val property = irClass.addProperty {
            origin = IrDeclarationOrigin.DEFINED
            name = template.name
            visibility = DescriptorVisibilities.PUBLIC
            modality = if (irClass.modality == Modality.FINAL) Modality.FINAL else Modality.OPEN
            isVar = true
        }.apply {
            overriddenSymbols = listOf(template.symbol)
        }
        val backingField = property.addBackingField { type = getterBridge.returnType }
        backingField.initializer = DeclarationIrBuilder(pluginContext, property.symbol).run {
            with(pluginContext) {
                when (property.name.asString()) {
                    "__kType" -> createKTypeProperty(irClass)
                    "__columns" -> createColumns(irClass, metadataClass)
                    "__tableName" -> createTableName(metadataClass)
                    "__tableComment" -> createTableComment(metadataClass)
                    "__tableIndexes" -> createTableIndexes(irClass)
                    "__createTime" -> createKronosSpecialField(irClass, AnnotationFqNames.CreateTime)
                    "__updateTime" -> createKronosSpecialField(irClass, AnnotationFqNames.UpdateTime)
                    "__logicDelete" -> createKronosSpecialField(irClass, AnnotationFqNames.LogicDelete)
                    "__optimisticLock" -> createKronosSpecialField(irClass, AnnotationFqNames.Version)
                    else -> error("Unsupported KPojo property: ${property.name}")
                }
            }
        }

        property.getter = getterBridge.apply {
            isFakeOverride = false
            origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            correspondingPropertySymbol = property.symbol
            overriddenSymbols = listOf(templateGetter.symbol)
            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                val receiver = parameters.single { it.kind == IrParameterKind.DispatchReceiver }
                +irReturn(irGetField(irGet(receiver), backingField))
            }
        }
        property.setter = setterBridge.apply {
            isFakeOverride = false
            origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            correspondingPropertySymbol = property.symbol
            overriddenSymbols = listOf(templateSetter.symbol)
            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                val receiver = parameters.single { it.kind == IrParameterKind.DispatchReceiver }
                val value = parameters.single { it.kind == IrParameterKind.Regular }
                +irSetField(
                    irGet(receiver),
                    backingField,
                    irGet(value)
                )
            }
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun materializeFunctionBridge(template: IrSimpleFunction) {
        val bridge = irClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { candidate ->
                candidate.correspondingPropertySymbol == null &&
                    candidate.isMaterializableKPojoBridge() &&
                    candidate.matchesKPojoContract(template)
            } ?: return

        bridge.isFakeOverride = false
        bridge.overriddenSymbols = listOf(template.symbol)
        bridge.body = DeclarationIrBuilder(pluginContext, bridge.symbol).run {
            with(pluginContext) {
                when (template.name.asString()) {
                    "toDataMap" -> createToDataMap(irClass, bridge)
                    "get" -> createPropertyGetter(irClass, bridge)
                    "set" -> createPropertySetter(irClass, bridge)
                    "fromMapData" -> createFromMapData(irClass, bridge)
                    "safeFromMapData" -> createSafeFromMapData(irClass, bridge)
                    else -> error("Unsupported KPojo function: ${template.name}")
                }
            }
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrSimpleFunction.matchesKPojoContract(target: IrSimpleFunction): Boolean {
        if (overridesTransitively(target)) return true
        if (name != target.name) return false
        val parameters = parameters.filter { it.kind == IrParameterKind.Regular }
        val targetParameters = target.parameters.filter { it.kind == IrParameterKind.Regular }
        return parameters.size == targetParameters.size &&
            parameters.zip(targetParameters).all { (candidate, contract) ->
                candidate.type.isContractCompatibleWith(contract.type)
            } &&
            returnType.isContractCompatibleWith(target.returnType)
    }

    private fun IrSimpleFunction.isMaterializableKPojoBridge(): Boolean =
        isFakeOverride ||
            origin == IrDeclarationOrigin.FAKE_OVERRIDE ||
            origin == IrDeclarationOrigin.BRIDGE ||
            origin == IrDeclarationOrigin.BRIDGE_SPECIAL

    private fun IrType.isContractCompatibleWith(other: IrType): Boolean {
        if (this == other) return true
        val classifier = (this as? IrSimpleType)?.classifier ?: return false
        val otherClassifier = (other as? IrSimpleType)?.classifier ?: return false
        if (classifier == otherClassifier) return true
        if (classifier is IrTypeParameterSymbol && otherClassifier is IrTypeParameterSymbol) return true
        val fqName = classFqName
        val otherFqName = other.classFqName
        return fqName != null && fqName == otherFqName
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrSimpleFunction.overridesTransitively(target: IrSimpleFunction): Boolean {
        val visited = mutableSetOf<IrSimpleFunction>()
        fun visit(function: IrSimpleFunction): Boolean {
            if (!visited.add(function)) return false
            if (function.symbol == target.symbol) return true
            return function.overriddenSymbols.any { visit(it.owner) }
        }
        return visit(this)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        if (declaration !is IrSimpleFunction || !declaration.isFakeOverride) {
            return super.visitFunctionNew(declaration)
        }

        fun replaceFakeBody(bodyFactory: DeclarationIrBuilder.() -> IrBlockBody) {
            declaration.isFakeOverride = false
            declaration[IrParameterKind.DispatchReceiver] = irClass.thisReceiver
            declaration.body = DeclarationIrBuilder(pluginContext, declaration.symbol).bodyFactory()
        }

        with(pluginContext) {
            when (declaration.name.asString()) {
                "toDataMap" -> replaceFakeBody { createToDataMap(irClass, declaration) }
                "get" -> replaceFakeBody { createPropertyGetter(irClass, declaration) }
                "set" -> replaceFakeBody { createPropertySetter(irClass, declaration) }
                "fromMapData" -> replaceFakeBody { createFromMapData(irClass, declaration) }
                "safeFromMapData" -> replaceFakeBody { createSafeFromMapData(irClass, declaration) }
            }
        }

        return super.visitFunctionNew(declaration)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitPropertyNew(declaration: IrProperty): IrStatement {
        if (declaration.isFakeOverride && declaration.backingField == null) {
            fun replaceFakeProp(initializer: () -> IrExpressionBody) {
                declaration.isFakeOverride = false
                declaration.addBackingField { type = declaration.getter!!.returnType }
                declaration.backingField!!.initializer = initializer()
                declaration.getter = null
                declaration.setter = null
                declaration.addDefaultGetter(declaration.parentAsClass, pluginContext.irBuiltIns)
                declaration.addDefaultSetter(declaration.parentAsClass, pluginContext.irBuiltIns)
            }
            with(DeclarationIrBuilder(pluginContext, declaration.symbol)) {
                with(pluginContext) {
                    when (declaration.name.asString()) {
                        "__kType" -> replaceFakeProp { createKTypeProperty(irClass) }
                        "__columns" -> replaceFakeProp { createColumns(irClass, metadataClass) }
                        "__tableName" -> replaceFakeProp { createTableName(metadataClass) }
                        "__tableComment" -> replaceFakeProp { createTableComment(metadataClass) }
                        "__tableIndexes" -> replaceFakeProp { createTableIndexes(irClass) }
                        "__createTime" -> replaceFakeProp { createKronosSpecialField(irClass, AnnotationFqNames.CreateTime) }
                        "__updateTime" -> replaceFakeProp { createKronosSpecialField(irClass, AnnotationFqNames.UpdateTime) }
                        "__logicDelete" -> replaceFakeProp { createKronosSpecialField(irClass, AnnotationFqNames.LogicDelete) }
                        "__optimisticLock" -> replaceFakeProp { createKronosSpecialField(irClass, AnnotationFqNames.Version) }
                    }
                }
            }
        }
        return super.visitPropertyNew(declaration)
    }
}

private val KPojoPropertyNames = setOf(
    "__kType",
    "__columns",
    "__tableName",
    "__tableComment",
    "__tableIndexes",
    "__createTime",
    "__updateTime",
    "__logicDelete",
    "__optimisticLock"
)

private val KPojoFunctionNames = setOf(
    "toDataMap",
    "get",
    "set",
    "fromMapData",
    "safeFromMapData"
)
