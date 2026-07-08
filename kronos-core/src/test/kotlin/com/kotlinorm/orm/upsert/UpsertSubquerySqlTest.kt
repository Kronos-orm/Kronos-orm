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

package com.kotlinorm.orm.upsert

import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.where
import com.kotlinorm.beans.subquery.SubqueryOrder
import com.kotlinorm.beans.subquery.SubqueryUser
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SamplePostgresJdbcWrapper
import com.kotlinorm.wrappers.SampleSqliteJdbcWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

class UpsertSubquerySqlTest : MysqlTestBase() {
    @Test
    fun `conflict assignment supports scalar subquery`() {
        val (sql, params) = SubqueryUser(id = 1, name = "seed")
            .upsert()
            .patch(
                "name" to SubqueryOrder()
                    .select { it.status }
                    .where { it.status == 15 }
                    .limit(1)
            )
            .on { it.id }
            .onConflict()
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_user` (`id`, `name`) VALUES (:id, :name) ON DUPLICATE KEY UPDATE `name` = (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status LIMIT 1)",
            sql
        )
        assertEquals(mapOf("id" to 1, "name" to "seed", "status" to 15), params)
    }

    @Test
    fun `conflict assignment scalar subquery renames colliding params`() {
        val (sql, params) = SubqueryOrder(id = 4, userId = 5, status = 9)
            .upsert()
            .patch(
                "status" to SubqueryOrder()
                    .select { it.status }
                    .where { it.status == 21 }
                    .limit(1)
            )
            .on { it.id }
            .onConflict()
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_order` (`id`, `user_id`, `status`) VALUES (:id, :userId, :status) ON DUPLICATE KEY UPDATE `status` = (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status@1 LIMIT 1)",
            sql
        )
        assertEquals(mapOf("id" to 4, "userId" to 5, "status" to 9, "status@1" to 21), params)
    }

    @Test
    fun `conflict assignment scalar subquery renders postgres sql`() {
        val (sql, params) = SubqueryUser(id = 6, name = "seed")
            .upsert()
            .patch(
                "name" to SubqueryOrder()
                    .select { it.status }
                    .where { it.status == 22 }
                    .limit(1)
            )
            .on { it.id }
            .onConflict()
            .build(SamplePostgresJdbcWrapper())

        assertEquals(
            "INSERT INTO \"tb_subquery_user\" (\"id\", \"name\") VALUES (:id, :name) ON CONFLICT (\"id\") DO UPDATE SET \"name\" = (SELECT \"status\" FROM \"tb_subquery_order\" WHERE \"tb_subquery_order\".\"status\" = :status LIMIT 1)",
            sql
        )
        assertEquals(mapOf("id" to 6, "name" to "seed", "status" to 22), params)
    }

    @Test
    fun `conflict assignment scalar subquery renders sqlite sql`() {
        val (sql, params) = SubqueryUser(id = 7, name = "seed")
            .upsert()
            .patch(
                "name" to SubqueryOrder()
                    .select { it.status }
                    .where { it.status == 23 }
                    .limit(1)
            )
            .on { it.id }
            .onConflict()
            .build(SampleSqliteJdbcWrapper)

        assertEquals(
            "INSERT INTO \"tb_subquery_user\" (\"id\", \"name\") VALUES (:id, :name) ON CONFLICT (\"id\") DO UPDATE SET \"name\" = (SELECT \"status\" FROM \"tb_subquery_order\" WHERE \"tb_subquery_order\".\"status\" = :status LIMIT 1)",
            sql
        )
        assertEquals(mapOf("id" to 7, "name" to "seed", "status" to 23), params)
    }

    @Test
    fun `set block conflict assignment supports scalar subquery`() {
        val (sql, params) = SubqueryUser(id = 2, name = "seed")
            .upsert()
            .on { it.id }
            .set { user ->
                setValue(
                    user.kronosColumns().single { it.name == "name" },
                    SubqueryOrder()
                        .select { it.status }
                        .where { it.status == 19 }
                        .limit(1)
                )
            }
            .onConflict()
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_user` (`id`, `name`) VALUES (:id, :name) ON DUPLICATE KEY UPDATE `name` = (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status LIMIT 1)",
            sql
        )
        assertEquals(mapOf("id" to 2, "name" to "seed", "status" to 19), params)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    @Test
    fun `set assignment scalar subquery type hint renders mysql sql`() {
        val (sql, params) = SubqueryUser(id = 3, name = "seed")
            .upsert()
            .on { it.id }
            .set {
                it.name = (SubqueryOrder()
                    .select { order -> order.status }
                    .where { order -> order.status == 21 }
                    .limit(1) as String?)
            }
            .onConflict()
            .build()

        assertEquals(
            "INSERT INTO `tb_subquery_user` (`id`, `name`) VALUES (:id, :name) ON DUPLICATE KEY UPDATE `name` = (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status LIMIT 1)",
            sql
        )
        assertEquals(mapOf("id" to 3, "name" to "seed", "status" to 21), params)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    @Test
    fun `set assignment scalar subquery type hint renders postgres sql`() {
        val (sql, params) = SubqueryUser(id = 8, name = "seed")
            .upsert()
            .on { it.id }
            .set {
                it.name = (SubqueryOrder()
                    .select { order -> order.status }
                    .where { order -> order.status == 24 }
                    .limit(1) as String?)
            }
            .onConflict()
            .build(SamplePostgresJdbcWrapper())

        assertEquals(
            "INSERT INTO \"tb_subquery_user\" (\"id\", \"name\") VALUES (:id, :name) ON CONFLICT (\"id\") DO UPDATE SET \"name\" = (SELECT \"status\" FROM \"tb_subquery_order\" WHERE \"tb_subquery_order\".\"status\" = :status LIMIT 1)",
            sql
        )
        assertEquals(mapOf("id" to 8, "name" to "seed", "status" to 24), params)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    @Test
    fun `set assignment scalar subquery type hint renders sqlite sql`() {
        val (sql, params) = SubqueryUser(id = 9, name = "seed")
            .upsert()
            .on { it.id }
            .set {
                it.name = (SubqueryOrder()
                    .select { order -> order.status }
                    .where { order -> order.status == 25 }
                    .limit(1) as String?)
            }
            .onConflict()
            .build(SampleSqliteJdbcWrapper)

        assertEquals(
            "INSERT INTO \"tb_subquery_user\" (\"id\", \"name\") VALUES (:id, :name) ON CONFLICT (\"id\") DO UPDATE SET \"name\" = (SELECT \"status\" FROM \"tb_subquery_order\" WHERE \"tb_subquery_order\".\"status\" = :status LIMIT 1)",
            sql
        )
        assertEquals(mapOf("id" to 9, "name" to "seed", "status" to 25), params)
    }
}
