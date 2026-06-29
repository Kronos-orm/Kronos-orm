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

// Verifies not-equal comparisons and OR conditions are lowered into Criteria trees.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_not_equal_or")
data class NotEqualOrUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo

fun notEqualOrWhere(user: NotEqualOrUser, block: ToFilter<NotEqualOrUser, Boolean?>): Criteria? {
    var result: Criteria? = null
    user.afterFilter {
        criteriaParamMap = user.toDataMap()
        block!!(it)
        result = criteria?.children?.singleOrNull()
    }
    return result
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = NotEqualOrUser(id = 1, name = "Ada", age = 36)
    val notEqual = notEqualOrWhere(user) { it.name != "Grace" }
    val or = notEqualOrWhere(user) { it.name == "Ada" || it.age < 18 }
    val negatedOr = notEqualOrWhere(user) { !(it.name == "Ada" || it.age < 18) }

    return when {
        notEqual?.type != ConditionType.EQUAL -> "Fail: notEqual type was ${notEqual?.type}"
        notEqual.not != true -> "Fail: notEqual not was ${notEqual.not}"
        notEqual.value != "Grace" -> "Fail: notEqual value was ${notEqual.value}"
        or?.type != ConditionType.OR -> "Fail: or type was ${or?.type}"
        or.children.size != 2 -> "Fail: or child size was ${or.children.size}"
        negatedOr?.type != ConditionType.AND -> "Fail: negatedOr type was ${negatedOr?.type}"
        negatedOr.children.any { it?.not != true } -> "Fail: negatedOr children were ${negatedOr.children.map { it?.not }}"
        else -> "OK"
    }
}
