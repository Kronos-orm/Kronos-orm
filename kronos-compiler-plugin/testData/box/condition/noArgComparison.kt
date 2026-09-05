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
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_no_arg_comparison")
data class NoArgComparisonUser(
    var age: Int? = null,
    var score: Int? = null,
) : KPojo

data class CapturedNoArgComparison(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun noArgComparisonWhere(user: NoArgComparisonUser, block: ToFilter<NoArgComparisonUser, Boolean?>): CapturedNoArgComparison {
    var result: CapturedNoArgComparison? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = CapturedNoArgComparison(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedNoArgComparison(null, emptyMap())
}

fun expectComparison(label: String, actual: CapturedNoArgComparison, operator: SqlBinaryOperator, value: Any?): String? {
    val binary = actual.expr as? SqlExpr.Binary
    val column = binary?.left as? SqlExpr.Column
    val parameter = (binary?.right as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named
    return when {
        binary == null -> "Fail: $label condition was ${actual.expr}"
        column?.columnName != "age" -> "Fail: $label field was ${column?.columnName}"
        binary.operator != operator -> "Fail: $label operator was ${binary.operator}"
        parameter == null -> "Fail: $label value expression was ${binary.right}"
        actual.parameters[parameter.name] != value -> "Fail: $label value was ${actual.parameters[parameter.name]}"
        else -> null
    }
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = NoArgComparisonUser(age = 36, score = 99)
    val failures = listOfNotNull(
        expectComparison("lt", noArgComparisonWhere(user) { it.age.lt }, SqlBinaryOperator.LessThan, 36),
        expectComparison("gt", noArgComparisonWhere(user) { it.age.gt }, SqlBinaryOperator.GreaterThan, 36),
        expectComparison("le", noArgComparisonWhere(user) { it.age.le }, SqlBinaryOperator.LessThanEqual, 36),
        expectComparison("ge", noArgComparisonWhere(user) { it.age.ge }, SqlBinaryOperator.GreaterThanEqual, 36),
    )

    return failures.firstOrNull() ?: "OK"
}
