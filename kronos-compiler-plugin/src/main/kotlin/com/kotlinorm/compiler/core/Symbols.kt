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

package com.kotlinorm.compiler.core

import com.kotlinorm.compiler.utils.CascadeAnnotationFqName
import com.kotlinorm.compiler.utils.ConditionTypeClassId
import com.kotlinorm.compiler.utils.FieldClassId
import com.kotlinorm.compiler.utils.FieldFqName
import com.kotlinorm.compiler.utils.FunctionFieldClassId
import com.kotlinorm.compiler.utils.FunctionFieldFqName
import com.kotlinorm.compiler.utils.IgnoreAnnotationFqName
import com.kotlinorm.compiler.utils.KCascadeClassId
import com.kotlinorm.compiler.utils.KCascadeFqName
import com.kotlinorm.compiler.utils.KColumnTypeClassId
import com.kotlinorm.compiler.utils.KColumnTypeFqName
import com.kotlinorm.compiler.utils.KPojoClassId
import com.kotlinorm.compiler.utils.KPojoFqName
import com.kotlinorm.compiler.utils.KronosCommonStrategyClassId
import com.kotlinorm.compiler.utils.KronosObjectClassId
import com.kotlinorm.compiler.utils.KTableForConditionClassId
import com.kotlinorm.compiler.utils.KTableForConditionFqName
import com.kotlinorm.compiler.utils.KTableForReferenceClassId
import com.kotlinorm.compiler.utils.KTableForReferenceFqName
import com.kotlinorm.compiler.utils.KTableForSelectClassId
import com.kotlinorm.compiler.utils.KTableForSelectFqName
import com.kotlinorm.compiler.utils.KTableForSetClassId
import com.kotlinorm.compiler.utils.KTableForSetFqName
import com.kotlinorm.compiler.utils.KTableForSortClassId
import com.kotlinorm.compiler.utils.KTableForSortFqName
import com.kotlinorm.compiler.utils.CriteriaClassId
import com.kotlinorm.compiler.utils.CriteriaFqName
import com.kotlinorm.compiler.utils.KronosInitAnnotationFqName
import com.kotlinorm.compiler.utils.PairClassId
import com.kotlinorm.compiler.utils.PairFqName
import com.kotlinorm.compiler.utils.SerializeAnnotationFqName
import com.kotlinorm.compiler.utils.StringClassId
import com.kotlinorm.compiler.utils.valueParameters
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Symbol references for Kronos compiler plugin
 *
 * Uses context receivers and top-level properties to provide access to commonly used symbols
 */

// ============================================================================
// Symbol Resolution and Debugging
// ============================================================================

/**
 * Resolve and log all symbols for debugging
 */
context(context: IrPluginContext)
fun resolveAndLogAllSymbols() {
    // Resolve class symbols - each in its own try-catch
    runCatching { kPojoClassSymbol }
    runCatching { fieldClassSymbol }
    runCatching { criteriaClassSymbol }
    runCatching { kColumnTypeSymbol }
    runCatching { kTableForSelectSymbol }
    runCatching { kTableForSetSymbol }
    runCatching { kTableForConditionSymbol }
    runCatching { kTableForSortSymbol }
    runCatching { kTableForReferenceSymbol }
    
    // Resolve method symbols - each in its own try-catch
    runCatching { addFieldMethodSymbol }
    runCatching { setValueMethodSymbol }
    runCatching { setAssignMethodSymbol }
    runCatching { setCriteriaMethodSymbol }
    runCatching { addSortFieldMethodSymbol }
}

// ============================================================================
// Class Symbols
// ============================================================================

/**
 * KPojo interface symbol
 */
context(context: IrPluginContext)
val kPojoClassSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KPojoClassId)
        return symbol ?: error("KPojo interface not found: ${KPojoFqName.asString()}")
    }

/**
 * Field class symbol
 */
context(context: IrPluginContext)
val fieldClassSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(FieldClassId)
        return symbol ?: error("Field class not found: ${FieldFqName.asString()}")
    }

/**
 * Field constructor symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val fieldConstructorSymbol: IrConstructorSymbol
    get() = fieldClassSymbol.constructors.firstOrNull()
        ?: error("Field constructor not found")

/**
 * KCascade class symbol
 */
