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

package com.kotlinorm.compiler.fir.beans

import com.kotlinorm.compiler.fir.utils.fieldSymbol
import com.kotlinorm.compiler.helpers.applyIrCall
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.constructors

/**
 * IrField
 *
 * Constructing a field IR, which contains all the information of a field by reading the Property information
 *
 * 读取Property信息构造一个Field IR，包含一个字段的所有信息
 *
 * @param columnName The name of the column
 * @param name The name of the field
 * @param type The type of the column
 * @param primaryKey Whether the field is a primary key
 * @param dateTimeFormat The format of the date time
 * @param tableName The name of the table
 * @param cascade The cascade type
 * @param cascadeIsArrayOrCollection Whether the cascade is an array or collection
 * @param cascadeTypeKClass The KClass of the cascade type
 * @param cascadeSelectIgnore Whether the cascade should be ignored in select
 * @param isColumn Whether the field is a column
 * @param columnTypeLength The length of the column type
 * @param columnDefaultValue The default value of the column
 * @param identity Whether the field is an identity
 * @param nullable Whether the field is nullable
 * @param serializable Whether the field is serializable
 * @param kDoc The documentation of the field
 *
 */
class FieldIR(
    private val columnName: IrExpression,
    private val name: String,
    private val type: IrExpression,
    private val primaryKey: Boolean,
    private val dateTimeFormat: IrExpression?,
    private val tableName: IrExpression,
    private val cascade: IrExpression,
    private val cascadeIsArrayOrCollection: Boolean,
    private val cascadeTypeKClass: IrExpression,
    private val cascadeSelectIgnore: Boolean,
    private val isColumn: Boolean,
    private val columnTypeLength: IrExpression?,
    private val columnDefaultValue: IrExpression?,
    private val identity: Boolean,
    private val nullable: Boolean,
    private val serializable: Boolean,
    private val kDoc: IrExpression
) {

    /**
     * Converts the current object to an IrVariable by creating a criteria using the provided parameters.
     *
     * @return an IrVariable representing the created criteria
     */
    context(IrBuilderWithScope, IrPluginContext)
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun build(): IrExpression {
        return applyIrCall(
            fieldSymbol.constructors.first(),
            columnName,
            irString(name),
            type,
            irBoolean(primaryKey),
            dateTimeFormat ?: irNull(),
            tableName,
            cascade,
            irBoolean(cascadeIsArrayOrCollection),
            cascadeTypeKClass,
            irBoolean(cascadeSelectIgnore),
            irBoolean(isColumn),
            columnTypeLength ?: irInt(0),
            columnDefaultValue ?: irNull(),
            irBoolean(identity),
            irBoolean(nullable),
            irBoolean(serializable),
            kDoc
        )
    }
}