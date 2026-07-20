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

// Verifies reversed equality and function/field RHS values lower to SQL expressions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_reversed_equality")
data class ReversedEqualityUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

data class CapturedReversedEquality(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun reversedEqualityWhere(
    user: ReversedEqualityUser,
    block: ToFilter<ReversedEqualityUser, Boolean?>,
): CapturedReversedEquality {
    var result: CapturedReversedEquality? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = CapturedReversedEquality(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedReversedEquality(null, emptyMap())
}

fun reversedEqualityParameter(actual: CapturedReversedEquality, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = ReversedEqualityUser(id = 3, name = "Ada")
    val reversedValue = reversedEqualityWhere(user) { "Ada" == it.name }
    val reversedFunction = reversedEqualityWhere(user) { 3 == f.length(it.name) }
    val fieldToFunction = reversedEqualityWhere(user) { it.id == f.length(it.name) }

    val valueBinary = reversedValue.expr as? SqlExpr.Binary
    val functionBinary = reversedFunction.expr as? SqlExpr.Binary
    val functionLeft = functionBinary?.left as? SqlExpr.Function
    val functionField = functionLeft?.args?.singleOrNull() as? SqlExpr.Column
    val fieldFunctionBinary = fieldToFunction.expr as? SqlExpr.Binary
    val fieldFunctionLeft = fieldFunctionBinary?.left as? SqlExpr.Function
    val fieldFunctionArg = fieldFunctionLeft?.args?.singleOrNull() as? SqlExpr.Column

    val failures = listOfNotNull(
        expectReversedEquality(valueBinary?.operator == SqlBinaryOperator.Equal) { "value operator was ${valueBinary?.operator}" },
        expectReversedEquality((valueBinary?.left as? SqlExpr.Column)?.columnName == "name") { "value left was ${valueBinary?.left}" },
        expectReversedEquality(reversedEqualityParameter(reversedValue, valueBinary?.right) == "Ada") {
            "value parameter was ${reversedEqualityParameter(reversedValue, valueBinary?.right)}"
        },
        expectReversedEquality(functionBinary?.operator == SqlBinaryOperator.Equal) {
            "function operator was ${functionBinary?.operator}"
        },
        expectReversedEquality(functionLeft?.name?.last == "LENGTH") { "function left was $functionLeft" },
        expectReversedEquality(functionField?.columnName == "name") { "function field was $functionField" },
        expectReversedEquality(reversedEqualityParameter(reversedFunction, functionBinary?.right) == 3) {
            "function parameter was ${reversedEqualityParameter(reversedFunction, functionBinary?.right)}"
        },
        expectReversedEquality(fieldFunctionLeft?.name?.last == "LENGTH") {
            "field/function left was ${fieldFunctionBinary?.left}"
        },
        expectReversedEquality(fieldFunctionArg?.columnName == "name") { "field/function arg was $fieldFunctionArg" },
        expectReversedEquality((fieldFunctionBinary?.right as? SqlExpr.Column)?.columnName == "id") {
            "field/function right was ${fieldFunctionBinary?.right}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expectReversedEquality(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
