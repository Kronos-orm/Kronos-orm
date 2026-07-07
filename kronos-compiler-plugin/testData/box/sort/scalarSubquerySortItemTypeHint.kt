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

// Verifies scalar subquery type hints in orderBy still lower as scalar subquery sort items.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.types.ToSort

@Table(name = "tb_sort_subquery_hint_user")
data class SortSubqueryHintUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table(name = "tb_sort_subquery_hint_order")
data class SortSubqueryHintOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun SortSubqueryHintUser.collectSortItems(block: ToSort<SortSubqueryHintUser, Any?>): List<KTableForSort.SortItem> {
    var result: List<KTableForSort.SortItem>? = null
    afterSort {
        block!!(it)
        result = sortedItems.toList()
    }
    return result ?: error("sort block did not run")
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val items = SortSubqueryHintUser().collectSortItems {
        (SortSubqueryHintOrder()
            .select { order -> order.status }
            .where { order -> order.userId == 1 }
            .limit(1) as Int?).desc()
    }

    val item = items.singleOrNull() as? KTableForSort.SortItem.SelectableItem

    val failures = listOfNotNull(
        expect(item != null) { "item was ${items.singleOrNull()?.let { it::class.qualifiedName }}" },
        expect(item?.ordering == SqlOrdering.Desc) { "sort type was ${item?.ordering}" },
        expect(item?.query?.toSqlQuery() is com.kotlinorm.syntax.statement.SqlQuery.Select) {
            "query was ${item?.query?.toSqlQuery()?.let { it::class.qualifiedName }}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
