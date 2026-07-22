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

// Verifies that the generated KPojo factory provider resolves KType values to direct constructors.

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.KPojoFactory
import java.security.MessageDigest
import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class FactoryProviderUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

data class FactoryProviderRequiredUser(
    var id: Int,
) : KPojo

fun box(): String {
    val identity = generatedProviderIdentity("kronos-test:main")
    val providerClass = Class.forName(identity.second)
    val provider = providerClass.getDeclaredConstructor().newInstance() as GeneratedTypeProvider
    val contributions = mutableListOf<KPojoContribution>()
    provider.contributeTo(object : GeneratedTypeRegistrar {
        override fun registerKPojo(
            type: KType,
            ownerId: String,
            constructorSignature: String,
            factory: KPojoFactory
        ) {
            contributions += KPojoContribution(type, ownerId, constructorSignature, factory)
        }

        override fun registerEnum(type: KType, entryNames: List<String>, factory: EnumFactory) = Unit
    })

    val contribution = contributions.singleOrNull { it.type == typeOf<FactoryProviderUser>() }
        ?: return "Fail: factory did not contribute ${typeOf<FactoryProviderUser>()}"
    val first = contribution.factory.create(typeOf<FactoryProviderUser>()) as? FactoryProviderUser
    val second = contribution.factory.create(typeOf<FactoryProviderUser>()) as? FactoryProviderUser
    return when {
        provider.id != identity.first -> "Fail: provider id was ${provider.id}"
        contributions.any { it.type == typeOf<FactoryProviderRequiredUser>() } -> "Fail: required constructor was contributed"
        first == null -> "Fail: factory did not match ${typeOf<FactoryProviderUser>()}"
        contribution.ownerId != "FactoryProviderUser" -> "Fail: owner id was ${contribution.ownerId}"
        !contribution.constructorSignature.startsWith("FactoryProviderUser(") ->
            "Fail: constructor signature was ${contribution.constructorSignature}"
        first.id != null -> "Fail: id was ${first.id}"
        first.name != null -> "Fail: name was ${first.name}"
        second == null -> "Fail: second factory result was null"
        first === second -> "Fail: factory reused an instance"
        else -> "OK"
    }
}

fun generatedProviderIdentity(moduleCoordinate: String): Pair<String, String> {
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(moduleCoordinate.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        .take(16)
    return "$moduleCoordinate#$hash" to
        "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_$hash"
}

data class KPojoContribution(
    val type: KType,
    val ownerId: String,
    val constructorSignature: String,
    val factory: KPojoFactory,
)
