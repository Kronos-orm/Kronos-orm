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

// Verifies scalar subquery limit diagnostics require limit(1), not just any limit call.

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_limit_one_user")
data class ProjectionLimitOneUser(
    var id: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_projection_limit_one_order")
data class ProjectionLimitOneOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun invalidScalarComparisonWithLimitOtherThanOne() {
    ProjectionLimitOneUser()
        .select()
        .where {
            it.status > <!KRONOS_SCALAR_SUBQUERY_REQUIRES_LIMIT!>ProjectionLimitOneOrder()
                .select { other -> other.status }
                .where { other -> other.userId == it.id }
                .limit(2)<!>
        }
}

fun validScalarComparisonWithLimitOne() {
    ProjectionLimitOneUser()
        .select()
        .where {
            it.status > ProjectionLimitOneOrder()
                .select { other -> other.status }
                .where { other -> other.userId == it.id }
                .limit(1)
        }
}
