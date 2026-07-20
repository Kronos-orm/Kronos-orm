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

// Verifies all condition clauses reject KPojo properties from unregistered SQL sources.

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update

data class ConditionDiagnosticUser(
    var id: Int? = null,
    var name: String? = null,
    var ownerId: Int? = null,
    var value: Int? = null,
) : KPojo

data class ConditionDiagnosticOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

fun invalidSelectWhereAndHaving() {
    val probe = ConditionDiagnosticUser(id = 7)

    ConditionDiagnosticUser()
        .select()
        .where { it.id == <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!> }

    ConditionDiagnosticUser()
        .select()
        .where { <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!> != it.id }

    ConditionDiagnosticUser()
        .select()
        .having { it.id > <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!> }

    ConditionDiagnosticUser()
        .select()
        .having { <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!> <= it.id }

    ConditionDiagnosticUser()
        .select()
        .where { it.id == <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>ConditionDiagnosticUser(id = 11).id<!> }

    ConditionDiagnosticUser()
        .select()
        .where { it.id == f.length(<!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.name<!>) }

    ConditionDiagnosticUser()
        .select()
        .where { (it.id == <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!>).takeIf(probe.id == 7) }

    ConditionDiagnosticUser()
        .select()
        .where { (it.id == <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!>).takeUnless(probe.id == 8) }

    ConditionDiagnosticUser()
        .select()
        .where { if (probe.id == 7) it.id == <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!> else it.id > 0 }

    ConditionDiagnosticUser()
        .select()
        .where {
            when (probe.id) {
                7 -> it.id == <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!>
                else -> it.id > 0
            }
        }
}

fun invalidMutationWhere() {
    val probe = ConditionDiagnosticUser(id = 8)

    ConditionDiagnosticUser()
        .update()
        .set { it.name = "updated" }
        .where { it.id >= <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!> }

    ConditionDiagnosticUser()
        .delete()
        .where { <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!> < it.id }
}

fun invalidJoinConditions() {
    val probe = ConditionDiagnosticUser(id = 9)

    ConditionDiagnosticUser().join(ConditionDiagnosticOrder()) { user, order ->
        leftJoin { user.id == <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!> }
            .select { user.id }
            .where { order.userId == <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!> }
            .having { <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!> != user.id }
    }
}

fun invalidCorrelatedSubqueryAndRealValueField() {
    val probe = ConditionDiagnosticUser(value = 10)

    ConditionDiagnosticUser()
        .select()
        .where { user ->
            exists(
                ConditionDiagnosticOrder()
                    .select()
                    .where { order -> order.userId == <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.id<!> }
            ) && user.value == <!KRONOS_UNREGISTERED_CONDITION_SOURCE!>probe.value<!>
        }
}
