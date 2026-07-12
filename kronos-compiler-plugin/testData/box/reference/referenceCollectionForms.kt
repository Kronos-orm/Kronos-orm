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

// Verifies reference DSL extraction from listOf and arrayOf property-reference forms.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToReference

@Table(name = "tb_reference_collection")
data class ReferenceCollectionUser(
    var id: Int? = null,
    @Column("user_name")
    var name: String? = null,
    var status: Int? = null,
) : KPojo {
    fun column(name: String): Field = __columns.single { it.name == name }
}

fun ReferenceCollectionUser.collectReferenceItems(block: ToReference<ReferenceCollectionUser, Any?>): List<Field> {
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

    val user = ReferenceCollectionUser()
    val list = user.collectReferenceItems { listOf(ReferenceCollectionUser::id, ReferenceCollectionUser::name) }
    val array = user.collectReferenceItems { arrayOf(ReferenceCollectionUser::status, ReferenceCollectionUser::name) }

    return when {
        list != listOf(user.column("id"), user.column("name")) -> "Fail: list references were ${list.map { it.name }}"
        list[1].columnName != "user_name" -> "Fail: list column name was ${list[1].columnName}"
        array != listOf(user.column("status"), user.column("name")) -> "Fail: array references were ${array.map { it.name }}"
        array[1].columnName != "user_name" -> "Fail: array column name was ${array[1].columnName}"
        else -> "OK"
    }
}
