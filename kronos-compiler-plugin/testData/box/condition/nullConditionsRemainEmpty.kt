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

// Verifies literal and labeled null conditions remain empty and preserve ordinary side effects.

import com.kotlinorm.beans.dsl.KTableForCondition
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

data class NullConditionUser(var id: Int? = null) : KPojo

fun box(): String {
    val user = NullConditionUser(7)
    var effects = 0
    val direct: ToFilter<NullConditionUser, Boolean?> = { null }
    val labeled: ToFilter<NullConditionUser, Boolean?> = condition@{
        effects++
        return@condition null
    }
    for ((index, condition) in listOf(direct, labeled).withIndex()) {
        val table = KTableForCondition<NullConditionUser>()
        table.sourceValues = user.toDataMap()
        val result = condition(table, user)
        if (result != null) return "Fail: null condition $index returned $result"
        if (table.sqlExpr != null) return "Fail: null condition $index produced ${table.sqlExpr}"
        if (table.parameterValues.isNotEmpty()) return "Fail: null condition $index bound ${table.parameterValues}"
        if (table.sourceValues != user.toDataMap()) return "Fail: null condition $index changed source values ${table.sourceValues}"
    }
    if (effects != 1) return "Fail: labeled null side effects were $effects"
    return "OK"
}
