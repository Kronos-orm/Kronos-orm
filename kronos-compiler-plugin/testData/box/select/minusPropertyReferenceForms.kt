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

// Verifies KPojo minus projection accepts property references and property-reference collections.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect

@Table(name = "tb_minus_property_reference")
data class MinusPropertyReferenceUser(
    var id: Int? = null,
    var name: String? = null,
    var password: String? = null,
    var token: String? = null,
) : KPojo

fun MinusPropertyReferenceUser.collectMinusReferenceSelect(
    block: ToSelect<MinusPropertyReferenceUser, Any?>
): List<Field> {
    val result = mutableListOf<Field>()
    afterSelect {
        block!!(it)
        result += fields
    }
    return result
}

fun expectMinusReference(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val source = MinusPropertyReferenceUser()
    val single = source.collectMinusReferenceSelect { it - MinusPropertyReferenceUser::password }
    val list = source.collectMinusReferenceSelect {
        it - listOf(MinusPropertyReferenceUser::password, MinusPropertyReferenceUser::token)
    }

    val failures = listOfNotNull(
        expectMinusReference(single.map { it.name } == listOf("id", "name", "token")) {
            "single fields were ${single.map { it.name }}"
        },
        expectMinusReference(list.map { it.name } == listOf("id", "name")) {
            "list fields were ${list.map { it.name }}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}
