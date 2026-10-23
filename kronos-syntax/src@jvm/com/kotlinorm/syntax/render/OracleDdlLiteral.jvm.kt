/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlTimeType
import com.kotlinorm.syntax.expr.SqlTimeZoneMode
import java.math.BigDecimal
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

internal actual fun oracleDdlLiteral(value: Any?, parameterName: String): SqlExpr = when (value) {
    null -> SqlExpr.NullLiteral
    is String -> SqlExpr.StringLiteral(value)
    is Char -> SqlExpr.StringLiteral(value.toString())
    is Boolean -> SqlExpr.NumberLiteral(if (value) "1" else "0")
    is BigDecimal -> SqlExpr.NumberLiteral(value.toPlainString())
    is Number -> SqlExpr.NumberLiteral(value.toString())
    is Enum<*> -> SqlExpr.StringLiteral(value.name)
    is UUID -> SqlExpr.StringLiteral(value.toString())
    is LocalDate -> SqlExpr.TimeLiteral(SqlTimeType.Date, value.toString())
    is java.sql.Date -> SqlExpr.TimeLiteral(SqlTimeType.Date, value.toLocalDate().toString())
    is LocalTime -> SqlExpr.TimeLiteral(SqlTimeType.Time(), value.toString())
    is Time -> SqlExpr.TimeLiteral(SqlTimeType.Time(), value.toLocalTime().toString())
    is LocalDateTime -> SqlExpr.TimeLiteral(SqlTimeType.Timestamp(), value.formatOracleTimestamp())
    is Timestamp -> SqlExpr.TimeLiteral(SqlTimeType.Timestamp(), value.toLocalDateTime().formatOracleTimestamp())
    is OffsetDateTime -> SqlExpr.TimeLiteral(
        SqlTimeType.Timestamp(SqlTimeZoneMode.WithTimeZone),
        value.format(oracleOffsetTimestampFormatter)
    )
    is ZonedDateTime -> oracleDdlLiteral(value.toOffsetDateTime(), parameterName)
    is java.time.Instant -> oracleDdlLiteral(value.atOffset(ZoneOffset.UTC), parameterName)
    is ByteArray -> SqlExpr.Function(
        name = SqlIdentifier.of("HEXTORAW"),
        args = listOf(SqlExpr.StringLiteral(value.joinToString("") { byte -> "%02x".format(byte) }))
    )
    else -> throw IllegalArgumentException(
        "Oracle CREATE TABLE AS SELECT parameter '$parameterName' has unsupported literal type ${value::class.qualifiedName}."
    )
}

private fun LocalDateTime.formatOracleTimestamp(): String =
    format(oracleLocalTimestampFormatter)

private val oracleLocalTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS")

private val oracleOffsetTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS xxx")
