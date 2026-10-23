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

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.ResultColumnMetadata
import com.kotlinorm.enums.ValueCodecDirection.DECODE
import com.kotlinorm.enums.ValueCodecOrigin.MAP
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.ValueCodecContext
import com.kotlinorm.interfaces.valueCodec
import com.kotlinorm.utils.codec.PreparedValue
import com.kotlinorm.utils.codec.PreparedValueKind
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.codec.ValueConversionRequest
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class StaticTypeMetadataTest {
    @Test
    fun `type signatures retain nullability nested arguments variance and star projections`() {
        val nullableString = StaticTypeRef("kotlin.String", nullable = true)
        val nestedMap = StaticTypeRef(
            key = "kotlin.collections.Map",
            nullable = false,
            arguments = listOf(
                StaticTypeArgument(StaticTypeVariance.INVARIANT, StaticTypeRef("kotlin.String", nullable = false)),
                StaticTypeArgument(
                    StaticTypeVariance.OUT,
                    StaticTypeRef(
                        key = "kotlin.collections.List",
                        nullable = true,
                        arguments = listOf(StaticTypeArgument(StaticTypeVariance.IN, nullableString))
                    )
                ),
                StaticTypeArgument(StaticTypeVariance.STAR)
            )
        )

        assertEquals(
            "22:kotlin.collections.Map!<=13:kotlin.String!<>,+23:kotlin.collections.List?<-13:kotlin.String?<>>,*>",
            nestedMap.structuralSignature()
        )
        assertNotEquals(
            StaticTypeRef("kotlin.String", nullable = false).structuralSignature(),
            nullableString.structuralSignature()
        )
        assertEquals(
            "A STAR type argument must not contain a type.",
            assertFailsWith<IllegalArgumentException> {
                StaticTypeArgument(StaticTypeVariance.STAR, nullableString)
            }.message
        )
        assertEquals(
            "A INVARIANT type argument must contain a type.",
            assertFailsWith<IllegalArgumentException> {
                StaticTypeArgument(StaticTypeVariance.INVARIANT)
            }.message
        )
    }

    @Test
    fun `metadata retains substituted direct supertypes and explicit primitive array shape`() {
        val user = StaticTypeRef("sample.User", nullable = false)
        val userList = StaticTypeRef("sample.UserList", nullable = false)
        val substitutedList = StaticTypeRef(
            key = "kotlin.collections.List",
            nullable = false,
            arguments = listOf(StaticTypeArgument(StaticTypeVariance.INVARIANT, user))
        )
        val primitiveArray = StaticTypeInfo(
            type = StaticTypeRef("kotlin.IntArray", nullable = false),
            directSupertypes = listOf(StaticTypeRef("kotlin.Any", nullable = true)),
            containerKind = StaticContainerKind.PRIMITIVE_ARRAY,
            elementType = StaticTypeRef("kotlin.Int", nullable = false)
        )
        val objectArray = StaticTypeInfo(
            type = StaticTypeRef(
                key = "kotlin.Array",
                nullable = false,
                arguments = listOf(StaticTypeArgument(StaticTypeVariance.INVARIANT, user))
            ),
            containerKind = StaticContainerKind.ARRAY,
            elementType = user
        )
        val kPojo = StaticTypeInfo(
            type = userList,
            directSupertypes = listOf(substitutedList),
            isKPojo = true
        )

        assertEquals(listOf(substitutedList), kPojo.directSupertypes)
        assertEquals(StaticContainerKind.ARRAY, objectArray.containerKind)
        assertEquals(user, objectArray.elementType)
        assertEquals(StaticContainerKind.PRIMITIVE_ARRAY, primitiveArray.containerKind)
        assertEquals(StaticTypeRef("kotlin.Int", nullable = false), primitiveArray.elementType)
        assertEquals(
            "type=15:kotlin.IntArray!<>|supertypes=10:kotlin.Any?<>|container=PRIMITIVE_ARRAY|element=10:kotlin.Int!<>|kPojo=false|enum=-|declarationVariances=",
            primitiveArray.structuralSignature()
        )
        assertEquals(
            "A primitive array type must declare its element type.",
            assertFailsWith<IllegalArgumentException> {
                StaticTypeInfo(
                    type = StaticTypeRef("kotlin.IntArray", nullable = false),
                    containerKind = StaticContainerKind.PRIMITIVE_ARRAY
                )
            }.message
        )
        assertEquals(
            "A map type must describe keys and values through type arguments.",
            assertFailsWith<IllegalArgumentException> {
                StaticTypeInfo(
                    type = StaticTypeRef("kotlin.collections.Map", nullable = false),
                    containerKind = StaticContainerKind.MAP,
                    elementType = user
                )
            }.message
        )
        assertEquals(
            "Declaration-site variance cannot be STAR.",
            assertFailsWith<IllegalArgumentException> {
                StaticTypeInfo(
                    type = StaticTypeRef("sample.Invalid", nullable = false),
                    declarationVariances = listOf(StaticTypeVariance.STAR)
                )
            }.message
        )
    }

    @Test
    fun `static registry preserves complete references and normalizes only root nullability`() {
        val nullableElement = StaticTypeRef("sample.Element", nullable = true)
        val mapType = StaticTypeRef(
            key = "kotlin.collections.Map",
            nullable = true,
            arguments = listOf(
                StaticTypeArgument(StaticTypeVariance.INVARIANT, StaticTypeRef("kotlin.String", nullable = false)),
                StaticTypeArgument(
                    StaticTypeVariance.INVARIANT,
                    StaticTypeRef(
                        key = "kotlin.collections.List",
                        nullable = false,
                        arguments = listOf(StaticTypeArgument(StaticTypeVariance.OUT, nullableElement))
                    )
                )
            )
        )
        val info = StaticTypeInfo(
            type = mapType,
            directSupertypes = listOf(StaticTypeRef("kotlin.Any", nullable = true)),
            containerKind = StaticContainerKind.MAP,
            declarationVariances = listOf(StaticTypeVariance.INVARIANT, StaticTypeVariance.OUT)
        )
        val provider = object : GeneratedTypeProvider {
            override val id: String = "static-types"

            override fun contributeTo(registrar: GeneratedTypeRegistrar) {
                registrar.registerTypeInfo(info)
            }
        }

        val metadata = loadGeneratedTypeMetadata(listOf(provider))
        val expected = info.copy(
            type = mapType.withoutTopLevelNullability(),
            directSupertypes = listOf(StaticTypeRef("kotlin.Any", nullable = false))
        )

        assertEquals(expected, metadata.typeInfo(mapType))
        assertEquals(expected, metadata.typeInfo(mapType.withoutTopLevelNullability()))
        assertEquals(
            nullableElement,
            metadata.typeInfo(mapType)?.type?.arguments?.get(1)?.type?.arguments?.single()?.type
        )
        assertEquals(
            null,
            metadata.typeInfo(
                mapType.copy(arguments = mapType.arguments.dropLast(1))
            )
        )
        val nestedNullabilityChanged = mapType.copy(
            arguments = listOf(
                mapType.arguments.first(),
                StaticTypeArgument(
                    StaticTypeVariance.INVARIANT,
                    mapType.arguments[1].type!!.copy(
                        arguments = listOf(
                            StaticTypeArgument(
                                StaticTypeVariance.OUT,
                                nullableElement.copy(nullable = false)
                            )
                        )
                    )
                )
            )
        )
        assertEquals(null, metadata.typeInfo(nestedNullabilityChanged))
        assertEquals(
            "Conflicting generated static type metadata was registered for ${mapType.withoutTopLevelNullability().structuralSignature()}",
            assertFailsWith<IllegalStateException> {
                loadGeneratedTypeMetadata(listOf(
                    provider,
                    object : GeneratedTypeProvider {
                        override val id: String = "conflicting-static-types"

                        override fun contributeTo(registrar: GeneratedTypeRegistrar) {
                            registrar.registerTypeInfo(info.copy(isKPojo = true))
                        }
                    }
                ))
            }.message
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `fields result columns and query tasks retain static metadata`() {
        val typeInfo = sampleTypeInfo()
        val field = Field(columnName = "name", kType = typeOf<String>(), typeInfo = typeInfo)
        val resultColumn = ResultColumnMetadata(typeOf<String>(), field)
        val query = KronosAtomicQueryTask(
            sql = "SELECT name FROM sample",
            targetType = typeOf<String>(),
            targetTypeInfo = typeInfo
        )
        val contract: KAtomicQueryTask = query

        assertSame(typeInfo, (field + "_alias").typeInfo)
        assertSame(typeInfo, field.copy().typeInfo)
        assertSame(typeInfo, resultColumn.typeInfo)
        assertSame(typeInfo, contract.targetTypeInfo)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `prepared values conversion requests and codec contexts retain static metadata`() {
        val sourceInfo = StaticTypeInfo(StaticTypeRef("sample.Source", nullable = false))
        val targetInfo = sampleTypeInfo()
        val physicalTargetInfo = StaticTypeInfo(StaticTypeRef("kotlin.String", nullable = false))
        val prepared = PreparedValue(
            value = "raw",
            sourceType = typeOf<String>(),
            kind = PreparedValueKind.READY_DATABASE_VALUE,
            sourceTypeInfo = sourceInfo
        )
        val request = ValueConversionRequest(
            value = prepared.value,
            direction = DECODE,
            origin = MAP,
            targetType = typeOf<String>(),
            sourceType = prepared.sourceType,
            physicalTargetType = typeOf<String>(),
            targetTypeInfo = targetInfo,
            sourceTypeInfo = prepared.sourceTypeInfo,
            physicalTargetTypeInfo = physicalTargetInfo
        )
        var observed: ValueCodecContext? = null
        val registration = ValueCodecRegistry.register(
            valueCodec(
                supports = { _, context ->
                    observed = context
                    true
                },
                convert = { value, _ -> value }
            )
        )

        try {
            assertEquals("raw", ValueCodecRegistry.convert(request))
        } finally {
            registration.close()
        }

        assertSame(sourceInfo, prepared.sourceTypeInfo)
        assertSame(targetInfo, request.targetTypeInfo)
        assertSame(sourceInfo, request.sourceTypeInfo)
        assertSame(physicalTargetInfo, request.physicalTargetTypeInfo)
        assertSame(sourceInfo, observed?.sourceTypeInfo)
        assertSame(targetInfo, observed?.targetTypeInfo)
    }

    private fun sampleTypeInfo(): StaticTypeInfo = StaticTypeInfo(
        type = StaticTypeRef("sample.Target", nullable = false),
        enumKey = "sample.Target"
    )
}
