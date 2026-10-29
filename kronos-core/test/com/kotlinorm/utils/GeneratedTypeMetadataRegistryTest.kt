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

import kotlin.reflect.typeOf
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.exceptions.InvalidKPojoFactoryResult
import kotlin.test.assertNotSame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GeneratedTypeMetadataRegistryTest {
    @Test
    fun equivalentProviderIdsAreDeduplicatedAndFreezeRejectsLateRegistration() {
        val registry = GeneratedTypeMetadataRegistry()
        val info = StaticTypeInfo(StaticTypeRef("fixture.Type", false))
        registry.register(provider("module", info))
        registry.register(provider("module", info))
        assertEquals(info, registry.snapshot().typeInfo(info.type))
        assertFailsWith<IllegalStateException> { registry.register(provider("late", info)) }
    }

    @Test
    fun conflictingSameProviderAndConflictingDifferentProvidersFailAtFreeze() {
        val original = StaticTypeInfo(StaticTypeRef("fixture.Type", false))
        val incompatible = original.copy(isKPojo = true)
        listOf("module", "other").forEach { secondId ->
            val registry = GeneratedTypeMetadataRegistry()
            registry.register(provider("module", original))
            registry.register(provider(secondId, incompatible))
            assertFailsWith<IllegalStateException> { registry.freeze() }
        }
    }

    @Test
    fun registrationCapturesMutableListsAndProviderCallbacksBeforeFreeze() {
        val supertypes = mutableListOf(StaticTypeRef("fixture.Base", false))
        val info = StaticTypeInfo(StaticTypeRef("fixture.Type", false), directSupertypes = supertypes)
        val registry = GeneratedTypeMetadataRegistry()
        registry.register(provider("module", info))
        supertypes.clear()
        assertEquals(
            listOf(StaticTypeRef("fixture.Base", false)),
            registry.snapshot().typeInfo(info.type)?.directSupertypes
        )
    }

    @Test
    fun snapshotLookupsReturnCopiesOfMutableMetadataLists() {
        val registry = GeneratedTypeMetadataRegistry()
        val info = StaticTypeInfo(
            StaticTypeRef("fixture.Type", false),
            directSupertypes = mutableListOf(StaticTypeRef("fixture.Base", false))
        )
        registry.register(provider("module", info))
        val snapshot = registry.snapshot()
        (snapshot.typeInfo(info.type)!!.directSupertypes as MutableList).clear()
        assertEquals(listOf(StaticTypeRef("fixture.Base", false)), snapshot.typeInfo(info.type)?.directSupertypes)
    }

    @Test
    fun nestedTypeArgumentsAreCopiedDuringRegistrationAndLookup() {
        val arguments = mutableListOf(StaticTypeArgument(StaticTypeVariance.OUT, StaticTypeRef("fixture.Item", true)))
        val mutableType = StaticTypeRef("fixture.Generic", false, arguments)
        val lookupType = mutableType.copy(arguments = arguments.toList())
        val registry = GeneratedTypeMetadataRegistry()
        registry.register(provider("module", StaticTypeInfo(mutableType)))
        arguments.clear()
        val snapshot = registry.snapshot()
        val returned = snapshot.typeInfo(lookupType)!!
        assertEquals(lookupType, returned.type)
        (returned.type.arguments as MutableList).clear()
        assertEquals(lookupType, snapshot.typeInfo(lookupType)?.type)
    }

    @Test
    fun enumNamesAreCapturedAndCopiedAtEveryLookup() {
        val names = mutableListOf("READY", "CLOSED")
        val registry = GeneratedTypeMetadataRegistry()
        registry.register(object : GeneratedTypeProvider {
            override val id = "enum-module"
            override fun contributeTo(registrar: GeneratedTypeRegistrar) {
                registrar.registerEnum(typeOf<RegistryStatus>(), names, EnumFactory { name ->
                    RegistryStatus.entries.firstOrNull { it.name == name }
                })
            }
        })
        names.clear()
        val snapshot = registry.snapshot()
        val first = snapshot.enumMetadata(typeOf<RegistryStatus>())!!
        (first.entryNames as MutableList).clear()
        assertEquals(listOf("READY", "CLOSED"), snapshot.enumMetadata(typeOf<RegistryStatus>())?.entryNames)
    }

    @Test
    fun explicitUserFactoriesRemainLazyAndValidateExactReturnTypes() {
        var calls = 0
        val type = typeOf<RegistryUserPojo>()
        val registration = registerKPojoFactory(type, KPojoFactory { calls++; RegistryUserPojo() })
        try {
            assertEquals(0, calls)
            assertNotSame(createKPojo(type), createKPojo(type))
            assertEquals(2, calls)
        } finally {
            registration.close()
            registration.close()
        }
        val wrong = registerKPojoFactory(type, KPojoFactory { RegistryOtherPojo() })
        try {
            assertFailsWith<InvalidKPojoFactoryResult> { createKPojo(type) }
        } finally {
            wrong.close()
        }
    }

    @Test
    fun generatedTestFactoriesUseTheSharedExplicitInitializationApi() {
        initializeGeneratedTestMetadata()
        val type = typeOf<RegistryUserPojo>()
        val first = createKPojo(type) as RegistryUserPojo
        val second = createKPojo(type) as RegistryUserPojo
        assertEquals(0, first.value)
        assertNotSame(first, second)
    }

    private fun provider(id: String, info: StaticTypeInfo): GeneratedTypeProvider = object : GeneratedTypeProvider {
        override val id: String = id
        override fun contributeTo(registrar: GeneratedTypeRegistrar) { registrar.registerTypeInfo(info) }
    }
}

private enum class RegistryStatus { READY, CLOSED }

internal data class RegistryUserPojo(var value: Int = 0) : KPojo
internal data class RegistryOtherPojo(var value: Int = 0) : KPojo

private val initializedTestMetadata by lazy { initializeKronosMetadata() }

/** Explicit once-only startup shared by generated Native test factory consumers. */
internal fun initializeGeneratedTestMetadata() { initializedTestMetadata }
