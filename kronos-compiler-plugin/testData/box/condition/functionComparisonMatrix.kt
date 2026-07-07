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

// Verifies function and operator comparison variants cover every comparison direction.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_function_matrix")
data class FunctionMatrixUser(
    var id: Int? = null,
    var name: String? = null,
    var score: Int? = null,
) : KPojo

data class CapturedFunctionMatrix(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun functionMatrixWhere(
    user: FunctionMatrixUser,
    block: ToFilter<FunctionMatrixUser, Boolean?>,
): CapturedFunctionMatrix {
    var result: CapturedFunctionMatrix? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedFunctionMatrix(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedFunctionMatrix(null, emptyMap())
}

fun functionMatrixParameter(actual: CapturedFunctionMatrix, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun expectFunctionMatrix(
    label: String,
    actual: CapturedFunctionMatrix,
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
        functionMatrixParameter(actual, binary.right) != value ->
            "Fail: $label value was ${functionMatrixParameter(actual, binary.right)}"
        else -> null
    }
}

fun expectOperatorMatrix(
    label: String,
    actual: CapturedFunctionMatrix,
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
        functionMatrixParameter(actual, binary.right) != value ->
            "Fail: $label value was ${functionMatrixParameter(actual, binary.right)}"
        else -> null
    }
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = FunctionMatrixUser(name = "Ada", score = 7)
    val failures = listOfNotNull(
        expectFunctionMatrix("functionGtLeft", functionMatrixWhere(user) { f.length(it.name) > 2 }, SqlBinaryOperator.GreaterThan, 2),
        expectFunctionMatrix(
            "functionGeLeft",
            functionMatrixWhere(user) { f.length(it.name) >= 3 },
            SqlBinaryOperator.GreaterThanEqual,
            3
        ),
        expectFunctionMatrix("functionLtLeft", functionMatrixWhere(user) { f.length(it.name) < 4 }, SqlBinaryOperator.LessThan, 4),
        expectFunctionMatrix(
            "functionLeLeft",
            functionMatrixWhere(user) { f.length(it.name) <= 5 },
            SqlBinaryOperator.LessThanEqual,
            5
        ),
        expectFunctionMatrix("functionGtRight", functionMatrixWhere(user) { 2 > f.length(it.name) }, SqlBinaryOperator.LessThan, 2),
        expectFunctionMatrix(
            "functionGeRight",
            functionMatrixWhere(user) { 3 >= f.length(it.name) },
            SqlBinaryOperator.LessThanEqual,
            3
        ),
        expectFunctionMatrix("functionLtRight", functionMatrixWhere(user) { 4 < f.length(it.name) }, SqlBinaryOperator.GreaterThan, 4),
        expectFunctionMatrix(
            "functionLeRight",
            functionMatrixWhere(user) { 5 <= f.length(it.name) },
            SqlBinaryOperator.GreaterThanEqual,
            5
        ),
        expectOperatorMatrix("operatorGtLeft", functionMatrixWhere(user) { it.score + 1 > 7 }, SqlBinaryOperator.GreaterThan, 7),
        expectOperatorMatrix("operatorLtRight", functionMatrixWhere(user) { 7 < it.score + 1 }, SqlBinaryOperator.GreaterThan, 7),
    )

    return failures.firstOrNull() ?: "OK"
}
