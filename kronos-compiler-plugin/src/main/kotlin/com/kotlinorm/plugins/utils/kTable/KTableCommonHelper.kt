/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.plugins.utils.kTable

import com.kotlinorm.plugins.helpers.applyIrCall
import com.kotlinorm.plugins.helpers.findByFqName
import com.kotlinorm.plugins.helpers.referenceClass
import com.kotlinorm.plugins.helpers.referenceFunctions
import com.kotlinorm.plugins.utils.getSqlType
import com.kotlinorm.plugins.utils.kTableConditional.funcName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName


const val KTABLE_CLASS = "com.kotlinorm.beans.dsl.KTable"

context(IrPluginContext)
private val kTableSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.KTable")!!

context(IrPluginContext)
internal val setValueSymbol
    get() = kTableSymbol.getSimpleFunction("setValue")!!

context(IrPluginContext)
internal val addFieldSymbol
    get() = kTableSymbol.getSimpleFunction("addField")!!

context(IrPluginContext)
internal val propParamSymbol
    get() = kTableSymbol.getSimpleFunction("getValueByFieldName")

context(IrPluginContext)
internal val aliasSymbol
    get() = kTableSymbol.getSimpleFunction("setAlias")!!

context(IrPluginContext)
internal val IrCall.correspondingName
    get() = symbol.owner.correspondingPropertySymbol?.owner?.name

context(IrPluginContext)
internal val fieldK2dbSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "fieldK2db").first()

context(IrPluginContext)
internal val tableK2dbSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "tableK2db").first()

val TableAnnotationsFqName = FqName("com.kotlinorm.annotations.Table")
val TableIndexAnnotationsFqName = FqName("com.kotlinorm.annotations.TableIndex")
val PrimaryKeyAnnotationsFqName = FqName("com.kotlinorm.annotations.PrimaryKey")
val ColumnAnnotationsFqName = FqName("com.kotlinorm.annotations.Column")
val ColumnTypeAnnotationsFqName = FqName("com.kotlinorm.annotations.ColumnType")
val DateTimeFormatAnnotationsFqName = FqName("com.kotlinorm.annotations.DateTimeFormat")
val DefaultValueAnnotationsFqName = FqName("com.kotlinorm.annotations.Default")

/**
 * Returns the column name of the given IrExpression.
 *
 * @param expression the [IrExpression] to get the column name from
 * @return the `IrExpression` representing the column name
 */
context(IrBuilderWithScope, IrPluginContext)
fun getColumnName(expression: IrExpression): IrExpression {
    if (!expression.isKronosColumn()) {
        return expression
    }
    return when (expression) {
        is IrCall -> {
            val propertyName = expression.correspondingName!!.asString()
            val irProperty =
                expression.dispatchReceiver!!.type.getClass()!!.properties.first { it.name.asString() == propertyName }
            getColumnName(irProperty, propertyName)
        }

        else -> applyIrCall(fieldSymbol.constructors.first(), irString(""), irString(""))
    }
}

/**
 * Returns the column name of the given IrProperty.
 *
 * @param irProperty the [IrProperty] to get the column name from
 * @param propertyName the name of the property (default: the name of the IrProperty)
 * @return the `IrExpression` representing the column name
 */
context(IrBuilderWithScope, IrPluginContext)
fun getColumnName(
    irProperty: IrProperty,
    propertyName: String = irProperty.name.asString()
): IrExpression {
    val parent = irProperty.parent as IrClass
    val columnAnnotation =
        irProperty.annotations.findByFqName(ColumnAnnotationsFqName)
    val columnName =
        columnAnnotation?.getValueArgument(0) ?: applyIrCall(fieldK2dbSymbol, irString(propertyName))

    val columnTypeAnnotation =
        irProperty.annotations.findByFqName(ColumnTypeAnnotationsFqName)
    val propertyType = irProperty.backingField!!.type.classFqName!!.asString()
    val columnType =
        columnTypeAnnotation?.getValueArgument(0) ?: irString(getSqlType(propertyType))
    val columnTypeLength =
        columnTypeAnnotation?.getValueArgument(1) ?: irInt(0)
    val columnDefaultValue =
        irProperty.annotations.findByFqName(DefaultValueAnnotationsFqName)?.getValueArgument(0) ?: irNull()
    val tableName = getTableName(parent)

    val primaryKeyAnnotation =
        irProperty.annotations.findByFqName(PrimaryKeyAnnotationsFqName)
    val identity = primaryKeyAnnotation?.getValueArgument(0) ?: irBoolean(false)

    return applyIrCall(
        fieldSymbol.constructors.first(),
        columnName,
        irString(propertyName),
        columnType,
        irBoolean(primaryKeyAnnotation != null),
        irProperty.annotations.findByFqName(DateTimeFormatAnnotationsFqName)?.getValueArgument(0),
        when (tableName) {
            is IrCall -> applyIrCall(
                fieldK2dbSymbol,
                irString((tableName.valueArguments[0] as IrConst<*>).value.toString())
            )

            else -> tableName
        },
        columnTypeLength,
        columnDefaultValue,
        identity
    )
}

/**
 * Kronos Column Value Type
 *
 * Enum class for the kronos column value type
 */
