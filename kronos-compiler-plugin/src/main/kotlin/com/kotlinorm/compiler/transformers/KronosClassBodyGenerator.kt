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

import com.kotlinorm.compiler.core.buildFieldFromProperty
import com.kotlinorm.compiler.core.fieldClassSymbol
import com.kotlinorm.compiler.core.fieldConstructorSymbol
import com.kotlinorm.compiler.core.isColumnType
import com.kotlinorm.compiler.core.getSafeValueSymbol
import com.kotlinorm.compiler.core.kronosCommonStrategyConstructorSymbol
import com.kotlinorm.compiler.core.kronosObjectSymbol
import com.kotlinorm.compiler.core.k2dbFunctionSymbol
import com.kotlinorm.compiler.core.tableNamingStrategyGetterSymbol
import com.kotlinorm.compiler.core.kClassSymbol
import com.kotlinorm.compiler.core.pairClassSymbol
import com.kotlinorm.compiler.utils.AnnotationFqNames
import com.kotlinorm.compiler.utils.IgnoreAnnotationFqName
import com.kotlinorm.compiler.utils.SerializeAnnotationFqName
import com.kotlinorm.compiler.utils.TableAnnotationFqName
import com.kotlinorm.compiler.utils.dispatchReceiver
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
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
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
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Kronos Class Body Generator
 *
 * Provides `DeclarationIrBuilder` extension functions that generate the IR bodies
 * for each KPojo interface method implemented by [KronosIrClassTransformer].
 *
 * Each function produces an `IrBlockBody` that is assigned to the corresponding
 * fake-override declaration:
 * - [createKronosColumns] — builds a `listOf(Field(...), ...)` expression.
 * - [createTableName] — returns the table name derived from `@Table` or the class name.
 * - [createTableComment] — returns the table comment string.
 * - [createKronosTableIndex] — returns the list of table indexes.
 * - [createKronosSpecialField] — returns the field annotated with a special annotation, or null.
 * - [createKClassFunction] — returns the `KClass` reference of the class.
 * - [createToDataMap] — serializes all column properties into a `MutableMap`.
 * - [createPropertyGetter] / [createPropertySetter] — dynamic property access by name.
 * - [createFromMapData] — populates the instance from a `Map<String, Any?>`.
 */

// ============================================================================
// Body generation functions — extension on DeclarationIrBuilder
// ============================================================================

/**
 * Generates: override fun kronosColumns() = listOf(Field(...), ...)
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createKronosColumns(irClass: IrClass): IrBlockBody {
    val fields = irClass.properties
        .filter { !it.isIgnoredForAll() }
        .map { buildFieldFromProperty(it) }
        .toList()
    return irBlockBody {
        +irReturn(buildListOf(fieldClassSymbol.defaultType, fields))
    }
}

/**
 * Generates: var __tableName: String = "table_name"
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createTableName(irClass: IrClass): IrExpressionBody =
    irExprBody(getTableNameExpr(irClass))

/**
 * Generates: var __tableComment: String = ""
 */
context(context: IrPluginContext)
fun DeclarationIrBuilder.createTableComment(@Suppress("UNUSED_PARAMETER") irClass: IrClass): IrExpressionBody =
    irExprBody(irString(""))

