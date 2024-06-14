package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.beans.User
import com.kotlinorm.orm.upsert.UpsertClause.Companion.build
import com.kotlinorm.orm.upsert.UpsertClause.Companion.on
import com.kotlinorm.orm.upsert.upsert
import com.kotlinorm.orm.upsert.upsertExcept
import com.kotlinorm.orm.utils.TestWrapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Upsert {
    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
            dataSource = { TestWrapper }
        }
    }

    @Test
    fun testUpsert() {
        val user = User(1)

        val (_, paramMap, list) = user.upsert { it.username }.on { it.id }.build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `create_time`, `update_time`, `deleted`) SELECT :id, :createTime, :updateTime, :deleted FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = :deleted)",
            list[0].sql
        )
        assertEquals(
            "UPDATE `tb_user` SET `username` = :username WHERE `id` = :id AND `deleted` = :deleted",
            list[1].sql
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
        val user = User(1)

        val (_, paramMap, list) = user.upsert().on { it.id }.build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `create_time`, `update_time`, `deleted`) SELECT :id, :createTime, :updateTime, :deleted FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = :deleted)",
            list[0].sql
        )
        assertEquals(
            "UPDATE `tb_user` SET `id` = :id, `username` = :username, `gender` = :gender WHERE `id` = :id AND `deleted` = :deleted",
            list[1].sql
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
        val testUser = User(1, "test")

        val (_, paramMap, list) = testUser.upsert { it.username }.on { it.id }.build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `username`, `create_time`, `update_time`, `deleted`) SELECT :id, :username, :createTime, :updateTime, :deleted FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = :deleted)",
            list[0].sql
        )
        assertEquals(
            "UPDATE `tb_user` SET `username` = :username WHERE `id` = :id AND `deleted` = :deleted", list[1].sql
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
        val testUser = User(1, "test")

        val (sql, paramMap) = testUser.upsert { it.username }.onDuplicateKey().build()

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
        val testUser = User(1, "test")
        val (sql, paramMap) = testUser.upsert { it.username }.onDuplicateKey().build()

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
        val testUser = User(1, "test")
        val (_, paramMap, list) = testUser.upsertExcept { it.username }.on { it.id }.build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `username`, `create_time`, `update_time`, `deleted`) SELECT :id, :username, :createTime, :updateTime, :deleted FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = :deleted)",
            list[0].sql
        )
        assertEquals(
            "UPDATE `tb_user` SET `id` = :id, `gender` = :gender WHERE `id` = :id AND `deleted` = :deleted", list[1].sql
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
        val testUser = User(1, "test")
        val testUser2 = User(2, "test2")
        val (_, _, list) = listOf(testUser, testUser2).upsert { it.username }.on { it.id }.build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `username`, `create_time`, `update_time`, `deleted`) SELECT :id, :username, :createTime, :updateTime, :deleted FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = :deleted)",
            list[0].sql
        )
        assertEquals(
            "UPDATE `tb_user` SET `username` = :username WHERE `id` = :id AND `deleted` = :deleted", list[1].sql
        )
        val testUserMap = mapOf(
            "id" to 1,
            "username" to "test",
            "deleted" to 0,
            "updateTime" to list[0].paramMap["updateTime"],
            "createTime" to list[0].paramMap["createTime"]
        )
        val testUser2Map = mapOf(
            "id" to 2,
            "username" to "test2",
            "deleted" to 0,
            "updateTime" to list[1].paramMap["updateTime"],
            "createTime" to list[1].paramMap["createTime"]
        )
        assertEquals(listOf(
            testUserMap,
            testUserMap,
            testUser2Map,
            testUser2Map,
        ), list.map { it.paramMap })
    }

    @Test
    fun testBatchUpsert1() {
        val testUser = User(1, "test")
        val testUser2 = User(2, "test2")
        val (_, _, list) = arrayOf(testUser, testUser2).upsert { it.username }.on { it.id }.build()

        assertEquals(
            "INSERT INTO `tb_user` (`id`, `username`, `create_time`, `update_time`, `deleted`) SELECT :id, :username, :createTime, :updateTime, :deleted FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = :deleted)",
            list[0].sql
        )
        assertEquals(
            "UPDATE `tb_user` SET `username` = :username WHERE `id` = :id AND `deleted` = :deleted", list[1].sql
        )
        val testUserMap = mapOf(
            "id" to 1,
            "username" to "test",
            "deleted" to 0,
            "updateTime" to list[0].paramMap["updateTime"],
            "createTime" to list[0].paramMap["createTime"]
        )
        val testUser2Map = mapOf(
            "id" to 2,
            "username" to "test2",
            "deleted" to 0,
            "updateTime" to list[1].paramMap["updateTime"],
            "createTime" to list[1].paramMap["createTime"]
        )
        assertEquals(listOf(
            testUserMap,
            testUserMap,
            testUser2Map,
            testUser2Map,
        ), list.map { it.paramMap })
    }

}