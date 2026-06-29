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

// Verifies that no-argument string match properties read values from criteriaParamMap.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_no_arg_string_match")
data class NoArgStringMatchUser(
    var id: Int? = null,
    var name: String? = null,
    var pattern: String? = null,
) : KPojo

fun noArgStringWhere(user: NoArgStringMatchUser, block: ToFilter<NoArgStringMatchUser, Boolean?>): Criteria? {
    var result: Criteria? = null
    user.afterFilter {
        criteriaParamMap = user.toDataMap()
        block!!(it)
        result = criteria?.children?.singleOrNull()
    }
    return result
}

fun expectStringCriteria(
    label: String,
    actual: Criteria?,
    fieldName: String,
    type: ConditionType,
    value: Any?,
    not: Boolean = false,
): String? {
    return when {
        actual == null -> "Fail: $label condition was null"
        actual.field.name != fieldName -> "Fail: $label field was ${actual.field.name}"
        actual.type != type -> "Fail: $label type was ${actual.type}"
        actual.not != not -> "Fail: $label not was ${actual.not}"
        actual.value != value -> "Fail: $label value was ${actual.value}"
        else -> null
    }
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = NoArgStringMatchUser(name = "Ada", pattern = "^A.*")
    val failures = listOfNotNull(
        expectStringCriteria("like", noArgStringWhere(user) { it.name.like }, "name", ConditionType.LIKE, "Ada"),
        expectStringCriteria("notLike", noArgStringWhere(user) { it.name.notLike }, "name", ConditionType.LIKE, "Ada", not = true),
        expectStringCriteria("startsWith", noArgStringWhere(user) { it.name.startsWith }, "name", ConditionType.LIKE, "Ada%"),
        expectStringCriteria("endsWith", noArgStringWhere(user) { it.name.endsWith }, "name", ConditionType.LIKE, "%Ada"),
        expectStringCriteria("contains", noArgStringWhere(user) { it.name.contains }, "name", ConditionType.LIKE, "%Ada%"),
        expectStringCriteria("regexp", noArgStringWhere(user) { it.pattern.regexp }, "pattern", ConditionType.REGEXP, "^A.*"),
        expectStringCriteria("notRegexp", noArgStringWhere(user) { it.pattern.notRegexp }, "pattern", ConditionType.REGEXP, "^A.*", not = true),
    )

    return failures.firstOrNull() ?: "OK"
}
