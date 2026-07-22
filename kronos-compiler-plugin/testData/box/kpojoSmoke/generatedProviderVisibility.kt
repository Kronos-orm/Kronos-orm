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

// Verifies generated providers retain accessible KPojo and enum metadata while omitting non-constructible shapes.

import com.kotlinorm.annotations.Serialize
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.KPojoFactory
import java.security.MessageDigest
import kotlin.reflect.KType
import kotlin.reflect.typeOf

enum class PublicVisibilityStatus { READY, DONE }

internal enum class InternalVisibilityStatus { INTERNAL }

data class PublicVisibilityEntity(
    var status: PublicVisibilityStatus = PublicVisibilityStatus.READY,
) : KPojo

internal data class InternalVisibilityEntity(
    var status: InternalVisibilityStatus = InternalVisibilityStatus.INTERNAL,
) : KPojo

class PublicDefaultArgumentsKPojo(
    var id: Int = 7,
) : KPojo

class PublicInternalConstructorKPojo internal constructor() : KPojo

private enum class PrivateSerializedStatus { HIDDEN }

internal class SerializedPrivateEnumEntity : KPojo {
    @Serialize
    private var status: PrivateSerializedStatus? = null

    @Serialize
    private var statuses: List<PrivateSerializedStatus> = emptyList()
}

abstract class PublicAbstractKPojo : KPojo

sealed class PublicSealedKPojo : KPojo

object PublicObjectKPojo : KPojo

interface PublicKPojoContract : KPojo

class PublicRequiredConstructorKPojo(val id: Int) : KPojo

class PublicPartiallyDefaultedKPojo(val id: Int, val name: String = "") : KPojo

class PublicPrivateConstructorKPojo private constructor() : KPojo

open class PublicProtectedConstructorKPojo protected constructor() : KPojo

class PublicVisibilityOwner {
    inner class InnerKPojo : KPojo
}

fun box(): String {
    val kPojoFactories = linkedMapOf<KType, KPojoFactory>()
    val enumFactories = linkedMapOf<KType, EnumFactory>()
    generatedProvider().contributeTo(object : GeneratedTypeRegistrar {
        override fun registerKPojo(
            type: KType,
            ownerId: String,
            constructorSignature: String,
            factory: KPojoFactory,
        ) {
            kPojoFactories[type] = factory
        }

        override fun registerEnum(type: KType, entryNames: List<String>, factory: EnumFactory) {
            enumFactories[type] = factory
        }
    })

    val publicFactory = kPojoFactories[typeOf<PublicVisibilityEntity>()]
        ?: return "Fail: public KPojo factory was not contributed"
    if (publicFactory.create(typeOf<PublicVisibilityEntity>()) !is PublicVisibilityEntity) {
        return "Fail: public KPojo factory returned the wrong type"
    }
    val internalFactory = kPojoFactories[typeOf<InternalVisibilityEntity>()]
        ?: return "Fail: internal KPojo factory was not contributed"
    if (internalFactory.create(typeOf<InternalVisibilityEntity>()) !is InternalVisibilityEntity) {
        return "Fail: internal KPojo factory returned the wrong type"
    }
    val defaultArgumentsFactory = kPojoFactories[typeOf<PublicDefaultArgumentsKPojo>()]
        ?: return "Fail: default-arguments KPojo factory was not contributed"
    val defaultArgumentsValue = defaultArgumentsFactory.create(typeOf<PublicDefaultArgumentsKPojo>())
    if (defaultArgumentsValue !is PublicDefaultArgumentsKPojo || defaultArgumentsValue.id != 7) {
        return "Fail: default-arguments KPojo factory returned $defaultArgumentsValue"
    }
    val internalConstructorFactory = kPojoFactories[typeOf<PublicInternalConstructorKPojo>()]
        ?: return "Fail: internal-constructor KPojo factory was not contributed"
    if (
        internalConstructorFactory.create(typeOf<PublicInternalConstructorKPojo>())
            !is PublicInternalConstructorKPojo
    ) {
        return "Fail: internal-constructor KPojo factory returned the wrong type"
    }
    val serializedPrivateEnumFactory = kPojoFactories[typeOf<SerializedPrivateEnumEntity>()]
        ?: return "Fail: serialized private-enum KPojo factory was not contributed"
    if (
        serializedPrivateEnumFactory.create(typeOf<SerializedPrivateEnumEntity>())
            !is SerializedPrivateEnumEntity
    ) {
        return "Fail: serialized private-enum KPojo factory returned the wrong type"
    }
    val nonConstructibleTypes = setOf(
        typeOf<PublicAbstractKPojo>(),
        typeOf<PublicSealedKPojo>(),
        typeOf<PublicObjectKPojo>(),
        typeOf<PublicKPojoContract>(),
        typeOf<PublicRequiredConstructorKPojo>(),
        typeOf<PublicPartiallyDefaultedKPojo>(),
        typeOf<PublicPrivateConstructorKPojo>(),
        typeOf<PublicProtectedConstructorKPojo>(),
        typeOf<PublicVisibilityOwner.InnerKPojo>(),
    )
    val contributedNonConstructibleTypes = kPojoFactories.keys.intersect(nonConstructibleTypes)
    if (contributedNonConstructibleTypes.isNotEmpty()) {
        return "Fail: non-constructible KPojo factories were $contributedNonConstructibleTypes"
    }

    val publicEnumFactory = enumFactories[typeOf<PublicVisibilityStatus>()]
        ?: return "Fail: public enum factory was not contributed"
    if (publicEnumFactory.create("DONE") != PublicVisibilityStatus.DONE) {
        return "Fail: public enum factory did not decode DONE"
    }
    val internalEnumFactory = enumFactories[typeOf<InternalVisibilityStatus>()]
        ?: return "Fail: internal enum factory was not contributed"
    if (internalEnumFactory.create("INTERNAL") != InternalVisibilityStatus.INTERNAL) {
        return "Fail: internal enum factory did not decode INTERNAL"
    }
    if (typeOf<PrivateSerializedStatus>() in enumFactories) {
        return "Fail: optional serialized private enum factory was contributed"
    }
    return "OK"
}

private fun generatedProvider(): GeneratedTypeProvider {
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
