package com.kotlinorm.beans.transformers

import com.kotlinorm.Kronos
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.reflect.typeOf
import kotlin.time.ExperimentalTime

class JvmDateTimeTransformerTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun `jvm date time transformer covers supported source and target shapes`() {
        val pattern = "yyyy-MM-dd HH:mm:ss"
        val epochLocal = LocalDateTime.ofInstant(Instant.EPOCH, Kronos.timeZone)
        val expectedText = DateTimeFormatter.ofPattern(pattern).format(epochLocal)
        val date = LocalDate.of(2026, 7, 14)
        val time = LocalTime.of(12, 34, 56)
        val dateTime = LocalDateTime.of(date, time)

        assertEquals(
            true,
            JvmDateTimeTransformer.isMatch(
                targetKotlinType = typeOf<String>(),
                sourceValueClass = LocalDateTime::class
            )
        )
        assertEquals(
            Instant.EPOCH,
            JvmDateTimeTransformer.transform(typeOf<Instant>(), 0L, pattern, Long::class)
        )
        assertEquals(
            expectedText,
            JvmDateTimeTransformer.transform(typeOf<String>(), Instant.EPOCH, pattern, Instant::class)
        )
        assertEquals(
            epochLocal,
            JvmDateTimeTransformer.transform(typeOf<LocalDateTime>(), Timestamp.from(Instant.EPOCH), pattern, Timestamp::class)
        )
        assertEquals(
            date,
            JvmDateTimeTransformer.transform(typeOf<LocalDate>(), "2026-07-14", pattern, String::class)
        )
        assertEquals(
            date,
            JvmDateTimeTransformer.transform(typeOf<LocalDate>(), "2026-07-14 12:34:56", pattern, String::class)
        )
        assertEquals(
            time,
            JvmDateTimeTransformer.transform(typeOf<LocalTime>(), "12:34:56", pattern, String::class)
        )
        assertEquals(
            time,
            JvmDateTimeTransformer.transform(typeOf<LocalTime>(), "2026-07-14 12:34:56", pattern, String::class)
        )
        assertEquals(
            date,
            JvmDateTimeTransformer.transform(typeOf<LocalDate>(), java.sql.Date.valueOf(date), pattern, java.sql.Date::class)
        )
        assertEquals(
            time,
            JvmDateTimeTransformer.transform(typeOf<LocalTime>(), Time.valueOf(time), pattern, Time::class)
        )
        assertEquals(
            dateTime,
            JvmDateTimeTransformer.transform(typeOf<LocalDateTime>(), dateTime, pattern, LocalDateTime::class)
        )
        assertEquals(
            dateTime,
            JvmDateTimeTransformer.transform(typeOf<LocalDateTime>(), "2026-07-14 12:34:56", pattern, String::class)
        )
        assertEquals(
            0L,
            JvmDateTimeTransformer.transform(typeOf<Long>(), kotlin.time.Instant.fromEpochMilliseconds(0), pattern, kotlin.time.Instant::class)
        )
        assertEquals(
            kotlin.time.Instant.fromEpochMilliseconds(0),
            JvmDateTimeTransformer.transform(typeOf<kotlin.time.Instant>(), expectedText, pattern, String::class)
        )
        assertEquals(
            java.sql.Date.valueOf(epochLocal.toLocalDate()),
            JvmDateTimeTransformer.transform(typeOf<java.sql.Date>(), java.sql.Date.valueOf(epochLocal.toLocalDate()), pattern, java.sql.Date::class)
        )
        assertEquals(
            Timestamp.valueOf(epochLocal),
            JvmDateTimeTransformer.transform(typeOf<Timestamp>(), expectedText, pattern, String::class)
        )
        assertEquals(
            "Unsupported target type: kotlin.collections.List<kotlin.String>",
            assertFailsWith<IllegalArgumentException> {
                JvmDateTimeTransformer.transform(typeOf<List<String>>(), expectedText, pattern, String::class)
            }.message
        )
    }

    @Test
    fun `local date and time sources preserve their exact components`() {
        val pattern = "yyyy-MM-dd HH:mm:ss"
        val date = LocalDate.of(2026, 7, 15)
        val time = LocalTime.of(9, 8, 7)
        val dateTime = LocalDateTime.of(date, time)

        assertEquals(date, JvmDateTimeTransformer.transform(typeOf<LocalDate>(), date, pattern, LocalDate::class))
        assertEquals(date, JvmDateTimeTransformer.transform(typeOf<LocalDate>(), dateTime, pattern, LocalDateTime::class))
        assertEquals(time, JvmDateTimeTransformer.transform(typeOf<LocalTime>(), time, pattern, LocalTime::class))
        assertEquals(time, JvmDateTimeTransformer.transform(typeOf<LocalTime>(), dateTime, pattern, LocalDateTime::class))
        assertEquals(
            date.atStartOfDay(),
            JvmDateTimeTransformer.transform(typeOf<LocalDateTime>(), date, pattern, LocalDate::class)
        )
        assertEquals(
            LocalDate.ofEpochDay(0).atTime(time),
            JvmDateTimeTransformer.transform(typeOf<LocalDateTime>(), time, pattern, LocalTime::class)
        )
        assertEquals(
            dateTime,
            JvmDateTimeTransformer.transform(typeOf<LocalDateTime>(), dateTime, pattern, LocalDateTime::class)
        )
    }

    @Test
    fun `sql time and util date sources convert to exact local date times`() {
        val pattern = "yyyy-MM-dd HH:mm:ss"
        val time = LocalTime.of(4, 5, 6)
        val dateTime = LocalDateTime.of(2026, 7, 15, 4, 5, 6)
        val utilDate = java.util.Date.from(dateTime.atZone(Kronos.timeZone).toInstant())

        assertEquals(
            LocalDate.ofEpochDay(0).atTime(time),
            JvmDateTimeTransformer.transform(typeOf<LocalDateTime>(), Time.valueOf(time), pattern, Time::class)
        )
        assertEquals(
            dateTime,
            JvmDateTimeTransformer.transform(typeOf<LocalDateTime>(), utilDate, pattern, java.util.Date::class)
        )
    }

    @Test
    fun `remaining supported target types use the configured time zone`() {
        val pattern = "yyyy-MM-dd HH:mm:ss"
        val dateTime = LocalDateTime.of(2026, 7, 15, 4, 5, 6)
        val zonedDateTime = dateTime.atZone(Kronos.timeZone)

        assertEquals(
            zonedDateTime,
            JvmDateTimeTransformer.transform(typeOf<java.time.ZonedDateTime>(), dateTime, pattern, LocalDateTime::class)
        )
        assertEquals(
            zonedDateTime.toOffsetDateTime(),
            JvmDateTimeTransformer.transform(typeOf<java.time.OffsetDateTime>(), dateTime, pattern, LocalDateTime::class)
        )
        assertEquals(
            java.util.Date.from(zonedDateTime.toInstant()),
            JvmDateTimeTransformer.transform(typeOf<java.util.Date>(), dateTime, pattern, LocalDateTime::class)
        )
        assertEquals(
            Time.valueOf(dateTime.toLocalTime()),
            JvmDateTimeTransformer.transform(typeOf<Time>(), dateTime, pattern, LocalDateTime::class)
        )
        assertEquals(
            false,
            JvmDateTimeTransformer.isMatch(targetKotlinType = typeOf<String>(), sourceValueClass = String::class)
        )
    }
}
