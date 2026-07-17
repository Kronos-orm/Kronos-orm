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
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
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

data class ConditionPositiveValueOuter(var value: ConditionPositiveValueInner? = null) : KPojo

data class ConditionPositiveValueInner(var value: Int? = null) : KPojo

class ConditionPositiveContext(
    val ownerId: Int? = null,
    val name: String? = null,
    val status: Int? = null,
)

data class ConditionPositiveDataContext(val ownerId: Int? = null, val name: String? = null)

object ConditionPositiveObjectContext {
    val ownerId: Int? get() = 11
    val name: String? get() = "Object"
}

class ConditionPositiveStaticContext(val marker: Int = 0) {
    companion object {
        val companionOwnerId: Int? get() = 12
        val companionName: String? get() = "Companion"

        @JvmStatic
        val staticOwnerId: Int? get() = 13

        @JvmStatic
        val staticName: String? get() = "Static"
    }
}

val conditionPositiveTopLevelOwnerId: Int? get() = 14
val conditionPositiveTopLevelName: String? get() = "TopLevel"

fun validCurrentAndExplicitValueSources() {
    val probe = ConditionPositiveUser(id = 7, value = 8)
    val context = ConditionPositiveContext(ownerId = 9, name = "Ada", status = 1)
    val dataContext = ConditionPositiveDataContext(ownerId = 10, name = "Data")
    val ownerId = context.ownerId
    val threshold = 3

    ConditionPositiveUser()
        .select()
        .where {
            it.id == it.ownerId &&
                it.status > threshold &&
                it.id == probe.id.value &&
                it.ownerId == context.ownerId &&
                it.id == dataContext.ownerId &&
                it.id == ConditionPositiveObjectContext.ownerId &&
                it.id == ConditionPositiveStaticContext.companionOwnerId &&
                it.id == ConditionPositiveStaticContext.staticOwnerId &&
                it.id == conditionPositiveTopLevelOwnerId &&
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
        .where { (it.id > 0).takeIf(context.ownerId != null) }

    ConditionPositiveUser()
        .select()
        .where { if (probe.id == 7) it.id > 0 else it.id < 0 }

    ConditionPositiveUser()
        .select()
        .where { if (context.ownerId != null) it.ownerId == context.ownerId else it.id < 0 }

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
        .where {
            when (context.status) {
                1 -> it.status == context.status
                else -> it.status < 0
            }
        }

    ConditionPositiveUser()
        .select()
        .having { it.status != probe.status.value }

    ConditionPositiveUser()
        .select()
        .having { it.status == context.status }

    ConditionPositiveUser()
        .select()
        .where { it.id == f.length(context.name) }

    ConditionPositiveUser()
        .select()
        .where {
            it.id == f.length(dataContext.name) &&
                it.id == f.length(ConditionPositiveObjectContext.name) &&
                it.id == f.length(ConditionPositiveStaticContext.companionName) &&
                it.id == f.length(ConditionPositiveStaticContext.staticName) &&
                it.id == f.length(conditionPositiveTopLevelName)
        }

    ConditionPositiveUser()
        .select()
        .where { it.id == ConditionPositiveUser(id = 9).id.value }

    ConditionPositiveUser()
        .select()
        .where { it.ownerId == ownerId }

    ConditionPositiveUser()
        .update()
        .set { it.status = threshold }
        .where { it.id == probe.id.value }

    ConditionPositiveUser()
        .update()
        .set { it.status = threshold }
        .where { it.ownerId == context.ownerId }

    ConditionPositiveUser()
        .delete()
        .where { it.status < threshold }

    val nestedProbe = ConditionPositiveValueOuter(ConditionPositiveValueInner(value = 15))
    ConditionPositiveValueOuter()
        .select()
        .where { it.value?.value == nestedProbe.value?.value.value }
}

fun validJoinSources() {
    val context = ConditionPositiveContext(ownerId = 9)
    ConditionPositiveUser().join(ConditionPositiveOrder()) { user, order ->
        on { user.id == order.userId }
        on { user.ownerId == context.ownerId }
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
