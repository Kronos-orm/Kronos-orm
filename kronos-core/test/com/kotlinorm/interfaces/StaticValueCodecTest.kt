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

package com.kotlinorm.interfaces

import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.utils.StaticTypeRef
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StaticValueCodecTest {
    private enum class Status {
        READY,
        CLOSED
    }

    private data class Payload(val value: String)

    @Test
    fun `common scalar nullable blob and temporal conversions preserve physical values`() {
        assertEquals(
            42,
            KronosValueCodecs.decode(KronosPhysicalValue.Integer(42), decodeContext("kotlin.Int"))
        )
        assertEquals(
            1.5,
            KronosValueCodecs.decode(KronosPhysicalValue.Real(1.5), decodeContext("kotlin.Double"))
        )
        assertNull(KronosValueCodecs.decode(KronosPhysicalValue.Null, decodeContext("kotlin.String", nullable = true)))
        assertFailsWith<StaticValueCodecException> {
            KronosValueCodecs.decode(KronosPhysicalValue.Null, decodeContext("kotlin.String"))
        }

        val bytes = byteArrayOf(0, 15, 127)
        val encodedBytes = KronosValueCodecs.encode(bytes, encodeContext("kotlin.ByteArray"))
        assertTrue(encodedBytes is KronosPhysicalValue.Blob)
        assertTrue(bytes.contentEquals(encodedBytes.bytes))
        val decodedBytes = KronosValueCodecs.decode(encodedBytes, decodeContext("kotlin.ByteArray")) as ByteArray
        assertTrue(bytes.contentEquals(decodedBytes))

        val instant = Instant.parse("2026-08-09T10:11:12Z")
        val encodedInstant = KronosValueCodecs.encode(instant, encodeContext("kotlin.time.Instant"))
        assertEquals(KronosPhysicalValue.Text(instant.toString()), encodedInstant)
        assertEquals(instant, KronosValueCodecs.decode(encodedInstant, decodeContext("kotlin.time.Instant")))
        assertEquals(
            instant,
            KronosValueCodecs.decode(
                KronosPhysicalValue.Integer(instant.toEpochMilliseconds()),
                decodeContext("kotlin.time.Instant")
            )
        )

    }

    @Test
    fun `common registrations share newest first priority and serialized text validates output`() {
        val target = StaticTypeRef("sample.Payload", nullable = false)
        val older = KronosValueCodecs.register(staticValueCodec(
            supports = { _, context -> context.targetType == target },
            convert = { _, _ -> "older" }
        ))
        val newer = KronosValueCodecs.register(staticValueCodec(
            supports = { _, context -> context.targetType == target },
            convert = { _, _ -> "newer" }
        ))
        try {
            assertEquals(
                "newer",
                KronosValueCodecs.decode(KronosPhysicalValue.Text("input"), decodeContext(target.key))
            )
            newer.close()
            assertEquals(
                "older",
                KronosValueCodecs.decode(KronosPhysicalValue.Text("input"), decodeContext(target.key))
            )
        } finally {
            newer.close()
            older.close()
        }

        val serialized = KronosValueCodecs.register(serializedTextValueCodec(
            encode = { value, type -> "${type.key}:${(value as Payload).value}" },
            decode = { text, _ -> Payload(text.substringAfter(':')) }
        ))
        try {
            val context = encodeContext(target.key, storage = ValueStorage.SERIALIZED)
            assertEquals(
                KronosPhysicalValue.Text("sample.Payload:value"),
                KronosValueCodecs.encode(Payload("value"), context)
            )
            assertEquals(
                Payload("value"),
                KronosValueCodecs.decode(
                    KronosPhysicalValue.Text("sample.Payload:value"),
                    decodeContext(target.key, storage = ValueStorage.SERIALIZED)
                )
            )
            assertFailsWith<StaticValueCodecException> {
                KronosValueCodecs.decode(
                    KronosPhysicalValue.Text("sample.Payload:value"),
                    StaticValueCodecContext(
                        direction = ValueCodecDirection.DECODE,
                        origin = ValueCodecOrigin.PARAMETER,
                        targetType = target,
                        storage = ValueStorage.SERIALIZED
                    )
                )
            }
        } finally {
            serialized.close()
        }

        val invalid = KronosValueCodecs.register(staticValueCodec(
            supports = { _, context -> context.storage == ValueStorage.SERIALIZED },
            convert = { _, _ -> 7 }
        ))
        try {
            val field = testField(serializable = true)
            val failure = assertFailsWith<StaticValueCodecException> {
                KronosValueCodecs.encode(
                    Payload("value"),
                    encodeContext(target.key, storage = ValueStorage.SERIALIZED, field = field)
                )
            }
            assertTrue(failure.message.orEmpty().contains("SERIALIZED"))
            assertTrue(failure.message.orEmpty().contains("ENCODE/PARAMETER"))
            assertEquals("payload", failure.fieldName)
            assertEquals("payload_text", failure.columnName)
        } finally {
            invalid.close()
        }

        val nullDecoder = KronosValueCodecs.register(staticValueCodec(
            supports = { _, context -> context.direction == ValueCodecDirection.DECODE && context.targetType == target },
            convert = { _, _ -> null }
        ))
        try {
            val failure = assertFailsWith<StaticValueCodecException> {
                KronosValueCodecs.decode(
                    KronosPhysicalValue.Text("input"),
                    decodeContext(target.key, field = testField(serializable = false))
                )
            }
            assertTrue(failure.message.orEmpty().contains("conversion produced null"))
            assertEquals("payload", failure.fieldName)
            assertEquals("payload_text", failure.columnName)
        } finally {
            nullDecoder.close()
        }
    }

    @Test
    fun `enum name and ordinal decoding use explicit generated metadata`() {
        val type = StaticTypeRef("sample.Status", nullable = false)
        val typeInfo = StaticTypeInfo(type, enumKey = type.key)
        val registration = KronosValueCodecs.registerEnum(object : StaticEnumValueCodec {
            override val type: StaticTypeRef = type
            override val entryNames: List<String> = Status.entries.map(Status::name)

            override fun decode(name: String): Enum<*>? = Status.entries.firstOrNull { it.name == name }
        })
        try {
            assertEquals(
                KronosPhysicalValue.Text("READY"),
                KronosValueCodecs.encode(Status.READY, encodeContext(type.key, typeInfo = typeInfo))
            )
            assertEquals(
                Status.READY,
                KronosValueCodecs.decode(KronosPhysicalValue.Text("READY"), decodeContext(type.key, typeInfo = typeInfo))
            )

            val ordinalEncode = encodeContext(type.key, typeInfo = typeInfo, enumStorage = StaticEnumStorage.ORDINAL)
            val ordinalDecode = decodeContext(type.key, typeInfo = typeInfo, enumStorage = StaticEnumStorage.ORDINAL)
            assertEquals(KronosPhysicalValue.Integer(1), KronosValueCodecs.encode(Status.CLOSED, ordinalEncode))
            assertEquals(Status.CLOSED, KronosValueCodecs.decode(KronosPhysicalValue.Integer(1), ordinalDecode))

            val failure = assertFailsWith<StaticValueCodecException> {
                KronosValueCodecs.decode(KronosPhysicalValue.Text("MISSING"), decodeContext(type.key, typeInfo = typeInfo))
            }
            assertTrue(failure.message.orEmpty().contains("unknown enum name 'MISSING'"))
        } finally {
            registration.close()
        }
    }

    private fun encodeContext(
        key: String,
        nullable: Boolean = false,
        storage: ValueStorage = ValueStorage.NONE,
        typeInfo: StaticTypeInfo? = null,
        enumStorage: StaticEnumStorage = StaticEnumStorage.NAME,
        field: ValueCodecField? = null
    ): StaticValueCodecContext = StaticValueCodecContext(
        direction = ValueCodecDirection.ENCODE,
        origin = ValueCodecOrigin.PARAMETER,
        targetType = StaticTypeRef(key, nullable),
        field = field,
        storage = storage,
        targetTypeInfo = typeInfo,
        enumStorage = enumStorage
    )

    private fun decodeContext(
        key: String,
        nullable: Boolean = false,
        storage: ValueStorage = ValueStorage.NONE,
        typeInfo: StaticTypeInfo? = null,
        enumStorage: StaticEnumStorage = StaticEnumStorage.NAME,
        field: ValueCodecField? = null
    ): StaticValueCodecContext = StaticValueCodecContext(
        direction = ValueCodecDirection.DECODE,
        origin = ValueCodecOrigin.DATABASE,
        targetType = StaticTypeRef(key, nullable),
        field = field,
        storage = storage,
        targetTypeInfo = typeInfo,
        enumStorage = enumStorage
    )

    private fun testField(serializable: Boolean): ValueCodecField = object : ValueCodecField {
        override val columnName: String = "payload_text"
        override val name: String = "payload"
        override val type: KColumnType = KColumnType.TEXT
        override val dateFormat: String? = null
        override val nullable: Boolean = false
        override val serializable: Boolean = serializable
        override val typeInfo: StaticTypeInfo? = null
    }
}