/**
 * Generates: override fun kronosTableIndex() = listOf()
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createKronosTableIndex(@Suppress("UNUSED_PARAMETER") irClass: IrClass): IrBlockBody =
    irBlockBody {
        val kTableIndexClassId = ClassId.topLevel(FqName("com.kotlinorm.beans.dsl.KTableIndex"))
        val kTableIndexSymbol = context.referenceClass(kTableIndexClassId)
        val listType = kTableIndexSymbol?.defaultType ?: context.irBuiltIns.anyType
        +irReturn(buildListOf(listType, emptyList()))
    }

/**
 * Generates: override fun kronosCreateTime/UpdateTime/LogicDelete/OptimisticLock(): KronosCommonStrategy
 *
 * If a property with the given annotation is found:
 *   return KronosCommonStrategy(enabled, Field("column_name", "propertyName"))
 * Otherwise:
 *   return the global default strategy from Kronos object
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createKronosSpecialField(irClass: IrClass, annotationFqName: FqName): IrBlockBody {
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
            return irBlockBody {
                +irReturn(
                    irCall(kronosCommonStrategyConstructorSymbol).apply {
                        arguments[0] = irBoolean(false)
                        arguments[1] = irCall(fieldConstructorSymbol).apply {
                            arguments[0] = irString("")
                            arguments[1] = irString("")
                        }
                    }
                )
            }
        }
    }

    val prop = irClass.properties.firstOrNull { it.hasAnnotation(annotationFqName) }
    return irBlockBody {
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
            +irReturn(
                irCall(kronosCommonStrategyConstructorSymbol).apply {
                    arguments[0] = irBoolean(enabled)
                    arguments[1] = fieldExpr
                }
            )
        } else {
            // No annotated property — return the global default from Kronos object
            val strategyName = when (annotationFqName) {
                AnnotationFqNames.CreateTime -> "createTimeStrategy"
                AnnotationFqNames.UpdateTime -> "updateTimeStrategy"
                AnnotationFqNames.LogicDelete -> "logicDeleteStrategy"
                AnnotationFqNames.Version -> "optimisticLockStrategy"
                else -> null
            }
            if (strategyName != null) {
                val getter = kronosObjectSymbol.owner.declarations
                    .filterIsInstance<IrProperty>()
                    .firstOrNull { it.name.asString() == strategyName }
                    ?.getter
                if (getter != null) {
                    +irReturn(
                        irCall(getter.symbol).apply {
                            dispatchReceiver = irGetObject(kronosObjectSymbol)
                        }
                    )
                } else {
                    +irReturn(irNull())
                }
            } else {
                +irReturn(irNull())
            }
        }
    }
}

/**
 * Generates: override fun kClass() = User::class
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createKClassFunction(irClass: IrClass): IrBlockBody =
    irBlockBody {
        val kClassRef = IrClassReferenceImpl(
            startOffset, endOffset,
            context.irBuiltIns.kClassClass.typeWith(irClass.defaultType),
            irClass.symbol,
            irClass.defaultType
        )
        +irReturn(kClassRef)
    }

/**
 * Generates: override fun toDataMap() = mutableMapOf("prop" to this.prop, ...)
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createToDataMap(irClass: IrClass, irFunction: IrFunction): IrBlockBody =
    irBlockBody {
        val dispatcher = irGet(irFunction.parameters.dispatchReceiver!!)
        val entries: List<IrExpression> = irClass.properties
            .filter { it.backingField != null && !it.isDelegated && !it.isIgnoredForAll() }
            .flatMap { prop ->
                listOf(
                    buildPairOf(
                        irString(prop.name.asString()),
                        irGetField(dispatcher, prop.backingField!!)
                    )
                )
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
        val dispatcher = irGet(irFunction.parameters.dispatchReceiver!!)
        val nameParam = irGet(irFunction.parameters.valueParameters.first())
        val branches = irClass.properties
            .filter { it.backingField != null && !it.isDelegated }
            .map { prop ->
                irBranch(
                    irEquals(nameParam, irString(prop.name.asString())),
                    irGetField(dispatcher, prop.backingField!!)
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
        val dispatcher = irGet(irFunction.parameters.dispatchReceiver!!)
        val nameParam = irGet(irFunction.parameters.valueParameters[0])
        val valueParam = irGet(irFunction.parameters.valueParameters[1])
        val branches = irClass.properties
            .filter { it.backingField != null && !it.isDelegated && it.isVar }
            .map { prop ->
                val fieldType = prop.backingField!!.type
                val castType = if (fieldType.isNullable()) fieldType else fieldType.makeNullable()
                irBranch(
                    irEquals(nameParam, irString(prop.name.asString())),
                    irSetField(
                        dispatcher,
                        prop.backingField!!,
                        IrTypeOperatorCallImpl(
                            startOffset, endOffset,
                            castType,
                            IrTypeOperator.SAFE_CAST,
                            castType,
                            valueParam
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
 * Generates: override fun fromMapData(map: Map<String, Any?>): KPojo { ... }
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createFromMapData(irClass: IrClass, irFunction: IrFunction): IrBlockBody =
    irBlockBody {
        val dispatcher = irGet(irFunction.parameters.dispatchReceiver!!)
        val mapParam = irGet(irFunction.parameters.valueParameters.first())
        val mapGetSymbol = context.irBuiltIns.mapClass.getSimpleFunction("get")!!

        irClass.properties
            .filter { it.backingField != null && !it.isDelegated && it.isVar && !it.isIgnoredForAll() }
            .forEach { prop ->
                val mapGet = irCall(mapGetSymbol).apply {
                    dispatchReceiver = mapParam
                    arguments[1] = irString(prop.name.asString())
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
                +irSetField(dispatcher, prop.backingField!!, cast)
            }
        +irReturn(dispatcher)
    }

/**
 * Generates: override fun safeFromMapData(map: Map<String, Any?>): KPojo { ... }
 * Uses getSafeValue() for type-safe conversion (handles enum, date, etc.)
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun DeclarationIrBuilder.createSafeFromMapData(irClass: IrClass, irFunction: IrFunction): IrBlockBody =
    irBlockBody {
        val dispatcher = irGet(irFunction.parameters.dispatchReceiver!!)
        val mapParam = irGet(irFunction.parameters.valueParameters.first())

        irClass.properties
            .filter { it.backingField != null && !it.isDelegated && it.isVar && !it.isIgnoredForAll() }
            .forEach { prop ->
                val fieldType = prop.backingField!!.type
                // Build: getSafeValue(this, FieldType::class, listOf(supertype1, ...), map, "propName", isSerializable)
                val kClassRef = IrClassReferenceImpl(
                    startOffset, endOffset,
                    kClassSymbol.typeWith(fieldType),
                    fieldType.classOrNull ?: context.irBuiltIns.anyClass,
                    fieldType
                )
                val superTypesList = buildListOf(
                    context.irBuiltIns.stringType,
                    (fieldType.classOrNull?.owner?.superTypes ?: emptyList()).mapNotNull { st ->
                        st.classFqName?.asString()?.let { irString(it) }
                    }
                )
                val isSerializable = irBoolean(prop.hasAnnotation(SerializeAnnotationFqName))

                val safeValueCall = irCall(getSafeValueSymbol).apply {
                    arguments[0] = dispatcher
                    arguments[1] = kClassRef
                    arguments[2] = superTypesList
                    arguments[3] = mapParam
                    arguments[4] = irString(prop.name.asString())
                    arguments[5] = isSerializable
                }

                val castType = if (fieldType.isNullable()) fieldType else fieldType.makeNullable()
                val cast = IrTypeOperatorCallImpl(
                    startOffset, endOffset,
                    castType,
                    IrTypeOperator.SAFE_CAST,
                    castType,
                    safeValueCall
                )
                +irSetField(dispatcher, prop.backingField!!, cast)
            }
        +irReturn(dispatcher)
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
    return if (tableAnnotation != null && tableAnnotation.arguments.isNotEmpty()) {
        tableAnnotation.arguments[0] ?: buildTableNamingStrategyCall(irClass.name.asString())
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
private fun IrBuilderWithScope.buildListOf(
    elementType: org.jetbrains.kotlin.ir.types.IrType,
    elements: List<IrExpression>
): IrExpression {
    val listOfFunctions = context.referenceFunctions(
        CallableId(FqName("kotlin.collections"), null, Name.identifier("listOf"))
    )
    val listOfSymbol = listOfFunctions.first {
        it.owner.parameters.valueParameters.singleOrNull()?.isVararg == true
    }
    return irCall(listOfSymbol).apply {
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
