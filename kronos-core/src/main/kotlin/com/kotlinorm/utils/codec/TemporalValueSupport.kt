/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm.utils.codec

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

private data class JdbcTemporalType(
    val nonNull: KType,
    val nullable: KType,
    val runtimeClass: Class<*>
)

/**
 * Lazily resolves optional JDBC temporal types once for the active classloader.
 *
 * Core compiler-box runtimes may intentionally omit the `java.sql` module. In
 * that environment this holder becomes `null`, and no JDBC target/source or
 * runtime fallback is considered. The holder is deliberately not initialized
 * at file load time so ordinary java.time codecs remain usable.
 */
private data class JdbcTemporalTypes(
    val date: JdbcTemporalType,
    val time: JdbcTemporalType,
    val timestamp: JdbcTemporalType
)

private val jdbcTemporalTypes: JdbcTemporalTypes? by lazy {
    try {
        JdbcTemporalTypes(
            date = JdbcTemporalType(
                typeOf<java.sql.Date>(),
                typeOf<java.sql.Date?>(),
                java.sql.Date::class.java
            ),
            time = JdbcTemporalType(
                typeOf<java.sql.Time>(),
                typeOf<java.sql.Time?>(),
                java.sql.Time::class.java
            ),
            timestamp = JdbcTemporalType(
                typeOf<java.sql.Timestamp>(),
                typeOf<java.sql.Timestamp?>(),
                java.sql.Timestamp::class.java
            )
        )
    } catch (_: LinkageError) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * Source categories used while normalizing runtime temporal values.
 *
 * JDBC date/time classes precede [UTIL_DATE] during detection because all three
 * extend [java.util.Date] but have different local/instant semantics.
 */
private enum class TemporalRuntimeKind {
    SQL_DATE,
    SQL_TIME,
    SQL_TIMESTAMP,
    UTIL_DATE,
    LOCAL_DATE_TIME,
    LOCAL_DATE,
    LOCAL_TIME,
    JAVA_INSTANT,
    ZONED_DATE_TIME,
    OFFSET_DATE_TIME,
    KOTLIN_INSTANT
}

/**
 * Resolves the conversion target from the complete [KType].
 *
 * Known targets are matched directly against complete nullable/non-null KTypes;
 * no classifier-name extraction drives dispatch. Unsupported or synthetic
 * KTypes are never broadened to one of their supertypes, preserving generic and
 * annotated type distinctions.
 *
 * @receiver complete logical or physical target type
 * @return a supported exact target kind, or `null` when the codec cannot create
 * an assignable value for this type
 */
internal fun KType.temporalTargetKind(): TemporalTargetKind? {
    val jdbcTypes = jdbcTemporalTypes
    return when {
        matchesType<LocalDateTime>() -> TemporalTargetKind.LOCAL_DATE_TIME
        matchesType<LocalDate>() -> TemporalTargetKind.LOCAL_DATE
        matchesType<LocalTime>() -> TemporalTargetKind.LOCAL_TIME
        matchesType<Instant>() -> TemporalTargetKind.JAVA_INSTANT
        matchesType<ZonedDateTime>() -> TemporalTargetKind.ZONED_DATE_TIME
        matchesType<OffsetDateTime>() -> TemporalTargetKind.OFFSET_DATE_TIME
        matchesType<kotlin.time.Instant>() -> TemporalTargetKind.KOTLIN_INSTANT
        matchesType<java.util.Date>() -> TemporalTargetKind.UTIL_DATE
        matchesType<Long>() -> TemporalTargetKind.LONG
        matchesType<String>() -> TemporalTargetKind.STRING
        jdbcTypes?.date?.matches(this) == true -> TemporalTargetKind.SQL_DATE
        jdbcTypes?.time?.matches(this) == true -> TemporalTargetKind.SQL_TIME
        jdbcTypes?.timestamp?.matches(this) == true -> TemporalTargetKind.SQL_TIMESTAMP
        else -> null
    }
}

private inline fun <reified T> KType.matchesType(): Boolean =
    this == typeOf<T>() || this == typeOf<T?>()

private fun JdbcTemporalType.matches(type: KType): Boolean =
    type == nonNull || type == nullable

/**
 * Recognizes a declared temporal source from its complete KType.
 *
 * Exact built-in types are matched first. Custom JDBC/driver types then use
 * KType subtype relationships, ordered from JDBC timestamp/date/time to their
 * shared `java.util.Date` parent so local/instant semantics remain distinct.
 */
private fun KType.temporalSourceKind(): TemporalRuntimeKind? {
    if (classifier !is KClass<*>) return null
    val jdbcTypes = jdbcTemporalTypes
    return when {
        jdbcTypes?.timestamp?.let { isSubtypeOfSafely(it.nullable) } == true -> TemporalRuntimeKind.SQL_TIMESTAMP
        jdbcTypes?.date?.let { isSubtypeOfSafely(it.nullable) } == true -> TemporalRuntimeKind.SQL_DATE
        jdbcTypes?.time?.let { isSubtypeOfSafely(it.nullable) } == true -> TemporalRuntimeKind.SQL_TIME
        isSubtypeOfSafely(typeOf<java.util.Date?>()) -> TemporalRuntimeKind.UTIL_DATE
        isSubtypeOfSafely(typeOf<LocalDateTime?>()) -> TemporalRuntimeKind.LOCAL_DATE_TIME
        isSubtypeOfSafely(typeOf<LocalDate?>()) -> TemporalRuntimeKind.LOCAL_DATE
        isSubtypeOfSafely(typeOf<LocalTime?>()) -> TemporalRuntimeKind.LOCAL_TIME
        isSubtypeOfSafely(typeOf<Instant?>()) -> TemporalRuntimeKind.JAVA_INSTANT
        isSubtypeOfSafely(typeOf<ZonedDateTime?>()) -> TemporalRuntimeKind.ZONED_DATE_TIME
        isSubtypeOfSafely(typeOf<OffsetDateTime?>()) -> TemporalRuntimeKind.OFFSET_DATE_TIME
        isSubtypeOfSafely(typeOf<kotlin.time.Instant?>()) -> TemporalRuntimeKind.KOTLIN_INSTANT
        else -> null
    }
}

private fun KType.isSubtypeOfSafely(supertype: KType): Boolean =
    try {
        isSubtypeOf(supertype)
    } catch (_: LinkageError) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

/**
 * Converts a temporal value to the exact classifier represented by [targetType].
 *
 * Target dispatch uses complete KType exact conditions, so nullable targets
 * retain the same conversion semantics without erasing generic/type information.
 * Source JDBC subclasses are recognized through declared KType supertypes first;
 * the optional JDBC holder is shared with runtime class fallback and remains
 * unavailable when the active classloader omits `java.sql`.
 * The lazy formatter is evaluated only for text parsing/formatting, so an
 * irrelevant invalid `dateFormat` cannot break native or epoch conversion.
 *
 * @param value non-null source selected by the registry
 * @param targetType complete logical or physical target KType
 * @param zoneId zone used when crossing between instant and local representations
 * @param formatter lazily validated formatter used only at a text boundary
 * @return one value assignable to the exact supported target classifier
 * @throws IllegalStateException when [targetType] is not an exact supported target
 */
internal fun convertTemporalValue(
    value: Any,
    targetType: KType,
    zoneId: ZoneId,
    formatter: Lazy<DateTimeFormatter>
): Any = when (targetType.temporalTargetKind() ?: error("Unsupported temporal target $targetType")) {
    TemporalTargetKind.LOCAL_DATE_TIME -> value.toLocalDateTime(zoneId, formatter)
    TemporalTargetKind.LOCAL_DATE -> value.toLocalDate(zoneId, formatter)
    TemporalTargetKind.LOCAL_TIME -> value.toLocalTime(zoneId, formatter)
    TemporalTargetKind.JAVA_INSTANT -> value.toJavaInstant(zoneId, formatter)
    TemporalTargetKind.ZONED_DATE_TIME -> value.toZonedDateTime(zoneId, formatter)
    TemporalTargetKind.OFFSET_DATE_TIME -> value.toZonedDateTime(zoneId, formatter).toOffsetDateTime()
    TemporalTargetKind.UTIL_DATE -> java.util.Date.from(value.toJavaInstant(zoneId, formatter))
    TemporalTargetKind.SQL_DATE -> java.sql.Date.valueOf(value.toLocalDate(zoneId, formatter))
    TemporalTargetKind.SQL_TIME -> java.sql.Time.valueOf(value.toLocalTime(zoneId, formatter))
    TemporalTargetKind.SQL_TIMESTAMP -> java.sql.Timestamp.valueOf(value.toLocalDateTime(zoneId, formatter))
    TemporalTargetKind.KOTLIN_INSTANT -> value.toJavaInstant(zoneId, formatter).toKotlinInstantPreservingNanos()
    TemporalTargetKind.LONG -> value.toJavaInstant(zoneId, formatter).toEpochMilli()
    TemporalTargetKind.STRING -> formatter.value.format(value.toLocalDateTime(zoneId, formatter))
}

/**
 * Classifies a runtime temporal source using explicit JVM superclass checks.
 *
 * This recognizes driver/custom subclasses of JDBC and `java.util.Date`
 * classes without turning target matching into broad supertype construction.
 * JDBC classes are checked before `java.util.Date` to retain their distinct
 * date-only, time-only and timestamp behavior.
 *
 * @receiver non-null source value
 * @return the nearest supported temporal category, or `null` for a non-temporal value
 */
private fun Any.temporalRuntimeKind(): TemporalRuntimeKind? {
    val runtimeClass = javaClass
    return when {
        this is LocalDateTime -> TemporalRuntimeKind.LOCAL_DATE_TIME
        this is LocalDate -> TemporalRuntimeKind.LOCAL_DATE
        this is LocalTime -> TemporalRuntimeKind.LOCAL_TIME
        this is Instant -> TemporalRuntimeKind.JAVA_INSTANT
        this is ZonedDateTime -> TemporalRuntimeKind.ZONED_DATE_TIME
        this is OffsetDateTime -> TemporalRuntimeKind.OFFSET_DATE_TIME
        this is kotlin.time.Instant -> TemporalRuntimeKind.KOTLIN_INSTANT
        this is java.util.Date -> runtimeClass.optionalJdbcRuntimeKind() ?: TemporalRuntimeKind.UTIL_DATE
        else -> null
    }
}

private fun Class<*>.optionalJdbcRuntimeKind(): TemporalRuntimeKind? {
    val jdbcTypes = jdbcTemporalTypes ?: return null
    return when {
        jdbcTypes.timestamp.runtimeClass.isAssignableFrom(this) -> TemporalRuntimeKind.SQL_TIMESTAMP
        jdbcTypes.date.runtimeClass.isAssignableFrom(this) -> TemporalRuntimeKind.SQL_DATE
        jdbcTypes.time.runtimeClass.isAssignableFrom(this) -> TemporalRuntimeKind.SQL_TIME
        else -> null
    }
}

internal fun Any.isTemporalRuntimeValue(): Boolean = temporalRuntimeKind() != null

/**
 * Determines temporal-source status from declared KType before runtime fallback.
 *
 * A known temporal declaration is authoritative. Runtime inspection is used
 * only when source metadata is absent, is broader than the actual runtime class,
 * or is incompatible with the value. This keeps complete KType metadata in the
 * normal path while supporting driver values exposed through broad APIs.
 *
 * @receiver declared source type from the conversion context, when available
 * @param value non-null runtime source
 * @return whether the source should enter temporal conversion
 */
internal fun KType?.isTemporalSource(value: Any): Boolean {
    if (this == null) return value.isTemporalRuntimeValue()
    if (temporalSourceKind() != null) return true
    val sourceClass = classifier as? KClass<*>
    val runtimeFallbackRequired = sourceClass == null || sourceClass != value::class || !accepts(value)
    return runtimeFallbackRequired && value.isTemporalRuntimeValue()
}

private fun Any.toLocalDateTime(zoneId: ZoneId, formatter: Lazy<DateTimeFormatter>): LocalDateTime {
    val runtimeKind = temporalRuntimeKind()
    return when {
        this is LocalDateTime -> this
        this is LocalDate -> atStartOfDay()
        this is LocalTime -> LocalDate.ofEpochDay(0).atTime(this)
        runtimeKind == TemporalRuntimeKind.SQL_TIMESTAMP -> (this as java.sql.Timestamp).toLocalDateTime()
        runtimeKind == TemporalRuntimeKind.SQL_DATE -> (this as java.sql.Date).toLocalDate().atStartOfDay()
        runtimeKind == TemporalRuntimeKind.SQL_TIME ->
            LocalDate.ofEpochDay(0).atTime((this as java.sql.Time).toLocalTime())
        this is ZonedDateTime -> withZoneSameInstant(zoneId).toLocalDateTime()
        this is OffsetDateTime -> atZoneSameInstant(zoneId).toLocalDateTime()
        this is Instant -> LocalDateTime.ofInstant(this, zoneId)
        this is kotlin.time.Instant -> LocalDateTime.ofInstant(toJavaInstantPreservingNanos(), zoneId)
        this is java.util.Date -> LocalDateTime.ofInstant(toInstant(), zoneId)
        this is Number -> LocalDateTime.ofInstant(Instant.ofEpochMilli(toLong()), zoneId)
        this is String -> parseLocalDateTime(this, formatter.value, zoneId)
        else -> parseLocalDateTime(toString(), formatter.value, zoneId)
    }
}

private fun Any.toLocalDate(zoneId: ZoneId, formatter: Lazy<DateTimeFormatter>): LocalDate {
    val runtimeKind = temporalRuntimeKind()
    return when {
        this is LocalDate -> this
        this is LocalDateTime -> toLocalDate()
        runtimeKind == TemporalRuntimeKind.SQL_DATE -> (this as java.sql.Date).toLocalDate()
        this is String -> parseLocalDate(this, formatter.value, zoneId)
        else -> toLocalDateTime(zoneId, formatter).toLocalDate()
    }
}

private fun Any.toLocalTime(zoneId: ZoneId, formatter: Lazy<DateTimeFormatter>): LocalTime {
    val runtimeKind = temporalRuntimeKind()
    return when {
        this is LocalTime -> this
        this is LocalDateTime -> toLocalTime()
        runtimeKind == TemporalRuntimeKind.SQL_TIME -> (this as java.sql.Time).toLocalTime()
        this is String -> parseLocalTime(this, formatter.value, zoneId)
        else -> toLocalDateTime(zoneId, formatter).toLocalTime()
    }
}

private fun Any.toJavaInstant(zoneId: ZoneId, formatter: Lazy<DateTimeFormatter>): Instant {
    val runtimeKind = temporalRuntimeKind()
    return when {
        this is Instant -> this
        this is kotlin.time.Instant -> toJavaInstantPreservingNanos()
        this is ZonedDateTime -> toInstant()
        this is OffsetDateTime -> toInstant()
        runtimeKind == TemporalRuntimeKind.SQL_TIMESTAMP -> (this as java.sql.Timestamp).toInstant()
        runtimeKind == TemporalRuntimeKind.SQL_DATE ->
            (this as java.sql.Date).toLocalDate().atStartOfDay(zoneId).toInstant()
        runtimeKind == TemporalRuntimeKind.SQL_TIME ->
            LocalDate.ofEpochDay(0).atTime((this as java.sql.Time).toLocalTime()).atZone(zoneId).toInstant()
        this is java.util.Date -> toInstant()
        this is Number -> Instant.ofEpochMilli(toLong())
        this is String -> parseInstant(this, zoneId, formatter.value)
        else -> toLocalDateTime(zoneId, formatter).atZone(zoneId).toInstant()
    }
}

private fun Any.toZonedDateTime(zoneId: ZoneId, formatter: Lazy<DateTimeFormatter>): ZonedDateTime {
    val runtimeKind = temporalRuntimeKind()
    return when {
        this is ZonedDateTime -> withZoneSameInstant(zoneId)
        this is OffsetDateTime -> atZoneSameInstant(zoneId)
        this is Instant -> atZone(zoneId)
        this is kotlin.time.Instant -> toJavaInstantPreservingNanos().atZone(zoneId)
        runtimeKind == TemporalRuntimeKind.SQL_TIMESTAMP -> (this as java.sql.Timestamp).toInstant().atZone(zoneId)
        runtimeKind == TemporalRuntimeKind.SQL_DATE ->
            (this as java.sql.Date).toLocalDate().atStartOfDay(zoneId)
        runtimeKind == TemporalRuntimeKind.SQL_TIME ->
            LocalDate.ofEpochDay(0).atTime((this as java.sql.Time).toLocalTime()).atZone(zoneId)
        this is java.util.Date -> toInstant().atZone(zoneId)
        this is Number -> Instant.ofEpochMilli(toLong()).atZone(zoneId)
        this is String -> parseZonedDateTime(this, zoneId, formatter.value)
        else -> toLocalDateTime(zoneId, formatter).atZone(zoneId)
    }
}

private fun parseLocalDateTime(text: String, formatter: DateTimeFormatter, zoneId: ZoneId): LocalDateTime =
    try {
        LocalDateTime.parse(text, formatter)
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(text)
        } catch (_: DateTimeParseException) {
            parseZonedDateTime(text, zoneId, formatter).toLocalDateTime()
        }
    }

