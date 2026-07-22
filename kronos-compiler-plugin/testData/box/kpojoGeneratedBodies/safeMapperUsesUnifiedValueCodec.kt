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

// Verifies that safe mapping performs one unified ValueCodec conversion per present field.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.serializedValueCodec
import com.kotlinorm.interfaces.valueCodec
import com.kotlinorm.utils.Extensions.mapperTo
import com.kotlinorm.utils.Extensions.safeMapperTo
import java.time.LocalDateTime
import kotlin.reflect.KType
import kotlin.reflect.typeOf

enum class MapperCodecStatus { READY, CLOSED }

data class MapperCodecMarker(val value: String)

data class MapperCodecTarget(
    var number: Int? = null,
    var status: MapperCodecStatus? = null,
    @DateTimeFormat("dd/MM/yyyy HH:mm:ss")
    var createdAt: LocalDateTime? = null,
    @Serialize
    var statuses: List<MapperCodecStatus>? = null,
    var marker: MapperCodecMarker? = null,
) : KPojo

fun box(): String {
    var markerSupports = 0
    var markerConversions = 0
    var serializedConversions = 0
    var serializedTargetType: KType? = null

    val serializedRegistration = Kronos.registerValueCodec(serializedValueCodec(
        encode = { _, type -> error("Unexpected encode for $type") },
        decode = { text, type ->
            serializedConversions++
            serializedTargetType = type
            text.split(',').map { MapperCodecStatus.valueOf(it) }
        },
    ))
    val markerRegistration = Kronos.registerValueCodec(valueCodec(
        supports = { value, context ->
            val matches = value is String &&
                context.direction == ValueCodecDirection.DECODE &&
                context.origin == ValueCodecOrigin.MAP &&
                context.storage == ValueStorage.NONE &&
                context.targetType == typeOf<MapperCodecMarker?>()
            if (matches) markerSupports++
            matches
        },
        convert = { value, _ ->
            markerConversions++
            MapperCodecMarker((value as String).removePrefix("marker:"))
        },
    ))
    val originalStrict = Kronos.strictSetValue
    Kronos.strictSetValue = false

    try {
        val input = linkedMapOf<String, Any?>(
            "number" to "12",
            "status" to "READY",
            "createdAt" to "21/07/2026 12:34:56",
            "statuses" to "READY,CLOSED",
            "marker" to "marker:custom",
        )
        val raw = input.mapperTo<MapperCodecTarget>()
        if (raw != MapperCodecTarget()) return "Fail: raw mapper performed conversion: $raw"
        if (markerConversions != 0 || serializedConversions != 0) {
            return "Fail: raw mapper called codecs: marker=$markerConversions serialized=$serializedConversions"
        }

        val safe = input.safeMapperTo<MapperCodecTarget>()
        val expected = MapperCodecTarget(
            number = 12,
            status = MapperCodecStatus.READY,
            createdAt = LocalDateTime.of(2026, 7, 21, 12, 34, 56),
            statuses = listOf(MapperCodecStatus.READY, MapperCodecStatus.CLOSED),
            marker = MapperCodecMarker("custom"),
        )
        if (safe != expected) return "Fail: safe mapper result was $safe"
        if (markerSupports != 1) return "Fail: marker supports was called $markerSupports times"
        if (markerConversions != 1) return "Fail: marker convert was called $markerConversions times"
        if (serializedConversions != 1) return "Fail: serialized decode was called $serializedConversions times"
        if (serializedTargetType != typeOf<List<MapperCodecStatus>?>()) {
            return "Fail: serialized target type was $serializedTargetType"
        }
    } finally {
        Kronos.strictSetValue = originalStrict
        markerRegistration.close()
        serializedRegistration.close()
    }

    return "OK"
}
