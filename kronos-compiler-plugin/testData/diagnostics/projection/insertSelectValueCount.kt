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

// Verifies insert-select explicit value lists match the target insertable field count.

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_insert_source")
data class ProjectionInsertSource(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_projection_insert_target")
data class ProjectionInsertTarget(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_projection_insert_identity_target")
data class ProjectionInsertIdentityTarget(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun invalidExplicitValueCountMismatch() {
    ProjectionInsertSource()
        .select { [it.id, it.userId, it.status] }
        .insert<ProjectionInsertTarget> {
            <!KRONOS_INSERT_SELECT_VALUE_COUNT_MISMATCH!>[it.id, it.userId]<!>
        }
}

fun validExplicitValueCount() {
    ProjectionInsertSource()
        .select { [it.id, it.userId, it.status] }
        .insert<ProjectionInsertTarget> {
            [it.id, it.userId, it.status]
        }
}

fun validExplicitValueCountWithIdentityTarget() {
    ProjectionInsertSource()
        .select { [it.id, it.userId, it.status] }
        .insert<ProjectionInsertIdentityTarget> {
            [it.userId, it.status]
        }
}
