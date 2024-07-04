package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.delete.DeleteClause.Companion.build
import com.kotlinorm.orm.delete.DeleteClause.Companion.where
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.tableOperation.beans.MysqlUser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Delete {
    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
        }
    }

    private val user = MysqlUser(1)
    private val testUser = MysqlUser(1, "username")

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
        assertEquals("DELETE FROM `tb_user` WHERE `id` > :idMin AND `id` < :idMax", sql)
        assertEquals(mapOf("idMin" to 10, "idMax" to 100), paramMap)
    }

    @Test
    fun testDelete4() {
        val (sql, paramMap) = user.delete().where {
            it.id.eq
        }.build()
        //delete from tb_user where id > 10 and id < 100
        assertEquals("DELETE FROM `tb_user` WHERE `id` = :id", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testDelete5() {
        val (sql, paramMap) = user.delete().logic().where {
            it.id.eq
        }.build()
        //delete from tb_user where id > 10 and id < 100
        assertEquals(
            "UPDATE `tb_user` SET `update_time` = :updateTimeNew, `deleted` = :deletedNew WHERE `id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("id" to 1, "updateTimeNew" to paramMap["updateTimeNew"], "deletedNew" to 1), paramMap)
    }

    @Test
    fun testDelete6() {
        val (sql, paramMap) = user.delete().where {
            it.username == "John" && it.gender == 0
        }.build()
        // delete from tb_user where name = 'John' and email like 'john%'
        assertEquals("DELETE FROM `tb_user` WHERE `username` = :username AND `gender1` = :gender", sql)
        assertEquals(mapOf("username" to "John", "gender" to 0), paramMap)
    }

    @Test
    fun testDeleteArray() {
        val (sql, _, list) = arrayOf(user, testUser).delete().where {
            it.username == "John" && it.gender == 0
        }.build()
        // delete from tb_user where name = 'John' and email like 'john%'
        assertEquals("DELETE FROM `tb_user` WHERE `username` = :username AND `gender1` = :gender", sql)
        assertEquals(
            arrayOf(
                mapOf("username" to "John", "gender" to 0),
                mapOf("username" to "John", "gender" to 0)
            ).toList(), list.map { it.paramMap.toMap() }

        )
    }

    @Test
    fun testDeleteIter() {
        val (sql, _, list) = listOf(user, testUser).delete().where {
            it.username == "John" && it.gender == 0
        }.build()
        // delete from tb_user where name = 'John' and email like 'john%'
        assertEquals("DELETE FROM `tb_user` WHERE `username` = :username AND `gender1` = :gender", sql)
        assertEquals(
            arrayOf(
                mapOf("username" to "John", "gender" to 0),
                mapOf("username" to "John", "gender" to 0)
            ).toList(), list.map { it.paramMap.toMap() }

        )
    }
}