package com.kotlinorm.interfaces

import com.kotlinorm.Kronos
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.codec.ValueConversionRequest
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ValueCodecPlatformParityTest {
    @Test
    fun compatibleMapValuesStillRunTheNewestUserCodecOnce() {
        var matches = 0
        var conversions = 0
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, context ->
                matches++
                context.origin == ValueCodecOrigin.MAP && context.targetType == typeOf<String>()
            },
            convert = { value, _ -> conversions++; (value as String).trim() }
        ))
        try {
            assertEquals("Ada", ValueCodecRegistry.convert(ValueConversionRequest(
                " Ada ", ValueCodecDirection.DECODE, ValueCodecOrigin.MAP, typeOf<String>())))
            assertEquals(1, matches)
            assertEquals(1, conversions)
            assertEquals(42, ValueCodecRegistry.convert(ValueConversionRequest(
                42L, ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE, typeOf<Int>())))
            assertEquals(2, matches)
            assertEquals(1, conversions)
        } finally { registration.close() }
    }

    @Test
    fun alreadyLogicalSerializedMapValuesAreNotDecodedAgain() {
        var calls = 0
        val registration = Kronos.registerValueCodec(serializedValueCodec(
            encode = { value, _ -> value.toString() },
            decode = { text, _ -> calls++; "decoded:$text" }
        ))
        try {
            val request = ValueConversionRequest("logical", ValueCodecDirection.DECODE,
                ValueCodecOrigin.MAP, typeOf<String>(), storage = ValueStorage.SERIALIZED)
            assertEquals("logical", ValueCodecRegistry.convert(request))
            assertEquals("decoded:logical", ValueCodecRegistry.convert(request.copy(origin = ValueCodecOrigin.DATABASE)))
            assertEquals(1, calls)
        } finally { registration.close() }
    }

    @Test
    fun sqliteCurrentTimestampTextUsesTheConfiguredZone() {
        val request = ValueConversionRequest("2026-10-27 09:30:15", ValueCodecDirection.DECODE,
            ValueCodecOrigin.DATABASE, typeOf<Instant>(), timeZone = "Asia/Shanghai")
        assertEquals(Instant.parse("2026-10-27T01:30:15Z"), ValueCodecRegistry.convert(request))
    }

    @Test
    fun fieldPatternAndExplicitOverrideRoundTripTextStorage() {
        val instant = Instant.parse("2026-10-27T01:30:15.123456789Z")
        val field = Field("created_at", "createdAt", KColumnType.VARCHAR,
            dateFormat = "dd/MM/yyyy HH:mm:ss.SSSSSSSSS", kType = typeOf<Instant>())
        val request = ValueConversionRequest(instant, ValueCodecDirection.ENCODE,
            ValueCodecOrigin.PARAMETER, typeOf<Instant>(), field = field,
            physicalTargetType = typeOf<String>(), timeZone = "Asia/Shanghai")
        val text = "27/10/2026 09:30:15.123456789"
        assertEquals(text, ValueCodecRegistry.convert(request))
        assertEquals(instant, ValueCodecRegistry.convert(request.copy(value = text,
            direction = ValueCodecDirection.DECODE, origin = ValueCodecOrigin.DATABASE, physicalTargetType = null)))
        val override = "yyyyMMdd HH:mm:ss.SSSSSSSSS"
        val overrideText = "20261027 09:30:15.123456789"
        assertEquals(overrideText, ValueCodecRegistry.convert(request.copy(dateFormat = override)))
        assertEquals(instant, ValueCodecRegistry.convert(request.copy(value = overrideText,
            direction = ValueCodecDirection.DECODE, origin = ValueCodecOrigin.DATABASE,
            physicalTargetType = null, dateFormat = override)))
    }

    @Test
    fun temporalStrategyValuesUseTheStringFieldsPattern() {
        val instant = Instant.parse("2026-10-27T01:30:15Z")
        val field = Field("created_at", "createdAt", KColumnType.VARCHAR,
            dateFormat = "dd/MM/yyyy HH:mm:ss", kType = typeOf<String>())
        assertEquals("27/10/2026 09:30:15", ValueCodecRegistry.convert(ValueConversionRequest(
            instant, ValueCodecDirection.ENCODE, ValueCodecOrigin.PARAMETER, typeOf<String>(),
            sourceType = typeOf<Instant>(), field = field, timeZone = "Asia/Shanghai")))
    }

    @Test
    fun globalPatternIsAParsingFallbackWhileIsoPreservesNanoseconds() {
        val original = Kronos.defaultDateFormat
        try {
            Kronos.defaultDateFormat = "dd.MM.yyyy HH:mm:ss"
            val request = ValueConversionRequest("27.10.2026 01:30:15", ValueCodecDirection.DECODE,
                ValueCodecOrigin.DATABASE, typeOf<Instant>(), timeZone = "UTC")
            assertEquals(Instant.parse("2026-10-27T01:30:15Z"), ValueCodecRegistry.convert(request))
            val precise = Instant.parse("2026-10-27T01:30:15.123456789Z")
            assertEquals(precise, ValueCodecRegistry.convert(request.copy(value = precise.toString())))
        } finally { Kronos.defaultDateFormat = original }
    }

    @Test
    fun generatedKeyRequestsUsePhysicalNamesForActionsAndBatches() {
        val field = Field("user_id", "id")
        val action = KronosAtomicActionTask("INSERT INTO person(name) VALUES (:name)",
            mapOf("name" to "Ada"), generatedKeyField = field)
        val batch = KronosAtomicBatchTask("INSERT INTO person(name) VALUES (:name)",
            paramMapArr = arrayOf(mapOf("name" to "Ada")), generatedKeyField = field)
        assertEquals("user_id", action.toKronosActionExecutionPlan().generatedKey?.fieldName)
        assertEquals("user_id", batch.toKronosBatchExecutionPlan().generatedKey?.fieldName)
    }
}
