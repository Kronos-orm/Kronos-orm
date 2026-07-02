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

package com.kotlinorm.orm.subquery

import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.where
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert
import com.kotlinorm.orm.union.union
import com.kotlinorm.Kronos
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Verifies user-facing subquery DSL rendering in core clause builders.
 */
class MysqlSubqueryDslTest : MysqlTestBase() {
    @Test
    fun `select where field in selectable subquery renders sql and params`() {
        val (inSql, inParams) = Scene2User()
            .select()
            .where {
                it.id in Scene2Order()
                    .select { order -> order.userId }
                    .where { order -> order.status == 1 }
            }
            .build()

        val (notInSql, notInParams) = Scene2User()
            .select()
            .where {
                it.id !in Scene2Order()
                    .select { order -> order.userId }
                    .where { order -> order.status == 2 }
            }
            .build()

        assertEquals(
            "SELECT `id`, `name` FROM `tb_scene2_user` WHERE `id` IN (SELECT `user_id` AS `userId` FROM `tb_scene2_order` WHERE `status` = :status)",
            inSql
        )
        assertEquals(mapOf("status" to 1), inParams)
        assertEquals(
            "SELECT `id`, `name` FROM `tb_scene2_user` WHERE `id` NOT IN (SELECT `user_id` AS `userId` FROM `tb_scene2_order` WHERE `status` = :status)",
            notInSql
        )
        assertEquals(mapOf("status" to 2), notInParams)
    }

    @Test
    fun `select where row tuple in selectable subquery renders sql and params`() {
        val (sql, params) = Scene2Order()
            .select()
            .where {
                [it.userId, it.status] in Scene2Order()
                    .select { order -> [order.userId, order.status] }
                    .where { order -> order.status == 17 }
            }
            .build()

        val (notInSql, notInParams) = Scene2Order()
            .select()
            .where {
                [it.userId, it.status] !in Scene2Order()
                    .select { order -> [order.userId, order.status] }
                    .where { order -> order.status == 20 }
            }
            .build()

        assertEquals(
            "SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE (`user_id`, `status`) IN (SELECT `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status)",
            sql
        )
        assertEquals(mapOf("status" to 17), params)
        assertEquals(
            "SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE (`user_id`, `status`) NOT IN (SELECT `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status)",
            notInSql
        )
        assertEquals(mapOf("status" to 20), notInParams)
    }

    @Test
    fun `update where field in selectable subquery renders sql and params`() {
        val (inSql, inParams) = Scene2User(name = "active")
            .update { [it.name] }
            .where {
                it.id in Scene2Order()
                    .select { order -> order.userId }
                    .where { order -> order.status == 1 }
            }
            .build()

        val (notInSql, notInParams) = Scene2User(name = "inactive")
            .update { [it.name] }
            .where {
                it.id !in Scene2Order()
                    .select { order -> order.userId }
                    .where { order -> order.status == 2 }
            }
            .build()

        assertEquals(
            "UPDATE `tb_scene2_user` SET `name` = :nameNew WHERE `id` IN (SELECT `user_id` AS `userId` FROM `tb_scene2_order` WHERE `status` = :status)",
            inSql
        )
        assertEquals(mapOf("status" to 1, "nameNew" to "active"), inParams)
        assertEquals(
            "UPDATE `tb_scene2_user` SET `name` = :nameNew WHERE `id` NOT IN (SELECT `user_id` AS `userId` FROM `tb_scene2_order` WHERE `status` = :status)",
            notInSql
        )
        assertEquals(mapOf("status" to 2, "nameNew" to "inactive"), notInParams)
    }

