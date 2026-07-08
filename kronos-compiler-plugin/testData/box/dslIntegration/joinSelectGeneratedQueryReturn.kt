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

// Verifies join select generated projections flow into no-arg queryList/queryOne return types.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join

@Table("tb_join_projection_user")
data class JoinProjectionUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_join_projection_order")
data class JoinProjectionOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {}

    if (System.currentTimeMillis() < 0) {
        val rows = JoinProjectionUser()
            .join(JoinProjectionOrder()) { user, order ->
                leftJoin(order) { user.id == order.userId }
            }
            .select { [it.id, it.name.alias("userName")] }
            .queryList()
        val id: Int? = rows.firstOrNull()?.id
        val userName: String? = rows.firstOrNull()?.userName
        if (id == -1 || userName == "unreachable") return "Fail: unreachable"
    }

    return "OK"
}
