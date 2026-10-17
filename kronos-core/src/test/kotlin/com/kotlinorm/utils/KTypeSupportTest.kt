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
    fun `static relation preserves root nullability`() {
        val nonNull = typeInfo("example.String")
        val nullable = typeInfo("example.String", nullable = true)

        assertTrue(assignable(nonNull, nullable, nonNull, nullable))
        assertFalse(assignable(nullable, nonNull, nonNull, nullable))
    }

    @Test
    fun `static relation follows a substituted direct supertype`() {
        val string = typeRef("example.String")
        val child = typeInfo(
            "example.Child",
            arguments = listOf(invariant(string)),
            directSupertypes = listOf(typeRef("example.Parent", arguments = listOf(invariant(string))))
        )
        val parent = typeInfo("example.Parent", arguments = listOf(invariant(string)))

        assertTrue(assignable(child, parent, child, parent))
    }

    @Test
    fun `static relation honors declaration-site covariance`() {
        val string = typeInfo(
            "example.String",
            directSupertypes = listOf(typeRef("kotlin.CharSequence"))
        )
        val charSequence = typeInfo("kotlin.CharSequence")
        val source = typeInfo(
            "example.Box",
            arguments = listOf(invariant(string.type)),
            declarationVariances = listOf(StaticTypeVariance.OUT)
        )
        val target = typeInfo(
            "example.Box",
            arguments = listOf(invariant(charSequence.type)),
            declarationVariances = listOf(StaticTypeVariance.OUT)
        )

        assertTrue(assignable(source, target, source, target, string, charSequence))
        assertFalse(assignable(target, source, source, target, string, charSequence))
    }

    @Test
    fun `static relation honors declaration-site contravariance`() {
        val string = typeInfo(
            "example.String",
            directSupertypes = listOf(typeRef("kotlin.Any"))
        )
        val any = typeInfo("kotlin.Any")
        val source = typeInfo(
            "example.Sink",
            arguments = listOf(invariant(any.type)),
            declarationVariances = listOf(StaticTypeVariance.IN)
        )
        val target = typeInfo(
            "example.Sink",
            arguments = listOf(invariant(string.type)),
            declarationVariances = listOf(StaticTypeVariance.IN)
        )

        assertTrue(assignable(source, target, source, target, string, any))
        assertFalse(assignable(target, source, source, target, string, any))
    }

    @Test
    fun `static relation accepts a target star projection`() {
        val source = typeInfo(
            "example.Box",
            arguments = listOf(invariant(typeRef("example.String"))),
            declarationVariances = listOf(StaticTypeVariance.OUT)
        )
        val target = typeInfo(
            "example.Box",
            arguments = listOf(StaticTypeArgument(StaticTypeVariance.STAR))
        )

        assertTrue(assignable(source, target, source, target))
    }

    @Test
    fun `static relation fails closed when an intermediate descriptor is absent`() {
        val source = typeInfo(
            "example.Child",
            directSupertypes = listOf(typeRef("example.Parent"))
        )
        val target = typeInfo("example.Grandparent")

        assertFalse(assignable(source, target, source, target))
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

    private fun assignable(
        source: StaticTypeInfo,
        target: StaticTypeInfo,
        vararg available: StaticTypeInfo
    ): Boolean {
        val descriptors = available.associateBy { it.type.withoutTopLevelNullability() }
        return source.isStructurallyAssignableTo(target) { type ->
            descriptors[type.withoutTopLevelNullability()]
        }
    }

    private fun typeInfo(
        key: String,
        nullable: Boolean = false,
        arguments: List<StaticTypeArgument> = emptyList(),
        directSupertypes: List<StaticTypeRef> = emptyList(),
        declarationVariances: List<StaticTypeVariance> = emptyList()
    ): StaticTypeInfo = StaticTypeInfo(
        typeRef(key, nullable, arguments),
        directSupertypes = directSupertypes,
        declarationVariances = declarationVariances
    )

    private fun typeRef(
        key: String,
        nullable: Boolean = false,
        arguments: List<StaticTypeArgument> = emptyList()
    ): StaticTypeRef = StaticTypeRef(key, nullable, arguments)

    private fun invariant(type: StaticTypeRef): StaticTypeArgument =
        StaticTypeArgument(StaticTypeVariance.INVARIANT, type)

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

}
