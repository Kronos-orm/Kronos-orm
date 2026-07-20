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

// Verifies generated JOIN Selected types remain selectable and union-compatible.

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_join_composition_user")
data class JoinCompositionUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_join_composition_order")
data class JoinCompositionOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

private fun joinedCompositionQuery() =
    JoinCompositionUser().join(JoinCompositionOrder()) { user, order ->
        leftJoin { user.id == order.userId }
            .select { [user.id, order.status.alias("orderStatus")] }
    }

fun box(): String {
    val derived = joinedCompositionQuery()
        .select { [it.id, it.orderStatus] }
        .where { it.orderStatus == 7 }
    val unionDerived = union(joinedCompositionQuery(), joinedCompositionQuery())
        .select { [it.id, it.orderStatus] }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val derivedRow = derived.first()
        val unionRow = unionDerived.first()
        val derivedId: Int? = derivedRow.id
        val derivedStatus: Int? = derivedRow.orderStatus
        val unionId: Int? = unionRow.id
        val unionStatus: Int? = unionRow.orderStatus
        return "Fail: generated values unexpectedly evaluated as $derivedId/$derivedStatus/$unionId/$unionStatus"
    }

    val derivedStatement = derived.toSqlQuery() as? SqlQuery.Select
        ?: return "Fail: derived query was ${derived.toSqlQuery()}"
    val unionStatement = unionDerived.toSqlQuery() as? SqlQuery.Select
        ?: return "Fail: union-derived query was ${unionDerived.toSqlQuery()}"
    val derivedSource = derivedStatement.from.singleOrNull() as? SqlTable.Subquery
    val unionSource = unionStatement.from.singleOrNull() as? SqlTable.Subquery
    val derivedNames = derivedStatement.outputNames()
    val unionNames = unionStatement.outputNames()

    val failures = listOfNotNull(
        expect(derivedSource?.query is SqlQuery.Select) { "derived source was ${derivedStatement.from}" },
        expect(unionSource?.query is SqlQuery.Set) { "union source was ${unionStatement.from}" },
        expect(derivedNames == listOf("id", "orderStatus")) { "derived names were $derivedNames" },
        expect(unionNames == listOf("id", "orderStatus")) { "union names were $unionNames" },
        expect(derivedStatement.where is SqlExpr.Binary) { "derived where was ${derivedStatement.where}" },
    )
    return failures.firstOrNull() ?: "OK"
}

private fun SqlQuery.Select.outputNames(): List<String> = select.mapNotNull { item ->
    val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
    expression.metadata?.outputName
        ?: expression.alias
        ?: (expression.expr as? SqlExpr.Column)?.columnName
}

private inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
