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

package com.kotlinorm.orm.delete

import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.where
import com.kotlinorm.beans.subquery.SubqueryLogicUser
import com.kotlinorm.beans.subquery.SubqueryOrder
import com.kotlinorm.beans.subquery.SubqueryUser
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteSubquerySqlTest : MysqlTestBase() {
    @Test
    fun `where supports field in and not in selectable subquery`() {
        val (inSql, inParams) = SubqueryUser()
            .delete()
            .logic(false)
            .where {
                it.id in SubqueryOrder()
                    .select { order -> order.userId }
                    .where { order -> order.status == 3 }
            }
            .build()

        val (notInSql, notInParams) = SubqueryUser()
            .delete()
            .logic(false)
            .where {
                it.id !in SubqueryOrder()
                    .select { order -> order.userId }
                    .where { order -> order.status == 4 }
            }
            .build()

        assertEquals(
            "DELETE FROM `tb_subquery_user` WHERE `tb_subquery_user`.`id` IN (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            inSql
        )
        assertEquals(mapOf("status" to 3), inParams)
        assertEquals(
            "DELETE FROM `tb_subquery_user` WHERE `tb_subquery_user`.`id` NOT IN (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            notInSql
        )
        assertEquals(mapOf("status" to 4), notInParams)
    }

    @Test
    fun `where supports row tuple in and not in selectable subquery`() {
        val (inSql, inParams) = SubqueryOrder()
            .delete()
            .logic(false)
            .where {
                [it.userId, it.status] in SubqueryOrder()
                    .select { order -> [order.userId, order.status] }
                    .where { order -> order.status == 23 }
            }
            .build()

        val (notInSql, notInParams) = SubqueryOrder()
            .delete()
            .logic(false)
            .where {
                [it.userId, it.status] !in SubqueryOrder()
                    .select { order -> [order.userId, order.status] }
                    .where { order -> order.status == 24 }
            }
            .build()

        assertEquals(
            "DELETE FROM `tb_subquery_order` WHERE (`tb_subquery_order`.`user_id`, `tb_subquery_order`.`status`) IN (SELECT `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            inSql
        )
        assertEquals(mapOf("status" to 23), inParams)
        assertEquals(
            "DELETE FROM `tb_subquery_order` WHERE (`tb_subquery_order`.`user_id`, `tb_subquery_order`.`status`) NOT IN (SELECT `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            notInSql
        )
        assertEquals(mapOf("status" to 24), notInParams)
    }

    @Test
    fun `where supports scalar subquery comparison`() {
        val (sql, params) = SubqueryOrder()
            .delete()
            .logic(false)
            .where {
                it.status > SubqueryOrder()
                    .select { order -> order.status }
                    .where { order -> order.userId == 26 }
                    .limit(1)
            }
            .build()

        assertEquals(
            "DELETE FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` > (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`user_id` = :userId LIMIT 1)",
            sql
        )
        assertEquals(mapOf("userId" to 26), params)
    }

    @Test
    fun `where supports quantified subquery comparison`() {
        val (sql, params) = SubqueryOrder()
            .delete()
            .logic(false)
            .where {
                it.status <= all<Int>(
                    SubqueryOrder()
                        .select { order -> order.status }
                        .where { order -> order.userId == 28 }
                )
            }
            .build()

        assertEquals(
            "DELETE FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` <= ALL (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`user_id` = :userId)",
            sql
        )
        assertEquals(mapOf("userId" to 28), params)
    }

    @Test
    fun `logic delete keeps subquery predicate and logic predicate`() {
        val (sql, params) = SubqueryLogicUser()
            .delete()
            .where {
                it.id in SubqueryOrder()
                    .select { order -> order.userId }
                    .where { order -> order.status == 5 }
            }
            .build()

        assertEquals(
            "UPDATE `tb_subquery_logic_user` SET `deleted` = :deletedNew WHERE `tb_subquery_logic_user`.`id` IN (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status) AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("status" to 5, "deletedNew" to 1), params)
    }

    @Test
    fun `where supports exists and not exists selectable subquery`() {
        val (existsSql, existsParams) = SubqueryUser()
            .delete()
            .logic(false)
            .where {
                exists(
                    SubqueryOrder()
                        .select()
                        .where { order -> order.status == 11 }
                )
            }
            .build()

        val (notExistsSql, notExistsParams) = SubqueryUser()
            .delete()
            .logic(false)
            .where {
                !exists(
                    SubqueryOrder()
                        .select()
                        .where { order -> order.status == 12 }
                )
            }
            .build()

        assertEquals(
            "DELETE FROM `tb_subquery_user` WHERE EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            existsSql
        )
        assertEquals(mapOf("status" to 11), existsParams)
        assertEquals(
            "DELETE FROM `tb_subquery_user` WHERE NOT EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            notExistsSql
        )
        assertEquals(mapOf("status" to 12), notExistsParams)
    }

    @Test
    fun `where subquery parameter suffixes use transformer safe conversion`() {
        val (sql, params) = SubqueryOrder()
            .delete()
            .logic(false)
            .where {
                (it.status == 41) &&
                        (it.userId in SubqueryOrder()
                            .select { order -> order.userId }
                            .where { order -> order.status == 42 })
            }
            .patch("status" to "41", "status@1" to "42")
            .build()

        assertEquals(
            "DELETE FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status AND `tb_subquery_order`.`user_id` IN (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status@1)",
            sql
        )
        assertEquals(mapOf("status" to 41, "status@1" to 42), params)
    }
}
