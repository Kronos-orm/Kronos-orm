package com.kotlinorm.interfaces

import com.kotlinorm.utils.codec.convertTemporalValue
import com.kotlinorm.utils.codec.toJvmTemporalRepresentation
import com.kotlinorm.utils.codec.toPortableJvmTemporal
import com.kotlinorm.utils.withoutTopLevelNullability
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import kotlin.reflect.typeOf

/** Preserve JVM locale-aware formats without replacing the common numeric/offset grammar. */
internal actual fun formatPlatformSpecificTemporalPattern(value: Any, context: StaticValueCodecContext, pattern: String): String = try {
    convertTemporalValue(value.toJvmTemporalRepresentation(), typeOf<String>(), context.zone(),
        lazy(LazyThreadSafetyMode.NONE) { DateTimeFormatter.ofPattern(pattern) }) as String
} catch (cause: java.time.DateTimeException) {
    throw context.temporalPatternFailure(pattern, cause)
} catch (cause: IllegalArgumentException) {
    throw context.temporalPatternFailure(pattern, cause)
}

internal actual fun parsePlatformSpecificTemporalPattern(text: String, context: StaticValueCodecContext, pattern: String): Any = try {
    val parsed = DateTimeFormatter.ofPattern(pattern).parse(text)
    when (context.targetType.withoutTopLevelNullability().key) {
        "kotlin.time.Instant", "kotlinx.datetime.Instant" -> {
            val instant = if (parsed.isSupported(ChronoField.INSTANT_SECONDS)) Instant.from(parsed)
                else LocalDateTime.from(parsed).atZone(context.zone()).toInstant()
            instant.toPortableJvmTemporal()
        }
        "kotlinx.datetime.LocalDate" -> LocalDate.from(parsed).toPortableJvmTemporal()
        "kotlinx.datetime.LocalTime" -> LocalTime.from(parsed).toPortableJvmTemporal()
        "kotlinx.datetime.LocalDateTime" -> (if (parsed.isSupported(ChronoField.INSTANT_SECONDS)) {
            LocalDateTime.ofInstant(Instant.from(parsed), context.zone())
        } else LocalDateTime.from(parsed)).toPortableJvmTemporal()
        else -> throw context.temporalPatternFailure(pattern)
    }
} catch (cause: java.time.DateTimeException) {
    try {
        parseIsoPlatformTemporal(text, context)
    } catch (isoCause: IllegalArgumentException) {
        throw context.temporalPatternFailure(pattern, isoCause).also { it.addSuppressed(cause) }
    }
} catch (cause: IllegalArgumentException) {
    throw context.temporalPatternFailure(pattern, cause)
}

private fun StaticValueCodecContext.zone(): ZoneId = timeZone?.let(ZoneId::of) ?: ZoneId.systemDefault()
