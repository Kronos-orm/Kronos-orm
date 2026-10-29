package com.kotlinorm.interfaces

import com.kotlinorm.Kronos
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.utils.StaticTypeRef
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.codec.ValueConversionRequest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant

class ValueCodecBoundaryMatrixTest {
    @Test
    fun configuredPatternsWinOverIsoAndRoundTripEveryPortableShape() {
        val cases = listOf(
            Triple(LocalDate(2026, 6, 5), "yyyy-dd-MM", "2026-05-06") to typeOf<LocalDate>(),
            Triple(LocalDateTime(2026, 6, 5, 11, 23, 17), "yyyy-dd-MM'T'HH:ss:mm", "2026-05-06T11:17:23") to typeOf<LocalDateTime>(),
            Triple(LocalTime(11, 23, 17), "HH:ss:mm", "11:17:23") to typeOf<LocalTime>(),
            Triple(Instant.parse("2026-06-05T11:23:17Z"), "yyyy-dd-MM'T'HH:ss:mm'Z'", "2026-05-06T11:17:23Z") to typeOf<Instant>()
        )
        for ((shape, type) in cases) {
            val request = ValueConversionRequest(shape.third, ValueCodecDirection.DECODE,
                ValueCodecOrigin.DATABASE, type, dateFormat = shape.second, timeZone = "UTC", strict = false)
            assertEquals(shape.first, ValueCodecRegistry.convert(request))
            assertEquals(shape.third, ValueCodecRegistry.convert(request.copy(value = shape.first,
                direction = ValueCodecDirection.ENCODE, physicalTargetType = typeOf<String>())))
            val static = StaticValueCodecContext(ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE,
                targetType = StaticTypeRef("kotlin.Any", false),
                dateFormat = shape.second, timeZone = "UTC")
            val key = when (shape.first) {
                is Instant -> "kotlin.time.Instant"
                is LocalDate -> "kotlinx.datetime.LocalDate"
                is LocalDateTime -> "kotlinx.datetime.LocalDateTime"
                else -> "kotlinx.datetime.LocalTime"
            }
            val context = static.copy(targetType = StaticTypeRef(key, false))
            assertEquals(shape.first, KronosValueCodecs.decode(KronosPhysicalValue.Text(shape.third), context))
            assertEquals(shape.third, (KronosValueCodecs.encode(shape.first,
                context.copy(direction = ValueCodecDirection.ENCODE)) as KronosPhysicalValue.Text).value)
        }
    }

    @Test
    fun temporalTextPatternPriorityIsRequestThenFieldThenGlobalWithIsoFallback() {
        val previous = Kronos.defaultDateFormat
        try {
            Kronos.defaultDateFormat = "yyyy-dd-MM"
            val base = ValueConversionRequest("2026-05-06", ValueCodecDirection.DECODE,
                ValueCodecOrigin.DATABASE, typeOf<LocalDate>(), strict = false)
            assertEquals(LocalDate(2026, 6, 5), ValueCodecRegistry.convert(base))
            val field = Field("day", dateFormat = "yyyy-MM-dd", kType = typeOf<LocalDate>())
            assertEquals(LocalDate(2026, 5, 6), ValueCodecRegistry.convert(base.copy(field = field)))
            assertEquals(LocalDate(2026, 6, 5), ValueCodecRegistry.convert(base.copy(field = field, dateFormat = "yyyy-dd-MM")))
            assertEquals(LocalDate(2026, 12, 31), ValueCodecRegistry.convert(base.copy(value = "2026-12-31")))
        } finally { Kronos.defaultDateFormat = previous }
    }

