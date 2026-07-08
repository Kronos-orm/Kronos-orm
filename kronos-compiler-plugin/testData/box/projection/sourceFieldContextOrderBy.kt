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

// Verifies orderBy Context can access Source fields that were not selected.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_projection_source_order")
data class ProjectionSourceOrderRow(
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {}

    val statement = ProjectionSourceOrderRow()
        .select { it.id }
        .orderBy { it.name.asc() }
        .toSqlQuery()

    val actual = statement.actualOrderByStatement()
    val orderBy = actual.orderBy.singleOrNull()
        ?: return "Fail: orderBy was ${actual.orderBy}"
    val expression = orderBy.expr as? SqlExpr.Column
        ?: return "Fail: orderBy expression was ${orderBy.expr}"

    return when {
        expression.columnName != "name" -> "Fail: orderBy column was ${expression.columnName}"
        orderBy.ordering != SqlOrdering.Asc -> "Fail: orderBy direction was ${orderBy.ordering}"
        else -> "OK"
    }
}

fun SqlQuery.actualOrderByStatement(): SqlQuery.Select {
    val select = this as? SqlQuery.Select ?: error("statement was ${this::class.qualifiedName}")
    return ((select.from.singleOrNull() as? SqlTable.Subquery)?.query as? SqlQuery.Select) ?: select
}