enum class KronosColumnValueType {
    Value, ColumnName
}

/**
 * Finds a Kronos Column in the given IrExpression.
 *
 * This function checks if the given IrExpression is a Kronos Column. If it is, it returns the expression itself.
 * If the expression is an instance of IrBlock and its origin is SAFE_CALL, it returns null.
 * If the expression is not an instance of IrCall, it returns the expression itself.
 * If the extension receiver or the dispatch receiver of the expression is an instance of IrCall, it recursively calls this function with the receiver.
 * If none of the above conditions are met, it iterates over the value arguments of the expression. If it finds an argument that is an instance of IrCall, it recursively calls this function with the argument.
 * If no Kronos Column is found, it returns the expression itself.
 *
 * @receiver the `IrExpression` to find the Kronos Column in.
 * @return returns the found Kronos Column `IrExpression`, or null if no Kronos Column is found.
 */
context(IrBlockBuilder, IrPluginContext)
fun IrExpression.findKronosColumn(): IrExpression? {
    if (this is IrBlock && origin == IrStatementOrigin.SAFE_CALL) return null
    if (this !is IrCall) return this
    if (isKronosColumn()) {
        return this
    } else if (extensionReceiver is IrCall) {
        return extensionReceiver!!.findKronosColumn()
    } else if (dispatchReceiver is IrCall) {
        return dispatchReceiver!!.findKronosColumn()
    } else {
        for (arg in valueArguments) {
            if (arg is IrCall) {
                return arg.findKronosColumn()
            }
        }
        return this
    }
}

/**
 * Determines the type and value of a Kronos Column.
 *
 * This function checks if the given IrExpression is a Kronos Column. If it is, it returns a pair with the type as ColumnName and the expression itself.
 * If the function name of the expression is "value", it returns a pair with the type as Value and the expression itself.
 * Otherwise, it tries to find a Kronos Column in the expression and returns a pair with the type as ColumnName and the found Kronos Column.
 * If no Kronos Column is found, it throws an IllegalStateException.
 *
 * @receiver the `IrExpression` to check.
 * @return returns a pair with the type and value of the Kronos Column.
 * @throws IllegalStateException if no Kronos Column is found in the expression and the function name is not "value".
 */
context(IrBlockBuilder, IrPluginContext)
fun IrExpression.columnValueGetter(): Pair<KronosColumnValueType, IrExpression> {
    return if (this.isKronosColumn()) {
        KronosColumnValueType.ColumnName to this
    } else if (this.funcName() == "value") {
        KronosColumnValueType.Value to this
    } else {
        KronosColumnValueType.ColumnName to
                (findKronosColumn()
                    ?: throw IllegalStateException("`?.` is not supported in CriteriaBuilder. Unless using `.value to get the real expression value."))
    }
}

/**
 * Checks if the given IrExpression is a Kronos Column.
 *
 * This function checks if the given IrExpression is an instance of IrCallImpl and if its origin is either GET_PROPERTY or EQ.
 * If these conditions are met, it retrieves the property name from the IrExpression and finds the corresponding property in the class.
 * It then checks if any of the super types of the parent class of the property is "com.kotlinorm.beans.dsl.KPojo".
 *
 * @receiver the `IrExpression` to check. It can be null.
 * @return returns true if the IrExpression is a Kronos Column, false otherwise.
 */
context(IrBuilderWithScope, IrPluginContext)
fun IrExpression?.isKronosColumn(): Boolean {
    if (this == null) return false
    return this is IrCallImpl && this.origin in listOf(
        IrStatementOrigin.GET_PROPERTY, IrStatementOrigin.EQ
    ) && this.let {
        val propertyName = correspondingName!!.asString()
        val irProperty = dispatchReceiver!!.type.getClass()!!.properties.first { it.name.asString() == propertyName }
        val parent = irProperty.parent as IrClass
        parent.superTypes.any { it.classFqName?.asString() == "com.kotlinorm.beans.dsl.KPojo" }
    }
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
context(IrBlockBuilder, IrPluginContext)
fun getColumnOrValue(expression: IrExpression?): IrExpression? {
    if (expression == null) return null
    val (type, expr) = expression.columnValueGetter()
    return when (type) {
        KronosColumnValueType.Value -> expr
        KronosColumnValueType.ColumnName -> getColumnName(expr)
    }
}

/**
 * Returns the table name associated with the given IrExpression.
 *
 * @param expression the [IrExpression] to retrieve the table name from
 * @return the `IrExpression` representing the table name
 * @throws IllegalStateException if the expression type is unexpected
 */
context(IrBuilderWithScope, IrPluginContext)
fun getTableName(expression: IrExpression): IrExpression {
    val irClass = when (expression) {
        is IrGetValue, is IrCall -> expression.type.getClass()
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
context(IrBuilderWithScope, IrPluginContext)
fun getTableName(irClass: IrClass): IrExpression {
    val tableAnnotation = irClass.annotations.findByFqName(TableAnnotationsFqName)
    return tableAnnotation?.getValueArgument(0) ?: applyIrCall(
        tableK2dbSymbol, irString(
            irClass.name.asString()
        )
    )
}