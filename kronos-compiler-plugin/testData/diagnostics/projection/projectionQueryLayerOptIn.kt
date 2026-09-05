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

// Verifies nested, derived, and union query layers share the standard duplicate-name opt-in rule.

import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UnsafeProjectionOverride
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union

@Table("tb_projection_query_layer")
data class ProjectionQueryLayerUser(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

fun invalidNestedProjection() {
    ProjectionQueryLayerUser().select { outer ->
        ProjectionQueryLayerUser().select { inner ->
            [inner.id, inner.<!OPT_IN_USAGE_ERROR!>id<!>]
        }
        outer.id
    }
}

fun invalidDerivedProjection() {
    val source = ProjectionQueryLayerUser().select {
        [it.id.alias("uid"), it.username]
    }

    source.select { [it.uid, it.<!OPT_IN_USAGE_ERROR!>uid<!>] }
}

fun invalidUnionProjection() {
    val source = union(
        ProjectionQueryLayerUser().select { [it.id, it.username] },
        ProjectionQueryLayerUser().select { [it.id, it.username] },
    )

    source.select { [it.id, it.<!OPT_IN_USAGE_ERROR!>id<!>] }
}

@OptIn(UnsafeProjectionOverride::class)
fun optedInQueryLayerDuplicates() {
    val derived = ProjectionQueryLayerUser()
        .select { [it.id.alias("uid"), it.username] }
        .select { [it.uid, it.uid] }
    val unionSource = union(
        ProjectionQueryLayerUser().select { [it.id, it.username] },
        ProjectionQueryLayerUser().select { [it.id, it.username] },
    )

    derived.select { [it.uid, it.uid] }
    unionSource.select { [it.id, it.id] }
}

fun distinctQueryLayerProjectionsDoNotRequireOptIn() {
    val derived = ProjectionQueryLayerUser().select {
        [it.id.alias("uid"), it.username]
    }
    val unionSource = union(
        ProjectionQueryLayerUser().select { [it.id, it.username] },
        ProjectionQueryLayerUser().select { [it.id, it.username] },
    )

    derived.select { [it.uid, it.username] }
    unionSource.select { [it.id, it.username] }
}
