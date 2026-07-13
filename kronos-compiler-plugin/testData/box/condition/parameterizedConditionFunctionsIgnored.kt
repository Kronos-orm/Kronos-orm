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

// Verifies unsupported parameterized and custom condition calls are not treated as Kronos condition DSL.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.types.ToFilter

@Table(name = "tb_parameterized_condition")
data class ParameterizedConditionUser(
    var id: Int? = null,
    var age: Int? = null,
) : KPojo

data class CapturedParameterizedCondition(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun Int?.eq(value: Int): Boolean = this == value

fun Int?.neq(value: Int): Boolean = this != value

fun Int?.gt(value: Int): Boolean = this != null && this > value

fun Int?.customCondition(value: Int): Boolean = this == value

fun parameterizedWhere(
    user: ParameterizedConditionUser,
    block: ToFilter<ParameterizedConditionUser, Boolean?>
): CapturedParameterizedCondition {
    var result: CapturedParameterizedCondition? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedParameterizedCondition(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedParameterizedCondition(null, emptyMap())
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = ParameterizedConditionUser(id = 1, age = 36)
    val eqCall = parameterizedWhere(user) { it.age.eq(18) }
    val neqCall = parameterizedWhere(user) { it.age.neq(18) }
    val gtCall = parameterizedWhere(user) { it.age.gt(18) }
    val customCall = parameterizedWhere(user) { it.age.customCondition(36) }

    return when {
        eqCall.expr != null -> "Fail: eq(value) produced ${eqCall.expr}"
        eqCall.parameters.isNotEmpty() -> "Fail: eq(value) parameters were ${eqCall.parameters}"
        neqCall.expr != null -> "Fail: neq(value) produced ${neqCall.expr}"
        neqCall.parameters.isNotEmpty() -> "Fail: neq(value) parameters were ${neqCall.parameters}"
        gtCall.expr != null -> "Fail: gt(value) produced ${gtCall.expr}"
        gtCall.parameters.isNotEmpty() -> "Fail: gt(value) parameters were ${gtCall.parameters}"
        customCall.expr != null -> "Fail: customCondition(value) produced ${customCall.expr}"
        customCall.parameters.isNotEmpty() -> "Fail: customCondition(value) parameters were ${customCall.parameters}"
        else -> "OK"
    }
}
