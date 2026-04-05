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

import com.kotlinorm.compiler.utils.DebugLogger
import com.kotlinorm.compiler.utils.extensionReceiverArgument
import com.kotlinorm.compiler.utils.dispatchReceiverArgument
import com.kotlinorm.compiler.utils.funcName
import com.kotlinorm.compiler.utils.getValueArgumentSafe
import com.kotlinorm.compiler.utils.irListOf
import com.kotlinorm.compiler.utils.irPairOf
import com.kotlinorm.compiler.utils.isKronosFunction
import com.kotlinorm.compiler.utils.mapTypeToKColumnType
import com.kotlinorm.compiler.utils.valueArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.properties

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
 * - IrCall: Property access, plus/minus operations, function calls
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
context(context: IrPluginContext, builder: IrBlockBuilder)
fun analyzeAndBuildFields(
    irFunction: IrFunction,
    expression: IrExpression,
    errorReporter: ErrorReporter
): List<IrExpression> {
    return when (expression) {
        is IrCall -> analyzeCallFields(irFunction, expression, errorReporter)
        is IrPropertyReference -> listOf(buildFieldFromPropertyRef(expression, errorReporter))
        is IrGetValue -> analyzeGetValueFields(irFunction, expression, errorReporter)
        is IrConst -> listOf(buildCustomSqlField(expression))
        else -> {
            // Unsupported expression type - don't report error, just return empty list
            // This allows the original expression to be preserved
            DebugLogger.logInfo("Unsupported field expression type: ${expression::class.simpleName}")
            emptyList()
        }
    }
}

