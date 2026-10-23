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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class StaticTypeMetadataRegistryTest {
    @Test
    fun `generated entry registration keeps provider identity and freeze semantics`() {
        val type = StaticTypeInfo(StaticTypeRef("sample.Generated", nullable = false))
        val registry = StaticTypeMetadataRegistry()

        registry.registerGeneratedTypeMetadata("sample:commonMain", listOf(type))

        assertEquals(type, registry.snapshot().typeInfo(type.type))
        assertFailsWith<IllegalStateException> {
            registry.registerGeneratedTypeMetadata("sample:late", emptyList())
        }
    }

    @Test
    fun `equivalent providers deduplicate and lookups normalize only root nullability`() {
        val nestedNullable = StaticTypeRef("sample.Element", nullable = true)
        val declaredType = StaticTypeRef(
            key = "sample.Container",
            nullable = true,
            arguments = listOf(StaticTypeArgument(StaticTypeVariance.OUT, nestedNullable))
        )
        val inputSupertypes = mutableListOf(StaticTypeRef("kotlin.Any", nullable = true))
        val typeInfo = StaticTypeInfo(
            type = declaredType,
            directSupertypes = inputSupertypes,
            declarationVariances = listOf(StaticTypeVariance.OUT)
        )
        val registry = StaticTypeMetadataRegistry()

        registry.register(provider("sample:container") { registrar ->
            registrar.registerTypeInfo(typeInfo)
        })
        registry.register(provider("sample:container") { registrar ->
            registrar.registerTypeInfo(typeInfo)
        })

        inputSupertypes += StaticTypeRef("sample.Mutated", nullable = false)
        val snapshot = registry.snapshot()
        val expected = StaticTypeInfo(
            type = declaredType.copy(nullable = false),
            directSupertypes = listOf(StaticTypeRef("kotlin.Any", nullable = false)),
            declarationVariances = listOf(StaticTypeVariance.OUT)
        )

        assertEquals(expected, snapshot.typeInfo(declaredType))
        assertEquals(expected, snapshot.typeInfo(declaredType.copy(nullable = false)))
        assertEquals(nestedNullable, snapshot.typeInfo(declaredType)?.type?.arguments?.single()?.type)
        assertEquals(
            null,
            snapshot.typeInfo(
                declaredType.copy(
                    arguments = listOf(
                        StaticTypeArgument(StaticTypeVariance.OUT, nestedNullable.copy(nullable = false))
                    )
                )
            )
        )
    }

    @Test
    fun `conflicting providers include their identity and descriptor conflicts include the type`() {
        val type = StaticTypeRef("sample.Item", nullable = false)
        val first = StaticTypeInfo(type)
        val incompatible = first.copy(isKPojo = true)

        val duplicateIdRegistry = StaticTypeMetadataRegistry()
        duplicateIdRegistry.register(provider("sample:module") { registrar ->
            registrar.registerTypeInfo(first)
        })
        duplicateIdRegistry.register(provider("sample:module") { registrar ->
            registrar.registerTypeInfo(incompatible)
        })

        assertEquals(
            "Static type metadata provider 'sample:module' contributed conflicting metadata.",
            assertFailsWith<IllegalArgumentException> { duplicateIdRegistry.snapshot() }.message
        )

        val typeConflictRegistry = StaticTypeMetadataRegistry()
        typeConflictRegistry.register(provider("sample:first") { registrar ->
            registrar.registerTypeInfo(first)
        })
        typeConflictRegistry.register(provider("sample:second") { registrar ->
            registrar.registerTypeInfo(incompatible)
        })

        assertEquals(
            "Conflicting generated static type metadata was registered for ${type.structuralSignature()}",
            assertFailsWith<IllegalArgumentException> { typeConflictRegistry.snapshot() }.message
        )
    }

    @Test
    fun `registry closes registration after the first snapshot`() {
        val registry = StaticTypeMetadataRegistry()
        registry.register(provider("sample:first") { registrar ->
            registrar.registerTypeInfo(StaticTypeInfo(StaticTypeRef("sample.First", nullable = false)))
        })

        val snapshot = registry.snapshot()

        assertEquals(
            "Static type metadata registry is frozen; register all providers before the first lookup.",
            assertFailsWith<IllegalStateException> {
                registry.register(provider("sample:late") { registrar ->
                    registrar.registerTypeInfo(StaticTypeInfo(StaticTypeRef("sample.Late", nullable = false)))
                })
            }.message
        )
        assertEquals(
            StaticTypeInfo(StaticTypeRef("sample.First", nullable = false)),
            snapshot.typeInfo(StaticTypeRef("sample.First", nullable = false))
        )
    }

    @Test
    fun `blank provider identities are rejected before executing their contribution`() {
        val registry = StaticTypeMetadataRegistry()
        var callbackCalls = 0

        val failure = assertFailsWith<IllegalArgumentException> {
            registry.register(provider(" \n\t") { callbackCalls++ })
        }

        assertEquals("Static type metadata provider id must not be blank.", failure.message)
        assertEquals(0, callbackCalls)
        registry.registerGeneratedTypeMetadata("sample:valid", emptyList())
        val snapshot = registry.snapshot()
        assertSame(snapshot, registry.snapshot())
    }

    @Test
    fun `a contradictory contribution cannot partially install its preceding descriptors`() {
        val registry = StaticTypeMetadataRegistry()
        val preceding = StaticTypeInfo(StaticTypeRef("sample.Preceding", nullable = false))
        val conflicting = StaticTypeInfo(StaticTypeRef("sample.Conflicting", nullable = false))

        val failure = assertFailsWith<IllegalArgumentException> {
            registry.register(provider("sample:contradictory") { registrar ->
                registrar.registerTypeInfo(preceding)
                registrar.registerTypeInfo(conflicting)
                registrar.registerTypeInfo(conflicting)
                registrar.registerTypeInfo(conflicting.copy(isKPojo = true))
            })
        }

        assertEquals(
            "Conflicting generated static type metadata was registered for ${conflicting.type.structuralSignature()}",
            failure.message
        )
        val valid = StaticTypeInfo(StaticTypeRef("sample.Valid", nullable = false))
        registry.registerGeneratedTypeMetadata("sample:valid", listOf(valid))
        val snapshot = registry.snapshot()
        assertNull(snapshot.typeInfo(preceding.type))
        assertNull(snapshot.typeInfo(conflicting.type))
        assertEquals(valid, snapshot.typeInfo(valid.type))
    }

    @Test
    fun `a provider callback cannot append metadata after it triggers the first lookup`() {
        val registry = StaticTypeMetadataRegistry()
        val installed = StaticTypeInfo(StaticTypeRef("sample.Installed", nullable = false))
        val late = StaticTypeInfo(StaticTypeRef("sample.Late", nullable = false))
        registry.registerGeneratedTypeMetadata("sample:installed", listOf(installed))
        var snapshotDuringContribution: StaticTypeMetadataSnapshot? = null

        val failure = assertFailsWith<IllegalStateException> {
            registry.register(provider("sample:late") { registrar ->
                registrar.registerTypeInfo(late)
                snapshotDuringContribution = registry.snapshot()
            })
        }

        assertEquals(
            "Static type metadata registry is frozen; register all providers before the first lookup.",
            failure.message
        )
        val snapshot = registry.snapshot()
        assertSame(snapshotDuringContribution, snapshot)
        assertEquals(installed, snapshot.typeInfo(installed.type))
        assertNull(snapshot.typeInfo(late.type))
    }

    @Test
    fun `registration snapshots deeply nested container metadata and star arguments`() {
        val leaf = StaticTypeRef("sample.Value", nullable = true)
        val expectedArguments = listOf(
            StaticTypeArgument(StaticTypeVariance.OUT, leaf),
            StaticTypeArgument(StaticTypeVariance.STAR)
        )
        val mutableArguments = expectedArguments.toMutableList()
        val expectedElement = StaticTypeRef("sample.Pair", nullable = true, arguments = expectedArguments)
        val mutableElement = expectedElement.copy(arguments = mutableArguments)
        val mutableOuterArguments = mutableListOf(StaticTypeArgument(StaticTypeVariance.OUT, mutableElement))
        val mutableVariances = mutableListOf(StaticTypeVariance.OUT)
        val expected = StaticTypeInfo(
            type = StaticTypeRef("sample.Collection", nullable = false,
                arguments = listOf(StaticTypeArgument(StaticTypeVariance.OUT, expectedElement))),
            containerKind = StaticContainerKind.ITERABLE,
            elementType = expectedElement,
            declarationVariances = listOf(StaticTypeVariance.OUT)
        )
        val supplied = expected.copy(
            type = expected.type.copy(nullable = true, arguments = mutableOuterArguments),
            elementType = mutableElement,
            declarationVariances = mutableVariances
        )
        val registry = StaticTypeMetadataRegistry()
        registry.registerGeneratedTypeMetadata("sample:container", listOf(supplied))

        mutableArguments.clear()
        mutableOuterArguments.clear()
        mutableVariances.clear()

        val snapshot = registry.snapshot()
        assertEquals(expected, snapshot.typeInfo(expected.type))
        assertEquals(expected, snapshot.typeInfo(expected.type.copy(nullable = true)))
        assertNull(snapshot.typeInfo(supplied.type))
    }

    private fun provider(
        id: String,
        contribution: (StaticTypeMetadataRegistrar) -> Unit
    ): StaticTypeMetadataProvider = object : StaticTypeMetadataProvider {
        override val id: String = id

        override fun contributeTo(registrar: StaticTypeMetadataRegistrar) = contribution(registrar)
    }
}
