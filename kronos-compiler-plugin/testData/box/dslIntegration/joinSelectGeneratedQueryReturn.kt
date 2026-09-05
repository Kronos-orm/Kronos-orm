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

// Verifies helper-returned JOIN projections support property reads from every no-arg terminal.

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
        val listId: Int? = joinedProjectionQuery().toList().firstOrNull()?.id
        val firstName: String? = joinedProjectionQuery().first().userName
        val nullableName: String? = joinedProjectionQuery().firstOrNull()?.userName
        if (listId == -1 || firstName == "unreachable" || nullableName == "unreachable") {
            return "Fail: terminals unexpectedly evaluated as $listId/$firstName/$nullableName"
        }
    }

    return "OK"
}

private fun joinedProjectionQuery() = JoinProjectionUser()
    .join(JoinProjectionOrder()) { user, order ->
        leftJoin { user.id == order.userId }
            .select { [user.id, user.name.alias("userName")] }
    }
