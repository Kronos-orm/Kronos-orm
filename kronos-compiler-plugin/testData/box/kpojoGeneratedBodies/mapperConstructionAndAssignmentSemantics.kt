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

// Verifies unified factory construction and raw/safe generated assignment semantics.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.Extensions.mapperTo
import com.kotlinorm.utils.Extensions.patchTo
import com.kotlinorm.utils.Extensions.safeMapperTo
import com.kotlinorm.utils.KPojoFactory
import kotlin.reflect.typeOf

data class MapperContractSource(
    var id: Int? = null,
    var name: String? = null,
    @Ignore([IgnoreAction.TO_MAP])
    var hidden: String? = null,
) : KPojo

data class MapperContractTarget(
    val id: Int? = 100,
    val name: String? = "generated-name",
    @Ignore([IgnoreAction.FROM_MAP])
    val ignored: String? = "generated-ignored",
    val required: String = "generated-required",
    val hidden: String? = "generated-hidden",
) : KPojo

fun box(): String {
    val targetType = typeOf<MapperContractTarget>()
    val registration = Kronos.registerKPojoFactory(targetType, KPojoFactory { requestedType ->
        if (requestedType != targetType) error("Unexpected requested type $requestedType")
        MapperContractTarget(
            id = 500,
            name = "factory-name",
            ignored = "factory-ignored",
            required = "factory-required",
            hidden = "factory-hidden",
        )
    })

    try {
        val raw = mapOf<String, Any?>(
            "id" to null,
            "ignored" to "changed",
            "required" to "raw-required",
            "unknown" to "ignored",
        ).mapperTo(targetType) as MapperContractTarget

        val safe = mapOf<String, Any?>(
            "id" to 7L,
            "ignored" to "changed",
            "required" to "safe-required",
        ).safeMapperTo<MapperContractTarget>()

        val source = MapperContractSource(id = 9, name = "source-name", hidden = "source-hidden")
        val rawFromPojo = source.mapperTo(targetType) as MapperContractTarget
        val safeFromPojo = source.safeMapperTo<MapperContractTarget>()
        val patched = source.patchTo(targetType, "name" to "patched", "unknown" to "ignored") as MapperContractTarget
        val explicitNullFailure = runCatching {
            mapOf<String, Any?>("required" to null).safeMapperTo(targetType)
        }.exceptionOrNull()

        val failure = when {
            raw.id != null -> "raw explicit null id was ${raw.id}"
            raw.name != "factory-name" -> "raw missing name was ${raw.name}"
            raw.ignored != "factory-ignored" -> "raw ignored field was ${raw.ignored}"
            raw.required != "raw-required" -> "raw required field was ${raw.required}"
            safe.id != 7 -> "safe numeric id was ${safe.id}"
            safe.name != "factory-name" -> "safe missing name was ${safe.name}"
            safe.ignored != "factory-ignored" -> "safe ignored field was ${safe.ignored}"
            safe.required != "safe-required" -> "safe required field was ${safe.required}"
            rawFromPojo != MapperContractTarget(9, "source-name", "factory-ignored", "factory-required", "factory-hidden") ->
                "raw KPojo mapping was $rawFromPojo"
            safeFromPojo != MapperContractTarget(9, "source-name", "factory-ignored", "factory-required", "factory-hidden") ->
                "safe KPojo mapping was $safeFromPojo"
            patched != MapperContractTarget(9, "patched", "factory-ignored", "factory-required", "factory-hidden") ->
                "patch mapping was $patched"
            explicitNullFailure !is ValueMappingException ->
                "safe non-null explicit null failure was ${explicitNullFailure?.let { it::class.qualifiedName }}"
            else -> null
        }
        if (failure != null) return "Fail: $failure"
    } finally {
        registration.close()
    }

    val restored = Kronos.createKPojo(targetType) as MapperContractTarget
    if (restored != MapperContractTarget()) return "Fail: generated factory was not restored: $restored"
    return "OK"
}
