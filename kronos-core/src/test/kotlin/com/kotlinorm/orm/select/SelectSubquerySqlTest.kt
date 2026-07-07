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

package com.kotlinorm.orm.select

import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.beans.subquery.SubqueryOrder
import com.kotlinorm.beans.subquery.SubqueryUser
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectSubquerySqlTest : MysqlTestBase() {
    @Test
    fun `where supports field in and not in selectable subquery`() {
        val (inSql, inParams) = SubqueryUser()
            .select()
            .where {
                it.id in SubqueryOrder()
                    .select { order -> order.userId }
                    .where { order -> order.status == 1 }
            }
            .build()

        val (notInSql, notInParams) = SubqueryUser()
            .select()
            .where {
                it.id !in SubqueryOrder()
                    .select { order -> order.userId }
                    .where { order -> order.status == 2 }
            }
            .build()

        assertEquals(
            "SELECT `id`, `name` FROM `tb_subquery_user` WHERE `tb_subquery_user`.`id` IN (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            inSql
        )
        assertEquals(mapOf("status" to 1), inParams)
        assertEquals(
            "SELECT `id`, `name` FROM `tb_subquery_user` WHERE `tb_subquery_user`.`id` NOT IN (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            notInSql
        )
        assertEquals(mapOf("status" to 2), notInParams)
    }

    @Test
    fun `where supports row tuple in and not in selectable subquery`() {
        val (sql, params) = SubqueryOrder()
            .select()
            .where {
                [it.userId, it.status] in SubqueryOrder()
                    .select { order -> [order.userId, order.status] }
                    .where { order -> order.status == 17 }
            }
            .build()

        val (notInSql, notInParams) = SubqueryOrder()
            .select()
            .where {
                [it.userId, it.status] !in SubqueryOrder()
                    .select { order -> [order.userId, order.status] }
                    .where { order -> order.status == 20 }
            }
            .build()

        assertEquals(
            "SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE (`tb_subquery_order`.`user_id`, `tb_subquery_order`.`status`) IN (SELECT `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            sql
        )
        assertEquals(mapOf("status" to 17), params)
        assertEquals(
            "SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE (`tb_subquery_order`.`user_id`, `tb_subquery_order`.`status`) NOT IN (SELECT `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            notInSql
        )
        assertEquals(mapOf("status" to 20), notInParams)
    }

    @Test
    fun `where subquery parameter suffixes use transformer safe conversion`() {
        val (sql, params) = SubqueryOrder()
            .select()
            .where {
                (it.status == 36) &&
                        (it.userId in SubqueryOrder()
                            .select { order -> order.userId }
                            .where { order -> order.status == 37 })
            }
            .patch("status" to "36", "status@1" to "37")
            .build()

        assertEquals(
            "SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status AND `tb_subquery_order`.`user_id` IN (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status@1)",
            sql
        )
        assertEquals(mapOf("status" to 36, "status@1" to 37), params)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    @Test
    fun `where scalar subquery type hint does not change sql`() {
        val (sql, params) = SubqueryUser()
            .select()
            .where {
                it.id > (SubqueryOrder()
                    .select { order -> order.userId }
                    .where { order -> order.status == 33 }
                    .limit(1) as Int?)
            }
            .build()

        assertEquals(
            "SELECT `id`, `name` FROM `tb_subquery_user` WHERE `tb_subquery_user`.`id` > (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status LIMIT 1)",
            sql
        )
        assertEquals(mapOf("status" to 33), params)
    }

    @Test
    fun `select item supports aliased scalar subquery`() {
        val (sql, params) = SubqueryUser()
            .select {
                [
                    it.id,
                    SubqueryOrder()
                        .select { order -> order.userId }
                        .where { order -> order.status == 6 }
                        .limit(1)
                        .alias("lastOrderUserId"),
                    it.name
                ]
            }
            .build()

        assertEquals(
            "SELECT `id`, (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status LIMIT 1) AS `lastOrderUserId`, `name` FROM `tb_subquery_user`",
            sql
        )
        assertEquals(mapOf("status" to 6), params)
    }

    @Test
    fun `selectable source can be filtered in the next query layer`() {
        val (sql, params) = SubqueryUser()
            .where { it.id == 1 }
            .select { [it.id, it.name] }
            .where { it.name == "Ada" }
            .build()

        assertEquals(
            "SELECT `q`.`id`, `q`.`name` FROM (SELECT `id`, `name` FROM `tb_subquery_user` WHERE `tb_subquery_user`.`id` = :id) AS `q` WHERE `q`.`name` = :name",
            sql
        )
        assertEquals(mapOf("id" to 1, "name" to "Ada"), params)
    }

    @Test
    fun `order by supports scalar subquery`() {
        val (sql, params) = SubqueryUser()
            .select()
            .orderBy {
                addSortSubquery(
                    SubqueryOrder()
                        .select { order -> order.status }
                        .where { order -> order.userId == 29 }
                        .limit(1),
                    SqlOrdering.Desc
                )
            }
            .build()

        assertEquals(
            "SELECT `id`, `name` FROM `tb_subquery_user` ORDER BY (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`user_id` = :userId LIMIT 1) DESC",
            sql
        )
        assertEquals(mapOf("userId" to 29), params)
    }

    @Test
    fun `where supports exists and not exists selectable subquery`() {
        val (existsSql, existsParams) = SubqueryUser()
            .select()
            .where {
                exists(
                    SubqueryOrder()
                        .select()
                        .where { order -> order.status == 7 }
                )
            }
            .build()

        val (notExistsSql, notExistsParams) = SubqueryUser()
            .select()
            .where {
                !exists(
                    SubqueryOrder()
                        .select()
                        .where { order -> order.status == 8 }
                )
            }
            .build()

        assertEquals(
            "SELECT `id`, `name` FROM `tb_subquery_user` WHERE EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            existsSql
        )
        assertEquals(mapOf("status" to 7), existsParams)
        assertEquals(
            "SELECT `id`, `name` FROM `tb_subquery_user` WHERE NOT EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status)",
            notExistsSql
        )
        assertEquals(mapOf("status" to 8), notExistsParams)
    }

    @Test
    fun `where correlated exists qualifies outer field`() {
        val (sql, params) = SubqueryUser()
            .select()
            .where {
                exists(
                    SubqueryOrder()
                        .select()
                        .where { order -> order.userId == it.id && order.status == 9 }
                )
            }
            .build()

        assertEquals(
            "SELECT `id`, `name` FROM `tb_subquery_user` WHERE EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`user_id` = `tb_subquery_user`.`id` AND `tb_subquery_order`.`status` = :status)",
            sql
        )
        assertEquals(mapOf("status" to 9), params)
    }

    @Test
    fun `where combines in exists and scalar subquery with colliding parameter names`() {
        val (sql, params) = SubqueryUser()
            .select { [it.id, it.name] }
            .where { user ->
                (user.id in SubqueryOrder()
                    .select { order -> order.userId }
                    .where { order -> order.status == 50 }) &&
                        exists(
                            SubqueryOrder()
                                .select()
                                .where { order -> order.userId == user.id && order.status == 51 }
                        ) &&
                        (user.id > SubqueryOrder()
                            .select { order -> order.userId }
                            .where { order -> order.status == 52 }
                            .limit(1))
            }
            .build()

        assertEquals(
            "SELECT `id`, `name` FROM `tb_subquery_user` WHERE `tb_subquery_user`.`id` IN (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status) AND EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`user_id` = `tb_subquery_user`.`id` AND `tb_subquery_order`.`status` = :status@1) AND `tb_subquery_user`.`id` > (SELECT `user_id` AS `userId` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status@2 LIMIT 1)",
            sql
        )
        assertEquals(mapOf("status" to 50, "status@1" to 51, "status@2" to 52), params)
    }

    @Test
    fun `window alias can be filtered in next layer`() {
        val ranked = SubqueryOrder()
            .select {
                [
                    it.id,
                    it.userId,
                    it.status,
                    f.rowNumber()
                        .over {
                            partitionBy(it.userId)
                            orderBy(it.status.asc())
                        }
                        .alias("rn")
                ]
            }

        val (sql, params) = ranked
            .select { [it.id, it.userId, it.status] }
            .where { it.rn == 1 }
            .build()

        assertEquals(
            "SELECT `q`.`id`, `q`.`userId`, `q`.`status` FROM (SELECT `id`, `user_id` AS `userId`, `status`, ROW_NUMBER() OVER (PARTITION BY `user_id` ORDER BY `status` ASC) AS rn FROM `tb_subquery_order`) AS `q` WHERE `q`.`rn` = :rn",
            sql
        )
        assertEquals(mapOf("rn" to 1), params)
    }

    @Test
    fun `window alias can be ordered in current layer`() {
        val (sql, params) = SubqueryOrder()
            .select {
                [
                    it.id,
                    f.rowNumber()
                        .over {
                            partitionBy(it.userId)
                            orderBy(it.status.desc())
                        }
                        .alias("rn")
                ]
            }
            .orderBy { it.rn.asc() }
            .build()

        assertEquals(
            "SELECT `id`, ROW_NUMBER() OVER (PARTITION BY `user_id` ORDER BY `status` DESC) AS rn FROM `tb_subquery_order` ORDER BY `rn` ASC",
            sql
        )
        assertEquals(emptyMap(), params)
    }
}
