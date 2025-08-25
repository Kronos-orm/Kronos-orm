/**
 * Copyright 2022-2025 kronos-orm
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

package com.kotlinorm.compiler.plugin.utils

import com.kotlinorm.compiler.helpers.dispatchReceiverArgument
import com.kotlinorm.compiler.helpers.findByFqName
import com.kotlinorm.compiler.helpers.invoke
import com.kotlinorm.compiler.helpers.irEnum
import com.kotlinorm.compiler.helpers.irListOf
import com.kotlinorm.compiler.helpers.irPairOf
import com.kotlinorm.compiler.helpers.nType
import com.kotlinorm.compiler.helpers.pairSymbol
import com.kotlinorm.compiler.helpers.referenceClass
import com.kotlinorm.compiler.helpers.sub
import com.kotlinorm.compiler.helpers.toKClass
import com.kotlinorm.compiler.helpers.valueArguments
import com.kotlinorm.compiler.plugin.beans.FieldIR
import com.kotlinorm.compiler.plugin.utils.kTableForCondition.columnValueGetter
import com.kotlinorm.compiler.plugin.utils.kTableForCondition.correspondingName
import com.kotlinorm.compiler.plugin.utils.kTableForCondition.funcName
import com.kotlinorm.compiler.plugin.utils.kTableForCondition.isColumn
import com.kotlinorm.compiler.plugin.utils.kTableForCondition.isKPojo
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.FqName

/**
 * Gets the symbol for the Field class.
 */
context(_: IrPluginContext)
internal val fieldSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.Field")!!

/**
 * Gets the symbol for the FunctionField class.
 */
context(_: IrPluginContext)
internal val functionSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.FunctionField")!!

/**
 * Gets the symbol for the `k2db` function in the KronosNamingStrategy interface.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val k2dbSymbol
    get() = referenceClass("com.kotlinorm.interfaces.KronosNamingStrategy")!!.getSimpleFunction("k2db")!!

/**
 * Gets the symbol for the fieldNamingStrategy property in the Kronos object.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val fieldNamingStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("fieldNamingStrategy")!!

/**
 * Gets the symbol for the tableNamingStrategy property in the Kronos object.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val tableNamingStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("tableNamingStrategy")!!

/**
 * Gets the symbol for the KColumnType enum class.
 */
context(_: IrPluginContext)
internal val kReferenceSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.KCascade")!!

val TableAnnotationsFqName = FqName("com.kotlinorm.annotations.Table")
val TableIndexAnnotationsFqName = FqName("com.kotlinorm.annotations.TableIndex")
val PrimaryKeyAnnotationsFqName = FqName("com.kotlinorm.annotations.PrimaryKey")
val ColumnAnnotationsFqName = FqName("com.kotlinorm.annotations.Column")
val ColumnTypeAnnotationsFqName = FqName("com.kotlinorm.annotations.ColumnType")
val DateTimeFormatAnnotationsFqName = FqName("com.kotlinorm.annotations.DateTimeFormat")
val CascadeAnnotationsFqName = FqName("com.kotlinorm.annotations.Cascade")
val IgnoreAnnotationsFqName = FqName("com.kotlinorm.annotations.Ignore")
val SerializeAnnotationsFqName = FqName("com.kotlinorm.annotations.Serialize")
val DefaultValueAnnotationsFqName = FqName("com.kotlinorm.annotations.Default")
val NecessaryAnnotationsFqName = FqName("com.kotlinorm.annotations.Necessary")


