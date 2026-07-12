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
import com.kotlinorm.compiler.utils.DslCollectionFunctionNames
import com.kotlinorm.compiler.utils.ErrorMessages
import com.kotlinorm.compiler.utils.GeneratedProjectionPackageFqName
import com.kotlinorm.compiler.utils.IgnoreAnnotationFqName
import com.kotlinorm.compiler.utils.KSelectableFqName
import com.kotlinorm.compiler.utils.NonNullAnnotationFqName
import com.kotlinorm.compiler.utils.PrimaryKeyAnnotationFqName
import com.kotlinorm.compiler.utils.SelectAliasFunctionName
import com.kotlinorm.compiler.utils.SortAscendingFunctionName
import com.kotlinorm.compiler.utils.SortDescendingFunctionName
import com.kotlinorm.compiler.utils.SerializeAnnotationFqName
import com.kotlinorm.compiler.utils.WindowOrderByFunctionName
import com.kotlinorm.compiler.utils.WindowOverFunctionName
import com.kotlinorm.compiler.utils.WindowPartitionByFunctionName
import com.kotlinorm.compiler.utils.extensionReceiverArgument
import com.kotlinorm.compiler.utils.dispatchReceiverArgument
import com.kotlinorm.compiler.utils.funcName
import com.kotlinorm.compiler.utils.getValueArgumentSafe
import com.kotlinorm.compiler.utils.irListOf
import com.kotlinorm.compiler.utils.isKronosFunction
import com.kotlinorm.compiler.utils.kronosFunctionName
import com.kotlinorm.compiler.utils.mapTypeToKColumnType
import com.kotlinorm.compiler.utils.valueArguments
import com.kotlinorm.compiler.backend.transformers.getTableNameExpr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
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
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isString
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

private val ArithmeticOperatorFunctionNames = setOf("plus", "minus", "times", "div", "rem")
private val SqlOperatorFunctionNames = mapOf(
    "minus" to "sub",
    "times" to "mul",
    "div" to "div",
    "rem" to "mod",
)

