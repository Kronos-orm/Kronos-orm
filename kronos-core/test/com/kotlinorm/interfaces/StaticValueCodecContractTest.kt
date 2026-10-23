package com.kotlinorm.interfaces

import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.utils.StaticTypeRef
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant

class StaticValueCodecContractTest {
    private enum class State { OPEN, CLOSED }

    @Test
    fun scalarEncodingPreservesEveryBindableRepresentation() {
        val cases = listOf(
            true to KronosPhysicalValue.Integer(1), false to KronosPhysicalValue.Integer(0),
            7.toByte() to KronosPhysicalValue.Integer(7), 8.toShort() to KronosPhysicalValue.Integer(8),
            9 to KronosPhysicalValue.Integer(9), 10L to KronosPhysicalValue.Integer(10),
            1.5f to KronosPhysicalValue.Real(1.5), 2.5 to KronosPhysicalValue.Real(2.5),
            'x' to KronosPhysicalValue.Text("x"), "text" to KronosPhysicalValue.Text("text")
        )
        cases.forEach { (logical, physical) -> assertEquals(physical, KronosValueCodecs.encode(logical, encode("kotlin.Any"))) }
        val prepared = KronosPhysicalValue.Text("already physical")
        assertSame(prepared, KronosValueCodecs.encode(prepared, encode("kotlin.String")))
        assertEquals(KronosPhysicalValue.Null, KronosValueCodecs.encode(null, encode("kotlin.String", true)))
        assertFailsWith<StaticValueCodecException> { KronosValueCodecs.encode(null, encode("kotlin.String")) }
        assertFailsWith<StaticValueCodecException> { KronosValueCodecs.encode(listOf(1), encode("kotlin.collections.List")) }
    }

