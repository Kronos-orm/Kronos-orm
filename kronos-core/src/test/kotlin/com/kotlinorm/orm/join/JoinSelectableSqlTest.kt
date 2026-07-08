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

package com.kotlinorm.orm.join

import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.where
import com.kotlinorm.beans.subquery.SubqueryOrder
import com.kotlinorm.beans.subquery.SubqueryUser
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class JoinSelectableSqlTest : MysqlTestBase() {
    @Test
    fun `join selectable source renders derived table sql and params`() {
        val orders = SubqueryOrder()
            .select { [it.userId, it.status] }
            .where { it.status == 40 }

        val task = SubqueryUser()
            .join(orders) { user, order ->
                leftJoin(order) { user.id == order.userId }
                select { [user.id, order.status] }
            }
            .build()
            .atomicTask

        assertEquals(
            "SELECT `tb_subquery_user`.`id` AS `id`, `q`.`status` AS `status` FROM `tb_subquery_user` LEFT JOIN (SELECT `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status) AS `q` ON `tb_subquery_user`.`id` = `q`.`userId`",
            task.sql
        )
        assertEquals(mapOf("status" to 40), task.paramMap)
    }
}
