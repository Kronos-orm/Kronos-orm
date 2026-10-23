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

// Verifies apply/also preserve the receiver and never select their callback tail, while let keeps its ordinary value.

import com.kotlinorm.beans.dsl.KTableForCondition
import com.kotlinorm.beans.dsl.KTableForInsertSelect
import com.kotlinorm.beans.dsl.KTableForReference
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.KTableForSet
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToInsertSelect
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSet
import com.kotlinorm.types.ToSort

data class ScopeReceiverUser(var id: Int? = 5, var name: String? = "source") : KPojo

fun box(): String {
    val user = ScopeReceiverUser()
    val id = user.__columns.single { it.name == "id" }
    var effects = 0
    val select: ToSelect<ScopeReceiverUser, Any?> = { row ->
        val applied = this.apply { effects++; row.name }
        check(applied === this)
        val original = this.also { context -> check(context === this); effects++; row.name }
        check(original === this)
        val value = this.let { context -> check(context === this); effects++; row.name }
        check(value == "source")
        [row.id]
    }
    val selectTable = KTableForSelect<ScopeReceiverUser>()
    select!!(selectTable, user)
    if (selectTable.fields != listOf(id)) return "Fail: apply/also/let selected extra fields ${selectTable.fields}"

    val condition: ToFilter<ScopeReceiverUser, Boolean?> = { row ->
        val applied = this.apply { effects++; row.name == "other" }
        check(applied === this)
        row.id == 7
    }
    val conditionTable = KTableForCondition<ScopeReceiverUser>()
    condition!!(conditionTable, user)
    if (conditionTable.parameterValues != mapOf("id" to 7)) return "Fail: apply added a condition ${conditionTable.parameterValues}"

    val reference: ToReference<ScopeReceiverUser, Any?> = { row ->
        check(this.apply { effects++; row::name } === this)
        row::id
    }
    val referenceTable = KTableForReference<ScopeReceiverUser>()
    reference!!(referenceTable, user)
    if (referenceTable.fields != listOf(id)) return "Fail: apply added a reference ${referenceTable.fields}"

    val insert: ToInsertSelect<ScopeReceiverUser, Any?> = { row ->
        check(this.apply { effects++; "not an INSERT value" } === this)
        [row.id]
    }
    val insertTable = KTableForInsertSelect<ScopeReceiverUser>()
    insert!!(insertTable, user)
    if (insertTable.values != listOf(id)) return "Fail: apply added an INSERT value ${insertTable.values}"

    val sort: ToSort<ScopeReceiverUser, Any?> = { row ->
        check(this.apply { effects++; row.name.desc() } === this)
        row.id.asc()
    }
    val sortTable = KTableForSort<ScopeReceiverUser>()
    sort!!(sortTable, user)
    val sorted = sortTable.sortedItems.map { (it as KTableForSort.SortItem.FieldItem).field to it.ordering }
    if (sorted != listOf(id to SqlOrdering.Asc)) return "Fail: apply added sort fields $sorted"

    val set: ToSet<ScopeReceiverUser, Unit> = { row ->
        val applied = this.apply { effects++; row.name = "assigned" }
        check(applied === this)
    }
    val setTable = KTableForSet<ScopeReceiverUser>()
    set!!(setTable, user)
    val name = user.__columns.single { it.name == "name" }
    if (setTable.fieldParamMap != mapOf(name to "assigned")) return "Fail: SET callback lost its assignment ${setTable.fieldParamMap}"
    if (effects != 8) return "Fail: scope callbacks evaluated $effects times"
    return "OK"
}
