package com.kotlinorm.plugins.utils

fun getSqlType(propertyType: String): String {
    return when (propertyType) {

        "kotlin.String" -> "VARCHAR"
        "kotlin.Int" -> "INT"
        "java.math.BigDecimal" -> "NUMERIC"
        "kotlin.Long" -> "BIGINT"
        "kotlin.Short" -> "SMALLINT"
        "kotlin.Float" -> "FLOAT"
        "kotlin.Double" -> "DOUBLE"
        "kotlin.Byte" , "kotlin.Boolean" -> "TINYINT"
        "kotlin.Char" -> "CHAR"
        "java.util.Date" , "java.sql.Date", "java.time.LocalDateTime", "java.time.LocalDate", "java.time.LocalTime", "kotlinx.datetime.LocalDateTime", "kotlinx.datetime.LocalDate", "kotlinx.datetime.LocalTime" -> "DATETIME"

        else -> "UNKOWN"
    }
}