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

// Verifies complete KPojo generation through a dependency-defined intermediate interface.

// MODULE: library
// FILE: library.kt

package binary.library

import com.kotlinorm.interfaces.KPojo

interface BinaryContract : KPojo

// MODULE: main(library)
// FILE: main.kt

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package binary.main

import binary.library.BinaryContract
import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.KPojoFactory
import java.security.MessageDigest
import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class BinaryEntity(
    var id: Int? = null,
    var name: String? = null,
) : BinaryContract

fun box(): String {
    val entity = BinaryEntity(7, "Ada")
    val expectedColumns = listOf("id", "name")
    val expectedMap = mutableMapOf<String, Any?>("id" to 7, "name" to "Ada")

    if (entity.__kType != typeOf<BinaryEntity>()) return "Fail: KType was ${entity.__kType}"
    if (entity.__columns.map { it.name } != expectedColumns) {
        return "Fail: columns were ${entity.__columns.map { it.name }}"
    }
    if (entity.toDataMap() != expectedMap) return "Fail: data map was ${entity.toDataMap()}"
    if (entity["id"] != 7) return "Fail: dynamic id was ${entity["id"]}"
    if (entity["name"] != "Ada") return "Fail: dynamic name was ${entity["name"]}"

    entity["id"] = 11
    entity["name"] = "Katherine"
    if (entity.id != 11 || entity.name != "Katherine") {
        return "Fail: dynamic assignment produced id=${entity.id}, name=${entity.name}"
    }

    val mapped = entity.fromMapData<BinaryEntity>(mapOf("id" to 13, "name" to "Grace"))
    if (mapped !== entity) return "Fail: fromMapData returned another instance"
    if (entity.id != 13 || entity.name != "Grace") {
        return "Fail: fromMapData produced id=${entity.id}, name=${entity.name}"
    }

    val safeMapped = entity.safeFromMapData<BinaryEntity>(mapOf("id" to 17, "name" to "Lin"))
    if (safeMapped !== entity) return "Fail: safeFromMapData returned another instance"
    if (entity.id != 17 || entity.name != "Lin") {
        return "Fail: safeFromMapData produced id=${entity.id}, name=${entity.name}"
    }

    val registrations = mutableListOf<Pair<KType, String>>()
    var entityFactory: KPojoFactory? = null
    mainGeneratedProvider().contributeTo(object : GeneratedTypeRegistrar {
        override fun registerKPojo(
            type: KType,
            ownerId: String,
            constructorSignature: String,
            factory: KPojoFactory,
        ) {
            registrations += type to ownerId
            if (type == typeOf<BinaryEntity>()) entityFactory = factory
        }

        override fun registerEnum(type: KType, entryNames: List<String>, factory: EnumFactory) = Unit
    })

    val expectedRegistrations = listOf(typeOf<BinaryEntity>() to "binary.main.BinaryEntity")
    if (typeOf<BinaryContract>() in registrations.map { it.first }) {
        return "Fail: binary interface received a factory"
    }
    if (registrations != expectedRegistrations) return "Fail: registrations were $registrations"
    val created = entityFactory?.create(typeOf<BinaryEntity>())
    if (created !is BinaryEntity) return "Fail: entity factory returned $created"
    return "OK"
}

private fun mainGeneratedProvider(): GeneratedTypeProvider {
    val moduleCoordinate = "kronos-test:main"
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(moduleCoordinate.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        .take(16)
    val fqName = "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_$hash"
    return Class.forName(fqName).getDeclaredConstructor().newInstance() as GeneratedTypeProvider
}
