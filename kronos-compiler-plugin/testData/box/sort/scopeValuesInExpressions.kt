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

// Verifies ordinary scope values in arguments, comparisons, collection literals and helper returns remain values.

import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.types.ToSort

data class ScopeValuePerson(var id: Int? = null, var name: String? = null) : KPojo

fun box(): String {
    val person = ScopeValuePerson()
    var captured = ""
    var effects = 0
    var sorted: List<KTableForSort.SortItem>? = null
    fun consume(value: String) { captured += value }
    val block: ToSort<ScopeValuePerson, Any?> = {
        consume(with(this) { effects++; "argument" })
        check(with(this) { effects++; "name" } == "name")
        check(run { effects++; "comparison" } == "comparison")
        fun helper(): String = with(this) { effects++; "helper" }
        fun returned(): String { return run { effects++; "return" } }
        consume(helper())
        consume(returned())
        val values = listOf(with(this) { effects++; "list" })
        check(values == listOf("list"))
        it.id.desc()
    }
    person.afterSort { block!!(it); sorted = sortedItems.toList() }
    if (captured != "argumenthelperreturn") return "Fail: captured values were $captured"
    if (effects != 6) return "Fail: side effects ran $effects times"
    val fields = sorted?.map { (it as KTableForSort.SortItem.FieldItem).field.name to it.ordering }
    if (fields != listOf("id" to SqlOrdering.Desc)) return "Fail: selected sort fields were $fields"
    return "OK"
}
