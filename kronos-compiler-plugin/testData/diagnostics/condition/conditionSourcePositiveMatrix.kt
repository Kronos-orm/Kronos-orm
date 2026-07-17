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

// Verifies registered SQL sources and explicit Kotlin values remain valid in every condition scope.

import com.kotlinorm.annotations.Ignore
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update

data class ConditionPositiveUser(
    var id: Int? = null,
    var ownerId: Int? = null,
    var status: Int? = null,
    var value: Int? = null,
    @Ignore var manager: ConditionPositiveUser? = null,
) : KPojo

data class ConditionPositiveOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
    var amount: Int? = null,
) : KPojo

fun validCurrentAndExplicitValueSources() {
    val probe = ConditionPositiveUser(id = 7, value = 8)
    val threshold = 3

    ConditionPositiveUser()
        .select()
        .where {
            it.id == it.ownerId &&
                it.status > threshold &&
                it.id == probe.id.value &&
                it.value == probe.value.value &&
                run { it.manager?.id == it.id }
        }

    ConditionPositiveUser()
        .select()
        .where { (it.id > 0).takeIf(probe.id == 7) }

    ConditionPositiveUser()
        .select()
        .where { (it.id > 0).takeUnless(probe.id == 8) }

    ConditionPositiveUser()
        .select()
        .where { if (probe.id == 7) it.id > 0 else it.id < 0 }

    ConditionPositiveUser()
        .select()
        .where {
            when (probe.id) {
                7 -> it.id > 0
                else -> it.id < 0
            }
        }

    ConditionPositiveUser()
        .select()
        .having { it.status != probe.status.value }

    ConditionPositiveUser()
        .select()
        .where { it.id == ConditionPositiveUser(id = 9).id.value }

    ConditionPositiveUser()
        .update()
        .set { it.status = threshold }
        .where { it.id == probe.id.value }

    ConditionPositiveUser()
        .delete()
        .where { it.status < threshold }
}

fun validJoinSources() {
    ConditionPositiveUser().join(ConditionPositiveOrder()) { user, order ->
        on { user.id == order.userId }
        leftJoin(order) { user.id == order.userId }
        where { user.status == order.status }
        having { order.amount > user.status }
    }
}

fun validCorrelatedSubquerySources() {
    ConditionPositiveUser()
        .select()
        .where { user ->
            exists(
                ConditionPositiveOrder()
                    .select()
                    .where { order ->
                        order.userId == user.id &&
                            order.status == user.status
                    }
            )
        }

    ConditionPositiveUser()
        .update()
        .set { user ->
            ConditionPositiveOrder()
                .select { order -> order.status }
                .where { order -> order.userId == user.id }
                .limit(1)
            user.status = 1
        }
        .where { it.id > 0 }
}
