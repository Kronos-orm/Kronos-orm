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

// Verifies two compiler modules emit coexisting providers without re-contributing dependency KPojo factories.

// MODULE: library
// FILE: library.kt

package provider.library

import com.kotlinorm.interfaces.KPojo

data class LibraryProviderRow(var id: Int? = null) : KPojo

// MODULE: main(library)
// FILE: main.kt

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package provider.main

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.KPojoFactory
import provider.library.LibraryProviderRow
import java.security.MessageDigest
import java.util.ServiceLoader
import kotlin.reflect.KType

data class MainProviderRow(
    var id: Int? = null,
    var dependency: LibraryProviderRow? = null,
) : KPojo

fun referenceDependency(): LibraryProviderRow = LibraryProviderRow()

fun box(): String {
    val providers = ServiceLoader.load(GeneratedTypeProvider::class.java).associateBy { it.id }
    val libraryId = providerId("kronos-test:library")
    val mainId = providerId("kronos-test:main")
    val libraryProvider = providers[libraryId] ?: return "Fail: provider $libraryId was not discovered"
    val mainProvider = providers[mainId] ?: return "Fail: provider $mainId was not discovered"
    if (libraryProvider::class == mainProvider::class) return "Fail: provider classes were identical"

    val libraryOwners = ownersFrom(libraryProvider)
    val mainOwners = ownersFrom(mainProvider)
    if (libraryOwners != listOf("provider.library.LibraryProviderRow")) {
        return "Fail: library owners were $libraryOwners"
    }
    if (mainOwners != listOf("provider.main.MainProviderRow")) {
        return "Fail: main owners were $mainOwners"
    }
    return "OK"
}

fun providerId(moduleCoordinate: String): String {
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(moduleCoordinate.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        .take(16)
    return "$moduleCoordinate#$hash"
}

fun ownersFrom(provider: GeneratedTypeProvider): List<String> {
    val owners = mutableListOf<String>()
    provider.contributeTo(object : GeneratedTypeRegistrar {
        override fun registerKPojo(
            type: KType,
            ownerId: String,
            constructorSignature: String,
            factory: KPojoFactory
        ) {
            owners += ownerId
        }

        override fun registerEnum(type: KType, entryNames: List<String>, factory: EnumFactory) = Unit
    })
    return owners
}
