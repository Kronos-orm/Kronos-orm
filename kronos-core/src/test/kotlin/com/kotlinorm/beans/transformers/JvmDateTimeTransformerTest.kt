package com.kotlinorm.beans.transformers

import com.kotlinorm.Kronos
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
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
}
