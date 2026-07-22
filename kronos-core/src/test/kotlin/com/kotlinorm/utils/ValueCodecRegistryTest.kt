/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.utils

import com.google.gson.Gson
import com.kotlinorm.utils.codec.TemporalTargetKind
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.codec.ValueConversionRequest
import com.kotlinorm.utils.codec.isConcreteEnumType
import com.kotlinorm.utils.codec.temporalTargetKind
import com.kotlinorm.Kronos
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.ValueCodecDirection.DECODE
import com.kotlinorm.enums.ValueCodecDirection.ENCODE
import com.kotlinorm.enums.ValueCodecOrigin.DATABASE
import com.kotlinorm.enums.ValueCodecOrigin.DELEGATE
import com.kotlinorm.enums.ValueCodecOrigin.MAP
import com.kotlinorm.enums.ValueCodecOrigin.PARAMETER
import com.kotlinorm.enums.ValueStorage.NONE
import com.kotlinorm.enums.ValueStorage.SERIALIZED
import com.kotlinorm.exceptions.ConflictingEnumMetadata
import com.kotlinorm.exceptions.ConflictingGeneratedProvider
import com.kotlinorm.exceptions.InvalidEnumOrdinal
import com.kotlinorm.exceptions.MissingEnumMetadata
import com.kotlinorm.exceptions.MissingSerializedCodec
import com.kotlinorm.exceptions.UnknownEnumValue
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.serializedValueCodec
import com.kotlinorm.interfaces.valueCodec
import com.kotlinorm.syntax.render.SqlDialect
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.concurrent.thread

class ValueCodecRegistryTest {
    private class NameOnlyKType(private val displayName: String) : KType {
        override val classifier: KClassifier? = null
        override val arguments: List<KTypeProjection> = emptyList()
        override val isMarkedNullable: Boolean = displayName.endsWith('?')
        override val annotations: List<Annotation> = emptyList()

        override fun toString(): String = displayName
    }

    private class CustomDate(millis: Long) : java.util.Date(millis)

    private class CustomSqlDate(millis: Long) : Date(millis)

    private class CustomSqlTime(millis: Long) : Time(millis)

    private class CustomSqlTimestamp(millis: Long) : Timestamp(millis)

    private enum class Status {
        READY,
        CLOSED
    }

    private enum class OtherStatus {
        READY
    }

    internal interface IndirectKPojo : KPojo

    internal data class IndirectEntity(var id: Int? = null) : IndirectKPojo

    @Test
    fun `temporal target resolution uses complete KType and limits name fallback`() {
        assertEquals(TemporalTargetKind.LOCAL_DATE_TIME, typeOf<LocalDateTime>().temporalTargetKind())
        assertEquals(TemporalTargetKind.LOCAL_DATE_TIME, typeOf<LocalDateTime?>().temporalTargetKind())
        assertNull(typeOf<CustomDate>().temporalTargetKind())
        assertEquals(
            TemporalTargetKind.LOCAL_DATE_TIME,
            NameOnlyKType("java.time.LocalDateTime?").temporalTargetKind()
        )
        assertNull(NameOnlyKType("example.UnknownTemporal").temporalTargetKind())
    }

    @Test
    fun `enum recognition uses complete KType subtype semantics`() {
        assertTrue(typeOf<Status>().isConcreteEnumType())
        assertTrue(typeOf<Status?>().isConcreteEnumType())
        assertFalse(typeOf<List<Status>>().isConcreteEnumType())
        assertFalse(NameOnlyKType("com.example.Status").isConcreteEnumType())
    }

    @Test
    fun `temporal dispatch uses KType classifier and JDBC superclass relationships`() {
        val local = LocalDateTime.of(2026, 7, 20, 12, 34, 56)
        val timestamp = CustomSqlTimestamp(Timestamp.valueOf(local).time)
        val sqlDate = CustomSqlDate(Date.valueOf(local.toLocalDate()).time)
        val sqlTime = CustomSqlTime(Time.valueOf(local.toLocalTime()).time)
        val zone = ZoneId.of("UTC")

        assertEquals(
            local,
            ValueCodecRegistry.convert(ValueConversionRequest(
                timestamp,
                DECODE,
                DATABASE,
                typeOf<LocalDateTime?>(),
                sourceType = typeOf<CustomSqlTimestamp>(),
                timeZone = zone
            ))
        )
        assertEquals(
            local.toLocalDate(),
            ValueCodecRegistry.convert(ValueConversionRequest(
                sqlDate,
                DECODE,
                DATABASE,
                typeOf<java.time.LocalDate>(),
                sourceType = typeOf<CustomSqlDate>(),
                timeZone = zone
            ))
        )
        assertEquals(
            local.toLocalTime(),
            ValueCodecRegistry.convert(ValueConversionRequest(
                sqlTime,
                DECODE,
                DATABASE,
                typeOf<java.time.LocalTime>(),
                sourceType = typeOf<CustomSqlTime>(),
                timeZone = zone
            ))
        )
        assertEquals(
            Instant.ofEpochMilli(timestamp.time),
            ValueCodecRegistry.convert(ValueConversionRequest(
                timestamp,
                DECODE,
                DATABASE,
                typeOf<Instant>(),
                sourceType = typeOf<CustomSqlTimestamp>(),
                timeZone = zone
            ))
        )
    }

    @Test
    fun `unsupported Date subclasses are not converted to an unassignable base value`() {
        val source = java.util.Date(0)
        val failure = assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(ValueConversionRequest(
                source,
                DECODE,
                DATABASE,
                typeOf<CustomDate>(),
                timeZone = ZoneId.of("UTC")
            ))
        }

