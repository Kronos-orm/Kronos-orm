//package com.kotlinorm.orm
//
//import com.kotlinorm.Kronos
//import com.kotlinorm.beans.config.KronosCommonStrategy
//import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
//import com.kotlinorm.orm.beans.User
//import com.kotlinorm.orm.delete.delete
//import com.kotlinorm.utils.DateTimeUtil.currentDateTime
//import org.junit.jupiter.api.Test
//import kotlin.test.assertEquals
//
//class Delete {
//    init {
//        Kronos.apply {
//            fieldNamingStrategy = LineHumpNamingStrategy
//            tableNamingStrategy = LineHumpNamingStrategy
//        }
//    }
//
//    private val user = User(1)
//
//    @Test
//    fun testDelete() {
//        val (sql, paramMap) = user.delete().by { it.id }.build()
//        //delete from tb_user where id = 1
//        assertEquals("DELETE FROM `tb_user` WHERE `id` = :id", sql)
//        assertEquals(mapOf("id" to 1), paramMap)
//    }
//
//    @Test
//    fun testDelete2() {
//        val (sql, paramMap) = user.delete().where().build()
//        //delete from tb_user where id = 1 and deleted = 0
//        assertEquals("DELETE FROM `tb_user` WHERE `id` = :id", sql)
//        assertEquals(mapOf("id" to 1), paramMap)
//    }
//
//    @Test
//    fun testDelete3() {
//        val (sql, paramMap) = user.delete().where {
//            it.id > 10 && it.id < 100
//        }.build()
//        //delete from tb_user where id > 10 and id < 100
//        assertEquals("DELETE FROM `tb_user` WHERE `id` > :idMin AND `id` < :idMax", sql)
//        assertEquals(mapOf("idMin" to 10, "idMax" to 100), paramMap)
//    }
//
//    @Test
//    fun testDelete4() {
//        val (sql, paramMap) = user.delete().where {
//            it.id.eq
//        }.build()
//        //delete from tb_user where id > 10 and id < 100
//        assertEquals("DELETE FROM `tb_user` WHERE `id` = :id", sql)
//        assertEquals(mapOf("id" to 1), paramMap)
//    }
//
//    @Test
//    fun testDelete5() {
//        val (sql, paramMap) = user.delete().logic().where {
//            it.id.eq
//        }.build()
//        //delete from tb_user where id > 10 and id < 100
//        assertEquals(
//            "UPDATE `tb_user` SET `update_time` = :updateTimeNew, `deleted` = :deletedNew WHERE `id` = :id AND `deleted` = 0",
//            sql
//        )
//        assertEquals(mapOf("id" to 1, "updateTimeNew" to paramMap["updateTimeNew"], "deletedNew" to 1), paramMap)
//    }
//
//    @Test
//    fun testDelete6() {
//        val (sql, paramMap) = user.delete().where {
//            it.username == "John" && it.gender == 0
//        }.build()
//        // delete from tb_user where name = 'John' and email like 'john%'
//        assertEquals("DELETE FROM `tb_user` WHERE `username` = :username AND `gender` = :gender", sql)
//        assertEquals(mapOf("username" to "John", "gender" to 0), paramMap)
//    }
//}