context(context: IrPluginContext)
val kCascadeClassSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KCascadeClassId)
        return symbol ?: error("KCascade class not found: ${KCascadeFqName.asString()}")
    }

/**
 * KCascade constructor symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val kCascadeConstructorSymbol: IrConstructorSymbol
    get() = kCascadeClassSymbol.constructors.firstOrNull()
        ?: error("KCascade constructor not found")

/**
 * FunctionField class symbol
 */
context(context: IrPluginContext)
val functionFieldClassSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(FunctionFieldClassId)
        return symbol ?: error("FunctionField class not found: ${FunctionFieldFqName.asString()}")
    }

/**
 * FunctionField constructor symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val functionFieldConstructorSymbol: IrConstructorSymbol
    get() = functionFieldClassSymbol.constructors.firstOrNull()
        ?: error("FunctionField constructor not found")

/**
 * Pair class symbol (kotlin.Pair)
 */
context(context: IrPluginContext)
val pairClassSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(PairClassId)
        return symbol ?: error("Pair class not found: ${PairFqName.asString()}")
    }

/**
 * Pair constructor symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val pairConstructorSymbol: IrConstructorSymbol
    get() = pairClassSymbol.constructors.firstOrNull()
        ?: error("Pair constructor not found")

/**
 * KronosCommonStrategy class symbol
 */
context(context: IrPluginContext)
val kronosCommonStrategyClassSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KronosCommonStrategyClassId)
        return symbol ?: error("KronosCommonStrategy class not found")
    }

/**
 * KronosCommonStrategy constructor symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val kronosCommonStrategyConstructorSymbol: IrConstructorSymbol
    get() = kronosCommonStrategyClassSymbol.constructors.firstOrNull()
        ?: error("KronosCommonStrategy constructor not found")

/**
 * Kronos object symbol
 */
context(context: IrPluginContext)
val kronosObjectSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KronosObjectClassId)
        return symbol ?: error("Kronos object not found")
    }

/**
 * Kronos.fieldNamingStrategy property getter symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val fieldNamingStrategyGetterSymbol: IrSimpleFunctionSymbol
    get() = kronosObjectSymbol.owner.declarations
        .filterIsInstance<IrProperty>()
        .first { it.name.asString() == "fieldNamingStrategy" }
        .getter!!.symbol

/**
 * Kronos.tableNamingStrategy property getter symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val tableNamingStrategyGetterSymbol: IrSimpleFunctionSymbol
    get() = kronosObjectSymbol.owner.declarations
        .filterIsInstance<IrProperty>()
        .first { it.name.asString() == "tableNamingStrategy" }
        .getter!!.symbol

/**
 * KronosNamingStrategy.k2db function symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val k2dbFunctionSymbol: IrSimpleFunctionSymbol
    get() {
        val namingStrategyClassId = ClassId.topLevel(FqName("com.kotlinorm.interfaces.KronosNamingStrategy"))
        val cls = context.referenceClass(namingStrategyClassId) ?: error("KronosNamingStrategy not found")
        return cls.owner.declarations
            .filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrSimpleFunction>()
            .first { it.name.asString() == "k2db" }
            .symbol
    }

/**
 * listOf function symbol (kotlin.collections.listOf with vararg)
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val listOfFunctionSymbol: IrSimpleFunctionSymbol
    get() {
        val functions = context.referenceFunctions(
            CallableId(
                FqName("kotlin.collections"),
                null,
                Name.identifier("listOf")
            )
        )
        return functions.first { 
            it.owner.parameters.valueParameters.singleOrNull()?.isVararg == true 
        }
    }

/**
 * mutableMapOf function symbol (kotlin.collections.mutableMapOf with vararg Pair)
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val mutableMapOfFunctionSymbol: IrSimpleFunctionSymbol
    get() {
        val functions = context.referenceFunctions(
            CallableId(
                FqName("kotlin.collections"),
                null,
                Name.identifier("mutableMapOf")
            )
        )
        return functions.first {
            it.owner.parameters.valueParameters.singleOrNull()?.isVararg == true
        }
    }

/**
 * Criteria class symbol
 */
context(context: IrPluginContext)
val criteriaClassSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(CriteriaClassId)
        return symbol ?: error("Criteria class not found: ${CriteriaFqName.asString()}")
    }

