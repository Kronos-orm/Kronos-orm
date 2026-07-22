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

// Verifies indirect KPojo generation preserves explicit property and function overrides.

import com.kotlinorm.interfaces.KPojo
import kotlin.reflect.typeOf

interface ExplicitIndirectKPojo : KPojo

data class ExplicitIndirectEntity(
    var id: Int? = null,
    var marker: String = "initial",
) : ExplicitIndirectKPojo {
    override var __tableName: String
        get() = "explicit_table"
        set(_) = Unit

    override fun toDataMap(): MutableMap<String, Any?> = mutableMapOf("explicit" to id)

    override operator fun get(name: String): Any? = "explicit:$name"

    override operator fun set(name: String, value: Any?) {
        marker = "set:$name=$value"
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : KPojo> fromMapData(map: Map<String, Any?>): T {
        marker = "from:${map["value"]}"
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : KPojo> safeFromMapData(map: Map<String, Any?>): T {
        marker = "safe:${map["value"]}"
        return this as T
    }
}

fun box(): String {
    val entity = ExplicitIndirectEntity(7)
    if (entity.__kType != typeOf<ExplicitIndirectEntity>()) return "Fail: KType was ${entity.__kType}"
    if (entity.__tableName != "explicit_table") return "Fail: table name was ${entity.__tableName}"
    if (entity.toDataMap() != mutableMapOf<String, Any?>("explicit" to 7)) {
        return "Fail: toDataMap was ${entity.toDataMap()}"
    }
    if (entity["id"] != "explicit:id") return "Fail: get returned ${entity["id"]}"

    entity["id"] = 9
    if (entity.marker != "set:id=9") return "Fail: set marker was ${entity.marker}"
    if (entity.fromMapData<ExplicitIndirectEntity>(mapOf("value" to 11)) !== entity) {
        return "Fail: fromMapData returned another instance"
    }
    if (entity.marker != "from:11") return "Fail: fromMapData marker was ${entity.marker}"
    if (entity.safeFromMapData<ExplicitIndirectEntity>(mapOf("value" to 13)) !== entity) {
        return "Fail: safeFromMapData returned another instance"
    }
    if (entity.marker != "safe:13") return "Fail: safeFromMapData marker was ${entity.marker}"
    return "OK"
}
