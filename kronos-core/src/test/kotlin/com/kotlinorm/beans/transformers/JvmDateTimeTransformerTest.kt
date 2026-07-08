package com.kotlinorm.beans.transformers

import com.kotlinorm.Kronos
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
                targetKotlinType = "kotlin.String",
                superTypesOfValue = listOf("java.time.LocalDateTime"),
                kClassOfValue = String::class
            )
        )
        assertEquals(
            Instant.EPOCH,
            JvmDateTimeTransformer.transform("java.time.Instant", 0L, emptyList(), pattern, Long::class)
        )
        assertEquals(
            expectedText,
            JvmDateTimeTransformer.transform("kotlin.String", Instant.EPOCH, emptyList(), pattern, Instant::class)
        )
        assertEquals(
            epochLocal,
            JvmDateTimeTransformer.transform("java.time.LocalDateTime", Timestamp.from(Instant.EPOCH), emptyList(), pattern, Timestamp::class)
        )
        assertEquals(
            0L,
            JvmDateTimeTransformer.transform("kotlin.Long", kotlin.time.Instant.fromEpochMilliseconds(0), emptyList(), pattern, kotlin.time.Instant::class)
        )
        assertEquals(
            kotlin.time.Instant.fromEpochMilliseconds(0),
            JvmDateTimeTransformer.transform("kotlin.time.Instant", expectedText, emptyList(), pattern, String::class)
        )
        assertEquals(
            java.sql.Date.valueOf(epochLocal.toLocalDate()),
            JvmDateTimeTransformer.transform("java.sql.Date", java.sql.Date.valueOf(epochLocal.toLocalDate()), emptyList(), pattern, java.sql.Date::class)
        )
        assertEquals(
            Timestamp.valueOf(epochLocal),
            JvmDateTimeTransformer.transform("java.sql.Timestamp", expectedText, emptyList(), pattern, String::class)
        )
        assertEquals(
            "Unsupported target type: unsupported.Type",
            assertFailsWith<IllegalArgumentException> {
                JvmDateTimeTransformer.transform("unsupported.Type", expectedText, emptyList(), pattern, String::class)
            }.message
        )
    }
}
