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

// Verifies negation and KPojo minus forms are lowered into syntax SqlExpr trees.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_minus")
data class MinusConditionUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
    @ColumnType(KColumnType.TINYINT)
    var status: Int? = null,
) : KPojo

data class CapturedMinusCondition(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun minusWhere(user: MinusConditionUser, block: ToFilter<MinusConditionUser, Boolean?>): CapturedMinusCondition {
    var result: CapturedMinusCondition? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = CapturedMinusCondition(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedMinusCondition(null, emptyMap())
}

fun collectAndLeaves(expr: SqlExpr?): List<SqlExpr.Binary> {
    val binary = expr as? SqlExpr.Binary ?: return emptyList()
    return if (binary.operator == SqlBinaryOperator.And) {
        collectAndLeaves(binary.left) + collectAndLeaves(binary.right)
    } else {
        listOf(binary)
    }
}

fun minusParameterValue(actual: CapturedMinusCondition, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = MinusConditionUser(id = 1, name = "Ada", age = 36, status = 1)

    val deMorgan = minusWhere(user) { !(it.name == "Ada" && it.age > 18) }
    val deMorganExpr = deMorgan.expr as? SqlExpr.Binary
    val deMorganLeft = deMorganExpr?.left as? SqlExpr.Binary
    val deMorganRight = deMorganExpr?.right as? SqlExpr.Binary
    if (deMorganExpr?.operator != SqlBinaryOperator.Or) return "Fail: De Morgan operator was ${deMorganExpr?.operator}"
    if (deMorganLeft?.operator != SqlBinaryOperator.NotEqual) return "Fail: De Morgan left was ${deMorganLeft?.operator}"
    if (deMorganRight?.operator != SqlBinaryOperator.LessThanEqual) return "Fail: De Morgan right was ${deMorganRight?.operator}"

    val excluded = minusWhere(user) { (it - it.status).eq }
    val excludedLeaves = collectAndLeaves(excluded.expr)
    val names = excludedLeaves.mapNotNull { (it.left as? SqlExpr.Column)?.columnName }.toSet()
    val values = excludedLeaves.associate { leaf ->
        ((leaf.left as? SqlExpr.Column)?.columnName ?: "") to minusParameterValue(excluded, leaf.right)
    }
    if ("status" in names) return "Fail: status should be excluded"
    if (names != setOf("id", "name", "age")) return "Fail: fields were $names"
    if (values != mapOf("id" to 1, "name" to "Ada", "age" to 36)) return "Fail: values were $values"

    val takeIfFalse = minusWhere(user) { (it.id == 1).takeIf(false) }
    if (takeIfFalse.expr != null) return "Fail: takeIf(false) should be null, was ${takeIfFalse.expr}"

    return "OK"
}
