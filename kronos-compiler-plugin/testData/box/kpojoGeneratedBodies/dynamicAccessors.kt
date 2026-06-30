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

// Verifies that generated dynamic KPojo accessors read and write mutable and source val properties.

import com.kotlinorm.interfaces.KPojo

data class AccessUser(
    var id: Int? = null,
    var name: String? = null,
    val fixed: String? = "initial",
) : KPojo

fun box(): String {
    val user = AccessUser()

    user["id"] = 12
    user["name"] = "Katherine"
    user["fixed"] = "changed"

    return when {
        user["id"] != 12 -> "Fail: dynamic id was ${user["id"]}"
        user["name"] != "Katherine" -> "Fail: dynamic name was ${user["name"]}"
        user.id != 12 -> "Fail: id property was ${user.id}"
        user.name != "Katherine" -> "Fail: name property was ${user.name}"
        user.fixed != "changed" -> "Fail: val property was ${user.fixed}"
        user["missing"] != null -> "Fail: missing field returned ${user["missing"]}"
        else -> "OK"
    }
}
