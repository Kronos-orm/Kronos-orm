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

// Verifies insert-select target fields exclude identity, ignored, cascade, and nested KPojo properties.

import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

data class ProjectionInsertFilteredChild(
    var id: Int? = null,
    var label: String? = null,
) : KPojo

data class ProjectionInsertFilteredSource(
    var id: Int? = null,
    var userId: Int? = null,
    var payload: String? = null,
    var status: Int? = null,
) : KPojo

data class ProjectionInsertFilteredTarget(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var userId: Int? = null,
    @Serialize
    var payload: String? = null,
    var status: Int? = null,
    @Ignore
    var ignored: String? = null,
    @Cascade(["id"], ["userId"])
    var child: ProjectionInsertFilteredChild? = null,
    var nested: ProjectionInsertFilteredChild? = null,
    var children: List<ProjectionInsertFilteredChild> = emptyList(),
) : KPojo

fun validTargetFilteringKeepsOnlyInsertableColumns() {
    ProjectionInsertFilteredSource()
        .select { [it.id, it.userId, it.payload, it.status] }
        .insert<ProjectionInsertFilteredTarget> {
            [it.userId, it.payload, it.status]
        }
}

fun invalidTargetFilteringCountMismatch() {
    ProjectionInsertFilteredSource()
        .select { [it.id, it.userId, it.payload, it.status] }
        .insert<ProjectionInsertFilteredTarget> {
            <!KRONOS_INSERT_SELECT_VALUE_COUNT_MISMATCH!>[it.userId, it.payload]<!>
        }
}

fun invalidTargetFilteringTypeMismatch() {
    ProjectionInsertFilteredSource()
        .select { [it.id, it.userId, it.payload, it.status] }
        .insert<ProjectionInsertFilteredTarget> {
            [it.userId, <!KRONOS_INSERT_SELECT_VALUE_TYPE_MISMATCH!>it.status<!>, it.status]
        }
}
