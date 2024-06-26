package com.kotlinorm.plugins.utils

/**
 * Get the sql type of the given property type.
 *
 * @param propertyType the kotlin type of the property
 * @return the Kronos sql type of the property
 */
fun getSqlType(propertyType: String) = when (propertyType) {
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
    else -> "VARCHAR"
}