/**
 * Criteria constructor symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val criteriaConstructorSymbol: IrConstructorSymbol
    get() = criteriaClassSymbol.constructors.firstOrNull()
        ?: error("Criteria constructor not found")

/**
 * KColumnType enum symbol
 */
context(context: IrPluginContext)
val kColumnTypeSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KColumnTypeClassId)
        return symbol ?: error("KColumnType enum not found: ${KColumnTypeFqName.asString()}")
    }

/**
 * KTableForSelect class symbol
 */
context(context: IrPluginContext)
val kTableForSelectSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KTableForSelectClassId)
        return symbol ?: error("KTableForSelect class not found: ${KTableForSelectFqName.asString()}")
    }

/**
 * KTableForSet class symbol
 */
context(context: IrPluginContext)
val kTableForSetSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KTableForSetClassId)
        return symbol ?: error("KTableForSet class not found: ${KTableForSetFqName.asString()}")
    }

/**
 * KTableForCondition class symbol
 */
context(context: IrPluginContext)
val kTableForConditionSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KTableForConditionClassId)
        return symbol ?: error("KTableForCondition class not found: ${KTableForConditionFqName.asString()}")
    }

/**
 * KTableForSort class symbol
 */
context(context: IrPluginContext)
val kTableForSortSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KTableForSortClassId)
        return symbol ?: error("KTableForSort class not found: ${KTableForSortFqName.asString()}")
    }

/**
 * KTableForReference class symbol
 */
context(context: IrPluginContext)
val kTableForReferenceSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KTableForReferenceClassId)
        return symbol ?: error("KTableForReference class not found: ${KTableForReferenceFqName.asString()}")
    }

/**
 * addField method symbol from KTableForReference
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val addRefFieldSymbol: IrSimpleFunctionSymbol
    get() {
        val symbol = kTableForReferenceSymbol.getSimpleFunction("addField")
        return symbol ?: error("addField method not found in KTableForReference")
    }

// ============================================================================
// Method Symbols
// ============================================================================

/**
 * addField method symbol from KTableForSelect
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val addFieldMethodSymbol: IrSimpleFunctionSymbol
    get() {
        val symbol = kTableForSelectSymbol.getSimpleFunction("addField")
        return symbol ?: error("addField method not found in KTableForSelect")
    }

/**
 * setValue method symbol from KTableForSet
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val setValueMethodSymbol: IrSimpleFunctionSymbol
    get() {
        val symbol = kTableForSetSymbol.getSimpleFunction("setValue")
        return symbol ?: error("setValue method not found in KTableForSet")
    }

/**
 * setAssign method symbol from KTableForSet
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val setAssignMethodSymbol: IrSimpleFunctionSymbol
    get() {
        val symbol = kTableForSetSymbol.getSimpleFunction("setAssign")
        return symbol ?: error("setAssign method not found in KTableForSet")
    }

/**
 * criteria property setter symbol from KTableForCondition
 *
 * Note: KTableForCondition doesn't have a setCriteria method,
 * instead it has a criteria property with a setter
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val setCriteriaMethodSymbol: IrSimpleFunctionSymbol
    get() {
        val symbol = kTableForConditionSymbol.getPropertySetter("criteria")
        return symbol ?: error("criteria property setter not found in KTableForCondition")
    }

/**
 * getValueByFieldName method symbol from KTableForCondition
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val getValueByFieldNameMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.getSimpleFunction("getValueByFieldName")
        ?: error("getValueByFieldName method not found in KTableForCondition")

/**
 * addSortField method symbol from KTableForSort
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val addSortFieldMethodSymbol: IrSimpleFunctionSymbol
    get() {
        val symbol = kTableForSortSymbol.getSimpleFunction("addSortField")
        return symbol ?: error("addSortField method not found in KTableForSort")
    }

/**
 * asc method symbol from KTableForSort
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val ascMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForSortSymbol.getSimpleFunction("asc")
        ?: error("asc method not found in KTableForSort")

/**
 * desc method symbol from KTableForSort
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val descMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForSortSymbol.getSimpleFunction("desc")
        ?: error("desc method not found in KTableForSort")

/**
 * ConditionType enum class symbol
 */
