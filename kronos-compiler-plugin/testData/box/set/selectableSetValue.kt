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

// Verifies that setValue can collect a selectable RHS for later core-side expression assignment.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.types.ToSet

@Table(name = "tb_set_selectable_user")
data class SelectableSetUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo {
    fun column(name: String): Field = __columns.single { it.name == name }
}

@Table(name = "tb_set_selectable_order")
data class SelectableSetOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

data class SelectableSetSnapshot(
    val fields: List<Field>,
    val params: Map<Field, Any?>,
)

fun SelectableSetUser.collectSet(block: ToSet<SelectableSetUser, Unit>): SelectableSetSnapshot {
    var result: SelectableSetSnapshot? = null
    afterSet {
        block!!(it)
        result = SelectableSetSnapshot(fields.toList(), fieldParamMap.toMap())
    }
    return result ?: error("set block did not run")
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = SelectableSetUser()
    val query = SelectableSetOrder()
        .select { it.status }
        .where { it.status == 1 }
        .limit(1)
    val result = user.collectSet {
        setValue(it.column("name"), query)
    }
    val value = result.params[user.column("name")]

    return when {
        result.fields.map { it.name } != listOf("name") -> "Fail: fields were ${result.fields.map { it.name }}"
        value !is KSelectable<*> -> "Fail: value was ${value?.let { it::class.qualifiedName }}"
        value !== query -> "Fail: selectable RHS was not preserved"
        else -> "OK"
    }
}
