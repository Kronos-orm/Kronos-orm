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

// Verifies reversed binary comparisons normalize to criteria on the KPojo field.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_reversed_comparison")
data class ReversedComparisonUser(
    var id: Int? = null,
    var age: Int? = null,
) : KPojo

fun reversedWhere(user: ReversedComparisonUser, block: ToFilter<ReversedComparisonUser, Boolean?>): Criteria? {
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

    val user = ReversedComparisonUser(age = 36)
    val lessThanField = reversedWhere(user) { 18 < it.age }
    val greaterThanField = reversedWhere(user) { 40 > it.age }

    return when {
        lessThanField?.field?.name != "age" -> "Fail: less field was ${lessThanField?.field?.name}"
        lessThanField.type != ConditionType.GT -> "Fail: less type was ${lessThanField.type}"
        lessThanField.value != 18 -> "Fail: less value was ${lessThanField.value}"
        greaterThanField?.field?.name != "age" -> "Fail: greater field was ${greaterThanField?.field?.name}"
        greaterThanField.type != ConditionType.LT -> "Fail: greater type was ${greaterThanField.type}"
        greaterThanField.value != 40 -> "Fail: greater value was ${greaterThanField.value}"
        else -> "OK"
    }
}
