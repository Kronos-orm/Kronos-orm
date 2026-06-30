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

// Verifies that operator function fields work in condition comparisons and string match values.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_operator")
data class OperatorConditionUser(
    var id: Int? = null,
    var score: Int = 0,
    var username: String? = null,
) : KPojo

fun operatorWhere(user: OperatorConditionUser, block: ToFilter<OperatorConditionUser, Boolean?>): Criteria? {
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

    val user = OperatorConditionUser(score = 42, username = "ada")
    val comparison = operatorWhere(user) { it.score + 10 > it.score - 10 }
    val left = comparison?.field as? FunctionField
    val right = comparison?.value as? FunctionField

    val like = operatorWhere(user) { it.username like ("%" + it.username + "%") }
    val likeValue = like?.value as? FunctionField
    val nestedLikeValue = likeValue?.fields?.getOrNull(0)?.first as? FunctionField

    val failures = listOfNotNull(
        expect(comparison?.type == ConditionType.GT) { "comparison type was ${comparison?.type}" },
        expect(left?.functionName == "add") { "left function was ${left?.functionName}" },
        expect(right?.functionName == "sub") { "right function was ${right?.functionName}" },
        expect(like?.type == ConditionType.LIKE) { "like type was ${like?.type}" },
        expect(like?.field?.name == "username") { "like field was ${like?.field?.name}" },
        expect(likeValue?.functionName == "concat") { "like value function was ${likeValue?.functionName}" },
        expect(nestedLikeValue?.functionName == "concat") { "nested like function was ${nestedLikeValue?.functionName}" },
        expect(nestedLikeValue?.fields?.getOrNull(1)?.first?.name == "username") {
            "nested like field was ${nestedLikeValue?.fields?.getOrNull(1)?.first?.name}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