/**
 * Analyzes IrCall expressions for field extraction
 *
 * Handles:
 * - GET_PROPERTY: Property access (it.name)
 * - PLUS: Field combination (it.name + it.age)
 * - MINUS: Field exclusion (it - User::password)
 * - Function calls: as_() for aliases, unaryPlus for explicit field selection
 *
 * @param irFunction The function being transformed
 * @param call The IrCall to analyze
 * @param errorReporter Error reporter for compilation errors
 * @return List of Field IR expressions
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun analyzeCallFields(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter
): List<IrExpression> {
    return when (call.origin) {
        IrStatementOrigin.GET_PROPERTY -> {
            // Property access: it.name
            listOf(buildFieldFromPropertyAccess(call, errorReporter))
        }
        
        IrStatementOrigin.PLUS -> {
            // Plus operation: it.name + it.age
            analyzePlusFields(irFunction, call, errorReporter)
        }
        
        IrStatementOrigin.MINUS -> {
            // Minus operation: it - User::password
            analyzeMinusFields(irFunction, call, errorReporter)
        }
        
        else -> {
            // Check for special function calls
            val functionName = call.symbol.owner.name.asString()
            when (functionName) {
                "unaryPlus" -> {
                    // Unary plus: +it.name
                    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument
                    if (receiver != null) {
                        analyzeAndBuildFields(irFunction, receiver, errorReporter)
                    } else {
                        emptyList()
                    }
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
                                listOf(buildFieldWithAlias(fields.first(), aliasValue))
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
                        listOf(buildFunctionField(irFunction, call, errorReporter))
                    } else {
                        // Unknown function call - don't report error
                        DebugLogger.logInfo("Unknown function call in field analysis: $functionName")
                        emptyList()
                    }
                }
            }
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
context(context: IrPluginContext, builder: IrBlockBuilder)
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
        val errorMsg = "Cannot find class for property access: $propertyName. " +
                      "Receiver type: ${receiverType?.classFqName?.asString() ?: "unknown"}. " +
                      "This usually means the property is accessed on a non-KPojo type."
        DebugLogger.logInfo(errorMsg)
        errorReporter.reportWarning(call, errorMsg)
        // Return a simple field with just the name
        return buildSimpleField(propertyName, "VARCHAR")
    }
    
    // Find the property in the class
    val irProperty = irClass.properties.firstOrNull { it.name.asString() == propertyName }
    
    if (irProperty == null) {
        val errorMsg = "Cannot find property '$propertyName' in class ${irClass.name.asString()}. " +
                      "Available properties: ${irClass.properties.joinToString { it.name.asString() }}. " +
                      "Make sure the property exists and is accessible."
        DebugLogger.logInfo(errorMsg)
        errorReporter.reportWarning(call, errorMsg)
        // Return a simple field with just the name
        return buildSimpleField(propertyName, "VARCHAR")
    }
    
    // Build field from property
    return buildFieldFromProperty(irProperty)
}

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
context(context: IrPluginContext, builder: IrBlockBuilder)
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
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildFieldFromProperty(irProperty: IrProperty): IrExpression {
    val propertyName = irProperty.name.asString()
    
    // Get property type
    val propertyType = irProperty.backingField?.type ?: context.irBuiltIns.anyNType
    
    // Map type to KColumnType
    val columnTypeName = mapTypeToKColumnType(propertyType)
    
    // Build simple field (for now, we'll expand this later with annotations)
    return buildSimpleField(propertyName, columnTypeName)
}

/**
 * Builds a simple Field IR with name and type
 *
 * Constructs a Field constructor call with minimal parameters.
 *
 * @param name The field name
 * @param columnTypeName The KColumnType enum name
 * @return Field IR expression
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
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
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildFieldWithAlias(fieldExpr: IrExpression, alias: String): IrExpression {
    // For now, just return the field as-is
    // TODO: Implement setAlias call when we have the method symbol
    DebugLogger.logInfo("Alias not yet implemented: $alias")
    return fieldExpr
}

/**
 * Analyzes plus expressions for field combination (it.name + it.age)
 *
 * Recursively analyzes both sides of the plus operation and combines the results.
 *
 * @param irFunction The function being transformed
 * @param call The plus operation call
 * @param errorReporter Error reporter for compilation errors
 * @return List of Field IR expressions
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
fun analyzePlusFields(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter
): List<IrExpression> {
    val fields = mutableListOf<IrExpression>()
    
    // Analyze left side (receiver)
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument
    if (receiver != null) {
        fields.addAll(analyzeAndBuildFields(irFunction, receiver, errorReporter))
    }
    
    // Analyze right side (argument)
    val argument = call.getValueArgumentSafe(0)
    if (argument != null) {
        fields.addAll(analyzeAndBuildFields(irFunction, argument, errorReporter))
    }
    
    return fields
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
context(context: IrPluginContext, builder: IrBlockBuilder)
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
        val errorMsg = "Cannot find class for minus operation. " +
                      "Receiver type: ${receiverType?.classFqName?.asString() ?: "unknown"}. " +
                      "The minus operation (it - User::field) requires a KPojo instance on the left side."
        DebugLogger.logInfo(errorMsg)
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
        val errorMsg = "No column properties found in class ${irClass.name.asString()} for minus operation. " +
                      "Make sure the class has properties annotated as columns."
        DebugLogger.logInfo(errorMsg)
        errorReporter.reportWarning(call, errorMsg)
    }
    
    columnProperties.forEach { prop ->
        if (prop.name.asString() !in excludedFields) {
            fields.add(buildFieldFromProperty(prop))
        }
    }
    
    DebugLogger.logInfo("Minus operation: ${columnProperties.size} total properties, " +
                       "${excludedFields.size} excluded, ${fields.size} included")
    
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
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun collectExcludedFieldNames(
    expression: IrExpression?,
    excludedFields: MutableSet<String>
) {
    when (expression) {
        is IrPropertyReference -> {
            excludedFields.add(expression.symbol.owner.name.asString())
        }
        
        is IrCall -> {
            if (expression.origin == IrStatementOrigin.PLUS) {
                // Handle multiple exclusions: User::password + User::email
                val receiver = expression.extensionReceiverArgument ?: expression.dispatchReceiverArgument
                collectExcludedFieldNames(receiver, excludedFields)
                collectExcludedFieldNames(expression.getValueArgumentSafe(0), excludedFields)
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
context(context: IrPluginContext, builder: IrBlockBuilder)
fun analyzeGetValueFields(
    irFunction: IrFunction,
    getValue: IrGetValue,
    errorReporter: ErrorReporter
): List<IrExpression> {
    // Check if this is the extension receiver parameter (it)
    val valueType = getValue.type
    val irClass = valueType.classOrNull?.owner
    
    if (irClass == null) {
        DebugLogger.logInfo("Cannot find class for getValue expression")
        return emptyList()
    }
    
    // Check if it's a KPojo type
    if (!valueType.isKPojoType()) {
        DebugLogger.logInfo("getValue type is not KPojo: ${irClass.name}")
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
context(context: IrPluginContext, builder: IrBlockBuilder)
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
context(context: IrPluginContext, builder: IrBlockBuilder)
fun buildFunctionField(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter
): IrExpression {
    val functionName = call.funcName()
    
    // Build argument list: List<Pair<Field?, Any?>>
    val argumentPairs = mutableListOf<IrExpression>()
    
    // Get Field? type (nullable Field)
    val nullableFieldType = fieldClassSymbol.typeWith().makeNullable()
    val anyNullableType = context.irBuiltIns.anyNType
    
    // Process each value argument
    call.valueArguments.forEach { arg ->
        if (arg != null) {
            // Check if argument is a Field (KPojo property or nested function)
            val fieldExpr = if (arg.isKPojoProperty() || arg.isKronosFunction()) {
                // Recursively analyze to get Field
                val fields = analyzeAndBuildFields(irFunction, arg, errorReporter)
                if (fields.isNotEmpty()) fields.first() else builder.irNull()
            } else {
                builder.irNull()
            }
            
            // Create Pair<Field?, Any?>(fieldExpr, arg)
            val pairExpr = irPairOf(
                nullableFieldType,
                anyNullableType,
                fieldExpr,
                arg
            )
            
            argumentPairs.add(pairExpr)
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

/**
 * Checks if an expression is a KPojo property access
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrExpression.isKPojoProperty(): Boolean {
    return when (this) {
        is IrCall -> this.origin == IrStatementOrigin.GET_PROPERTY && 
                     this.dispatchReceiver?.type?.classFqName == null // Simple property access
        is IrPropertyReference -> true
        else -> false
    }
}
