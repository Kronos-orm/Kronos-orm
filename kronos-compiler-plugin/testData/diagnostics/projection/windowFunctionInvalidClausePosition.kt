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

// Verifies window OVER is only exposed in select/orderBy-capable DSL scopes.

import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_window_position_order")
data class ProjectionWindowPositionOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun invalidWhereWindowExpression() {
    ProjectionWindowPositionOrder()
        .select()
        .where {
            f.rowNumber()
                .<!UNRESOLVED_REFERENCE!>over<!> {
                    <!UNRESOLVED_REFERENCE!>partitionBy<!>(it.userId)
                    <!UNRESOLVED_REFERENCE!>orderBy<!>(it.status.<!UNRESOLVED_REFERENCE!>asc<!>())
                } == 1
        }
}