    @Test
    fun logicalTemporalConversionUsesStructureAndFixedAnchorsRegardlessOfTextPattern() {
        val instant = Instant.parse("2026-06-05T11:23:17Z")
        val local = LocalDateTime(2026, 6, 5, 11, 23, 17)
        val date = LocalDate(2026, 6, 5)
        val time = LocalTime(11, 23, 17)
        val inputs = listOf(instant to local, local to local,
            date to LocalDateTime(2026, 6, 5, 0, 0), time to LocalDateTime(1970, 1, 1, 11, 23, 17))
        for ((input, expected) in inputs) {
            val base = ValueConversionRequest(input, ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE,
                typeOf<LocalDateTime>(), dateFormat = "yyyy-dd-MM HH:ss:mm", timeZone = "UTC", strict = false)
            assertEquals(expected, ValueCodecRegistry.convert(base))
            assertEquals(expected.date, ValueCodecRegistry.convert(base.copy(targetType = typeOf<LocalDate>())))
            assertEquals(expected.time, ValueCodecRegistry.convert(base.copy(targetType = typeOf<LocalTime>())))
            assertEquals(expected.toInstant(TimeZone.UTC), ValueCodecRegistry.convert(base.copy(targetType = typeOf<Instant>())))
            assertEquals(expected.toInstant(TimeZone.UTC).toEpochMilliseconds(), ValueCodecRegistry.convert(base.copy(
                direction = ValueCodecDirection.ENCODE,
                targetType = when (input) {
                    is Instant -> typeOf<Instant>()
                    is LocalDateTime -> typeOf<LocalDateTime>()
                    is LocalDate -> typeOf<LocalDate>()
                    else -> typeOf<LocalTime>()
                }, physicalTargetType = typeOf<Long>())))
        }
    }

    @Test
    fun configuredOffsetsRoundTripWithoutLosingTheirInstant() {
        val value = Instant.parse("2026-06-05T01:23:17Z")
        val base = ValueConversionRequest(value, ValueCodecDirection.ENCODE, ValueCodecOrigin.PARAMETER,
            typeOf<Instant>(), physicalTargetType = typeOf<String>(), dateFormat = "yyyy-MM-dd HH:mm:ssXXX",
            timeZone = "Asia/Shanghai", strict = false)
        val text = "2026-06-05 09:23:17+08:00"
        assertEquals(text, ValueCodecRegistry.convert(base))
        assertEquals(value, ValueCodecRegistry.convert(base.copy(value = text, direction = ValueCodecDirection.DECODE,
            physicalTargetType = null, timeZone = "UTC")))
    }

    @Test
    fun strictTemporalReadsAcceptIdentityAndRejectTextAndEpoch() {
        val instant = Instant.parse("2026-06-05T01:23:17Z")
        for (origin in ValueCodecOrigin.entries) {
            val base = ValueConversionRequest(instant, ValueCodecDirection.DECODE, origin, typeOf<Instant>(), strict = true)
            assertEquals(instant, ValueCodecRegistry.convert(base))
            for (input in listOf(instant.toString(), instant.toEpochMilliseconds())) {
                assertFailsWith<ValueMappingException> { ValueCodecRegistry.convert(base.copy(value = input)) }
            }
        }
    }

    @Test
    fun builtinNumericConversionsRejectOverflowAcrossDirectionsAndOrigins() {
        val invalid = listOf(
            128L to typeOf<Byte>(), -129L to typeOf<Byte>(), 32768L to typeOf<Short>(),
            2147483648L to typeOf<Int>(), Double.MAX_VALUE to typeOf<Float>(),
            "1e999" to typeOf<Double>(), "3.5e38" to typeOf<Float>(),
            65536L to typeOf<Char>(), -1L to typeOf<Char>(),
            Double.NaN to typeOf<Int>(), Double.POSITIVE_INFINITY to typeOf<Long>())
        for (direction in ValueCodecDirection.entries) for (origin in ValueCodecOrigin.entries) {
            for ((value, type) in invalid) {
                assertFailsWith<ValueMappingException>("$direction/$origin $value -> $type") {
                    scalar(value, type, direction, origin)
                }
            }
            assertEquals('B', scalar("Beta", typeOf<Char>(), direction, origin))
            assertEquals('A', scalar(65.9, typeOf<Char>(), direction, origin))
            assertEquals(7, scalar(7.9, typeOf<Int>(), direction, origin))
            assertEquals(42, scalar("42", typeOf<Int>(), direction, origin))
            assertEquals(Float.MAX_VALUE, scalar(Float.MAX_VALUE.toDouble(), typeOf<Float>(), direction, origin))
        }
        assertFailsWith<ValueMappingException> {
            scalar(Double.MAX_VALUE, typeOf<Float>(), ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE, strict = true)
        }
        assertFailsWith<ValueMappingException> {
            scalar(7.9, typeOf<Int>(), ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE, strict = true)
        }
    }

