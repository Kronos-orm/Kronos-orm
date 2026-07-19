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

// Verifies JOIN and UNION Selected values retain their scalar and predicate subquery shapes.

import com.kotlinorm.annotations.Table
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem

@Table("tb_projection_layer_subquery_user")
data class ProjectionLayerSubqueryUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_projection_layer_subquery_order")
data class ProjectionLayerSubqueryOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

private fun joinedAmounts() = ProjectionLayerSubqueryUser()
    .join(ProjectionLayerSubqueryOrder()) { user, order ->
        innerJoin { user.id == order.userId }
            .select { order.amount }
    }

private fun unionedUserIds() = union(
    ProjectionLayerSubqueryUser().select { it.id },
    ProjectionLayerSubqueryUser().select { it.id },
)

fun box(): String {
    val joined = joinedAmounts()
    val unioned = unionedUserIds()
    val scalar = ProjectionLayerSubqueryUser().select {
        [
            it.id,
            joined.limit(1).alias("joinedAmount"),
            unioned.limit(1).alias("unionUserId"),
        ]
    }
    val predicate = ProjectionLayerSubqueryUser()
        .select()
        .where {
            (it.id in joined) && (it.id in unioned)
        }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val scalarRow = scalar.first()
        val joinedAmount: Int? = scalarRow.joinedAmount
        val unionUserId: Int? = scalarRow.unionUserId
        val joinedRow = joined.first()
        val unionRow = unioned.first()
        return "Fail: generated values were $joinedAmount/$unionUserId/${joinedRow.amount}/${unionRow.id}"
    }

    val scalarStatement = scalar.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val scalarItems = scalarStatement.select.filterIsInstance<SqlSelectItem.Expr>()
    val scalarAliases = scalarItems.mapNotNull { it.metadata?.outputName ?: it.alias }
    val scalarSubqueries = scalarItems.mapNotNull { (it.expr as? SqlExpr.Subquery)?.query }
    val predicateStatement = predicate.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val predicateSubqueries = predicateStatement.where.collectInSubqueries()

    val failures = listOfNotNull(
        expect(scalarAliases == listOf("id", "joinedAmount", "unionUserId")) {
            "scalar aliases were $scalarAliases"
        },
        expect(scalarSubqueries.size == 2) { "scalar subqueries were $scalarSubqueries" },
        expect(scalarSubqueries[0].outputNames() == listOf("amount")) {
            "JOIN scalar outputs were ${scalarSubqueries[0].outputNames()}"
        },
        expect(scalarSubqueries[1].outputNames() == listOf("id")) {
            "UNION scalar outputs were ${scalarSubqueries[1].outputNames()}"
        },
        expect(predicateSubqueries.size == 2) { "predicate subqueries were $predicateSubqueries" },
        expect(predicateSubqueries.map { it.outputNames() } == listOf(listOf("amount"), listOf("id"))) {
            "predicate outputs were ${predicateSubqueries.map { it.outputNames() }}"
        },
    )
    return failures.firstOrNull() ?: "OK"
}

private fun SqlExpr?.collectInSubqueries(): List<SqlQuery> = when (this) {
    is SqlExpr.Binary -> left.collectInSubqueries() + right.collectInSubqueries()
    is SqlExpr.In -> listOfNotNull((`in` as? SqlInRightOperand.Subquery)?.query)
    else -> emptyList()
}

private fun SqlQuery.outputNames(): List<String> = when (this) {
    is SqlQuery.Select -> select.mapNotNull { item ->
        val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
        expression.metadata?.outputName
            ?: expression.alias
            ?: (expression.expr as? SqlExpr.Column)?.columnName
    }
    is SqlQuery.Set -> left.outputNames()
    is SqlQuery.With -> query.outputNames()
    is SqlQuery.Values -> emptyList()
}

private inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
