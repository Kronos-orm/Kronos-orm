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

package com.kotlinorm.orm.update

import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.where
import com.kotlinorm.beans.subquery.SubqueryOrder
import com.kotlinorm.beans.subquery.SubqueryUser
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateSubquerySqlTest : MysqlTestBase() {
    @Test
    fun `where supports field in and not in selectable subquery`() {
        val (inSql, inParams) = SubqueryUser(name = "active")
            .update { [it.name] }
            .where {
                it.id in SubqueryOrder()
                    .select { order -> order.userId }
                    .where { order -> order.status == 1 }
            }
            .build()

        val (notInSql, notInParams) = SubqueryUser(name = "inactive")
            .update { [it.name] }
            .where {
                it.id !in SubqueryOrder()
                    .select { order -> order.userId }
                    .where { order -> order.status == 2 }
            }
            .build()

        assertEquals(
            "UPDATE `tb_subquery_user` SET `name` = :nameNew WHERE `tb_subquery_user`.`id` IN (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            inSql
        )
        assertEquals(mapOf("status" to 1, "nameNew" to "active"), inParams)
        assertEquals(
            "UPDATE `tb_subquery_user` SET `name` = :nameNew WHERE `tb_subquery_user`.`id` NOT IN (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            notInSql
        )
        assertEquals(mapOf("status" to 2, "nameNew" to "inactive"), notInParams)
    }

    @Test
    fun `where supports row tuple in and not in selectable subquery`() {
        val (inSql, inParams) = SubqueryOrder(status = 30)
            .update { [it.status] }
            .where {
                [it.userId, it.status] in SubqueryOrder()
                    .select { order -> [order.userId, order.status] }
                    .where { order -> order.status == 21 }
            }
            .build()

        val (notInSql, notInParams) = SubqueryOrder(status = 31)
            .update { [it.status] }
            .where {
                [it.userId, it.status] !in SubqueryOrder()
                    .select { order -> [order.userId, order.status] }
                    .where { order -> order.status == 22 }
            }
            .build()

        assertEquals(
            "UPDATE `tb_subquery_order` SET `status` = :statusNew WHERE (`tb_subquery_order`.`user_id`, `tb_subquery_order`.`status`) IN (SELECT `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            inSql
        )
        assertEquals(mapOf("status" to 21, "statusNew" to 30), inParams)
        assertEquals(
            "UPDATE `tb_subquery_order` SET `status` = :statusNew WHERE (`tb_subquery_order`.`user_id`, `tb_subquery_order`.`status`) NOT IN (SELECT `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            notInSql
        )
        assertEquals(mapOf("status" to 22, "statusNew" to 31), notInParams)
    }

    @Test
    fun `where subquery parameter suffixes use transformer safe conversion`() {
        val (sql, params) = SubqueryOrder()
            .update()
            .patch("status" to "38")
            .where {
                (it.status == 39) &&
                        (it.userId in SubqueryOrder()
                            .select { order -> order.userId }
                            .where { order -> order.status == 40 })
            }
            .build()

        assertEquals(
            "UPDATE `tb_subquery_order` SET `status` = :statusNew WHERE `tb_subquery_order`.`status` = :status AND `tb_subquery_order`.`user_id` IN (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status@1)",
            sql
        )
        assertEquals(mapOf("status" to 39, "status@1" to 40, "statusNew" to 38), params)
    }

    @Test
    fun `where supports scalar subquery comparison`() {
        val (sql, params) = SubqueryOrder(status = 32)
            .update { [it.status] }
            .where {
                it.status > SubqueryOrder()
                    .select { order -> order.status }
                    .where { order -> order.userId == 25 }
                    .limit(1)
            }
            .build()

        assertEquals(
            "UPDATE `tb_subquery_order` SET `status` = :statusNew WHERE `tb_subquery_order`.`status` > (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`user_id` = :userId LIMIT 1)",
            sql
        )
        assertEquals(mapOf("userId" to 25, "statusNew" to 32), params)
    }

    @Test
    fun `where supports quantified subquery comparison`() {
        val (sql, params) = SubqueryOrder(status = 33)
            .update { [it.status] }
            .where {
                it.status > any<Int>(
                    SubqueryOrder()
                        .select { order -> order.status }
                        .where { order -> order.userId == 27 }
                )
            }
            .build()

        assertEquals(
            "UPDATE `tb_subquery_order` SET `status` = :statusNew WHERE `tb_subquery_order`.`status` > ANY (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`user_id` = :userId)",
            sql
        )
        assertEquals(mapOf("userId" to 27, "statusNew" to 33), params)
    }

    @Test
    fun `where supports exists and not exists selectable subquery`() {
        val (existsSql, existsParams) = SubqueryUser(name = "active")
            .update { [it.name] }
            .where {
                exists(
                    SubqueryOrder()
                        .select()
                        .where { order -> order.status == 9 }
                )
            }
            .build()

        val (notExistsSql, notExistsParams) = SubqueryUser(name = "inactive")
            .update { [it.name] }
            .where {
                !exists(
                    SubqueryOrder()
                        .select()
                        .where { order -> order.status == 10 }
                )
            }
            .build()

        assertEquals(
            "UPDATE `tb_subquery_user` SET `name` = :nameNew WHERE EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            existsSql
        )
        assertEquals(mapOf("status" to 9, "nameNew" to "active"), existsParams)
        assertEquals(
            "UPDATE `tb_subquery_user` SET `name` = :nameNew WHERE NOT EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            notExistsSql
        )
        assertEquals(mapOf("status" to 10, "nameNew" to "inactive"), notExistsParams)
    }

    @Test
    fun `patch supports scalar subquery assignment`() {
        val (sql, params) = SubqueryUser()
            .update()
            .patch(
                "name" to SubqueryOrder()
                    .select { it.status }
                    .where { it.status == 14 }
                    .limit(1)
            )
            .where { it.id == 1 }
            .build()

        assertEquals(
            "UPDATE `tb_subquery_user` SET `name` = (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status LIMIT 1) WHERE `tb_subquery_user`.`id` = :id",
            sql
        )
        assertEquals(mapOf("status" to 14, "id" to 1), params)
    }

    @Test
    fun `set block supports scalar subquery assignment`() {
        val (sql, params) = SubqueryUser()
            .update()
            .set { user ->
                setValue(
                    user.__columns.single { it.name == "name" },
                    SubqueryOrder()
                        .select { it.status }
                        .where { it.status == 18 }
                        .limit(1)
                )
            }
            .where { it.id == 2 }
            .build()

        assertEquals(
            "UPDATE `tb_subquery_user` SET `name` = (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status LIMIT 1) WHERE `tb_subquery_user`.`id` = :id",
            sql
        )
        assertEquals(mapOf("status" to 18, "id" to 2), params)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    @Test
    fun `set assignment scalar subquery type hint renders sql`() {
        val (sql, params) = SubqueryOrder()
            .update()
            .set {
                it.status = (SubqueryOrder()
                    .select { order -> order.status }
                    .where { order -> order.userId == 44 }
                    .limit(1) as Int?)
            }
            .where { it.id == 3 }
            .build()

        assertEquals(
            "UPDATE `tb_subquery_order` SET `status` = (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`user_id` = :userId LIMIT 1) WHERE `tb_subquery_order`.`id` = :id",
            sql
        )
        assertEquals(mapOf("userId" to 44, "id" to 3), params)
    }
}
