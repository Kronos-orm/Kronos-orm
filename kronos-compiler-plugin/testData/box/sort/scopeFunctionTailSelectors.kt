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

// Verifies result-returning run/with selectors collect fields while receiver-returning apply stays ordinary.

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
    val ifFields = person.collectTail { if (true) run { it.id.desc() } else with(this) { it.name.asc() } }
    val elseFields = person.collectTail { if (false) run { it.id.desc() } else with(this) { it.name.asc() } }
    val whenFields = person.collectTail {
        when (1) { 1 -> run { with(this) { it.name.asc() } }; else -> run { it.id.desc() } }
    }
    val tryFields = person.collectTail {
        try { run { it.id.desc() } } catch (_: IllegalArgumentException) { with(this) { it.name.asc() } }
    }
    val recoveredFields = person.collectTail {
        try {
            require(false) { "recover selector" }
            run { it.id.desc() }
        } catch (expected: IllegalArgumentException) {
            check(expected.message == "recover selector")
            with(this) { it.name.asc() }
        }
    }
    if (ifFields != listOf("id" to SqlOrdering.Desc)) return "Fail: if fields were $ifFields"
    if (elseFields != listOf("name" to SqlOrdering.Asc)) return "Fail: else fields were $elseFields"
    if (whenFields != listOf("name" to SqlOrdering.Asc)) return "Fail: when fields were $whenFields"
    if (tryFields != listOf("id" to SqlOrdering.Desc)) return "Fail: try fields were $tryFields"
    if (recoveredFields != listOf("name" to SqlOrdering.Asc)) return "Fail: catch fields were $recoveredFields"
    if (runFields != listOf("id" to SqlOrdering.Desc)) return "Fail: run fields were $runFields"
    if (withFields != listOf("name" to SqlOrdering.Asc)) return "Fail: with fields were $withFields"
    if (applyFields != emptyList<Pair<String, SqlOrdering>>()) return "Fail: apply collected callback fields $applyFields"
    return "OK"
}
