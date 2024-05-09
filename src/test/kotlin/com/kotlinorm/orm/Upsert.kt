package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.beans.User
import com.kotlinorm.orm.upsert.upsert
import com.kotlinorm.orm.upsert.upsertExcept
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Upsert {
    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
        }
    }

    @Test
    fun testUpsert() {
        val user = User(1)

        val (sql, paramMap) = user.upsert()
            .on { it.id }.build()

        assertEquals("insert into tb_user (id) values (:id) on duplicate key Update username = :username", sql)
        assertEquals(mapOf("id" to 1, "username" to "123"), paramMap)
        // Update tb_user set username = '123' where id = 1 and delete = 0
    }

    @Test
    fun testUpsert2() {
        val user = User(1)

        val (sql, paramMap) = user.upsert()
            .on { it.id }.build()

        assertEquals("insert into tb_user (id) values (:id) on duplicate key Update gender = :gender", sql)
        assertEquals(mapOf("id" to 1, "gender" to 1), paramMap)
        // Update tb_user set gender = 1 where id = 1 and delete = 0

    }

    @Test
    fun testUpsert3() {
        val testUser = User(1, "test")

        val (sql, paramMap) = testUser.upsert { it.username }
            .on { it.id }.build()

        assertEquals("insert into tb_user (id) values (:id) on duplicate key Update username = :username", sql)
        assertEquals(mapOf("id" to 1, "username" to "test"), paramMap)
        // Update tb_user set username = 'test' where id = 1 and delete = 0
    }

    @Test
    fun testUpsert4() {
        val testUser = User(1, "test")

        val (sql, paramMap) = testUser.upsert { it.username }
            .onDuplicateKey().build()

        assertEquals("insert into tb_user (id) values (:id) on duplicate key Update username = :username", sql)
        assertEquals(mapOf("id" to 1, "username" to "test"), paramMap)
        // Update tb_user set username = 'test' where id = 1 and delete = 0
    }

    @Test
    fun testUpsert5() {
        val testUser = User(1, "test")
        val (sql, paramMap) = testUser.upsert { it.username }
            .onDuplicateKey().build()

        assertEquals("insert into tb_user (id) values (:id) on duplicate key Update username = :username", sql)
        assertEquals(mapOf("id" to 1, "username" to "test"), paramMap)
        // Update tb_user set username = 'test' where id = 1 and delete = 0
    }

    @Test
    fun testUpsertExcept() {
        val testUser = User(1, "test")
        val (sql, paramMap) = testUser.upsertExcept { it.username }
            .on { it.id }.build()

        assertEquals("insert into tb_user (id) values (:id) on duplicate key Update username = :username", sql)
        assertEquals(mapOf("id" to 1, "username" to "test"), paramMap)
        // Update tb_user set username = 'test' where id = 1

    }

}