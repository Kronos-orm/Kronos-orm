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

import com.kotlinorm.Kronos
import com.kotlinorm.cache.kPojoInstanceCache
import com.kotlinorm.exceptions.InvalidKPojoFactoryResult
import com.kotlinorm.exceptions.KPojoConstructionException
import com.kotlinorm.exceptions.MissingKPojoFactory
import com.kotlinorm.exceptions.UnsupportedType
import com.kotlinorm.interfaces.KPojo
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KPojoFactoryRegistryTest {
    data class ExactUser(var id: Int? = null) : KPojo
    data class GeneratedUser(var id: Int? = null) : KPojo
    data class LayeredUser(var id: Int? = null) : KPojo
    data class ThrowingUser(var id: Int? = null) : KPojo
    data class ExpectedUser(var id: Int? = null) : KPojo
    data class WrongUser(var id: Int? = null) : KPojo
    data class MissingUser(var id: Int) : KPojo
    data class CachedUser(var id: Int? = null) : KPojo
    interface IndirectKPojo : KPojo
    data class IndirectUser(var id: Int? = null) : IndirectKPojo
    private class DirectKPojoBound<T : KPojo>
    private class IndirectKPojoBound<Base : KPojo, T : Base>
    private class UnboundedTypeParameter<T>
    private class AnyBoundTypeParameter<T : Any>
    private class FirstTypeParameterOwner<T>
    private class SecondTypeParameterOwner<T>
    private data class PlainThirdPartyType(val id: Int)

    @Test
    fun `KPojo type detection follows the complete declared KType graph`() {
        assertTrue(typeOf<KPojo>().isKPojoType())
        assertTrue(typeOf<ExactUser>().isKPojoType())
        assertTrue(typeOf<IndirectUser?>().isKPojoType())
        assertFalse(typeOf<String>().isKPojoType())
        assertFalse(typeOf<List<ExactUser>>().isKPojoType())
    }

    @Test
    fun `KPojo type detection follows direct and indirect type parameter bounds`() {
        val directBound = DirectKPojoBound::class.typeParameters.single().createType()
        val indirectBound = IndirectKPojoBound::class.typeParameters.last().createType()
        val unbounded = UnboundedTypeParameter::class.typeParameters.single().createType()
        val anyBound = AnyBoundTypeParameter::class.typeParameters.single().createType()
        val nothingType = SyntheticKType(Nothing::class, emptyList())

        assertTrue(directBound.isKPojoType())
        assertTrue(indirectBound.isKPojoType())
        assertFalse(unbounded.isKPojoType())
        assertFalse(anyBound.isKPojoType())
        assertFalse(nothingType.isKPojoType())
    }

    @Test
    fun `factory still requires a concrete non-generic KPojo type`() {
        val typeParameter = DirectKPojoBound::class.typeParameters.single().createType()
        val genericType = SyntheticKType(
            classifier = ExactUser::class,
            arguments = listOf(KTypeProjection.invariant(typeOf<String>()))
        )

        assertFailsWith<UnsupportedType> {
            Kronos.registerKPojoFactory(typeParameter, KPojoFactory { ExactUser() })
        }
        assertFailsWith<UnsupportedType> {
            Kronos.registerKPojoFactory(genericType, KPojoFactory { ExactUser() })
        }
    }

    @Test
    fun `registering a factory cannot turn an ordinary class into a KPojo`() {
        val type = typeOf<PlainThirdPartyType>()
        var factoryCalls = 0

        val failure = assertFailsWith<UnsupportedType> {
            Kronos.registerKPojoFactory(type, KPojoFactory {
                factoryCalls += 1
                ExactUser()
            })
        }

        assertEquals(type, failure.targetType)
        assertEquals(
            "Unsupported KPojo type $type: classifier must implement KPojo",
            failure.message
        )
        assertEquals(0, factoryCalls)
    }

    @Test
    fun `factory normalization accepts an indirect KPojo KType`() {
        val type = typeOf<IndirectUser>()
        val registration = Kronos.registerKPojoFactory(type, KPojoFactory { IndirectUser(7) })

        try {
            assertEquals(IndirectUser(7), Kronos.createKPojo(type))
        } finally {
            registration.close()
        }
    }

    @Test
    fun `user factory overrides generated default and close restores it`() {
        val type = typeOf<GeneratedUser>()
        assertEquals(GeneratedUser(), Kronos.createKPojo(type))
        val registration = Kronos.registerKPojoFactory(type, KPojoFactory { GeneratedUser(8) })

        try {
            assertEquals(GeneratedUser(8), Kronos.createKPojo(type))
        } finally {
            registration.close()
        }

        assertEquals(GeneratedUser(), Kronos.createKPojo(type))
    }

    @Test
    fun `reified factory registration uses the exact KType and restores generated metadata`() {
        val registration = registerKPojo { GeneratedUser(12) }

        try {
            assertEquals(GeneratedUser(12), createKPojo<GeneratedUser>())
        } finally {
            registration.close()
        }

        assertEquals(GeneratedUser(), createKPojo<GeneratedUser>())
    }

    @Test
    fun `factory registration uses exact KType and receives the requested type`() {
        val registeredType = typeOf<ExactUser>()
        val requestedType = typeOf<ExactUser?>()
        var receivedType: KType? = null
        val registration = Kronos.registerKPojoFactory(registeredType, KPojoFactory { type ->
            receivedType = type
            ExactUser(7)
        })

        try {
            assertEquals(ExactUser(7), Kronos.createKPojo(requestedType))
            assertEquals(requestedType, receivedType)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `last active factory wins and registrations can close out of order`() {
        val type = typeOf<LayeredUser>()
        val first = Kronos.registerKPojoFactory(type, KPojoFactory { LayeredUser(1) })
        val middle = Kronos.registerKPojoFactory(type, KPojoFactory { LayeredUser(2) })
        val latest = Kronos.registerKPojoFactory(type, KPojoFactory { LayeredUser(3) })

        try {
            assertEquals(LayeredUser(3), Kronos.createKPojo(type))
            middle.close()
            assertEquals(LayeredUser(3), Kronos.createKPojo(type))
            latest.close()
            assertEquals(LayeredUser(1), Kronos.createKPojo(type))
            middle.close()
        } finally {
            latest.close()
            middle.close()
            first.close()
        }
    }

    @Test
    fun `missing generated and user factory has a specific error`() {
        val type = typeOf<MissingUser>()

        val failure = assertFailsWith<MissingKPojoFactory> { Kronos.createKPojo(type) }

        assertEquals(type, failure.targetType)
    }

    @Test
    fun `factory exceptions retain the requested type and cause`() {
        val type = typeOf<ThrowingUser>()
        val cause = IllegalArgumentException("factory failed")
        val registration = Kronos.registerKPojoFactory(type, KPojoFactory { throw cause })

        try {
            val failure = assertFailsWith<KPojoConstructionException> { Kronos.createKPojo(type) }
            assertEquals(type, failure.targetType)
            assertSame(cause, failure.cause)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `factory result complete KType must match requested KType`() {
        val type = typeOf<ExpectedUser>()
        val registration = Kronos.registerKPojoFactory(type, KPojoFactory { WrongUser(9) })

        try {
            val failure = assertFailsWith<InvalidKPojoFactoryResult> { Kronos.createKPojo(type) }
            assertEquals(type, failure.targetType)
            assertEquals(typeOf<WrongUser>(), failure.actualType)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `factory propagates Error without wrapping`() {
        val type = typeOf<ThrowingUser>()
        val cause = AssertionError("factory invariant failed")
        val registration = Kronos.registerKPojoFactory(type, KPojoFactory { throw cause })

        try {
            val failure = assertFailsWith<AssertionError> { Kronos.createKPojo(type) }
            assertSame(cause, failure)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `factory propagates cancellation without wrapping`() {
        val type = typeOf<ThrowingUser>()
        val cause = CancellationException("factory cancelled")
        val registration = Kronos.registerKPojoFactory(type, KPojoFactory { throw cause })

        try {
            val failure = assertFailsWith<CancellationException> { Kronos.createKPojo(type) }
            assertSame(cause, failure)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `factory is invoked for every fresh instance request`() {
        val type = typeOf<ExactUser>()
        var sequence = 0
        val registration = Kronos.registerKPojoFactory(type, KPojoFactory { ExactUser(++sequence) })

        try {
            val first = Kronos.createKPojo(type)
            val second = Kronos.createKPojo(type)
            assertNotSame(first, second)
            assertEquals(ExactUser(1), first)
            assertEquals(ExactUser(2), second)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `factory result rejects a structural KType mismatch with the same classifier`() {
        val requestedType = typeOf<ExpectedUser>()
        val actualType = SyntheticKType(
            classifier = ExpectedUser::class,
            arguments = listOf(KTypeProjection.invariant(typeOf<String>()))
        )
        val registration = Kronos.registerKPojoFactory(requestedType, KPojoFactory {
            ExpectedUser(9).apply { __kType = actualType }
        })

        try {
            val failure = assertFailsWith<InvalidKPojoFactoryResult> {
                Kronos.createKPojo(requestedType)
            }
            assertEquals(requestedType, failure.targetType)
            assertSame(actualType, failure.actualType)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `metadata cache normalizes nullable KType and invokes its factory once`() {
        val calls = AtomicInteger()
        val registration = Kronos.registerKPojoFactory(typeOf<CachedUser>(), KPojoFactory {
            calls.incrementAndGet()
            CachedUser(11)
        })

        try {
            val nullable = kPojoInstanceCache[typeOf<CachedUser?>()]
            val nonNull = kPojoInstanceCache[typeOf<CachedUser>()]
            assertSame(nullable, nonNull)
            assertEquals(CachedUser(11), nonNull)
            assertEquals(1, calls.get())
        } finally {
            registration.close()
        }
    }

    @Test
    fun `KTypeKey preserves generic structure and only ignores top-level nullability`() {
        val contravariantList = SyntheticKType(
            classifier = MutableList::class,
            arguments = listOf(KTypeProjection.contravariant(typeOf<String>()))
        )
        val covariantList = SyntheticKType(
            classifier = MutableList::class,
            arguments = listOf(KTypeProjection.covariant(typeOf<String>()))
        )
        val firstTypeParameter = FirstTypeParameterOwner::class.typeParameters.single().createType()
        val secondTypeParameter = SecondTypeParameterOwner::class.typeParameters.single().createType()

        assertNotEquals(
            KTypeKey.from(typeOf<List<*>>()),
            KTypeKey.from(typeOf<List<String>>())
        )
        assertNotEquals(
            KTypeKey.from(typeOf<Map<String, List<*>>>()),
            KTypeKey.from(typeOf<Map<String, List<String>>>()),
        )
        assertNotEquals(
            KTypeKey.from(typeOf<List<String>>()),
            KTypeKey.from(typeOf<List<Int>>())
        )
        assertNotEquals(
            KTypeKey.from(typeOf<List<String?>>(), ignoreTopLevelNullability = true),
            KTypeKey.from(typeOf<List<String>>(), ignoreTopLevelNullability = true),
        )
        assertEquals(
            KTypeKey.from(typeOf<List<*>?>(), ignoreTopLevelNullability = true),
            KTypeKey.from(typeOf<List<*>>(), ignoreTopLevelNullability = true),
        )
        assertNotEquals(KTypeKey.from(contravariantList), KTypeKey.from(covariantList))
        assertNotEquals(KTypeKey.from(firstTypeParameter), KTypeKey.from(secondTypeParameter))
    }

    private data class SyntheticKType(
        override val classifier: KClassifier?,
        override val arguments: List<KTypeProjection>,
        override val isMarkedNullable: Boolean = false,
        override val annotations: List<Annotation> = emptyList()
    ) : KType
}
