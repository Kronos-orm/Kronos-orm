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

// Integration matrix verifying filter lowering for negation, OR, gates, and selectable subqueries.

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.filter
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_filter_condition_user")
data class FilterConditionUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_filter_condition_order")
data class FilterConditionOrder(
    var id: Int? = null,
    var userId: Int? = null,
) : KPojo

fun box(): String {
    val projected = FilterConditionUser()
        .select { [it.id.alias("uid"), it.name.alias("displayName")] }

    val negatedOr = projected
        .filter { !(it.uid == 1 || it.displayName == "blocked") }
        .toSqlQuery() as SqlQuery.Select
    val takeIfDropped = projected
        .filter { (it.uid > 0).takeIf(false) }
        .toSqlQuery() as SqlQuery.Select
    val takeUnlessKept = projected
        .filter { (it.uid > 0).takeUnless(false) }
        .toSqlQuery() as SqlQuery.Select
    val subquery = projected
        .filter { it.uid in FilterConditionOrder().select { order -> order.userId } }
        .toSqlQuery() as SqlQuery.Select

    val negatedRoot = negatedOr.where as? SqlExpr.Binary
    val negatedLeft = negatedRoot?.left as? SqlExpr.Binary
    val negatedRight = negatedRoot?.right as? SqlExpr.Binary
    val takeUnless = takeUnlessKept.where as? SqlExpr.Binary
    val inPredicate = subquery.where as? SqlExpr.In
    val inSubquery = inPredicate?.`in` as? SqlInRightOperand.Subquery
    val inQuery = inSubquery?.query as? SqlQuery.Select

    val failures = listOfNotNull(
        expectFilterCondition(negatedRoot?.operator == SqlBinaryOperator.And) {
            "negated OR root was ${negatedRoot?.operator}"
        },
        expectFilterCondition(negatedLeft?.operator == SqlBinaryOperator.NotEqual) {
            "negated OR left was ${negatedLeft?.operator}"
        },
        expectFilterCondition(negatedLeft.column("uid")) { "negated OR left was $negatedLeft" },
        expectFilterCondition(negatedRight?.operator == SqlBinaryOperator.NotEqual) {
            "negated OR right was ${negatedRight?.operator}"
        },
        expectFilterCondition(negatedRight.column("displayName")) { "negated OR right was $negatedRight" },
        expectFilterCondition(takeIfDropped.where == null) {
            "takeIf(false) filter was ${takeIfDropped.where}"
        },
        expectFilterCondition(takeIfDropped.from.singleOrNull() is SqlTable.Subquery) {
            "takeIf(false) source was ${takeIfDropped.from}"
        },
        expectFilterCondition(takeUnless?.operator == SqlBinaryOperator.GreaterThan) {
            "takeUnless(false) operator was ${takeUnless?.operator}"
        },
        expectFilterCondition(takeUnless.column("uid")) { "takeUnless(false) filter was $takeUnless" },
        expectFilterCondition((inPredicate?.expr as? SqlExpr.Column)?.tableName == "q") {
            "subquery filter table was ${inPredicate?.expr}"
        },
        expectFilterCondition((inPredicate?.expr as? SqlExpr.Column)?.columnName == "uid") {
            "subquery filter column was ${inPredicate?.expr}"
        },
        expectFilterCondition(inPredicate?.withNot == false) {
            "subquery filter NOT flag was ${inPredicate?.withNot}"
        },
        expectFilterCondition(inQuery?.outputNames() == listOf("userId")) {
            "subquery filter outputs were ${inQuery?.outputNames()}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

private fun SqlExpr.Binary?.column(name: String): Boolean {
    val column = this?.left as? SqlExpr.Column
    return column?.tableName == "q" && column.columnName == name
}

private fun SqlQuery.Select.outputNames(): List<String> =
    select.mapNotNull { item ->
        val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
        expression.metadata?.outputName
            ?: expression.alias
            ?: (expression.expr as? SqlExpr.Column)?.columnName
    }

private inline fun expectFilterCondition(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
