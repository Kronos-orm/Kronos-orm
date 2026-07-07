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

// Verifies projection diagnostics also inspect listOf/arrayOf projection forms.

import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_collection_diag_user")
data class ProjectionCollectionDiagUser(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

fun invalidDuplicateInListProjection() {
    ProjectionCollectionDiagUser()
        .select { listOf(it.id, <!KRONOS_DUPLICATE_PROJECTION_FIELD!>it.id<!>) }
}

fun invalidFunctionWithoutAliasInArrayProjection() {
    ProjectionCollectionDiagUser()
        .select { arrayOf<Any?>(it.id, <!KRONOS_SELECT_ITEM_REQUIRES_ALIAS!>f.length(it.username)<!>) }
}

fun invalidAliasConflictInListProjection() {
    ProjectionCollectionDiagUser()
        .select { listOf(it.id, <!KRONOS_SELECTED_FIELD_CONFLICTS_WITH_SOURCE!>f.length(it.username).alias("username")<!>) }
}
