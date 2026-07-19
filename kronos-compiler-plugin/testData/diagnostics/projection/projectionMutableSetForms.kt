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

// Verifies duplicate-name opt-in diagnostics inspect mutableListOf and setOf projection forms.

import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_mutable_set_diag_user")
data class ProjectionMutableSetDiagUser(
    var id: Int? = null,
    var username: String? = null,
    var status: Int? = null,
) : KPojo

fun invalidDuplicateInMutableListProjection() {
    ProjectionMutableSetDiagUser()
        .select {
            mutableListOf(it.id, it.<!OPT_IN_USAGE_ERROR!>id<!>)
        }
}

fun invalidFunctionWithoutAliasInSetProjection() {
    ProjectionMutableSetDiagUser()
        .select {
            setOf<Any?>(it.id, <!KRONOS_SELECT_ITEM_REQUIRES_ALIAS!>f.length(it.username)<!>)
        }
}

fun invalidAliasConflictInMutableListProjection() {
    ProjectionMutableSetDiagUser()
        .select {
            mutableListOf(it.id, f.length(it.username).alias("username"))
        }
}
