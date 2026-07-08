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

// Verifies union results expose the first branch generated Selected type as a next-layer source.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union

@Table("tb_union_projection_order")
data class UnionProjectionOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {}

    if (System.currentTimeMillis() < 0) {
        val source = union(
            UnionProjectionOrder().select { [it.id, it.userId.alias("uid")] },
            UnionProjectionOrder().select { [it.id, it.userId.alias("uid")] }
        )
        val rows = source
            .select { [it.id, it.uid] }
            .where { it.uid == 7 }
            .queryList()
        val id: Int? = rows.firstOrNull()?.id
        val uid: Int? = rows.firstOrNull()?.uid
        if (id == -1 || uid == -1) return "Fail: unreachable"
    }

    return "OK"
}