    @Test
    fun `update where row tuple in selectable subquery renders sql and params`() {
        val (inSql, inParams) = Scene2Order(status = 30)
            .update { [it.status] }
            .where {
                [it.userId, it.status] in Scene2Order()
                    .select { order -> [order.userId, order.status] }
                    .where { order -> order.status == 21 }
            }
            .build()

        val (notInSql, notInParams) = Scene2Order(status = 31)
            .update { [it.status] }
            .where {
                [it.userId, it.status] !in Scene2Order()
                    .select { order -> [order.userId, order.status] }
                    .where { order -> order.status == 22 }
            }
            .build()

        assertEquals(
            "UPDATE `tb_scene2_order` SET `status` = :statusNew WHERE (`user_id`, `status`) IN (SELECT `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status)",
            inSql
        )
        assertEquals(mapOf("status" to 21, "statusNew" to 30), inParams)
        assertEquals(
            "UPDATE `tb_scene2_order` SET `status` = :statusNew WHERE (`user_id`, `status`) NOT IN (SELECT `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status)",
            notInSql
        )
        assertEquals(mapOf("status" to 22, "statusNew" to 31), notInParams)
    }

    @Test
    fun `update where scalar subquery comparison renders sql and params`() {
        val (sql, params) = Scene2Order(status = 32)
            .update { [it.status] }
            .where {
                it.status > Scene2Order()
                    .select { order -> order.status }
                    .where { order -> order.userId == 25 }
                    .limit(1)
            }
            .build()

        assertEquals(
            "UPDATE `tb_scene2_order` SET `status` = :statusNew WHERE `status` > (SELECT `status` FROM `tb_scene2_order` WHERE `user_id` = :userId LIMIT 1)",
            sql
        )
        assertEquals(mapOf("userId" to 25, "statusNew" to 32), params)
    }

    @Test
    fun `delete where field in selectable subquery renders sql and params`() {
        val (inSql, inParams) = Scene2User()
            .delete()
            .logic(false)
            .where {
                it.id in Scene2Order()
                    .select { order -> order.userId }
                    .where { order -> order.status == 3 }
            }
            .build()

        val (notInSql, notInParams) = Scene2User()
            .delete()
            .logic(false)
            .where {
                it.id !in Scene2Order()
                    .select { order -> order.userId }
                    .where { order -> order.status == 4 }
            }
            .build()

        assertEquals(
            "DELETE FROM `tb_scene2_user` WHERE `id` IN (SELECT `user_id` AS `userId` FROM `tb_scene2_order` WHERE `status` = :status)",
            inSql
        )
        assertEquals(mapOf("status" to 3), inParams)
        assertEquals(
            "DELETE FROM `tb_scene2_user` WHERE `id` NOT IN (SELECT `user_id` AS `userId` FROM `tb_scene2_order` WHERE `status` = :status)",
            notInSql
        )
        assertEquals(mapOf("status" to 4), notInParams)
    }

    @Test
    fun `delete where row tuple in selectable subquery renders sql and params`() {
        val (inSql, inParams) = Scene2Order()
            .delete()
            .logic(false)
            .where {
                [it.userId, it.status] in Scene2Order()
                    .select { order -> [order.userId, order.status] }
                    .where { order -> order.status == 23 }
            }
            .build()

        val (notInSql, notInParams) = Scene2Order()
            .delete()
            .logic(false)
            .where {
                [it.userId, it.status] !in Scene2Order()
                    .select { order -> [order.userId, order.status] }
                    .where { order -> order.status == 24 }
            }
            .build()

        assertEquals(
            "DELETE FROM `tb_scene2_order` WHERE (`user_id`, `status`) IN (SELECT `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status)",
            inSql
        )
        assertEquals(mapOf("status" to 23), inParams)
        assertEquals(
            "DELETE FROM `tb_scene2_order` WHERE (`user_id`, `status`) NOT IN (SELECT `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status)",
            notInSql
        )
        assertEquals(mapOf("status" to 24), notInParams)
    }

    @Test
    fun `delete where scalar subquery comparison renders sql and params`() {
        val (sql, params) = Scene2Order()
            .delete()
            .logic(false)
            .where {
                it.status > Scene2Order()
                    .select { order -> order.status }
                    .where { order -> order.userId == 26 }
                    .limit(1)
            }
            .build()

        assertEquals(
            "DELETE FROM `tb_scene2_order` WHERE `status` > (SELECT `status` FROM `tb_scene2_order` WHERE `user_id` = :userId LIMIT 1)",
            sql
        )
        assertEquals(mapOf("userId" to 26), params)
    }

