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

// Verifies scalar subqueries used as values must declare limit(1).

import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.max
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_limit_user")
data class ProjectionLimitUser(
    var id: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_projection_limit_order")
data class ProjectionLimitOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
    var status: Int? = null,
) : KPojo

fun invalidScalarProjectionWithoutLimit() {
    ProjectionLimitUser()
        .select {
            [
                it.id,
                <!KRONOS_SCALAR_SUBQUERY_REQUIRES_LIMIT!>ProjectionLimitOrder()
                    .select { order -> order.amount }
                    .where { order -> order.userId == it.id }<!>
                    .alias("lastAmount")
            ]
        }
}

fun invalidScalarComparisonWithoutLimit() {
    ProjectionLimitOrder()
        .select()
        .where {
            it.status > <!KRONOS_SCALAR_SUBQUERY_REQUIRES_LIMIT!>ProjectionLimitOrder()
                .select { other -> other.status }
                .where { other -> other.userId == it.userId }<!>
        }
}

fun validScalarSubqueriesWithLimit() {
    ProjectionLimitUser()
        .select {
            [
                it.id,
                ProjectionLimitOrder()
                    .select { order -> order.amount }
                    .where { order -> order.userId == it.id }
                    .limit(1)
                    .alias("lastAmount")
            ]
        }

    ProjectionLimitOrder()
        .select()
        .where {
            it.status > ProjectionLimitOrder()
                .select { other -> other.status }
                .where { other -> other.userId == it.userId }
                .limit(1)
        }
}

fun validAggregateScalarSubqueryWithoutLimit() {
    ProjectionLimitOrder()
        .select()
        .where {
            it.status > ProjectionLimitOrder()
                .select { other -> f.max(other.status).alias("maxStatus") }
                .where { other -> other.userId == it.userId }
        }
}

fun invalidGroupedAggregateScalarSubqueryWithoutLimit() {
    ProjectionLimitOrder()
        .select()
        .where {
            it.status > <!KRONOS_SCALAR_SUBQUERY_REQUIRES_LIMIT!>ProjectionLimitOrder()
                .select { other -> f.max(other.status).alias("maxStatus") }
                .where { other -> other.userId == it.userId }
                .groupBy { other -> other.userId }<!>
        }
}
