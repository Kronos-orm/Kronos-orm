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

// Verifies scalar subqueries used as values must project exactly one item.

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_scalar_shape_user")
data class ProjectionScalarShapeUser(
    var id: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_projection_scalar_shape_order")
data class ProjectionScalarShapeOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
    var status: Int? = null,
) : KPojo

fun invalidScalarProjectionMultipleColumns() {
    ProjectionScalarShapeUser()
        .select {
            [
                it.id,
                <!KRONOS_SCALAR_SUBQUERY_REQUIRES_SINGLE_COLUMN!>ProjectionScalarShapeOrder()
                    .select { order -> [order.amount, order.status] }
                    .where { order -> order.userId == it.id }
                    .limit(1)<!>
                    .alias("lastAmount")
            ]
        }
}

fun invalidScalarComparisonMultipleColumns() {
    ProjectionScalarShapeOrder()
        .select()
        .where {
            it.status > <!KRONOS_SCALAR_SUBQUERY_REQUIRES_SINGLE_COLUMN!>ProjectionScalarShapeOrder()
                .select { other -> [other.status, other.amount] }
                .where { other -> other.userId == it.userId }
                .limit(1)<!>
        }
}

fun validScalarSubquerySingleColumn() {
    ProjectionScalarShapeOrder()
        .select()
        .where {
            it.status > ProjectionScalarShapeOrder()
                .select { other -> other.status }
                .where { other -> other.userId == it.userId }
                .limit(1)
        }
}