/**
 * Returns the column name of the given IrExpression.
 *
 * @param expression the [IrExpression] to get the column name from
 * @return the `IrExpression` representing the column name
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBuilderWithScope)
fun getColumnName(expression: IrExpression): IrExpression {
    if (!expression.isKPojo()) {
        return expression
    }
    return when (expression) {
        is IrCall -> {
            val propertyName = expression.correspondingName!!.asString()
            val irProperty =
                expression.dispatchReceiverArgument!!.type.getClass()!!.properties.first { it.name.asString() == propertyName }
            getColumnName(irProperty, propertyName)
        }

        is IrPropertyReference -> {
            val propertyName = expression.symbol.owner.name.asString()
            val irProperty = expression.symbol.owner
            getColumnName(irProperty, propertyName)
        }

        else -> fieldSymbol.constructors.first()(builder.irString(""), builder.irString(""))
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun getFunctionName(expression: IrExpression): IrExpression {
    return when (expression) {
        is IrCall -> {
            val args = mutableListOf<IrExpression>()
            expression.valueArguments.forEach {
                if (it is IrVarargImpl) {
                    args.addAll(it.elements.map { element ->
                        irPairOf(
                            fieldSymbol.nType,
                            context.irBuiltIns.anyNType,
                            (element as IrExpression).irFieldOrNull() to element
                        )
                    })
                } else {
                    args.add(
                        irPairOf(
                            fieldSymbol.nType,
                            context.irBuiltIns.anyNType,
                            it.irFieldOrNull() to it
                        )
                    )
                }
            }
            functionSymbol.constructors.first()(
                builder.irString(expression.funcName()),
                irListOf(
                    pairSymbol.owner.returnType,
                    args
                ),
            )
        }

        else -> {
            throw IllegalStateException("Unexpected expression type: $expression")
        }
    }
}

val ARRAY_OR_COLLECTION_FQ_NAMES = arrayOf(
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

/**
 * Returns the column name of the given IrProperty.
 *
 * @param irProperty the [IrProperty] to get the column name from
 * @param propertyName the name of the property (default: the name of the IrProperty)
 * @return the `IrExpression` representing the column name
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun getColumnName(
    irProperty: IrProperty, propertyName: String = irProperty.name.asString()
): IrExpression {
    val parent = irProperty.parent as IrClass
    val annotations = irProperty.annotations

    // detect annotations
    var columnAnnotation: IrConstructorCall? = null // @Column
    var columnTypeAnnotation: IrConstructorCall? = null // @ColumnType
    var cascadeAnnotation: IrConstructorCall? = null // @Cascade
    var ignoreAnnotation: IrConstructorCall? = null // @Ignore
    var defaultValueAnnotation: IrConstructorCall? = null // @DefaultValue
    var primaryKeyAnnotation: IrConstructorCall? = null // @PrimaryKey
    var dateTimeFormatAnnotation: IrConstructorCall? = null // @DateTimeFormat
    var requiredAnnotation: IrConstructorCall? = null // @Necessary
    var serializeAnnotation: IrConstructorCall? = null // @Serializable

    annotations.forEach {
        when (it.symbol.owner.returnType.getClass()!!.fqNameWhenAvailable) {
            ColumnTypeAnnotationsFqName -> columnTypeAnnotation = it
            ColumnAnnotationsFqName -> columnAnnotation = it
            CascadeAnnotationsFqName -> cascadeAnnotation = it
            IgnoreAnnotationsFqName -> ignoreAnnotation = it
            DefaultValueAnnotationsFqName -> defaultValueAnnotation = it
            PrimaryKeyAnnotationsFqName -> primaryKeyAnnotation = it
            DateTimeFormatAnnotationsFqName -> dateTimeFormatAnnotation = it
            NecessaryAnnotationsFqName -> requiredAnnotation = it
            SerializeAnnotationsFqName -> serializeAnnotation = it
        }
    }

    val columnName = columnAnnotation?.valueArguments[0] ?: k2dbSymbol(
        fieldNamingStrategySymbol(builder.irGetObject(KronosSymbol)),
        builder.irString(propertyName)
    )
    val irPropertyType = irProperty.backingField?.type ?: context.irBuiltIns.anyNType
    val propertyType = irPropertyType.classFqName!!.asString()
    val columnType =
        columnTypeAnnotation?.valueArguments[0] ?: irEnum(kColumnTypeSymbol, kotlinTypeToKColumnType(propertyType))
    val tableName = getTableName(parent)
    val propKClass = irPropertyType.getClass()
    val cascadeIsArrayOrCollection = irPropertyType.superTypes().any { it.classFqName in ARRAY_OR_COLLECTION_FQ_NAMES }
    val kClass = if (irProperty.isDelegated) {
        builder.irNull()
    } else {
        (if (cascadeIsArrayOrCollection) {
            irPropertyType.sub()!!.getClass()
        } else {
            propKClass
        }!!).symbol.toKClass()
    }
    val superTypes = propKClass?.superTypes?.mapNotNull {
        it.classFqName?.asString()?.let { builder.irString(it) }
    } ?: emptyList()

    val kCascade = if (cascadeAnnotation != null) {
        kReferenceSymbol.constructors.first()(*cascadeAnnotation.valueArguments.toTypedArray())
    } else {
        builder.irNull()
    }

    val propsOfPrimaryKey = arrayOf(
        "identity",
        "uuid",
        "snowflake",
        "custom"
    )

    val primaryKeyAnnotationIndex =
        primaryKeyAnnotation?.valueArguments?.indexOfFirst { it is IrConstImpl && it.value == true }

    val primaryKey = when {
        primaryKeyAnnotation == null -> "not"
        primaryKeyAnnotationIndex == null -> "default"
        else -> propsOfPrimaryKey.getOrNull(primaryKeyAnnotationIndex) ?: "default"
    }

    return FieldIR(
        columnName = columnName,
        name = propertyName,
        type = columnType,
        primaryKey = primaryKey,
        dateTimeFormat = dateTimeFormatAnnotation?.valueArguments[0],
        tableName = tableName,
        cascade = kCascade,
        cascadeIsArrayOrCollection = cascadeIsArrayOrCollection,
        kClass = kClass,
        superTypes = superTypes,
        ignore = ignoreAnnotation?.valueArguments[0],
        isColumn = irProperty.isColumn(irPropertyType, ignoreAnnotation),
        columnTypeLength = columnTypeAnnotation?.valueArguments[1],
        columnTypeScale = columnTypeAnnotation?.valueArguments[2],
        columnDefaultValue = defaultValueAnnotation?.valueArguments[0],
        nullable = requiredAnnotation == null && primaryKeyAnnotation == null,
        serializable = serializeAnnotation != null,
        kDoc = irProperty.getKDocString()
    ).build()
}

/**
 * Kronos Column Value Type
 *
 * Enum class for the kronos column value type
 */
