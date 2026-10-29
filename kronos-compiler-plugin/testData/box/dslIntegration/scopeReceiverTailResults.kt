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

// Verifies tail apply/also return the DSL context unchanged rather than delegating selection to the callback.

import com.kotlinorm.beans.dsl.KTableForCondition
import com.kotlinorm.beans.dsl.KTableForInsertSelect
import com.kotlinorm.beans.dsl.KTableForReference
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToInsertSelect
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSort

data class TailReceiverUser(var id: Int? = 5, var name: String? = "source") : KPojo

fun box(): String {
    val user = TailReceiverUser()
    var effects = 0
    val select: ToSelect<TailReceiverUser, Any?> = { row -> this.apply { effects++; row.name } }
    val selectTable = KTableForSelect<TailReceiverUser>()
    if (select!!(selectTable, user) !== selectTable || selectTable.fields.isNotEmpty()) {
        return "Fail: tail apply changed the SELECT receiver or selected ${selectTable.fields}"
    }
    val condition: ToFilter<TailReceiverUser, Any?> = { row -> this.apply { effects++; row.id == 99 } }
    val conditionTable = KTableForCondition<TailReceiverUser>()
    if (condition!!(conditionTable, user) !== conditionTable || conditionTable.sqlExpr != null ||
        conditionTable.parameterValues.isNotEmpty()) return "Fail: tail apply added a predicate"

    val reference: ToReference<TailReceiverUser, Any?> = { row -> this.apply { effects++; row::name } }
    val referenceTable = KTableForReference<TailReceiverUser>()
    if (reference!!(referenceTable, user) !== referenceTable || referenceTable.fields.isNotEmpty()) {
        return "Fail: tail apply changed references ${referenceTable.fields}"
    }
    val insert: ToInsertSelect<TailReceiverUser, Any?> = { this.apply { effects++; "not a value" } }
    val insertTable = KTableForInsertSelect<TailReceiverUser>()
    if (insert!!(insertTable, user) !== insertTable || insertTable.values.isNotEmpty()) {
        return "Fail: tail apply changed INSERT values ${insertTable.values}"
    }
    val sort: ToSort<TailReceiverUser, Any?> = { row -> this.also { effects++; row.name.desc() } }
    val sortTable = KTableForSort<TailReceiverUser>()
    if (sort!!(sortTable, user) !== sortTable || sortTable.sortedItems.isNotEmpty()) {
        return "Fail: tail also changed sort items ${sortTable.sortedItems}"
    }
    if (effects != 5) return "Fail: tail callback effects ran $effects times"
    return "OK"
}
