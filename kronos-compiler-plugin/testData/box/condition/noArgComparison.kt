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

// Verifies no-argument comparison properties read the current KPojo values.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_no_arg_comparison")
data class NoArgComparisonUser(
    var age: Int? = null,
    var score: Int? = null,
) : KPojo

fun noArgComparisonWhere(user: NoArgComparisonUser, block: ToFilter<NoArgComparisonUser, Boolean?>): Criteria? {
    var result: Criteria? = null
    user.afterFilter {
        criteriaParamMap = user.toDataMap()
        block!!(it)
        result = criteria?.children?.singleOrNull()
    }
    return result
}

fun expectComparison(label: String, criteria: Criteria?, type: ConditionType, value: Any?): String? {
    return when {
        criteria == null -> "Fail: $label condition was null"
        criteria.field.name != "age" -> "Fail: $label field was ${criteria.field.name}"
        criteria.type != type -> "Fail: $label type was ${criteria.type}"
        criteria.value != value -> "Fail: $label value was ${criteria.value}"
        else -> null
    }
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = NoArgComparisonUser(age = 36, score = 99)
    val failures = listOfNotNull(
        expectComparison("lt", noArgComparisonWhere(user) { it.age.lt }, ConditionType.LT, 36),
        expectComparison("gt", noArgComparisonWhere(user) { it.age.gt }, ConditionType.GT, 36),
        expectComparison("le", noArgComparisonWhere(user) { it.age.le }, ConditionType.LE, 36),
        expectComparison("ge", noArgComparisonWhere(user) { it.age.ge }, ConditionType.GE, 36),
    )

    return failures.firstOrNull() ?: "OK"
}
