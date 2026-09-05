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

// Verifies non-identity primary keys stay insertable while identity primary keys are filtered out.

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

data class ProjectionInsertPrimaryKeySource(
    var id: Int? = null,
    var code: String? = null,
    var status: Int? = null,
) : KPojo

data class ProjectionInsertPrimaryKeyTarget(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @PrimaryKey(identity = false)
    var code: String? = null,
    var status: Int? = null,
) : KPojo

fun validNonIdentityPrimaryKeyIsInsertable() {
    ProjectionInsertPrimaryKeySource()
        .select { [it.id, it.code, it.status] }
        .insert<ProjectionInsertPrimaryKeyTarget> {
            [it.code, it.status]
        }
}

fun invalidPrimaryKeyFilteringCountMismatch() {
    ProjectionInsertPrimaryKeySource()
        .select { [it.id, it.code, it.status] }
        .insert<ProjectionInsertPrimaryKeyTarget> {
            <!KRONOS_INSERT_SELECT_VALUE_COUNT_MISMATCH!>[it.status]<!>
        }
}

fun invalidPrimaryKeyFilteringTypeMismatch() {
    ProjectionInsertPrimaryKeySource()
        .select { [it.id, it.code, it.status] }
        .insert<ProjectionInsertPrimaryKeyTarget> {
            [<!KRONOS_INSERT_SELECT_VALUE_TYPE_MISMATCH!>it.status<!>, it.status]
        }
}