private fun parseLocalDate(text: String, formatter: DateTimeFormatter, zoneId: ZoneId): LocalDate =
    try {
        LocalDate.parse(text, formatter)
    } catch (_: DateTimeParseException) {
        try {
            LocalDate.parse(text)
        } catch (_: DateTimeParseException) {
            parseLocalDateTime(text, formatter, zoneId).toLocalDate()
        }
    }

private fun parseLocalTime(text: String, formatter: DateTimeFormatter, zoneId: ZoneId): LocalTime =
    try {
        LocalTime.parse(text, formatter)
    } catch (_: DateTimeParseException) {
        try {
            LocalTime.parse(text)
        } catch (_: DateTimeParseException) {
            parseLocalDateTime(text, formatter, zoneId).toLocalTime()
        }
    }

private fun parseInstant(text: String, zoneId: ZoneId, formatter: DateTimeFormatter): Instant =
    try {
        Instant.parse(text)
    } catch (_: DateTimeParseException) {
        try {
            OffsetDateTime.parse(text).toInstant()
        } catch (_: DateTimeParseException) {
            try {
                ZonedDateTime.parse(text).toInstant()
            } catch (_: DateTimeParseException) {
                parseLocalText(text, formatter).atZone(zoneId).toInstant()
            }
        }
    }

private fun parseZonedDateTime(text: String, zoneId: ZoneId, formatter: DateTimeFormatter): ZonedDateTime =
    try {
        ZonedDateTime.parse(text).withZoneSameInstant(zoneId)
    } catch (_: DateTimeParseException) {
        try {
            OffsetDateTime.parse(text).atZoneSameInstant(zoneId)
        } catch (_: DateTimeParseException) {
            try {
                Instant.parse(text).atZone(zoneId)
            } catch (_: DateTimeParseException) {
                parseLocalText(text, formatter).atZone(zoneId)
            }
        }
    }

private fun parseLocalText(text: String, formatter: DateTimeFormatter): LocalDateTime =
    try {
        LocalDateTime.parse(text, formatter)
    } catch (_: DateTimeParseException) {
        LocalDateTime.parse(text)
    }

private fun kotlin.time.Instant.toJavaInstantPreservingNanos(): Instant =
    Instant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())

private fun Instant.toKotlinInstantPreservingNanos(): kotlin.time.Instant =
    kotlin.time.Instant.fromEpochSeconds(epochSecond, nano)
