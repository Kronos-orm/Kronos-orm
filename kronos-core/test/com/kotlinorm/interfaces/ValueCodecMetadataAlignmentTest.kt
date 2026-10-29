@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.interfaces

import com.kotlinorm.Kronos
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.ResultColumnMetadata
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.utils.StaticContainerKind
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.utils.StaticTypeRef
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.codec.ValueConversionRequest
import com.kotlinorm.wrappers.resultColumnForType
import com.kotlinorm.wrappers.withTargetType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class ValueCodecMetadataAlignmentTest {
    @Test
    fun explicitScalarRetargetingReplacesExpressionUpperBoundHintsOnEveryFacadeOrigin() {
        val number = StaticTypeInfo(StaticTypeRef("kotlin.Number", true))
        val source = Field("total", kType = typeOf<Number?>(), typeInfo = number)
        val declared = ResultColumnMetadata(typeOf<Number?>(), source)
        for (origin in ValueCodecOrigin.entries) {
            val integer = declared.withTargetType(typeOf<Int?>())
            assertNull(integer.typeInfo)
            assertSame(source, integer.field)
            assertEquals(4, ValueCodecRegistry.convert(ValueConversionRequest(4L, ValueCodecDirection.DECODE,
                origin, integer.type, field = integer.field, targetTypeInfo = integer.typeInfo, strict = false)))
            val long = declared.withTargetType(typeOf<Long?>())
            assertEquals(65L, ValueCodecRegistry.convert(ValueConversionRequest(65, ValueCodecDirection.DECODE,
                origin, long.type, field = long.field, targetTypeInfo = long.typeInfo, strict = false)))
        }
    }

    @Test
    fun unchangedAndExplicitDestinationMetadataRetainAuthoritativeOpaqueKeys() {
        val sourceInfo = StaticTypeInfo(StaticTypeRef("sample.Number", true))
        val targetInfo = StaticTypeInfo(StaticTypeRef("authoritative.Int", true))
        val source = Field("total", kType = typeOf<Number?>(), typeInfo = sourceInfo, dateFormat = "source-policy")
        val declared = ResultColumnMetadata(typeOf<Number?>(), source)
        assertSame(sourceInfo, declared.withTargetType(typeOf<Number?>()).typeInfo)
        val redirected = declared.withTargetType(typeOf<Int?>(), targetInfo)
        assertSame(targetInfo, redirected.typeInfo)
        assertEquals(targetInfo.type, redirected.type.toStaticTypeRef(redirected.typeInfo))
        assertSame(source, redirected.field)
        assertEquals("source-policy", redirected.field?.dateFormat)
        assertSame(sourceInfo, redirected.field?.typeInfo)
        assertSame(targetInfo, redirected.withTargetType(typeOf<Int?>()).typeInfo)
        assertSame(sourceInfo, resultColumnForType(typeOf<Any?>(), declared, "alias")?.typeInfo)
    }

    @Test
    fun genericRetargetingClearsThePreviousFullShapeButKeepsExplicitDestinationMetadata() {
        val strings = StaticTypeInfo(typeOf<List<String?>>().toStaticTypeRef(), containerKind = StaticContainerKind.ITERABLE,
            elementType = StaticTypeRef("kotlin.String", true))
        val ints = StaticTypeInfo(typeOf<List<Int>>().toStaticTypeRef(), containerKind = StaticContainerKind.ITERABLE,
            elementType = StaticTypeRef("kotlin.Int", false))
        val declared = ResultColumnMetadata(typeOf<List<String?>>(), typeInfo = strings)
        for (target in listOf(typeOf<List<Int>>(), typeOf<List<String>>(), typeOf<List<*>>(), typeOf<List<String?>?>())) {
            assertNull(declared.withTargetType(target).typeInfo)
        }
        assertSame(strings, declared.withTargetType(typeOf<List<String?>>()).typeInfo)
        assertSame(ints, declared.withTargetType(typeOf<List<Int>>(), ints).typeInfo)
    }

    @Test
    fun retargetedUserCodecsSeeDestinationAndSourceFieldPoliciesExactlyOnce() {
        val number = StaticTypeInfo(StaticTypeRef("kotlin.Number", true))
        val source = Field("total", kType = typeOf<Number?>(), typeInfo = number)
        val column = ResultColumnMetadata(typeOf<Number?>(), source).withTargetType(typeOf<Int>())
        var supports = 0
        var conversions = 0
        val registration = KronosValueCodecs.register(staticValueCodec(supports = { _, context ->
            supports++
            assertEquals(StaticTypeRef("kotlin.Int", false), context.targetType)
            assertNull(context.targetTypeInfo)
            assertSame(source, context.field)
            assertSame(number, context.field?.typeInfo)
            true
        }, convert = { _, _ -> conversions++; 7 }))
        try {
            for (direction in ValueCodecDirection.entries) for (origin in ValueCodecOrigin.entries) {
                assertEquals(7, ValueCodecRegistry.convert(ValueConversionRequest(7L, direction, origin,
                    column.type, field = column.field, targetTypeInfo = column.typeInfo,
                    physicalTargetType = if (direction == ValueCodecDirection.ENCODE) typeOf<Int>() else null, strict = false)))
            }
            assertEquals(ValueCodecDirection.entries.size * ValueCodecOrigin.entries.size, supports)
            assertEquals(supports, conversions)
        } finally { registration.close() }
    }

    @Test
    fun validGenericMetadataSurvivesAndWrongElementsStillFailAfterOneConversion() {
        val exact = StaticTypeInfo(typeOf<List<Int>>().toStaticTypeRef(), containerKind = StaticContainerKind.ITERABLE,
            elementType = StaticTypeRef("kotlin.Int", false))
        val column = ResultColumnMetadata(typeOf<List<Int>>(), typeInfo = exact).withTargetType(typeOf<List<Int>>())
        var conversions = 0
        val registration = Kronos.registerValueCodec(valueCodec(supports = { _, context ->
            assertSame(exact, context.targetTypeInfo)
            context.storage == ValueStorage.SERIALIZED
        }, convert = { _, _ -> conversions++; listOf("wrong") }))
        try {
            assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest("stored", ValueCodecDirection.DECODE,
                    ValueCodecOrigin.DATABASE, column.type, targetTypeInfo = column.typeInfo, storage = ValueStorage.SERIALIZED))
            }
            assertEquals(1, conversions)
        } finally { registration.close() }
    }

    @Test
    fun physicalDescriptorIsIsolatedFromAuthoritativeLogicalMetadata() {
        val logicalInfo = StaticTypeInfo(StaticTypeRef("sample.Boolean", false))
        val request = ValueConversionRequest(true, ValueCodecDirection.ENCODE, ValueCodecOrigin.PARAMETER,
            typeOf<Boolean>(), targetTypeInfo = logicalInfo, physicalTargetType = typeOf<Int>())
        assertEquals(1, ValueCodecRegistry.convert(request))
    }
}
