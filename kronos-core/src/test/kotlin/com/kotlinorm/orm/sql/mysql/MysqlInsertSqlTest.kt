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

import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class MysqlInsertSqlTest : MysqlTestBase() {

    private val user by lazy { TestUser(1) }

    @Test
    fun testInsert() {
        val (sql, paramMap) = user.insert().build()
        assertEquals(
            "INSERT INTO `tb_user` (`id`, `username`, `score`, `gender`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :gender, :createTime, :updateTime, :deleted)",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "username" to null,
                "score" to null,
                "gender" to 0,
                "createTime" to paramMap["createTime"],
                "updateTime" to paramMap["updateTime"],
                "deleted" to 0
            ), paramMap
        )
    }

    @Test
    fun testBatchInsert() {
        val tasks = (1..100).toList().map {
            TestUser(it).insert().build()
        }.merge().atomicTasks
        tasks.forEachIndexed { id, (sql, paramMap) ->
            assertEquals(
                "INSERT INTO `tb_user` (`id`, `username`, `score`, `gender`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :score, :gender, :createTime, :updateTime, :deleted)",
                sql
            )
            assertEquals(
                mapOf(
                    "id" to id + 1,
                    "username" to null,
                    "score" to null,
                    "gender" to 0,
                    "createTime" to paramMap["createTime"],
                    "updateTime" to paramMap["updateTime"],
                    "deleted" to 0
                ), paramMap
            )
        }
    }

}
