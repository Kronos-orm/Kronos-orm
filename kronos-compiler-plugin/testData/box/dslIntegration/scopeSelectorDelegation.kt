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

// Verifies scope selectors delegate collection once across DSLs and preserve branch/try side effects.

import com.kotlinorm.beans.dsl.KTableForCondition
import com.kotlinorm.beans.dsl.KTableForInsertSelect
import com.kotlinorm.beans.dsl.KTableForReference
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToInsertSelect
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect

data class ScopeSelectorUser(var id: Int? = 5, var name: String? = "source") : KPojo

fun box(): String {
    val user = ScopeSelectorUser()
    val id = user.__columns.single { it.name == "id" }
    val name = user.__columns.single { it.name == "name" }
    var chooseId = true
    var effects = 0
    val select: ToSelect<ScopeSelectorUser, Any?> = {
        if (chooseId) run { effects++; it.id } else it.name
    }
    val selectedId = KTableForSelect<ScopeSelectorUser>()
    select!!(selectedId, user)
    if (selectedId.fields != listOf(id)) return "Fail: selected ID fields were ${selectedId.fields}"
    chooseId = false
    val selectedName = KTableForSelect<ScopeSelectorUser>()
    select(selectedName, user)
    if (selectedName.fields != listOf(name)) return "Fail: direct branch fields were ${selectedName.fields}"

    val condition: ToFilter<ScopeSelectorUser, Boolean?> = {
        with(this) { effects++; it.id == 7 }
    }
    val conditionTable = KTableForCondition<ScopeSelectorUser>()
    condition!!(conditionTable, user)
    if (conditionTable.parameterValues != mapOf("id" to 7)) return "Fail: delegated condition parameters were ${conditionTable.parameterValues}"

    val reference: ToReference<ScopeSelectorUser, Any?> = {
        try { run { effects++; it::id } } finally { effects++ }
    }
    val referenceTable = KTableForReference<ScopeSelectorUser>()
    reference!!(referenceTable, user)
    if (referenceTable.fields != listOf(id)) return "Fail: delegated reference fields were ${referenceTable.fields}"

    val insert: ToInsertSelect<ScopeSelectorUser, Unit> = {
        when (chooseId) {
            true -> "direct"
            false -> with(this) { effects++; "delegated" }
        }
    }
    val insertTable = KTableForInsertSelect<ScopeSelectorUser>()
    insert!!(insertTable, user)
    if (insertTable.values != listOf("delegated")) return "Fail: delegated INSERT SELECT values were ${insertTable.values}"
    chooseId = true
    val directInsert = KTableForInsertSelect<ScopeSelectorUser>()
    insert(directInsert, user)
    if (directInsert.values != listOf("direct")) return "Fail: direct INSERT SELECT values were ${directInsert.values}"
    if (effects != 5) return "Fail: selector side effects ran $effects times"
    return "OK"
}
