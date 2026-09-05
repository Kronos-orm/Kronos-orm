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

// Verifies operator function expressions work inside comparisons and string match values.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_operator")
data class OperatorConditionUser(
    var id: Int? = null,
    var score: Int = 0,
    var username: String? = null,
) : KPojo

fun operatorWhere(user: OperatorConditionUser, block: ToFilter<OperatorConditionUser, Boolean?>): SqlExpr? {
    var result: SqlExpr? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = sqlExpr
    }
    return result
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = OperatorConditionUser(score = 42, username = "ada")
    val comparison = operatorWhere(user) { it.score + 10 > it.score - 10 } as? SqlExpr.Binary
    val left = comparison?.left as? SqlExpr.Binary
    val right = comparison?.right as? SqlExpr.Binary

    val like = operatorWhere(user) { it.username like ("%" + it.username + "%") } as? SqlExpr.Like
    val likeColumn = like?.expr as? SqlExpr.Column
    val likeExpr = like?.pattern as? SqlExpr.Function
    val nestedLikeExpr = likeExpr?.args?.getOrNull(0) as? SqlExpr.Function
    val nestedField = nestedLikeExpr?.args?.getOrNull(1) as? SqlExpr.Column

    val failures = listOfNotNull(
        expect(comparison?.operator == SqlBinaryOperator.GreaterThan) { "comparison operator was ${comparison?.operator}" },
        expect(left?.operator == SqlBinaryOperator.Plus) { "left operator was ${left?.operator}" },
        expect(right?.operator == SqlBinaryOperator.Minus) { "right operator was ${right?.operator}" },
        expect(likeColumn?.columnName == "username") { "like field was ${likeColumn?.columnName}" },
        expect(like?.withNot == false) { "like not was ${like?.withNot}" },
        expect(likeExpr?.name?.last == "CONCAT") { "like SQL function was ${likeExpr?.name?.last}" },
        expect(nestedLikeExpr?.name?.last == "CONCAT") { "nested SQL function was ${nestedLikeExpr?.name?.last}" },
        expect(nestedField?.columnName == "username") {
            "nested like field was ${nestedField?.columnName}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
