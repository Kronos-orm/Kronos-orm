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

// Verifies insert-select explicit value types match the target insertable field order.

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_insert_type_source")
data class ProjectionInsertTypeSource(
    var id: Int? = null,
    var username: String? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_projection_insert_type_target")
data class ProjectionInsertTypeTarget(
    @PrimaryKey
    var id: Int? = null,
    var status: Int? = null,
    var username: String? = null,
) : KPojo

fun invalidExplicitValueTypeMismatch() {
    ProjectionInsertTypeSource()
        .select { [it.id, it.username, it.status] }
        .insert<ProjectionInsertTypeTarget> {
            [it.id, <!KRONOS_INSERT_SELECT_VALUE_TYPE_MISMATCH!>it.username<!>, it.username]
        }
}

fun validExplicitValueTypesAndNull() {
    ProjectionInsertTypeSource()
        .select { [it.id, it.username, it.status] }
        .insert<ProjectionInsertTypeTarget> {
            [it.id, null, it.username]
        }
}
