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

package com.kotlinorm.compiler.core

import com.kotlinorm.compiler.utils.CascadeAnnotationFqName
import com.kotlinorm.compiler.utils.ColumnAnnotationFqName
import com.kotlinorm.compiler.utils.ColumnTypeAnnotationFqName
import com.kotlinorm.compiler.utils.DateTimeFormatAnnotationFqName
import com.kotlinorm.compiler.utils.DefaultValueAnnotationFqName
import com.kotlinorm.compiler.utils.ErrorMessages
import com.kotlinorm.compiler.utils.GeneratedProjectionPackageFqName
import com.kotlinorm.compiler.utils.IgnoreAnnotationFqName
import com.kotlinorm.compiler.utils.KSelectableFqName
import com.kotlinorm.compiler.utils.NonNullAnnotationFqName
import com.kotlinorm.compiler.utils.PrimaryKeyAnnotationFqName
import com.kotlinorm.compiler.utils.SerializeAnnotationFqName
import com.kotlinorm.compiler.utils.extensionReceiverArgument
import com.kotlinorm.compiler.utils.dispatchReceiverArgument
import com.kotlinorm.compiler.utils.funcName
import com.kotlinorm.compiler.utils.getValueArgumentSafe
import com.kotlinorm.compiler.utils.irListOf
import com.kotlinorm.compiler.utils.irPairOf
import com.kotlinorm.compiler.utils.isKronosFunction
import com.kotlinorm.compiler.utils.mapTypeToKColumnType
import com.kotlinorm.compiler.utils.valueArguments
import com.kotlinorm.compiler.backend.transformers.getTableNameExpr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Field analysis and construction
 *
 * Analyzes field expressions and directly constructs Field IR nodes.
 * This module handles all field-related transformations for KTableForSelect.
 */

