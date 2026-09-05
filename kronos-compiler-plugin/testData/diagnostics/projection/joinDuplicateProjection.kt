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

// Verifies join fields use the same duplicate Selected opt-in rule as ordinary select.

import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UnsafeProjectionOverride
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join

@Table("tb_projection_join_user")
data class ProjectionJoinUser(
    var id: Int? = null,
    var companyId: Int? = null,
) : KPojo

@Table("tb_projection_join_company")
data class ProjectionJoinCompany(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

fun invalidJoinDuplicateProjection() {
    ProjectionJoinUser().join(ProjectionJoinCompany()) { user, company ->
        leftJoin { user.companyId == company.id }
            .select { [user.id, company.<!OPT_IN_USAGE_ERROR!>id<!>] }
    }
}

@OptIn(UnsafeProjectionOverride::class)
fun optedInJoinDuplicateProjection() {
    ProjectionJoinUser().join(ProjectionJoinCompany()) { user, company ->
        leftJoin { user.companyId == company.id }
            .select { [user.id, company.id] }
    }
}

fun distinctJoinProjectionDoesNotRequireOptIn() {
    ProjectionJoinUser().join(ProjectionJoinCompany()) { user, company ->
        leftJoin { user.companyId == company.id }
            .select { [user.id, company.name] }
    }
}
