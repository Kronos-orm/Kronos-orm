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

// Verifies reference DSL extraction from setOf, mutableListOf, and cast property references.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToReference
import kotlin.reflect.KProperty1

@Table(name = "tb_reference_cast")
data class ReferenceCastUser(
    var id: Int? = null,
    @Column("user_name")
    var name: String? = null,
    var status: Int? = null,
) : KPojo {
    fun column(name: String): Field = kronosColumns().single { it.name == name }
}

fun ReferenceCastUser.collectReferenceItems(block: ToReference<ReferenceCastUser, Any?>): List<Field> {
    val result = mutableListOf<Field>()
    afterReference {
        block!!(it)
        result += fields
    }
    return result
}

fun expectReference(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = ReferenceCastUser()
    val setItems = user.collectReferenceItems {
        setOf(ReferenceCastUser::id, ReferenceCastUser::name)
    }
    val mutableItems = user.collectReferenceItems {
        mutableListOf(ReferenceCastUser::status, ReferenceCastUser::name)
    }
    val castItems = user.collectReferenceItems {
        [
            ReferenceCastUser::id as KProperty1<ReferenceCastUser, Int?>,
            ReferenceCastUser::name as KProperty1<ReferenceCastUser, String?>
        ]
    }

    val failures = listOfNotNull(
        expectReference(setItems == listOf(user.column("id"), user.column("name"))) {
            "set references were ${setItems.map { it.name }}"
        },
        expectReference(setItems[1].columnName == "user_name") { "set column name was ${setItems[1].columnName}" },
        expectReference(mutableItems == listOf(user.column("status"), user.column("name"))) {
            "mutable references were ${mutableItems.map { it.name }}"
        },
        expectReference(mutableItems[1].columnName == "user_name") { "mutable column name was ${mutableItems[1].columnName}" },
        expectReference(castItems == listOf(user.column("id"), user.column("name"))) {
            "cast references were ${castItems.map { it.name }}"
        },
        expectReference(castItems[1].columnName == "user_name") { "cast column name was ${castItems[1].columnName}" },
    )

    return failures.firstOrNull() ?: "OK"
}
