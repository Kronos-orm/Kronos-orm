package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.interfaces.valueCodec
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.codec.ValueConversionRequest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.Locale
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class JvmPortableTemporalCodecTest {
    @Test
    fun jdbcAndJavaValuesNormalizeStructurallyBeforeCommonConversion() {
        val date = LocalDate(2026, 6, 5)
        val time = LocalTime(11, 23, 17, 123456789)
        val local = LocalDateTime(2026, 6, 5, 11, 23, 17, 123456789)
        assertEquals(date, read(Date.valueOf("2026-06-05"), typeOf<LocalDate>()))
        assertEquals(LocalTime(11, 23, 17), read(Time.valueOf("11:23:17"), typeOf<LocalTime>()))
        assertEquals(local, read(Timestamp.valueOf("2026-06-05 11:23:17.123456789"), typeOf<LocalDateTime>()))
        assertEquals(local, read(java.time.LocalDateTime.of(2026, 6, 5, 11, 23, 17, 123456789), typeOf<LocalDateTime>()))
        assertEquals(time, read(java.time.LocalTime.of(11, 23, 17, 123456789), typeOf<LocalTime>()))
        assertEquals(LocalDateTime(2026, 6, 5, 0, 0), read(Date.valueOf("2026-06-05"), typeOf<LocalDateTime>()))
        assertEquals(LocalDateTime(1970, 1, 1, 11, 23, 17), read(Time.valueOf("11:23:17"), typeOf<LocalDateTime>()))
        val offset = OffsetDateTime.parse("2026-06-05T11:23:17.123456789+08:00")
        assertEquals(Instant.parse("2026-06-05T03:23:17.123456789Z"), read(offset, typeOf<Instant>()))
        assertEquals(LocalDateTime(2026, 6, 5, 3, 23, 17, 123456789), read(offset, typeOf<LocalDateTime>()))
        assertEquals(LocalDateTime(2026, 6, 5, 11, 23, 17, 123456789), read(offset, typeOf<LocalDateTime>(), "Asia/Shanghai"))
    }

    @Test
    fun timestampInstantTargetsPreserveEpochWhileLocalTargetsPreserveJdbcWallClock() {
        val javaInstant = java.time.Instant.parse("2026-06-05T03:23:17.123456789Z")
        val timestamp = Timestamp.from(javaInstant)
        val expectedInstant = Instant.parse("2026-06-05T03:23:17.123456789Z")
        val jdbcLocal = timestamp.toLocalDateTime()
        val expectedLocal = LocalDateTime(jdbcLocal.year, jdbcLocal.monthValue, jdbcLocal.dayOfMonth,
            jdbcLocal.hour, jdbcLocal.minute, jdbcLocal.second, jdbcLocal.nano)
        for (zone in listOf("UTC", "Asia/Shanghai", "America/New_York")) {
            assertEquals(expectedInstant, read(timestamp, typeOf<Instant>(), zone))
            assertEquals(expectedLocal, read(timestamp, typeOf<LocalDateTime>(), zone))
            assertEquals(expectedLocal.date, read(timestamp, typeOf<LocalDate>(), zone))
            assertEquals(expectedLocal.time, read(timestamp, typeOf<LocalTime>(), zone))
            assertEquals(javaInstant, write(timestamp, typeOf<Instant>(), typeOf<java.time.Instant>(), zone))
            assertEquals(expectedInstant.toEpochMilliseconds(), write(timestamp, typeOf<Instant>(), typeOf<Long>(), zone))
        }
    }

    @Test
    fun portableModelValuesBindToExplicitJdbcAndJavaPhysicalTypes() {
        val date = LocalDate(2026, 6, 5)
        val time = LocalTime(11, 23, 17, 123456789)
        val local = LocalDateTime(2026, 6, 5, 11, 23, 17, 123456789)
        assertEquals(Date.valueOf("2026-06-05"), write(date, typeOf<LocalDate>(), typeOf<Date>()))
        assertEquals(Time.valueOf("11:23:17"), write(time, typeOf<LocalTime>(), typeOf<Time>()))
        assertEquals(Timestamp.valueOf("2026-06-05 11:23:17.123456789"), write(local, typeOf<LocalDateTime>(), typeOf<Timestamp>()))
        assertEquals(java.time.LocalDate.of(2026, 6, 5), write(local, typeOf<LocalDateTime>(), typeOf<java.time.LocalDate>()))
        assertEquals(java.time.LocalTime.of(11, 23, 17, 123456789), write(local, typeOf<LocalDateTime>(), typeOf<java.time.LocalTime>()))
        assertEquals(java.time.Instant.parse("2026-06-05T11:23:17.123456789Z"), write(local, typeOf<LocalDateTime>(), typeOf<java.time.Instant>()))
        assertEquals(Timestamp.from(java.time.Instant.parse("2026-06-05T03:23:17.123456789Z")),
            write(Instant.parse("2026-06-05T03:23:17.123456789Z"), typeOf<Instant>(), typeOf<Timestamp>(), "Asia/Shanghai"))
        assertEquals(java.time.LocalDateTime.of(2026, 6, 5, 0, 0),
            write(date, typeOf<LocalDate>(), typeOf<java.time.LocalDateTime>()))
    }

    @Test
    fun temporalBridgePreservesSingleUserDispatchAndStrictDecodePolicy() {
        var matches = 0
        val registration = Kronos.registerValueCodec(valueCodec(supports = { _, _ -> matches++; false }, convert = { _, _ -> error("unmatched") }))
        try {
            val input = Timestamp.valueOf("2026-06-05 11:23:17.123456789")
            assertEquals(LocalDateTime(2026, 6, 5, 11, 23, 17, 123456789), read(input, typeOf<LocalDateTime>()))
            assertEquals(1, matches)
            assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest(input, ValueCodecDirection.DECODE,
                    ValueCodecOrigin.DATABASE, typeOf<LocalDateTime>(), strict = true))
            }
            assertEquals(2, matches)
        } finally { registration.close() }
    }

    @Test
    fun legacyJvmLocalePatternsRoundTripOnlyThroughTheControlledFormatterBridge() {
        val previous = Locale.getDefault(Locale.Category.FORMAT)
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US)
            val instant = Instant.parse("2026-06-05T11:23:17.123456789Z")
            val pattern = "EEE, dd MMM yyyy HH:mm:ss.SSSSSSSSS"
            val text = "Fri, 05 Jun 2026 11:23:17.123456789"
            val request = ValueConversionRequest(instant, ValueCodecDirection.ENCODE, ValueCodecOrigin.PARAMETER,
                typeOf<Instant>(), physicalTargetType = typeOf<String>(), dateFormat = pattern, timeZone = "UTC", strict = false)
            assertEquals(text, ValueCodecRegistry.convert(request))
            assertEquals(instant, ValueCodecRegistry.convert(request.copy(value = text,
                direction = ValueCodecDirection.DECODE, physicalTargetType = null)))
            assertEquals(instant, ValueCodecRegistry.convert(request.copy(value = instant.toString(),
                direction = ValueCodecDirection.DECODE, physicalTargetType = null)))
            val localRequest = request.copy(value = LocalDateTime(2026, 6, 5, 11, 23, 17, 123456789),
                targetType = typeOf<LocalDateTime>())
            assertEquals(text, ValueCodecRegistry.convert(localRequest))
            assertEquals(localRequest.value, ValueCodecRegistry.convert(localRequest.copy(value = text,
                direction = ValueCodecDirection.DECODE, physicalTargetType = null)))
            val failure = assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(request.copy(dateFormat = "V"))
            }
            assertEquals(ValueCodecDirection.ENCODE, failure.direction)
            assertEquals(ValueCodecOrigin.PARAMETER, failure.origin)
        } finally { Locale.setDefault(Locale.Category.FORMAT, previous) }
    }

    @Test
    fun javaAndPortableInstantTargetsUseConfiguredPatternsBeforeIso() {
        val cases = listOf(
            Triple("2025-08-07T00:00:00Z", "yyyy-dd-MM'T'HH:mm:ssX", "2025-07-08T00:00:00Z"),
            Triple("2025-08-07T00:00:00.123456789+05:30", "yyyy-dd-MM'T'HH:mm:ss.SSSSSSSSSXXX", "2025-07-07T18:30:00.123456789Z"),
            Triple("2025-08-07T00:00:00 Europe/Berlin", "yyyy-dd-MM'T'HH:mm:ss VV", "2025-07-07T22:00:00Z"),
            Triple("2025-12-31T21:23:45.123456789+02:00", "dd/MM/yyyy HH:mm:ss", "2025-12-31T19:23:45.123456789Z")
        )
        for ((text, pattern, iso) in cases) for (zone in listOf("UTC", "Asia/Shanghai", "America/New_York")) {
            assertInstantTargets(text, pattern, zone, java.time.Instant.parse(iso))
        }
        // An offset-free physical value is a wall clock in the configured zone, never an already-known epoch.
        for (zone in listOf("UTC", "Asia/Shanghai", "America/New_York")) {
            val expected = java.time.LocalDateTime.of(2025, 7, 8, 0, 0).atZone(java.time.ZoneId.of(zone)).toInstant()
            assertInstantTargets("2025-08-07T00:00:00", "yyyy-dd-MM'T'HH:mm:ss", zone, expected)
        }
    }

    @Test
    fun javaAndPortableInstantPatternPriorityIsRequestFieldGlobal() {
        val previous = Kronos.defaultDateFormat
        try {
            Kronos.defaultDateFormat = "yyyy-dd-MM'T'HH:mm:ssX"
            for (type in listOf(typeOf<java.time.Instant>(), typeOf<Instant>(),
                typeOf<java.time.ZonedDateTime>(), typeOf<java.time.OffsetDateTime>())) {
                val base = ValueConversionRequest("2025-08-07T00:00:00Z", ValueCodecDirection.DECODE,
                    ValueCodecOrigin.DATABASE, type, timeZone = "UTC", strict = false)
                fun expected(iso: String): Any = expectedTemporalTarget(type, java.time.Instant.parse(iso), "UTC")
                assertEquals(expected("2025-07-08T00:00:00Z"), ValueCodecRegistry.convert(base))
                val field = com.kotlinorm.beans.dsl.Field("created_at", dateFormat = "yyyy-MM-dd'T'HH:mm:ssX", kType = type)
                assertEquals(expected("2025-08-07T00:00:00Z"), ValueCodecRegistry.convert(base.copy(field = field)))
                assertEquals(expected("2025-07-08T00:00:00Z"), ValueCodecRegistry.convert(base.copy(field = field,
                    dateFormat = "yyyy-dd-MM'T'HH:mm:ssX")))
            }
        } finally { Kronos.defaultDateFormat = previous }
    }

    private fun assertInstantTargets(text: String, pattern: String, zone: String, expected: java.time.Instant) {
        for (type in listOf(typeOf<java.time.Instant>(), typeOf<Instant>(),
            typeOf<java.time.ZonedDateTime>(), typeOf<java.time.OffsetDateTime>())) {
            val request = ValueConversionRequest(text, ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE,
                type, dateFormat = pattern, timeZone = zone, strict = false)
            assertEquals(expectedTemporalTarget(type, expected, zone), ValueCodecRegistry.convert(request), "$type $text $pattern $zone")
        }
    }

    private fun expectedTemporalTarget(type: KType, instant: java.time.Instant, zone: String): Any = when (type) {
        typeOf<Instant>() -> Instant.fromEpochSeconds(instant.epochSecond, instant.nano)
        typeOf<java.time.ZonedDateTime>() -> instant.atZone(java.time.ZoneId.of(zone))
        typeOf<java.time.OffsetDateTime>() -> instant.atZone(java.time.ZoneId.of(zone)).toOffsetDateTime()
        else -> instant
    }

    @Test
    fun timestampTargetsPreserveEpochAcrossDefaultAndRequestZones() {
        val previous = java.util.TimeZone.getDefault()
        val instant = java.time.Instant.parse("2026-06-05T03:23:17.123456789Z")
        val milliseconds = java.time.Instant.ofEpochMilli(instant.toEpochMilli())
        val inputs = listOf(
            Triple(instant, typeOf<java.time.Instant>(), instant),
            Triple(instant.atZone(java.time.ZoneId.of("Asia/Tokyo")), typeOf<java.time.ZonedDateTime>(), instant),
            Triple(instant.atOffset(java.time.ZoneOffset.ofHours(-3)), typeOf<java.time.OffsetDateTime>(), instant),
            Triple(instant.toEpochMilli(), typeOf<Long>(), milliseconds),
            Triple(java.util.Date.from(instant), typeOf<java.util.Date>(), milliseconds),
            Triple(Timestamp.from(instant), typeOf<Timestamp>(), instant),
            Triple(Instant.fromEpochSeconds(instant.epochSecond, instant.nano), typeOf<Instant>(), instant)
        )
        try {
            for (defaultZone in listOf("UTC", "Asia/Shanghai", "America/New_York")) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(defaultZone))
                for (zone in listOf("UTC", "Asia/Shanghai", "America/New_York")) for ((value, logical, expected) in inputs) {
                    for (direction in ValueCodecDirection.entries) {
                        val request = ValueConversionRequest(value, direction, ValueCodecOrigin.DATABASE,
                            if (direction == ValueCodecDirection.ENCODE) logical else typeOf<Timestamp>(),
                            physicalTargetType = if (direction == ValueCodecDirection.ENCODE) typeOf<Timestamp>() else null,
                            timeZone = zone, dateFormat = "[invalid", strict = false)
                        val result = ValueCodecRegistry.convert(request) as Timestamp
                        assertEquals(expected, result.toInstant(), "$logical $direction default=$defaultZone requested=$zone")
                    }
                }
            }
        } finally { java.util.TimeZone.setDefault(previous) }
    }

    @Test
    fun timestampLocalAndTextTargetsKeepJdbcWallClockAcrossDefaultAndRequestZones() {
        val previous = java.util.TimeZone.getDefault()
        val local = java.time.LocalDateTime.of(2026, 6, 5, 11, 23, 17, 123456789)
        try {
            for (defaultZone in listOf("UTC", "Asia/Shanghai", "America/New_York")) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(defaultZone))
                val inputs = listOf(
                    Triple(local, typeOf<java.time.LocalDateTime>(), local),
                    Triple(local.toLocalDate(), typeOf<java.time.LocalDate>(), local.toLocalDate().atStartOfDay()),
                    Triple(local.toLocalTime(), typeOf<java.time.LocalTime>(), java.time.LocalDate.ofEpochDay(0).atTime(local.toLocalTime())),
                    Triple(Date.valueOf(local.toLocalDate()), typeOf<Date>(), local.toLocalDate().atStartOfDay()),
                    Triple(Time.valueOf(local.toLocalTime()), typeOf<Time>(), java.time.LocalDate.ofEpochDay(0).atTime(local.toLocalTime().withNano(0))),
                    Triple("2026-06-05 11:23:17.123456789", typeOf<String>(), local),
                    Triple(LocalDateTime(2026, 6, 5, 11, 23, 17, 123456789), typeOf<LocalDateTime>(), local)
                )
                for (zone in listOf("UTC", "Asia/Shanghai", "America/New_York")) for ((value, logical, expected) in inputs) {
                    for (direction in ValueCodecDirection.entries) {
                        val request = ValueConversionRequest(value, direction, ValueCodecOrigin.DATABASE,
                            if (direction == ValueCodecDirection.ENCODE) logical else typeOf<Timestamp>(),
                            physicalTargetType = if (direction == ValueCodecDirection.ENCODE) typeOf<Timestamp>() else null,
                            timeZone = zone, dateFormat = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS", strict = false)
                        val result = ValueCodecRegistry.convert(request) as Timestamp
                        assertEquals(expected, result.toLocalDateTime(), "$logical $direction default=$defaultZone requested=$zone")
                    }
                }
            }
        } finally { java.util.TimeZone.setDefault(previous) }
    }

    private fun read(value: Any, type: KType, zone: String = "UTC"): Any? = ValueCodecRegistry.convert(
        ValueConversionRequest(value, ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE, type,
            timeZone = zone, dateFormat = "yyyy-dd-MM HH:ss:mm", strict = false))

    private fun write(value: Any, logical: KType, physical: KType, zone: String = "UTC"): Any? = ValueCodecRegistry.convert(
        ValueConversionRequest(value, ValueCodecDirection.ENCODE, ValueCodecOrigin.PARAMETER, logical,
            physicalTargetType = physical, timeZone = zone, dateFormat = "[invalid", strict = false))
}
