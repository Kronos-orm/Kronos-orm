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

// Verifies duplicate Selected names accept standard opt-in at function, class, and property scopes.

import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UnsafeProjectionOverride
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_opt_in_scopes")
data class ProjectionOptInUser(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

@OptIn(UnsafeProjectionOverride::class)
class ProjectionOptInScopeHolder {
    fun classScope() {
        ProjectionOptInUser().select {
            [f.length(it.username).alias("username"), f.length(it.username).alias("username")]
        }
    }

    fun functionScope() {
        ProjectionOptInUser().select {
            [f.length(it.username).alias("username"), f.length(it.username).alias("username")]
        }
    }
}

fun propertyScope() {
    @OptIn(UnsafeProjectionOverride::class)
    val query = ProjectionOptInUser().select {
        [f.length(it.username).alias("username"), f.length(it.username).alias("username")]
    }
}

fun sourceMinusThenRestore() {
    ProjectionOptInUser().select { [it - it.username, f.length(it.username).alias("username")] }
}

fun duplicateRequiresOptIn() {
    ProjectionOptInUser().select {
        [
            f.length(it.username).alias("username"),
            f.length(it.username).<!OPT_IN_USAGE_ERROR!>alias<!>("username"),
        ]
    }
}
