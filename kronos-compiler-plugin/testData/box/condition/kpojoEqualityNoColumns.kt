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

// Verifies KPojo equality on a class with no columns is ignored instead of producing SqlExpr nodes.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.types.ToFilter

@Table(name = "tb_empty_condition")
data class EmptyConditionUser(
    @Ignore([IgnoreAction.ALL])
    var transientName: String? = null,
) : KPojo

data class CapturedEmptyCondition(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>,
)

fun emptyConditionWhere(
    user: EmptyConditionUser,
    block: ToFilter<EmptyConditionUser, Boolean?>,
): CapturedEmptyCondition {
    var result: CapturedEmptyCondition? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedEmptyCondition(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedEmptyCondition(null, emptyMap())
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = EmptyConditionUser("runtime-only")
    val equal = emptyConditionWhere(user) { it.eq }
    val other = EmptyConditionUser("other")
    val objectEqual = emptyConditionWhere(user) { other == it }

    val failures = listOfNotNull(
        expectEmptyCondition(equal.expr == null) { "no-column eq produced ${equal.expr}" },
        expectEmptyCondition(equal.parameters.isEmpty()) { "no-column eq parameters were ${equal.parameters}" },
        expectEmptyCondition(objectEqual.expr == null) { "no-column object equality produced ${objectEqual.expr}" },
        expectEmptyCondition(objectEqual.parameters.isEmpty()) {
            "no-column object equality parameters were ${objectEqual.parameters}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expectEmptyCondition(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
