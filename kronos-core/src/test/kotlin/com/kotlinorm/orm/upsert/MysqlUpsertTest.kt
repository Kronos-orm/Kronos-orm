package com.kotlinorm.orm.upsert

import com.kotlinorm.Kronos
import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.orm.upsert.UpsertClause.Companion.execute
import com.kotlinorm.orm.upsert.UpsertClause.Companion.on
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class MysqlUpsertTest {

    object UpsertWrapper : SampleMysqlJdbcWrapper() {
        override fun update(task: KAtomicActionTask): Int {
            val sql = task.sql
            val paramMap = task.paramMap

            when (paramMap["id"]) {
                1 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `gender`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :gender, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 1,
                            "gender" to "0",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }

                2 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `gender`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :gender, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 2,
                            "gender" to "0",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }

                3 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `gender`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :gender, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 3,
                            "gender" to "0",
                            "username" to "test",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }

                4 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `deleted`, `create_time`, `update_time`) VALUES (:id, :username, :deleted, :createTime, :updateTime) ON DUPLICATE KEY UPDATE `username` = :username",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 4,
                            "username" to "test",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }

                5 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `deleted`, `create_time`, `update_time`) VALUES (:id, :username, :deleted, :createTime, :updateTime) ON DUPLICATE KEY UPDATE `username` = :username",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 5,
                            "username" to "test",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }

                6 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `gender`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :gender, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 6,
                            "gender" to "0",
                            "username" to "test",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }
                7 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `gender`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :gender, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 7,
                            "gender" to "0",
                            "username" to "test",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }
                8 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `gender`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :gender, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 8,
                            "gender" to "0",
                            "username" to "test2",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }
                9 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `gender`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :gender, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 9,
                            "gender" to "0",
                            "username" to "test",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }
                10 -> {
                    assertEquals(
                        "INSERT INTO `tb_user` (`id`, `username`, `score`, `gender`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :gender, :createTime, :updateTime, :deleted)",
                        sql
                    )
                    assertEquals(
                        mapOf(
                            "id" to 10,
                            "gender" to "0",
                            "username" to "test2",
                            "deleted" to 0,
                            "updateTime" to paramMap["updateTime"],
                            "createTime" to paramMap["createTime"]
                        ), paramMap
                    )
                }
            }
            return super.update(task)
        }

        override fun transact(block: () -> Any?): Any? {
            return block.invoke()
        }
    }

    init {
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
            dataSource = { UpsertWrapper }
        }
    }

    @Test
    fun testUpsert() {
        val user = MysqlUser(1)
        user.upsert { it.username }.on { it.id }.execute()
    }

    @Test
    fun testUpsert2() {
        val user = MysqlUser(2)
        user.upsert().on { it.id }.execute()
    }

    @Test
    fun testUpsert3() {
        val testUser = MysqlUser(3, "test")
        testUser.upsert { it.username }.on { it.id }.execute()
    }

    @Test
    fun testUpsert4() {
        val testUser = MysqlUser(4, "test")
        testUser.upsert { it.username }.onConflict().execute()
    }

    @Test
    fun testUpsert5() {
        val testUser = MysqlUser(5, "test")
        testUser.upsert { it.username }.onConflict().execute()
    }

    @Test
    fun testUpsertExcept() {
        val testUser = MysqlUser(6, "test")
        testUser.upsert { it - it.username }.on { it.id }.execute()
    }

    //  支持批量upsert
    @Test
    fun testBatchUpsert() {
        val testUser = MysqlUser(7, "test")
        val testUser2 = MysqlUser(8, "test2")
        listOf(testUser, testUser2).upsert { it.username }.on { it.id }.execute()
    }

    @Test
    fun testBatchUpsert1() {
        val testUser = MysqlUser(9, "test")
        val testUser2 = MysqlUser(10, "test2")
        arrayOf(testUser, testUser2).upsert { it.username }.execute()
    }

}