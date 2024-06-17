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

import com.kotlinorm.plugins.helpers.*
import com.kotlinorm.plugins.utils.kTableConditional.funcName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
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

context(IrPluginContext)
internal val kReferenceSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.KReference")!!

val TableAnnotationsFqName = FqName("com.kotlinorm.annotations.Table")
val ColumnAnnotationsFqName = FqName("com.kotlinorm.annotations.Column")
val DateTimeFormatAnnotationsFqName = FqName("com.kotlinorm.annotations.DateTimeFormat")
val ReferenceAnnotationsFqName = FqName("com.kotlinorm.annotations.Reference")
val UseSerializeResolverAnnotationsFqName = FqName("com.kotlinorm.annotations.UseSerializeResolver")

/**
 * Returns the column name of the given IrExpression.
 *
 * @param expression the [IrExpression] to get the column name from
 * @return the IrExpression representing the column name
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
 * @return the IrExpression representing the column name
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
    val tableName = getTableName(parent)
    val referenceAnnotation = irProperty.annotations.findByFqName(ReferenceAnnotationsFqName)
    var referenceTypeKClassName = irProperty.backingField!!.type.getClass()!!.classId!!.asFqNameString()
    if (referenceTypeKClassName.startsWith("kotlin.collections")) {
        referenceTypeKClassName = irProperty.backingField!!.type.subType()!!.getClass()!!.classId!!.asFqNameString()
    }
    val reference = if (referenceAnnotation != null) {
        applyIrCall(
            kReferenceSymbol.constructors.first(),
            *referenceAnnotation.valueArguments.toTypedArray()
        )
    } else {
        irNull()
    }

    return applyIrCall(
        fieldSymbol.constructors.first(),
        columnName,
        irString(propertyName),
        irString(""),
        irBoolean(false),
        irProperty.annotations.findByFqName(DateTimeFormatAnnotationsFqName)?.getValueArgument(0),
        when (tableName) {
            is IrCall -> applyIrCall(
                fieldK2dbSymbol,
                irString((tableName.valueArguments[0] as IrConst<*>).value.toString())
            )

            else -> irString((tableName as IrConst<*>).value.toString())
        },
        reference,
        irString(referenceTypeKClassName),
        irBoolean(
            /**
             * for custom serialization, the property is a column if it has a `@UseSerializeResolver` annotation
             * for properties that are not columns, we need to check if :
             * 1. the type is a KPojo
             * 2. has a KPojo in its super types
             * 3. is a Collection of KPojo
             * 4. has Annotation `@Reference`
             */
            irProperty.hasAnnotation(UseSerializeResolverAnnotationsFqName) ||
                    (!irProperty.hasAnnotation(ReferenceAnnotationsFqName) &&
                            !irProperty.backingField!!.type.isKronosColumn() &&
                            irProperty.backingField!!.type.subType()?.isKronosColumn() != true)
        )
    )
}

enum class KronosColumnValueType {
    Value, ColumnName
}

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

context(IrBuilderWithScope, IrPluginContext)
fun IrExpression?.isKronosColumn(): Boolean {
    if (this == null) return false
    return this is IrCallImpl && this.origin in listOf(
        IrStatementOrigin.GET_PROPERTY, IrStatementOrigin.EQ
    ) && this.let {
        val propertyName = correspondingName!!.asString()
        (dispatchReceiver!!.type.getClass()!!.properties.first { it.name.asString() == propertyName }.parent as IrClass).isKronosColumn()
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun IrClass.isKronosColumn(): Boolean {
    return superTypes.any { it.classFqName?.asString() == "com.kotlinorm.beans.dsl.KPojo" }
}

context(IrBuilderWithScope, IrPluginContext)
fun IrType.isKronosColumn(): Boolean {
    return superTypes().any { it.classFqName?.asString() == "com.kotlinorm.beans.dsl.KPojo" }
}

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
 * @return the IrExpression representing the table name
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
 * @return the IrExpression representing the table name
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