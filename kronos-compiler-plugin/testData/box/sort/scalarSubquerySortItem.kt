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

// Verifies that order-by DSL can hand off selectable scalar subqueries as sort items.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.ast.DeferredSubqueryExpression
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.enums.SortType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.types.ToSort

@Table(name = "tb_sort_subquery_user")
data class SortSubqueryUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table(name = "tb_sort_subquery_order")
data class SortSubqueryOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun SortSubqueryUser.collectSortItems(block: ToSort<SortSubqueryUser, Any?>): List<KTableForSort.SortItem> {
    var result: List<KTableForSort.SortItem>? = null
    afterSort {
        block!!(it)
        result = sortedItems.toList()
    }
    return result ?: error("sort block did not run")
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val items = SortSubqueryUser().collectSortItems {
        SortSubqueryOrder()
            .select { order -> order.status }
            .where { order -> order.userId == 1 }
            .limit(1)
            .desc()
    }

    val item = items.singleOrNull() as? KTableForSort.SortItem.ExpressionItem
    val expression = item?.expression

    val failures = listOfNotNull(
        expect(item != null) { "item was ${items.singleOrNull()?.let { it::class.qualifiedName }}" },
        expect(item?.sortType == SortType.DESC) { "sort type was ${item?.sortType}" },
        expect(expression is DeferredSubqueryExpression.Scalar) { "expression was ${expression?.let { it::class.qualifiedName }}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
