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

// Verifies that no-argument KPojo equality expands to equality criteria for every column property.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_kpojo_equality")
data class KPojoEqualityUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo

fun kpojoEqualityWhere(user: KPojoEqualityUser, block: ToFilter<KPojoEqualityUser, Boolean?>): Criteria? {
    var result: Criteria? = null
    user.afterFilter {
        criteriaParamMap = user.toDataMap()
        block!!(it)
        result = criteria?.children?.singleOrNull()
    }
    return result
}

fun expectKPojo(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = KPojoEqualityUser(id = 1, name = "Ada", age = 36)
    val equal = kpojoEqualityWhere(user) { it.eq }

    val equalNames = equal?.children?.mapNotNull { it?.field?.name }?.toSet().orEmpty()
    val equalValues = equal?.children?.associate { it!!.field.name to it.value }.orEmpty()

    val failures = listOfNotNull(
        expectKPojo(equal?.type == ConditionType.AND) { "equal type was ${equal?.type}" },
        expectKPojo(equalNames == setOf("id", "name", "age")) { "equal fields were $equalNames" },
        expectKPojo(equalValues["id"] == 1) { "id value was ${equalValues["id"]}" },
        expectKPojo(equalValues["name"] == "Ada") { "name value was ${equalValues["name"]}" },
        expectKPojo(equalValues["age"] == 36) { "age value was ${equalValues["age"]}" },
    )

    return failures.firstOrNull() ?: "OK"
}