    @Test
    fun `update where quantified subquery comparison renders sql and params`() {
        val (sql, params) = Scene2Order(status = 33)
            .update { [it.status] }
            .where {
                it.status > any<Int>(
                    Scene2Order()
                        .select { order -> order.status }
                        .where { order -> order.userId == 27 }
                )
            }
            .build()

        assertEquals(
            "UPDATE `tb_scene2_order` SET `status` = :statusNew WHERE `status` > ANY (SELECT `status` FROM `tb_scene2_order` WHERE `user_id` = :userId)",
            sql
        )
        assertEquals(mapOf("userId" to 27, "statusNew" to 33), params)
    }

    @Test
    fun `delete where quantified subquery comparison renders sql and params`() {
        val (sql, params) = Scene2Order()
            .delete()
            .logic(false)
            .where {
                it.status <= all<Int>(
                    Scene2Order()
                        .select { order -> order.status }
                        .where { order -> order.userId == 28 }
                )
            }
            .build()

        assertEquals(
            "DELETE FROM `tb_scene2_order` WHERE `status` <= ALL (SELECT `status` FROM `tb_scene2_order` WHERE `user_id` = :userId)",
            sql
        )
        assertEquals(mapOf("userId" to 28), params)
    }

    @Test
    fun `logic delete where field in selectable subquery renders update sql and params`() {
        val (sql, params) = Scene2LogicUser()
            .delete()
            .where {
                it.id in Scene2Order()
                    .select { order -> order.userId }
                    .where { order -> order.status == 5 }
            }
            .build()

        assertEquals(
            "UPDATE `tb_scene2_logic_user` SET `deleted` = :deletedNew WHERE `id` IN (SELECT `user_id` AS `userId` FROM `tb_scene2_order` WHERE `status` = :status) AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("status" to 5, "deletedNew" to 1), params)
    }

    @Test
    fun `select scalar subquery item renders sql and params`() {
        val (sql, params) = Scene2User()
            .select {
                [
                    it.id,
                    Scene2Order()
                        .select { order -> order.userId }
                        .where { order -> order.status == 6 }
                        .limit(1)
                        .alias("lastOrderUserId"),
                    it.name
                ]
            }
            .build()

        assertEquals(
            "SELECT `id`, (SELECT `user_id` AS `userId` FROM `tb_scene2_order` WHERE `status` = :status LIMIT 1) AS `lastOrderUserId`, `name` FROM `tb_scene2_user`",
            sql
        )
        assertEquals(mapOf("status" to 6), params)
    }

    @Test
    fun `select from selectable source renders derived query sql and params`() {
        val (sql, params) = Scene2User()
            .where { it.id == 1 }
            .select { [it.id, it.name] }
            .where { it.name == "Ada" }
            .build()

        assertEquals(
            "SELECT `q`.`id`, `q`.`name` FROM (SELECT `id`, `name` FROM `tb_scene2_user` WHERE `id` = :id) AS `q` WHERE `q`.`name` = :name",
            sql
        )
        assertEquals(mapOf("id" to 1, "name" to "Ada"), params)
    }

    @Test
    fun `join selectable source renders derived table sql and params`() {
        val orders = Scene2Order()
            .select { [it.userId, it.status] }
            .where { it.status == 40 }

        val task = Scene2User()
            .join(orders) { user, order ->
                leftJoin(order) { user.id == order.userId }
                select { [user.id, order.status] }
            }
            .build()
            .atomicTask

        assertEquals(
            "SELECT `tb_scene2_user`.`id` AS `id`, `q`.`status` AS `status` FROM `tb_scene2_user` LEFT JOIN (SELECT `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status) AS `q` ON `tb_scene2_user`.`id` = `q`.`userId`",
            task.sql
        )
        assertEquals(mapOf("status" to 40), task.paramMap)
    }

    @Test
    fun `select order by scalar subquery renders sql and params`() {
        val (sql, params) = Scene2User()
            .select()
            .orderBy {
                addSortSubquery(
                    Scene2Order()
                        .select { order -> order.status }
                        .where { order -> order.userId == 29 }
                        .limit(1),
                    com.kotlinorm.enums.SortType.DESC
                )
            }
            .build()

        assertEquals(
            "SELECT `id`, `name` FROM `tb_scene2_user` ORDER BY (SELECT `status` FROM `tb_scene2_order` WHERE `user_id` = :userId LIMIT 1) DESC",
            sql
        )
        assertEquals(mapOf("userId" to 29), params)
    }

    @Test
    fun `select where exists selectable subquery renders sql and params`() {
        val (existsSql, existsParams) = Scene2User()
            .select()
            .where {
                exists(
                    Scene2Order()
                        .select()
                        .where { order -> order.status == 7 }
                )
            }
            .build()

        val (notExistsSql, notExistsParams) = Scene2User()
            .select()
            .where {
                !exists(
                    Scene2Order()
                        .select()
                        .where { order -> order.status == 8 }
                )
            }
            .build()

        assertEquals(
            "SELECT `id`, `name` FROM `tb_scene2_user` WHERE EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status)",
            existsSql
        )
        assertEquals(mapOf("status" to 7), existsParams)
        assertEquals(
            "SELECT `id`, `name` FROM `tb_scene2_user` WHERE NOT EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status)",
            notExistsSql
        )
        assertEquals(mapOf("status" to 8), notExistsParams)
    }

    @Test
    fun `update where exists selectable subquery renders sql and params`() {
        val (existsSql, existsParams) = Scene2User(name = "active")
            .update { [it.name] }
            .where {
                exists(
                    Scene2Order()
                        .select()
                        .where { order -> order.status == 9 }
                )
            }
            .build()

        val (notExistsSql, notExistsParams) = Scene2User(name = "inactive")
            .update { [it.name] }
            .where {
                !exists(
                    Scene2Order()
                        .select()
                        .where { order -> order.status == 10 }
                )
            }
            .build()

        assertEquals(
            "UPDATE `tb_scene2_user` SET `name` = :nameNew WHERE EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status)",
            existsSql
        )
        assertEquals(mapOf("status" to 9, "nameNew" to "active"), existsParams)
        assertEquals(
            "UPDATE `tb_scene2_user` SET `name` = :nameNew WHERE NOT EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status)",
            notExistsSql
        )
        assertEquals(mapOf("status" to 10, "nameNew" to "inactive"), notExistsParams)
    }

    @Test
    fun `update set scalar subquery renders sql and params`() {
        val (sql, params) = Scene2User()
            .update()
            .patch(
                "name" to Scene2Order()
                    .select { it.status }
                    .where { it.status == 14 }
                    .limit(1)
            )
            .where { it.id == 1 }
            .build()

        assertEquals(
            "UPDATE `tb_scene2_user` SET `name` = (SELECT `status` FROM `tb_scene2_order` WHERE `status` = :status LIMIT 1) WHERE `id` = :id",
            sql
        )
        assertEquals(mapOf("status" to 14, "id" to 1), params)
    }

    @Test
    fun `update set block scalar subquery renders sql and params`() {
        val (sql, params) = Scene2User()
            .update()
            .set { user ->
                setValue(
                    user.kronosColumns().single { it.name == "name" },
                    Scene2Order()
                        .select { it.status }
                        .where { it.status == 18 }
                        .limit(1)
                )
            }
            .where { it.id == 2 }
            .build()

        assertEquals(
            "UPDATE `tb_scene2_user` SET `name` = (SELECT `status` FROM `tb_scene2_order` WHERE `status` = :status LIMIT 1) WHERE `id` = :id",
            sql
        )
        assertEquals(mapOf("status" to 18, "id" to 2), params)
    }

    @Test
    fun `delete where exists selectable subquery renders sql and params`() {
        val (existsSql, existsParams) = Scene2User()
            .delete()
            .logic(false)
            .where {
                exists(
                    Scene2Order()
                        .select()
                        .where { order -> order.status == 11 }
                )
            }
            .build()

        val (notExistsSql, notExistsParams) = Scene2User()
            .delete()
            .logic(false)
            .where {
                !exists(
                    Scene2Order()
                        .select()
                        .where { order -> order.status == 12 }
                )
            }
            .build()

        assertEquals(
            "DELETE FROM `tb_scene2_user` WHERE EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status)",
            existsSql
        )
        assertEquals(mapOf("status" to 11), existsParams)
        assertEquals(
            "DELETE FROM `tb_scene2_user` WHERE NOT EXISTS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status)",
            notExistsSql
        )
        assertEquals(mapOf("status" to 12), notExistsParams)
    }

    @Test
    fun `insert selectable source renders insert select sql and params`() {
        val (sql, params) = Scene2Order()
            .select {
                [
                    it.id,
                    it.userId,
                    it.status
                ]
            }
            .where { it.status == 13 }
            .insert<Scene2OrderArchive>()
            .build()

        assertEquals(
            "INSERT INTO `tb_scene2_order_archive` (`id`, `user_id`, `status`) SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 13), params)
    }

    @Test
    fun `insert selectable source with explicit values renders mapped select list`() {
        val (sql, params) = Scene2Order()
            .select()
            .where { it.status == 36 }
            .insert<Scene2OrderArchive> {
                [
                    it.id,
                    null,
                    99
                ]
            }
            .build()

        assertEquals(
            "INSERT INTO `tb_scene2_order_archive` (`id`, `user_id`, `status`) SELECT `id`, NULL, :status@1 FROM `tb_scene2_order` WHERE `status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 36, "status@1" to 99), params)
    }

    @Test
    fun `insert selectable source rejects default source column count mismatch`() {
        val error = assertFailsWith<IllegalArgumentException> {
            Scene2Order()
                .select { it.id }
                .insert<Scene2OrderArchive>()
                .build()
        }

        assertEquals(
            "Insert-select source column count (1) must match target insertable field count (3).",
            error.message
        )
    }

    @Test
    fun `insert derived selectable source renders insert select sql and params`() {
        val (sql, params) = Scene2Order()
            .where { it.status == 41 }
            .select {
                [
                    it.id,
                    it.userId,
                    it.status
                ]
            }
            .where { it.userId == 42 }
            .insert<Scene2OrderArchive>()
            .build()

        assertEquals(
            "INSERT INTO `tb_scene2_order_archive` (`id`, `user_id`, `status`) SELECT `q`.`id`, `q`.`user_id` AS `userId`, `q`.`status` FROM (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status) AS `q` WHERE `q`.`user_id` = :userId",
            sql
        )
        assertEquals(mapOf("status" to 41, "userId" to 42), params)
    }

    @Test
    fun `insert join source renders insert select sql and params`() {
        val joinQuery = Scene2User().join(Scene2Order()) { user, order ->
            leftJoin(order) { user.id == order.userId }
            select {
                [
                    user.id,
                    order.userId,
                    order.status
                ]
            }
            where { user.id == 33 }
        }

        val (sql, params) = joinQuery
            .insert<Scene2OrderArchive>()
            .build()

        assertEquals(
            "INSERT INTO `tb_scene2_order_archive` (`id`, `user_id`, `status`) SELECT `tb_scene2_user`.`id` AS `id`, `tb_scene2_order`.`user_id` AS `userId`, `tb_scene2_order`.`status` AS `status` FROM `tb_scene2_user` LEFT JOIN `tb_scene2_order` ON `tb_scene2_user`.`id` = `tb_scene2_order`.`user_id` WHERE `tb_scene2_user`.`id` = :id",
            sql
        )
        assertEquals(mapOf("id" to 33), params)
    }

    @Test
    fun `insert union source renders insert select sql and params`() {
        val unionQuery = union(
            Scene2Order()
                .select { [it.id, it.userId, it.status] }
                .where { it.status == 34 },
            Scene2Order()
                .select { [it.id, it.userId, it.status] }
                .where { it.status == 35 }
        )

        val (sql, params) = unionQuery
            .insert<Scene2OrderArchive>()
            .build()

        assertEquals(
            "INSERT INTO `tb_scene2_order_archive` (`id`, `user_id`, `status`) (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status) UNION (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status@1)",
            sql
        )
        assertEquals(mapOf("status" to 34, "status@1" to 35), params)
    }

    @Test
    fun `insert selectable source with explicit values rewrites select list`() {
        val (sql, params) = Scene2Order()
            .select {
                [
                    it.id,
                    it.userId,
                    it.status
                ]
            }
            .where { it.status == 36 }
            .insert<Scene2OrderArchive> {
                [
                    it.userId,
                    null,
                    99
                ]
            }
            .build()

        assertEquals(
            "INSERT INTO `tb_scene2_order_archive` (`id`, `user_id`, `status`) SELECT `userId`, NULL, :status@1 FROM `tb_scene2_order` WHERE `status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 36, "status@1" to 99), params)
    }

    @Test
    fun `insert selectable source with function and scalar subquery values renders sql and params`() {
        val (sql, params) = Scene2Order()
            .select {
                [
                    it.id,
                    it.userId,
                    it.status
                ]
            }
            .where { it.status == 37 }
            .insert<Scene2OrderArchive> {
                [
                    it.id,
                    it.userId + 1,
                    Scene2Order()
                        .select { it.status }
                        .where { it.userId == 38 }
                        .limit(1)
                ]
            }
            .build()

        assertEquals(
            "INSERT INTO `tb_scene2_order_archive` (`id`, `user_id`, `status`) SELECT `id`, (`userId` + 1), (SELECT `status` FROM `tb_scene2_order` WHERE `user_id` = :userId LIMIT 1) FROM `tb_scene2_order` WHERE `status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 37, "userId" to 38), params)
    }

    @Test
    fun `insert selectable source rejects default column count mismatch`() {
        val error = assertFailsWith<IllegalArgumentException> {
            Scene2Order()
                .select {
                    [
                        it.id,
                        it.userId
                    ]
                }
                .insert<Scene2OrderArchive>()
                .build()
        }

        assertEquals(
            "Insert-select source column count (2) must match target insertable field count (3).",
            error.message
        )
    }

    @Test
    fun `upsert conflict assignment scalar subquery renders sql and params`() {
        val (sql, params) = Scene2User(id = 1, name = "seed")
            .upsert()
            .patch(
                "name" to Scene2Order()
                    .select { it.status }
                    .where { it.status == 15 }
                    .limit(1)
            )
            .on { it.id }
            .onConflict()
            .build()

        assertEquals(
            "INSERT INTO `tb_scene2_user` (`id`, `name`) VALUES (:id, :name) ON DUPLICATE KEY UPDATE `name` = (SELECT `status` FROM `tb_scene2_order` WHERE `status` = :status LIMIT 1)",
            sql
        )
        assertEquals(mapOf("id" to 1, "name" to "seed", "status" to 15), params)
    }

    @Test
    fun `upsert set block conflict assignment scalar subquery renders sql and params`() {
        val (sql, params) = Scene2User(id = 2, name = "seed")
            .upsert()
            .on { it.id }
            .set { user ->
                setValue(
                    user.kronosColumns().single { it.name == "name" },
                    Scene2Order()
                        .select { it.status }
                        .where { it.status == 19 }
                        .limit(1)
                )
            }
            .onConflict()
            .build()

        assertEquals(
            "INSERT INTO `tb_scene2_user` (`id`, `name`) VALUES (:id, :name) ON DUPLICATE KEY UPDATE `name` = (SELECT `status` FROM `tb_scene2_order` WHERE `status` = :status LIMIT 1)",
            sql
        )
        assertEquals(mapOf("id" to 2, "name" to "seed", "status" to 19), params)
    }

    @Test
    fun `create table as selectable source renders sql and params`() {
        val (sql, params) = Kronos.dataSource.table
            .buildCreateTableAsSelectTask(
                Scene2OrderArchive(),
                Scene2Order()
                    .select {
                        [
                            it.id,
                            it.userId,
                            it.status
                        ]
                    }
                    .where { it.status == 16 }
            )

        assertEquals(
            "CREATE TABLE IF NOT EXISTS `tb_scene2_order_archive` AS SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 16), params)
    }

    @Test
    fun `create table as derived selectable source renders sql and params`() {
        val (sql, params) = Kronos.dataSource.table
            .buildCreateTableAsSelectTask(
                Scene2OrderArchive(),
                Scene2Order()
                    .where { it.status == 43 }
                    .select {
                        [
                            it.id,
                            it.userId,
                            it.status
                        ]
                    }
                    .where { it.userId == 44 }
            )

        assertEquals(
            "CREATE TABLE IF NOT EXISTS `tb_scene2_order_archive` AS SELECT `q`.`id`, `q`.`user_id` AS `userId`, `q`.`status` FROM (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status) AS `q` WHERE `q`.`user_id` = :userId",
            sql
        )
        assertEquals(mapOf("status" to 43, "userId" to 44), params)
    }

    @Test
    fun `create table as join source renders sql and params`() {
        val joinQuery = Scene2User().join(Scene2Order()) { user, order ->
            leftJoin(order) { user.id == order.userId }
            select {
                [
                    user.id,
                    order.userId,
                    order.status
                ]
            }
            where { user.id == 30 }
        }

        val (sql, params) = Kronos.dataSource.table
            .buildCreateTableAsSelectTask(Scene2OrderArchive(), joinQuery)

        assertEquals(
            "CREATE TABLE IF NOT EXISTS `tb_scene2_order_archive` AS SELECT `tb_scene2_user`.`id` AS `id`, `tb_scene2_order`.`user_id` AS `userId`, `tb_scene2_order`.`status` AS `status` FROM `tb_scene2_user` LEFT JOIN `tb_scene2_order` ON `tb_scene2_user`.`id` = `tb_scene2_order`.`user_id` WHERE `tb_scene2_user`.`id` = :id",
            sql
        )
        assertEquals(mapOf("id" to 30), params)
    }

    @Test
    fun `create table as union source renders sql and params`() {
        val unionQuery = union(
            Scene2Order()
                .select { [it.id, it.userId, it.status] }
                .where { it.status == 31 },
            Scene2Order()
                .select { [it.id, it.userId, it.status] }
                .where { it.status == 32 }
        )

        val (sql, params) = Kronos.dataSource.table
            .buildCreateTableAsSelectTask(Scene2OrderArchive(), unionQuery)

        assertEquals(
            "CREATE TABLE IF NOT EXISTS `tb_scene2_order_archive` AS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status) UNION (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_scene2_order` WHERE `status` = :status@1)",
            sql
        )
        assertEquals(mapOf("status" to 31, "status@1" to 32), params)
    }

    @Test
    fun `window alias next layer filter renders derived sql and params`() {
        val ranked = Scene2Order()
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
            "SELECT `q`.`id`, `q`.`userId`, `q`.`status` FROM (SELECT `id`, `user_id` AS `userId`, `status`, ROW_NUMBER() OVER (PARTITION BY `user_id` ORDER BY `status` ASC) AS rn FROM `tb_scene2_order`) AS `q` WHERE `q`.`rn` = :rn",
            sql
        )
        assertEquals(mapOf("rn" to 1), params)
    }

    @Test
    fun `window alias order by renders current layer sql`() {
        val (sql, params) = Scene2Order()
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
            "SELECT `id`, ROW_NUMBER() OVER (PARTITION BY `user_id` ORDER BY `status` DESC) AS rn FROM `tb_scene2_order` ORDER BY `rn` ASC",
            sql
        )
        assertEquals(emptyMap(), params)
    }
}

@Table(name = "tb_scene2_user")
internal data class Scene2User(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table(name = "tb_scene2_order")
internal data class Scene2Order(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

@Table(name = "tb_scene2_logic_user")
internal data class Scene2LogicUser(
    var id: Int? = null,
    @LogicDelete
    var deleted: Boolean? = null,
) : KPojo

@Table(name = "tb_scene2_order_archive")
internal data class Scene2OrderArchive(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo
