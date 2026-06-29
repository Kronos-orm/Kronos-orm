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

// Verifies contains-style string criteria and array membership criteria.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_contains_membership")
data class ContainsMembershipUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

fun containsMembershipWhere(
    user: ContainsMembershipUser,
    block: ToFilter<ContainsMembershipUser, Boolean?>,
): Criteria? {
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

    val user = ContainsMembershipUser(id = 2, name = "Ada")
    val contains = containsMembershipWhere(user) { it.name.contains("d") }
    val ids = arrayOf<Int?>(1, 2, 3)
    val membership = containsMembershipWhere(user) { it.id in ids }

    return when {
        contains?.type != ConditionType.LIKE -> "Fail: contains type was ${contains?.type}"
        contains.value != "%d%" -> "Fail: contains value was ${contains.value}"
        membership?.type != ConditionType.IN -> "Fail: membership type was ${membership?.type}"
        membership.field.name != "id" -> "Fail: membership field was ${membership.field.name}"
        membership.value !== ids -> "Fail: membership value was ${membership.value}"
        else -> "OK"
    }
}
