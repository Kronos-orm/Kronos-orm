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

// Verifies set assignment values treat `limit(1) as T` as a scalar subquery type hint.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.types.ToSet

@Table(name = "tb_set_scalar_hint_user")
data class SetScalarHintUser(
    var id: Int? = null,
    var status: Int? = null,
) : KPojo {
    fun column(name: String): Field = __columns.single { it.name == name }
}

@Table(name = "tb_set_scalar_hint_order")
data class SetScalarHintOrder(
    var id: Int? = null,
    var status: Int? = null,
) : KPojo

data class SetScalarHintSnapshot(
    val fields: List<Field>,
    val fieldParamMap: Map<Field, Any?>,
)

fun SetScalarHintUser.collectSet(block: ToSet<SetScalarHintUser, Unit>): SetScalarHintSnapshot {
    var result: SetScalarHintSnapshot? = null
    afterSet {
        block!!(it)
        result = SetScalarHintSnapshot(fields.toList(), fieldParamMap.toMap())
    }
    return result ?: error("set block did not run")
}

@Suppress("CAST_NEVER_SUCCEEDS")
fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = SetScalarHintUser()
    val query = SetScalarHintOrder()
        .select { it.status }
        .where { it.status == 7 }
        .limit(1)

    val result = user.collectSet {
        it.status = (query as Int?)
    }
    val value = result.fieldParamMap[user.column("status")]

    return when {
        result.fields.map { it.name } != listOf("status") -> "Fail: fields were ${result.fields.map { it.name }}"
        value !is KSelectable<*> -> "Fail: value was ${value?.let { it::class.qualifiedName }}"
        value !== query -> "Fail: selectable RHS was not preserved"
        else -> "OK"
    }
}
