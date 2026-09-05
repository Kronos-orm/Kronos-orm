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

// Verifies that scalar subquery aliases are FIR-visible on Selected and orderBy Context receivers.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import kotlin.reflect.KClass

@Table("tb_projection_scalar_alias_user")
data class ProjectionScalarAliasUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_projection_scalar_alias_order")
data class ProjectionScalarAliasOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val clause = ProjectionScalarAliasUser()
        .select {
            [
                it.id,
                ProjectionScalarAliasOrder()
                    .select { order -> order.amount }
                    .limit(1)
                    .alias("lastAmount")
            ]
        }
        .orderBy { it.lastAmount.desc() }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val selected = clause.first()
        val aliasValue: Int? = selected.lastAmount
        return "Fail: selected alias unexpectedly evaluated as $aliasValue"
    }

    val statement = clause.toSqlQuery() as SqlQuery.Select

    val failures = listOfNotNull(
        expect(statement.hasScalarSubqueryAlias("lastAmount")) { "select aliases were ${statement.selectAliases()}" },
        expect(statement.hasOrderByColumn("lastAmount", SqlOrdering.Desc)) { "order by was ${statement.allOrderBy()}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}

fun SqlQuery.Select.selectAliases(): List<String?> {
    return allSelectStatements().flatMap { statement ->
        statement.select.map { item -> (item as? SqlSelectItem.Expr)?.metadata?.outputName }
    }
}

fun SqlQuery.Select.hasScalarSubqueryAlias(name: String): Boolean {
    return allSelectStatements().any { statement ->
        statement.select.any { item ->
            item is SqlSelectItem.Expr &&
                item.alias == name &&
                item.expr is SqlExpr.Subquery &&
                item.metadata?.outputName == name
        }
    }
}

fun SqlQuery.Select.hasOrderByColumn(name: String, direction: SqlOrdering): Boolean {
    return allOrderBy().any { item ->
        item.ordering == direction &&
            item.expr.containsExpression(SqlExpr.Column::class) { it.columnName == name }
    }
}

fun SqlQuery.Select.allOrderBy(): List<SqlOrderingItem> {
    return allSelectStatements().flatMap { it.orderBy }
}

fun SqlQuery.Select.allSelectStatements(): List<SqlQuery.Select> {
    val nested = ((from.singleOrNull() as? SqlTable.Subquery)?.query as? SqlQuery.Select)?.allSelectStatements().orEmpty()
    return listOf(this) + nested
}

fun <T : Any> Any?.containsExpression(type: KClass<T>, match: (T) -> Boolean): Boolean {
    return when (this) {
        null -> false
        else -> {
            @Suppress("UNCHECKED_CAST")
            val currentMatches = type.isInstance(this) && match(this as T)
            currentMatches || when (this) {
                is SqlExpr.Binary -> left.containsExpression(type, match) || right.containsExpression(type, match)
                else -> false
            }
        }
    }
}
