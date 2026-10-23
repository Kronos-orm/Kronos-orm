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

// Verifies Unit callback side effects preserve delegated SQL and Unit-adapted Boolean predicates.

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table("unit_callback_user")
data class UnitCallbackUser(var id: Int? = null) : KPojo

class CallbackInput {
    var reads = 0
    val id: Int get() { reads++; return 7 }
}

class CallbackSnapshot {
    var effects = 0
    var expression: SqlExpr? = null
    var parameters: Map<String, Any?> = emptyMap()
}

fun captureCondition(user: UnitCallbackUser, condition: ToFilter<UnitCallbackUser, Boolean?>): CallbackSnapshot {
    val snapshot = CallbackSnapshot()
    user.afterFilter filter@{
        sourceValues = user.toDataMap()
        condition(it)
        with(snapshot) {
            effects++
            expression = this@filter.sqlExpr
            parameters = this@filter.parameterValues.toMap()
        }
    }
    return snapshot
}

fun box(): String {
    val user = UnitCallbackUser()
    val input = CallbackInput()
    val condition: ToFilter<UnitCallbackUser, Boolean?> = { it.id == input.id }
    val expected = SqlExpr.Binary(SqlExpr.Column("unit_callback_user", "id"), SqlBinaryOperator.Equal,
        SqlExpr.Parameter(SqlParameter.Named("id")))
    val snapshot = captureCondition(user, condition)
    if (snapshot.expression != expected) return "Fail: forwarded SQL was ${snapshot.expression}"
    if (snapshot.parameters != mapOf("id" to 7)) return "Fail: forwarded parameters were ${snapshot.parameters}"
    if (snapshot.effects != 1 || input.reads != 1) return "Fail: forwarding effects=${snapshot.effects}, reads=${input.reads}"

    val unitPredicate: ToFilter<UnitCallbackUser, Unit> = { it.id == input.id }
    val unitTable = KTableForCondition<UnitCallbackUser>()
    unitTable.sourceValues = user.toDataMap()
    unitPredicate(unitTable, user)
    if (unitTable.sqlExpr != expected) return "Fail: Unit-adapted predicate SQL was ${unitTable.sqlExpr}"
    if (unitTable.parameterValues != mapOf("id" to 7)) return "Fail: Unit-adapted parameters were ${unitTable.parameterValues}"
    if (input.reads != 2) return "Fail: Unit-adapted capture reads were ${input.reads}"

    var assignments = 0
    val sideEffectTail: ToFilter<UnitCallbackUser, Unit> = {
        condition(it)
        assignments += 1
    }
    val sideEffectTable = KTableForCondition<UnitCallbackUser>()
    sideEffectTable.sourceValues = user.toDataMap()
    sideEffectTail(sideEffectTable, user)
    if (sideEffectTable.sqlExpr != expected) return "Fail: side-effect tail SQL was ${sideEffectTable.sqlExpr}"
    if (sideEffectTable.parameterValues != mapOf("id" to 7)) return "Fail: side-effect parameters were ${sideEffectTable.parameterValues}"
    if (assignments != 1 || input.reads != 3) return "Fail: assignments=$assignments, reads=${input.reads}"
    return "OK"
}
