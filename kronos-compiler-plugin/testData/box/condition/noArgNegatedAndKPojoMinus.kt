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

// Verifies no-argument negation and KPojo-minus equality keep precise fields and values.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter
import com.kotlinorm.utils.TransformerSafeValue
import kotlin.reflect.typeOf

@Table(name = "tb_condition_no_arg_negated")
data class NoArgNegatedUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
    var status: String? = null,
) : KPojo

data class CapturedNoArgNegated(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun noArgNegatedWhere(user: NoArgNegatedUser, block: ToFilter<NoArgNegatedUser, Boolean?>): CapturedNoArgNegated {
    var result: CapturedNoArgNegated? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = CapturedNoArgNegated(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedNoArgNegated(null, emptyMap())
}

fun noArgNegatedLeaves(expr: SqlExpr?): List<SqlExpr.Binary> {
    val binary = expr as? SqlExpr.Binary ?: return emptyList()
    return if (binary.operator == SqlBinaryOperator.And) {
        noArgNegatedLeaves(binary.left) + noArgNegatedLeaves(binary.right)
    } else {
        listOf(binary)
    }
}

fun noArgNegatedParameter(actual: CapturedNoArgNegated, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun expectNoArg(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = NoArgNegatedUser(id = 1, name = "Ada", age = 36, status = "ACTIVE")
    val neq = noArgNegatedWhere(user) { it.age.neq }
    val neqExpr = neq.expr as? SqlExpr.Binary
    val negatedGt = noArgNegatedWhere(user) { !(it.age.gt) }
    val negatedGtExpr = negatedGt.expr as? SqlExpr.Binary
    val minus = noArgNegatedWhere(user) { (it - it.status - it.age).eq }
    val minusLeaves = noArgNegatedLeaves(minus.expr)
    val minusValues = minusLeaves.associate { leaf ->
        ((leaf.left as? SqlExpr.Column)?.columnName ?: "") to noArgNegatedParameter(minus, leaf.right)
    }
    val negatedContains = noArgNegatedWhere(user) { !(it.name.contains) }
    val contains = negatedContains.expr as? SqlExpr.Like
    val containsColumn = contains?.expr as? SqlExpr.Column

    val failures = listOfNotNull(
        expectNoArg((neqExpr?.left as? SqlExpr.Column)?.columnName == "age") { "neq field was ${neqExpr?.left}" },
        expectNoArg(neqExpr?.operator == SqlBinaryOperator.NotEqual) { "neq operator was ${neqExpr?.operator}" },
        expectNoArg(noArgNegatedParameter(neq, neqExpr?.right) == 36) {
            "neq value was ${noArgNegatedParameter(neq, neqExpr?.right)}"
        },
        expectNoArg((negatedGtExpr?.left as? SqlExpr.Column)?.columnName == "age") {
            "negated gt field was ${negatedGtExpr?.left}"
        },
        expectNoArg(negatedGtExpr?.operator == SqlBinaryOperator.LessThanEqual) {
            "negated gt operator was ${negatedGtExpr?.operator}"
        },
        expectNoArg(noArgNegatedParameter(negatedGt, negatedGtExpr?.right) == 36) {
            "negated gt value was ${noArgNegatedParameter(negatedGt, negatedGtExpr?.right)}"
        },
        expectNoArg(minusLeaves.mapNotNull { (it.left as? SqlExpr.Column)?.columnName }.toSet() == setOf("id", "name")) {
            "minus fields were ${minusLeaves.map { it.left }}"
        },
        expectNoArg(minusLeaves.all { it.operator == SqlBinaryOperator.Equal }) {
            "minus operators were ${minusLeaves.map { it.operator }}"
        },
        expectNoArg(minusValues == mapOf("id" to 1, "name" to "Ada")) { "minus values were $minusValues" },
        expectNoArg(containsColumn?.columnName == "name") { "contains field was ${containsColumn?.columnName}" },
        expectNoArg(contains?.withNot == true) { "contains not was ${contains?.withNot}" },
        expectNoArg(noArgNegatedParameter(negatedContains, contains?.pattern) == TransformerSafeValue("%Ada%", typeOf<String>())) {
            "contains value was ${noArgNegatedParameter(negatedContains, contains?.pattern)}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}
