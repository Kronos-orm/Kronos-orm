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

// Verifies field-valued RHS expressions and raw `.value` expressions lower to SqlExpr.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_field_value_criteria")
data class FieldValueCriteriaUser(
    var id: Int? = null,
    var age: Int? = null,
    var otherAge: Int? = null,
) : KPojo

data class CapturedFieldValueCondition(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun fieldValueWhere(user: FieldValueCriteriaUser, block: ToFilter<FieldValueCriteriaUser, Boolean?>): CapturedFieldValueCondition {
    var result: CapturedFieldValueCondition? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedFieldValueCondition(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedFieldValueCondition(null, emptyMap())
}

fun fieldValueParameter(actual: CapturedFieldValueCondition, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = FieldValueCriteriaUser(id = 1, age = 36, otherAge = 40)
    val fieldRhs = fieldValueWhere(user) { it.age == it.otherAge }.expr as? SqlExpr.Binary
    val fieldLeft = fieldRhs?.left as? SqlExpr.Column
    val fieldRight = fieldRhs?.right as? SqlExpr.Column
    val rawValue = fieldValueWhere(user) { it.age == it.otherAge.value }
    val rawExpr = rawValue.expr as? SqlExpr.Binary
    val rawLeft = rawExpr?.left as? SqlExpr.Column

    return when {
        fieldLeft?.columnName != "age" -> "Fail: fieldRhs left was ${fieldLeft?.columnName}"
        fieldRhs?.operator != SqlBinaryOperator.Equal -> "Fail: fieldRhs operator was ${fieldRhs?.operator}"
        fieldRight?.columnName != "other_age" -> "Fail: fieldRhs right was ${fieldRight?.columnName}"
        rawLeft?.columnName != "age" -> "Fail: rawValue left was ${rawLeft?.columnName}"
        rawExpr?.operator != SqlBinaryOperator.Equal -> "Fail: rawValue operator was ${rawExpr?.operator}"
        fieldValueParameter(rawValue, rawExpr?.right) != 40 -> "Fail: rawValue value was ${fieldValueParameter(rawValue, rawExpr?.right)}"
        else -> "OK"
    }
}