    @Test
    fun booleanAndIntegerPhysicalTargetsUseTheirOwnTypeDescriptorsAcrossDirections() {
        val booleanInfo = StaticTypeInfo(StaticTypeRef("kotlin.Boolean", false))
        val integerInfo = StaticTypeInfo(StaticTypeRef("kotlin.Int", false))
        val booleanField = Field("deleted", kType = typeOf<Boolean>(), typeInfo = booleanInfo)
        val integerField = Field("flag", kType = typeOf<Int>(), typeInfo = integerInfo)
        for (origin in ValueCodecOrigin.entries) for (strict in listOf(false, true)) for (flag in listOf(false, true)) {
            val integer = if (flag) 1 else 0
            for (physicalInfo in listOf(null, integerInfo)) {
                val encode = ValueConversionRequest(flag, ValueCodecDirection.ENCODE, origin, typeOf<Boolean>(),
                    field = booleanField, strict = strict, physicalTargetType = typeOf<Int>(), physicalTargetTypeInfo = physicalInfo)
                assertEquals(integer, ValueCodecRegistry.convert(encode))
                assertEquals(integer, ValueCodecRegistry.convert(encode.copy(targetType = typeOf<Boolean?>())))
            }
            assertEquals(flag, ValueCodecRegistry.convert(ValueConversionRequest(integer, ValueCodecDirection.ENCODE,
                origin, typeOf<Int>(), field = integerField, physicalTargetType = typeOf<Boolean>(),
                physicalTargetTypeInfo = booleanInfo, strict = strict)))
            assertEquals(flag, ValueCodecRegistry.convert(ValueConversionRequest(integer, ValueCodecDirection.DECODE,
                origin, typeOf<Boolean>(), field = booleanField, strict = false)))
            assertEquals(integer, ValueCodecRegistry.convert(ValueConversionRequest(flag, ValueCodecDirection.DECODE,
                origin, typeOf<Int>(), field = integerField, strict = false)))
        }
    }

    @Test
    fun explicitNonFiniteValuesAndUserOverridesAreNotMistakenForFiniteOverflow() {
        for (strict in listOf(false, true)) for (origin in ValueCodecOrigin.entries) {
            assertTrue((scalar(Double.NaN, typeOf<Double>(), origin = origin, strict = strict) as Double).isNaN())
            assertTrue((scalar(Float.NaN, typeOf<Float>(), origin = origin, strict = strict) as Float).isNaN())
            assertEquals(Double.POSITIVE_INFINITY, scalar(Double.POSITIVE_INFINITY, typeOf<Double>(), origin = origin, strict = strict))
            assertEquals(Float.NEGATIVE_INFINITY, scalar(Float.NEGATIVE_INFINITY, typeOf<Float>(), origin = origin, strict = strict))
        }
        assertEquals(Float.POSITIVE_INFINITY, scalar(Double.POSITIVE_INFINITY, typeOf<Float>()))
        assertEquals(Double.NEGATIVE_INFINITY, scalar(Float.NEGATIVE_INFINITY, typeOf<Double>()))
        val registration = Kronos.registerValueCodec(valueCodec(supports = { _, c -> c.targetType == typeOf<Float>() },
            convert = { _, _ -> Float.POSITIVE_INFINITY }))
        try { assertEquals(Float.POSITIVE_INFINITY, scalar(Double.MAX_VALUE, typeOf<Float>())) }
        finally { registration.close() }
    }

    private fun scalar(value: Any, type: KType, direction: ValueCodecDirection = ValueCodecDirection.DECODE,
        origin: ValueCodecOrigin = ValueCodecOrigin.DATABASE, strict: Boolean = false): Any? =
        ValueCodecRegistry.convert(ValueConversionRequest(value, direction, origin, type, strict = strict,
            physicalTargetType = if (direction == ValueCodecDirection.ENCODE) type else null))
}
