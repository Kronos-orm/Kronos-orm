package com.kotlinorm.interfaces

import com.kotlinorm.Kronos
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.utils.StaticTypeRef
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.codec.ValueConversionRequest
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

class ValueCodecDispatchMatrixTest {
    @Test
    fun originsAndStorageShareExactlyOneUserDispatchExceptSerializedMapIdentity() {
        var matches = 0
        var conversions = 0
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, _ -> matches++; true },
            convert = { value, _ -> conversions++; "converted:$value" }))
        try {
            for (direction in ValueCodecDirection.entries) for (origin in ValueCodecOrigin.entries) {
                for (storage in ValueStorage.entries) {
                    val previousMatches = matches
                    val previousConversions = conversions
                    val identity = direction == ValueCodecDirection.DECODE && origin == ValueCodecOrigin.MAP &&
                        storage == ValueStorage.SERIALIZED
                    assertEquals(if (identity) "raw" else "converted:raw", ValueCodecRegistry.convert(
                        ValueConversionRequest("raw", direction, origin, typeOf<String>(), storage = storage)))
                    assertEquals(previousMatches + if (identity) 0 else 1, matches)
                    assertEquals(previousConversions + if (identity) 0 else 1, conversions)
                }
            }
        } finally { registration.close() }
    }

    @Test
    fun unmatchedUserCodecsAreNotRetriedByEncodeOrDecodeFallbacks() {
        for (direction in ValueCodecDirection.entries) for (origin in ValueCodecOrigin.entries) {
            var matches = 0
            var conversions = 0
            val registration = Kronos.registerValueCodec(valueCodec(
                supports = { _, _ -> matches++; false },
                convert = { _, _ -> conversions++; error("Unmatched codec must not run") }))
            try {
                val input: Any = if (direction == ValueCodecDirection.ENCODE) "7" else 7L
                assertEquals(7, ValueCodecRegistry.convert(ValueConversionRequest(input, direction, origin,
                    typeOf<Int>(), strict = false, physicalTargetType = if (direction == ValueCodecDirection.ENCODE) typeOf<Int>() else null)))
                assertEquals(1, matches)
                assertEquals(0, conversions)
            } finally { registration.close() }
        }
    }

    @Test
    fun builtInCoercionCannotSelectAUserCodecForTheSecondTime() {
        for (direction in ValueCodecDirection.entries) {
            var matches = 0
            var conversions = 0
            val registration = Kronos.registerValueCodec(valueCodec(
                supports = { _, _ -> ++matches == 2 },
                convert = { _, _ -> conversions++; 99 }))
            try {
                val input: Any = if (direction == ValueCodecDirection.ENCODE) "7" else 7L
                assertEquals(7, ValueCodecRegistry.convert(ValueConversionRequest(input, direction,
                    ValueCodecOrigin.DATABASE, typeOf<Int>(), strict = false,
                    physicalTargetType = if (direction == ValueCodecDirection.ENCODE) typeOf<Int>() else null)))
                assertEquals(1, matches)
                assertEquals(0, conversions)
            } finally { registration.close() }
        }
    }

    @Test
    fun nullHandlingAlwaysPrecedesUserCallbacksAndRespectsTheLogicalBound() {
        var matches = 0
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, _ -> matches++; true }, convert = { _, _ -> "unexpected" }))
        try {
            for (direction in ValueCodecDirection.entries) for (origin in ValueCodecOrigin.entries) {
                for (storage in ValueStorage.entries) {
                    val nullable = ValueConversionRequest(null, direction, origin, typeOf<String?>(), storage = storage)
                    assertNull(ValueCodecRegistry.convert(nullable))
                    assertFailsWith<ValueMappingException> {
                        ValueCodecRegistry.convert(nullable.copy(targetType = typeOf<String>()))
                    }
                }
            }
            assertEquals(0, matches)
        } finally { registration.close() }
    }

    @Test
    fun declaredGenericEncodeMismatchNeverReachesUserCodeAndDecodeChecksEveryElement() {
        var conversions = 0
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, _ -> true },
            convert = { _, context -> conversions++; if (context.direction == ValueCodecDirection.ENCODE) "encoded" else listOf("wrong") }))
        try {
            assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest(listOf("wrong"), ValueCodecDirection.ENCODE,
                    ValueCodecOrigin.PARAMETER, typeOf<List<Int>>(), sourceType = typeOf<List<String>>(),
                    storage = ValueStorage.SERIALIZED))
            }
            assertEquals(0, conversions)
            assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest("encoded", ValueCodecDirection.DECODE,
                    ValueCodecOrigin.DATABASE, typeOf<List<Int>>(), storage = ValueStorage.SERIALIZED))
            }
            assertEquals(1, conversions)
        } finally { registration.close() }
    }

    @Test
    fun strictNumericNormalizationBelongsOnlyToDatabaseReads() {
        for (origin in ValueCodecOrigin.entries) {
            val request = ValueConversionRequest(7L, ValueCodecDirection.DECODE, origin, typeOf<Int>(), strict = true)
            if (origin == ValueCodecOrigin.DATABASE) assertEquals(7, ValueCodecRegistry.convert(request))
            else assertFailsWith<ValueMappingException> { ValueCodecRegistry.convert(request) }
        }
    }

    @Test
    fun portableDateTimeTypesShareCustomPatternsAndNanoseconds() {
        val cases = listOf(
            Triple("kotlinx.datetime.LocalDate", "27/10/2026", "dd/MM/yyyy") to LocalDate(2026, 10, 27),
            Triple("kotlinx.datetime.LocalTime", "09:30:15.123456789", "HH:mm:ss.SSSSSSSSS") to LocalTime(9, 30, 15, 123456789),
            Triple("kotlinx.datetime.LocalDateTime", "27/10/2026 09:30:15.123456789", "dd/MM/yyyy HH:mm:ss.SSSSSSSSS") to
                LocalDateTime(2026, 10, 27, 9, 30, 15, 123456789)
        )
        for ((format, expected) in cases) {
            val context = StaticValueCodecContext(ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE,
                targetType = StaticTypeRef(format.first, false), dateFormat = format.third, timeZone = "UTC")
            assertEquals(expected, KronosValueCodecs.decode(KronosPhysicalValue.Text(format.second), context))
            assertEquals(KronosPhysicalValue.Text(format.second), KronosValueCodecs.encode(expected,
                context.copy(direction = ValueCodecDirection.ENCODE)))
        }
        val request = ValueConversionRequest("27/10/2026 09:30:15", ValueCodecDirection.DECODE,
            ValueCodecOrigin.DATABASE, typeOf<LocalDateTime>(), dateFormat = "dd/MM/yyyy HH:mm:ss")
        assertEquals(LocalDateTime(2026, 10, 27, 9, 30, 15), ValueCodecRegistry.convert(request))
    }
}
