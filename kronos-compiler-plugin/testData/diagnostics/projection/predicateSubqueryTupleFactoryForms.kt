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

// Verifies predicate subquery tuple diagnostics inspect listOf tuple forms.

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_tuple_factory_user")
data class ProjectionTupleFactoryUser(
    var id: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_projection_tuple_factory_order")
data class ProjectionTupleFactoryOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun validListTuplePredicateSubquery() {
    ProjectionTupleFactoryUser()
        .select()
        .where {
            listOf(it.id, it.status) in ProjectionTupleFactoryOrder()
                .select { order -> [order.userId, order.status] }
        }
}

fun invalidSingleElementListTuplePredicateSubquery() {
    ProjectionTupleFactoryUser()
        .select()
        .where {
            <!KRONOS_ROW_VALUE_TUPLE_REQUIRES_MULTIPLE_FIELDS!>listOf(it.id)<!> in ProjectionTupleFactoryOrder()
                .select { order -> order.userId }
        }
}

fun invalidListTuplePredicateSubqueryColumnCount() {
    ProjectionTupleFactoryUser()
        .select()
        .where {
            listOf(it.id, it.status) in <!KRONOS_PREDICATE_SUBQUERY_COLUMN_COUNT_MISMATCH!>ProjectionTupleFactoryOrder()
                .select { order -> order.userId }<!>
        }
}
