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

// Verifies boundary condition forms that still lower through the SQL expression transformer.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_boundary")
data class ConditionBoundaryUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo

data class CapturedConditionBoundary(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun conditionBoundaryWhere(
    user: ConditionBoundaryUser,
    block: ToFilter<ConditionBoundaryUser, Boolean?>,
): CapturedConditionBoundary {
    var result: CapturedConditionBoundary? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedConditionBoundary(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedConditionBoundary(null, emptyMap())
}

fun conditionBoundaryLeaves(expr: SqlExpr?): List<SqlExpr.Binary> {
    val binary = expr as? SqlExpr.Binary ?: return emptyList()
    return if (binary.operator == SqlBinaryOperator.And) {
        conditionBoundaryLeaves(binary.left) + conditionBoundaryLeaves(binary.right)
    } else {
        listOf(binary)
    }
}

fun conditionBoundaryParameter(actual: CapturedConditionBoundary, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun expectBoundary(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = ConditionBoundaryUser(id = 1, name = "Ada", age = 36)
    val other = ConditionBoundaryUser(id = 2, name = "Grace", age = 40)
    val fieldToField = conditionBoundaryWhere(user) { it.age == it.id }
    val kpojoRight = conditionBoundaryWhere(user) { other == it }
    val runtimeTrue = conditionBoundaryWhere(user) { 1 == 1 }

    val fieldExpr = fieldToField.expr as? SqlExpr.Binary
    val kpojoLeaves = conditionBoundaryLeaves(kpojoRight.expr)
    val runtimeExpr = runtimeTrue.expr as? SqlExpr.BooleanLiteral

    val failures = listOfNotNull(
        expectBoundary(fieldExpr?.operator == SqlBinaryOperator.Equal) { "field-to-field operator was ${fieldExpr?.operator}" },
        expectBoundary(((fieldExpr?.left as? SqlExpr.Column)?.columnName) == "age") { "left field was ${fieldExpr?.left}" },
        expectBoundary(((fieldExpr?.right as? SqlExpr.Column)?.columnName) == "id") { "right field was ${fieldExpr?.right}" },
        expectBoundary(kpojoLeaves.map { (it.left as? SqlExpr.Column)?.columnName }.toSet() == setOf("id", "name", "age")) {
            "kpojo fields were ${kpojoLeaves.map { it.left }}"
        },
        expectBoundary(kpojoLeaves.all { it.operator == SqlBinaryOperator.Equal }) {
            "kpojo operators were ${kpojoLeaves.map { it.operator }}"
        },
        expectBoundary(
            kpojoLeaves.associate { ((it.left as? SqlExpr.Column)?.columnName ?: "") to conditionBoundaryParameter(kpojoRight, it.right) } ==
                mapOf("id" to 1, "name" to "Ada", "age" to 36)
        ) { "kpojo values were ${kpojoLeaves.map { conditionBoundaryParameter(kpojoRight, it.right) }}" },
        expectBoundary(runtimeExpr?.boolean == true) {
            "runtime expression was ${runtimeTrue.expr}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}
