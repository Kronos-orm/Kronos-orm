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

package com.kotlinorm.compiler.utils

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName

/**
 * Type utility functions for Kronos compiler plugin
 *
 * Provides functions for mapping Kotlin types to KColumnType enum values
 * and other type-related operations
 */

/**
 * Maps a Kotlin IrType to its corresponding KColumnType enum name
 *
 * This function converts Kotlin types to database column types used by Kronos ORM.
 * The mapping follows these rules:
 * - Primitive types map to their SQL equivalents (Int -> INT, Boolean -> BIT, etc.)
 * - Date/time types map to appropriate SQL temporal types
 * - String maps to VARCHAR
 * - ByteArray maps to BLOB
 * - Unknown types default to VARCHAR
 *
 * @param type The Kotlin IrType to map
 * @return The name of the corresponding KColumnType enum value (e.g., "INT", "VARCHAR")
 */
context(context: IrPluginContext)
fun mapTypeToKColumnType(type: IrType): String {
    val fqName = type.classFqName?.asString() ?: return "VARCHAR"

    return KColumnTypeByFqName[fqName] ?: "VARCHAR"
}

private val KColumnTypeByFqName = mapOf(
    "kotlin.Boolean" to "BIT",
    "kotlin.Byte" to "TINYINT",
    "kotlin.Short" to "SMALLINT",
    "kotlin.Int" to "INT",
    "kotlin.Long" to "BIGINT",
    "kotlin.Float" to "FLOAT",
    "kotlin.Double" to "DOUBLE",
    "java.math.BigDecimal" to "DECIMAL",
    "kotlin.Char" to "CHAR",
    "kotlin.String" to "VARCHAR",
    "java.util.Date" to "DATE",
    "java.sql.Date" to "DATE",
    "java.time.LocalDate" to "DATE",
    "kotlinx.datetime.LocalDate" to "DATE",
    "java.time.LocalTime" to "TIME",
    "kotlinx.datetime.LocalTime" to "TIME",
    "java.time.LocalDateTime" to "DATETIME",
    "kotlinx.datetime.LocalDateTime" to "DATETIME",
    "java.sql.Timestamp" to "TIMESTAMP",
    "kotlin.ByteArray" to "BLOB",
)
