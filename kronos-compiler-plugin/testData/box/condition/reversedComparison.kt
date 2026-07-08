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

// Verifies reversed binary comparisons normalize to SqlExpr conditions on the KPojo field.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_reversed_comparison")
data class ReversedComparisonUser(
    var id: Int? = null,
    var age: Int? = null,
) : KPojo

data class CapturedReversedComparison(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun reversedWhere(user: ReversedComparisonUser, block: ToFilter<ReversedComparisonUser, Boolean?>): CapturedReversedComparison {
    var result: CapturedReversedComparison? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedReversedComparison(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedReversedComparison(null, emptyMap())
}

fun capturedValue(actual: CapturedReversedComparison, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = ReversedComparisonUser(age = 36)
    val lessThanField = reversedWhere(user) { 18 < it.age }
    val greaterThanField = reversedWhere(user) { 40 > it.age }
    val lessExpr = lessThanField.expr as? SqlExpr.Binary
    val greaterExpr = greaterThanField.expr as? SqlExpr.Binary
    val lessColumn = lessExpr?.left as? SqlExpr.Column
    val greaterColumn = greaterExpr?.left as? SqlExpr.Column

    return when {
        lessColumn?.columnName != "age" -> "Fail: less field was ${lessColumn?.columnName}"
        lessExpr.operator != SqlBinaryOperator.GreaterThan -> "Fail: less operator was ${lessExpr.operator}"
        capturedValue(lessThanField, lessExpr.right) != 18 -> "Fail: less value was ${capturedValue(lessThanField, lessExpr.right)}"
        greaterColumn?.columnName != "age" -> "Fail: greater field was ${greaterColumn?.columnName}"
        greaterExpr?.operator != SqlBinaryOperator.LessThan -> "Fail: greater operator was ${greaterExpr?.operator}"
        capturedValue(greaterThanField, greaterExpr?.right) != 40 -> "Fail: greater value was ${capturedValue(greaterThanField, greaterExpr?.right)}"
        else -> "OK"
    }
}
