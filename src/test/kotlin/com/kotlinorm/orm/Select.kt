package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.select.select
import org.junit.jupiter.api.Test
import com.kotlinorm.orm.beans.User
import com.kotlinorm.orm.utils.TestWrapper
import kotlin.test.assertEquals

class Select {
    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
            dataSource = {TestWrapper}
        }
    }

    val user = User(1)

    @Test
    fun testSelect() {

        val (sql, paramMap) = user.select { it.id + it.username + it.gender }.build()

        assertEquals("SELECT `id`,`username`,`gender`,`create_time`,`update_time`,`deleted` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testSelect2() {
        val (sql, paramMap) = user.select { it.id }.page(1, 10)/*.withTotal()*/.build()

        assertEquals("select id from tb_user where deleted = 0 limit 10 offset 0", sql)
        assertEquals(mapOf(), paramMap)
    }

//    @Test
//    fun testSelect3() {
//        val (sql, paramMap) = User()
//            .select { it.username }
//            .where { it.id > 10 }
//            .distinct()
//            .groupBy { it.id }
//            .orderBy { it.id.desc + it.username.asc }
//            .having { it.id.eq }
//
//        assertEquals(
//            "select distinct username from tb_user where id > :idMin group by id order by id desc, username asc",
//            sql
//        )
//        assertEquals(mapOf("idMin" to 10, "id" to 1), paramMap)
//    }
}