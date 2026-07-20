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

@JvmInline
value class Cursor(val value: String)

internal data class CursorSpec(
    val cursor: Cursor?,
    val pageSize: Int,
    val values: Map<String, Any?> = cursor?.decodeCursorValues().orEmpty()
)

internal fun Map<String, Any?>.toCursor(): Cursor {
    val body = entries.joinToString("\n") { (name, value) ->
        "${escape(name)}=${encodeValue(value)}"
    }
    return Cursor(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(body.toByteArray(Charsets.UTF_8)))
}

private fun Cursor.decodeCursorValues(): Map<String, Any?> {
    val body = String(java.util.Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
    if (body.isBlank()) return emptyMap()
    return body.lineSequence().associate { line ->
        val index = line.indexOf('=')
        require(index > 0) { "Invalid cursor token." }
        unescape(line.substring(0, index)) to decodeValue(line.substring(index + 1))
    }
}

@OptIn(ExperimentalTime::class)
private fun encodeValue(value: Any?): String = when (value) {
    null -> "n:"
    is Int -> "i:$value"
    is Long -> "l:$value"
    is Short -> "h:$value"
    is Byte -> "y:$value"
    is Float -> "f:$value"
    is Double -> "d:$value"
    is Boolean -> "b:$value"
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
    is kotlin.time.Instant -> "kotlinInstant:$value"
    else -> "s:${escape(value.toString())}"
}

@OptIn(ExperimentalTime::class)
private fun decodeValue(encoded: String): Any? {
    val type = encoded.substringBefore(':')
    val raw = encoded.substringAfter(':', "")
    return when (type) {
        "n" -> null
        "i" -> raw.toInt()
        "l" -> raw.toLong()
        "h" -> raw.toShort()
        "y" -> raw.toByte()
        "f" -> raw.toFloat()
        "d" -> raw.toDouble()
        "b" -> raw.toBooleanStrict()
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
        "kotlinInstant" -> kotlin.time.Instant.parse(raw)
        "s" -> unescape(raw)
        else -> error("Invalid cursor token.")
    }
}

private fun escape(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

private fun unescape(value: String): String = java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
