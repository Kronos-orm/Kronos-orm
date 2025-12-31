package com.kotlinorm.utils

import com.kotlinorm.Kronos.defaultDateFormat
import com.kotlinorm.Kronos.timeZone
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.transformers.TransformerManager.registerValueTransformer
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

class CommonUtilTest {
    @Test
    fun tesSetCommonStrategy() {
        val strategy = KronosCommonStrategy(false, Field("field"))

        strategy.execute(defaultValue = 0) { field, value ->
            assertEquals(field, Field("field"))
            assertTrue(value == 0)
        }

        strategy.execute(true) { _, value ->
            assertTrue(value is String)
            assertTrue(

                LocalDateTime.now(Clock.system(timeZone)).isAfter(
                    LocalDateTime.parse(value, DateTimeFormatter.ofPattern(defaultDateFormat))
                )
            )
        }

        val pattern = "MMM dd, yyyy HH:mm:ss"
        val dateTimeStrategy = KronosCommonStrategy(false, Field("field", dateFormat = pattern))
        dateTimeStrategy.execute(true) { _, value ->
            assertTrue(value is String)
            assertTrue(
                LocalDateTime.now(Clock.system(timeZone)).isAfter(
                    LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern))
                )
            )
        }
    }

    @Test
    fun testToLinkedSet() {
        val list = listOf(1, 2, 3, 4, 5)
        val linkedSet = list.toLinkedSet()
        assertEquals(linkedSet, linkedSetOf(1, 2, 3, 4, 5))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testGetTypeSafeValue() {
        // 测试整数类型
        assertEquals(42, getTypeSafeValue("kotlin.Int", 42))
        assertEquals(42, getTypeSafeValue("kotlin.Int", "42"))
        assertEquals(42, getTypeSafeValue("kotlin.Int", 42.0))

        // 测试长整型
        assertEquals(42L, getTypeSafeValue("kotlin.Long", 42))
        assertEquals(42L, getTypeSafeValue("kotlin.Long", "42"))
        assertEquals(42L, getTypeSafeValue("kotlin.Long", 42.0))

        // 测试短整型
        assertEquals(42.toShort(), getTypeSafeValue("kotlin.Short", 42))
        assertEquals(42.toShort(), getTypeSafeValue("kotlin.Short", "42"))

        // 测试浮点型
        assertEquals(42f, getTypeSafeValue("kotlin.Float", 42))
        assertEquals(42f, getTypeSafeValue("kotlin.Float", "42.0"))

        // 测试双精度浮点型
        assertEquals(42.0, getTypeSafeValue("kotlin.Double", 42))
        assertEquals(42.0, getTypeSafeValue("kotlin.Double", "42.0"))

        // 测试字节型
        assertEquals(42.toByte(), getTypeSafeValue("kotlin.Byte", 42))
        assertEquals(42.toByte(), getTypeSafeValue("kotlin.Byte", "42"))

        // 测试字符型
        assertEquals('A', getTypeSafeValue("kotlin.Char", 65))
        assertEquals('A', getTypeSafeValue("kotlin.Char", "A"))

        // 测试字符串型
        assertEquals("42", getTypeSafeValue("kotlin.String", 42))
        assertEquals("Hello", getTypeSafeValue("kotlin.String", "Hello"))

        // 测试布尔型
        assertEquals(true, getTypeSafeValue("kotlin.Boolean", 1))
        assertEquals(false, getTypeSafeValue("kotlin.Boolean", 0))
        assertEquals(true, getTypeSafeValue("kotlin.Boolean", "true"))
        assertEquals(false, getTypeSafeValue("kotlin.Boolean", "false"))

        // 测试日期时间类型
        val dateTimeString = "2023-10-17T10:00:00"
        assertEquals(
            LocalDateTime.parse(dateTimeString),
            getTypeSafeValue("java.time.LocalDateTime", dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).toLocalDate(),
            getTypeSafeValue("java.time.LocalDate", dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).toLocalTime(),
            getTypeSafeValue("java.time.LocalTime", dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()),
            getTypeSafeValue("java.time.ZonedDateTime", dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()).toOffsetDateTime(),
            getTypeSafeValue("java.time.OffsetDateTime", dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()).toInstant(),
            getTypeSafeValue("java.time.Instant", dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            getTypeSafeValue("kotlin.Long", LocalDateTime.parse(dateTimeString))
        )
        assertEquals(
            "2023-10-17 10:00:00",
            getTypeSafeValue("kotlin.String", LocalDateTime.parse(dateTimeString))
        )

        // 测试Instant类型
        assertEquals(
            getTypeSafeValue("java.time.Instant", dateTimeString), LocalDateTime.parse(dateTimeString)
                .atZone(timeZone).toInstant()
        )

        // 测试java.util.Date类型
        val date = getTypeSafeValue("java.util.Date", dateTimeString)
        assertEquals(
            date,
            Date.from(LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()).toInstant())
        )

        // 测试LocalDate类型
        val dateString = dateTimeString.slice(0..<10)
        assertEquals(
            LocalDate.parse(dateString),
            getTypeSafeValue(
                "java.time.LocalDate", java.sql.Date.valueOf(dateString)
            )
        )

        // 将 KotlinXDateTimeTransformer 添加到值转换器列表中
        registerValueTransformer(KotlinXDateTimeTransformer)

        // 测试kotlinx.datetime类型
        val dateTime = kotlinx.datetime.LocalDateTime.parse(dateTimeString)
        val instant = dateTime.toInstant(TimeZone.of(timeZone.id))
        val localDate = dateTime.date
        val localTime = dateTime.time

        assertEquals(dateTime, getTypeSafeValue("kotlinx.datetime.LocalDateTime", dateTimeString))
        assertEquals(instant, getTypeSafeValue("kotlinx.datetime.Instant", dateTimeString))
        assertEquals(localDate, getTypeSafeValue("kotlinx.datetime.LocalDate", dateTimeString))
        assertEquals(localTime, getTypeSafeValue("kotlinx.datetime.LocalTime", dateTimeString))
        assertEquals("2023-10-17 10:00:00", getTypeSafeValue("kotlin.String", dateTime))
        assertEquals(instant.toEpochMilliseconds(), getTypeSafeValue("kotlin.Long", dateTime))

        // 测试无效输入
        assertFailsWith<NumberFormatException> {
            getTypeSafeValue("kotlin.Int", "invalid")
        }
        assertEquals(false, getTypeSafeValue("kotlin.Boolean", "invalid"))
    }

    @Test
    fun returnsPairOfNumbersWhenBothArePresent() {
        val result = extractNumberInParentheses("(123,456)")
        assertEquals(Pair(123, 456), result)
    }

    @Test
    fun returnsPairWithSecondNumberAsZeroWhenOnlyOneNumberIsPresent() {
        val result = extractNumberInParentheses("(123)")
        assertEquals(Pair(123, 0), result)
    }

    @Test
    fun returnsDefaultPairWhenNoParenthesesArePresent() {
        val result = extractNumberInParentheses("No numbers here")
        assertEquals(Pair(0, 0), result)
    }

    @Test
    fun returnsDefaultPairWhenInputIsEmpty() {
        val result = extractNumberInParentheses("")
        assertEquals(Pair(0, 0), result)
    }

    @Test
    fun handlesExtraWhitespaceAroundParentheses() {
        val result = extractNumberInParentheses("  (123, 456)  ")
        assertEquals(Pair(123, 456), result)
    }

    @Test
    fun handlesNestedParenthesesByExtractingFirstMatch() {
        val result = extractNumberInParentheses("(123,456)(789,101)")
        assertEquals(Pair(123, 456), result)
    }

    @Test
    fun ignoresInvalidFormatsAndReturnsDefaultPair() {
        val result = extractNumberInParentheses("(abc,def)")
        assertEquals(Pair(0, 0), result)
    }
}