/**
 * Analyzes field expression and builds Field IR expressions
 *
 * Main entry point for field analysis. Handles different types of expressions:
 * - IrCall: Property access, projection collection calls, minus operations, function calls
 * - IrPropertyReference: Property references (User::name)
 * - IrGetValue: KPojo instance (it)
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
        is IrVararg -> expression.expressionElements().flatMap { analyzeAndBuildFields(irFunction, it, errorReporter) }
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
 * - Function calls: alias() for aliases
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

        IrStatementOrigin.PLUS -> emptyList()
        
        IrStatementOrigin.MINUS -> {
            val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument
            if (receiver?.type?.isKPojoType() == true) {
                // Field exclusion: it - it.password
                analyzeMinusFields(irFunction, call, errorReporter)
            } else {
                emptyList()
            }
        }
        
        else -> {
            // Check for special function calls
            val functionName = call.symbol.owner.name.asString()
            when {
                functionName in DslCollectionFunctionNames -> analyzeFieldProjection(irFunction, call, errorReporter)
                functionName in ArithmeticOperatorFunctionNames -> emptyList()
                functionName == WindowOverFunctionName -> emptyList()
                functionName == "unaryPlus" -> {
                    errorReporter.reportError(
                        call,
                        ErrorMessages.UNSUPPORTED_FIELD_OPERATOR,
                        ErrorMessages.UNSUPPORTED_FIELD_OPERATOR_FIX
                    )
                    emptyList()
                }

                functionName == "alias" -> {
                    // Alias: it.name.alias("alias")
                    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument
                    val aliasValue = (call.getValueArgumentSafe(0) as? IrConst)?.value as? String ?: return emptyList()
                    val fields = analyzeAndBuildFields(irFunction, receiver ?: call, errorReporter)
                    if (fields.isEmpty()) emptyList() else [buildFieldWithAlias(fields.first(), aliasValue)]
                }

                else -> emptyList()
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
        origin == IrStatementOrigin.PLUS || rawName == "plus" -> {
            val hasStringOperand = operatorOperands().any {
                it.type.isString() || it.type.classFqName?.asString() == "kotlin.CharSequence"
            }
            if (hasStringOperand) "concat" else "add"
        }
        origin == IrStatementOrigin.MINUS -> "sub"
        else -> null
    } ?: SqlOperatorFunctionNames[rawName]
}

internal sealed class SelectProjectionIr {
    data class FieldProjection(val field: IrExpression) : SelectProjectionIr()
    data class FunctionProjection(val function: IrExpression) : SelectProjectionIr()
    data class ScalarSubqueryProjection(val query: IrExpression, val alias: String) : SelectProjectionIr()
    data class RawSqlProjection(val sql: String, val alias: String? = null) : SelectProjectionIr()
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
internal fun analyzeAndBuildSelectProjections(
    irFunction: IrFunction,
    expression: IrExpression,
    errorReporter: ErrorReporter
): List<SelectProjectionIr> {
    return when {
        expression is IrCall && expression.isProjectionCollectionCall() ->
            expression.flattenValueArguments()
                .flatMap { analyzeAndBuildSelectProjections(irFunction, it, errorReporter) }

        expression is IrCall && expression.symbol.owner.name.asString() == "alias" -> {
            val receiver = expression.extensionReceiverArgument ?: expression.dispatchReceiverArgument
            val valueReceiver = receiver?.safeSelectableValueExpression()
            val alias = (expression.getValueArgumentSafe(0) as? IrConst)?.value as? String
            if (valueReceiver is IrConst && valueReceiver.value is String && alias != null) {
                listOf(SelectProjectionIr.RawSqlProjection(valueReceiver.value as String, alias))
            } else if (valueReceiver != null && valueReceiver.type.isKSelectableType() && alias != null) {
                listOf(SelectProjectionIr.ScalarSubqueryProjection(valueReceiver, alias))
            } else if (valueReceiver is IrCall && valueReceiver.isFunctionProjectionCall()) {
                val function = buildKronosFunctionExpr(irFunction, valueReceiver, errorReporter)
                if (alias != null) {
                    listOf(SelectProjectionIr.FunctionProjection(buildFunctionAlias(function, alias)))
                } else {
                    listOf(SelectProjectionIr.FunctionProjection(function))
                }
            } else {
                analyzeAndBuildFields(irFunction, expression, errorReporter)
                    .map { SelectProjectionIr.FieldProjection(it) }
            }
        }

        expression is IrCall && expression.isFunctionProjectionCall() ->
            listOf(SelectProjectionIr.FunctionProjection(buildKronosFunctionExpr(irFunction, expression, errorReporter)))

        expression is IrConst && expression.value is String ->
            listOf(SelectProjectionIr.RawSqlProjection(expression.value as String))

        else -> analyzeAndBuildFields(irFunction, expression, errorReporter)
            .map { SelectProjectionIr.FieldProjection(it) }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
internal fun analyzeAndBuildInsertSelectValues(
    irFunction: IrFunction,
    expression: IrExpression,
    errorReporter: ErrorReporter
): List<IrExpression> {
    return when {
        expression is IrCall && expression.isProjectionCollectionCall() ->
            expression.flattenValueArguments()
                .flatMap { analyzeAndBuildInsertSelectValues(irFunction, it, errorReporter) }

        expression is IrVararg -> expression.expressionElements()
            .flatMap { analyzeAndBuildInsertSelectValues(irFunction, it, errorReporter) }

        else -> listOf(buildInsertSelectValue(irFunction, expression, errorReporter))
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildInsertSelectValue(
    irFunction: IrFunction,
    expression: IrExpression,
    errorReporter: ErrorReporter
): IrExpression {
    val valueExpression = expression.safeSelectableValueExpression()
    return when (valueExpression) {
        is IrPropertyReference -> buildFieldFromPropertyRef(valueExpression, errorReporter)
        is IrCall -> when {
            valueExpression.origin == IrStatementOrigin.GET_PROPERTY ->
                buildFieldFromPropertyAccess(valueExpression, errorReporter)

            valueExpression.operatorFunctionName() != null ->
                buildKronosFunctionExpr(irFunction, valueExpression, errorReporter)

            valueExpression.symbol.owner.name.asString() == WindowOverFunctionName ->
                buildKronosFunctionExpr(irFunction, valueExpression, errorReporter)

            valueExpression.isKronosFunction() ->
                buildKronosFunctionExpr(irFunction, valueExpression, errorReporter)

            valueExpression.symbol.owner.name.asString() == SelectAliasFunctionName -> {
                val receiver = valueExpression.extensionReceiverArgument ?: valueExpression.dispatchReceiverArgument
                buildInsertSelectValue(irFunction, receiver!!, errorReporter)
            }

            else -> valueExpression
        }

        else -> valueExpression
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrCall.isProjectionCollectionCall(): Boolean {
    return symbol.owner.name.asString() in DslCollectionFunctionNames
}

context(context: IrPluginContext)
internal fun IrType.isKSelectableType(): Boolean {
    return classFqName == KSelectableFqName || superTypes().any { it.classFqName == KSelectableFqName }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
internal fun IrExpression.safeSelectableValueExpression(): IrExpression {
    return when {
        this is IrTypeOperatorCall && argument.type.isKSelectableType() -> argument
        else -> this
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
internal fun IrExpression.isSelectableTypeHintValue(): Boolean {
    return safeSelectableValueExpression() !== this
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun analyzeFieldProjection(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter
): List<IrExpression> {
    return call.flattenValueArguments().flatMap { analyzeAndBuildFields(irFunction, it, errorReporter) }
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
    val propertyName = propertyGetter.correspondingPropertySymbol!!.owner.name.asString()
    
    // Get the receiver type to find the property
    val irClass = call.dispatchReceiver!!.type.classOrNull!!.owner

    if (irClass.isGeneratedProjectionClass()) {
        return buildSimpleField(propertyName, mapTypeToKColumnType(propertyGetter.returnType))
    }
    
    // Find the property in the class
    val irProperty = irClass.properties.firstOrNull { it.name.asString() == propertyName }
        ?: error(
            ErrorMessages.cannotFindPropertyInClass(
                propertyName,
                irClass.name.asString(),
                irClass.properties.joinToString { it.name.asString() }
            )
        )
    
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
    val columnAnnotation = irProperty.annotation(ColumnAnnotationFqName)

    // columnName: @Column("custom") or Kronos.fieldNamingStrategy.k2db(propertyName)
    val columnNameExpr: IrExpression = columnAnnotation.stringArg(0)?.let { builder.irString(it) }
        ?: buildNamingStrategyCall(propertyName)

    // Check @ColumnType annotation
    val columnTypeAnnotation = irProperty.annotation(ColumnTypeAnnotationFqName)
    val columnTypeExpr = cloneEnumValue(columnTypeAnnotation.arg(0)) {
        buildKColumnTypeEnum(columnTypeName)
    }
    val columnLengthExpr = columnTypeAnnotation.intArg(1)?.let { builder.irInt(it) }
    val columnScaleExpr = columnTypeAnnotation.intArg(2)?.let { builder.irInt(it) }

    // Check @PrimaryKey annotation
    val primaryKeyAnnotation = irProperty.annotation(PrimaryKeyAnnotationFqName)
    val primaryKeyExpr = buildPrimaryKeyType(primaryKeyAnnotation)

    // Check @DateTimeFormat annotation
    val dateTimeFormatAnnotation = irProperty.annotation(DateTimeFormatAnnotationFqName)
    val dateFormatExpr = dateTimeFormatAnnotation.stringArg(0)?.let { builder.irString(it) }

    // Table name
    val parent = irProperty.parent as? IrClass
    val tableNameOwner = metadataClass ?: parent
    val tableNameExpr = builder.getTableNameExpr(
        tableNameOwner ?: error(ErrorMessages.cannotFindClassForProperty(propertyName, null))
    )

    // Check @Default annotation
    val defaultValueAnnotation = irProperty.annotation(DefaultValueAnnotationFqName)
    val defaultValueExpr = defaultValueAnnotation.stringArg(0)?.let { builder.irString(it) }

    // Check @NonNull annotation
    val nonNullAnnotation = irProperty.annotation(NonNullAnnotationFqName)
    val nullable = nonNullAnnotation == null && primaryKeyAnnotation == null

    // Check @Serialize annotation
    val serializeAnnotation = irProperty.annotation(SerializeAnnotationFqName)
    val serializable = serializeAnnotation != null

    // Check @Cascade annotation
    val cascadeAnnotation = irProperty.annotation(CascadeAnnotationFqName)

    // Check @Ignore annotation
    val ignoreAnnotation = irProperty.annotation(IgnoreAnnotationFqName)

    // Cascade-related fields keep the same column-name extraction rules as generated KPojo metadata.
    val cascadeExpr: IrExpression = if (cascadeAnnotation != null) {
        builder.irCall(kCascadeConstructorSymbol).apply {
            arguments[0] = cloneStringVararg(cascadeAnnotation.arg(0))
            arguments[1] = cloneStringVararg(cascadeAnnotation.arg(1))
            arguments[2] = cloneEnumValue(cascadeAnnotation.arg(2)) { null }
            arguments[3] = cloneStringVararg(cascadeAnnotation.arg(3))
            arguments[4] = cloneEnumVararg(cascadeAnnotation.arg(4))
        }
    } else {
        builder.irNull()
    }

    val kTypeExpr: IrExpression = if (irProperty.isDelegated) {
        builder.irNull()
    } else {
        builder.irCall(typeOfFunctionSymbol).apply {
            typeArguments[0] = propertyType
        }
    }

    val ignoreExpr: IrExpression = cloneEnumVararg(ignoreAnnotation.arg(0)) ?: builder.irNull()

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
        arguments[7] = kTypeExpr                               // kType
        arguments[8] = ignoreExpr                              // ignore
        arguments[9] = builder.irBoolean(isColumn)             // isColumn
        if (columnLengthExpr != null) arguments[10] = columnLengthExpr  // length
        if (columnScaleExpr != null) arguments[11] = columnScaleExpr    // scale
        arguments[12] = defaultValueExpr                       // defaultValue
        arguments[13] = builder.irBoolean(nullable)            // nullable
        arguments[14] = builder.irBoolean(serializable)        // serializable
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrProperty.annotation(fqName: FqName): IrConstructorCall? =
    annotations.firstOrNull { it.symbol.owner.returnType.classFqName == fqName }

private fun IrConstructorCall?.arg(index: Int): IrExpression? =
    this?.arguments?.getOrNull(index)

private fun IrConstructorCall?.stringArg(index: Int): String? =
    arg(index)?.stringValueOrNull()

private fun IrConstructorCall?.intArg(index: Int): Int? =
    arg(index)?.intValueOrNull()

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
        .first { it.name.asString() == columnTypeName }
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
            else -> propsOfPrimaryKey[enabledIndex].uppercase()
        }
    }

    val entry = entries.first { it.name.asString() == entryName }
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
    
    val columnTypeValue = IrGetEnumValueImpl(
        builder.startOffset,
        builder.endOffset,
        kColumnTypeEnumSymbol.defaultType,
        enumEntry!!.symbol
    )
    
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
    val (source, excludedFields) = collectSourceMinusFields(call)
        ?: return emptyList()
    val receiverType = source.type
    val irClass = receiverType.classOrNull!!.owner

    // Get all column properties except excluded ones
    val fields = mutableListOf<IrExpression>()
    val columnProperties = irClass.properties.filter { it.isColumnType() }.toList()
    
    columnProperties.forEach { prop ->
        if (prop.name.asString() !in excludedFields) {
            fields.add(buildFieldFromProperty(prop))
        }
    }
    
    return fields
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun collectSourceMinusFields(call: IrCall): Pair<IrExpression, MutableSet<String>>? {
    if (call.origin != IrStatementOrigin.MINUS) return null
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val (source, excludedFields) = if (receiver is IrCall && receiver.origin == IrStatementOrigin.MINUS) {
        val (innerSource, innerExcludedFields) = collectSourceMinusFields(receiver) ?: return null
        innerSource to innerExcludedFields.toCollection(linkedSetOf())
    } else {
        if (!receiver.type.isKPojoType()) return null
        receiver to linkedSetOf<String>()
    }
    collectExcludedFieldNames(call.getValueArgumentSafe(0), excludedFields)
    return source to excludedFields
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
            if (functionName in DslCollectionFunctionNames) {
                expression.flattenValueArguments().forEach { collectExcludedFieldNames(it, excludedFields) }
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

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun buildKronosFunctionExpr(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter,
    functionName: String = call.operatorFunctionName() ?: call.kronosFunctionName()
): IrExpression {
    val windowReceiver = call.windowReceiverCall()
    val sourceCall = windowReceiver ?: call
    val effectiveFunctionName = windowReceiver?.kronosFunctionName() ?: functionName
    val window = if (windowReceiver != null) {
        buildSqlWindow(irFunction, call, errorReporter)
    } else {
        builder.irNull()
    }
    val args = mutableListOf<IrExpression>()

    fun processArg(arg: IrExpression) {
        val expression = when {
            arg is IrCall && arg.isFunctionProjectionCall() ->
                buildKronosFunctionExpr(irFunction, arg, errorReporter)
            arg.isKronosFieldExpression() ->
                analyzeAndBuildFields(irFunction, arg, errorReporter).firstOrNull() ?: arg.deepCopyWithSymbols()
            else -> arg.deepCopyWithSymbols()
        }
        args += expression
    }

    (sourceCall.operatorOperands().takeIf { sourceCall.operatorFunctionName() != null }
        ?: sourceCall.flattenValueArguments())
        .forEach(::processArg)

    return builder.irCall(kronosFunctionCallWindowArgsSymbol).apply {
        dispatchReceiver = builder.irGetObject(kronosFunctionExpressionsSymbol)
        arguments[1] = builder.irString(effectiveFunctionName)
        arguments[2] = irListOf(context.irBuiltIns.anyNType, args)
        arguments[3] = window
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildFunctionAlias(function: IrExpression, alias: String): IrExpression =
    builder.irCall(kronosFunctionAliasSymbol).apply {
        dispatchReceiver = function
        arguments[1] = builder.irString(alias)
    }

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
private fun IrCall.isFunctionProjectionCall(): Boolean =
    (operatorFunctionName() != null && (extensionReceiverArgument ?: dispatchReceiverArgument)?.type?.isKPojoType() != true) ||
        symbol.owner.name.asString() == WindowOverFunctionName ||
        isKronosFunction()

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrCall.windowReceiverCall(): IrCall? {
    if (funcName() != WindowOverFunctionName) return null
    return (extensionReceiverArgument ?: dispatchReceiverArgument) as? IrCall
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildSqlWindow(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter
): IrExpression? {
    val lambda = call.valueArguments.firstNotNullOfOrNull { it as? IrFunctionExpression } ?: return null
    val body = lambda.function.body as? IrBlockBody ?: return null
    val parts = WindowParts()

    body.statements.forEach { statement ->
        collectWindowStatement(irFunction, statement, errorReporter, parts)
    }

    return builder.irCall(sqlWindowConstructorSymbol).apply {
        arguments[0] = irListOf(expressionClassSymbol.defaultType, parts.partitionBy)
        arguments[1] = irListOf(sqlOrderingItemClassSymbol.defaultType, parts.orderBy)
        arguments[2] = builder.irNull()
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun collectWindowStatement(
    irFunction: IrFunction,
    statement: IrStatement,
    errorReporter: ErrorReporter,
    parts: WindowParts
) {
    val expression = when (statement) {
        is IrReturn -> statement.value
        is IrExpression -> statement
        else -> return
    }
    val call = expression as? IrCall ?: return

    when (call.funcName()) {
        WindowPartitionByFunctionName -> {
            parts.partitionBy += call.flattenValueArguments().mapNotNull {
                buildWindowExpression(irFunction, it, errorReporter)
            }
        }
        WindowOrderByFunctionName -> {
            parts.orderBy += call.flattenValueArguments().mapNotNull {
                buildWindowOrderByItem(irFunction, it, errorReporter)
            }
        }
    }
}

private fun IrCall.flattenValueArguments(): List<IrExpression> {
    return valueArguments.filterIsInstance<IrExpression>().flatMap { argument ->
        if (argument is IrVararg) argument.expressionElements() else listOf(argument)
    }
}

private fun IrVararg.expressionElements(): List<IrExpression> =
    elements.filterIsInstance<IrExpression>()

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildWindowOrderByItem(
    irFunction: IrFunction,
    expression: IrExpression,
    errorReporter: ErrorReporter
): IrExpression? {
    val call = expression as? IrCall
    val orderingName = when (call?.funcName()) {
        SortDescendingFunctionName.asString() -> "Desc"
        SortAscendingFunctionName.asString() -> "Asc"
        else -> "Asc"
    }
    val value = if (
        call?.funcName() == SortDescendingFunctionName.asString() ||
        call?.funcName() == SortAscendingFunctionName.asString()
    ) {
        call.extensionReceiverArgument ?: call.dispatchReceiverArgument
    } else {
        expression
    } ?: return null

    val orderExpression = buildWindowExpression(irFunction, value, errorReporter)
    return builder.irCall(sqlOrderingItemConstructorSymbol).apply {
        arguments[0] = orderExpression
        arguments[1] = buildSqlOrderingEnum(orderingName)
        arguments[2] = builder.irNull()
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildWindowExpression(
    irFunction: IrFunction,
    expression: IrExpression,
    errorReporter: ErrorReporter
): IrExpression {
    val call = expression as? IrCall ?: error("Unsupported Kronos window expression: ${expression::class.simpleName}")
    return if (call.origin == IrStatementOrigin.GET_PROPERTY) {
        buildSqlExprFromField(buildFieldFromPropertyAccess(call, errorReporter))
    } else {
        buildSqlExprFromKronosFunction(buildKronosFunctionExpr(irFunction, call, errorReporter))
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildSqlExprFromField(fieldExpression: IrExpression): IrExpression {
    val columnName = (fieldExpression as IrConstructorCall).arguments[0]!!
    return builder.irCall(sqlExprColumnConstructorSymbol).apply {
        arguments[0] = builder.irNull()
        arguments[1] = columnName
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildSqlExprFromKronosFunction(functionExpression: IrExpression): IrExpression {
    return builder.irCall(kronosFunctionExprGetterSymbol).apply {
        dispatchReceiver = functionExpression
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildSqlOrderingEnum(name: String): IrExpression {
    val entry = sqlOrderingClassSymbol.owner.declarations
        .filterIsInstance<IrEnumEntry>()
        .first { it.name.asString() == name }
    return IrGetEnumValueImpl(
        builder.startOffset,
        builder.endOffset,
        sqlOrderingClassSymbol.defaultType,
        entry.symbol
    )
}

private data class WindowParts(
    val partitionBy: MutableList<IrExpression> = mutableListOf(),
    val orderBy: MutableList<IrExpression> = mutableListOf()
)

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
    return (this as? IrConst)?.value as? Int
}

/**
 * Checks if an expression is a KPojo property access
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrExpression.isKPojoProperty(): Boolean {
    return this is IrPropertyReference || (this is IrCall && origin == IrStatementOrigin.GET_PROPERTY)
}

/**
 * Checks whether an expression should be lowered as a Kronos field-like function argument.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrExpression.isKronosFieldExpression(): Boolean {
    if (isKPojoProperty()) return true
    val call = this as? IrCall ?: return false
    return call.isKronosFunction() || call.operatorFunctionName() != null
}
