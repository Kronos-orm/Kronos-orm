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

// Verifies nested function returns keep their values while the outer sort lambda still collects its fields once.

import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.types.ToSort

data class NestedReturnSortPerson(var id: Int? = null, var name: String? = null) : KPojo

fun NestedReturnSortPerson.collectNestedSort(block: ToSort<NestedReturnSortPerson, Any?>): List<KTableForSort.SortItem> {
    var result: List<KTableForSort.SortItem>? = null
    afterSort { block!!(it); result = sortedItems.toList() }
    return result ?: error("Sort callback was not invoked")
}

class NestedSortLabel { var value: String = "" }

fun box(): String {
    val person = NestedReturnSortPerson()
    var effects = 0
    val holder = NestedSortLabel()
    val items = person.collectNestedSort {
        fun score(): Int { return 3 }
        val label = run { return@run "name" }
        val otherLabel = with(this) { return@with "name" }
        var reassigned = ""
        reassigned = run { return@run "reassigned" }
        holder.value = with(this) { return@with "stored" }
        if (reassigned != "reassigned") error("Reassigned scope value changed $reassigned")
        effects += score()
        if (label != "name" || otherLabel != "name") error("Nested return changed $label/$otherLabel")
        [it.id.desc(), it.name.asc()]
    }
    if (holder.value != "stored") return "Fail: property scope value was ${holder.value}"
    if (effects != 3) return "Fail: nested side effect count was $effects"
    val fields = items.map { (it as? KTableForSort.SortItem.FieldItem)?.field?.name to it.ordering }
    val expected = listOf("id" to SqlOrdering.Desc, "name" to SqlOrdering.Asc)
    if (fields != expected) return "Fail: outer sort fields were $fields"
    return "OK"
}
