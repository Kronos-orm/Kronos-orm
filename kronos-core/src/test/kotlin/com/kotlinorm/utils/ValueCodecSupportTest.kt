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

package com.kotlinorm.utils

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.codec.accepts
import com.kotlinorm.utils.codec.isBindableScalar
import com.kotlinorm.utils.codec.isConcreteEnumType
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValueCodecSupportTest {
    private class Entity : KPojo

    private enum class Status {
        READY
    }

    @Test
    fun `runtime validation preserves nested map iterable and array KTypes`() {
        val nestedMapType = typeOf<Map<String, List<Int?>>>()

        assertTrue(nestedMapType.accepts(mapOf("items" to listOf(1, null))))
        assertFalse(nestedMapType.accepts(mapOf(1 to listOf(1))))
        assertFalse(nestedMapType.accepts(mapOf("items" to listOf("1"))))
        assertTrue(typeOf<Map<*, *>>().accepts(mapOf(1 to "raw")))

        assertTrue(typeOf<List<String?>>().accepts(listOf("value", null)))
        assertFalse(typeOf<List<String>>().accepts(listOf("value", null)))
        assertTrue(typeOf<List<*>>().accepts(listOf(1, "raw")))

        assertTrue(typeOf<Array<String?>>().accepts(arrayOf("value", null)))
        assertFalse(typeOf<Array<String>>().accepts(arrayOf("value", null)))
        assertTrue(typeOf<Array<*>>().accepts(arrayOf(1, "raw")))
    }

    @Test
    fun `declared source KType participates after runtime validation`() {
        assertTrue(
            typeOf<List<CharSequence>>().accepts(
                listOf("value"),
                typeOf<List<String>>()
            )
        )
        assertFalse(
            typeOf<List<String>>().accepts(
                listOf("value"),
                typeOf<List<Any>>()
            )
        )
        assertFalse(typeOf<String>().accepts(1))
    }

    @Test
    fun `database binding rejects logical containers while retaining binary scalars`() {
        val rejected = listOf<Any>(
            Entity(),
            listOf(1),
            mapOf("id" to 1),
            sequenceOf(1),
            Status.READY,
            Unit,
            arrayOf(1),
            booleanArrayOf(true),
            charArrayOf('a'),
            doubleArrayOf(1.0),
            floatArrayOf(1F),
            intArrayOf(1),
            longArrayOf(1L),
            shortArrayOf(1)
        )

        rejected.forEach { value -> assertFalse(value.isBindableScalar()) }
        assertTrue(byteArrayOf(1).isBindableScalar())
        assertTrue("value".isBindableScalar())
    }

    @Test
    fun `enum recognition requires a concrete non-generic KType`() {
        assertTrue(typeOf<Status>().isConcreteEnumType())
        assertTrue(typeOf<Status?>().isConcreteEnumType())
        assertFalse(
            SyntheticKType(
                classifier = Status::class,
                arguments = listOf(KTypeProjection.STAR)
            ).isConcreteEnumType()
        )
        assertFalse(SyntheticKType(classifier = null).isConcreteEnumType())
    }

    private data class SyntheticKType(
        override val classifier: KClassifier?,
        override val arguments: List<KTypeProjection> = emptyList(),
        override val isMarkedNullable: Boolean = false,
        override val annotations: List<Annotation> = emptyList()
    ) : KType
}
