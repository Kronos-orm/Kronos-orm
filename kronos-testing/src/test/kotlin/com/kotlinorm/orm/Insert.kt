package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.beans.User
import com.kotlinorm.orm.insert.insert
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Insert {
    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
        }
    }

    val user = User(1)
    private val testUser = User(1, "test")

    @Test
    fun testInsert() {
        val (sql, paramMap) = user.insert().build()
        assertEquals(
            "INSERT INTO `tb_user` (`id`, `username`, `gender`, `create_time`, `update_time`, `deleted`) VALUES (:id, :username, :gender, :createTime, :updateTime, :deleted)",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "username" to null,
                "gender" to null,
                "createTime" to paramMap["createTime"],
                "updateTime" to paramMap["updateTime"],
                "deleted" to 0
            ), paramMap
        )
    }

}