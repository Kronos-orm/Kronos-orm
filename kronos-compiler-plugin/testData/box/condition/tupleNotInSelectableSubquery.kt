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

// Verifies `[field1, field2] !in query` lowers to syntax row-value NOT IN subquery.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.types.ToFilter

@Table(name = "tb_tuple_not_user")
data class TupleNotUser(
    var id: Int? = null,
    var status: Int? = null,
) : KPojo

@Table(name = "tb_tuple_not_order")
data class TupleNotOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun tupleNotWhere(user: TupleNotUser, block: ToFilter<TupleNotUser, Boolean?>): SqlExpr? {
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

    val condition = tupleNotWhere(TupleNotUser(id = 7, status = 1)) {
        [it.id, it.status] !in TupleNotOrder().select { order -> [order.userId, order.status] }
    } as? SqlExpr.In
    val tuple = condition?.expr as? SqlExpr.Tuple
    val tupleColumns = tuple?.items?.mapNotNull { (it as? SqlExpr.Column)?.columnName }.orEmpty()
    val subquery = condition?.`in` as? SqlInRightOperand.Subquery

    val failures = listOfNotNull(
        expect(tupleColumns == listOf("id", "status")) { "tuple fields were $tupleColumns" },
        expect(condition?.withNot == true) { "not flag was ${condition?.withNot}" },
        expect(subquery?.query is SqlQuery.Select) { "query was ${subquery?.query?.let { it::class.qualifiedName }}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
