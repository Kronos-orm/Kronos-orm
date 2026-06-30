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

// Verifies that generated KPojo mapping bodies can write source val properties.

import com.kotlinorm.interfaces.KPojo

data class ValMappedUser(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null,
) : KPojo

fun box(): String {
    val direct = ValMappedUser()
    direct["id"] = 7
    direct["name"] = "Ada"

    val mapped = ValMappedUser().fromMapData<ValMappedUser>(
        mapOf("id" to 8, "name" to "Grace", "age" to 44)
    )
    val safeMapped = ValMappedUser().safeFromMapData<ValMappedUser>(
        mapOf("id" to 9, "name" to "Lin", "age" to 36)
    )

    return when {
        direct.id != 7 -> "Fail: direct id was ${direct.id}"
        direct.name != "Ada" -> "Fail: direct name was ${direct.name}"
        mapped.id != 8 -> "Fail: mapped id was ${mapped.id}"
        mapped.name != "Grace" -> "Fail: mapped name was ${mapped.name}"
        mapped.age != 44 -> "Fail: mapped age was ${mapped.age}"
        safeMapped.id != 9 -> "Fail: safe mapped id was ${safeMapped.id}"
        safeMapped.name != "Lin" -> "Fail: safe mapped name was ${safeMapped.name}"
        safeMapped.age != 36 -> "Fail: safe mapped age was ${safeMapped.age}"
        else -> "OK"
    }
}
