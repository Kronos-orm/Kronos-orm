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
import com.kotlinorm.compiler.utils.DebugLogger
import com.kotlinorm.compiler.utils.FieldClassId
import com.kotlinorm.compiler.utils.FieldFqName
import com.kotlinorm.compiler.utils.FunctionFieldClassId
import com.kotlinorm.compiler.utils.FunctionFieldFqName
import com.kotlinorm.compiler.utils.IgnoreAnnotationFqName
import com.kotlinorm.compiler.utils.KColumnTypeClassId
import com.kotlinorm.compiler.utils.KColumnTypeFqName
import com.kotlinorm.compiler.utils.KPojoClassId
import com.kotlinorm.compiler.utils.KPojoFqName
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
import com.kotlinorm.compiler.utils.PairClassId
import com.kotlinorm.compiler.utils.PairFqName
import com.kotlinorm.compiler.utils.SerializeAnnotationFqName
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
        DebugLogger.logSymbolResolution("kPojoClassSymbol", symbol != null, KPojoFqName.asString())
        return symbol ?: error("KPojo interface not found: ${KPojoFqName.asString()}")
    }

/**
 * Field class symbol
 */
context(context: IrPluginContext)
val fieldClassSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(FieldClassId)
        DebugLogger.logSymbolResolution("fieldClassSymbol", symbol != null, FieldFqName.asString())
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
 * FunctionField class symbol
 */
context(context: IrPluginContext)
val functionFieldClassSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(FunctionFieldClassId)
        DebugLogger.logSymbolResolution("functionFieldClassSymbol", symbol != null, FunctionFieldFqName.asString())
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
        DebugLogger.logSymbolResolution("pairClassSymbol", symbol != null, PairFqName.asString())
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
 * listOf function symbol (kotlin.collections.listOf with vararg)
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val listOfFunctionSymbol: IrSimpleFunctionSymbol
    get() {
        val functions = context.referenceFunctions(
            org.jetbrains.kotlin.name.CallableId(
                FqName("kotlin.collections"),
                null,
                org.jetbrains.kotlin.name.Name.identifier("listOf")
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
        DebugLogger.logSymbolResolution("criteriaClassSymbol", symbol != null, CriteriaFqName.asString())
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
        DebugLogger.logSymbolResolution("kColumnTypeSymbol", symbol != null, KColumnTypeFqName.asString())
        return symbol ?: error("KColumnType enum not found: ${KColumnTypeFqName.asString()}")
    }

/**
 * KTableForSelect class symbol
 */
context(context: IrPluginContext)
val kTableForSelectSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KTableForSelectClassId)
        DebugLogger.logSymbolResolution("kTableForSelectSymbol", symbol != null, KTableForSelectFqName.asString())
        return symbol ?: error("KTableForSelect class not found: ${KTableForSelectFqName.asString()}")
    }

/**
 * KTableForSet class symbol
 */
context(context: IrPluginContext)
val kTableForSetSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KTableForSetClassId)
        DebugLogger.logSymbolResolution("kTableForSetSymbol", symbol != null, KTableForSetFqName.asString())
        return symbol ?: error("KTableForSet class not found: ${KTableForSetFqName.asString()}")
    }

/**
 * KTableForCondition class symbol
 */
context(context: IrPluginContext)
val kTableForConditionSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KTableForConditionClassId)
        DebugLogger.logSymbolResolution("kTableForConditionSymbol", symbol != null, KTableForConditionFqName.asString())
        return symbol ?: error("KTableForCondition class not found: ${KTableForConditionFqName.asString()}")
    }

/**
 * KTableForSort class symbol
 */
context(context: IrPluginContext)
val kTableForSortSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KTableForSortClassId)
        DebugLogger.logSymbolResolution("kTableForSortSymbol", symbol != null, KTableForSortFqName.asString())
        return symbol ?: error("KTableForSort class not found: ${KTableForSortFqName.asString()}")
    }

/**
 * KTableForReference class symbol
 */
context(context: IrPluginContext)
val kTableForReferenceSymbol: IrClassSymbol
    get() {
        val symbol = context.referenceClass(KTableForReferenceClassId)
        DebugLogger.logSymbolResolution("kTableForReferenceSymbol", symbol != null, KTableForReferenceFqName.asString())
        return symbol ?: error("KTableForReference class not found: ${KTableForReferenceFqName.asString()}")
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
        DebugLogger.logSymbolResolution("addFieldMethodSymbol", symbol != null, null)
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
        DebugLogger.logSymbolResolution("setValueMethodSymbol", symbol != null, null)
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
        DebugLogger.logSymbolResolution("setAssignMethodSymbol", symbol != null, null)
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
        DebugLogger.logSymbolResolution("setCriteriaMethodSymbol", symbol != null, null)
        return symbol ?: error("criteria property setter not found in KTableForCondition")
    }

/**
 * addSortField method symbol from KTableForSort
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val addSortFieldMethodSymbol: IrSimpleFunctionSymbol
    get() {
        val symbol = kTableForSortSymbol.getSimpleFunction("addSortField")
        DebugLogger.logSymbolResolution("addSortFieldMethodSymbol", symbol != null, null)
        return symbol ?: error("addSortField method not found in KTableForSort")
    }

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
    // Check if the type itself is KPojo or if any of its supertypes is KPojo
    val result = classFqName == KPojoFqName || superTypes().any { it.classFqName == KPojoFqName }
    
    val reason = when {
        classFqName == KPojoFqName -> "type itself is KPojo"
        superTypes().any { it.classFqName == KPojoFqName } -> "supertype is KPojo"
        else -> "not KPojo"
    }
    
    DebugLogger.logTypeJudgment(this, "isKPojoType", result, reason)
    return result
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
    val propertyName = name.asString()
    
    // @Ignore means not a column
    if (hasAnnotation(IgnoreAnnotationFqName)) {
        DebugLogger.logPropertyJudgment(this, "isColumnType", false, "has @Ignore annotation")
        return false
    }
    
    // @Cascade means it's a relationship, not a column
    if (hasAnnotation(CascadeAnnotationFqName)) {
        DebugLogger.logPropertyJudgment(this, "isColumnType", false, "has @Cascade annotation")
        return false
    }
    
    // @Serialize forces it to be a column (will be serialized to JSON/text)
    if (hasAnnotation(SerializeAnnotationFqName)) {
        DebugLogger.logPropertyJudgment(this, "isColumnType", true, "has @Serialize annotation")
        return true
    }
    
    // Get the property type
    val propertyType = backingField?.type ?: context.irBuiltIns.anyNType
    
    // If the type is KPojo, it's a relationship, not a column
    if (propertyType.isKPojoType()) {
        DebugLogger.logPropertyJudgment(this, "isColumnType", false, "type is KPojo")
        return false
    }
    
    // If it's a collection/array of KPojo, it's also a relationship
    val elementType = propertyType.firstTypeArgument()
    if (elementType?.isKPojoType() == true) {
        DebugLogger.logPropertyJudgment(this, "isColumnType", false, "type is List<KPojo>")
        return false
    }
    
    // Otherwise, it's a regular column
    DebugLogger.logPropertyJudgment(this, "isColumnType", true, "regular column type")
    return true
}
