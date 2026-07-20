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

// Verifies collection membership and boolean composition lower to syntax SqlExpr conditions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_group_user")
data class ConditionGroupUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo

data class CapturedGroupCondition(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun groupWhere(user: ConditionGroupUser, block: ToFilter<ConditionGroupUser, Boolean?>): CapturedGroupCondition {
    var result: CapturedGroupCondition? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = CapturedGroupCondition(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedGroupCondition(null, emptyMap())
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = ConditionGroupUser()
    val ids = [1, 2, 3]
    val capturedInCondition = groupWhere(user) { it.id in ids }
    val inCondition = capturedInCondition.expr as? SqlExpr.In
    val inColumn = inCondition?.expr as? SqlExpr.Column
    val inValues = (inCondition?.`in` as? SqlInRightOperand.Values)?.items
    if (inColumn?.columnName != "id") return "Fail: in field was ${inColumn?.columnName}"
    if (inValues != listOf(SqlExpr.Parameter(SqlParameter.Named("idList"), expandAsList = true))) {
        return "Fail: in values were $inValues"
    }
    if (capturedInCondition.parameters != mapOf("idList" to listOf(1, 2, 3))) {
        return "Fail: in parameters were ${capturedInCondition.parameters}"
    }

    val andCondition = groupWhere(user) { (it.name == "Ada") && (it.age > 18) }.expr as? SqlExpr.Binary
    if (andCondition?.operator != SqlBinaryOperator.And) return "Fail: group operator was ${andCondition?.operator}"
    val firstColumn = (andCondition.left as? SqlExpr.Binary)?.left as? SqlExpr.Column
    val secondColumn = (andCondition.right as? SqlExpr.Binary)?.left as? SqlExpr.Column
    if (firstColumn?.columnName != "name") return "Fail: first child was ${firstColumn?.columnName}"
    if (secondColumn?.columnName != "age") return "Fail: second child was ${secondColumn?.columnName}"

    return "OK"
}
