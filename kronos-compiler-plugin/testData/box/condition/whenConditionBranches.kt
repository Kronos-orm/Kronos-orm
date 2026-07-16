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

// Verifies ordinary Kotlin if/when selects one SQL-expression branch at runtime.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_when_branch")
data class WhenConditionUser(
    var id: Int? = null,
    var age: Int? = null,
    var status: Int? = null,
) : KPojo

data class CapturedWhenCondition(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun whenConditionWhere(user: WhenConditionUser, block: ToFilter<WhenConditionUser, Boolean?>): CapturedWhenCondition {
    var result: CapturedWhenCondition? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedWhenCondition(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedWhenCondition(null, emptyMap())
}

fun whenConditionParameter(actual: CapturedWhenCondition, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

data class WhenConditionLeaf(
    val column: String?,
    val operator: SqlBinaryOperator?,
    val value: Any?
)

fun whenConditionLeaves(actual: CapturedWhenCondition): List<WhenConditionLeaf> =
    flattenWhenConditionAnd(actual.expr).map { expr ->
        WhenConditionLeaf(
            (expr.left as? SqlExpr.Column)?.columnName,
            expr.operator,
            whenConditionParameter(actual, expr.right)
        )
    }

fun flattenWhenConditionAnd(expr: SqlExpr?): List<SqlExpr.Binary> {
    val binary = expr as? SqlExpr.Binary ?: return emptyList()
    return if (binary.operator == SqlBinaryOperator.And) {
        flattenWhenConditionAnd(binary.left) + flattenWhenConditionAnd(binary.right)
    } else {
        listOf(binary)
    }
}

fun expectWhenCondition(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = WhenConditionUser(id = 1, age = 36, status = 2)
    val runtimeIf = whenConditionWhere(user) { if (user.status == 2) it.age > 18 else it.id == 1 }
    val nestedWhen = whenConditionWhere(user) {
        when {
            user.status == 1 -> it.id == 1
            user.status == 2 -> it.age > 18
            else -> it.status == 0
        }
    }
    val literalOnly = whenConditionWhere(user) { true }
    val singleWhenChild = whenConditionWhere(user) { if (user.id == 1) true else false }
    val negatedRuntimeIf = whenConditionWhere(user) { !(if (user.status == 2) it.age > 18 else it.id == 1) }
    val runtimeLeaves = whenConditionLeaves(runtimeIf)
    val nestedLeaves = whenConditionLeaves(nestedWhen)
    val singleWhenLeaves = whenConditionLeaves(singleWhenChild)
    val negatedRuntimeLeaves = whenConditionLeaves(negatedRuntimeIf)

    val failures = listOfNotNull(
        expectWhenCondition(
            runtimeLeaves == listOf(WhenConditionLeaf("age", SqlBinaryOperator.GreaterThan, 18))
        ) {
            "runtime if leaves were $runtimeLeaves"
        },
        expectWhenCondition(
            nestedLeaves == listOf(WhenConditionLeaf("age", SqlBinaryOperator.GreaterThan, 18))
        ) {
            "nested when leaves were $nestedLeaves"
        },
        expectWhenCondition(literalOnly.expr == null) {
            "literal-only expression was ${literalOnly.expr}"
        },
        expectWhenCondition(literalOnly.parameters.isEmpty()) {
            "literal-only parameters were ${literalOnly.parameters}"
        },
        expectWhenCondition(
            singleWhenLeaves.isEmpty()
        ) {
            "single when leaves were $singleWhenLeaves"
        },
        expectWhenCondition(
            negatedRuntimeLeaves == listOf(WhenConditionLeaf("age", SqlBinaryOperator.LessThanEqual, 18))
        ) {
            "negated runtime if leaves were $negatedRuntimeLeaves"
        },
    )

    return failures.firstOrNull() ?: "OK"
}
