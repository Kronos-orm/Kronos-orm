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

package com.kotlinorm.compiler.plugin.beans

import com.kotlinorm.compiler.helpers.invoke
import com.kotlinorm.compiler.helpers.irEnum
import com.kotlinorm.compiler.helpers.irListOf
import com.kotlinorm.compiler.helpers.referenceClass
import com.kotlinorm.compiler.plugin.utils.fieldSymbol
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.constructors

/**
 * Gets the symbol for the PrimaryKeyType enum class.
 */
context(_: IrPluginContext)
internal val primaryKeyTypeSymbol
    get() = referenceClass("com.kotlinorm.enums.PrimaryKeyType")!!

/**
 * IrField
 *
 * Constructing a field IR, which contains all the information of a field by reading the Property information
 *
 * 读取Property信息构造一个Field IR，包含一个字段的所有信息
 *
 * @property columnName The name of the column
 * @property name The name of the field
 * @property type The type of the column
 * @property primaryKey whether the field is a primary key and primary key type: none, default, identity, uuid, snowflake
 * @property dateTimeFormat The format of the date time
 * @property tableName The name of the table
 * @property cascade The cascade type
 * @property cascadeIsArrayOrCollection Whether the cascade is an array or collection
 * @property kClass The KClass of the cascade type
 * @property ignore operations should be ignored
 * @property isColumn Whether the field is a column
 * @property columnTypeLength The length of the column type
 * @property columnTypeScale The scale of the column type
 * @property columnDefaultValue The default value of the column
 * @property nullable Whether the field is nullable
 * @property serializable Whether the field is serializable
 * @property kDoc The documentation of the field
 *
 */
class FieldIR(
    internal val columnName: IrExpression,
    internal val name: String,
    internal val type: IrExpression,
    internal val primaryKey: String,
    internal val dateTimeFormat: IrExpression?,
    internal val tableName: IrExpression,
    internal val cascade: IrExpression,
    internal val cascadeIsArrayOrCollection: Boolean,
    internal val kClass: IrExpression,
    internal val superTypes: List<IrConstImpl>,
    internal val ignore: IrExpression?,
    internal val isColumn: Boolean,
    internal val columnTypeLength: IrExpression?,
    internal val columnTypeScale: IrExpression?,
    internal val columnDefaultValue: IrExpression?,
    internal val nullable: Boolean,
    internal val serializable: Boolean,
    internal val kDoc: IrExpression
){


    /**
     * Converts the current object to an IrVariable by creating a criteria using the provided propertyeters.
     *
     * @return an IrVariable representing the created criteria
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    context(context: IrPluginContext, builder: IrBuilderWithScope)
    fun build(): IrExpression {
        return fieldSymbol.constructors.first()(
            columnName,
            builder.irString(name),
            type,
            irEnum(primaryKeyTypeSymbol, primaryKey),
            dateTimeFormat ?: builder.irNull(),
            tableName,
            cascade,
            builder.irBoolean(cascadeIsArrayOrCollection),
            kClass,
            irListOf(context.irBuiltIns.stringType, superTypes),
            ignore ?: builder.irNull(),
            builder.irBoolean(isColumn),
            columnTypeLength ?: builder.irInt(0),
            columnTypeScale ?: builder.irInt(0),
            columnDefaultValue ?: builder.irNull(),
            builder.irBoolean(nullable),
            builder.irBoolean(serializable),
            kDoc
        )
    }
}