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

package com.kotlinorm.orm.ddl

import com.kotlinorm.Kronos
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.where
import com.kotlinorm.beans.subquery.SubqueryOrder
import com.kotlinorm.beans.subquery.SubqueryOrderArchive
import com.kotlinorm.beans.subquery.SubqueryUser
import com.kotlinorm.orm.union.union
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateTableAsSelectSqlTest : MysqlTestBase() {
    @Test
    fun `selectable source renders create table as select`() {
        val (sql, params) = Kronos.dataSource.table
            .buildCreateTableAsSelectTask(
                SubqueryOrderArchive(),
                SubqueryOrder()
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
            "CREATE TABLE IF NOT EXISTS `tb_subquery_order_archive` AS SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 16), params)
    }

    @Test
    fun `scalar subquery select item renames colliding params`() {
        val (sql, params) = Kronos.dataSource.table
            .buildCreateTableAsSelectTask(
                SubqueryOrderArchive(),
                SubqueryOrder()
                    .select {
                        [
                            it.id,
                            it.userId,
                            SubqueryOrder()
                                .select { order -> order.status }
                                .where { order -> order.status == 38 }
                                .limit(1)
                                .alias("latestStatus")
                        ]
                    }
                    .where { it.status == 37 }
            )

        assertEquals(
            "CREATE TABLE IF NOT EXISTS `tb_subquery_order_archive` AS SELECT `id`, `user_id` AS `userId`, (SELECT `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status@1 LIMIT 1) AS `latestStatus` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status",
            sql
        )
        assertEquals(mapOf("status" to 37, "status@1" to 38), params)
    }

    @Test
    fun `syntax task filters final params by exact sql names`() {
        val statement = SqlDdlStatement.CreateTableAsSelect(
            tableName = SqlIdentifier.of("tb_subquery_order_archive"),
            query = SqlQuery.Select(
                select = listOf(
                    SqlSelectItem.Expr(SqlExpr.Parameter(SqlParameter.Named("status@1")), "status")
                ),
                from = listOf(SqlTable.Ident("tb_subquery_order"))
            )
        )
        val (sql, params) = Kronos.dataSource.table.buildCreateTableAsSelectTaskFromQuery(
            statement,
            mapOf("status" to 37, "status@1" to 38)
        )

        assertEquals(
            "CREATE TABLE IF NOT EXISTS `tb_subquery_order_archive` AS SELECT :status@1 AS status FROM `tb_subquery_order`",
            sql
        )
        assertEquals(mapOf("status@1" to 38), params)
    }

    @Test
    fun `syntax params use transformer safe conversion with field metadata`() {
        val statement = SqlDdlStatement.CreateTableAsSelect(
            tableName = SqlIdentifier.of("tb_subquery_order_archive"),
            query = SqlQuery.Select(
                select = listOf(
                    SqlSelectItem.Expr(SqlExpr.Parameter(SqlParameter.Named("status@1")), "status")
                ),
                from = listOf(SqlTable.Ident("tb_subquery_order")),
                where = SqlExpr.Binary(
                    SqlExpr.Column(columnName = "status"),
                    SqlBinaryOperator.Equal,
                    SqlExpr.Parameter(SqlParameter.Named("status@1"))
                )
            )
        )
        val (sql, params) = Kronos.dataSource.table.buildCreateTableAsSelectTaskFromQuery(
            statement,
            mapOf("status@1" to "16"),
            fieldsMapCache[SubqueryOrder().__kClass as kotlin.reflect.KClass<com.kotlinorm.interfaces.KPojo>]!!
        )

        assertEquals(
            "CREATE TABLE IF NOT EXISTS `tb_subquery_order_archive` AS SELECT :status@1 AS status FROM `tb_subquery_order` WHERE `status` = :status@1",
            sql
        )
        assertEquals(mapOf("status@1" to 16), params)
    }

    @Test
    fun `derived selectable source renders create table as select`() {
        val (sql, params) = Kronos.dataSource.table
            .buildCreateTableAsSelectTask(
                SubqueryOrderArchive(),
                SubqueryOrder()
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
            "CREATE TABLE IF NOT EXISTS `tb_subquery_order_archive` AS SELECT `q`.`id`, `q`.`userId`, `q`.`status` FROM (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status) AS `q` WHERE `q`.`userId` = :userId",
            sql
        )
        assertEquals(mapOf("status" to 43, "userId" to 44), params)
    }

    @Test
    fun `join source renders create table as select`() {
        val joinQuery = SubqueryUser().join(SubqueryOrder()) { user, order ->
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
            .buildCreateTableAsSelectTask(SubqueryOrderArchive(), joinQuery)

        assertEquals(
            "CREATE TABLE IF NOT EXISTS `tb_subquery_order_archive` AS SELECT `tb_subquery_user`.`id` AS `id`, `tb_subquery_order`.`user_id` AS `userId`, `tb_subquery_order`.`status` AS `status` FROM `tb_subquery_user` LEFT JOIN `tb_subquery_order` ON `tb_subquery_user`.`id` = `tb_subquery_order`.`user_id` WHERE `tb_subquery_user`.`id` = :id",
            sql
        )
        assertEquals(mapOf("id" to 30), params)
    }

    @Test
    fun `union source renders create table as select`() {
        val unionQuery = union(
            SubqueryOrder()
                .select { [it.id, it.userId, it.status] }
                .where { it.status == 31 },
            SubqueryOrder()
                .select { [it.id, it.userId, it.status] }
                .where { it.status == 32 }
        )

        val (sql, params) = Kronos.dataSource.table
            .buildCreateTableAsSelectTask(SubqueryOrderArchive(), unionQuery)

        assertEquals(
            "CREATE TABLE IF NOT EXISTS `tb_subquery_order_archive` AS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status) UNION (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status@1)",
            sql
        )
        assertEquals(mapOf("status" to 31, "status@1" to 32), params)
    }

    @Test
    fun `union source params use transformer safe conversion`() {
        val unionQuery = union(
            SubqueryOrder()
                .select { [it.id, it.userId, it.status] }
                .where { it.status == 31 },
            SubqueryOrder()
                .select { [it.id, it.userId, it.status] }
                .where { it.status == 32 }
        )
        val parameterValues = mutableMapOf<String, Any?>()
        val parameterFields = mutableMapOf<String, Field>()
        val statement = Kronos.dataSource.table.buildCreateTableAsSelectStatement(
            SubqueryOrderArchive(),
            unionQuery,
            parameterValues,
            parameterFields
        )
        parameterValues["status"] = "31"
        parameterValues["status@1"] = "32"

        val (sql, params) = Kronos.dataSource.table.buildCreateTableAsSelectTaskFromQuery(
            statement,
            parameterValues,
            parameterFields
        )

        assertEquals(
            "CREATE TABLE IF NOT EXISTS `tb_subquery_order_archive` AS (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status) UNION (SELECT `id`, `user_id` AS `userId`, `status` FROM `tb_subquery_order` WHERE `tb_subquery_order`.`status` = :status@1)",
            sql
        )
        assertEquals(mapOf("status" to 31, "status@1" to 32), params)
    }

}
