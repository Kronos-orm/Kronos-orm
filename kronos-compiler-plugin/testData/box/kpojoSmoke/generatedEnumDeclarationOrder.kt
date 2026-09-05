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

// Verifies declaration-order enum metadata and direct name branches without ordinal/reflection lookup.

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.KPojoFactory
import java.security.MessageDigest
import java.util.ServiceLoader
import kotlin.reflect.KType
import kotlin.reflect.typeOf

enum class DeclaredOrderStatus(val wireCode: Int) {
    ARCHIVED(42),
    READY(7),
    PENDING(99),
}

data class DeclaredOrderRow(
    var status: DeclaredOrderStatus = DeclaredOrderStatus.ARCHIVED,
) : KPojo

fun box(): String {
    val identity = generatedProviderIdentity("kronos-test:main")
    val provider = ServiceLoader.load(GeneratedTypeProvider::class.java)
        .singleOrNull { it.id == identity.first }
        ?: return "Fail: generated provider was not discovered"
    var names: List<String>? = null
    var capturedFactory: EnumFactory? = null
    provider.contributeTo(object : GeneratedTypeRegistrar {
        override fun registerKPojo(
            type: KType,
            ownerId: String,
            constructorSignature: String,
            factory: KPojoFactory,
        ) = Unit

        override fun registerEnum(type: KType, entryNames: List<String>, factory: EnumFactory) {
            if (type == typeOf<DeclaredOrderStatus>()) {
                names = entryNames
                capturedFactory = factory
            }
        }
    })
    val expectedNames = listOf("ARCHIVED", "READY", "PENDING")
    val generatedFactory = capturedFactory ?: return "Fail: enum metadata was not contributed"
    return when {
        names != expectedNames -> "Fail: declaration order was $names"
        generatedFactory.create("ARCHIVED") !== DeclaredOrderStatus.ARCHIVED ->
            "Fail: ARCHIVED did not resolve directly"
        generatedFactory.create("READY") !== DeclaredOrderStatus.READY ->
            "Fail: READY did not resolve directly"
        generatedFactory.create("PENDING") !== DeclaredOrderStatus.PENDING ->
            "Fail: PENDING did not resolve directly"
        generatedFactory.create("UNKNOWN") != null ->
            "Fail: unknown name resolved to an enum entry"
        else -> "OK"
    }
}

private fun generatedProviderIdentity(moduleCoordinate: String): Pair<String, String> {
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(moduleCoordinate.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        .take(16)
    return "$moduleCoordinate#$hash" to
        "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_$hash"
}
