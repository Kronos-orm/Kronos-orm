package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.beans.User
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.utils.TestWrapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Select {
    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
            dataSource = { TestWrapper }
        }
    }

    val user = User(1)

    @Test
    fun testSelect() {

        val (sql, paramMap) = user.select { it.id + it.username + it.gender + "123" }.build()

        assertEquals("SELECT `id`, `username`, `gender`, `123` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testSelect2() {
        val (sql, paramMap) = user.select { it.id }.page(1, 10)/*.withTotal()*/.build()

        assertEquals(mapOf("id" to 1), paramMap)
        assertEquals("select id from tb_user where deleted = 0 limit 10 offset 0", sql)
    }

    @Test
    fun testSelect3() {
        val (sql, paramMap) = User()
            .select { it.username + it.gender }
            .where { it.id > 10 }
            .distinct()
            .groupBy { it.id + it.gender }
            //   .orderBy { it.id.desc + it.username.asc }
            .having { it.id.eq }
            .build()

        assertEquals(mapOf("idMin" to 10), paramMap)
        assertEquals(
            "SELECT DISTINCT username FROM tb_user WHERE id > :idMin GROUP BY id ORDER BY id DESC, username ASC HAVING id = :id",
            sql
        )
    }

    @Test
    fun testSelect4() {

        val (sql, paramMap) = user.select { it.id + it.username + it.gender + "COUNT(1) as `count`" }.build()

        assertEquals("SELECT `id`, `username`, `gender`, `123` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }
}