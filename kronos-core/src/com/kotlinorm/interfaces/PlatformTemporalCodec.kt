@file:OptIn(kotlinx.datetime.format.FormatStringsInDatetimeFormats::class)

package com.kotlinorm.interfaces

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import com.kotlinorm.Kronos
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.format
import kotlinx.datetime.offsetAt
import kotlinx.datetime.atTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import com.kotlinorm.utils.withoutTopLevelNullability

internal fun encodePlatformTemporal(
    value: Any,
    context: StaticValueCodecContext
): KronosPhysicalValue? {
    if (value !is Instant && value !is LocalDate && value !is LocalDateTime && value !is LocalTime) return null
    val pattern = context.explicitPattern() ?: return KronosPhysicalValue.Text(value.toString())
    val format = portableTemporalFormat(pattern)
        ?: return KronosPhysicalValue.Text(formatPlatformSpecificTemporalPattern(value, context, pattern))
    val text = format.format {
        when (value) {
            is Instant -> {
                setDateTime(value.toLocalDateTime(context.zone()))
                setOffset(context.zone().offsetAt(value))
                timeZoneId = context.zone().id
            }
            is LocalDateTime -> setDateTime(value)
            is LocalDate -> setDateTime(value.atTime(0, 0))
            is LocalTime -> setDateTime(LocalDate.fromEpochDays(0).atTime(value))
        }
    }
    return KronosPhysicalValue.Text(text)
}

internal fun decodePlatformTemporal(
    value: KronosPhysicalValue,
    context: StaticValueCodecContext
): Any? {
    val key = context.targetType.withoutTopLevelNullability().key
    if (!key.startsWith("kotlinx.datetime.") && key != "kotlin.time.Instant") return null
    if (value is KronosPhysicalValue.Integer && key in setOf("kotlinx.datetime.Instant", "kotlin.time.Instant")) {
        return Instant.fromEpochMilliseconds(value.value)
    }
    val text = (value as? KronosPhysicalValue.Text)?.value ?: return null
    val pattern = context.pattern()
    val format = portableTemporalFormat(pattern)
        ?: return parsePlatformSpecificTemporalPattern(text, context, pattern)
    return when (key) {
        "kotlinx.datetime.Instant", "kotlin.time.Instant" -> configuredOrIso(text, format,
            { it.instant(context.zone()) }, { isoInstant(text, context.zone()) })
        "kotlinx.datetime.LocalDate" -> configuredOrIso(text, format,
            { it.toLocalDate() }, { isoLocalDate(text, context.zone()) })
        "kotlinx.datetime.LocalDateTime" -> configuredOrIso(text, format,
            { if (it.offsetHours != null || it.timeZoneId != null) it.instant(context.zone()).toLocalDateTime(context.zone()) else it.toLocalDateTime() },
            { isoLocalDateTime(text, context.zone()) })
        "kotlinx.datetime.LocalTime" -> configuredOrIso(text, format,
            { it.toLocalTime() }, { isoLocalTime(text, context.zone()) })
        else -> null
    }
}

private fun StaticValueCodecContext.zone(): TimeZone = timeZone?.let(TimeZone::of) ?: TimeZone.currentSystemDefault()
private fun StaticValueCodecContext.explicitPattern(): String? = dateFormat ?: field?.dateFormat
private fun StaticValueCodecContext.pattern(): String = explicitPattern() ?: Kronos.defaultDateFormat

/** Builder rejection is a capability result: unsupported directives may throw either documented exception type. */
private fun portableTemporalFormat(pattern: String): kotlinx.datetime.format.DateTimeFormat<DateTimeComponents>? = try {
    DateTimeComponents.Format { byUnicodePattern(pattern) }
} catch (_: UnsupportedOperationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

/** Configured patterns win even when their text is also valid ISO (e.g. yyyy-dd-MM). */
private inline fun <T> configuredOrIso(text: String, format: kotlinx.datetime.format.DateTimeFormat<DateTimeComponents>,
    configured: (DateTimeComponents) -> T, iso: () -> T): T = try {
    configured(format.parse(text))
} catch (_: IllegalArgumentException) {
    iso()
}

/** Used only when the common formatter cannot build a requested pattern. */
internal expect fun formatPlatformSpecificTemporalPattern(value: Any, context: StaticValueCodecContext, pattern: String): String
internal expect fun parsePlatformSpecificTemporalPattern(text: String, context: StaticValueCodecContext, pattern: String): Any

internal fun StaticValueCodecContext.temporalPatternFailure(pattern: String, cause: Throwable? = null) =
    StaticValueCodecException(direction, origin, targetType, field?.name, field?.columnName,
        "unsupported or invalid temporal pattern '$pattern'", cause)

private fun DateTimeComponents.instant(zone: TimeZone): Instant = if (offsetHours != null) {
    toInstantUsingOffset()
} else {
    toLocalDateTime().toInstant(timeZoneId?.let(TimeZone::of) ?: zone)
}

private fun isoInstant(text: String, zone: TimeZone): Instant = try {
    Instant.parse(text)
} catch (_: IllegalArgumentException) {
    LocalDateTime.parse(text).toInstant(zone)
}

internal fun parseIsoPlatformTemporal(text: String, context: StaticValueCodecContext): Any = when (context.targetType.withoutTopLevelNullability().key) {
    "kotlin.time.Instant", "kotlinx.datetime.Instant" -> isoInstant(text, context.zone())
    "kotlinx.datetime.LocalDate" -> isoLocalDate(text, context.zone())
    "kotlinx.datetime.LocalDateTime" -> isoLocalDateTime(text, context.zone())
    "kotlinx.datetime.LocalTime" -> isoLocalTime(text, context.zone())
    else -> error("Unsupported portable temporal type ${context.targetType.key}")
}

private fun isoLocalDate(text: String, zone: TimeZone): LocalDate = try { LocalDate.parse(text) }
    catch (_: IllegalArgumentException) { isoInstant(text, zone).toLocalDateTime(zone).date }
private fun isoLocalDateTime(text: String, zone: TimeZone): LocalDateTime = try { LocalDateTime.parse(text) }
    catch (_: IllegalArgumentException) { isoInstant(text, zone).toLocalDateTime(zone) }
private fun isoLocalTime(text: String, zone: TimeZone): LocalTime = try { LocalTime.parse(text) }
    catch (_: IllegalArgumentException) { isoInstant(text, zone).toLocalDateTime(zone).time }
