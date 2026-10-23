package com.kotlinorm.orm.pagination

import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.ExperimentalTime

internal actual fun platformEncodeCursorValue(value: Any): String? = when (value) {
    is BigDecimal -> "bd:$value"
    is BigInteger -> "bi:$value"
    is UUID -> "uuid:$value"
    is java.sql.Date -> "sqlDate:${value.toLocalDate()}"
    is Time -> "sqlTime:${value.toLocalTime()}"
    is Timestamp -> "sqlTimestamp:${value.toLocalDateTime()}"
    is java.util.Date -> "utilDate:${value.toInstant()}"
    is LocalDate -> "localDate:$value"
    is LocalTime -> "localTime:$value"
    is LocalDateTime -> "localDateTime:$value"
    is Instant -> "instant:$value"
    is ZonedDateTime -> "zonedDateTime:$value"
    is OffsetDateTime -> "offsetDateTime:$value"
    else -> null
}
internal actual fun platformDecodeCursorValue(type: String, raw: String): Any = when (type) {
        "bd" -> raw.toBigDecimal()
        "bi" -> raw.toBigInteger()
        "uuid" -> UUID.fromString(raw)
        "sqlDate" -> java.sql.Date.valueOf(LocalDate.parse(raw))
        "sqlTime" -> Time.valueOf(LocalTime.parse(raw))
        "sqlTimestamp" -> Timestamp.valueOf(LocalDateTime.parse(raw))
        "utilDate" -> java.util.Date.from(Instant.parse(raw))
        "localDate" -> LocalDate.parse(raw)
        "localTime" -> LocalTime.parse(raw)
        "localDateTime" -> LocalDateTime.parse(raw)
        "instant" -> Instant.parse(raw)
        "zonedDateTime" -> ZonedDateTime.parse(raw)
        "offsetDateTime" -> OffsetDateTime.parse(raw)
    else -> error("Invalid cursor token.")
}
