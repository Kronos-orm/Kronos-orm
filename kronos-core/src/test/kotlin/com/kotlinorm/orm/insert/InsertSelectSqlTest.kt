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

package com.kotlinorm.orm.insert

import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.where
import com.kotlinorm.beans.subquery.SubqueryIdentityArchive
import com.kotlinorm.beans.subquery.SubqueryOrder
import com.kotlinorm.beans.subquery.SubqueryOrderArchive
import com.kotlinorm.beans.subquery.SubqueryStrategyArchive
import com.kotlinorm.beans.subquery.SubqueryUser
import com.kotlinorm.orm.union.union
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class InsertSelectSqlTest : MysqlTestBase() {
    @Test
    fun `selectable source renders insert select`() {
        val (sql, params) = SubqueryOrder()
            .select {
                [
                    it.id,
                    it.userId,
                    it.status
                ]
            }
            .where { it.status == 13 }
            .insert<SubqueryOrderArchive>()
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_order_archive` (`id`, `user_id`, `status`) SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 13), params)
    }

    @Test
    fun `explicit values render mapped select list`() {
        val (sql, params) = SubqueryOrder()
            .select()
            .where { it.status == 36 }
            .insert<SubqueryOrderArchive> {
                [
                    it.id,
                    null,
                    99
                ]
            }
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_order_archive` (`id`, `user_id`, `status`) SELECT `id`, NULL, :status@1 FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 36, "status@1" to 99), params)
    }

    @Test
    fun `source params use transformer safe conversion`() {
        val (sql, params) = SubqueryOrder()
            .select()
            .where { it.status == 36 }
            .patch("status" to "36")
            .insert<SubqueryOrderArchive> {
                [
                    it.id,
                    null,
                    99
                ]
            }
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_order_archive` (`id`, `user_id`, `status`) SELECT `id`, NULL, :status@1 FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 36, "status@1" to 99), params)
    }

    @Test
    fun `default target field list excludes identity primary key`() {
        val (sql, params) = SubqueryOrder()
            .select {
                [
                    it.userId,
                    it.status
                ]
            }
            .where { it.status == 47 }
            .insert<SubqueryIdentityArchive>()
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_identity_archive` (`user_id`, `status`) SELECT `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 47), params)
    }

    @Test
    fun `explicit values map by full target insertable field order`() {
        val (sql, params) = SubqueryOrder()
            .select()
            .where { it.status == 48 }
            .insert<SubqueryStrategyArchive> {
                [
                    it.userId,
                    it.status,
                    7,
                    null,
                    null,
                    0
                ]
            }
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_strategy_archive` (`user_id`, `status`, `flag`, `create_time`, `update_time`, `deleted`) SELECT `tb_subquery_order`.`user_id`, `status`, :flag, NULL, NULL, :deleted FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 48, "flag" to 7, "deleted" to 0), params)
    }

    @Test
    fun `derived selectable source renders insert select`() {
        val (sql, params) = SubqueryOrder()
            .where { it.status == 41 }
            .select {
                [
                    it.id,
                    it.userId,
                    it.status
                ]
            }
            .where { it.userId == 42 }
            .insert<SubqueryOrderArchive>()
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_order_archive` (`id`, `user_id`, `status`) SELECT `q`.`id`, `q`.`userId`, `q`.`status` FROM (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status) AS `q` WHERE `q`.`userId` = :userId",
            sql
        )
        assertEquals(mapOf("status" to 41, "userId" to 42), params)
    }

    @Test
    fun `join source renders insert select`() {
        val joinQuery = SubqueryUser().join(SubqueryOrder()) { user, order ->
            leftJoin { user.id == order.userId }.select {
                [
                    user.id,
                    order.userId,
                    order.status
                ]
            }.where { user.id == 33 }
        }

        val (sql, params) = joinQuery
            .insert<SubqueryOrderArchive>()
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_order_archive` (`id`, `user_id`, `status`) SELECT `tb_subquery_user`.`id` AS `id`, `tb_subquery_order`.`user_id` AS `userId`, `tb_subquery_order`.`status` AS `status` FROM `tb_subquery_user` LEFT JOIN `tb_subquery_order` ON `tb_subquery_user`.`id` = `tb_subquery_order`.`user_id` WHERE `tb_subquery_user`.`id` = :id",
            sql
        )
        assertEquals(mapOf("id" to 33), params)
    }

    @Test
    fun `union source renders insert select`() {
        val unionQuery = union(
            SubqueryOrder()
                .select { [it.id, it.userId, it.status] }
                .where { it.status == 34 },
            SubqueryOrder()
                .select { [it.id, it.userId, it.status] }
                .where { it.status == 35 }
        )

        val (sql, params) = unionQuery
            .insert<SubqueryOrderArchive>()
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_order_archive` (`id`, `user_id`, `status`) (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status) UNION (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status@1)",
            sql
        )
        assertEquals(mapOf("status" to 34, "status@1" to 35), params)
    }

    @Test
    fun `union source with explicit values rewrites each branch and params`() {
        val unionQuery = union(
            SubqueryOrder()
                .select { [it.id, it.userId, it.status] }
                .where { it.status == 45 },
            SubqueryOrder()
                .select { [it.id, it.userId, it.status] }
                .where { it.status == 46 }
        )

        val (sql, params) = unionQuery
            .insert<SubqueryOrderArchive> {
                [
                    it.userId,
                    null,
                    99
                ]
            }
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_order_archive` (`id`, `user_id`, `status`) (SELECT `user_id`, NULL, :status@2 FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status) UNION (SELECT `user_id`, NULL, :status@2 FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status@1)",
            sql
        )
        assertEquals(mapOf("status" to 45, "status@1" to 46, "status@2" to 99), params)
    }

    @Test
    fun `explicit values can reference source fields after source select list rewrite`() {
        val (sql, params) = SubqueryOrder()
            .select {
                [
                    it.id,
                    it.userId,
                    it.status
                ]
            }
            .where { it.status == 36 }
            .insert<SubqueryOrderArchive> {
                [
                    it.userId,
                    null,
                    99
                ]
            }
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_order_archive` (`id`, `user_id`, `status`) SELECT `user_id`, NULL, :status@1 FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 36, "status@1" to 99), params)
    }

    @Test
    fun `explicit values can include expression and scalar subquery`() {
        val (sql, params) = SubqueryOrder()
            .select {
                [
                    it.id,
                    it.userId,
                    it.status
                ]
            }
            .where { it.status == 37 }
            .insert<SubqueryOrderArchive> {
                [
                    it.id,
                    it.userId + 1,
                    SubqueryOrder()
                        .select { it.status }
                        .where { it.userId == 38 }
                        .limit(1)
                ]
            }
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_order_archive` (`id`, `user_id`, `status`) SELECT `id`, (`user_id` + 1), (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`user_id` = :userId LIMIT 1) FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 37, "userId" to 38), params)
    }

    @Test
    fun `explicit scalar subquery value renames colliding params`() {
        val (sql, params) = SubqueryOrder()
            .select {
                [
                    it.id,
                    it.userId,
                    it.status
                ]
            }
            .where { it.status == 37 }
            .insert<SubqueryOrderArchive> {
                [
                    it.id,
                    it.userId,
                    SubqueryOrder()
                        .select { it.status }
                        .where { it.status == 38 }
                        .limit(1)
                ]
            }
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_order_archive` (`id`, `user_id`, `status`) SELECT `id`, `user_id`, (SELECT `status` FROM `tb_subquery_order` WHERE `status` = :status@1 LIMIT 1) FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 37, "status@1" to 38), params)
    }

}
