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

// Verifies set assignment type hints preserve selectable RHS values for nullable and non-null casts.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.types.ToSet

@Table(name = "tb_set_selectable_hint_variant_user")
data class SetSelectableHintVariantUser(
    var id: Int? = null,
    var status: Int? = null,
    var label: String? = null,
) : KPojo {
    fun column(name: String): Field = kronosColumns().single { it.name == name }
}

@Table(name = "tb_set_selectable_hint_variant_order")
data class SetSelectableHintVariantOrder(
    var id: Int? = null,
    var status: Int? = null,
    var label: String? = null,
) : KPojo

data class SetSelectableHintVariantSnapshot(
    val fields: List<Field>,
    val fieldParamMap: Map<Field, Any?>,
)

fun SetSelectableHintVariantUser.collectSet(block: ToSet<SetSelectableHintVariantUser, Unit>): SetSelectableHintVariantSnapshot {
    var result: SetSelectableHintVariantSnapshot? = null
    afterSet {
        block!!(it)
        result = SetSelectableHintVariantSnapshot(fields.toList(), fieldParamMap.toMap())
    }
    return result ?: error("set block did not run")
}

@Suppress("CAST_NEVER_SUCCEEDS")
fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = SetSelectableHintVariantUser()
    val nullableQuery = SetSelectableHintVariantOrder()
        .select { it.status }
        .where { it.status == 1 }
        .limit(1)
    val nonNullQuery = SetSelectableHintVariantOrder()
        .select { it.label }
        .where { it.label == "ready" }
        .limit(1)

    val result = user.collectSet {
        it.status = (nullableQuery as Int?)
        it.label = (nonNullQuery as String)
    }
    val nullableValue = result.fieldParamMap[user.column("status")]
    val nonNullValue = result.fieldParamMap[user.column("label")]

    val failures = listOfNotNull(
        expectSetSelectableHintVariant(result.fields.map { it.name } == listOf("status", "label")) {
            "fields were ${result.fields.map { it.name }}"
        },
        expectSetSelectableHintVariant(nullableValue is KSelectable<*>) {
            "nullable value was ${nullableValue?.let { it::class.qualifiedName }}"
        },
        expectSetSelectableHintVariant(nullableValue === nullableQuery) { "nullable selectable RHS was not preserved" },
        expectSetSelectableHintVariant(nonNullValue is KSelectable<*>) {
            "non-null value was ${nonNullValue?.let { it::class.qualifiedName }}"
        },
        expectSetSelectableHintVariant(nonNullValue === nonNullQuery) { "non-null selectable RHS was not preserved" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expectSetSelectableHintVariant(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

