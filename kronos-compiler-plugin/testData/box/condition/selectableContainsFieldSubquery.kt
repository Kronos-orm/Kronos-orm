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

// Verifies `query.contains(field)` lowers to syntax IN with a subquery right operand.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.types.ToFilter

@Table(name = "tb_contains_subquery_user")
data class ContainsSubqueryUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table(name = "tb_contains_subquery_order")
data class ContainsSubqueryOrder(
    var id: Int? = null,
    var userId: Int? = null,
) : KPojo

fun selectableContainsWhere(
    user: ContainsSubqueryUser,
    block: ToFilter<ContainsSubqueryUser, Boolean?>,
): SqlExpr? {
    var result: SqlExpr? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = sqlExpr
    }
    return result
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val query = ContainsSubqueryOrder().select { order -> order.userId }
    val condition = selectableContainsWhere(ContainsSubqueryUser(id = 7)) {
        query.contains(it.id)
    } as? SqlExpr.In
    val column = condition?.expr as? SqlExpr.Column
    val subquery = condition?.`in` as? SqlInRightOperand.Subquery

    val failures = listOfNotNull(
        expectSelectableContains(column?.columnName == "id") { "field was ${column?.columnName}" },
        expectSelectableContains(condition?.withNot == false) { "not flag was ${condition?.withNot}" },
        expectSelectableContains(subquery?.query is SqlQuery.Select) {
            "query was ${subquery?.query?.let { it::class.qualifiedName }}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expectSelectableContains(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
