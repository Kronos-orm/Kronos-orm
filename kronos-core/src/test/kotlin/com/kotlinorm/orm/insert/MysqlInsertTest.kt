package com.kotlinorm.orm.insert

import com.kotlinorm.Kronos
import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.orm.insert.InsertClause.Companion.build
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper.Companion.sampleMysqlJdbcWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

class MysqlInsertTest {
    init {
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
            dataSource = { sampleMysqlJdbcWrapper }
        }
    }

    val user = MysqlUser(1)

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
                "gender" to "0",
                "createTime" to paramMap["createTime"],
                "updateTime" to paramMap["updateTime"],
                "deleted" to 0
            ), paramMap
        )
    }

    @Test
    fun testBatchInsert() {
        val tasks = (1..100).toList().map {
            MysqlUser(it)
        }.insert().build().atomicTasks
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
                    "gender" to "0",
                    "createTime" to paramMap["createTime"],
                    "updateTime" to paramMap["updateTime"],
                    "deleted" to 0
                ), paramMap
            )
        }
    }

}