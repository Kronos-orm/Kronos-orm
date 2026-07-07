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

// Verifies same-layer having uses Source and cannot access aggregate aliases.

import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_diag_order")
data class ProjectionDiagOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

fun invalidHavingAlias() {
    ProjectionDiagOrder()
        .select { [it.userId, f.count(it.id).alias("orderCount")] }
        .groupBy { it.userId }
        .having { it.<!UNRESOLVED_REFERENCE!>orderCount<!> > 1 }
}
