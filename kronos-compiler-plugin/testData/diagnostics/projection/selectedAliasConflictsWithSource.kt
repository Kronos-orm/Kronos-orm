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

// Verifies a selected alias may shadow a Source name until the conflicting Context field is used.

import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UnsafeProjectionOverride
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select

@Table("tb_projection_diag_user")
data class ProjectionDiagUser(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

@Table("tb_projection_diag_order")
data class ProjectionDiagOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var username: String? = null,
) : KPojo

fun invalidContextConflict() {
    ProjectionDiagUser()
        .select { [it.id, f.length(it.username).alias("username")] }
        .orderBy { it.<!OPT_IN_USAGE_ERROR!>username<!>.desc() }
}

fun harmlessSelectedShadow() {
    ProjectionDiagUser()
        .select { [it.id, f.length(it.username).alias("username")] }
        .where { it.username == "source-value" }
        .groupBy { it.username }
        .having { it.username != null }
}

fun unshadowedContextReadDoesNotRequireOptIn() {
    ProjectionDiagUser()
        .select { [it.id, f.length(it.username).alias("usernameLength")] }
        .orderBy { [it.username.asc(), it.usernameLength.desc()] }
}

@OptIn(UnsafeProjectionOverride::class)
fun optedInContextConflict() {
    ProjectionDiagUser()
        .select { [it.id, f.length(it.username).alias("username")] }
        .orderBy { it.username.desc() }
}

fun expressionOptedInContextConflict() {
    @Suppress("UNUSED_VARIABLE")
    val query = @OptIn(UnsafeProjectionOverride::class) ProjectionDiagUser()
        .select { [it.id, f.length(it.username).alias("username")] }
        .orderBy { it.username.desc() }
}

fun invalidJoinContextConflict() {
    ProjectionDiagUser().join(ProjectionDiagOrder()) { user, order ->
        innerJoin { user.id == order.userId }
            .select { [user.id, f.length(order.username).alias("username")] }
            .orderBy { [it.id.asc(), it.<!OPT_IN_USAGE_ERROR!>username<!>.desc()] }
    }
}

@OptIn(UnsafeProjectionOverride::class)
fun optedInJoinContextConflict() {
    ProjectionDiagUser().join(ProjectionDiagOrder()) { user, order ->
        innerJoin { user.id == order.userId }
            .select { [user.id, f.length(order.username).alias("username")] }
            .orderBy { [it.id.asc(), it.username.desc()] }
    }
}

fun unshadowedJoinContextReadDoesNotRequireOptIn() {
    ProjectionDiagUser().join(ProjectionDiagOrder()) { user, order ->
        innerJoin { user.id == order.userId }
            .select { [user.id, f.length(order.username).alias("usernameLength")] }
            .orderBy { [it.username.asc(), it.usernameLength.desc()] }
    }
}
