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

// Verifies select field projection recognizes Kotlin collection factory calls.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect

@Table(name = "tb_collection_factory_fields")
data class CollectionFactoryFieldUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo

fun CollectionFactoryFieldUser.collectFactoryFields(block: ToSelect<CollectionFactoryFieldUser, Any?>): List<Field> {
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

    val user = CollectionFactoryFieldUser()
    val listFields = user.collectFactoryFields { listOf<Any?>(it.id, it.name) }
    val setFields = user.collectFactoryFields { setOf<Any?>(it.age, it.name) }
    val arrayFields = user.collectFactoryFields { arrayOf<Any?>(it.name, it.id) }

    return when {
        listFields.map { it.name } != listOf("id", "name") -> "Fail: list fields were ${listFields.map { it.name }}"
        setFields.map { it.name } != listOf("age", "name") -> "Fail: set fields were ${setFields.map { it.name }}"
        arrayFields.map { it.name } != listOf("name", "id") -> "Fail: array fields were ${arrayFields.map { it.name }}"
        else -> "OK"
    }
}
