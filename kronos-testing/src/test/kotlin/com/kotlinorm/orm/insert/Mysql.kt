package com.kotlinorm.orm.insert

import com.kotlinorm.Kronos
import com.kotlinorm.database.beans.MysqlUser
import com.kotlinorm.orm.beans.wrappers.SampleMysqlJdbcWrapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Insert {
    init {
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
            dataSource = { SampleMysqlJdbcWrapper }
        }
    }

    val user = MysqlUser(1)
    private val testUser = MysqlUser(1, "test")

    @Test
    fun testInsert() {
        val (sql, paramMap) = user.insert().build()
        assertEquals(
            "INSERT INTO `tb_user` (`id`, `create_time`, `update_time`, `deleted`) VALUES (:id, :createTime, :updateTime, :deleted)",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "createTime" to paramMap["createTime"],
                "updateTime" to paramMap["updateTime"],
                "deleted" to 0
            ), paramMap
        )
    }

}