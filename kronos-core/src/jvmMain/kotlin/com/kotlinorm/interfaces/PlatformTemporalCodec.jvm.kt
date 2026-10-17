package com.kotlinorm.interfaces

import java.time.ZoneId
import kotlinx.datetime.Instant as KotlinxInstant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import com.kotlinorm.utils.withoutTopLevelNullability

internal actual fun encodePlatformTemporal(
    value: Any,
    context: StaticValueCodecContext
): KronosPhysicalValue? = when (value) {
    is KotlinxInstant,
    is LocalDate,
    is LocalDateTime,
    is LocalTime -> KronosPhysicalValue.Text(value.toString())
    else -> null
}

internal actual fun decodePlatformTemporal(
    value: KronosPhysicalValue,
    context: StaticValueCodecContext
): Any? {
    val key = context.targetType.withoutTopLevelNullability().key
    if (!key.startsWith("kotlinx.datetime.") && key != "kotlin.time.Instant") return null
    val zone = context.timeZone?.let(ZoneId::of) ?: ZoneId.systemDefault()
    val timeZone = TimeZone.of(zone.id)
    val text = (value as? KronosPhysicalValue.Text)?.value
    return when (key) {
        "kotlinx.datetime.Instant" -> when (value) {
            is KronosPhysicalValue.Integer -> KotlinxInstant.fromEpochMilliseconds(value.value)
            is KronosPhysicalValue.Text -> parseInstant(value.value, timeZone)
            else -> null
        }
        "kotlinx.datetime.LocalDate" -> text?.let { parseLocalDate(it, timeZone) }
        "kotlinx.datetime.LocalDateTime" -> text?.let { parseLocalDateTime(it, timeZone) }
        "kotlinx.datetime.LocalTime" -> text?.let { parseLocalTime(it, timeZone) }
        "kotlin.time.Instant" -> when (value) {
            is KronosPhysicalValue.Integer -> Instant.fromEpochMilliseconds(value.value)
            is KronosPhysicalValue.Text -> parseKotlinInstant(value.value, timeZone)
            else -> null
        }
        else -> null
    }
}

private fun parseInstant(text: String, zone: TimeZone): KotlinxInstant = try {
    KotlinxInstant.parse(text)
} catch (_: Throwable) {
    LocalDateTime.parse(text).toInstant(zone)
}

private fun parseLocalDate(text: String, zone: TimeZone): LocalDate = try {
    LocalDate.parse(text)
} catch (_: Throwable) {
    parseInstant(text, zone).toLocalDateTime(zone).date
}

private fun parseLocalDateTime(text: String, zone: TimeZone): LocalDateTime = try {
    LocalDateTime.parse(text)
} catch (_: Throwable) {
    parseInstant(text, zone).toLocalDateTime(zone)
}

private fun parseLocalTime(text: String, zone: TimeZone): LocalTime = try {
    LocalTime.parse(text)
} catch (_: Throwable) {
    parseInstant(text, zone).toLocalDateTime(zone).time
}

private fun parseKotlinInstant(text: String, zone: TimeZone): Instant = try {
    Instant.parse(text)
} catch (_: Throwable) {
    val value = LocalDateTime.parse(text).toInstant(zone)
    Instant.fromEpochSeconds(value.epochSeconds, value.nanosecondsOfSecond)
}
