package com.kotlinorm.orm.upsert

import com.kotlinorm.Kronos
import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.orm.upsert.UpsertClause.Companion.build
import com.kotlinorm.orm.upsert.UpsertClause.Companion.on
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper.Companion.sampleMysqlJdbcWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

class MysqlUpsertTest {
    init {
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
            dataSource = { sampleMysqlJdbcWrapper }
        }
    }

    @Test
    fun testUpsert() {
        val user = MysqlUser(1)

        val (sql, paramMap) = user.upsert { it.username }
            .on { it.id }.build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `create_time`, `update_time`, `deleted`) SELECT :id, :createTime, :updateTime, :deleted FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = :deleted); UPDATE `tb_user` SET `username` = :username WHERE `id` = :id AND `deleted` = :deleted",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "deleted" to 0,
                "updateTime" to paramMap["updateTime"],
                "createTime" to paramMap["createTime"],
                "username" to null
            ), paramMap
        )
    }

    @Test
    fun testUpsert2() {
        val user = MysqlUser(1)

        val (sql, paramMap) = user.upsert()
            .on { it.id }.build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `create_time`, `update_time`, `deleted`) SELECT :id, :createTime, :updateTime, :deleted FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = :deleted); UPDATE `tb_user` SET `id` = :id, `username` = :username, `gender` = :gender WHERE `id` = :id AND `deleted` = :deleted",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "gender" to null,
                "username" to null,
                "deleted" to 0,
                "updateTime" to paramMap["updateTime"],
                "createTime" to paramMap["createTime"]
            ), paramMap
        )

    }

    @Test
    fun testUpsert3() {
        val testUser = MysqlUser(1, "test")

        val (sql, paramMap) = testUser.upsert { it.username }
            .on { it.id }.build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `username`, `create_time`, `update_time`, `deleted`) SELECT :id, :username, :createTime, :updateTime, :deleted FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = :deleted); UPDATE `tb_user` SET `username` = :username WHERE `id` = :id AND `deleted` = :deleted",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "username" to "test",
                "deleted" to 0,
                "updateTime" to paramMap["updateTime"],
                "createTime" to paramMap["createTime"]
            ), paramMap
        )
    }

    @Test
    fun testUpsert4() {
        val testUser = MysqlUser(1, "test")

        val (sql, paramMap) = testUser.upsert { it.username }
            .onConflict().build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `username`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :createTime, :updateTime, :deleted) ON DUPLICATE KEY UPDATE `username` = :username",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "username" to "test",
                "deleted" to 0,
                "updateTime" to paramMap["updateTime"],
                "createTime" to paramMap["createTime"]
            ), paramMap
        )
    }

    @Test
    fun testUpsert5() {
        val testUser = MysqlUser(1, "test")
        val (sql, paramMap) = testUser.upsert { it.username }
            .onConflict().build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `username`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :createTime, :updateTime, :deleted) ON DUPLICATE KEY UPDATE `username` = :username",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "username" to "test",
                "deleted" to 0,
                "updateTime" to paramMap["updateTime"],
                "createTime" to paramMap["createTime"]
            ), paramMap
        )
    }

    @Test
    fun testUpsertExcept() {
        val testUser = MysqlUser(1, "test")
        val (sql, paramMap) = testUser.upsert { it - it.username }
            .on { it.id }.build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `username`, `create_time`, `update_time`, `deleted`) SELECT :id, :username, :createTime, :updateTime, :deleted FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = :deleted); UPDATE `tb_user` SET `id` = :id, `gender` = :gender WHERE `id` = :id AND `deleted` = :deleted",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "username" to "test",
                "deleted" to 0,
                "updateTime" to paramMap["updateTime"],
                "createTime" to paramMap["createTime"],
                "gender" to null
            ), paramMap
        )
    }

    //  支持批量upsert
    @Test
    fun testBatchUpsert() {
        val testUser = MysqlUser(1, "test")
        val testUser2 = MysqlUser(2, "test2")
        val (sql, _, list) = listOf(testUser, testUser2).upsert { it.username }
            .on { it.id }.build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `username`, `create_time`, `update_time`, `deleted`) SELECT :id, :username, :createTime, :updateTime, :deleted FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = :deleted); UPDATE `tb_user` SET `username` = :username WHERE `id` = :id AND `deleted` = :deleted",
            sql
        )
        assertEquals(
            listOf(
                mapOf(
                    "id" to 1,
                    "username" to "test",
                    "deleted" to 0,
                    "updateTime" to list[0].paramMap["updateTime"],
                    "createTime" to list[0].paramMap["createTime"]
                ),
                mapOf(
                    "id" to 2,
                    "username" to "test2",
                    "deleted" to 0,
                    "updateTime" to list[1].paramMap["updateTime"],
                    "createTime" to list[1].paramMap["createTime"]
                )
            ), list.map { it.paramMap }
        )
    }

    @Test
    fun testBatchUpsert1() {
        val testUser = MysqlUser(1, "test")
        val testUser2 = MysqlUser(2, "test2")
        val (sql, _, list) = arrayOf(testUser, testUser2).upsert { it.username }
            .on { it.id }.build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `username`, `create_time`, `update_time`, `deleted`) SELECT :id, :username, :createTime, :updateTime, :deleted FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = :deleted); UPDATE `tb_user` SET `username` = :username WHERE `id` = :id AND `deleted` = :deleted",
            sql
        )
        assertEquals(
            listOf(
                mapOf(
                    "id" to 1,
                    "username" to "test",
                    "deleted" to 0,
                    "updateTime" to list[0].paramMap["updateTime"],
                    "createTime" to list[0].paramMap["createTime"]
                ),
                mapOf(
                    "id" to 2,
                    "username" to "test2",
                    "deleted" to 0,
                    "updateTime" to list[1].paramMap["updateTime"],
                    "createTime" to list[1].paramMap["createTime"]
                )
            ), list.map { it.paramMap }
        )
    }

}