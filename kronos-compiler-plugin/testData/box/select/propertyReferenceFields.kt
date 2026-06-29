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

// Verifies property references are accepted as select field expressions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect

@Table(name = "tb_property_reference_fields")
data class PropertyReferenceFieldUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo

fun PropertyReferenceFieldUser.collectReferences(block: ToSelect<PropertyReferenceFieldUser, Any?>): List<Field> {
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

    val references = PropertyReferenceFieldUser().collectReferences {
        [PropertyReferenceFieldUser::id, PropertyReferenceFieldUser::name]
    }

    return when {
        references.map { it.name } != listOf("id", "name") -> "Fail: references were ${references.map { it.name }}"
        else -> "OK"
    }
}
