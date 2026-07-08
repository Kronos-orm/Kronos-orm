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

// Verifies function and operator expressions can appear on either side of condition comparisons.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_function_comparison")
data class FunctionComparisonUser(
    var id: Int? = null,
    var name: String? = null,
    var score: Int? = null,
) : KPojo

data class CapturedFunctionComparison(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun functionComparisonWhere(
    user: FunctionComparisonUser,
    block: ToFilter<FunctionComparisonUser, Boolean?>,
): CapturedFunctionComparison {
    var result: CapturedFunctionComparison? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedFunctionComparison(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedFunctionComparison(null, emptyMap())
}

fun functionComparisonParameter(actual: CapturedFunctionComparison, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun expectFunctionBinary(
    label: String,
    actual: CapturedFunctionComparison,
    operator: SqlBinaryOperator,
    value: Any?,
): String? {
    val binary = actual.expr as? SqlExpr.Binary
    val function = binary?.left as? SqlExpr.Function
    val field = function?.args?.singleOrNull() as? SqlExpr.Column
    return when {
        binary == null -> "Fail: $label condition was ${actual.expr}"
        function == null -> "Fail: $label left was ${binary.left}"
        function.name.last != "LENGTH" -> "Fail: $label function was ${function.name.last}"
        field?.columnName != "name" -> "Fail: $label field was ${field?.columnName}"
        binary.operator != operator -> "Fail: $label operator was ${binary.operator}"
        functionComparisonParameter(actual, binary.right) != value ->
            "Fail: $label value was ${functionComparisonParameter(actual, binary.right)}"
        else -> null
    }
}

fun expectOperatorBinary(
    label: String,
    actual: CapturedFunctionComparison,
    operator: SqlBinaryOperator,
    value: Any?,
): String? {
    val binary = actual.expr as? SqlExpr.Binary
    val left = binary?.left as? SqlExpr.Binary
    val field = left?.left as? SqlExpr.Column
    return when {
        binary == null -> "Fail: $label condition was ${actual.expr}"
        left == null -> "Fail: $label left was ${binary.left}"
        left.operator != SqlBinaryOperator.Plus -> "Fail: $label left operator was ${left.operator}"
        field?.columnName != "score" -> "Fail: $label field was ${field?.columnName}"
        binary.operator != operator -> "Fail: $label operator was ${binary.operator}"
        functionComparisonParameter(actual, binary.right) != value ->
            "Fail: $label value was ${functionComparisonParameter(actual, binary.right)}"
        else -> null
    }
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = FunctionComparisonUser(name = "Ada", score = 7)
    val failures = listOfNotNull(
        expectFunctionBinary("functionEqLeft", functionComparisonWhere(user) { f.length(it.name) == 3 }, SqlBinaryOperator.Equal, 3),
        expectFunctionBinary("functionEqRight", functionComparisonWhere(user) { 3 == f.length(it.name) }, SqlBinaryOperator.Equal, 3),
        expectOperatorBinary("operatorLeLeft", functionComparisonWhere(user) { it.score + 1 <= 10 }, SqlBinaryOperator.LessThanEqual, 10),
        expectOperatorBinary("operatorGeRight", functionComparisonWhere(user) { 10 <= it.score + 1 }, SqlBinaryOperator.GreaterThanEqual, 10),
    )

    return failures.firstOrNull() ?: "OK"
}
