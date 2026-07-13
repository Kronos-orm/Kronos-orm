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

// Verifies @Ignore without explicit targets behaves as IgnoreAction.ALL.

import com.kotlinorm.annotations.Ignore
import com.kotlinorm.interfaces.KPojo

data class DefaultIgnoredUser(
    var id: Int? = null,
    @Ignore
    var secret: String? = null,
    var name: String? = null,
) : KPojo

fun box(): String {
    val user = DefaultIgnoredUser(1, "secret", "Ada")
    val columns = user.__columns.map { it.name }.toSet()
    val map = user.toDataMap()
    val patched = DefaultIgnoredUser().fromMapData<DefaultIgnoredUser>(
        mapOf("id" to 2, "secret" to "changed", "name" to "Grace")
    )

    return when {
        "secret" in columns -> "Fail: secret column was present"
        "secret" in map -> "Fail: secret map value was present"
        patched.secret != null -> "Fail: secret was ${patched.secret}"
        patched.id != 2 -> "Fail: id was ${patched.id}"
        patched.name != "Grace" -> "Fail: name was ${patched.name}"
        else -> "OK"
    }
}
