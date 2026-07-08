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

// Verifies reference DSL extraction from class-qualified property references.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToReference

@Table(name = "tb_reference_user")
data class ReferenceUser(
    var id: Int? = null,
    var name: String? = null,
    @Column("phone_number")
    var phone: String? = null,
) : KPojo {
    fun column(name: String): Field {
        return kronosColumns().single { it.name == name }
    }
}

fun ReferenceUser.collectReference(block: ToReference<ReferenceUser, Any?>): List<Field> {
    val result = mutableListOf<Field>()
    afterReference {
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

    val user = ReferenceUser()
    val result = user.collectReference { [ReferenceUser::id, ReferenceUser::name, ReferenceUser::phone] }

    return when {
        result != listOf(user.column("id"), user.column("name"), user.column("phone")) -> "Fail: references were ${result.map { it.name }}"
        result[2].columnName != "phone_number" -> "Fail: columnName was ${result[2].columnName}"
        else -> "OK"
    }
}
