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

// Verifies every DSL retains scope values consumed by ordinary Kotlin expressions while set callbacks still collect assignments.

import com.kotlinorm.beans.dsl.KTableForCondition
import com.kotlinorm.beans.dsl.KTableForInsertSelect
import com.kotlinorm.beans.dsl.KTableForReference
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.KTableForSet
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToInsertSelect
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSet

data class ScopeValueUser(var id: Int? = 5, var name: String? = "original") : KPojo

fun box(): String {
    val user = ScopeValueUser()
    val id = user.__columns.single { it.name == "id" }
    val name = user.__columns.single { it.name == "name" }
    var effects = 0
    var captured = ""
    fun consume(value: String) { captured += value }
    val select: ToSelect<ScopeValueUser, Any?> = {
        consume(with(this) { effects++; "select" })
        val fallback = with(this) { null as String? } ?: with(this) { "fallback" }
        check(fallback == "fallback")
        var count = 0
        while (count++ < 2) consume(with(this) { "loop" })
        fun helper(): String { return with(this) { "helper" } }
        check(helper() == "helper")
        [it.id]
    }
    val selectTable = KTableForSelect<ScopeValueUser>()
    select!!(selectTable, user)
    if (selectTable.fields != listOf(id)) return "Fail: select collected ${selectTable.fields}"
    if (captured != "selectlooploop") return "Fail: ordinary values were $captured"

    val condition: ToFilter<ScopeValueUser, Boolean?> = {
        check(with(this) { effects++; it.id == 5 })
        it.id == 7
    }
    val conditionTable = KTableForCondition<ScopeValueUser>()
    condition!!(conditionTable, user)
    if (conditionTable.parameterValues != mapOf("id" to 7)) return "Fail: condition parameters were ${conditionTable.parameterValues}"

    val reference: ToReference<ScopeValueUser, Any?> = {
        val ordinary = with(this) { effects++; it::name }
        check(ordinary.name == "name")
        it::id
    }
    val referenceTable = KTableForReference<ScopeValueUser>()
    reference!!(referenceTable, user)
    if (referenceTable.fields != listOf(id)) return "Fail: references were ${referenceTable.fields}"

    val insert: ToInsertSelect<ScopeValueUser, Any?> = {
        val ordinary = with(this) { effects++; "insert" }
        check(ordinary == "insert")
        [it.id]
    }
    val insertTable = KTableForInsertSelect<ScopeValueUser>()
    insert!!(insertTable, user)
    if (insertTable.values != listOf(id)) return "Fail: INSERT SELECT values were ${insertTable.values}"

    val set: ToSet<ScopeValueUser, Unit> = {
        val value = with(this) { effects++; it.name = "nested"; "set" }
        check(value == "set")
        it.name = value
    }
    val setTable = KTableForSet<ScopeValueUser>()
    set!!(setTable, user)
    if (setTable.fieldParamMap != mapOf(name to "set")) return "Fail: SET parameters were ${setTable.fieldParamMap}"
    if (effects != 5) return "Fail: scope values evaluated $effects times"
    return "OK"
}
