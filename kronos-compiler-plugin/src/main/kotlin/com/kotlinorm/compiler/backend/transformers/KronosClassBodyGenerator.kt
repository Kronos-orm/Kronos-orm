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

@file:Suppress("DEPRECATION")

package com.kotlinorm.compiler.backend.transformers

import com.kotlinorm.compiler.core.buildFieldFromProperty
import com.kotlinorm.compiler.core.fieldClassSymbol
import com.kotlinorm.compiler.core.fieldConstructorSymbol
import com.kotlinorm.compiler.core.isColumnType
import com.kotlinorm.compiler.core.getSafeValueSymbol
import com.kotlinorm.compiler.core.kronosCommonStrategyConstructorSymbol
import com.kotlinorm.compiler.core.kronosObjectSymbol
import com.kotlinorm.compiler.core.k2dbFunctionSymbol
import com.kotlinorm.compiler.core.tableNamingStrategyGetterSymbol
import com.kotlinorm.compiler.core.pairClassSymbol
import com.kotlinorm.compiler.core.typeOfFunctionSymbol
import com.kotlinorm.compiler.utils.AnnotationFqNames
import com.kotlinorm.compiler.utils.IgnoreAnnotationFqName
import com.kotlinorm.compiler.utils.SerializeAnnotationFqName
import com.kotlinorm.compiler.utils.TableAnnotationFqName
import com.kotlinorm.compiler.utils.dispatchReceiver
import com.kotlinorm.compiler.utils.getKDocString
import com.kotlinorm.compiler.utils.invoke
import com.kotlinorm.compiler.utils.valueParameters
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Kronos Class Body Generator
 *
 * Provides `DeclarationIrBuilder` extension functions that generate IR bodies
 * and property initializers for KPojo members implemented by [KronosIrClassTransformer].
 *
 * Metadata helpers produce `IrExpressionBody` initializers for backing fields:
 * - [createColumns] — builds a `mutableListOf(Field(...), ...)` expression.
 * - [createTableName] — builds the table name derived from `@Table` or the class name.
 * - [createTableComment] — builds the table comment string.
 * - [createTableIndexes] — builds the mutable list of table indexes.
 * - [createKronosSpecialField] — builds the special strategy value.
 * - [createKTypeProperty] — builds the concrete `KType` of the class.
 * - [createToDataMap] — serializes all column properties into a `MutableMap`.
 * - [createPropertyGetter] / [createPropertySetter] — dynamic property access by name.
 * - [createFromMapData] — populates the instance from a `Map<String, Any?>`.
 */

// ============================================================================
// Body generation functions — extension on DeclarationIrBuilder
// ============================================================================

/**
 * Generates: var __columns: MutableList<Field> = mutableListOf(Field(...), ...)
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createColumns(irClass: IrClass, metadataClass: IrClass = irClass): IrExpressionBody {
    val fields = irClass.properties
        .filter { !it.isIgnoredForAll() }
        .map { buildFieldFromProperty(it, metadataClass) }
        .toList()
    return irExprBody(buildMutableListOf(fieldClassSymbol.defaultType, fields))
}

/**
 * Generates: var __tableName: String = "table_name"
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
fun DeclarationIrBuilder.createTableName(irClass: IrClass): IrExpressionBody =
    irExprBody(getTableNameExpr(irClass))

/**
 * Generates: var __tableComment: String = "extracted KDoc comment"
 */
context(_: IrPluginContext)
fun DeclarationIrBuilder.createTableComment(irClass: IrClass): IrExpressionBody =
    irExprBody(irClass.getKDocString())

