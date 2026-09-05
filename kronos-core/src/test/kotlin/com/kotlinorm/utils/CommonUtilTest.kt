package com.kotlinorm.utils

import com.kotlinorm.utils.codec.PreparedValue
import com.kotlinorm.utils.codec.PreparedValueKind
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.codec.ValueConversionRequest
import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.timeZone
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.valueCodec
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
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class CommonUtilTest {
    @Test
    fun tesSetCommonStrategy() {
        val strategy = KronosCommonStrategy(false, Field("field"))
        val clock = Clock.system(timeZone)

        fun assertStrategyTemporalValue(
            target: KronosCommonStrategy,
            expectedField: Field
        ) {
            val before = LocalDateTime.now(clock)
            lateinit var current: PreparedValue

            target.execute(true) { field, value ->
                assertEquals(expectedField, field)
                assertTrue(value is PreparedValue)
                current = value
            }

            val after = LocalDateTime.now(clock)
            assertEquals(PreparedValueKind.STRATEGY_TEMPORAL, current.kind)
            assertEquals(typeOf<LocalDateTime>(), current.sourceType)
            assertNull(current.dateFormat)
            assertTrue((current.value as LocalDateTime) in before..after)
        }

        strategy.execute(defaultValue = 0) { field, value ->
            assertEquals(field, Field("field"))
            assertTrue(value == 0)
        }

        assertStrategyTemporalValue(strategy, Field("field"))

        val pattern = "MMM dd, yyyy HH:mm:ss"
        val dateTimeStrategy = KronosCommonStrategy(false, Field("field", dateFormat = pattern))
        assertStrategyTemporalValue(dateTimeStrategy, Field("field", dateFormat = pattern))
    }

    @Test
    fun testToLinkedSet() {
        val list = [1, 2, 3, 4, 5]
        val linkedSet = list.toLinkedSet()
        assertEquals(linkedSet, linkedSetOf(1, 2, 3, 4, 5))
    }

    @Test
    fun testEscapeLikeLiteral() {
        assertEquals("plain", escapeLikeLiteral("plain"))
        assertEquals("\\%\\_", escapeLikeLiteral("%_"))
        assertEquals("a\\\\b\\%c\\_", escapeLikeLiteral("a\\b%c_"))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun valueCodecRegistryConvertsBuiltInAndCustomValues() {
        // 测试整数类型
        assertEquals(42, convert(typeOf<Int>(), 42))
        assertEquals(42, convert(typeOf<Int>(), "42"))
        assertEquals(42, convert(typeOf<Int>(), 42.0))

        // 测试长整型
        assertEquals(42L, convert(typeOf<Long>(), 42))
        assertEquals(42L, convert(typeOf<Long>(), "42"))
        assertEquals(42L, convert(typeOf<Long>(), 42.0))

        // 测试短整型
        assertEquals(42.toShort(), convert(typeOf<Short>(), 42))
        assertEquals(42.toShort(), convert(typeOf<Short>(), "42"))

        // 测试浮点型
        assertEquals(42f, convert(typeOf<Float>(), 42))
        assertEquals(42f, convert(typeOf<Float>(), "42.0"))

        // 测试双精度浮点型
        assertEquals(42.0, convert(typeOf<Double>(), 42))
        assertEquals(42.0, convert(typeOf<Double>(), "42.0"))

        // 测试精确数值类型
        assertEquals(BigDecimal("42.50"), convert(typeOf<BigDecimal>(), "42.50"))
        assertEquals(BigDecimal("42.5"), convert(typeOf<BigDecimal>(), 42.5))
        assertEquals(BigDecimal("42"), convert(typeOf<BigDecimal>(), BigInteger("42")))
        assertEquals(BigInteger("42"), convert(typeOf<BigInteger>(), "42"))
        assertEquals(BigInteger("42"), convert(typeOf<BigInteger>(), BigDecimal("42.50")))

        // 测试字节型
        assertEquals(42.toByte(), convert(typeOf<Byte>(), 42))
        assertEquals(42.toByte(), convert(typeOf<Byte>(), "42"))

        // 测试字符型
        assertEquals('A', convert(typeOf<Char>(), 65))
        assertEquals('A', convert(typeOf<Char>(), "A"))

        // 测试字符串型
        assertEquals("42", convert(typeOf<String>(), 42))
        assertEquals("Hello", convert(typeOf<String>(), "Hello"))

        // 测试布尔型
        assertEquals(true, convert(typeOf<Boolean>(), 1))
        assertEquals(false, convert(typeOf<Boolean>(), 0))
        assertEquals(true, convert(typeOf<Boolean>(), "true"))
        assertEquals(false, convert(typeOf<Boolean>(), "false"))

        // 测试日期时间类型
        val dateTimeString = "2023-10-17T10:00:00"
        assertEquals(
            LocalDateTime.parse(dateTimeString),
            convert(typeOf<LocalDateTime>(), dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).toLocalDate(),
            convert(typeOf<LocalDate>(), dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).toLocalTime(),
            convert(typeOf<java.time.LocalTime>(), dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()),
            convert(typeOf<java.time.ZonedDateTime>(), dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()).toOffsetDateTime(),
            convert(typeOf<java.time.OffsetDateTime>(), dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()).toInstant(),
            convert(typeOf<java.time.Instant>(), dateTimeString)
        )
        assertEquals(
            LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            convert(typeOf<Long>(), LocalDateTime.parse(dateTimeString))
        )
        assertEquals(
            "2023-10-17 10:00:00",
            convert(typeOf<String>(), LocalDateTime.parse(dateTimeString))
        )

        // 测试Instant类型
        assertEquals(
            convert(typeOf<java.time.Instant>(), dateTimeString), LocalDateTime.parse(dateTimeString)
                .atZone(timeZone).toInstant()
        )

        // 测试java.util.Date类型
        val date = convert(typeOf<Date>(), dateTimeString)
        assertEquals(
            date,
            Date.from(LocalDateTime.parse(dateTimeString).atZone(ZoneId.systemDefault()).toInstant())
        )

        // 测试LocalDate类型
        val dateString = dateTimeString.slice(0..<10)
        assertEquals(
            LocalDate.parse(dateString),
            convert(
                typeOf<LocalDate>(), java.sql.Date.valueOf(dateString)
            )
        )

        val kotlinXRegistration = Kronos.registerValueCodec(KotlinXDateTimeCodec)

        // 测试kotlinx.datetime类型
        val dateTime = kotlinx.datetime.LocalDateTime.parse(dateTimeString)
        val instant = dateTime.toInstant(TimeZone.of(timeZone.id))
        val localDate = dateTime.date
        val localTime = dateTime.time

        assertEquals(dateTime, convert(typeOf<kotlinx.datetime.LocalDateTime>(), dateTimeString))
        assertEquals(instant, convert(typeOf<Instant>(), dateTimeString))
        assertEquals(localDate, convert(typeOf<kotlinx.datetime.LocalDate>(), dateTimeString))
        assertEquals(localTime, convert(typeOf<kotlinx.datetime.LocalTime>(), dateTimeString))
        assertEquals("2023-10-17 10:00:00", convert(typeOf<String>(), dateTime))
        assertEquals(instant.toEpochMilliseconds(), convert(typeOf<Long>(), dateTime))

        // 测试无效输入
        assertFailsWith<ValueMappingException> {
            convert(typeOf<Int>(), "invalid")
        }
        assertEquals(false, convert(typeOf<Boolean>(), "invalid"))
        kotlinXRegistration.close()
    }

    @Test
    fun toDatabaseValueUsesFieldKType() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val intField = Field("age", "age", kType = typeOf<Int>())

        assertEquals(42, toDatabaseValue(wrapper, intField, "42"))
    }

    @Test
    fun fieldBoundEncodePassesTheCompleteDeclaredSourceKType() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val fieldType = typeOf<Int?>()
        val intField = Field("age", "age", kType = fieldType)
        var seenSourceType: KType? = null
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, context ->
                seenSourceType = context.sourceType
                context.direction == ValueCodecDirection.ENCODE && context.targetType == fieldType
            },
            convert = { _, _ -> 42 }
        ))

        try {
            assertEquals(42, toDatabaseValue(wrapper, intField, "42"))
            assertEquals(fieldType, seenSourceType)
        } finally {
            registration.close()
        }
    }

    @Test
    fun toDatabaseParameterValueUsesFieldKTypeForSuffixedParameter() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val intField = Field("status", "status", kType = typeOf<Int>())

        assertEquals(7, toDatabaseParameterValue(wrapper, mapOf("status" to intField), "status@1", "7"))
    }

    @Test
    fun expandedListIndexBelongsToValueNameAndDoesNotCreateABatchIndex() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val intField = Field("status", "status", kType = typeOf<Int>())
        val fields = mapOf("status" to intField)

        val nonBatchFailure = assertFailsWith<ValueMappingException> {
            toDatabaseParameterValue(
                wrapper,
                fields,
                "status",
                listOf("invalid"),
                expandAsList = true
            )
        }
        assertEquals("status[0]", nonBatchFailure.valueName)
        assertNull(nonBatchFailure.batchIndex)

        val batchFailure = assertFailsWith<ValueMappingException> {
            toDatabaseParameterValue(
                wrapper,
                fields,
                "status",
                listOf("invalid"),
                expandAsList = true,
                batchIndex = 7
            )
        }
        assertEquals("status[0]", batchFailure.valueName)
        assertEquals(7, batchFailure.batchIndex)
    }

    @Test
    fun databaseValueEntryPointsEncodeEnumsWithBuiltInCodec() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val statusField = Field("status", "status", kType = typeOf<CommonUtilStatus>())
        val nullableStatusField = Field("optional_status", "optionalStatus", kType = typeOf<CommonUtilStatus?>())

        assertEquals("READY", toDatabaseValue(wrapper, statusField, CommonUtilStatus.READY))
        assertEquals("READY", toDatabaseValue(wrapper, nullableStatusField, CommonUtilStatus.READY))
        assertEquals(
            "READY",
            toDatabaseParameterValue(
                wrapper,
                mapOf("status" to statusField),
                "status@1",
                CommonUtilStatus.READY
            )
        )
    }

    @Test
    fun valueCodecRegistryDecodesEnumFromGeneratedMetadata() {
        assertEquals(
            CommonUtilStatus.CLOSED,
            convert(typeOf<CommonUtilStatus>(), "CLOSED")
        )
    }

    @Test
    fun toDatabaseValueUsesReadyDatabaseValueBeforeFieldType() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val intField = Field("id", "id", kType = typeOf<Int>())
        val pattern = PreparedValue(
            value = "1%",
            sourceType = typeOf<String>(),
            kind = PreparedValueKind.READY_DATABASE_VALUE
        )

        assertEquals("1%", toDatabaseValue(wrapper, intField, pattern))
        assertEquals("1%", toDatabaseParameterValue(wrapper, mapOf("id" to intField), "id", pattern))
    }

    @Test
    fun preparedNullIsUnwrappedWithoutFallingBackToTheTransportObject() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val nullableField = Field("value", "value", kType = typeOf<String?>())
        val readyNull = PreparedValue(
            value = null,
            sourceType = typeOf<String?>(),
            kind = PreparedValueKind.READY_DATABASE_VALUE
        )
        val strategyNull = PreparedValue(
            value = null,
            sourceType = typeOf<LocalDateTime?>(),
            kind = PreparedValueKind.STRATEGY_TEMPORAL
        )

        assertNull(toDatabaseValue(wrapper, nullableField, readyNull))
        assertNull(toDatabaseValue(wrapper, nullableField, strategyNull))
    }

    @Test
    fun fieldBoundNullValidatesDeclaredKTypeNullability() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val nonNullField = Field("required", "required", kType = typeOf<String>())
        val nullableField = Field("optional", "optional", kType = typeOf<String?>())

        val failure = assertFailsWith<ValueMappingException> {
            toDatabaseValue(wrapper, nonNullField, null)
        }

        assertEquals(ValueCodecDirection.ENCODE, failure.direction)
        assertEquals(ValueCodecOrigin.PARAMETER, failure.origin)
        assertEquals(typeOf<String>(), failure.targetType)
        assertEquals("required", failure.fieldName)
        assertEquals("required", failure.valueName)
        assertEquals(null, toDatabaseValue(wrapper, nullableField, null))
        assertEquals(null, toDatabaseValue(wrapper, Field("untyped"), null))
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

    @Test
    fun temporalValueUsesDateFormatForVarcharStorage() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val current = LocalDateTime.of(2026, 7, 21, 13, 14, 15)
        val field = Field(
            "created_at",
            "createdAt",
            type = KColumnType.VARCHAR,
            dateFormat = "dd/MM/yyyy HH:mm:ss",
            kType = typeOf<LocalDateTime>()
        )

        assertEquals("21/07/2026 13:14:15", toDatabaseValue(wrapper, field, current))
    }

    @Test
    fun strategyTemporalValueUsesPropertyTypeForVarcharStorage() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val current = LocalDateTime.of(2026, 7, 17, 12, 34, 56)
        val value = strategyTemporalValue(current)
        val pattern = "yyyy/MM/dd HH:mm:ss"

        assertEquals(
            "2026/07/17 12:34:56",
            toDatabaseValue(wrapper, Field("created_at", type = KColumnType.VARCHAR, dateFormat = pattern, kType = typeOf<String>()), value)
        )
        assertEquals(
            current,
            toDatabaseValue(wrapper, Field("created_at", type = KColumnType.VARCHAR, dateFormat = pattern, kType = typeOf<LocalDateTime>()), value)
        )
        assertEquals(
            current.atZone(timeZone).toInstant().toEpochMilli(),
            toDatabaseValue(wrapper, Field("created_at", type = KColumnType.VARCHAR, dateFormat = pattern, kType = typeOf<Long>()), value)
        )
        assertEquals(
            Date.from(current.atZone(timeZone).toInstant()),
            toDatabaseValue(wrapper, Field("created_at", type = KColumnType.VARCHAR, dateFormat = pattern, kType = typeOf<Date>()), value)
        )
    }

    @Test
    fun strategyTemporalValueConvertsLongBigintWithoutReadingDateFormat() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val current = LocalDateTime.of(2026, 7, 17, 12, 34, 56)
        val field = Field(
            "created_at",
            type = KColumnType.BIGINT,
            dateFormat = "[invalid",
            kType = typeOf<Long>()
        )

        assertEquals(
            current.atZone(timeZone).toInstant().toEpochMilli(),
            toDatabaseValue(wrapper, field, strategyTemporalValue(current))
        )
    }

    @Test
    fun strategyTemporalValueFallsBackToColumnTypeWithoutKType() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val current = LocalDateTime.of(2026, 7, 17, 12, 34, 56)
        val value = strategyTemporalValue(current)
        val cases = listOf(
            Field("as_bigint", type = KColumnType.BIGINT) to
                current.atZone(timeZone).toInstant().toEpochMilli(),
            Field("as_date", type = KColumnType.DATE) to java.sql.Date.valueOf(current.toLocalDate()),
            Field("as_time", type = KColumnType.TIME) to java.sql.Time.valueOf(current.toLocalTime()),
            Field("as_datetime", type = KColumnType.DATETIME) to current,
            Field("as_timestamp", type = KColumnType.TIMESTAMP) to java.sql.Timestamp.valueOf(current),
            Field("as_text", type = KColumnType.VARCHAR, dateFormat = "yyyy/MM/dd HH:mm:ss") to
                "2026/07/17 12:34:56"
        )

        cases.forEach { (field, expected) ->
            assertEquals(expected, toDatabaseValue(wrapper, field, value), field.type.name)
        }
    }

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

    private fun convert(
        targetType: KType,
        value: Any?
    ): Any? = ValueCodecRegistry.convert(
        ValueConversionRequest(
            value = value,
            direction = ValueCodecDirection.DECODE,
            origin = ValueCodecOrigin.MAP,
            targetType = targetType
        )
    )

    private fun strategyTemporalValue(value: LocalDateTime): PreparedValue = PreparedValue(
        value = value,
        sourceType = typeOf<LocalDateTime>(),
        kind = PreparedValueKind.STRATEGY_TEMPORAL
    )

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

enum class CommonUtilStatus {
    READY,
    CLOSED
}

data class CommonUtilEnumEntity(
    var status: CommonUtilStatus? = null
) : KPojo
