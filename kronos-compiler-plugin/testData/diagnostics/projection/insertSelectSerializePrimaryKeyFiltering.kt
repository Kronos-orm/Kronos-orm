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

// Verifies insert-select target filtering handles serialized primary-key combinations.

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

data class ProjectionInsertSerializedChild(
    var id: Int? = null,
    var label: String? = null,
) : KPojo

data class ProjectionInsertSerializedSource(
    var id: Int? = null,
    var code: String? = null,
    var payload: String? = null,
    var status: Int? = null,
) : KPojo

data class ProjectionInsertSerializedTarget(
    @Serialize
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Serialize
    @PrimaryKey(identity = false)
    var code: String? = null,
    @Serialize
    var payload: String? = null,
    var status: Int? = null,
    var nested: ProjectionInsertSerializedChild? = null,
    var nestedList: List<ProjectionInsertSerializedChild> = emptyList(),
) : KPojo

fun validSerializePrimaryKeyFilteringKeepsNonIdentitySerializedColumns() {
    ProjectionInsertSerializedSource()
        .select { [it.id, it.code, it.payload, it.status] }
        .insert<ProjectionInsertSerializedTarget> {
            [it.code, it.payload, it.status]
        }
}

fun invalidSerializePrimaryKeyFilteringCountMismatch() {
    ProjectionInsertSerializedSource()
        .select { [it.id, it.code, it.payload, it.status] }
        .insert<ProjectionInsertSerializedTarget> {
            <!KRONOS_INSERT_SELECT_VALUE_COUNT_MISMATCH!>[it.code, it.payload]<!>
        }
}

fun invalidSerializePrimaryKeyFilteringTypeMismatch() {
    ProjectionInsertSerializedSource()
        .select { [it.id, it.code, it.payload, it.status] }
        .insert<ProjectionInsertSerializedTarget> {
            [it.code, <!KRONOS_INSERT_SELECT_VALUE_TYPE_MISMATCH!>it.status<!>, it.status]
        }
}
