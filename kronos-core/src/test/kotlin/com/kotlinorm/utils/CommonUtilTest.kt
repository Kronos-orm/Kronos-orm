package com.kotlinorm.utils

import com.kotlinorm.Kronos.defaultDateFormat
import com.kotlinorm.Kronos.timeZone
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.transformers.TransformerManager.registerValueTransformer
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import com.kotlinorm.wrappers.SamplePostgresJdbcWrapper
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.math.BigDecimal
import java.math.BigInteger
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
import kotlin.reflect.typeOf
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
        val list = [1, 2, 3, 4, 5]
        val linkedSet = list.toLinkedSet()
        assertEquals(linkedSet, linkedSetOf(1, 2, 3, 4, 5))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testGetTypeSafeValue() {
        // 测试整数类型
        assertEquals(42, getTypeSafeValue(typeOf<Int>(), 42))
        assertEquals(42, getTypeSafeValue(typeOf<Int>(), "42"))
        assertEquals(42, getTypeSafeValue(typeOf<Int>(), 42.0))

        // 测试长整型
        assertEquals(42L, getTypeSafeValue(typeOf<Long>(), 42))
        assertEquals(42L, getTypeSafeValue(typeOf<Long>(), "42"))
        assertEquals(42L, getTypeSafeValue(typeOf<Long>(), 42.0))

        // 测试短整型
        assertEquals(42.toShort(), getTypeSafeValue(typeOf<Short>(), 42))
        assertEquals(42.toShort(), getTypeSafeValue(typeOf<Short>(), "42"))

        // 测试浮点型
        assertEquals(42f, getTypeSafeValue(typeOf<Float>(), 42))
        assertEquals(42f, getTypeSafeValue(typeOf<Float>(), "42.0"))

        // 测试双精度浮点型
        assertEquals(42.0, getTypeSafeValue(typeOf<Double>(), 42))
        assertEquals(42.0, getTypeSafeValue(typeOf<Double>(), "42.0"))

        // 测试精确数值类型
        assertEquals(BigDecimal("42.50"), getTypeSafeValue(typeOf<BigDecimal>(), "42.50"))
        assertEquals(BigDecimal("42.5"), getTypeSafeValue(typeOf<BigDecimal>(), 42.5))
        assertEquals(BigDecimal("42"), getTypeSafeValue(typeOf<BigDecimal>(), BigInteger("42")))
        assertEquals(BigInteger("42"), getTypeSafeValue(typeOf<BigInteger>(), "42"))
        assertEquals(BigInteger("42"), getTypeSafeValue(typeOf<BigInteger>(), BigDecimal("42.50")))

        // 测试字节型
        assertEquals(42.toByte(), getTypeSafeValue(typeOf<Byte>(), 42))
        assertEquals(42.toByte(), getTypeSafeValue(typeOf<Byte>(), "42"))

        // 测试字符型
        assertEquals('A', getTypeSafeValue(typeOf<Char>(), 65))
        assertEquals('A', getTypeSafeValue(typeOf<Char>(), "A"))

        // 测试字符串型
        assertEquals("42", getTypeSafeValue(typeOf<String>(), 42))
        assertEquals("Hello", getTypeSafeValue(typeOf<String>(), "Hello"))

        // 测试布尔型
        assertEquals(true, getTypeSafeValue(typeOf<Boolean>(), 1))
        assertEquals(false, getTypeSafeValue(typeOf<Boolean>(), 0))
        assertEquals(true, getTypeSafeValue(typeOf<Boolean>(), "true"))
        assertEquals(false, getTypeSafeValue(typeOf<Boolean>(), "false"))

        // 测试日期时间类型
        val dateTimeString = "2023-10-17T10:00:00"
        assertEquals(
            LocalDateTime.parse(dateTimeString),
            getTypeSafeValue(typeOf<LocalDateTime>(), dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).toLocalDate(),
            getTypeSafeValue(typeOf<LocalDate>(), dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).toLocalTime(),
            getTypeSafeValue(typeOf<java.time.LocalTime>(), dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()),
            getTypeSafeValue(typeOf<java.time.ZonedDateTime>(), dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()).toOffsetDateTime(),
            getTypeSafeValue(typeOf<java.time.OffsetDateTime>(), dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()).toInstant(),
            getTypeSafeValue(typeOf<java.time.Instant>(), dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            getTypeSafeValue(typeOf<Long>(), LocalDateTime.parse(dateTimeString))
        )
        assertEquals(
            "2023-10-17 10:00:00",
            getTypeSafeValue(typeOf<String>(), LocalDateTime.parse(dateTimeString))
        )

        // 测试Instant类型
        assertEquals(
            getTypeSafeValue(typeOf<java.time.Instant>(), dateTimeString), LocalDateTime.parse(dateTimeString)
                .atZone(timeZone).toInstant()
        )

        // 测试java.util.Date类型
        val date = getTypeSafeValue(typeOf<Date>(), dateTimeString)
        assertEquals(
            date,
            Date.from(LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()).toInstant())
        )

        // 测试LocalDate类型
        val dateString = dateTimeString.slice(0..<10)
        assertEquals(
            LocalDate.parse(dateString),
            getTypeSafeValue(
                typeOf<LocalDate>(), java.sql.Date.valueOf(dateString)
            )
        )

        // 将 KotlinXDateTimeTransformer 添加到值转换器列表中
        registerValueTransformer(KotlinXDateTimeTransformer)

        // 测试kotlinx.datetime类型
        val dateTime = kotlinx.datetime.LocalDateTime.parse(dateTimeString)
        val instant = dateTime.toInstant(TimeZone.of(timeZone.id))
        val localDate = dateTime.date
        val localTime = dateTime.time

        assertEquals(dateTime, getTypeSafeValue(typeOf<kotlinx.datetime.LocalDateTime>(), dateTimeString))
        assertEquals(instant, getTypeSafeValue(typeOf<kotlinx.datetime.Instant>(), dateTimeString))
        assertEquals(localDate, getTypeSafeValue(typeOf<kotlinx.datetime.LocalDate>(), dateTimeString))
        assertEquals(localTime, getTypeSafeValue(typeOf<kotlinx.datetime.LocalTime>(), dateTimeString))
        assertEquals("2023-10-17 10:00:00", getTypeSafeValue(typeOf<String>(), dateTime))
        assertEquals(instant.toEpochMilliseconds(), getTypeSafeValue(typeOf<Long>(), dateTime))

        // 测试无效输入
        assertFailsWith<NumberFormatException> {
            getTypeSafeValue(typeOf<Int>(), "invalid")
        }
        assertEquals(false, getTypeSafeValue(typeOf<Boolean>(), "invalid"))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun toDatabaseValueUsesTransformerSafeValueForFieldKClass() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val intField = Field("age", "age", kType = typeOf<Int>())

        assertEquals(42, toDatabaseValue(wrapper, intField, "42"))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun toDatabaseParameterValueUsesTransformerSafeValueForSuffixedParameter() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val intField = Field("status", "status", kType = typeOf<Int>())

        assertEquals(7, toDatabaseParameterValue(wrapper, mapOf("status" to intField), "status@1", "7"))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun toDatabaseValueUsesExplicitTransformerSafeValueBeforeFieldType() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val intField = Field("id", "id", kType = typeOf<Int>())
        val pattern = TransformerSafeValue("1%", typeOf<String>())

        assertEquals("1%", toDatabaseValue(wrapper, intField, pattern))
        assertEquals("1%", toDatabaseParameterValue(wrapper, mapOf("id" to intField), "id", pattern))
    }

    @Test
    fun toDatabaseValueUsesDialectDatetimeBinding() {
        val wrapper = SamplePostgresJdbcWrapper()
        val field = Field("created_at", "createdAt", type = KColumnType.DATETIME)

        assertEquals(
            java.sql.Timestamp.valueOf("2023-10-17 10:00:00"),
            toDatabaseValue(wrapper, field, "2023-10-17T10:00:00")
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun databaseBooleanValueUsesNativeBooleanOnlyForBooleanFields() {
        val wrapper = SamplePostgresJdbcWrapper()
        val booleanField = Field("deleted", "deleted", type = KColumnType.BIT)
        val intField = Field("deleted", "deleted", kType = typeOf<Int>())

        assertEquals(false, toDatabaseBooleanValue(wrapper, booleanField, false))
        assertEquals(0, toDatabaseBooleanValue(wrapper, intField, false))
        assertEquals(SqlExpr.BooleanLiteral(false), databaseBooleanLiteral(wrapper, booleanField, false))
        assertEquals(SqlExpr.NumberLiteral("0"), databaseBooleanLiteral(wrapper, intField, false))
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
