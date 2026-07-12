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

// Verifies mutableListOf, setOf, raw SQL, and scalar subquery sort directions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.types.ToSort

@Table(name = "tb_sort_matrix_user")
data class SortMatrixUser(
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
) : KPojo {
    fun column(name: String): Field = __columns.single { it.name == name }
}

@Table(name = "tb_sort_matrix_order")
data class SortMatrixOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

fun SortMatrixUser.collectItems(block: ToSort<SortMatrixUser, Any?>): List<KTableForSort.SortItem> {
    var result: List<KTableForSort.SortItem>? = null
    afterSort {
        block!!(it)
        result = sortedItems.toList()
    }
    return result ?: error("sort block did not run")
}

fun expectSort(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = SortMatrixUser()
    val mutableItems = user.collectItems {
        mutableListOf("lower(name)".desc(), it.status.asc())
    }
    val setItems = user.collectItems {
        setOf(
            SortMatrixOrder()
                .select { order -> order.amount }
                .where { order -> order.userId == 7 }
                .limit(1)
                .asc(),
            SortMatrixOrder()
                .select { order -> order.amount }
                .where { order -> order.userId == 8 }
                .limit(1)
                .desc()
        )
    }
    val raw = mutableItems.getOrNull(0) as? KTableForSort.SortItem.ExpressionItem
    val field = mutableItems.getOrNull(1) as? KTableForSort.SortItem.FieldItem
    val subqueryAsc = setItems.getOrNull(0) as? KTableForSort.SortItem.SelectableItem
    val subqueryDesc = setItems.getOrNull(1) as? KTableForSort.SortItem.SelectableItem

    val failures = listOfNotNull(
        expectSort(mutableItems.size == 2) { "mutable size was ${mutableItems.size}" },
        expectSort((raw?.expression as? SqlExpr.UnsafeRaw)?.sql == "lower(name)") { "raw expression was ${raw?.expression}" },
        expectSort(raw?.ordering == SqlOrdering.Desc) { "raw ordering was ${raw?.ordering}" },
        expectSort(field?.field == user.column("status")) { "field item was ${field?.field}" },
        expectSort(field?.ordering == SqlOrdering.Asc) { "field ordering was ${field?.ordering}" },
        expectSort(setItems.size == 2) { "set size was ${setItems.size}" },
        expectSort(subqueryAsc?.ordering == SqlOrdering.Asc) { "subqueryAsc ordering was ${subqueryAsc?.ordering}" },
        expectSort(subqueryAsc?.query?.toSqlQuery() is SqlQuery.Select) {
            "subqueryAsc query was ${subqueryAsc?.query?.toSqlQuery()?.let { it::class.qualifiedName }}"
        },
        expectSort(subqueryDesc?.ordering == SqlOrdering.Desc) { "subqueryDesc ordering was ${subqueryDesc?.ordering}" },
        expectSort(subqueryDesc?.query?.toSqlQuery() is SqlQuery.Select) {
            "subqueryDesc query was ${subqueryDesc?.query?.toSqlQuery()?.let { it::class.qualifiedName }}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}
