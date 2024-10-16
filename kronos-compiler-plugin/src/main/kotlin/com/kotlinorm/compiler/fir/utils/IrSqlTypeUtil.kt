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

package com.kotlinorm.compiler.fir.utils

import com.kotlinorm.compiler.helpers.referenceClass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType


context(IrBuilderWithScope, IrPluginContext)
internal val kColumnTypeSymbol
    get() = referenceClass("com.kotlinorm.enums.KColumnType")!!

/**
 * Retrieves the condition type enum value based on the given type string.
 *
 * @param type The type string to retrieve the condition type for.
 * @return The IrExpression representing the condition type enum value.
 */
context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun getKColumnType(type: String): IrExpression {
    val columnType = kotlinTypeToKColumnType(type)
    val enumEntries = kColumnTypeSymbol.owner.declarations.filterIsInstance<IrEnumEntry>()
    val enumEntry = enumEntries.find { it.name.asString() == columnType }!!
    return IrGetEnumValueImpl(startOffset, endOffset, kColumnTypeSymbol.defaultType, enumEntry.symbol)
}

/**
 * Get the sql type of the given property type.
 *
 * @param propertyType the kotlin type of the property
 * @return the Kronos sql type of the property
 */
fun kotlinTypeToKColumnType(propertyType: String) = when (propertyType) {
    "kotlin.Boolean" -> "TINYINT"
    "kotlin.Byte" -> "TINYINT"
    "kotlin.Short" -> "SMALLINT"
    "kotlin.Int" -> "INT"
    "kotlin.Long" -> "BIGINT"
    "kotlin.Float" -> "FLOAT"
    "kotlin.Double" -> "DOUBLE"
    "java.math.BigDecimal" -> "NUMERIC"
    "kotlin.Char" -> "CHAR"
    "kotlin.String" -> "VARCHAR"
    "java.util.Date", "java.sql.Date", "java.time.LocalDate", "kotlinx.datetime.LocalDate" -> "DATE"
    "java.time.LocalTime", "kotlinx.datetime.LocalTime" -> "TIME"
    "java.time.LocalDateTime", "kotlinx.datetime.LocalDateTime" -> "DATETIME"
    "kotlin.ByteArray" -> "BINARY"
    "CUSTOM_CRITERIA_SQL" -> "CUSTOM_CRITERIA_SQL"
    else -> "VARCHAR"
}