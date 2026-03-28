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
    
    return when (fqName) {
        // Boolean types
        "kotlin.Boolean" -> "BIT"
        
        // Integer types
        "kotlin.Byte" -> "TINYINT"
        "kotlin.Short" -> "SMALLINT"
        "kotlin.Int" -> "INT"
        "kotlin.Long" -> "BIGINT"
        
        // Floating point types
        "kotlin.Float" -> "FLOAT"
        "kotlin.Double" -> "DOUBLE"
        
        // Decimal types
        "java.math.BigDecimal" -> "DECIMAL"
        
        // Character types
        "kotlin.Char" -> "CHAR"
        "kotlin.String" -> "VARCHAR"
        
        // Date types
        "java.util.Date",
        "java.sql.Date",
        "java.time.LocalDate",
        "kotlinx.datetime.LocalDate" -> "DATE"
        
        // Time types
        "java.time.LocalTime",
        "kotlinx.datetime.LocalTime" -> "TIME"
        
        // DateTime types
        "java.time.LocalDateTime",
        "kotlinx.datetime.LocalDateTime" -> "DATETIME"
        
        // Timestamp types
        "java.sql.Timestamp" -> "TIMESTAMP"
        
        // Binary types
        "kotlin.ByteArray" -> "BLOB"
        
        // Special types
        "CUSTOM_CRITERIA_SQL" -> "CUSTOM_CRITERIA_SQL"
        
        // Default fallback
        else -> "VARCHAR"
    }
}

/**
 * Maps a Kotlin type name (as string) to its corresponding KColumnType enum name
 *
 * This is a convenience function that works with string type names instead of IrType.
 * Useful when you already have the fully qualified type name as a string.
 *
 * @param typeName The fully qualified Kotlin type name (e.g., "kotlin.Int")
 * @return The name of the corresponding KColumnType enum value (e.g., "INT")
 */
fun mapTypeNameToKColumnType(typeName: String): String {
    return when (typeName) {
        // Boolean types
        "kotlin.Boolean" -> "BIT"
        
        // Integer types
        "kotlin.Byte" -> "TINYINT"
        "kotlin.Short" -> "SMALLINT"
        "kotlin.Int" -> "INT"
        "kotlin.Long" -> "BIGINT"
        
        // Floating point types
        "kotlin.Float" -> "FLOAT"
        "kotlin.Double" -> "DOUBLE"
        
        // Decimal types
        "java.math.BigDecimal" -> "DECIMAL"
        
        // Character types
        "kotlin.Char" -> "CHAR"
        "kotlin.String" -> "VARCHAR"
        
        // Date types
        "java.util.Date",
        "java.sql.Date",
        "java.time.LocalDate",
        "kotlinx.datetime.LocalDate" -> "DATE"
        
        // Time types
        "java.time.LocalTime",
        "kotlinx.datetime.LocalTime" -> "TIME"
        
        // DateTime types
        "java.time.LocalDateTime",
        "kotlinx.datetime.LocalDateTime" -> "DATETIME"
        
        // Timestamp types
        "java.sql.Timestamp" -> "TIMESTAMP"
        
        // Binary types
        "kotlin.ByteArray" -> "BLOB"
        
        // Special types
        "CUSTOM_CRITERIA_SQL" -> "CUSTOM_CRITERIA_SQL"
        
        // Default fallback
        else -> "VARCHAR"
    }
}

/**
 * Checks if a type is a numeric type
 *
 * @param type The IrType to check
 * @return true if the type is numeric (integer or floating point)
 */
context(context: IrPluginContext)
fun isNumericType(type: IrType): Boolean {
    val fqName = type.classFqName?.asString() ?: return false
    
    return fqName in setOf(
        "kotlin.Byte",
        "kotlin.Short",
        "kotlin.Int",
        "kotlin.Long",
        "kotlin.Float",
        "kotlin.Double",
        "java.math.BigDecimal"
    )
}

/**
 * Checks if a type is a temporal type (date, time, or datetime)
 *
 * @param type The IrType to check
 * @return true if the type represents a date, time, or datetime
 */
context(context: IrPluginContext)
fun isTemporalType(type: IrType): Boolean {
    val fqName = type.classFqName?.asString() ?: return false
    
    return fqName in setOf(
        "java.util.Date",
        "java.sql.Date",
        "java.sql.Timestamp",
        "java.time.LocalDate",
        "java.time.LocalTime",
        "java.time.LocalDateTime",
        "kotlinx.datetime.LocalDate",
        "kotlinx.datetime.LocalTime",
        "kotlinx.datetime.LocalDateTime"
    )
}

/**
 * Checks if a type is a string type
 *
 * @param type The IrType to check
 * @return true if the type is String or Char
 */
context(context: IrPluginContext)
fun isStringType(type: IrType): Boolean {
    val fqName = type.classFqName?.asString() ?: return false
    
    return fqName in setOf("kotlin.String", "kotlin.Char")
}

/**
 * Checks if a type is a binary type
 *
 * @param type The IrType to check
 * @return true if the type is ByteArray
 */
context(context: IrPluginContext)
fun isBinaryType(type: IrType): Boolean {
    val fqName = type.classFqName?.asString() ?: return false
    
    return fqName == "kotlin.ByteArray"
}

/**
 * Gets a human-readable description of a type for error messages
 *
 * @param type The IrType to describe
 * @return A human-readable type description
 */
context(context: IrPluginContext)
fun getTypeDescription(type: IrType): String {
    val fqName = type.classFqName?.asString() ?: return "Unknown"
    
    // Simplify common Kotlin types
    return when {
        fqName.startsWith("kotlin.") -> fqName.removePrefix("kotlin.")
        fqName.startsWith("java.lang.") -> fqName.removePrefix("java.lang.")
        else -> fqName
    }
}
