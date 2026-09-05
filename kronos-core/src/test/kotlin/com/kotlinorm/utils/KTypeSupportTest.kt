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

import java.util.concurrent.CancellationException
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KTypeSupportTest {
    @Test
    fun `assignability honors declaration-site covariance`() {
        assertTrue(typeOf<List<String>>().isStructurallyAssignableTo(typeOf<List<CharSequence>>()))
        assertFalse(typeOf<List<CharSequence>>().isStructurallyAssignableTo(typeOf<List<String>>()))
    }

    @Test
    fun `assignability honors declaration-site contravariance`() {
        assertTrue(typeOf<Contravariant<Any>>().isStructurallyAssignableTo(typeOf<Contravariant<String>>()))
        assertFalse(typeOf<Contravariant<String>>().isStructurallyAssignableTo(typeOf<Contravariant<Any>>()))
    }

    @Test
    fun `assignability conservatively follows reflected use-site projections`() {
        val covariantTarget = MutableList::class.createType(
            listOf(KTypeProjection.covariant(typeOf<CharSequence>()))
        )
        val contravariantTarget = MutableList::class.createType(
            listOf(KTypeProjection.contravariant(typeOf<String>()))
        )

        assertTrue(typeOf<MutableList<String>>().isStructurallyAssignableTo(covariantTarget))
        assertFalse(typeOf<MutableList<CharSequence>>().isStructurallyAssignableTo(typeOf<MutableList<String>>()))
        // Kotlin/JVM reflection folds MutableList's classifier and declaration variance;
        // conservatively reject this valid relationship until generic support is explicit.
        assertFalse(typeOf<MutableList<Any>>().isStructurallyAssignableTo(contravariantTarget))
        assertFalse(typeOf<MutableList<String>>().isStructurallyAssignableTo(typeOf<MutableList<Any>>()))
    }

    @Test
    fun `assignability honors root and nested star projections`() {
        assertTrue(typeOf<List<String>>().isStructurallyAssignableTo(typeOf<List<*>>()))
        assertFalse(typeOf<List<*>>().isStructurallyAssignableTo(typeOf<List<String>>()))
        assertTrue(
            typeOf<Map<String, List<Int>>>()
                .isStructurallyAssignableTo(typeOf<Map<String, List<*>>>())
        )
        assertFalse(
            typeOf<Map<String, List<*>>>()
                .isStructurallyAssignableTo(typeOf<Map<String, List<Int>>>())
        )
    }

    @Test
    fun `assignability preserves top-level and nested nullability`() {
        assertTrue(typeOf<String>().isStructurallyAssignableTo(typeOf<String?>()))
        assertFalse(typeOf<String?>().isStructurallyAssignableTo(typeOf<String>()))
        assertTrue(typeOf<List<String>>().isStructurallyAssignableTo(typeOf<List<String?>>()))
        assertFalse(typeOf<List<String?>>().isStructurallyAssignableTo(typeOf<List<String>>()))
        assertTrue(
            typeOf<Map<String, List<Int>>>()
                .isStructurallyAssignableTo(typeOf<Map<String, List<Int?>>?>())
        )
        assertFalse(
            typeOf<Map<String, List<Int?>>?>()
                .isStructurallyAssignableTo(typeOf<Map<String, List<Int>>>())
        )
    }

    @Test
    fun `assignability normalizes only equivalent JVM primitive boxing`() {
        val boxedInt = java.lang.Integer::class.createType()
        val nullableBoxedInt = java.lang.Integer::class.createType(nullable = true)

        assertTrue(boxedInt.isStructurallyAssignableTo(typeOf<Int>()))
        assertTrue(typeOf<Int>().isStructurallyAssignableTo(boxedInt))
        assertFalse(nullableBoxedInt.isStructurallyAssignableTo(typeOf<Int>()))
        assertFalse(boxedInt.isStructurallyAssignableTo(typeOf<Long>()))
        assertTrue(typeOf<Int>().isStructurallyAssignableTo(typeOf<Number>()))
    }

    @Test
    fun `synthetic KType uses primitive fallback without reflection failure`() {
        val syntheticInt = SyntheticKType(Int::class)

        assertTrue(syntheticInt.isStructurallyAssignableTo(typeOf<Int>()))
        assertTrue(typeOf<Int>().isStructurallyAssignableTo(syntheticInt))
    }

    @Test
    fun `foreign generic and incompatible KTypes return false without failing`() {
        val syntheticString = SyntheticKType(String::class)
        val syntheticList = SyntheticKType(
            List::class,
            listOf(KTypeProjection.STAR)
        )

        assertFalse(syntheticString.isStructurallyAssignableTo(typeOf<Int>()))
        assertFalse(syntheticList.isStructurallyAssignableTo(typeOf<List<String>>()))
    }

    @Test
    fun `unavailable foreign type metadata returns false`() {
        assertFalse(
            FailingClassifierKType(NoClassDefFoundError("optional type unavailable"))
                .isStructurallyAssignableTo(typeOf<Int>())
        )
        assertFalse(
            FailingClassifierKType(IllegalArgumentException("unsupported foreign KType"))
                .isStructurallyAssignableTo(typeOf<Int>())
        )
    }

    @Test
    fun `assignability does not swallow cancellation or unrelated Error`() {
        val cancellation = CancellationException("type resolution cancelled")
        val assertion = AssertionError("type resolution invariant failed")

        val cancellationFailure = assertFailsWith<CancellationException> {
            FailingClassifierKType(cancellation).isStructurallyAssignableTo(typeOf<Int>())
        }
        val assertionFailure = assertFailsWith<AssertionError> {
            FailingClassifierKType(assertion).isStructurallyAssignableTo(typeOf<Int>())
        }

        assertSame(cancellation, cancellationFailure)
        assertSame(assertion, assertionFailure)
    }

    private data class SyntheticKType(
        override val classifier: KClassifier?,
        override val arguments: List<KTypeProjection> = emptyList(),
        override val isMarkedNullable: Boolean = false,
        override val annotations: List<Annotation> = emptyList()
    ) : KType

    private class FailingClassifierKType(private val failure: Throwable) : KType {
        override val classifier: KClassifier?
            get() = throw failure
        override val arguments: List<KTypeProjection> = emptyList()
        override val isMarkedNullable: Boolean = false
        override val annotations: List<Annotation> = emptyList()
    }

    private interface Contravariant<in T>
}
