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

class MysqlDeleteSqlTest : MysqlTestBase() {

    private val user by lazy { TestUser(1) }
    private val testUser by lazy { TestUser(1, "username") }

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
        assertEquals("DELETE FROM `tb_user`", sql)
        assertEquals(emptyMap(), paramMap)
    }

    @Test
    fun testDelete3() {
        val (sql, paramMap) = user.delete().logic(false).where {
            it.id > 10 && it.id < 100
        }.build()
        //delete from tb_user where id > 10 and id < 100
        assertEquals("DELETE FROM `tb_user` WHERE `tb_user`.`id` > :idMin AND `tb_user`.`id` < :idMax", sql)
        assertEquals(mapOf("idMin" to 10, "idMax" to 100), paramMap)
    }

    @Test
    fun testDelete4() {
        val (sql, paramMap) = user.delete().logic(false).where {
            it.id.eq
        }.build()
        //delete from tb_user where id > 10 and id < 100
        assertEquals("DELETE FROM `tb_user` WHERE `tb_user`.`id` = :id", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testDelete5() {
        val (sql, paramMap) = user.delete().where {
            it.id.eq
        }.build()
        //delete from tb_user where id > 10 and id < 100
        assertEquals(
            "UPDATE `tb_user` SET `update_time` = :updateTimeNew, `deleted` = :deletedNew WHERE `tb_user`.`id` = :id AND `deleted` = 0",
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
        assertEquals("DELETE FROM `tb_user` WHERE `tb_user`.`username` = :username AND `tb_user`.`gender` = :gender", sql)
        assertEquals(mapOf("username" to "John", "gender" to 0), paramMap)
    }

    @Test
    fun testDeleteArray() {
        val (sql, _, list) = arrayOf(user, testUser).map { row ->
            row.delete().logic(false).where {
                it.username == "John" && it.gender == 0
            }.build()
        }.merge()
        // delete from tb_user where name = 'John' and email like 'john%'
        assertEquals("DELETE FROM `tb_user` WHERE `tb_user`.`username` = :username AND `tb_user`.`gender` = :gender", sql)
        assertEquals(
            arrayOf(
                mapOf("username" to "John", "gender" to 0),
                mapOf("username" to "John", "gender" to 0)
            ).toList(), list.map { it.paramMap.toMap() }

        )
    }

    @Test
    fun testDeleteIter() {
        val (sql, _, list) = [user, testUser].map { row ->
            row.delete().logic(false).where {
                it.username == "John" && it.gender == 0
            }.build()
        }.merge()
        // delete from tb_user where name = 'John' and email like 'john%'
        assertEquals("DELETE FROM `tb_user` WHERE `tb_user`.`username` = :username AND `tb_user`.`gender` = :gender", sql)
        assertEquals(
            arrayOf(
                mapOf("username" to "John", "gender" to 0),
                mapOf("username" to "John", "gender" to 0)
            ).toList(), list.map { it.paramMap.toMap() }

        )
    }

    @Test
    fun testLogicDeleteDefault() {
        // Without logic(false), TestUser has @LogicDelete so delete should generate UPDATE
        val (sql, paramMap) = user.delete().build()
        assertEquals(
            "UPDATE `tb_user` SET `update_time` = :updateTimeNew, `deleted` = :deletedNew WHERE `deleted` = 0",
            sql
        )
        assertEquals(mapOf("updateTimeNew" to paramMap["updateTimeNew"], "deletedNew" to 1), paramMap)
    }

    @Test
    fun testLogicDeleteParamsContainTimestamp() {
        val (_, paramMap) = user.delete().where { it.id.eq }.build()
        // Logic delete should include updateTimeNew in params
        assertEquals(mapOf("id" to 1, "updateTimeNew" to paramMap["updateTimeNew"], "deletedNew" to 1), paramMap)
    }

    @Test
    fun testRealDeleteUsesDeleteFrom() {
        // logic(false) forces real DELETE FROM
        val (sql, _) = user.delete().logic(false).build()
        assertEquals("DELETE FROM `tb_user`", sql)
    }

    @Test
    fun testDeleteWithUsernameCondition() {
        val (sql, paramMap) = testUser.delete().logic(false).where {
            it.username == "test_user"
        }.build()
        assertEquals("DELETE FROM `tb_user` WHERE `tb_user`.`username` = :username", sql)
        assertEquals(mapOf("username" to "test_user"), paramMap)
    }

    @Test
    fun testLogicDeleteWithMultipleConditions() {
        val (sql, paramMap) = user.delete().where {
            it.username == "John" && it.gender == 0
        }.build()
        assertEquals(
            "UPDATE `tb_user` SET `update_time` = :updateTimeNew, `deleted` = :deletedNew WHERE `tb_user`.`username` = :username AND `tb_user`.`gender` = :gender AND `deleted` = 0",
            sql
        )
        assertEquals("John", paramMap["username"])
        assertEquals(0, paramMap["gender"])
        assertEquals(1, paramMap["deletedNew"])
    }

    @Test
    fun testDeleteSqlUsesBacktickQuoting() {
        val (sql, _) = user.delete().logic(false).build()
        assertEquals("DELETE FROM `tb_user`", sql)
    }
}
