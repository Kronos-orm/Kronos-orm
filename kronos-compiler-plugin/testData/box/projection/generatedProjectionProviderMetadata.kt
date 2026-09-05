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

// Verifies synthetic projections contribute factories and copied enum metadata after materialization.

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.KPojoFactory
import java.security.MessageDigest
import kotlin.reflect.KType
import kotlin.reflect.typeOf

enum class ProjectionStatus { READY, ARCHIVED }

data class ProjectionProviderSource(
    var id: Int? = null,
    var status: ProjectionStatus = ProjectionStatus.READY,
) : KPojo

fun box(): String {
    ProjectionProviderSource().select { [it.id, it.status] }

    val providerFqName = projectionProviderFqName("kronos-test:main")
    val provider = Class.forName(providerFqName)
        .getDeclaredConstructor()
        .newInstance() as GeneratedTypeProvider
    val kPojoContributions = mutableListOf<ProjectionKPojoContribution>()
    val enumTypes = linkedMapOf<KType, List<String>>()
    provider.contributeTo(object : GeneratedTypeRegistrar {
        override fun registerKPojo(
            type: KType,
            ownerId: String,
            constructorSignature: String,
            factory: KPojoFactory
        ) {
            kPojoContributions += ProjectionKPojoContribution(type, ownerId, factory)
        }

        override fun registerEnum(type: KType, entryNames: List<String>, factory: EnumFactory) {
            enumTypes[type] = entryNames
        }
    })

    val projection = kPojoContributions.singleOrNull {
        it.ownerId.startsWith("com.kotlinorm.generated.projection.KronosSelectResult_")
    } ?: return "Fail: synthetic projection factory was not contributed: $kPojoContributions"
    val instance = projection.factory.create(projection.type)
    val columnNames = instance.__columns.map { it.name }
    if (columnNames != listOf("id", "status")) return "Fail: projection columns were $columnNames"
    val expectedEnumMetadata = mapOf(typeOf<ProjectionStatus>() to listOf("READY", "ARCHIVED"))
    if (enumTypes != expectedEnumMetadata) return "Fail: projection enum metadata was $enumTypes"
    return "OK"
}

data class ProjectionKPojoContribution(
    val type: KType,
    val ownerId: String,
    val factory: KPojoFactory,
)

fun projectionProviderFqName(moduleCoordinate: String): String {
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(moduleCoordinate.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        .take(16)
    return "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_$hash"
}
