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

// Verifies transformed Boolean condition lambdas retain a real return on every exit.

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table("condition_return_user")
data class ConditionReturnUser(var id: Int? = null) : KPojo

class CapturedConditionId {
    var reads = 0
    val value: Int get() { reads++; return 7 }
}

fun box(): String {
    val user = ConditionReturnUser()
    val captured = CapturedConditionId()
    val direct: ToFilter<ConditionReturnUser, Boolean?> = { it.id == captured.value }
    val explicit: ToFilter<ConditionReturnUser, Boolean?> = condition@{
        val id = run { captured.value }
        return@condition it.id == id
    }
    val expected = SqlExpr.Binary(
        SqlExpr.Column("condition_return_user", "id"),
        SqlBinaryOperator.Equal,
        SqlExpr.Parameter(SqlParameter.Named("id")),
    )
    for ((index, condition) in listOf(direct, explicit).withIndex()) {
        val table = KTableForCondition<ConditionReturnUser>()
        table.sourceValues = user.toDataMap()
        val returned = condition(table, user)
        if (returned != false) return "condition $index returned $returned"
        if (table.sqlExpr != expected) return "condition $index expression ${table.sqlExpr}"
        if (table.parameterValues != mapOf("id" to 7)) return "condition $index parameters ${table.parameterValues}"
        if (captured.reads != index + 1) return "condition $index evaluated capture ${captured.reads} times"
    }
    return "OK"
}
