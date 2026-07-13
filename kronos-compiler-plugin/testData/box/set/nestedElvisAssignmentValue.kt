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

// Verifies nested set blocks still bind Elvis RHS expressions as assignment values.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSet

@Table(name = "tb_nested_elvis_user")
data class NestedElvisUser(
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
) : KPojo {
    fun column(name: String): Field {
        return __columns.single { it.name == name }
    }
}

data class NestedElvisSnapshot(
    val fields: List<Field>,
    val fieldParamMap: Map<Field, Any?>,
)

fun NestedElvisUser.collectNestedElvisSet(block: ToSet<NestedElvisUser, Unit>): NestedElvisSnapshot {
    var result: NestedElvisSnapshot? = null
    afterSet {
        block!!(it)
        result = NestedElvisSnapshot(fields.toList(), fieldParamMap.toMap())
    }
    return result ?: error("set block did not run")
}

fun nestedName(): String? = null

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = NestedElvisUser()
    val result = user.collectNestedElvisSet {
        run {
            it.name = nestedName() ?: "Anonymous"
        }
        run {
            it.status = 1
        }
    }

    val names = result.fields.map { it.name }
    return when {
        names != listOf("name", "status") -> "Fail: fields were $names"
        result.fieldParamMap[user.column("name")] != "Anonymous" ->
            "Fail: name value was ${result.fieldParamMap[user.column("name")]}"
        result.fieldParamMap[user.column("status")] != 1 ->
            "Fail: status value was ${result.fieldParamMap[user.column("status")]}"
        else -> "OK"
    }
}