enum class KronosColumnValueType {
    Value, ColumnName, Function
}

/**
 * Retrieves the column or value from the given IrExpression.
 *
 * This function checks if the given IrExpression is null. If it is, it returns null.
 * Otherwise, it determines the type and value of the IrExpression using the columnValueGetter function.
 * If the type is Value, it returns the expression itself.
 * If the type is ColumnName, it retrieves the column name from the expression using the getColumnName function.
 *
 * @param expression the IrExpression to retrieve the column or value from. It can be null.
 * @return returns the column or value from the `IrExpression`, or null if the IrExpression is null.
 */
context(_: IrPluginContext, builder: IrBlockBuilder)
fun getColumnOrValue(expression: IrExpression?): IrExpression? {
    if (expression == null) return null
    val (type, expr) = expression.columnValueGetter()
    return when (type) {
        KronosColumnValueType.Value -> expr
        KronosColumnValueType.ColumnName -> getColumnName(expr)
        KronosColumnValueType.Function -> getFunctionName(expr)
    }
}

/**
 * Returns the table name associated with the given IrExpression.
 *
 * @param expression the [IrExpression] to retrieve the table name from
 * @return the `IrExpression` representing the table name
 * @throws IllegalStateException if the expression type is unexpected
 */
context(_: IrPluginContext, builder: IrBuilderWithScope)
fun getTableName(expression: IrExpression): IrExpression {
    val irClass = when (expression) {
        is IrGetValue, is IrCall, is IrGetObjectValue -> expression.type.getClass()
        else -> throw IllegalStateException("Unexpected expression type: $expression")
    }!!
    return getTableName(irClass)
}

/**
 * Returns the table name associated with the given IrClass.
 *
 * @param irClass the [IrClass] to retrieve the table name from
 * @return the `IrExpression` representing the table name
 * @throws IllegalStateException if the table annotation is not found
 */
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun getTableName(irClass: IrClass): IrExpression {
    val tableAnnotation = irClass.annotations.findByFqName(TableAnnotationsFqName)
    return tableAnnotation?.valueArguments[0] ?: k2dbSymbol(
        tableNamingStrategySymbol(builder.irGetObject(KronosSymbol)),
        builder.irString(irClass.name.asString())
    )
}

/**
 * Returns the field expression if the given IrExpression is a KPojo, otherwise returns a null expression.
 *
 * @return 
 */
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun IrExpression?.irFieldOrNull(): IrExpression {
    return if (this != null && this.isKPojo()) getColumnName(this) else builder.irNull()
}