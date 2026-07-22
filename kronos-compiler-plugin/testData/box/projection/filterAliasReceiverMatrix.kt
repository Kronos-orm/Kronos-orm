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

// Integration matrix verifying aggregate, scalar, serialized, and custom projection filter receivers.

import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.filter
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import kotlin.reflect.typeOf

@Table("tb_filter_alias_user")
data class FilterAliasUser(
    var id: Int? = null,
    var username: String? = null,
    @Serialize
    var tags: List<String?>? = null,
) : KPojo

@Table("tb_filter_alias_order")
data class FilterAliasOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

data class FilterCustomProjectionRow(
    var id: Int? = null,
    var displayName: String? = null,
) : KPojo

fun box(): String {
    val aggregateProjection = FilterAliasUser()
        .select { [it.id, f.count(it.id).alias("totalCount")] }
    val scalarProjection = FilterAliasUser()
        .select {
            [
                it.id,
                FilterAliasOrder()
                    .select { order -> order.amount }
                    .limit(1)
                    .alias("latestAmount")
            ]
        }
    val serializedProjection = FilterAliasUser()
        .select { [it.id, it.tags.alias("labels")] }
    val customProjection = FilterAliasUser()
        .select<FilterAliasUser, FilterCustomProjectionRow>(typeOf<FilterCustomProjectionRow>()) {
            [it.id, it.username.alias("displayName")]
        }

    val aggregate = aggregateProjection.filter { it.totalCount > 1 }.toSqlQuery() as SqlQuery.Select
    val scalar = scalarProjection.filter { it.latestAmount > 10 }.toSqlQuery() as SqlQuery.Select
    val serialized = serializedProjection.filter { it.labels != null }.toSqlQuery() as SqlQuery.Select
    val custom = customProjection.filter { it.displayName == "Ada" }.toSqlQuery() as SqlQuery.Select

    val aggregateInner = aggregate.innerSelect()
    val scalarInner = scalar.innerSelect()
    val serializedInner = serialized.innerSelect()
    val aggregateItem = aggregateInner?.item("totalCount")
    val scalarItem = scalarInner?.item("latestAmount")
    val aggregateFunction = aggregateItem?.expr as? SqlExpr.Function
    val scalarSubquery = (scalarItem?.expr as? SqlExpr.Subquery)?.query as? SqlQuery.Select
    val aggregateWhere = aggregate.where as? SqlExpr.Binary
    val scalarWhere = scalar.where as? SqlExpr.Binary
    val serializedWhere = serialized.where as? SqlExpr.Binary
    val customWhere = custom.where as? SqlExpr.Binary

    val failures = listOfNotNull(
        expectFilterAlias(aggregateFunction?.name?.last == "COUNT") {
            "aggregate item was $aggregateItem"
        },
        expectFilterAlias(aggregateWhere?.operator == SqlBinaryOperator.GreaterThan) {
            "aggregate filter operator was ${aggregateWhere?.operator}"
        },
        expectFilterAlias(aggregateWhere.column("totalCount")) { "aggregate filter was $aggregateWhere" },
        expectFilterAlias(scalarSubquery?.outputNames() == listOf("amount")) {
            "scalar item was $scalarItem"
        },
        expectFilterAlias(scalarWhere?.operator == SqlBinaryOperator.GreaterThan) {
            "scalar filter operator was ${scalarWhere?.operator}"
        },
        expectFilterAlias(scalarWhere.column("latestAmount")) { "scalar filter was $scalarWhere" },
        expectFilterAlias(serializedInner?.item("labels") != null) {
            "serialized projection aliases were ${serializedInner?.outputNames()}"
        },
        expectFilterAlias(serializedWhere?.operator == SqlBinaryOperator.Is(withNot = true)) {
            "serialized filter operator was ${serializedWhere?.operator}"
        },
        expectFilterAlias(serializedWhere.column("labels")) { "serialized filter was $serializedWhere" },
        expectFilterAlias(customWhere?.operator == SqlBinaryOperator.Equal) {
            "custom projection filter operator was ${customWhere?.operator}"
        },
        expectFilterAlias(customWhere.column("displayName")) { "custom projection filter was $customWhere" },
    )

    return failures.firstOrNull() ?: "OK"
}

private fun SqlQuery.Select.innerSelect(): SqlQuery.Select? =
    ((from.singleOrNull() as? SqlTable.Subquery)?.query as? SqlQuery.Select)

private fun SqlQuery.Select.item(name: String): SqlSelectItem.Expr? =
    select.filterIsInstance<SqlSelectItem.Expr>().singleOrNull { it.metadata?.outputName == name }

private fun SqlQuery.Select.outputNames(): List<String> =
    select.filterIsInstance<SqlSelectItem.Expr>().mapNotNull { item ->
        item.metadata?.outputName ?: item.alias ?: (item.expr as? SqlExpr.Column)?.columnName
    }

private fun SqlExpr.Binary?.column(name: String): Boolean {
    val column = this?.left as? SqlExpr.Column
    return column?.tableName == "q" && column.columnName == name
}

private inline fun expectFilterAlias(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
