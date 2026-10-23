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

// Verifies run, with and apply callbacks used as sort selectors retain their collected fields.

import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.types.ToSort

data class ScopeTailPerson(var id: Int? = null, var name: String? = null) : KPojo

fun ScopeTailPerson.collectTail(block: ToSort<ScopeTailPerson, Any?>): List<Pair<String, SqlOrdering>> {
    var result: List<Pair<String, SqlOrdering>>? = null
    afterSort {
        block!!(it)
        result = sortedItems.map { item ->
            val field = item as? KTableForSort.SortItem.FieldItem ?: error("Unexpected sort item $item")
            field.field.name to field.ordering
        }
    }
    return result ?: error("Sort callback was not invoked")
}

fun box(): String {
    val person = ScopeTailPerson()
    val runFields = person.collectTail { run { it.id.desc() } }
    val withFields = person.collectTail { with(this) { it.name.asc() } }
    val applyFields = person.collectTail { apply { it.id.desc() } }
    if (runFields != listOf("id" to SqlOrdering.Desc)) return "Fail: run fields were $runFields"
    if (withFields != listOf("name" to SqlOrdering.Asc)) return "Fail: with fields were $withFields"
    if (applyFields != listOf("id" to SqlOrdering.Desc)) return "Fail: apply fields were $applyFields"
    return "OK"
}
