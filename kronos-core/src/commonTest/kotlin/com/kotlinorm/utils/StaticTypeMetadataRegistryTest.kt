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

    private fun provider(
        id: String,
        contribution: (StaticTypeMetadataRegistrar) -> Unit
    ): StaticTypeMetadataProvider = object : StaticTypeMetadataProvider {
        override val id: String = id

        override fun contributeTo(registrar: StaticTypeMetadataRegistrar) = contribution(registrar)
    }
}
