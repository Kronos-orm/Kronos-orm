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

// Verifies KPojo minus projection returns all column fields except the excluded properties.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect

@Table(name = "tb_minus_projection")
data class MinusProjectionUser(
    var id: Int? = null,
    var name: String? = null,
    var password: String? = null,
    var token: String? = null,
) : KPojo

fun MinusProjectionUser.collectMinusSelect(block: ToSelect<MinusProjectionUser, Any?>): List<Field> {
    val result = mutableListOf<Field>()
    afterSelect {
        block!!(it)
        result += fields
    }
    return result
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val fields = MinusProjectionUser().collectMinusSelect { it - [it.password, it.token] }
    val names = fields.map { it.name }

    return when {
        names != listOf("id", "name") -> "Fail: fields were $names"
        fields.any { it.name == "password" } -> "Fail: password was not excluded"
        fields.any { it.name == "token" } -> "Fail: token was not excluded"
        else -> "OK"
    }
}