/**
 * Generates: var __tableIndexes: MutableList<KTableIndex> = mutableListOf(KTableIndex(...), ...)
 *
 * Reads @TableIndex annotations from the class and constructs KTableIndex instances.
 * The @TableIndex annotation parameters (name, columns, type, method, concurrently)
 * map directly to the KTableIndex constructor parameters.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createTableIndexes(irClass: IrClass): IrExpressionBody {
    val kTableIndexClassId = ClassId.topLevel(FqName("com.kotlinorm.beans.dsl.KTableIndex"))
    val kTableIndexSymbol = context.referenceClass(kTableIndexClassId)
        ?: error("KTableIndex class not found")
    val kTableIndexConstructor = kTableIndexSymbol.constructors.first()
    val listType = kTableIndexSymbol.defaultType

    val tableIndexAnnotations = irClass.annotations.filter {
        it.symbol.owner.returnType.classFqName == AnnotationFqNames.TableIndex
    }

    val indexExpressions = tableIndexAnnotations.map { annotation ->
        val name = annotation.arguments.getOrNull(0).stringValue()
        val columns = annotation.arguments.getOrNull(1).stringArrayValue()
        val type = annotation.arguments.getOrNull(2).stringValue()
        val method = annotation.arguments.getOrNull(3).stringValue()
        val concurrently = annotation.arguments.getOrNull(4).booleanValue()

        irCall(kTableIndexConstructor).apply {
            arguments[0] = irString(name)
            arguments[1] = irVararg(
                context.irBuiltIns.stringType,
                columns.map { irString(it) }
            )
            arguments[2] = irString(type)
            arguments[3] = irString(method)
            arguments[4] = irBoolean(concurrently)
        }
    }

    return irExprBody(buildMutableListOf(listType, indexExpressions))
}

/**
 * Generates: var __createTime/__updateTime/__logicDelete/__optimisticLock: KronosCommonStrategy = ...
 *
 * If a property with the given annotation is found:
 *   KronosCommonStrategy(enabled, Field("column_name", "propertyName"))
 * Otherwise:
 *   the global default strategy from Kronos object
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createKronosSpecialField(irClass: IrClass, annotationFqName: FqName): IrExpressionBody =
    irExprBody(buildKronosSpecialField(irClass, annotationFqName))

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
private fun IrBuilderWithScope.buildKronosSpecialField(irClass: IrClass, annotationFqName: FqName): IrExpression {
    // Check class-level annotation first (e.g., @CreateTime(enable = false) on the class)
    val classAnno = irClass.annotations.firstOrNull { it.symbol.owner.returnType.classFqName == annotationFqName }
    if (classAnno != null) {
        val classEnabled = if (classAnno.arguments.isNotEmpty()) {
            val firstArg = classAnno.arguments[0]
            !(firstArg is IrConst && firstArg.value == false)
        } else {
            true
        }
        if (!classEnabled) {
            // Class-level annotation explicitly disables the strategy
            return irCall(kronosCommonStrategyConstructorSymbol).apply {
                arguments[0] = irBoolean(false)
                arguments[1] = irCall(fieldConstructorSymbol).apply {
                    arguments[0] = irString("")
                    arguments[1] = irString("")
                }
            }
        }
    }

    val prop = irClass.properties.firstOrNull { it.hasAnnotation(annotationFqName) }
    if (prop != null) {
        // Check if annotation explicitly disables the strategy: @LogicDelete(false)
        val anno = prop.annotations.firstOrNull { it.symbol.owner.returnType.classFqName == annotationFqName }
        val enabled = if (anno != null && anno.arguments.isNotEmpty()) {
            val firstArg = anno.arguments[0]
            !(firstArg is IrConst && firstArg.value == false)
        } else {
            true
        }
        val fieldExpr = buildFieldFromProperty(prop)
        return irCall(kronosCommonStrategyConstructorSymbol).apply {
            arguments[0] = irBoolean(enabled)
            arguments[1] = fieldExpr
        }
    }

    // No annotated property — return the global default from Kronos object
    val strategyName = when (annotationFqName) {
        AnnotationFqNames.CreateTime -> "createTimeStrategy"
        AnnotationFqNames.UpdateTime -> "updateTimeStrategy"
        AnnotationFqNames.LogicDelete -> "logicDeleteStrategy"
        AnnotationFqNames.Version -> "optimisticLockStrategy"
        else -> error("Unsupported common strategy annotation: $annotationFqName")
    }
    val getter = kronosObjectSymbol.owner.declarations
        .filterIsInstance<IrProperty>()
        .first { it.name.asString() == strategyName }
        .getter ?: error("Kronos.$strategyName getter is missing")
    return irCall(getter.symbol).apply {
        dispatchReceiver = irGetObject(kronosObjectSymbol)
    }
}

/**
 * Generates the concrete `typeOf<DeclaredClass>()` initializer for `KPojo.__kType`.
 *
 * The type argument is the transformed class itself, preserving nullability and generic
 * arguments known to the compiler. No runtime class lookup or reflection fallback is emitted.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createKTypeProperty(irClass: IrClass): IrExpressionBody =
    irExprBody(irCall(typeOfFunctionSymbol).apply { typeArguments[0] = irClass.defaultType })

/**
 * Generates `toDataMap()` as a mutable map of field names to backing-field values.
 *
 * Delegated and `Ignore(ALL)` properties are excluded, as are properties ignored for the
 * TO_MAP direction. This body does not encode field values; field-level storage policies are
 * applied by the normal ValueCodec path before persistence.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createToDataMap(irClass: IrClass, irFunction: IrFunction): IrBlockBody =
    irBlockBody {
        fun dispatcher() = irGet(irFunction.parameters.dispatchReceiver!!)
        val entries: List<IrExpression> = irClass.properties
            .filter { it.backingField != null && !it.isDelegated && !it.isIgnoredForAll() && !it.isIgnoredForToMap() }
            .flatMap { prop ->
                [
                    buildPairOf(
                        irString(prop.name.asString()),
                        irGetField(dispatcher(), prop.backingField!!)
                    )
                ]
            }.toList()
        +irReturn(buildMutableMapOf(context.irBuiltIns.stringType, context.irBuiltIns.anyNType, entries))
    }

/**
 * Generates: override fun get(name: String): Any? = when(name) { "prop" -> this.prop; else -> null }
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createPropertyGetter(irClass: IrClass, irFunction: IrFunction): IrBlockBody =
    irBlockBody {
        fun dispatcher() = irGet(irFunction.parameters.dispatchReceiver!!)
        fun nameParam() = irGet(irFunction.parameters.valueParameters.first())
        val branches = irClass.properties
            .filter { it.backingField != null && !it.isDelegated }
            .map { prop ->
                irBranch(
                    irEquals(nameParam(), irString(prop.name.asString())),
                    irGetField(dispatcher(), prop.backingField!!)
                )
            }.toMutableList<Any>()
        branches.add(irElseBranch(irNull()))
        +irReturn(
            irWhen(
                context.irBuiltIns.anyNType,
                @Suppress("UNCHECKED_CAST")
                (branches as List<org.jetbrains.kotlin.ir.expressions.IrBranch>)
            )
        )
    }

/**
 * Generates: override fun set(name: String, value: Any?) { when(name) { "prop" -> this.prop = value as Type } }
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createPropertySetter(irClass: IrClass, irFunction: IrFunction): IrBlockBody =
    irBlockBody {
        fun dispatcher() = irGet(irFunction.parameters.dispatchReceiver!!)
        fun nameParam() = irGet(irFunction.parameters.valueParameters[0])
        fun valueParam() = irGet(irFunction.parameters.valueParameters[1])
        val branches = irClass.properties
            .filter { it.backingField != null && !it.isDelegated }
            .map { prop ->
                val fieldType = prop.backingField!!.type
                val castType = if (fieldType.isNullable()) fieldType else fieldType.makeNullable()
                irBranch(
                    irEquals(nameParam(), irString(prop.name.asString())),
                    irSetField(
                        dispatcher(),
                        prop.backingField!!,
                        IrTypeOperatorCallImpl(
                            startOffset, endOffset,
                            castType,
                            IrTypeOperator.SAFE_CAST,
                            castType,
                            valueParam()
                        )
                    )
                )
            }.toMutableList<Any>()
        branches.add(irElseBranch(IrConstImpl.constNull(startOffset, endOffset, context.irBuiltIns.nothingNType)))
        +irWhen(
            context.irBuiltIns.unitType,
            @Suppress("UNCHECKED_CAST")
            (branches as List<org.jetbrains.kotlin.ir.expressions.IrBranch>)
        )
    }

/**
 * Generates the direct `fromMapData` assignment body.
 *
 * A property is assigned only when its map key is present. Values are safe-cast to the
 * declared field type, so a missing key leaves the existing value untouched and an incompatible
 * value is represented as `null` before the generated assignment semantics apply. Properties
 * ignored for FROM_MAP are omitted.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createFromMapData(irClass: IrClass, irFunction: IrFunction): IrBlockBody =
    irBlockBody {
        fun dispatcher() = irGet(irFunction.parameters.dispatchReceiver!!)
        fun mapParam() = irGet(irFunction.parameters.valueParameters.first())
        val mapGetSymbol = context.irBuiltIns.mapClass.getSimpleFunction("get")!!
        val mapContainsKeySymbol = context.irBuiltIns.mapClass.getSimpleFunction("containsKey")!!

        irClass.properties
            .filter { it.backingField != null && !it.isDelegated && !it.isIgnoredForAll() && !it.isIgnoredForFromMap() }
            .forEach { prop ->
                val propertyName = prop.name.asString()
                val mapGet = irCall(mapGetSymbol).apply {
                    dispatchReceiver = mapParam()
                    arguments[1] = irString(propertyName)
                }
                val fieldType = prop.backingField!!.type
                val castType = if (fieldType.isNullable()) fieldType else fieldType.makeNullable()
                val cast = IrTypeOperatorCallImpl(
                    startOffset, endOffset,
                    castType,
                    IrTypeOperator.SAFE_CAST,
                    castType,
                    mapGet
                )
                val containsKey = irCall(mapContainsKeySymbol).apply {
                    dispatchReceiver = mapParam()
                    arguments[1] = irString(propertyName)
                }
                +irIfThen(
                    context.irBuiltIns.unitType,
                    containsKey,
                    irSetField(dispatcher(), prop.backingField!!, cast)
                )
            }
        +irReturn(dispatcher())
    }

/**
 * Generates `safeFromMapData` with one `getSafeValue` call per present field.
 *
 * The generated call receives the complete field `KType` and serialization flag, making it
 * the single ValueCodecRegistry decode boundary for enum, temporal, serialized, and custom
 * values. Missing keys are not decoded or assigned, and properties ignored for FROM_MAP are
 * omitted.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createSafeFromMapData(irClass: IrClass, irFunction: IrFunction): IrBlockBody =
    irBlockBody {
        fun dispatcher() = irGet(irFunction.parameters.dispatchReceiver!!)
        fun mapParam() = irGet(irFunction.parameters.valueParameters.first())
        val mapContainsKeySymbol = context.irBuiltIns.mapClass.getSimpleFunction("containsKey")!!

        irClass.properties
            .filter { it.backingField != null && !it.isDelegated && !it.isIgnoredForAll() && !it.isIgnoredForFromMap() }
            .forEach { prop ->
                val fieldType = prop.backingField!!.type
                val propertyName = prop.name.asString()
                val kTypeExpr = irCall(typeOfFunctionSymbol).apply {
                    typeArguments[0] = fieldType
                }
                val isSerializable = irBoolean(prop.hasAnnotation(SerializeAnnotationFqName))

                val safeValueCall = irCall(getSafeValueSymbol).apply {
                    arguments[0] = dispatcher()
                    arguments[1] = kTypeExpr
                    arguments[2] = mapParam()
                    arguments[3] = irString(propertyName)
                    arguments[4] = isSerializable
                }

                val cast = IrTypeOperatorCallImpl(
                    startOffset, endOffset,
                    fieldType,
                    IrTypeOperator.CAST,
                    fieldType,
                    safeValueCall
                )
                val containsKey = irCall(mapContainsKeySymbol).apply {
                    dispatchReceiver = mapParam()
                    arguments[1] = irString(propertyName)
                }
                +irIfThen(
                    context.irBuiltIns.unitType,
                    containsKey,
                    irSetField(dispatcher(), prop.backingField!!, cast)
                )
            }
        +irReturn(dispatcher())
    }

// ============================================================================
// Private helpers
// ============================================================================

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
internal fun IrBuilderWithScope.getTableNameExpr(irClass: IrClass): IrExpression {
    val tableAnnotation = irClass.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == TableAnnotationFqName
    }
    return if (tableAnnotation != null) {
        tableAnnotation.arguments[0].stringValueOrNull()?.let { irString(it) }
            ?: error("@Table name for ${irClass.name.asString()} is not a string constant")
    } else {
        buildTableNamingStrategyCall(irClass.name.asString())
    }
}

/**
 * Generates: Kronos.tableNamingStrategy.k2db(className)
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
private fun IrBuilderWithScope.buildTableNamingStrategyCall(className: String): IrExpression {
    val strategyGetter = irCall(tableNamingStrategyGetterSymbol).apply {
        dispatchReceiver = irGetObject(kronosObjectSymbol)
    }
    return irCall(k2dbFunctionSymbol).apply {
        dispatchReceiver = strategyGetter
        arguments[1] = irString(className)
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
private fun IrBuilderWithScope.buildMutableListOf(
    elementType: org.jetbrains.kotlin.ir.types.IrType,
    elements: List<IrExpression>
): IrExpression {
    val mutableListOfFunctions = context.referenceFunctions(
        CallableId(FqName("kotlin.collections"), null, Name.identifier("mutableListOf"))
    )
    val mutableListOfSymbol = mutableListOfFunctions.first {
        it.owner.parameters.valueParameters.singleOrNull()?.isVararg == true
    }
    return irCall(mutableListOfSymbol).apply {
        typeArguments[0] = elementType
        arguments[0] = irVararg(elementType, elements)
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
private fun IrBuilderWithScope.buildMutableMapOf(
    keyType: org.jetbrains.kotlin.ir.types.IrType,
    valueType: org.jetbrains.kotlin.ir.types.IrType,
    pairs: List<IrExpression>
): IrExpression {
    val mutableMapOfFunctions = context.referenceFunctions(
        CallableId(FqName("kotlin.collections"), null, Name.identifier("mutableMapOf"))
    )
    val mutableMapOfSymbol = mutableMapOfFunctions.first {
        it.owner.parameters.valueParameters.singleOrNull()?.isVararg == true
    }
    val pairType = pairClassSymbol.typeWith(keyType, valueType)
    return irCall(mutableMapOfSymbol).apply {
        typeArguments[0] = keyType
        typeArguments[1] = valueType
        arguments[0] = irVararg(pairType, pairs)
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
private fun IrBuilderWithScope.buildPairOf(first: IrExpression, second: IrExpression): IrExpression {
    val pairConstructor = pairClassSymbol.constructors.first()
    return irCall(pairConstructor).apply {
        typeArguments[0] = context.irBuiltIns.stringType
        typeArguments[1] = context.irBuiltIns.anyNType
        arguments[0] = first
        arguments[1] = second
    }
}

// ============================================================================
// @Ignore annotation helpers
// ============================================================================

/**
 * Gets the @Ignore annotation constructor call from a property, if present.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.ignoreAnnotationValue(): IrConstructorCall? {
    return annotations.find { it.symbol.owner.returnType.classFqName == IgnoreAnnotationFqName }
}

/**
 * Checks if an @Ignore annotation includes a specific action name (e.g., "all", "to_map", "from_map").
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrConstructorCall?.ignore(name: String): Boolean {
    if (this == null) return false
    val action = arguments[0] ?: return true
    return (
        action is IrVarargImpl &&
        action.elements.isNotEmpty() &&
        action.elements.any {
            it is IrGetEnumValueImpl && it.symbol.owner.name.asString() == name.uppercase()
        }
    )
}

/**
 * Checks if a property is annotated with @Ignore([IgnoreAction.ALL]).
 */
fun IrProperty.isIgnoredForAll(): Boolean = ignoreAnnotationValue().ignore("all")

fun IrProperty.isIgnoredForToMap(): Boolean = ignoreAnnotationValue().ignore("to_map")

fun IrProperty.isIgnoredForFromMap(): Boolean = ignoreAnnotationValue().ignore("from_map")

private fun IrExpression?.stringValueOrNull(): String? = (this as? IrConst)?.value as? String

private fun IrExpression?.stringValue(default: String = ""): String = stringValueOrNull() ?: default

private fun IrExpression?.booleanValue(default: Boolean = false): Boolean = (this as? IrConst)?.value as? Boolean ?: default

private fun IrExpression?.stringArrayValue(): List<String> {
    val vararg = this as? IrVarargImpl ?: return emptyList()
    return vararg.elements.filterIsInstance<IrConst>().map { it.value as String }
}
