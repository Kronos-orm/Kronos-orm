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

// Verifies recursive enum metadata discovery, Ignore direction handling, and direct entry factories.

import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.database.SqlExecutor.queryOne
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.KPojoFactory
import java.security.MessageDigest
import kotlin.reflect.KType
import kotlin.reflect.typeOf

enum class DirectStatus { DIRECT_ON, DIRECT_OFF }
enum class NullableStatus { NULLABLE_ON, NULLABLE_OFF }
enum class ListStatus { LIST_ON, LIST_OFF }
enum class NestedStatus { NESTED_ON, NESTED_OFF }
enum class FromMapStatus { FROM_ON, FROM_OFF }
enum class ToMapStatus { TO_ON, TO_OFF }
enum class SerializedStatus { SERIALIZED_ON, SERIALIZED_OFF }
enum class IgnoredStatus { IGNORED_ON, IGNORED_OFF }
enum class ScalarStatus { SCALAR_ON, SCALAR_OFF }

data class EnumMetadataRow(
    var direct: DirectStatus = DirectStatus.DIRECT_ON,
    var nullable: NullableStatus? = null,
    var list: List<ListStatus> = emptyList(),
    var nested: Map<String, List<NestedStatus?>> = emptyMap(),
    @Ignore([IgnoreAction.FROM_MAP])
    var fromMap: FromMapStatus? = null,
    @Ignore([IgnoreAction.TO_MAP])
    var toMap: ToMapStatus? = null,
    @Serialize
    var serialized: List<SerializedStatus> = emptyList(),
    @Ignore([IgnoreAction.ALL])
    var ignored: IgnoredStatus? = null,
) : KPojo

@Suppress("UNUSED_PARAMETER")
fun staticallyTypedScalarQuery(wrapper: KronosDataSourceWrapper): ScalarStatus =
    wrapper.queryOne<ScalarStatus>("select status")

fun box(): String {
    val identity = enumMetadataProviderIdentity("kronos-test:main")
    val provider = Class.forName(identity.second)
        .getDeclaredConstructor()
        .newInstance() as GeneratedTypeProvider
    val metadata = linkedMapOf<KType, EnumContribution>()
    provider.contributeTo(object : GeneratedTypeRegistrar {
        override fun registerKPojo(
            type: KType,
            ownerId: String,
            constructorSignature: String,
            factory: KPojoFactory
        ) = Unit

        override fun registerEnum(type: KType, entryNames: List<String>, factory: EnumFactory) {
            metadata[type] = EnumContribution(entryNames, factory)
        }
    })

    val expectedEntries = linkedMapOf(
        typeOf<DirectStatus>() to listOf("DIRECT_ON", "DIRECT_OFF"),
        typeOf<FromMapStatus>() to listOf("FROM_ON", "FROM_OFF"),
        typeOf<ListStatus>() to listOf("LIST_ON", "LIST_OFF"),
        typeOf<NestedStatus>() to listOf("NESTED_ON", "NESTED_OFF"),
        typeOf<NullableStatus>() to listOf("NULLABLE_ON", "NULLABLE_OFF"),
        typeOf<ScalarStatus>() to listOf("SCALAR_ON", "SCALAR_OFF"),
        typeOf<SerializedStatus>() to listOf("SERIALIZED_ON", "SERIALIZED_OFF"),
        typeOf<ToMapStatus>() to listOf("TO_ON", "TO_OFF"),
    )
    val actualEntries = metadata.mapValues { it.value.entryNames }
    if (provider.id != identity.first) return "Fail: provider id was ${provider.id}"
    if (actualEntries != expectedEntries) return "Fail: enum metadata was $actualEntries"
    if (typeOf<IgnoredStatus>() in metadata) return "Fail: Ignore(ALL) metadata was contributed"

    val decodedNames = metadata.mapValues { (_, contribution) ->
        contribution.factory.create(contribution.entryNames.first())?.name
    }
    val expectedDecodedNames = expectedEntries.mapValues { it.value.first() }
    if (decodedNames != expectedDecodedNames) return "Fail: decoded names were $decodedNames"
    if (metadata.values.any { it.factory.create("UNKNOWN") != null }) {
        return "Fail: unknown enum name produced an entry"
    }
    return "OK"
}

data class EnumContribution(
    val entryNames: List<String>,
    val factory: EnumFactory,
)

fun enumMetadataProviderIdentity(moduleCoordinate: String): Pair<String, String> {
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(moduleCoordinate.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        .take(16)
    return "$moduleCoordinate#$hash" to
        "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_$hash"
}
