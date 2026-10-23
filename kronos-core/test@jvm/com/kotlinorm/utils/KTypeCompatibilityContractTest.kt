@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.utils

import com.kotlinorm.exceptions.UnsupportedType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.toStaticTypeRef
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KTypeCompatibilityContractTest {
    private enum class Status { OPEN }

    @Test
    fun foreignClassifierAndClassCastFailuresFailClosed() {
        val unknown = ForeignType(null)
        assertFalse(unknown.isStructurallyAssignableTo(typeOf<String>()))
        assertFalse(typeOf<String>().isStructurallyAssignableTo(unknown))
        val foreign = object : KClassifier { override fun toString() = "foreign classifier" }
        assertFalse(ForeignType(foreign).isKPojoType())
        val invalid = object : KType {
            override val annotations = emptyList<Annotation>()
            override val arguments = emptyList<KTypeProjection>()
            override val isMarkedNullable = false
            override val classifier: KClassifier get() = throw ClassCastException("foreign classifier unavailable")
        }
        assertFalse(invalid.isStructurallyAssignableTo(typeOf<String>()))
    }

    @Test
    fun recursiveTypeParameterBoundsTerminateAndRecognizeKPojoBounds() {
        val parameter = Parameter("Recursive")
        parameter.bounds = listOf(ForeignType(parameter))
        assertFalse(ForeignType(parameter).isKPojoType())
        parameter.bounds = listOf(ForeignType(null), typeOf<KPojo>())
        assertTrue(ForeignType(parameter).isKPojoType())
    }

    @Test
    fun stableKeysPreserveProjectionDirectionsStarsAndNestedNullability() {
        val key = KTypeKey(String::class, true, listOf(
            KTypeArgumentKey(KVariance.IN, KTypeKey(Int::class, true, emptyList())),
            KTypeArgumentKey(KVariance.OUT, KTypeKey(Long::class, false, emptyList())),
            KTypeArgumentKey(KVariance.INVARIANT, KTypeKey(Boolean::class, false, emptyList())),
            KTypeArgumentKey(null, null)
        ))
        assertEquals("kotlin.String<in kotlin.Int?, out kotlin.Long, kotlin.Boolean, *>?", key.stableSignature())
        assertEquals("*", KTypeKey(null, false, emptyList()).stableSignature())
        assertEquals("foreign", KTypeKey(object : KClassifier { override fun toString() = "foreign" }, false, emptyList()).stableSignature())
    }

    @Test
    fun descriptorFallbackKeepsUseSiteVarianceAndUsesCompilerMetadataWhenProvided() {
        assertEquals(StaticTypeVariance.IN, typeOf<MutableList<in String>>().toStaticTypeRef().arguments.single().variance)
        assertEquals(StaticTypeVariance.OUT, typeOf<MutableList<out String>>().toStaticTypeRef().arguments.single().variance)
        assertEquals(StaticTypeVariance.STAR, typeOf<List<*>>().toStaticTypeRef().arguments.single().variance)
        val supplied = StaticTypeInfo(StaticTypeRef("authoritative.Type", false))
        assertEquals(StaticTypeRef("authoritative.Type", true), typeOf<String?>().toStaticTypeRef(supplied))
        assertEquals("foreign type", ForeignType(null).toStaticTypeRef().key)
        val blank = ForeignType(null, description = "")
        assertEquals("<unknown>", blank.toStaticTypeRef().key)
    }

    @Test
    fun factoryAndEnumValidationRejectNonConcreteAndGenericKeys() {
        assertFailsWith<UnsupportedType> { ForeignType(null).normalizedKPojoType() }
        assertFailsWith<UnsupportedType> { typeOf<String>().normalizedKPojoType() }
        assertFailsWith<UnsupportedType> { ForeignType(KPojo::class, listOf(KTypeProjection.STAR)).normalizedKPojoType() }
        assertFailsWith<IllegalArgumentException> { ForeignType(null).normalizedEnumType() }
        assertFailsWith<IllegalArgumentException> { typeOf<String>().normalizedEnumType() }
        assertFailsWith<IllegalArgumentException> { ForeignType(Status::class, listOf(KTypeProjection.STAR)).normalizedEnumType() }
        assertEquals(KTypeKey.from(typeOf<Status>()), typeOf<Status?>().normalizedEnumType())
    }

    private data class ForeignType(override val classifier: KClassifier?, override val arguments: List<KTypeProjection> = emptyList(),
        override val isMarkedNullable: Boolean = false, val description: String = "foreign type") : KType {
        override val annotations = emptyList<Annotation>()
        override fun toString() = description
    }
    private class Parameter(override val name: String) : KTypeParameter {
        var bounds: List<KType> = emptyList()
        override val upperBounds get() = bounds
        override val variance = KVariance.INVARIANT
        override val isReified = false
    }
}
