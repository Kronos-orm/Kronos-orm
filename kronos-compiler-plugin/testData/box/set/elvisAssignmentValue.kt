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

// Verifies Elvis expressions on the RHS of set assignments are bound as assignment values.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSet

@Table(name = "tb_set_elvis_user")
data class ElvisSetUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo {
    fun column(name: String): Field {
        return __columns.single { it.name == name }
    }
}

data class ElvisSetSnapshot(
    val fields: List<Field>,
    val fieldParamMap: Map<Field, Any?>,
)

fun ElvisSetUser.collectElvisSet(block: ToSet<ElvisSetUser, Unit>): ElvisSetSnapshot {
    var result: ElvisSetSnapshot? = null
    afterSet {
        block!!(it)
        result = ElvisSetSnapshot(fields.toList(), fieldParamMap.toMap())
    }
    return result ?: error("set block did not run")
}

fun displayName(): String? = null

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = ElvisSetUser()
    val result = user.collectElvisSet {
        it.name = displayName() ?: "匿名"
    }

    return when {
        result.fields.map { it.name } != listOf("name") -> "Fail: fields were ${result.fields.map { it.name }}"
        result.fieldParamMap[user.column("name")] != "匿名" -> "Fail: name value was ${result.fieldParamMap[user.column("name")]}"
        else -> "OK"
    }
}