context(context: IrPluginContext)
val conditionTypeEnumSymbol: IrClassSymbol
    get() = context.referenceClass(ConditionTypeClassId)
        ?: error("ConditionType enum not found in classpath. Ensure kronos-core is on the compile classpath.")

/**
 * addChild method symbol from Criteria
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val addChildMethodSymbol: IrSimpleFunctionSymbol
    get() {
        val symbol = criteriaClassSymbol.getSimpleFunction("addChild")
        return symbol ?: error("addChild method not found in Criteria")
    }

/**
 * KClass class symbol (kotlin.reflect.KClass)
 */
context(context: IrPluginContext)
val kClassSymbol: IrClassSymbol
    get() = context.referenceClass(
        org.jetbrains.kotlin.name.ClassId.topLevel(FqName("kotlin.reflect.KClass"))
    ) ?: error("kotlin.reflect.KClass not found in classpath")

/**
 * String class symbol
 */
context(context: IrPluginContext)
val stringClassSymbol: IrClassSymbol
    get() = context.referenceClass(StringClassId)
        ?: error("kotlin.String not found in classpath")

/**
 * String.plus function symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val stringPlusSymbol: IrSimpleFunctionSymbol
    get() = stringClassSymbol.getSimpleFunction("plus")
        ?: error("String.plus not found in classpath")

/**
 * kClassCreator property setter symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val kClassCreatorSetterSymbol: IrSimpleFunctionSymbol
    get() {
        val prop = context.referenceProperties(
            CallableId(FqName("com.kotlinorm.utils"), null, Name.identifier("kClassCreator"))
        ).firstOrNull() ?: error("kClassCreator property not found in com.kotlinorm.utils")
        return prop.owner.setter?.symbol ?: error("kClassCreator has no setter")
    }

/**
 * getSafeValue function symbol from com.kotlinorm.utils.CommonUtil
 */
context(context: IrPluginContext)
val getSafeValueSymbol: IrSimpleFunctionSymbol
    get() = context.referenceFunctions(
        CallableId(FqName("com.kotlinorm.utils"), null, Name.identifier("getSafeValue"))
    ).firstOrNull() ?: error("getSafeValue function not found in com.kotlinorm.utils")

// ============================================================================
// Type Judgment Extension Functions
// ============================================================================

/**
 * Checks if this type is a KPojo type
 *
 * @return true if this type is or implements KPojo interface
 */
context(context: IrPluginContext)
fun IrType.isKPojoType(): Boolean {
    return classFqName == KPojoFqName || superTypes().any { it.classFqName == KPojoFqName }
}

/**
 * Gets the first type argument of this type (for generic types like List<T>)
 *
 * @return the first type argument, or null if not available
 */
fun IrType.firstTypeArgument(): IrType? {
    return (this as? IrSimpleType)
        ?.arguments
        ?.firstOrNull()
        ?.let { typeArg ->
            when (typeArg) {
                is IrTypeProjection -> typeArg.type
                else -> null
            }
        }
}

/**
 * Checks if this property represents a database column
 *
 * A property is considered a column if:
 * - It doesn't have @Ignore annotation
 * - It doesn't have @Cascade annotation (relationships are not columns)
 * - Its type is not a KPojo or collection of KPojo (relationships are not columns)
 * - Exception: @Serialize annotation forces it to be a column (serialized as JSON/text)
 *
 * @return true if this property represents a column field
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun IrProperty.isColumnType(): Boolean {
    // @Ignore means not a column
    if (hasAnnotation(IgnoreAnnotationFqName)) return false

    // @Cascade means it's a relationship, not a column
    if (hasAnnotation(CascadeAnnotationFqName)) {
        return false
    }
    
    // @Serialize forces it to be a column (will be serialized to JSON/text)
    if (hasAnnotation(SerializeAnnotationFqName)) {
        return true
    }
    
    // Get the property type
    val propertyType = backingField?.type ?: context.irBuiltIns.anyNType
    
    // If the type is KPojo, it's a relationship, not a column
    if (propertyType.isKPojoType()) {
        return false
    }
    
    // If it's a collection/array of KPojo, it's also a relationship
    val elementType = propertyType.firstTypeArgument()
    if (elementType?.isKPojoType() == true) {
        return false
    }
    
    // Otherwise, it's a regular column
    return true
}
