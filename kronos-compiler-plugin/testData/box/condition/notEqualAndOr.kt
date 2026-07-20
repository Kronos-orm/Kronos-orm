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

// Verifies not-equal comparisons and OR conditions are lowered into SqlExpr trees.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_not_equal_or")
data class NotEqualOrUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo

data class CapturedNotEqualOr(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun notEqualOrWhere(user: NotEqualOrUser, block: ToFilter<NotEqualOrUser, Boolean?>): CapturedNotEqualOr {
    var result: CapturedNotEqualOr? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = CapturedNotEqualOr(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedNotEqualOr(null, emptyMap())
}

fun parameterValue(captured: CapturedNotEqualOr, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return captured.parameters[name]
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = NotEqualOrUser(id = 1, name = "Ada", age = 36)
    val notEqual = notEqualOrWhere(user) { it.name != "Grace" }
    val or = notEqualOrWhere(user) { it.name == "Ada" || it.age < 18 }
    val negatedOr = notEqualOrWhere(user) { !(it.name == "Ada" || it.age < 18) }
    val notEqualExpr = notEqual.expr as? SqlExpr.Binary
    val orExpr = or.expr as? SqlExpr.Binary
    val negatedOrExpr = negatedOr.expr as? SqlExpr.Binary

    return when {
        notEqualExpr?.operator != SqlBinaryOperator.NotEqual -> "Fail: notEqual operator was ${notEqualExpr?.operator}"
        parameterValue(notEqual, notEqualExpr.right) != "Grace" -> "Fail: notEqual value was ${parameterValue(notEqual, notEqualExpr.right)}"
        orExpr?.operator != SqlBinaryOperator.Or -> "Fail: or operator was ${orExpr?.operator}"
        (orExpr.left as? SqlExpr.Binary)?.operator != SqlBinaryOperator.Equal -> "Fail: or left was ${orExpr.left}"
        (orExpr.right as? SqlExpr.Binary)?.operator != SqlBinaryOperator.LessThan -> "Fail: or right was ${orExpr.right}"
        negatedOrExpr?.operator != SqlBinaryOperator.And -> "Fail: negatedOr operator was ${negatedOrExpr?.operator}"
        (negatedOrExpr.left as? SqlExpr.Binary)?.operator != SqlBinaryOperator.NotEqual -> "Fail: negatedOr left was ${negatedOrExpr.left}"
        (negatedOrExpr.right as? SqlExpr.Binary)?.operator != SqlBinaryOperator.GreaterThanEqual -> "Fail: negatedOr right was ${negatedOrExpr.right}"
        else -> "OK"
    }
}
