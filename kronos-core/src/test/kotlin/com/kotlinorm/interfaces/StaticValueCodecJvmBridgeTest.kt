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

import com.kotlinorm.Kronos
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.utils.StaticTypeRef
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.codec.ValueConversionRequest
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StaticValueCodecJvmBridgeTest {
    @Test
    fun `legacy and static codecs use one newest first registration ledger`() {
        val legacy = Kronos.registerValueCodec(valueCodec(
            supports = { _, context -> context.targetType == typeOf<String>() },
            convert = { _, _ -> "legacy" }
        ))
        val static = KronosValueCodecs.register(staticValueCodec(
            supports = { _, context ->
                context.direction == ValueCodecDirection.DECODE &&
                    context.targetType == StaticTypeRef("kotlin.String", nullable = false)
            },
            convert = { _, _ -> "static" }
        ))

        try {
            assertEquals("static", decodeString())
            static.close()
            assertEquals("legacy", decodeString())
        } finally {
            static.close()
            legacy.close()
        }
    }

    @Test
    fun `legacy supports failures retain the original error and codec context`() {
        val cause = IllegalStateException("supports failed")
        val legacy = Kronos.registerValueCodec(valueCodec(
            supports = { _, _ -> throw cause },
            convert = { _, _ -> error("convert must not run") }
        ))
        try {
            val failure = assertFailsWith<ValueMappingException> { decodeString() }
            assertSame(cause, failure.cause)
            assertTrue(failure.message.orEmpty().contains("user ValueCodec"))
            assertTrue(failure.message.orEmpty().contains("failed in supports"))
        } finally {
            legacy.close()
        }
    }

    private fun decodeString(): Any? = ValueCodecRegistry.convert(
        ValueConversionRequest(
            value = 1,
            direction = ValueCodecDirection.DECODE,
            origin = ValueCodecOrigin.MAP,
            targetType = typeOf<String>()
        )
    )
}
