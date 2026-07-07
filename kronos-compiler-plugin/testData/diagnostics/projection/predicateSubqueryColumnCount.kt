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

// Verifies predicate subqueries match field and tuple arity.

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_predicate_user")
data class ProjectionPredicateUser(
    var id: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_projection_predicate_order")
data class ProjectionPredicateOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
    var status: Int? = null,
) : KPojo

fun invalidFieldInMultipleColumnSubquery() {
    ProjectionPredicateUser()
        .select()
        .where {
            it.id in <!KRONOS_PREDICATE_SUBQUERY_COLUMN_COUNT_MISMATCH!>ProjectionPredicateOrder()
                .select { order -> [order.userId, order.status] }<!>
        }
}

fun invalidTupleInMismatchedSubquery() {
    ProjectionPredicateUser()
        .select()
        .where {
            [it.id, it.status] in <!KRONOS_PREDICATE_SUBQUERY_COLUMN_COUNT_MISMATCH!>ProjectionPredicateOrder()
                .select { order -> order.userId }<!>
        }
}

fun invalidSingleElementTupleInSubquery() {
    ProjectionPredicateUser()
        .select()
        .where {
            <!KRONOS_ROW_VALUE_TUPLE_REQUIRES_MULTIPLE_FIELDS!>[it.id]<!> in ProjectionPredicateOrder()
                .select { order -> order.userId }
        }
}

fun invalidAnyMultipleColumnSubquery() {
    ProjectionPredicateOrder()
        .select()
        .where {
            it.status > any(
                <!KRONOS_PREDICATE_SUBQUERY_COLUMN_COUNT_MISMATCH!>ProjectionPredicateOrder()
                    .select { other -> [other.status, other.amount] }<!>
            )
        }
}

fun validPredicateSubqueries() {
    ProjectionPredicateUser()
        .select()
        .where {
            it.id in ProjectionPredicateOrder()
                .select { order -> order.userId }
        }

    ProjectionPredicateUser()
        .select()
        .where {
            [it.id, it.status] in ProjectionPredicateOrder()
                .select { order -> [order.userId, order.status] }
        }

    ProjectionPredicateOrder()
        .select()
        .where {
            it.status > any(
                ProjectionPredicateOrder()
                    .select { other -> other.status }
            )
        }
}
