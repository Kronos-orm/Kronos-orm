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

package com.kotlinorm.compiler.plugin.utils

import com.kotlinorm.compiler.helpers.referenceClass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope

internal val IrPluginContext.kColumnTypeSymbol
    get() = referenceClass("com.kotlinorm.enums.KColumnType")!!

/**
 * Get the sql type of the given property type.
 *
 * @param propertyType the kotlin type of the property
 * @return the Kronos sql type of the property
 */
fun kotlinTypeToKColumnType(propertyType: String) = when (propertyType) {
    "kotlin.Boolean", "kotlin.Byte" -> "TINYINT"
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