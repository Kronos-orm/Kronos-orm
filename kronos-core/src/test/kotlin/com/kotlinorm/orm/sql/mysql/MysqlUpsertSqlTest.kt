package com.kotlinorm.orm.sql.mysql

import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.where
import com.kotlinorm.orm.union.union
import com.kotlinorm.orm.union.unionAll
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert

import com.kotlinorm.Kronos
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

class MysqlUpsertSqlTest : MysqlTestBase() {

    object UpsertWrapper : SampleMysqlJdbcWrapper() {
        override fun update(task: KAtomicActionTask): Int {
            val sql = task.sql
            val paramMap = task.paramMap

            when (paramMap["id"]) {
                1 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 1,
                            "username" to null,
                            "score" to null,
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }

                2 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 2,
                            "username" to null,
                            "score" to null,
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }

                3 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 3,
                            "username" to null,
                            "score" to null,
                            "username" to "test",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }

                4 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :createTime, :updateTime, :deleted) ON DUPLICATE KEY UPDATE `username` = :username, `update_time` = :updateTime, `deleted` = :deleted",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 4,
                            "username" to "test",
                            "score" to null,
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }

                5 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :createTime, :updateTime, :deleted) ON DUPLICATE KEY UPDATE `username` = :username, `update_time` = :updateTime, `deleted` = :deleted",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 5,
                            "username" to "test",
                            "score" to null,
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }

                6 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 6,
                            "score" to null,
                            "username" to "test",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }
                7 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 7,
                            "score" to null,
                            "username" to "test",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }
                8 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 8,
                            "score" to null,
                            "username" to "test2",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }
                9 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 9,
                            "score" to null,
                            "username" to "test",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }
                10 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 10,
                            "score" to null,
                            "username" to "test2",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }

                null -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`username`, `score`, `create_time`, `update_time`, `deleted`) VALUES (:username, :score, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "score" to null,
                            "username" to "test",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }
            }
            return super.update(task)
        }

        override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? {
            return TransactionScope().block()
        }
    }

    init {
        Kronos.dataSource = { UpsertWrapper }
    }

    @Test
    fun testUpsert() {
        val user = TestUser(1)
        user.upsert { it.username }.on { it.id }.execute()
    }

    @Test
    fun testUpsert2() {
        val user = TestUser(2)
        user.upsert().on { it.id }.execute()
    }

    @Test
    fun testUpsert3() {
        val testUser = TestUser(3, "test")
        testUser.upsert { it.username }.on { it.id }.execute()
    }

    @Test
    fun testUpsert4() {
        val testUser = TestUser(4, "test")
        testUser.upsert { it.username }.onConflict().execute()
    }

    @Test
    fun testUpsert5() {
        val testUser = TestUser(5, "test")
        testUser.upsert { it.username }.onConflict().execute()
    }

    @Test
    fun testUpsertExcept() {
        val testUser = TestUser(6, "test")
        testUser.upsert { it - it.username }.on { it.id }.execute()
    }

    //  支持批量upsert
    @Test
    fun testBatchUpsert() {
        val testUser = TestUser(7, "test")
        val testUser2 = TestUser(8, "test2")
        [testUser, testUser2].map { row ->
            row.upsert { it.username }.on { it.id }.build()
        }.merge().execute()
    }

    @Test
    fun testBatchUpsert1() {
        val testUser = TestUser(9, "test")
        val testUser2 = TestUser(10, "test2")
        arrayOf(testUser, testUser2).map { row ->
            row.upsert { it.username }.build()
        }.merge().execute()
    }

    @Test
    fun testUpsertEmptyId() {
        val testUser = TestUser(null, "test")
        testUser.upsert { it.username }.on { it.id }.execute()
    }
}
