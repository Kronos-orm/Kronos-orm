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
import com.kotlinorm.compiler.utils.ColumnAnnotationFqName
import com.kotlinorm.compiler.utils.ColumnTypeAnnotationFqName
import com.kotlinorm.compiler.utils.DateTimeFormatAnnotationFqName
import com.kotlinorm.compiler.utils.DefaultValueAnnotationFqName
import com.kotlinorm.compiler.utils.ErrorMessages
import com.kotlinorm.compiler.utils.IgnoreAnnotationFqName
import com.kotlinorm.compiler.utils.NecessaryAnnotationFqName
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
import com.kotlinorm.compiler.transformers.getTableNameExpr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
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
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.hasAnnotation
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
context(context: IrPluginContext, builder: IrBuilderWithScope)
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
context(context: IrPluginContext, builder: IrBuilderWithScope)
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
fun buildFieldFromProperty(irProperty: IrProperty): IrExpression {
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
    val columnNameExpr: IrExpression = if (columnAnnotation != null) {
        val customName = columnAnnotation.arguments[0]
        if (customName != null) {
            customName
        } else {
            buildNamingStrategyCall(propertyName)
        }
    } else {
        buildNamingStrategyCall(propertyName)
    }

    // Check @ColumnType annotation
    val columnTypeAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == ColumnTypeAnnotationFqName
    }
    val columnTypeExpr = columnTypeAnnotation?.arguments?.get(0) ?: buildKColumnTypeEnum(columnTypeName)
    val columnLengthExpr = columnTypeAnnotation?.arguments?.getOrNull(1)
    val columnScaleExpr = columnTypeAnnotation?.arguments?.getOrNull(2)

    // Check @PrimaryKey annotation
    val primaryKeyAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == PrimaryKeyAnnotationFqName
    }
    val primaryKeyExpr = buildPrimaryKeyType(primaryKeyAnnotation)

    // Check @DateTimeFormat annotation
    val dateTimeFormatAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == DateTimeFormatAnnotationFqName
    }
    val dateFormatExpr = dateTimeFormatAnnotation?.arguments?.get(0)

    // Table name
    val parent = irProperty.parent as? IrClass
    val tableNameExpr = if (parent != null) builder.getTableNameExpr(parent) else builder.irString("")

    // Check @Default annotation
    val defaultValueAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == DefaultValueAnnotationFqName
    }
    val defaultValueExpr = defaultValueAnnotation?.arguments?.get(0)

    // Check @Necessary annotation
    val necessaryAnnotation = irProperty.annotations.firstOrNull {
        it.symbol.owner.returnType.classFqName == NecessaryAnnotationFqName
    }
    val nullable = necessaryAnnotation == null && primaryKeyAnnotation == null

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
            // KCascade constructor params match @Cascade annotation params 1:1
            for (i in cascadeAnnotation.arguments.indices) {
                arguments[i] = cascadeAnnotation.arguments[i]
            }
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

    val ignoreExpr: IrExpression = ignoreAnnotation?.arguments?.get(0) ?: builder.irNull()

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
context(context: IrPluginContext, builder: IrBuilderWithScope)
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
            if (expression.origin == IrStatementOrigin.PLUS) {
                // Handle multiple exclusions: User::password + User::email
                val receiver = expression.extensionReceiverArgument ?: expression.dispatchReceiverArgument
                collectExcludedFieldNames(receiver, excludedFields)
                collectExcludedFieldNames(expression.getValueArgumentSafe(0), excludedFields)
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
    errorReporter: ErrorReporter
): IrExpression {
    val functionName = call.funcName()

    // Build argument list: List<Pair<Field?, Any?>>
    val argumentPairs = mutableListOf<IrExpression>()

    // Get Field? type (nullable Field)
    val nullableFieldType = fieldClassSymbol.typeWith().makeNullable()
    val anyNullableType = context.irBuiltIns.anyNType

    fun processArg(arg: IrExpression) {
        // Check if argument is a Field (KPojo property or nested function)
        val fieldExpr = if (arg.isKPojoProperty() || arg.isKronosFunction()) {
            val fields = analyzeAndBuildFields(irFunction, arg, errorReporter)
            if (fields.isNotEmpty()) fields.first() else builder.irNull()
        } else {
            builder.irNull()
        }

        // Create Pair<Field?, Any?>(fieldExpr, arg)
        argumentPairs.add(irPairOf(nullableFieldType, anyNullableType, fieldExpr, arg))
    }

    // Process each value argument, unpacking varargs
    call.valueArguments.forEach { arg ->
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
