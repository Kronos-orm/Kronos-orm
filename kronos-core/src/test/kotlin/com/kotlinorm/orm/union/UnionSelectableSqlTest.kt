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

package com.kotlinorm.orm.union

import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.where
import com.kotlinorm.beans.subquery.SubqueryOrder
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class UnionSelectableSqlTest : MysqlTestBase() {
    @Test
    fun `union result can be selected as next layer source`() {
        val source = union(
            SubqueryOrder()
                .select()
                .where { it.status == 41 },
            SubqueryOrder()
                .select()
                .where { it.status == 42 }
        )

        val (sql, params) = source
            .select()
            .where { it.status == 43 }
            .build()

        assertEquals(
            "SELECT `q`.`id`, `q`.`userId`, `q`.`status` FROM ((SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status@1) UNION (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status@2)) AS `q` WHERE `q`.`status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 43, "status@1" to 41, "status@2" to 42), params)
    }
}
