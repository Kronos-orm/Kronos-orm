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

// Verifies selecting the KPojo receiver expands to every column field and excludes nested KPojo values.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect

@Table(name = "tb_get_value_address")
data class GetValueAddress(
    var id: Int? = null,
    var city: String? = null,
) : KPojo

@Table(name = "tb_get_value_user")
data class GetValueUser(
    var id: Int? = null,
    var name: String? = null,
    var address: GetValueAddress? = null,
) : KPojo

fun GetValueUser.collectGetValueSelect(block: ToSelect<GetValueUser, Any?>): List<Field> {
    val result = mutableListOf<Field>()
    afterSelect {
        block!!(it)
        result += fields
    }
    return result
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val fields = GetValueUser().collectGetValueSelect { it }
    val names = fields.map { it.name }

    return when {
        names != listOf("id", "name") -> "Fail: fields were $names"
        fields.any { it.name == "address" } -> "Fail: nested KPojo field was selected"
        else -> "OK"
    }
}