/**
 * Analyzes field expression and builds Field IR expressions
 *
 * Main entry point for field analysis. Handles different types of expressions:
 * - IrCall: Property access, projection collection calls, minus operations, function calls
 * - IrPropertyReference: Property references (User::name)
 * - IrGetValue: KPojo instance (it)
 * - IrConst: String constants for custom SQL
 *
 * @param irFunction The function being transformed
 * @param expression The expression to analyze
 * @param errorReporter Error reporter for compilation errors
 * @return List of Field IR expressions
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun analyzeAndBuildFields(
    irFunction: IrFunction,
    expression: IrExpression,
    errorReporter: ErrorReporter
): List<IrExpression> {
    return when (expression) {
        is IrCall -> analyzeCallFields(irFunction, expression, errorReporter)
        is IrPropertyReference -> [buildFieldFromPropertyRef(expression, errorReporter)]
        is IrGetValue -> analyzeGetValueFields(irFunction, expression, errorReporter)
        is IrConst -> [buildCustomSqlField(expression)]
        is IrVararg -> expression.elements.flatMap { element ->
            if (element is IrExpression) analyzeAndBuildFields(irFunction, element, errorReporter) else emptyList()
        }
        else -> {
            // Unsupported expression type - don't report error, just return empty list
            // This allows the original expression to be preserved
            emptyList()
        }
    }
}

/**
 * Analyzes IrCall expressions for field extraction
 *
 * Handles:
 * - GET_PROPERTY: Property access (it.name)
 * - projection collection calls: Field combination ([it.name, it.age])
 * - MINUS: Field exclusion (it - User::password)
 * - Function calls: as_() for aliases
 *
 * @param irFunction The function being transformed
 * @param call The IrCall to analyze
 * @param errorReporter Error reporter for compilation errors
 * @return List of Field IR expressions
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun analyzeCallFields(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter
): List<IrExpression> {
    return when (call.origin) {
        IrStatementOrigin.GET_PROPERTY -> {
            // Property access: it.name
            [buildFieldFromPropertyAccess(call, errorReporter)]
        }

        IrStatementOrigin.PLUS -> [buildOperatorFunctionField(irFunction, call, errorReporter)]
        
        IrStatementOrigin.MINUS -> {
            val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument
            if (receiver?.type?.isKPojoType() == true) {
                // Field exclusion: it - it.password
                analyzeMinusFields(irFunction, call, errorReporter)
            } else {
                [buildOperatorFunctionField(irFunction, call, errorReporter)]
            }
        }
        
        else -> {
            // Check for special function calls
            val functionName = call.symbol.owner.name.asString()
            when (functionName) {
                "get", "of", "listOf", "mutableListOf", "setOf", "arrayOf" -> {
                    analyzeFieldProjection(irFunction, call, errorReporter)
                }

                "plus", "minus", "times", "div", "rem" -> [buildOperatorFunctionField(irFunction, call, errorReporter)]
                "unaryPlus" -> {
                    errorReporter.reportError(
                        call,
                        ErrorMessages.UNSUPPORTED_FIELD_OPERATOR,
                        ErrorMessages.UNSUPPORTED_FIELD_OPERATOR_FIX
                    )
                    emptyList()
                }

                "as_" -> {
                    // Alias: it.name.as_("alias")
                    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument
                    val aliasArg = call.getValueArgumentSafe(0)
                    if (receiver != null && aliasArg is IrConst) {
                        val fields = analyzeAndBuildFields(irFunction, receiver, errorReporter)
                        // Apply alias to the first field
                        if (fields.isNotEmpty()) {
                            val aliasValue = aliasArg.value as? String
                            if (aliasValue != null) {
                                [buildFieldWithAlias(fields.first(), aliasValue)]
                            } else {
                                fields
                            }
                        } else {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }
                
                else -> {
                    // Check if it's a Kronos function call (f.rand(), f.trunc(), etc.)
                    if (call.isKronosFunction()) {
                        [buildFunctionField(irFunction, call, errorReporter)]
                    } else {
                        // Unknown function call - don't report error
                        emptyList()
                    }
                }
            }
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrCall.operatorOperands(): List<IrExpression> {
    val operands = mutableListOf<IrExpression>()
    (extensionReceiverArgument ?: dispatchReceiverArgument)?.let { operands += it }
    getValueArgumentSafe(0)?.let { operands += it }
    return operands
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrCall.operatorFunctionName(): String? {
    val rawName = symbol.owner.name.asString()
    return when {
        origin == IrStatementOrigin.PLUS || rawName == "plus" ->
            if (operatorOperands().any { it.type.isString() || it.type.classFqName?.asString() == "kotlin.CharSequence" }) "concat" else "add"
        origin == IrStatementOrigin.MINUS || rawName == "minus" -> "sub"
        rawName == "times" -> "mul"
        rawName == "div" -> "div"
        rawName == "rem" -> "mod"
        else -> null
    }
}

internal sealed class SelectProjectionIr {
    data class FieldProjection(val field: IrExpression) : SelectProjectionIr()
    data class ScalarSubqueryProjection(val query: IrExpression, val alias: String) : SelectProjectionIr()
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
internal fun analyzeAndBuildSelectProjections(
    irFunction: IrFunction,
    expression: IrExpression,
    errorReporter: ErrorReporter
): List<SelectProjectionIr> {
    return when {
        expression is IrCall && expression.isProjectionCollectionCall() -> {
            expression.valueArguments.flatMap { arg ->
                when (arg) {
                    is IrVararg -> arg.elements.flatMap { element ->
                        if (element is IrExpression) {
                            analyzeAndBuildSelectProjections(irFunction, element, errorReporter)
                        } else {
                            emptyList()
                        }
                    }
                    is IrExpression -> analyzeAndBuildSelectProjections(irFunction, arg, errorReporter)
                    else -> emptyList()
                }
            }
        }

        expression is IrCall && expression.symbol.owner.name.asString() == "as_" -> {
            val receiver = expression.extensionReceiverArgument ?: expression.dispatchReceiverArgument
            val aliasArg = expression.getValueArgumentSafe(0)
            if (receiver != null && receiver.type.isKSelectableType() && aliasArg is IrConst) {
                (aliasArg.value as? String)?.let { alias ->
                    listOf(SelectProjectionIr.ScalarSubqueryProjection(receiver, alias))
                } ?: emptyList()
            } else {
                analyzeAndBuildFields(irFunction, expression, errorReporter)
                    .map { SelectProjectionIr.FieldProjection(it) }
            }
        }

        else -> analyzeAndBuildFields(irFunction, expression, errorReporter)
            .map { SelectProjectionIr.FieldProjection(it) }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrCall.isProjectionCollectionCall(): Boolean {
    return when (symbol.owner.name.asString()) {
        "get", "of", "listOf", "mutableListOf", "setOf", "arrayOf" -> true
        else -> false
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
internal fun buildOperatorFunctionField(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter
): IrExpression {
    return buildFunctionField(irFunction, call, errorReporter, functionName = call.operatorFunctionName() ?: call.funcName())
}

context(context: IrPluginContext)
internal fun IrType.isKSelectableType(): Boolean {
    return classFqName == KSelectableFqName || superTypes().any { it.classFqName == KSelectableFqName }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun analyzeFieldProjection(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter
): List<IrExpression> {
    return call.valueArguments.flatMap { arg ->
        when (arg) {
            is IrVararg -> arg.elements.flatMap { element ->
                if (element is IrExpression) analyzeAndBuildFields(irFunction, element, errorReporter) else emptyList()
            }
            is IrExpression -> analyzeAndBuildFields(irFunction, arg, errorReporter)
            else -> emptyList()
        }
    }
}

/**
 * Builds a Field IR from property access (it.name)
 *
 * Extracts the property name and type from the property access call
 * and constructs a Field IR node.
 *
 * @param call The property access call
 * @param errorReporter Error reporter for compilation errors
 * @return Field IR expression
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun buildFieldFromPropertyAccess(
    call: IrCall,
    errorReporter: ErrorReporter
): IrExpression {
    // Get the property being accessed
    val propertyGetter = call.symbol.owner
    val propertyName = propertyGetter.correspondingPropertySymbol?.owner?.name?.asString()
        ?: propertyGetter.name.asString().removePrefix("get").replaceFirstChar { it.lowercase() }
    
    // Get the receiver type to find the property
    val receiverType = call.dispatchReceiver?.type
    val irClass = receiverType?.classOrNull?.owner
    
    if (irClass == null) {
        val errorMsg = ErrorMessages.cannotFindClassForProperty(propertyName, receiverType?.classFqName?.asString())
        errorReporter.reportWarning(call, errorMsg)
        // Return a simple field with just the name
        return buildSimpleField(propertyName, "VARCHAR")
    }

    if (irClass.isGeneratedProjectionClass()) {
        return buildSimpleField(propertyName, mapTypeToKColumnType(propertyGetter.returnType))
    }
    
    // Find the property in the class
    val irProperty = irClass.properties.firstOrNull { it.name.asString() == propertyName }
    
    if (irProperty == null) {
        val errorMsg = ErrorMessages.cannotFindPropertyInClass(propertyName, irClass.name.asString(), irClass.properties.joinToString { it.name.asString() })
        errorReporter.reportWarning(call, errorMsg)
        // Return a simple field with just the name
        return buildSimpleField(propertyName, "VARCHAR")
    }
    
    // Build field from property
    return buildFieldFromProperty(irProperty)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClass.isGeneratedProjectionClass(): Boolean =
    kotlinFqName.parent() == GeneratedProjectionPackageFqName

/**
 * Builds a Field IR from property reference (User::name)
 *
 * Extracts the property from the reference and constructs a Field IR node.
 *
 * @param propertyRef The property reference
 * @param errorReporter Error reporter for compilation errors
 * @return Field IR expression
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun buildFieldFromPropertyRef(
    propertyRef: IrPropertyReference,
    errorReporter: ErrorReporter
): IrExpression {
    val irProperty = propertyRef.symbol.owner
    return buildFieldFromProperty(irProperty)
}

/**
 * Builds a Field IR from an IrProperty
 *
 * This is the core function that constructs a Field IR node from property metadata.
 * It reads annotations and property type to determine field characteristics.
 *
 * @param irProperty The property to build field from
 * @return Field IR expression
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun buildFieldFromProperty(irProperty: IrProperty, metadataClass: IrClass? = null): IrExpression {
    val propertyName = irProperty.name.asString()

    // Get property type
    val propertyType = irProperty.backingField?.type ?: context.irBuiltIns.anyNType

    // Map type to KColumnType
    val columnTypeName = mapTypeToKColumnType(propertyType)

    // Check @Column annotation for custom column name
    val columnAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == ColumnAnnotationFqName
    }

    // columnName: @Column("custom") or Kronos.fieldNamingStrategy.k2db(propertyName)
    val columnNameExpr: IrExpression = columnAnnotation
        ?.arguments
        ?.getOrNull(0)
        ?.stringValueOrNull()
        ?.let { builder.irString(it) }
        ?: buildNamingStrategyCall(propertyName)

    // Check @ColumnType annotation
    val columnTypeAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == ColumnTypeAnnotationFqName
    }
    val columnTypeExpr = cloneEnumValue(columnTypeAnnotation?.arguments?.getOrNull(0)) {
        buildKColumnTypeEnum(columnTypeName)
    }
    val columnLengthExpr = columnTypeAnnotation?.arguments?.getOrNull(1)?.intValueOrNull()?.let { builder.irInt(it) }
    val columnScaleExpr = columnTypeAnnotation?.arguments?.getOrNull(2)?.intValueOrNull()?.let { builder.irInt(it) }

    // Check @PrimaryKey annotation
    val primaryKeyAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == PrimaryKeyAnnotationFqName
    }
    val primaryKeyExpr = buildPrimaryKeyType(primaryKeyAnnotation)

    // Check @DateTimeFormat annotation
    val dateTimeFormatAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == DateTimeFormatAnnotationFqName
    }
    val dateFormatExpr = dateTimeFormatAnnotation?.arguments?.getOrNull(0)
        ?.stringValueOrNull()
        ?.let { builder.irString(it) }

    // Table name
    val parent = irProperty.parent as? IrClass
    val tableNameOwner = metadataClass ?: parent
    val tableNameExpr = if (tableNameOwner != null) builder.getTableNameExpr(tableNameOwner) else builder.irString("")

    // Check @Default annotation
    val defaultValueAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == DefaultValueAnnotationFqName
    }
    val defaultValueExpr = defaultValueAnnotation?.arguments?.getOrNull(0)
        ?.stringValueOrNull()
        ?.let { builder.irString(it) }

    // Check @NonNull annotation
    val nonNullAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == NonNullAnnotationFqName
    }
    val nullable = nonNullAnnotation == null && primaryKeyAnnotation == null

    // Check @Serialize annotation
    val serializeAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == SerializeAnnotationFqName
    }
    val serializable = serializeAnnotation != null

    // Check @Cascade annotation
    val cascadeAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == CascadeAnnotationFqName
    }

    // Check @Ignore annotation
    val ignoreAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == IgnoreAnnotationFqName
    }

    // Cascade-related fields (following legacy plugin's getColumnName)
    val cascadeExpr: IrExpression = if (cascadeAnnotation != null) {
        builder.irCall(kCascadeConstructorSymbol).apply {
            arguments[0] = cloneStringVararg(cascadeAnnotation.arguments.getOrNull(0))
            arguments[1] = cloneStringVararg(cascadeAnnotation.arguments.getOrNull(1))
            arguments[2] = cloneEnumValue(cascadeAnnotation.arguments.getOrNull(2)) { null }
            arguments[3] = cloneStringVararg(cascadeAnnotation.arguments.getOrNull(3))
            arguments[4] = cloneEnumVararg(cascadeAnnotation.arguments.getOrNull(4))
        }
    } else {
        builder.irNull()
    }

    val arrayOrCollectionFqNames = arrayOf(
        FqName("kotlin.collections.Collection"),
        FqName("kotlin.collections.Iterator"),
        FqName("kotlin.Array"),
        FqName("kotlin.IntArray"),
        FqName("kotlin.LongArray"),
        FqName("kotlin.ShortArray"),
        FqName("kotlin.DoubleArray"),
        FqName("kotlin.FloatArray"),
        FqName("kotlin.CharArray"),
        FqName("kotlin.ByteArray"),
        FqName("kotlin.BooleanArray"),
    )
    val cascadeIsCollectionOrArray = propertyType.superTypes().any { it.classFqName in arrayOrCollectionFqNames }

    val propKClass = propertyType.classOrNull
    val kClassExpr: IrExpression = if (irProperty.isDelegated) {
        builder.irNull()
    } else {
        val targetClassSymbol = if (cascadeIsCollectionOrArray) {
            propertyType.firstTypeArgument()?.classOrNull
        } else {
            propKClass
        }
        if (targetClassSymbol != null) {
            val classType = targetClassSymbol.defaultType
            IrClassReferenceImpl(
                builder.startOffset, builder.endOffset,
                context.irBuiltIns.kClassClass.typeWith(classType),
                targetClassSymbol,
                classType
            )
        } else {
            builder.irNull()
        }
    }

    val superTypesExprs = propKClass?.owner?.superTypes?.mapNotNull {
        it.classFqName?.asString()?.let { fqn -> builder.irString(fqn) }
    } ?: emptyList()
    val superTypesExpr = irListOf(context.irBuiltIns.stringType, superTypesExprs)

    val ignoreExpr: IrExpression = cloneEnumVararg(ignoreAnnotation?.arguments?.getOrNull(0)) ?: builder.irNull()

    val isColumn = irProperty.isColumnType()

    // Build Field constructor call with all parameters
    return builder.irCall(fieldConstructorSymbol).apply {
        arguments[0] = columnNameExpr                          // columnName
        arguments[1] = builder.irString(propertyName)          // name
        arguments[2] = columnTypeExpr                          // type
        arguments[3] = primaryKeyExpr                          // primaryKey
        arguments[4] = dateFormatExpr                          // dateFormat
        arguments[5] = tableNameExpr                           // tableName
        arguments[6] = cascadeExpr                             // cascade
        arguments[7] = builder.irBoolean(cascadeIsCollectionOrArray) // cascadeIsCollectionOrArray
        arguments[8] = kClassExpr                              // kClass
        arguments[9] = superTypesExpr                          // superTypes
        arguments[10] = ignoreExpr                             // ignore
        arguments[11] = builder.irBoolean(isColumn)            // isColumn
        if (columnLengthExpr != null) arguments[12] = columnLengthExpr  // length
        if (columnScaleExpr != null) arguments[13] = columnScaleExpr    // scale
        arguments[14] = defaultValueExpr                       // defaultValue
        arguments[15] = builder.irBoolean(nullable)            // nullable
        arguments[16] = builder.irBoolean(serializable)        // serializable
    }
}

/**
 * Generates: Kronos.fieldNamingStrategy.k2db(propertyName)
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildNamingStrategyCall(propertyName: String): IrExpression {
    val strategyGetter = builder.irCall(fieldNamingStrategyGetterSymbol).apply {
        dispatchReceiver = builder.irGetObject(kronosObjectSymbol)
    }
    return builder.irCall(k2dbFunctionSymbol).apply {
        dispatchReceiver = strategyGetter
        arguments[1] = builder.irString(propertyName)
    }
}

/**
 * Builds a KColumnType enum value expression
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildKColumnTypeEnum(columnTypeName: String): IrExpression {
    val kColumnTypeEnumSymbol = kColumnTypeSymbol
    val kColumnTypeEnum = kColumnTypeEnumSymbol.owner
    val enumEntry = kColumnTypeEnum.declarations
        .filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrEnumEntry>()
        .firstOrNull { it.name.asString() == columnTypeName }
        ?: kColumnTypeEnum.declarations
            .filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrEnumEntry>()
            .first { it.name.asString() == "VARCHAR" }
    return IrGetEnumValueImpl(
        builder.startOffset, builder.endOffset,
        kColumnTypeEnumSymbol.defaultType,
        enumEntry.symbol
    )
}

/**
 * Builds a PrimaryKeyType enum value from @PrimaryKey annotation
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildPrimaryKeyType(primaryKeyAnnotation: IrConstructorCall?): IrExpression {
    val primaryKeyTypeClassId = ClassId.topLevel(FqName("com.kotlinorm.enums.PrimaryKeyType"))
    val primaryKeyTypeSymbol = context.referenceClass(primaryKeyTypeClassId) ?: error("PrimaryKeyType not found")
    val entries = primaryKeyTypeSymbol.owner.declarations
        .filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrEnumEntry>()

    val entryName = if (primaryKeyAnnotation == null) {
        "NOT"
    } else {
        val propsOfPrimaryKey = arrayOf("identity", "uuid", "snowflake", "custom")
        val enabledIndex = primaryKeyAnnotation.arguments
            .indexOfFirst { it is IrConst && it.value == true }
        when {
            enabledIndex < 0 -> "DEFAULT"
            else -> propsOfPrimaryKey.getOrNull(enabledIndex)?.uppercase() ?: "DEFAULT"
        }
    }

    val entry = entries.firstOrNull { it.name.asString() == entryName } ?: entries.first { it.name.asString() == "NOT" }
    return IrGetEnumValueImpl(
        builder.startOffset, builder.endOffset,
        primaryKeyTypeSymbol.defaultType,
        entry.symbol
    )
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildSimpleField(name: String, columnTypeName: String): IrExpression {
    // Get KColumnType enum value
    val kColumnTypeEnumSymbol = kColumnTypeSymbol
    val kColumnTypeEnum = kColumnTypeEnumSymbol.owner
    val enumEntry = kColumnTypeEnum.declarations
        .filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrEnumEntry>()
        .firstOrNull { it.name.asString() == columnTypeName }
    
    val columnTypeValue = if (enumEntry != null) {
        IrGetEnumValueImpl(
            builder.startOffset,
            builder.endOffset,
            kColumnTypeEnumSymbol.defaultType,
            enumEntry.symbol
        )
    } else {
        // Fallback to VARCHAR if enum entry not found
        val varcharEntry = kColumnTypeEnum.declarations
            .filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrEnumEntry>()
            .first { it.name.asString() == "VARCHAR" }
        IrGetEnumValueImpl(
            builder.startOffset,
            builder.endOffset,
            kColumnTypeEnumSymbol.defaultType,
            varcharEntry.symbol
        )
    }
    
    // Construct Field(columnName, name, type)
    return builder.irCall(fieldConstructorSymbol).apply {
        // Parameter 0: columnName (String)
        arguments[0] = builder.irString(name)
        
        // Parameter 1: name (String) - defaults to columnName
        arguments[1] = builder.irString(name)
        
        // Parameter 2: type (KColumnType)
        arguments[2] = columnTypeValue
    }
}

/**
 * Builds a Field IR with an alias
 *
 * Takes an existing field IR and wraps it with a setAlias call.
 *
 * @param fieldExpr The field expression
 * @param alias The alias name
 * @return Field IR expression with alias
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildFieldWithAlias(fieldExpr: IrExpression, alias: String): IrExpression {
    // Field.name is a var — use the setter to set the alias
    val nameSetter = fieldClassSymbol.owner.declarations
        .filterIsInstance<IrProperty>()
        .first { it.name.asString() == "name" }
        .setter!!.symbol
    // Call setName on the field and return it using also { it.name = alias }
    return builder.irBlock {
        val fieldVar = irTemporary(fieldExpr, nameHint = "aliasedField")
        +irCall(nameSetter).apply {
            dispatchReceiver = irGet(fieldVar)
            arguments[1] = irString(alias)
        }
        +irGet(fieldVar)
    }
}

/**
 * Analyzes minus expressions for field exclusion (it - User::password)
 *
 * Gets all fields from the KPojo and excludes the specified fields.
 *
 * @param irFunction The function being transformed
 * @param call The minus operation call
 * @param errorReporter Error reporter for compilation errors
 * @return List of Field IR expressions (all fields except excluded ones)
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun analyzeMinusFields(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter
): List<IrExpression> {
    // Get the receiver (should be KPojo instance)
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument
    val receiverType = receiver?.type
    val irClass = receiverType?.classOrNull?.owner
    
    if (irClass == null) {
        val errorMsg = ErrorMessages.cannotFindClassForMinus(receiverType?.classFqName?.asString())
        errorReporter.reportWarning(call, errorMsg)
        return emptyList()
    }
    
    // Get the excluded fields
    val excludedFields = mutableSetOf<String>()
    val argument = call.getValueArgumentSafe(0)
    
    // Recursively collect excluded field names
    collectExcludedFieldNames(argument, excludedFields)
    
    // Get all column properties except excluded ones
    val fields = mutableListOf<IrExpression>()
    val columnProperties = irClass.properties.filter { it.isColumnType() }.toList()
    
    if (columnProperties.isEmpty()) {
        val errorMsg = ErrorMessages.noColumnPropertiesForMinus(irClass.name.asString())
        errorReporter.reportWarning(call, errorMsg)
    }
    
    columnProperties.forEach { prop ->
        if (prop.name.asString() !in excludedFields) {
            fields.add(buildFieldFromProperty(prop))
        }
    }
    
    return fields
}

/**
 * Collects excluded field names from an expression
 *
 * Recursively traverses the expression to find all property references
 * that should be excluded.
 *
 * @param expression The expression to analyze
 * @param excludedFields Set to collect excluded field names
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun collectExcludedFieldNames(
    expression: IrExpression?,
    excludedFields: MutableSet<String>
) {
    when (expression) {
        is IrPropertyReference -> {
            excludedFields.add(expression.symbol.owner.name.asString())
        }

        is IrCall -> {
            val functionName = expression.symbol.owner.name.asString()
            if (functionName in setOf("get", "of", "listOf", "mutableListOf", "setOf", "arrayOf")) {
                expression.valueArguments.forEach { arg ->
                    when (arg) {
                        is IrVararg -> arg.elements.forEach { element ->
                            collectExcludedFieldNames(element as? IrExpression, excludedFields)
                        }
                        is IrExpression -> collectExcludedFieldNames(arg, excludedFields)
                    }
                }
            } else if (expression.origin == IrStatementOrigin.GET_PROPERTY) {
                // Handle property access: it.username
                val propName = expression.symbol.owner.correspondingPropertySymbol?.owner?.name?.asString()
                if (propName != null) excludedFields.add(propName)
            }
        }

        else -> {
            // Ignore other expression types
        }
    }
}

/**
 * Analyzes IrGetValue for all fields (it)
 *
 * When the expression is just "it" (the KPojo instance), returns all column fields.
 *
 * @param irFunction The function being transformed
 * @param getValue The IrGetValue expression
 * @param errorReporter Error reporter for compilation errors
 * @return List of Field IR expressions (all column fields)
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun analyzeGetValueFields(
    irFunction: IrFunction,
    getValue: IrGetValue,
    errorReporter: ErrorReporter
): List<IrExpression> {
    // Check if this is the extension receiver parameter (it)
    val valueType = getValue.type
    val irClass = valueType.classOrNull?.owner ?: return emptyList()

    // Check if it's a KPojo type
    if (!valueType.isKPojoType()) {
        return emptyList()
    }
    
    // Get all column properties
    val fields = mutableListOf<IrExpression>()
    irClass.properties.forEach { prop ->
        if (prop.isColumnType()) {
            fields.add(buildFieldFromProperty(prop))
        }
    }
    
    return fields
}

/**
 * Builds a custom SQL field from a string constant
 *
 * Creates a Field with CUSTOM_CRITERIA_SQL type for raw SQL expressions.
 *
 * @param const The string constant
 * @return Field IR expression
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun buildCustomSqlField(const: IrConst): IrExpression {
    val sqlValue = const.value?.toString() ?: ""
    
    // Get CUSTOM_CRITERIA_SQL enum value
    val kColumnTypeEnumSymbol = kColumnTypeSymbol
    val kColumnTypeEnum = kColumnTypeEnumSymbol.owner
    val customSqlEntry = kColumnTypeEnum.declarations
        .filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrEnumEntry>()
        .first { it.name.asString() == "CUSTOM_CRITERIA_SQL" }
    
    val columnTypeValue = IrGetEnumValueImpl(
        builder.startOffset,
        builder.endOffset,
        kColumnTypeEnumSymbol.defaultType,
        customSqlEntry.symbol
    )
    
    // Construct Field(columnName, name, type)
    return builder.irCall(fieldConstructorSymbol).apply {
        // Parameter 0: columnName (the SQL string)
        arguments[0] = builder.irString(sqlValue)
        
        // Parameter 1: name (same as columnName)
        arguments[1] = builder.irString(sqlValue)
        
        // Parameter 2: type (CUSTOM_CRITERIA_SQL)
        arguments[2] = columnTypeValue
    }
}

/**
 * Builds a FunctionField IR from a Kronos function call
 *
 * Handles function calls like f.rand(), f.trunc(it.score, 2), f.abs(f.trunc(it.score, 1))
 * Recursively processes function arguments to support nested functions.
 *
 * @param irFunction The function being transformed
 * @param call The function call expression
 * @param errorReporter Error reporter for compilation errors
 * @return FunctionField IR expression
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun buildFunctionField(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter,
    functionName: String = call.funcName()
): IrExpression {
    // Build argument list: List<Pair<Field?, Any?>>
    val argumentPairs = mutableListOf<IrExpression>()

    // Get Field? type (nullable Field)
    val nullableFieldType = fieldClassSymbol.typeWith().makeNullable()
    val anyNullableType = context.irBuiltIns.anyNType

    fun processArg(arg: IrExpression) {
        // Check if argument is a Field (KPojo property or nested function)
        val fieldExpr = if (arg.isKronosFieldExpression()) {
            val fields = analyzeAndBuildFields(irFunction, arg, errorReporter)
            if (fields.isNotEmpty()) fields.first() else builder.irNull()
        } else {
            builder.irNull()
        }

        // Create Pair<Field?, Any?>(fieldExpr, arg copy). The original expression remains in the user's tree.
        argumentPairs.add(irPairOf(nullableFieldType, anyNullableType, fieldExpr, arg.deepCopyWithSymbols()))
    }

    // Process each value argument, unpacking varargs
    call.operatorOperands().takeIf { call.operatorFunctionName() != null }?.forEach { arg ->
        processArg(arg)
    } ?: call.valueArguments.forEach { arg ->
        if (arg != null) {
            if (arg is IrVararg) {
                // Unpack vararg elements
                arg.elements.forEach { element ->
                    if (element is IrExpression) {
                        processArg(element)
                    }
                }
            } else {
                processArg(arg)
            }
        }
    }

    // Build List<Pair<Field?, Any?>>
    val pairType = pairClassSymbol.typeWith(nullableFieldType, anyNullableType)

    val listExpr = irListOf(pairType, argumentPairs)

    // Construct FunctionField(functionName, fields)
    return builder.irCall(functionFieldConstructorSymbol).apply {
        // Parameter 0: functionName (String)
        arguments[0] = builder.irString(functionName)

        // Parameter 1: fields (List<Pair<Field?, Any?>>)
        arguments[1] = listExpr
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun cloneEnumValue(expression: IrExpression?, fallback: () -> IrExpression?): IrExpression? {
    val enumValue = expression as? IrGetEnumValue ?: return fallback()
    return IrGetEnumValueImpl(
        builder.startOffset,
        builder.endOffset,
        enumValue.type,
        enumValue.symbol
    )
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun cloneStringVararg(expression: IrExpression?): IrExpression? {
    val vararg = expression as? IrVararg ?: return null
    return builder.irVararg(
        vararg.varargElementType,
        vararg.elements.mapNotNull { element ->
            (element as? IrConst)?.value?.toString()?.let { builder.irString(it) }
        }
    )
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun cloneEnumVararg(expression: IrExpression?): IrExpression? {
    val vararg = expression as? IrVararg ?: return null
    return builder.irVararg(
        vararg.varargElementType,
        vararg.elements.mapNotNull { element ->
            cloneEnumValue(element as? IrExpression) { null }
        }
    )
}

private fun IrExpression?.stringValueOrNull(): String? = (this as? IrConst)?.value as? String

private fun IrExpression?.intValueOrNull(): Int? {
    val value = (this as? IrConst)?.value
    return when (value) {
        is Int -> value
        is Number -> value.toInt()
        else -> null
    }
}

/**
 * Checks if an expression is a KPojo property access
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrExpression.isKPojoProperty(): Boolean {
    return when (this) {
        is IrCall -> this.origin == IrStatementOrigin.GET_PROPERTY
        is IrPropertyReference -> true
        else -> false
    }
}

/**
 * Checks whether an expression should be lowered into a Field or FunctionField argument.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrExpression.isKronosFieldExpression(): Boolean {
    return isKPojoProperty() || (this is IrCall && (isKronosFunction() || operatorFunctionName() != null))
}
