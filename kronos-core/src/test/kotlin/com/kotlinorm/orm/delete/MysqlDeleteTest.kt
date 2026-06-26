package com.kotlinorm.orm.delete

import com.kotlinorm.beans.sample.databases.MysqlUser
import com.kotlinorm.orm.delete.DeleteClause.Companion.build
import com.kotlinorm.orm.delete.DeleteClause.Companion.logic
import com.kotlinorm.orm.delete.DeleteClause.Companion.where
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MysqlDeleteTest : MysqlTestBase() {

    private val user by lazy { MysqlUser(1) }
    private val testUser by lazy { MysqlUser(1, "username") }

    @Test
    fun testDelete() {
        val (sql, paramMap) = user.delete().logic(false).by { it.id }.build()
        //delete from tb_user where id = 1
        assertEquals("DELETE FROM `tb_user` WHERE `id` = :id", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testDelete2() {
        val (sql, paramMap) = user.delete().logic(false).build()
        //delete from tb_user where id = 1 and deleted = 0
        assertEquals("DELETE FROM `tb_user` WHERE `id` = :id", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testDelete3() {
        val (sql, paramMap) = user.delete().logic(false).where {
            it.id > 10 && it.id < 100
        }.build()
        //delete from tb_user where id > 10 and id < 100
        assertEquals("DELETE FROM `tb_user` WHERE `id` > :idMin AND `id` < :idMax", sql)
        assertEquals(mapOf("idMin" to 10, "idMax" to 100), paramMap)
    }

    @Test
    fun testDelete4() {
        val (sql, paramMap) = user.delete().logic(false).where {
            it.id.eq
        }.build()
        //delete from tb_user where id > 10 and id < 100
        assertEquals("DELETE FROM `tb_user` WHERE `id` = :id", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testDelete5() {
        val (sql, paramMap) = user.delete().where {
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
        val (sql, paramMap) = user.delete().logic(false).where {
            it.username == "John" && it.gender == 0
        }.build()
        // delete from tb_user where name = 'John' and email like 'john%'
        assertEquals("DELETE FROM `tb_user` WHERE `username` = :username AND `gender` = :gender", sql)
        assertEquals(mapOf("username" to "John", "gender" to 0), paramMap)
    }

    @Test
    fun testDeleteArray() {
        val (sql, _, list) = arrayOf(user, testUser).delete().logic(false).where {
            it.username == "John" && it.gender == 0
        }.build()
        // delete from tb_user where name = 'John' and email like 'john%'
        assertEquals("DELETE FROM `tb_user` WHERE `username` = :username AND `gender` = :gender", sql)
        assertEquals(
            arrayOf(
                mapOf("username" to "John", "gender" to 0),
                mapOf("username" to "John", "gender" to 0)
            ).toList(), list.map { it.paramMap.toMap() }

        )
    }

    @Test
    fun testDeleteIter() {
        val (sql, _, list) = listOf(user, testUser).delete().logic(false).where {
            it.username == "John" && it.gender == 0
        }.build()
        // delete from tb_user where name = 'John' and email like 'john%'
        assertEquals("DELETE FROM `tb_user` WHERE `username` = :username AND `gender` = :gender", sql)
        assertEquals(
            arrayOf(
                mapOf("username" to "John", "gender" to 0),
                mapOf("username" to "John", "gender" to 0)
            ).toList(), list.map { it.paramMap.toMap() }

        )
    }

    @Test
    fun testLogicDeleteDefault() {
        // Without logic(false), MysqlUser has @LogicDelete so delete should generate UPDATE
        val (sql, paramMap) = user.delete().build()
        assertTrue(sql.startsWith("UPDATE"), "Logic delete should generate UPDATE statement, got: $sql")
        assertTrue(sql.contains("`deleted` = :deletedNew"), "SQL should set deleted flag")
        assertTrue(sql.contains("`update_time` = :updateTimeNew"), "SQL should set update_time")
        assertTrue(sql.contains("AND `deleted` = 0"), "SQL should filter by deleted = 0")
        assertEquals(1, paramMap["id"])
        assertEquals(1, paramMap["deletedNew"])
    }

    @Test
    fun testLogicDeleteParamsContainTimestamp() {
        val (_, paramMap) = user.delete().where { it.id.eq }.build()
        // Logic delete should include updateTimeNew in params
        assertTrue(paramMap.containsKey("updateTimeNew"), "paramMap should contain updateTimeNew")
        assertTrue(paramMap.containsKey("deletedNew"), "paramMap should contain deletedNew")
        assertTrue(paramMap.containsKey("id"), "paramMap should contain id")
        assertEquals(1, paramMap["deletedNew"])
        assertEquals(1, paramMap["id"])
    }

    @Test
    fun testRealDeleteUsesDeleteFrom() {
        // logic(false) forces real DELETE FROM
        val (sql, _) = user.delete().logic(false).build()
        assertTrue(sql.startsWith("DELETE FROM"), "Real delete should use DELETE FROM, got: $sql")
        assertTrue(!sql.contains("UPDATE"), "Real delete should not contain UPDATE")
        assertTrue(sql.contains("`tb_user`"), "SQL should reference tb_user table")
    }

    @Test
    fun testDeleteWithUsernameCondition() {
        val (sql, paramMap) = testUser.delete().logic(false).where {
            it.username == "test_user"
        }.build()
        assertEquals("DELETE FROM `tb_user` WHERE `username` = :username", sql)
        assertEquals(mapOf("username" to "test_user"), paramMap)
    }

    @Test
    fun testLogicDeleteWithMultipleConditions() {
        val (sql, paramMap) = user.delete().where {
            it.username == "John" && it.gender == 0
        }.build()
        assertEquals(
            "UPDATE `tb_user` SET `update_time` = :updateTimeNew, `deleted` = :deletedNew WHERE `username` = :username AND `gender` = :gender AND `deleted` = 0",
            sql
        )
        assertEquals("John", paramMap["username"])
        assertEquals(0, paramMap["gender"])
        assertEquals(1, paramMap["deletedNew"])
    }

    @Test
    fun testDeleteSqlUsesBacktickQuoting() {
        val (sql, _) = user.delete().logic(false).build()
        assertTrue(sql.contains("`tb_user`"), "MySQL should use backtick quoting for table")
        assertTrue(sql.contains("`id`"), "MySQL should use backtick quoting for columns")
    }
}