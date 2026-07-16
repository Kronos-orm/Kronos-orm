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

// Verifies valid projection checker boundary forms do not report diagnostics.

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.functions.bundled.exts.MathFunctions.abs
import com.kotlinorm.functions.bundled.exts.MathFunctions.add
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.max
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select

data class ProjectionCheckerPositiveUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
) : KPojo

data class ProjectionCheckerPositiveOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
    var amount: Int? = null,
) : KPojo

data class ProjectionCheckerPositiveTarget(
    var name: String? = null,
    var status: Int? = null,
    var amount: Int? = null,
) : KPojo

fun validSingleAndWrappedProjectionItems() {
    ProjectionCheckerPositiveUser()
        .select { it.id }

    ProjectionCheckerPositiveUser()
        .select {
            arrayOf<Any?>(it.id, it.name.alias("userName"))
        }

    ProjectionCheckerPositiveUser()
        .select { [it - listOf(it.id, it.name), it.id.alias("sourceId")] }

    ProjectionCheckerPositiveUser()
        .select { [it - listOf(it.id, it.name, it.status), it.name.alias("restoredName")] }

    ProjectionCheckerPositiveUser()
        .select { [it - [it.id, it.name], it.status.alias("sourceStatus")] }
}

fun validInsertSelectCollectionFormsAndNullValues() {
    ProjectionCheckerPositiveUser()
        .select { [it.id, it.name, it.status] }
        .insert<ProjectionCheckerPositiveTarget> {
            listOf(it.name, it.status, null)
        }

    ProjectionCheckerPositiveUser()
        .select { [it.id, it.name, it.status] }
        .insert<ProjectionCheckerPositiveTarget> {
            arrayOf<Any?>(it.name, null, it.status)
        }
}

fun validPredicateContainsAndQuantifierForms() {
    ProjectionCheckerPositiveUser()
        .select()
        .where {
            ProjectionCheckerPositiveOrder()
                .select { order -> [order.userId, order.status] }
                .contains(listOf(it.id, it.status))
        }

    ProjectionCheckerPositiveOrder()
        .select()
        .where {
            it.status > some(
                ProjectionCheckerPositiveOrder()
                    .select { other -> other.status }
            )
        }

    ProjectionCheckerPositiveOrder()
        .select()
        .where {
            it.status > all(
                ProjectionCheckerPositiveOrder()
                    .select { other -> other.status }
            )
        }
}

fun validScalarAggregateAndLimitedCandidateForms() {
    ProjectionCheckerPositiveOrder()
        .select()
        .where {
            ProjectionCheckerPositiveOrder()
                .select { other -> f.max(other.status).alias("maxStatus") }
                .where { other -> other.userId == it.userId }
                .contains(it.status)
        }

    ProjectionCheckerPositiveUser()
        .select {
            [
                ProjectionCheckerPositiveOrder()
                    .select { order -> order.amount }
                    .where { order -> order.userId == it.id }
                    .limit(1)
                    .alias("lastAmount")
            ]
        }

    ProjectionCheckerPositiveOrder()
        .select()
        .where {
            it.status > ProjectionCheckerPositiveOrder()
                .select { other -> f.abs(f.max(other.status)).alias("maxStatusAbs") }
                .where { other -> other.userId == it.userId }
        }

    ProjectionCheckerPositiveOrder()
        .select()
        .where {
            it.status > ProjectionCheckerPositiveOrder()
                .select { other -> f.add(f.max(other.status), 1).alias("maxStatusPlusOne") }
                .where { other -> other.userId == it.userId }
        }
}

fun validPlainInsertIsNotCheckedAsInsertSelect() {
    ProjectionCheckerPositiveUser(name = "Ada", status = 1)
        .insert()
}
