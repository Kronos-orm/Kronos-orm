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

// Verifies same-layer where/groupBy/having use Source and cannot access current window aliases.

import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_window_diag_order")
data class ProjectionWindowDiagOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun invalidWindowWhereAlias() {
    ProjectionWindowDiagOrder()
        .select {
            [
                it.id,
                f.rowNumber()
                    .over {
                        partitionBy(it.userId)
                        orderBy(it.status.asc())
                    }
                    .alias("rn")
            ]
        }
        .where { it.<!UNRESOLVED_REFERENCE!>rn<!> == 1 }
}

fun invalidWindowGroupByAlias() {
    ProjectionWindowDiagOrder()
        .select {
            [
                it.id,
                f.rowNumber()
                    .over {
                        partitionBy(it.userId)
                        orderBy(it.status.asc())
                    }
                    .alias("rn")
            ]
        }
        .groupBy { <!CANNOT_INFER_PARAMETER_TYPE!>[it.<!UNRESOLVED_REFERENCE!>rn<!>]<!> }
}

fun invalidWindowHavingAlias() {
    ProjectionWindowDiagOrder()
        .select {
            [
                it.id,
                f.rowNumber()
                    .over {
                        partitionBy(it.userId)
                        orderBy(it.status.asc())
                    }
                    .alias("rn")
            ]
        }
        .having { it.<!UNRESOLVED_REFERENCE!>rn<!> == 1 }
}
