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

// Verifies listOf, arrayOf, raw expression, and bare receiver sort forms.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.types.ToSort

@Table(name = "tb_sort_collection_receiver")
data class SortCollectionUser(
    var id: Int? = null,
    var name: String? = null,
    var score: Int? = null,
) : KPojo {
    fun column(name: String): Field = __columns.single { it.name == name }
}

fun SortCollectionUser.collectItems(block: ToSort<SortCollectionUser, Any?>): List<KTableForSort.SortItem> {
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

    val user = SortCollectionUser()
    val bare = user.collectItems { it }
    val list = user.collectItems { listOf(it.id.desc(), "score + 1".asc()) }
    val array = user.collectItems { arrayOf<Any?>(it.name.asc(), it.score.desc()) }

    val bareFields = bare.mapNotNull { (it as? KTableForSort.SortItem.FieldItem)?.field?.name }
    val listFirst = list.getOrNull(0) as? KTableForSort.SortItem.FieldItem
    val listSecond = list.getOrNull(1) as? KTableForSort.SortItem.ExpressionItem
    val arrayFirst = array.getOrNull(0) as? KTableForSort.SortItem.FieldItem
    val arraySecond = array.getOrNull(1) as? KTableForSort.SortItem.FieldItem

    return when {
        bareFields != listOf("id", "name", "score") -> "Fail: bare fields were $bareFields"
        bare.any { it.ordering != SqlOrdering.Asc } -> "Fail: bare orderings were ${bare.map { it.ordering }}"
        listFirst?.field != user.column("id") -> "Fail: list first was $listFirst"
        listFirst.ordering != SqlOrdering.Desc -> "Fail: list first ordering was ${listFirst.ordering}"
        (listSecond?.expression as? SqlExpr.UnsafeRaw)?.sql != "score + 1" ->
            "Fail: list second expression was ${listSecond?.expression}"
        listSecond.ordering != SqlOrdering.Asc -> "Fail: list second ordering was ${listSecond.ordering}"
        arrayFirst?.field != user.column("name") -> "Fail: array first was $arrayFirst"
        arrayFirst.ordering != SqlOrdering.Asc -> "Fail: array first ordering was ${arrayFirst.ordering}"
        arraySecond?.field != user.column("score") -> "Fail: array second was $arraySecond"
        arraySecond.ordering != SqlOrdering.Desc -> "Fail: array second ordering was ${arraySecond.ordering}"
        else -> "OK"
    }
}
