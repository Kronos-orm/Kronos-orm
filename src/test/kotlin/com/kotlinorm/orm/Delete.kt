package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.beans.User
import com.kotlinorm.orm.delete.delete
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Delete {
    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
        }
    }

    private val user = User(1)

    @Test
    fun testDelete() {
        val (sql, paramMap) = user.delete().by { it.id }.build()
        //delete from tb_user where id = 1
        assertEquals("DELETE FROM `tb_user` WHERE `id` = :id", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testDelete2() {
        val (sql, paramMap) = user.delete().where().build()
        //delete from tb_user where id = 1 and deleted = 0
        assertEquals("DELETE FROM `tb_user` WHERE `id` = :id", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testDelete3() {
        val (sql, paramMap) = user.delete().where {
            it.id > 10 && it.id < 100
        }.build()
        //delete from tb_user where id > 10 and id < 100
        assertEquals("delete from `tb_user` where id > :idMin and id < :idMax", sql)
        assertEquals(mapOf("idMin" to 10, "idMax" to 100), paramMap)
    }

    @Test
    fun testDelete4() {
        val (sql, paramMap) = user.delete().where {
            it.id.eq
        }.build()
        //delete from tb_user where id > 10 and id < 100
        assertEquals("delete from `tb_user` where id > :idMin and id < :idMax", sql)
        assertEquals(mapOf("id" to 10, "idMax" to 100), paramMap)
    }

    @Test
    fun testDelete5() {
        val (sql, paramMap) = user.delete().logic().where {
            it.id.eq
        }.build()
        //delete from tb_user where id > 10 and id < 100
        assertEquals(
            "UPDATE `tb_user` SET `update_time` = :updateTimeNew, `delete` = :deleteNew WHERE `id` = :id AND `delete` = 0",
            sql
        )
        assertEquals(mapOf("id" to 1, "updateTime" to "024-05-08 10:23:25", "delete" to "1"), paramMap)
    }
}