        assertEquals(typeOf<CustomDate>(), failure.targetType)
    }

    @Test
    fun `basic enum and identity codecs retain declared complete KTypes`() {
        assertEquals(
            42,
            ValueCodecRegistry.convert(ValueConversionRequest(
                "42",
                DECODE,
                MAP,
                typeOf<Int?>(),
                sourceType = typeOf<String>()
            ))
        )
        assertEquals(
            "READY",
            ValueCodecRegistry.convert(ValueConversionRequest(
                Status.READY,
                ENCODE,
                PARAMETER,
                typeOf<Status?>(),
                sourceType = typeOf<Status>()
            ))
        )

        val values = listOf(Status.READY)
        assertSame(
            values,
            ValueCodecRegistry.convert(ValueConversionRequest(
                values,
                DECODE,
                MAP,
                typeOf<List<Status>>(),
                sourceType = typeOf<List<Status>>()
            ))
        )
        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(ValueConversionRequest(
                values,
                DECODE,
                MAP,
                typeOf<List<Status>>(),
                sourceType = typeOf<List<OtherStatus>>()
            ))
        }
    }

    @Test
    fun `last registered user codec wins and close restores the immutable snapshot`() {
        val older = Kronos.registerValueCodec(valueCodec(
            supports = { _, context -> context.targetType == typeOf<String>() },
            convert = { _, _ -> "older" }
        ))
        lateinit var latest: AutoCloseable
        latest = Kronos.registerValueCodec(valueCodec(
            supports = { _, context ->
                latest.close()
                context.targetType == typeOf<String>()
            },
            convert = { _, _ -> "latest" }
        ))

        try {
            assertEquals("latest", ValueCodecRegistry.convert(decodeRequest(1, typeOf<String>())))
            assertEquals("older", ValueCodecRegistry.convert(decodeRequest(1, typeOf<String>())))
            latest.close()
        } finally {
            latest.close()
            older.close()
        }
    }

    @Test
    fun `in flight conversion retains its entry snapshot while registrations change`() {
        val enteredSupports = CountDownLatch(1)
        val continueSupports = CountDownLatch(1)
        val result = AtomicReference<Any?>()
        val failure = AtomicReference<Throwable?>()
        val older = Kronos.registerValueCodec(valueCodec(
            supports = { _, context -> context.targetType == typeOf<String>() },
            convert = { _, _ -> "older" }
        ))
        val blocking = Kronos.registerValueCodec(valueCodec(
            supports = { _, _ ->
                enteredSupports.countDown()
                continueSupports.await()
                false
            },
            convert = { _, _ -> error("blocking codec must not convert") }
        ))
        var late: AutoCloseable? = null
        try {
            val worker = thread(start = true) {
                try {
                    result.set(ValueCodecRegistry.convert(decodeRequest(1, typeOf<String>())))
                } catch (cause: Throwable) {
                    failure.set(cause)
                }
            }
            assertTrue(enteredSupports.await(5, TimeUnit.SECONDS))
            late = Kronos.registerValueCodec(valueCodec(
                supports = { _, context -> context.targetType == typeOf<String>() },
                convert = { _, _ -> "late" }
            ))
            older.close()
            continueSupports.countDown()
            worker.join(5_000)

            assertFalse(worker.isAlive)
            assertNull(failure.get())
            assertEquals("older", result.get())
            assertEquals("late", ValueCodecRegistry.convert(decodeRequest(1, typeOf<String>())))
        } finally {
            continueSupports.countDown()
            late?.close()
            blocking.close()
            older.close()
        }
    }

    @Test
    fun `null handling runs before codecs and enforces target nullability`() {
        var calls = 0
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, _ -> true },
            convert = { _, _ -> calls += 1 }
        ))
        try {
            assertNull(ValueCodecRegistry.convert(decodeRequest(null, typeOf<String?>())))
            val failure = assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(decodeRequest(null, typeOf<String>()))
            }
            assertEquals(typeOf<String>(), failure.targetType)
            assertEquals(0, calls)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `serialized map String is identity while database and delegate String are decoded`() {
        var decodeCalls = 0
        val registration = Kronos.registerValueCodec(serializedValueCodec(
            encode = { value, _ -> value.toString() },
            decode = { text, type ->
                decodeCalls += 1
                assertEquals(typeOf<String>(), type)
                "decoded:$text"
            }
        ))
        try {
            assertEquals("raw", ValueCodecRegistry.convert(serializedDecode("raw", typeOf<String>(), MAP)))
            assertEquals("decoded:raw", ValueCodecRegistry.convert(serializedDecode("raw", typeOf<String>(), DATABASE)))
            assertEquals("decoded:raw", ValueCodecRegistry.convert(serializedDecode("raw", typeOf<String>(), DELEGATE)))
            assertEquals(2, decodeCalls)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `serialized List of enum is encoded and decoded once with its complete KType`() {
        val listType = typeOf<List<Status>?>()
        var encodeCalls = 0
        var decodeCalls = 0
        val seenTypes = mutableListOf<KType>()
        val registration = Kronos.registerValueCodec(serializedValueCodec(
            encode = { value, type ->
                encodeCalls += 1
                seenTypes += type
                assertEquals(listOf(Status.READY, Status.CLOSED), value)
                "[\"READY\",\"CLOSED\"]"
            },
            decode = { text, type ->
                decodeCalls += 1
                seenTypes += type
                assertEquals("[\"READY\",\"CLOSED\"]", text)
                listOf(Status.READY, Status.CLOSED)
            }
        ))
        try {
            val encoded = ValueCodecRegistry.convert(ValueConversionRequest(
                listOf(Status.READY, Status.CLOSED),
                ENCODE,
                PARAMETER,
                listType,
                sourceType = listType,
                storage = SERIALIZED
            ))
            val decoded = ValueCodecRegistry.convert(ValueConversionRequest(
                "[\"READY\",\"CLOSED\"]",
                DECODE,
                DATABASE,
                listType,
                sourceType = typeOf<String>(),
                storage = SERIALIZED
            ))

            assertEquals("[\"READY\",\"CLOSED\"]", encoded)
            assertEquals(listOf(Status.READY, Status.CLOSED), decoded)
            assertEquals(1, encodeCalls)
            assertEquals(1, decodeCalls)
            assertEquals(listOf(listType, listType), seenTypes)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `serialized single enum uses serialized storage instead of enum name`() {
        val enumType = typeOf<Status>()
        val gson = Gson()
        val encodedTypes = mutableListOf<KType>()
        val decodedTypes = mutableListOf<KType>()
        val registration = Kronos.registerValueCodec(serializedValueCodec(
            encode = { value, type ->
                encodedTypes += type
                gson.toJson(value, type.javaType)
            },
            decode = { text, type ->
                decodedTypes += type
                gson.fromJson(text, type.javaType)
            }
        ))

        try {
            val encoded = ValueCodecRegistry.convert(ValueConversionRequest(
                Status.READY,
                ENCODE,
                PARAMETER,
                enumType,
                sourceType = enumType,
                storage = SERIALIZED
            ))
            val decoded = ValueCodecRegistry.convert(ValueConversionRequest(
                encoded,
                DECODE,
                DATABASE,
                enumType,
                sourceType = typeOf<String>(),
                storage = SERIALIZED
            ))

            assertEquals("\"READY\"", encoded)
            assertEquals(Status.READY, decoded)
            assertEquals(listOf(enumType), encodedTypes)
            assertEquals(listOf(enumType), decodedTypes)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `serialized generic enum map round trips with its complete KType`() {
        val mapType = typeOf<Map<String, List<Status?>>>()
        val source = linkedMapOf(
            "primary" to listOf(Status.READY, null),
            "secondary" to listOf(Status.CLOSED)
        )
        val gson = Gson()
        val encodedTypes = mutableListOf<KType>()
        val decodedTypes = mutableListOf<KType>()
        val registration = Kronos.registerValueCodec(serializedValueCodec(
            encode = { value, type ->
                encodedTypes += type
                gson.toJson(value, type.javaType)
            },
            decode = { text, type ->
                decodedTypes += type
                gson.fromJson(text, type.javaType)
            }
        ))

        try {
            val encoded = ValueCodecRegistry.convert(ValueConversionRequest(
                source,
                ENCODE,
                PARAMETER,
                mapType,
                sourceType = mapType,
                storage = SERIALIZED
            ))
            val decoded = ValueCodecRegistry.convert(ValueConversionRequest(
                encoded,
                DECODE,
                DATABASE,
                mapType,
                sourceType = typeOf<String>(),
                storage = SERIALIZED
            ))

            assertEquals("{\"primary\":[\"READY\",null],\"secondary\":[\"CLOSED\"]}", encoded)
            assertEquals(source, decoded)
            assertEquals(listOf(mapType), encodedTypes)
            assertEquals(listOf(mapType), decodedTypes)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `missing serialized codec is explicit except for map identity`() {
        val mappedStatuses = listOf(Status.READY)
        assertSame(
            mappedStatuses,
            ValueCodecRegistry.convert(serializedDecode(mappedStatuses, typeOf<List<Status>>(), MAP))
        )
        val failure = assertFailsWith<MissingSerializedCodec> {
            ValueCodecRegistry.convert(serializedDecode("[]", typeOf<List<Status>>(), DATABASE))
        }
        assertEquals(typeOf<List<Status>>(), failure.targetType)
    }

    @Test
    fun `serialized text null is validated after codec conversion`() {
        val registration = Kronos.registerValueCodec(serializedValueCodec(
            encode = { _, _ -> "null" },
            decode = { text, _ -> if (text == "null") null else error("unexpected text: $text") }
        ))
        try {
            assertNull(ValueCodecRegistry.convert(serializedDecode("null", typeOf<Status?>(), DATABASE)))
            assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(serializedDecode("null", typeOf<Status>(), DATABASE))
            }
        } finally {
            registration.close()
        }
    }

    @Test
    fun `declared generic source prevents an unsafe serialized map identity`() {
        val failure = assertFailsWith<MissingSerializedCodec> {
            ValueCodecRegistry.convert(ValueConversionRequest(
                listOf("READY"),
                DECODE,
                MAP,
                typeOf<List<Status>>(),
                sourceType = typeOf<List<String>>(),
                storage = SERIALIZED
            ))
        }
        assertEquals(typeOf<List<Status>>(), failure.targetType)
    }

    @Test
    fun `serialized codec output validates nested collection element types`() {
        val valid = Kronos.registerValueCodec(serializedValueCodec(
            encode = { _, _ -> "encoded" },
            decode = { _, _ -> mapOf("statuses" to listOf(Status.READY)) }
        ))
        try {
            assertEquals(
                mapOf("statuses" to listOf(Status.READY)),
                ValueCodecRegistry.convert(serializedDecode(
                    "{}",
                    typeOf<Map<String, List<Status>>>(),
                    DATABASE
                ))
            )
        } finally {
            valid.close()
        }

        val wrongList = Kronos.registerValueCodec(serializedValueCodec(
            encode = { _, _ -> "encoded" },
            decode = { _, _ -> listOf("READY") }
        ))
        try {
            val failure = assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(serializedDecode("[]", typeOf<List<Status>>(), DATABASE))
            }
            assertEquals(typeOf<List<Status>>(), failure.targetType)
            assertNull(failure.declaredSourceType)
            assertEquals(typeOf<String>(), failure.runtimeSourceType)
        } finally {
            wrongList.close()
        }

        val wrongArray = Kronos.registerValueCodec(valueCodec(
            supports = { _, context -> context.targetType == typeOf<Array<Status>>() },
            convert = { _, _ -> arrayOf("READY") }
        ))
        try {
            assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(decodeRequest("ignored", typeOf<Array<Status>>()))
            }
        } finally {
            wrongArray.close()
        }
    }

    @Test
    fun `enum uses name for encode and generated metadata for decode`() {
        val metadata = enumMetadata()
        assertEquals(
            "READY",
            ValueCodecRegistry.convert(ValueConversionRequest(Status.READY, ENCODE, PARAMETER, typeOf<Status>()))
        )
        assertEquals(
            Status.CLOSED,
            ValueCodecRegistry.convert(decodeRequest("CLOSED", typeOf<Status>()), metadata)
        )
        assertEquals(
            Status.READY,
            ValueCodecRegistry.convert(decodeRequest(Status.READY, typeOf<Status>()), metadata)
        )
    }

    @Test
    fun `enum errors distinguish missing metadata unknown names and source mismatch`() {
        val emptyMetadata = loadGeneratedTypeMetadata(emptyList())
        assertFailsWith<MissingEnumMetadata> {
            ValueCodecRegistry.convert(decodeRequest("READY", typeOf<Status>()), emptyMetadata)
        }

        val unknown = assertFailsWith<UnknownEnumValue> {
            ValueCodecRegistry.convert(decodeRequest("x".repeat(200), typeOf<Status>()), enumMetadata())
        }
        assertEquals("x".repeat(128) + "...", unknown.rawValuePreview)
        assertFalse(unknown.message.orEmpty().contains("x".repeat(200)))
        assertTrue(unknown.message.orEmpty().contains("x".repeat(128) + "..."))

        listOf("", "ready").forEach { raw ->
            assertFailsWith<UnknownEnumValue> {
                ValueCodecRegistry.convert(decodeRequest(raw, typeOf<Status>()), enumMetadata())
            }
        }

        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(decodeRequest(1, typeOf<Status>()), enumMetadata())
        }

        var codecCalls = 0
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, context ->
                codecCalls += 1
                context.targetType == typeOf<Status>()
            },
            convert = { _, _ -> "READY" }
        ))
        try {
            val failure = assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest(
                    OtherStatus.READY,
                    ENCODE,
                    PARAMETER,
                    typeOf<Status>(),
                    physicalTargetType = typeOf<String>()
                ))
            }
            assertNull(failure.declaredSourceType)
            assertEquals(typeOf<OtherStatus>(), failure.runtimeSourceType)
            assertEquals(typeOf<Status>(), failure.targetType)
            assertEquals(0, codecCalls)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `enum integer fields use declaration ordinal while string fields use name`() {
        val metadata = enumMetadata()
        val integerTypes = listOf(
            KColumnType.TINYINT,
            KColumnType.SMALLINT,
            KColumnType.INT,
            KColumnType.MEDIUMINT,
            KColumnType.SERIAL,
            KColumnType.BIGINT
        )
        integerTypes.forEach { columnType ->
            val field = Field("status", type = columnType, kType = typeOf<Status>())
            assertEquals(
                1,
                ValueCodecRegistry.convert(ValueConversionRequest(
                    Status.CLOSED,
                    ENCODE,
                    PARAMETER,
                    typeOf<Status>(),
                    sourceType = typeOf<Status>(),
                    field = field
                ))
            )
            assertEquals(
                Status.CLOSED,
                ValueCodecRegistry.convert(ValueConversionRequest(
                    1,
                    DECODE,
                    DATABASE,
                    typeOf<Status>(),
                    sourceType = typeOf<Int>(),
                    field = field
                ), metadata)
            )
        }

        val stringField = Field("status", type = KColumnType.VARCHAR, kType = typeOf<Status>())
        assertEquals(
            "CLOSED",
            ValueCodecRegistry.convert(ValueConversionRequest(
                Status.CLOSED,
                ENCODE,
                PARAMETER,
                typeOf<Status>(),
                sourceType = typeOf<Status>(),
                field = stringField
            ))
        )
        assertEquals(
            Status.CLOSED,
            ValueCodecRegistry.convert(ValueConversionRequest(
                "CLOSED",
                DECODE,
                DATABASE,
                typeOf<Status>(),
                sourceType = typeOf<String>(),
                field = stringField
            ), metadata)
        )
        assertEquals(
            "CLOSED",
            ValueCodecRegistry.convert(ValueConversionRequest(Status.CLOSED, ENCODE, PARAMETER, typeOf<Status>()))
        )
    }

    @Test
    fun `enum ordinal decoding rejects non-integers negative and out of range values with context`() {
        val field = Field("status_col", "status", type = KColumnType.INT, kType = typeOf<Status>())
        listOf<Any>(BigDecimal("1.5"), -1, 2, Long.MAX_VALUE, "1").forEach { raw ->
            val failure = assertFailsWith<InvalidEnumOrdinal> {
                ValueCodecRegistry.convert(ValueConversionRequest(
                    raw,
                    DECODE,
                    DATABASE,
                    typeOf<Status>(),
                    sourceType = raw::class.starProjectedType,
                    field = field,
                    valueName = "status",
                    batchIndex = 2
                ), enumMetadata())
            }
            assertEquals("status", failure.fieldName)
            assertEquals("status_col", failure.columnName)
            assertEquals(2, failure.batchIndex)
            assertEquals(raw.toString(), failure.rawValuePreview)
            assertTrue(failure.message.orEmpty().contains("invalid enum ordinal"))
        }
    }

    @Test
    fun `user enum codec overrides integer ordinal and closing restores it`() {
        val field = Field("status", type = KColumnType.INT, kType = typeOf<Status>())
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { value, context ->
                context.storage == NONE &&
                    context.field?.type == KColumnType.INT &&
                    context.targetType == typeOf<Status>() &&
                    when (context.direction) {
                        ENCODE -> value is Status
                        DECODE -> value is Int
                    }
            },
            convert = { value, context ->
                when (context.direction) {
                    ENCODE -> if (value == Status.READY) 70 else 80
                    DECODE -> if (value == 70) Status.READY else Status.CLOSED
                }
            }
        ))
        try {
            assertEquals(
                70,
                ValueCodecRegistry.convert(ValueConversionRequest(
                    Status.READY,
                    ENCODE,
                    PARAMETER,
                    typeOf<Status>(),
                    sourceType = typeOf<Status>(),
                    field = field
                ))
            )
            assertEquals(
                Status.CLOSED,
                ValueCodecRegistry.convert(ValueConversionRequest(
                    80,
                    DECODE,
                    DATABASE,
                    typeOf<Status>(),
                    sourceType = typeOf<Int>(),
                    field = field
                ), enumMetadata())
            )
        } finally {
            registration.close()
        }

        assertEquals(
            1,
            ValueCodecRegistry.convert(ValueConversionRequest(
                Status.CLOSED,
                ENCODE,
                PARAMETER,
                typeOf<Status>(),
                sourceType = typeOf<Status>(),
                field = field
            ))
        )
        assertEquals(
            Status.CLOSED,
            ValueCodecRegistry.convert(ValueConversionRequest(
                1,
                DECODE,
                DATABASE,
                typeOf<Status>(),
                sourceType = typeOf<Int>(),
                field = field
            ), enumMetadata())
        )
    }

    @Test
    fun `user codec overrides enum in both directions and close restores defaults`() {
        val metadata = enumMetadata()
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { value, context ->
                context.storage == NONE &&
                    context.targetType == typeOf<Status>() &&
                    when (context.direction) {
                        ENCODE -> value is Status
                        DECODE -> value is Int
                    }
            },
            convert = { value, context ->
                when (context.direction) {
                    ENCODE -> if (value == Status.READY) 7 else 8
                    DECODE -> if (value == 7) Status.READY else Status.CLOSED
                }
            }
        ))
        try {
            assertEquals(7, ValueCodecRegistry.convert(ValueConversionRequest(Status.READY, ENCODE, PARAMETER, typeOf<Status>()), metadata))
            assertEquals(Status.CLOSED, ValueCodecRegistry.convert(decodeRequest(8, typeOf<Status>()), metadata))
        } finally {
            registration.close()
        }

        assertEquals("READY", ValueCodecRegistry.convert(ValueConversionRequest(Status.READY, ENCODE, PARAMETER, typeOf<Status>()), metadata))
        assertEquals(Status.CLOSED, ValueCodecRegistry.convert(decodeRequest("CLOSED", typeOf<Status>()), metadata))
    }

    @Test
    fun `plain enum collection cannot become a database scalar by identity`() {
        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(ValueConversionRequest(
                listOf(Status.READY),
                ENCODE,
                PARAMETER,
                typeOf<List<Status>>(),
                sourceType = typeOf<List<Status>>()
            ))
        }
    }

    @Test
    fun `generated metadata normalizes nullable enum and treats equal signatures as idempotent`() {
        val provider = provider("nullable") { registrar ->
            registrar.registerEnum(typeOf<Status>(), listOf("READY", "CLOSED"), statusFactory())
            registrar.registerEnum(typeOf<Status?>(), listOf("READY", "CLOSED"), statusFactory())
        }
        val metadata = loadGeneratedTypeMetadata(listOf(provider))

        assertEquals(listOf("READY", "CLOSED"), metadata.enumMetadata(typeOf<Status>())?.entryNames)
        assertEquals(listOf("READY", "CLOSED"), metadata.enumMetadata(typeOf<Status?>())?.entryNames)
    }

    @Test
    fun `generated metadata recognizes an indirect KPojo through the JVM boundary`() {
        val metadata = loadGeneratedTypeMetadata(listOf(provider("indirect-kpojo") { registrar ->
            registrar.registerKPojo(
                typeOf<IndirectEntity>(),
                "indirect-owner",
                "IndirectEntity()",
                KPojoFactory { IndirectEntity() }
            )
        }))

        assertEquals(typeOf<IndirectEntity>(), metadata.kPojoFactory(typeOf<IndirectEntity>())?.type)
    }

    @Test
    fun `generated enum and provider conflicts are deterministic`() {
        val first = enumProvider("first", typeOf<Status>(), listOf("READY", "CLOSED"), statusFactory())
        val conflict = enumProvider("second", typeOf<Status>(), listOf("CLOSED", "READY"), statusFactory())
        assertFailsWith<ConflictingEnumMetadata> {
            loadGeneratedTypeMetadata(listOf(first, conflict))
        }
        assertFailsWith<ConflictingEnumMetadata> {
            loadGeneratedTypeMetadata(listOf(conflict, first))
        }

        val duplicateId = enumProvider("first", typeOf<OtherStatus>(), listOf("READY"), EnumFactory { OtherStatus.READY })
        assertFailsWith<ConflictingGeneratedProvider> {
            loadGeneratedTypeMetadata(listOf(first, duplicateId))
        }

        val duplicate = enumProvider("first", typeOf<Status?>(), listOf("READY", "CLOSED"), statusFactory())
        val forward = loadGeneratedTypeMetadata(listOf(first, duplicate))
        val reverse = loadGeneratedTypeMetadata(listOf(duplicate, first))
        assertEquals(forward.enumMetadata(typeOf<Status>())?.entryNames, reverse.enumMetadata(typeOf<Status>())?.entryNames)

        val ordered = provider("ordered") { registrar ->
            registrar.registerEnum(typeOf<Status>(), listOf("READY", "CLOSED"), statusFactory())
            registrar.registerEnum(typeOf<OtherStatus>(), listOf("READY"), EnumFactory { OtherStatus.READY })
        }
        val reordered = provider("ordered") { registrar ->
            registrar.registerEnum(typeOf<OtherStatus?>(), listOf("READY"), EnumFactory { OtherStatus.READY })
            registrar.registerEnum(typeOf<Status?>(), listOf("READY", "CLOSED"), statusFactory())
        }
        val reorderedMetadata = loadGeneratedTypeMetadata(listOf(ordered, reordered))
        assertEquals(listOf("READY", "CLOSED"), reorderedMetadata.enumMetadata(typeOf<Status?>())?.entryNames)
        assertEquals(listOf("READY"), reorderedMetadata.enumMetadata(typeOf<OtherStatus>())?.entryNames)

        val sameEnumFromAnotherModule = enumProvider(
            "third",
            typeOf<Status?>(),
            listOf("READY", "CLOSED"),
            statusFactory()
        )
        val otherEnum = enumProvider(
            "other",
            typeOf<OtherStatus>(),
            listOf("READY"),
            EnumFactory { name -> OtherStatus.READY.takeIf { name == "READY" } }
        )
        val modulesForward = loadGeneratedTypeMetadata(listOf(first, sameEnumFromAnotherModule, otherEnum))
        val modulesReverse = loadGeneratedTypeMetadata(listOf(otherEnum, sameEnumFromAnotherModule, first))
        assertEquals(
            modulesForward.enumMetadata(typeOf<Status>())?.entryNames,
            modulesReverse.enumMetadata(typeOf<Status?>())?.entryNames
        )
        assertEquals(
            modulesForward.enumMetadata(typeOf<OtherStatus>())?.entryNames,
            modulesReverse.enumMetadata(typeOf<OtherStatus>())?.entryNames
        )
    }

    @Test
    fun `basic codecs cover numbers booleans chars strings and strict decode`() {
        assertEquals(42, ValueCodecRegistry.convert(decodeRequest("42", typeOf<Int>())))
        assertEquals(42L, ValueCodecRegistry.convert(decodeRequest(BigDecimal("42.9"), typeOf<Long>())))
        assertEquals(BigDecimal("42.5"), ValueCodecRegistry.convert(decodeRequest(42.5, typeOf<BigDecimal>())))
        assertEquals(
            BigInteger("42"),
            ValueCodecRegistry.convert(decodeRequest(BigDecimal("42.5"), typeOf<BigInteger>()))
        )
        assertEquals(true, ValueCodecRegistry.convert(decodeRequest(1, typeOf<Boolean>())))
        assertEquals(false, ValueCodecRegistry.convert(decodeRequest("", typeOf<Boolean>())))
        assertEquals('A', ValueCodecRegistry.convert(decodeRequest(65, typeOf<Char>())))
        assertEquals("42", ValueCodecRegistry.convert(decodeRequest(42, typeOf<String>())))
        assertEquals(
            0,
            ValueCodecRegistry.convert(ValueConversionRequest(
                false,
                ENCODE,
                PARAMETER,
                typeOf<Boolean>(),
                physicalTargetType = typeOf<Int>(),
                dialect = SqlDialect.MySql,
                strict = true
            ))
        )

        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(decodeRequest("42", typeOf<Int>()).copy(strict = true))
        }
    }

    @Test
    fun `strict database decode normalizes JDBC Number implementations across numeric targets`() {
        fun databaseNumber(value: Number, targetType: KType): ValueConversionRequest =
            decodeRequest(value, targetType).copy(origin = DATABASE, strict = true)

        assertEquals(1.toByte(), ValueCodecRegistry.convert(databaseNumber(BigDecimal("1"), typeOf<Byte>())))
        assertEquals(2.toShort(), ValueCodecRegistry.convert(databaseNumber(BigInteger("2"), typeOf<Short>())))
        assertEquals(3, ValueCodecRegistry.convert(databaseNumber(BigDecimal("3.000"), typeOf<Int?>())))
        assertEquals(4L, ValueCodecRegistry.convert(databaseNumber(BigInteger("4"), typeOf<Long>())))
        assertEquals(5.5F, ValueCodecRegistry.convert(databaseNumber(BigDecimal("5.5"), typeOf<Float>())))
        assertEquals(6.5, ValueCodecRegistry.convert(databaseNumber(BigDecimal("6.5"), typeOf<Double>())))
        assertEquals(BigInteger("7"), ValueCodecRegistry.convert(databaseNumber(7L, typeOf<BigInteger>())))
        assertEquals(BigDecimal("8"), ValueCodecRegistry.convert(databaseNumber(8, typeOf<BigDecimal>())))

        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(decodeRequest(BigDecimal("9"), typeOf<Int>()).copy(strict = true))
        }
    }

    @Test
    fun `strict database decode normalizes only exact Oracle family BIT values to Boolean`() {
        fun oracleBit(value: Number, type: KColumnType = KColumnType.BIT): ValueConversionRequest =
            ValueConversionRequest(
                value = value,
                direction = DECODE,
                origin = DATABASE,
                targetType = typeOf<Boolean?>(),
                field = Field("deleted", type = type, kType = typeOf<Boolean?>()),
                dialect = SqlDialect.Oracle,
                strict = true
            )

        assertEquals(false, ValueCodecRegistry.convert(oracleBit(BigDecimal.ZERO)))
        assertEquals(true, ValueCodecRegistry.convert(oracleBit(BigDecimal("1.000"))))

        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(oracleBit(BigDecimal.ONE).copy(dialect = SqlDialect.SQLite))
        }
        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(oracleBit(BigDecimal.ONE, KColumnType.INT))
        }
        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(oracleBit(BigDecimal("2")))
        }
        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(oracleBit(BigDecimal("0.5")))
        }
    }

    @Test
    fun `strict database numeric decode rejects fractional integral values and range overflow`() {
        fun strictDatabaseNumber(value: Number, targetType: KType): ValueConversionRequest =
            decodeRequest(value, targetType).copy(origin = DATABASE, strict = true)

        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(strictDatabaseNumber(BigDecimal("1.5"), typeOf<Int>()))
        }
        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(
                strictDatabaseNumber(
                    BigInteger.valueOf(Int.MAX_VALUE.toLong()).add(BigInteger.ONE),
                    typeOf<Int>()
                )
            )
        }
        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(strictDatabaseNumber(BigDecimal("1E+10000"), typeOf<Double>()))
        }
    }

    @Test
    fun `strict still executes user serialized and enum codecs`() {
        val user = Kronos.registerValueCodec(valueCodec(
            supports = { _, context -> context.targetType == typeOf<BigDecimal>() },
            convert = { _, _ -> BigDecimal.TEN }
        ))
        val serialized = Kronos.registerValueCodec(serializedValueCodec(
            encode = { _, _ -> "encoded" },
            decode = { _, _ -> listOf(Status.READY) }
        ))
        try {
            assertEquals(
                BigDecimal.TEN,
                ValueCodecRegistry.convert(decodeRequest("ignored", typeOf<BigDecimal>()).copy(strict = true))
            )
            assertEquals(
                listOf(Status.READY),
                ValueCodecRegistry.convert(serializedDecode("[]", typeOf<List<Status>>(), DATABASE).copy(strict = true))
            )
            assertEquals(
                Status.READY,
                ValueCodecRegistry.convert(decodeRequest("READY", typeOf<Status>()).copy(strict = true), enumMetadata())
            )
        } finally {
            serialized.close()
            user.close()
        }
    }

    @Test
    fun `date format field default timezone and text boundaries are preserved`() {
        val originalFormat = Kronos.defaultDateFormat
        val originalZone = Kronos.timeZone
        try {
            Kronos.defaultDateFormat = "dd-MM-yyyy HH:mm:ss"
            Kronos.timeZone = ZoneId.of("Asia/Shanghai")
            val field = Field(
                "created_at",
                "createdAt",
                dateFormat = "yyyy/MM/dd HH:mm:ss",
                kType = typeOf<LocalDateTime>()
            )
            assertEquals(
                LocalDateTime.of(2026, 7, 20, 12, 34, 56),
                ValueCodecRegistry.convert(ValueConversionRequest(
                    "2026/07/20 12:34:56",
                    DECODE,
                    MAP,
                    typeOf<LocalDateTime>(),
                    field = field
                ))
            )
            assertEquals(
                LocalDateTime.of(2026, 7, 20, 12, 34, 56),
                ValueCodecRegistry.convert(decodeRequest("20-07-2026 12:34:56", typeOf<LocalDateTime>()))
            )
            assertEquals(
                LocalDateTime.of(1970, 1, 1, 8, 0),
                ValueCodecRegistry.convert(decodeRequest(Instant.EPOCH, typeOf<LocalDateTime>()))
            )
            assertEquals(
                LocalDateTime.of(2026, 7, 20, 18, 0),
                ValueCodecRegistry.convert(decodeRequest(
                    OffsetDateTime.parse("2026-07-20T12:00:00+02:00"),
                    typeOf<LocalDateTime>()
                ))
            )
        } finally {
            Kronos.defaultDateFormat = originalFormat
            Kronos.timeZone = originalZone
        }
    }

    @Test
    fun `native temporal and epoch paths never read an irrelevant invalid date format`() {
        val invalidField = Field("created_at", dateFormat = "[invalid", kType = typeOf<LocalDateTime>())
        val local = LocalDateTime.of(2026, 7, 20, 12, 34, 56)
        val utc = ZoneId.of("UTC")
        assertEquals(
            local,
            ValueCodecRegistry.convert(ValueConversionRequest(
                Timestamp.valueOf(local),
                DECODE,
                DATABASE,
                typeOf<LocalDateTime>(),
                field = invalidField
            ))
        )
        assertEquals(
            0L,
            ValueCodecRegistry.convert(ValueConversionRequest(
                Instant.EPOCH,
                DECODE,
                MAP,
                typeOf<Long>(),
                field = Field("epoch", dateFormat = "[invalid", kType = typeOf<Long>())
            ))
        )
        assertEquals(
            Instant.EPOCH,
            ValueCodecRegistry.convert(ValueConversionRequest(
                0L,
                DECODE,
                MAP,
                typeOf<Instant>(),
                field = Field("instant", dateFormat = "[invalid", kType = typeOf<Instant>())
            ))
        )
        assertEquals(
            Instant.parse("2026-07-20T00:00:00Z"),
            ValueCodecRegistry.convert(ValueConversionRequest(
                java.sql.Date.valueOf("2026-07-20"),
                DECODE,
                DATABASE,
                typeOf<Instant>(),
                field = Field("date", dateFormat = "[invalid", kType = typeOf<Instant>()),
                timeZone = utc
            ))
        )
        assertEquals(
            Instant.parse("2026-07-20T12:34:56Z"),
            ValueCodecRegistry.convert(ValueConversionRequest(
                Timestamp.from(Instant.parse("2026-07-20T12:34:56Z")),
                DECODE,
                DATABASE,
                typeOf<Instant>(),
                field = Field("timestamp", dateFormat = "[invalid", kType = typeOf<Instant>()),
                timeZone = utc
            ))
        )
        assertEquals(
            Instant.parse("2026-07-20T12:34:56Z").toEpochMilli(),
            ValueCodecRegistry.convert(ValueConversionRequest(
                Timestamp.from(Instant.parse("2026-07-20T12:34:56Z")),
                DECODE,
                DATABASE,
                typeOf<Long>(),
                field = Field("timestamp", dateFormat = "[invalid", kType = typeOf<Long>()),
                timeZone = ZoneId.of("Asia/Shanghai")
            ))
        )
        val timestampAsZoned = ValueCodecRegistry.convert(ValueConversionRequest(
            Timestamp.from(Instant.parse("2026-07-20T12:34:56Z")),
            DECODE,
            DATABASE,
            typeOf<ZonedDateTime>(),
            field = Field("timestamp", dateFormat = "[invalid", kType = typeOf<ZonedDateTime>()),
            timeZone = ZoneId.of("Asia/Shanghai")
        )) as ZonedDateTime
        assertEquals(Instant.parse("2026-07-20T12:34:56Z"), timestampAsZoned.toInstant())
        assertEquals(ZoneId.of("Asia/Shanghai"), timestampAsZoned.zone)
        assertEquals(
            LocalTime.of(12, 34, 56).toSecondOfDay() * 1_000L,
            ValueCodecRegistry.convert(ValueConversionRequest(
                java.sql.Time.valueOf("12:34:56"),
                DECODE,
                DATABASE,
                typeOf<Long>(),
                field = Field("time", dateFormat = "[invalid", kType = typeOf<Long>()),
                timeZone = utc
            ))
        )
        assertEquals(
            ZonedDateTime.parse("2026-07-20T00:00:00Z[UTC]"),
            ValueCodecRegistry.convert(ValueConversionRequest(
                java.sql.Date.valueOf("2026-07-20"),
                DECODE,
                DATABASE,
                typeOf<ZonedDateTime>(),
                field = Field("date", dateFormat = "[invalid", kType = typeOf<ZonedDateTime>()),
                timeZone = utc
            ))
        )
    }

    @Test
    fun `timestamp java Instant and kotlin Instant preserve nanoseconds`() {
        val javaInstant = Instant.ofEpochSecond(1_753_000_000L, 123_456_789)
        val timestamp = Timestamp.from(javaInstant)

        val kotlinInstant = ValueCodecRegistry.convert(ValueConversionRequest(
            timestamp,
            DECODE,
            DATABASE,
            typeOf<kotlin.time.Instant>()
        )) as kotlin.time.Instant
        assertEquals(javaInstant.epochSecond, kotlinInstant.epochSeconds)
        assertEquals(javaInstant.nano, kotlinInstant.nanosecondsOfSecond)

        assertEquals(
            javaInstant,
            ValueCodecRegistry.convert(ValueConversionRequest(
                kotlinInstant,
                ENCODE,
                PARAMETER,
                typeOf<kotlin.time.Instant>(),
                physicalTargetType = typeOf<Instant>()
            ))
        )
        assertEquals(
            javaInstant,
            (ValueCodecRegistry.convert(ValueConversionRequest(
                kotlinInstant,
                ENCODE,
                PARAMETER,
                typeOf<kotlin.time.Instant>(),
                physicalTargetType = typeOf<Timestamp>()
            )) as Timestamp).toInstant()
        )
        assertEquals(
            kotlinInstant,
            ValueCodecRegistry.convert(decodeRequest(javaInstant, typeOf<kotlin.time.Instant>()))
        )
    }

    @Test
    fun `temporal encode honors physical target dialect explicit format and strict`() {
        val local = LocalDateTime.of(2026, 7, 20, 12, 34, 56)
        val request = ValueConversionRequest(
            local,
            ENCODE,
            PARAMETER,
            typeOf<LocalDateTime>(),
            physicalTargetType = typeOf<Timestamp>(),
            dialect = SqlDialect.PostgreSql,
            dateFormat = "[invalid",
            strict = true
        )
        assertEquals(Timestamp.valueOf(local), ValueCodecRegistry.convert(request))

        assertEquals(
            "2026/07/20 12:34:56",
            ValueCodecRegistry.convert(ValueConversionRequest(
                local,
                ENCODE,
                PARAMETER,
                typeOf<String>(),
                sourceType = typeOf<LocalDateTime>(),
                field = Field(
                    "created_at",
                    dateFormat = "dd-MM-yyyy HH:mm:ss",
                    kType = typeOf<String>()
                ),
                dateFormat = "yyyy/MM/dd HH:mm:ss",
                strict = true
            ))
        )

        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(decodeRequest("2026-07-20T12:34:56", typeOf<LocalDateTime>()).copy(strict = true))
        }
    }

    @Test
    fun `ready database value bypasses serialized encoding but still validates physical output`() {
        assertEquals(
            "already-json",
            ValueCodecRegistry.acceptPrepared(ValueConversionRequest(
                "already-json",
                ENCODE,
                PARAMETER,
                typeOf<List<Status>>(),
                sourceType = typeOf<String>(),
                storage = SERIALIZED,
                physicalTargetType = typeOf<String>()
            ))
        )
        assertEquals(
            7,
            ValueCodecRegistry.acceptPrepared(ValueConversionRequest(
                7,
                ENCODE,
                PARAMETER,
                typeOf<List<Status>>(),
                sourceType = typeOf<Int>(),
                storage = SERIALIZED,
                physicalTargetType = typeOf<Int>()
            ))
        )
        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.acceptPrepared(ValueConversionRequest(
                7,
                ENCODE,
                PARAMETER,
                typeOf<List<Status>>(),
                sourceType = typeOf<Int>(),
                storage = SERIALIZED,
                physicalTargetType = typeOf<String>()
            ))
        }
        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.acceptPrepared(ValueConversionRequest(
                listOf("already-json"),
                ENCODE,
                PARAMETER,
                typeOf<List<String>>(),
                sourceType = typeOf<List<String>>()
            ))
        }
    }

    @Test
    fun `ready database null observes target nullability`() {
        assertNull(ValueCodecRegistry.acceptPrepared(ValueConversionRequest(
            null,
            ENCODE,
            PARAMETER,
            typeOf<String?>(),
            sourceType = typeOf<String?>()
        )))
        assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.acceptPrepared(ValueConversionRequest(
                null,
                ENCODE,
                PARAMETER,
                typeOf<String>(),
                sourceType = typeOf<String?>()
            ))
        }
    }

    @Test
    fun `serialized encode requires text from every ordinary user codec`() {
        val targetType = typeOf<List<Status>?>()
        val statuses = listOf(Status.READY)
        val field = Field(
            columnName = "status_json",
            name = "statuses",
            kType = targetType,
            serializable = true
        )
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, context -> context.storage == SERIALIZED && context.direction == ENCODE },
            convert = { _, _ -> 7L }
        ))
        try {
            val failure = assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest(
                    statuses,
                    ENCODE,
                    PARAMETER,
                    targetType,
                    sourceType = targetType,
                    field = field,
                    valueName = "statuses@0"
                ))
            }

            assertEquals(targetType, failure.declaredSourceType)
            assertEquals(statuses::class.starProjectedType, failure.runtimeSourceType)
            assertEquals(targetType, failure.targetType)
            assertEquals("statuses", failure.fieldName)
            assertEquals("status_json", failure.columnName)
            assertEquals("statuses@0", failure.valueName)
            assertTrue(failure.message.orEmpty().contains("SERIALIZED encode must produce kotlin.String"))
        } finally {
            registration.close()
        }

        val nullRegistration = Kronos.registerValueCodec(valueCodec(
            supports = { _, context -> context.storage == SERIALIZED && context.direction == ENCODE },
            convert = { _, _ -> null }
        ))
        try {
            val failure = assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest(
                    listOf(Status.READY),
                    ENCODE,
                    PARAMETER,
                    targetType,
                    field = field
                ))
            }
            assertTrue(failure.message.orEmpty().contains("serialized codec returned null"))
        } finally {
            nullRegistration.close()
        }
    }

    @Test
    fun `encode rejects a wrong logical input before invoking a codec`() {
        var codecCalls = 0
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, _ ->
                codecCalls += 1
                true
            },
            convert = { _, _ -> "READY" }
        ))
        try {
            val failure = assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest(
                    "READY",
                    ENCODE,
                    PARAMETER,
                    typeOf<Status>(),
                    physicalTargetType = typeOf<String>()
                ))
            }
            assertNull(failure.declaredSourceType)
            assertEquals(typeOf<String>(), failure.runtimeSourceType)
            assertEquals(typeOf<Status>(), failure.targetType)
            assertEquals(0, codecCalls)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `serialized encode validates concrete generic elements without a declared source type`() {
        var encodeCalls = 0
        val registration = Kronos.registerValueCodec(serializedValueCodec(
            encode = { _, _ ->
                encodeCalls += 1
                "encoded"
            },
            decode = { _, _ -> error("decode is not expected") }
        ))
        try {
            assertEquals(
                "encoded",
                ValueCodecRegistry.convert(ValueConversionRequest(
                    listOf(Status.READY),
                    ENCODE,
                    PARAMETER,
                    typeOf<List<Status>>(),
                    storage = SERIALIZED
                ))
            )
            assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest(
                    listOf("READY"),
                    ENCODE,
                    PARAMETER,
                    typeOf<List<Status>>(),
                    storage = SERIALIZED
                ))
            }
            assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest(
                    listOf(Status.READY),
                    ENCODE,
                    PARAMETER,
                    typeOf<List<Status>>(),
                    sourceType = typeOf<List<String>>(),
                    storage = SERIALIZED
                ))
            }
            assertEquals(1, encodeCalls)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `codec output and failures are validated and wrapped with request context`() {
        val wrongDecode = Kronos.registerValueCodec(valueCodec(
            supports = { _, context -> context.targetType == typeOf<Int>() },
            convert = { _, _ -> "not-an-int" }
        ))
        try {
            val failure = assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(decodeRequest("1", typeOf<Int>()))
            }
            assertNull(failure.declaredSourceType)
            assertEquals(typeOf<String>(), failure.runtimeSourceType)
        } finally {
            wrongDecode.close()
        }

        val cause = IllegalStateException("codec failure")
        val throwing = Kronos.registerValueCodec(valueCodec(
            supports = { _, _ -> true },
            convert = { _, _ -> throw cause }
        ))
        try {
            val failure = assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest(
                    Status.READY,
                    ENCODE,
                    PARAMETER,
                    typeOf<Status>(),
                    physicalTargetType = typeOf<String>(),
                    dialect = SqlDialect.PostgreSql,
                    valueName = "status@1"
                ))
            }
            assertSame(cause, failure.cause)
            assertEquals("status@1", failure.valueName)
            assertNull(failure.columnName)
            assertEquals("PostgreSql", failure.dialect)
        } finally {
            throwing.close()
        }

        val wrongPhysical = Kronos.registerValueCodec(valueCodec(
            supports = { _, _ -> true },
            convert = { _, _ -> 7 }
        ))
        try {
            assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest(
                    Status.READY,
                    ENCODE,
                    PARAMETER,
                    typeOf<Status>(),
                    physicalTargetType = typeOf<String>()
                ))
            }
        } finally {
            wrongPhysical.close()
        }
    }

    @Test
    fun `codec control flow errors are propagated unchanged`() {
        val fatal = AssertionError("fatal codec error")
        val fatalRegistration = Kronos.registerValueCodec(valueCodec(
            supports = { _, _ -> throw fatal },
            convert = { _, _ -> error("convert must not run") }
        ))
        try {
            assertSame(
                fatal,
                assertFailsWith<AssertionError> {
                    ValueCodecRegistry.convert(decodeRequest("1", typeOf<Int>()))
                }
            )
        } finally {
            fatalRegistration.close()
        }

        val cancellation = CancellationException("cancel codec")
        val cancellationRegistration = Kronos.registerValueCodec(valueCodec(
            supports = { _, _ -> true },
            convert = { _, _ -> throw cancellation }
        ))
        try {
            assertSame(
                cancellation,
                assertFailsWith<CancellationException> {
                    ValueCodecRegistry.convert(decodeRequest("1", typeOf<Int>()))
                }
            )
        } finally {
            cancellationRegistration.close()
        }
    }

    @Test
    fun `mapping failure distinguishes declared and runtime source KTypes`() {
        val failure = assertFailsWith<ValueMappingException> {
            ValueCodecRegistry.convert(ValueConversionRequest(
                "not-an-int",
                DECODE,
                MAP,
                typeOf<Int>(),
                sourceType = typeOf<Any>()
            ))
        }

        assertEquals(typeOf<Any>(), failure.declaredSourceType)
        assertEquals(typeOf<String>(), failure.runtimeSourceType)
        assertTrue(failure.message.orEmpty().contains("declared source kotlin.Any"))
        assertTrue(failure.message.orEmpty().contains("runtime source kotlin.String"))
    }

    @Test
    fun `source KType reaches user codec unchanged with a runtime fallback`() {
        val sourceType = typeOf<List<String>>()
        var seenSourceType: KType? = null
        var seenRuntimeClass: kotlin.reflect.KClass<*>? = null
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { value, context ->
                seenSourceType = context.sourceType
                seenRuntimeClass = value::class
                context.targetType == typeOf<String>()
            },
            convert = { _, _ -> "ok" }
        ))
        try {
            assertEquals(
                "ok",
                ValueCodecRegistry.convert(ValueConversionRequest(
                    listOf("a"),
                    DECODE,
                    MAP,
                    typeOf<String>(),
                    sourceType = sourceType
                ))
            )
            assertEquals(sourceType, seenSourceType)
            assertEquals("ok", ValueCodecRegistry.convert(decodeRequest(7, typeOf<String>())))
            assertEquals(typeOf<Int>(), seenSourceType)
            assertEquals(Int::class, seenRuntimeClass)
            val runtimeList = listOf("a")
            assertEquals("ok", ValueCodecRegistry.convert(decodeRequest(runtimeList, typeOf<String>())))
            assertEquals(runtimeList::class.starProjectedType, seenSourceType)
        } finally {
            registration.close()
        }
    }

    private fun decodeRequest(value: Any?, targetType: KType): ValueConversionRequest =
        ValueConversionRequest(value, DECODE, MAP, targetType)

    private fun serializedDecode(value: Any, targetType: KType, origin: com.kotlinorm.enums.ValueCodecOrigin) =
        ValueConversionRequest(value, DECODE, origin, targetType, storage = SERIALIZED)

    private fun enumMetadata(): GeneratedTypeMetadataSnapshot =
        loadGeneratedTypeMetadata(listOf(enumProvider(
            "status-provider",
            typeOf<Status>(),
            listOf("READY", "CLOSED"),
            statusFactory()
        )))

    private fun statusFactory(): EnumFactory = EnumFactory { name ->
        when (name) {
            "READY" -> Status.READY
            "CLOSED" -> Status.CLOSED
            else -> null
        }
    }

    private fun enumProvider(
        id: String,
        type: KType,
        entries: List<String>,
        factory: EnumFactory
    ): GeneratedTypeProvider = provider(id) { registrar ->
        registrar.registerEnum(type, entries, factory)
    }

    private fun provider(
        providerId: String,
        contribution: (GeneratedTypeRegistrar) -> Unit
    ): GeneratedTypeProvider = object : GeneratedTypeProvider {
        override val id: String = providerId

        override fun contributeTo(registrar: GeneratedTypeRegistrar) = contribution(registrar)
    }
}
