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

// Verifies projection diagnostics still run when the FIR bridge uses IDEA-active projection lookup tags.

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_diag_ide_user")
data class ProjectionDiagIdeUser(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

fun invalidDuplicateProjectionIdeActive() {
    ProjectionDiagIdeUser()
        .<!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>select<!> { [it.id, <!KRONOS_DUPLICATE_PROJECTION_FIELD!>it.id<!>] }
}
