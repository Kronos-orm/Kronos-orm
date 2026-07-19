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

// Verifies offset-page Selected shapes participate in scalar and predicate diagnostics.

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_offset_page_user")
data class ProjectionOffsetPageUser(
    var id: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_projection_offset_page_order")
data class ProjectionOffsetPageOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun invalidOffsetPageScalarWithoutExplicitLimit() {
    val page = ProjectionOffsetPageOrder()
        .select { it.status }
        .page(pageIndex = 1, pageSize = 1)

    ProjectionOffsetPageUser().select {
        [
            it.id,
            <!KRONOS_SCALAR_SUBQUERY_REQUIRES_LIMIT!>page<!>.alias("pagedStatus"),
        ]
    }
}

fun invalidOffsetPageScalarWithMultipleColumns() {
    val page = ProjectionOffsetPageOrder()
        .select { [it.userId, it.status] }
        .page(pageIndex = 1, pageSize = 1)

    ProjectionOffsetPageUser().select {
        [
            it.id,
            <!KRONOS_SCALAR_SUBQUERY_REQUIRES_SINGLE_COLUMN!>page<!>.alias("pagedStatus"),
        ]
    }
}

fun validOffsetPagePredicateWithMatchingArity() {
    val page = ProjectionOffsetPageOrder()
        .select { it.userId }
        .page(pageIndex = 1, pageSize = 10)

    ProjectionOffsetPageUser()
        .select()
        .where { it.id in page }
}

fun invalidOffsetPagePredicateWithMismatchedArity() {
    val page = ProjectionOffsetPageOrder()
        .select { [it.userId, it.status] }
        .page(pageIndex = 1, pageSize = 10)

    ProjectionOffsetPageUser()
        .select()
        .where { it.id in <!KRONOS_PREDICATE_SUBQUERY_COLUMN_COUNT_MISMATCH!>page<!> }
}