    @Test
    fun booleanTextAndNumericDecodingHaveExplicitAcceptedValues() {
        listOf("1", "true", "TRUE", " true ").forEach { text ->
            assertEquals(true, decode(KronosPhysicalValue.Text(text), "kotlin.Boolean"))
        }
        listOf("0", "", "false", "FALSE", " false ").forEach { text ->
            assertEquals(false, decode(KronosPhysicalValue.Text(text), "kotlin.Boolean"))
        }
        assertEquals(false, decode(KronosPhysicalValue.Integer(0), "kotlin.Boolean"))
        assertEquals(true, decode(KronosPhysicalValue.Real(-0.5), "kotlin.Boolean"))
        assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Text("yes"), "kotlin.Boolean") }
        assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Blob(byteArrayOf(1)), "kotlin.Boolean") }
    }

    @Test
    fun numericDecodingChecksIntegralAndNarrowingBoundsInsteadOfTruncating() {
        assertEquals(Byte.MIN_VALUE, decode(KronosPhysicalValue.Integer(Byte.MIN_VALUE.toLong()), "kotlin.Byte"))
        assertEquals(Byte.MAX_VALUE, decode(KronosPhysicalValue.Text(Byte.MAX_VALUE.toString()), "kotlin.Byte"))
        assertEquals(Short.MIN_VALUE, decode(KronosPhysicalValue.Integer(Short.MIN_VALUE.toLong()), "kotlin.Short"))
        assertEquals(Short.MAX_VALUE, decode(KronosPhysicalValue.Integer(Short.MAX_VALUE.toLong()), "kotlin.Short"))
        assertEquals(Int.MIN_VALUE, decode(KronosPhysicalValue.Integer(Int.MIN_VALUE.toLong()), "kotlin.Int"))
        assertEquals(Int.MAX_VALUE, decode(KronosPhysicalValue.Integer(Int.MAX_VALUE.toLong()), "kotlin.Int"))
        listOf("kotlin.Byte" to 128L, "kotlin.Byte" to -129L, "kotlin.Short" to 32768L,
            "kotlin.Short" to -32769L, "kotlin.Int" to 2147483648L, "kotlin.Int" to -2147483649L).forEach { (type, value) ->
            assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Integer(value), type) }
        }
        assertEquals(42L, decode(KronosPhysicalValue.Real(42.0), "kotlin.Long"))
        assertEquals(Long.MIN_VALUE, decode(KronosPhysicalValue.Real(Long.MIN_VALUE.toDouble()), "kotlin.Long"))
        assertEquals(Long.MAX_VALUE - 1023L, decode(KronosPhysicalValue.Real(Long.MAX_VALUE.toDouble() - 1024.0), "kotlin.Long"))
        listOf(1.5, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Long.MAX_VALUE.toDouble(), Long.MIN_VALUE.toDouble() - 2048.0).forEach { value ->
            assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Real(value), "kotlin.Long") }
        }
        assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Text("not a number"), "kotlin.Long") }
        assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Blob(byteArrayOf()), "kotlin.Long") }
        assertEquals(2.5f, decode(KronosPhysicalValue.Text("2.5"), "kotlin.Float"))
        assertEquals(3.5, decode(KronosPhysicalValue.Text("3.5"), "kotlin.Double"))
        assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Text("not numeric"), "kotlin.Double") }
        assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Blob(byteArrayOf()), "kotlin.Double") }
    }

    @Test
    fun characterBlobAndUnknownTypesRejectIncompatiblePhysicalStorage() {
        assertEquals('Q', decode(KronosPhysicalValue.Text("Q"), "kotlin.Char"))
        assertEquals('A', decode(KronosPhysicalValue.Integer(65), "kotlin.Char"))
        assertEquals("42", decode(KronosPhysicalValue.Integer(42), "kotlin.String"))
        listOf("", "multiple").forEach { value ->
            assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Text(value), "kotlin.Char") }
        }
        assertContentEquals(byteArrayOf(0, 127), decode(KronosPhysicalValue.Blob(byteArrayOf(0, 127)), "kotlin.ByteArray") as ByteArray)
        assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Text("bytes"), "kotlin.ByteArray") }
        val unknown = assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Text("value"), "example.Unknown") }
        assertEquals("no scalar codec for example.Unknown", unknown.detail)
        val strict = assertFailsWith<StaticValueCodecException> {
            KronosValueCodecs.decode(KronosPhysicalValue.Text("42"), context("kotlin.Int"), null, allowBuiltInCoercion = false)
        }
        assertEquals("strict decode requires an assignable value or numeric normalization", strict.detail)
    }

    @Test
    fun serializedNullAndMissingCodecFailuresRespectDirectionAndNullability() {
        val encodeContext = encode("example.Json", nullable = true).copy(storage = ValueStorage.SERIALIZED)
        val decode = context("example.Json", nullable = true).copy(storage = ValueStorage.SERIALIZED)
        assertFailsWith<StaticValueCodecException> { KronosValueCodecs.encode("payload", encodeContext) }
        assertFailsWith<StaticValueCodecException> { KronosValueCodecs.decode(KronosPhysicalValue.Text("{}"), decode) }
        val registration = KronosValueCodecs.register(staticValueCodec(
            supports = { _, c -> c.targetType.key == "example.Json" }, convert = { _, _ -> null }))
        try {
            assertEquals(KronosPhysicalValue.Null, KronosValueCodecs.encode("payload", encodeContext))
            assertNull(KronosValueCodecs.decode(KronosPhysicalValue.Text("{}"), decode))
            assertFailsWith<StaticValueCodecException> {
                KronosValueCodecs.decode(KronosPhysicalValue.Text("{}"), decode.copy(targetType = StaticTypeRef("example.Json", false)))
            }
        } finally { registration.close() }
        assertFailsWith<IllegalArgumentException> { KronosValueCodecs.encode(1, context("kotlin.Int")) }
        assertFailsWith<IllegalArgumentException> { KronosValueCodecs.decode(KronosPhysicalValue.Integer(1), encode("kotlin.Int")) }
        assertFailsWith<IllegalArgumentException> {
            context("example.Json").copy(field = field(KColumnType.TEXT, serializable = true), storage = ValueStorage.NONE)
        }
    }

    @Test
    fun enumFallbackHonorsRegisteredOverrideAndRejectsCorruptMetadata() {
        val type = StaticTypeRef("example.ContractState", false)
        val info = StaticTypeInfo(type, enumKey = type.key)
        val fallback = enumCodec(type)
        val context = context(type.key).copy(targetTypeInfo = info)
        assertEquals(State.OPEN, KronosValueCodecs.decode(KronosPhysicalValue.Text("OPEN"), context, fallback))
        assertFailsWith<StaticValueCodecException> { KronosValueCodecs.decode(KronosPhysicalValue.Text("OPEN"), context) }
        assertFailsWith<StaticValueCodecException> { KronosValueCodecs.decode(KronosPhysicalValue.Integer(0), context, fallback) }
        val ordinal = context.copy(enumStorage = StaticEnumStorage.ORDINAL)
        assertEquals(State.CLOSED, KronosValueCodecs.decode(KronosPhysicalValue.Text("1"), ordinal, fallback))
        listOf(-1L, 2L).forEach { value ->
            assertFailsWith<StaticValueCodecException> { KronosValueCodecs.decode(KronosPhysicalValue.Integer(value), ordinal, fallback) }
        }
        val invalidResult = enumCodec(type) { State.CLOSED }
        assertFailsWith<StaticValueCodecException> { KronosValueCodecs.decode(KronosPhysicalValue.Text("OPEN"), context, invalidResult) }
        val registered = KronosValueCodecs.registerEnum(fallback)
        try {
            assertEquals(State.OPEN, KronosValueCodecs.decode(KronosPhysicalValue.Text("OPEN"), context, invalidResult))
        } finally { registered.close(); registered.close() }
        assertFailsWith<IllegalArgumentException> { KronosValueCodecs.registerEnum(enumCodec(type, names = emptyList())) }
        assertFailsWith<IllegalArgumentException> { KronosValueCodecs.registerEnum(enumCodec(type, names = listOf("OPEN", "OPEN"))) }
    }

    @Test
    fun integralFieldFamiliesSelectOrdinalEnumStorage() {
        listOf(KColumnType.TINYINT, KColumnType.SMALLINT, KColumnType.INT, KColumnType.MEDIUMINT,
            KColumnType.SERIAL, KColumnType.BIGINT).forEach { physicalType ->
            val c = StaticValueCodecContext(ValueCodecDirection.ENCODE, ValueCodecOrigin.PARAMETER,
                targetType = StaticTypeRef("example.State", false), field = field(physicalType))
            assertEquals(KronosPhysicalValue.Integer(1), KronosValueCodecs.encode(State.CLOSED, c))
        }
        val text = StaticValueCodecContext(ValueCodecDirection.ENCODE, ValueCodecOrigin.PARAMETER,
            targetType = StaticTypeRef("example.State", false), field = field(KColumnType.TEXT))
        assertEquals(KronosPhysicalValue.Text("CLOSED"), KronosValueCodecs.encode(State.CLOSED, text))
    }

    @Test
    fun callbackFailuresRetainContextWhileCancellationAndErrorsPassThrough() {
        val failures = listOf(IllegalStateException("callback failed"), CancellationException("cancelled"), AssertionError("fatal"),
            StaticValueCodecException(ValueCodecDirection.DECODE, ValueCodecOrigin.MAP, StaticTypeRef("kotlin.String", false), null, null, "original"))
        for (inSupports in listOf(true, false)) for (cause in failures) {
            val codec = object : StaticValueCodec {
                override fun supports(value: Any, context: StaticValueCodecContext): Boolean {
                    if (inSupports) {
                        throw cause
                    }
                    return true
                }
                override fun convert(value: Any, context: StaticValueCodecContext): Any? = throw cause
            }
            val registration = KronosValueCodecs.register(codec)
            try {
                val actual = assertFailsWith<Throwable> { decode(KronosPhysicalValue.Text("input"), "kotlin.String") }
                if (cause is CancellationException) {
                    assertSame(cause, actual)
                } else if (cause is IllegalStateException) {
                    assertTrue(actual is StaticValueCodecException)
                    assertSame(cause, actual.cause)
                    assertEquals("user StaticValueCodec failed in ${if (inSupports) "supports" else "convert"}", actual.detail)
                } else assertSame(cause, actual)
            } finally { registration.close() }
        }
    }

    @Test
    fun temporalNumericFallbackPreservesMillisecondsAndRejectsFractions() {
        val instant = Instant.fromEpochMilliseconds(1000)
        assertEquals(instant, decode(KronosPhysicalValue.Real(1000.0), "kotlin.time.Instant"))
        assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Real(1000.5), "kotlin.time.Instant") }
        assertFailsWith<StaticValueCodecException> { decode(KronosPhysicalValue.Blob(byteArrayOf(1)), "kotlin.time.Instant") }
    }

    @Test
    fun scalarCompatibilityInputsNormalizeWithoutReflectionOrTextRoundTrips() {
        assertEquals(true, true.toLogicalScalar(context("kotlin.Boolean")))
        for (value in listOf(42.toByte(), 42.toShort(), 42, 42L, 42.0f, 42.0)) {
            assertEquals(42L, value.toLogicalScalar(context("kotlin.Long")))
            assertEquals(42.0, value.toLogicalScalar(context("kotlin.Double")))
        }
        assertEquals('x', 'x'.toLogicalScalar(context("kotlin.Char")))
        for (value in listOf(1000, 1000L, 1000.0f, 1000.0)) {
            assertEquals(Instant.fromEpochMilliseconds(1000), value.toLogicalScalar(context("kotlin.time.Instant")))
        }
        val instantContext = context("kotlin.time.Instant").copy(timeZone = "UTC")
        assertEquals(Instant.fromEpochMilliseconds(1000), "1970-01-01T00:00:01Z".toLogicalScalar(instantContext))
        assertEquals(Instant.fromEpochMilliseconds(1000), "1970-01-01T00:00:01".toLogicalScalar(instantContext))
        val invalid = assertFailsWith<StaticValueCodecException> { "invalid instant".toLogicalScalar(instantContext) }
        assertEquals("cannot decode 'invalid instant' as kotlin.time.Instant", invalid.detail)
        assertEquals(1, invalid.suppressedExceptions.size)
        assertEquals(KronosPhysicalValue.Text("1970-01-01T00:00:01Z"),
            KronosValueCodecs.encode(Instant.fromEpochMilliseconds(1000), encode("kotlin.time.Instant")))
        val bytes = byteArrayOf(1, 2, 3)
        assertEquals(KronosPhysicalValue.Blob(bytes), KronosValueCodecs.encode(bytes, encode("kotlin.ByteArray")))
    }

    @Test
    fun serializedTextCodecClaimsOnlyTextReadsAndSerializedFields() {
        val codec = serializedTextValueCodec(encode = { value, type -> "${type.key}:$value" },
            decode = { text, type -> listOf(type.key, text) })
        val serializedField = field(KColumnType.TEXT, serializable = true)
        val decode = StaticValueCodecContext(ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE,
            targetType = StaticTypeRef("example.Json", false), field = serializedField)
        assertEquals(ValueStorage.SERIALIZED, decode.storage)
        val encodeContext = decode.copy(direction = ValueCodecDirection.ENCODE)
        val handle = KronosValueCodecs.register(codec)
        try {
            assertEquals(KronosPhysicalValue.Text("example.Json:payload"), KronosValueCodecs.encode("payload", encodeContext))
            assertEquals(listOf("example.Json", "payload"), KronosValueCodecs.decode(KronosPhysicalValue.Text("payload"), decode))
            assertFailsWith<StaticValueCodecException> { KronosValueCodecs.decode(KronosPhysicalValue.Integer(1), decode) }
            assertFailsWith<StaticValueCodecException> { KronosValueCodecs.decode(KronosPhysicalValue.Text("payload"),
                decode.copy(origin = ValueCodecOrigin.PARAMETER)) }
        } finally { handle.close() }
        val invalid = KronosValueCodecs.register(staticValueCodec(supports = { _, _ -> true }, convert = { _, _ -> 42 }))
        try {
            assertFailsWith<StaticValueCodecException> { KronosValueCodecs.encode("payload", encodeContext) }
        } finally { invalid.close() }
        assertEquals(KronosPhysicalValue.Null, KronosValueCodecs.encode(null, encode("kotlin.String", true)))
        assertNull(KronosValueCodecs.decode(KronosPhysicalValue.Null, context("kotlin.String", true)))
        assertFailsWith<StaticValueCodecException> { KronosValueCodecs.decode(KronosPhysicalValue.Null, context("kotlin.String")) }
    }

    private fun context(key: String, nullable: Boolean = false) = StaticValueCodecContext(
        ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE, targetType = StaticTypeRef(key, nullable))
    private fun encode(key: String, nullable: Boolean = false) = context(key, nullable).copy(direction = ValueCodecDirection.ENCODE)
    private fun decode(value: KronosPhysicalValue, key: String): Any? = KronosValueCodecs.decode(value, context(key))
    private fun enumCodec(ref: StaticTypeRef, names: List<String> = State.entries.map { it.name },
        decoder: (String) -> Enum<*>? = { name -> State.entries.firstOrNull { it.name == name } }) = object : StaticEnumValueCodec {
        override val type = ref
        override val entryNames = names
        override fun decode(name: String): Enum<*>? = decoder(name)
    }
    private fun field(physicalType: KColumnType, serializable: Boolean = false) = object : ValueCodecField {
        override val columnName = "state_value"
        override val name = "state"
        override val type = physicalType
        override val dateFormat: String? = null
        override val nullable = false
        override val serializable = serializable
        override val typeInfo: StaticTypeInfo? = null
    }
}
