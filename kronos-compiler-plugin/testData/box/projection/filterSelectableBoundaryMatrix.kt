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

// Integration matrix verifying filter receivers and boundaries for JOIN, UNION, and already-derived results.

import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.filter
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_filter_boundary_user")
data class FilterBoundaryUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_filter_boundary_order")
data class FilterBoundaryOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    val projected = FilterBoundaryUser()
        .select { [it.id.alias("userId"), f.length(it.name).alias("nameLength")] }
    val joined = FilterBoundaryUser().join(FilterBoundaryOrder()) { user, order ->
        innerJoin { user.id == order.userId }
            .select { [user.id.alias("joinedUserId"), order.status.alias("orderStatus")] }
    }
    val alreadyDerived = projected.select { [it.userId, it.nameLength] }

    val joinedStatement = joined
        .filter { it.orderStatus == 7 }
        .toSqlQuery() as SqlQuery.Select
    val unionStatement = union(projected, projected)
        .filter { it.nameLength > 2 }
        .toSqlQuery() as SqlQuery.Select
    val derivedStatement = alreadyDerived
        .filter { it.userId > 0 }
        .toSqlQuery() as SqlQuery.Select

    val joinedSource = joinedStatement.from.singleOrNull() as? SqlTable.Subquery
    val joinedInner = joinedSource?.query as? SqlQuery.Select
    val unionSource = unionStatement.from.singleOrNull() as? SqlTable.Subquery
    val derivedSource = derivedStatement.from.singleOrNull() as? SqlTable.Subquery
    val derivedInner = derivedSource?.query as? SqlQuery.Select
    val joinedWhere = joinedStatement.where as? SqlExpr.Binary
    val unionWhere = unionStatement.where as? SqlExpr.Binary
    val derivedWhere = derivedStatement.where as? SqlExpr.Binary

    val failures = listOfNotNull(
        expectFilterBoundary(joinedInner?.from?.singleOrNull() is SqlTable.Join) {
            "filtered JOIN inner source was ${joinedInner?.from}"
        },
        expectFilterBoundary(unionSource?.query is SqlQuery.Set) {
            "filtered UNION source query was ${unionSource?.query}"
        },
        expectFilterBoundary(derivedInner?.from?.singleOrNull() is SqlTable.Subquery) {
            "filtered already-derived inner source was ${derivedInner?.from}"
        },
        expectFilterBoundary(joinedWhere?.operator == SqlBinaryOperator.Equal) {
            "JOIN filter operator was ${joinedWhere?.operator}"
        },
        expectFilterBoundary(joinedWhere.column("orderStatus")) { "JOIN filter was $joinedWhere" },
        expectFilterBoundary(unionWhere?.operator == SqlBinaryOperator.GreaterThan) {
            "UNION filter operator was ${unionWhere?.operator}"
        },
        expectFilterBoundary(unionWhere.column("nameLength")) { "UNION filter was $unionWhere" },
        expectFilterBoundary(derivedWhere?.operator == SqlBinaryOperator.GreaterThan) {
            "already-derived filter operator was ${derivedWhere?.operator}"
        },
        expectFilterBoundary(derivedWhere.column("userId")) { "already-derived filter was $derivedWhere" },
    )

    return failures.firstOrNull() ?: "OK"
}

private fun SqlExpr.Binary?.column(name: String): Boolean {
    val column = this?.left as? SqlExpr.Column
    return column?.tableName == "q" && column.columnName == name
}

private inline fun expectFilterBoundary(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
