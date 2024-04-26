package tests

import tests.beans.User
import com.kotoframework.KotoApp
import com.kotoframework.beans.namingStrategy.LineHumpNamingStrategy
import com.kotoframework.orm.update.update
import com.kotoframework.orm.update.updateExcept
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Update {

    init {
        KotoApp.apply {
            LineHumpNamingStrategy().let {
                fieldNamingStrategy = it
                tableNamingStrategy = it
            }
        }
    }

    private val user = User(1)
    private val testUser = User(1, "test")

    @Test
    fun testUpdate() {
        val (sql, paramMap) = user.update()
            .set {
                it.username = "123"
                it.gender = 1
            }
            .by { it.id }
            .build()


        println(sql)
        println(paramMap)

        assertEquals("UPDATE tb_user SET username = :usernameNew, gender = :genderNew WHERE id = :id", sql)
        assertEquals(mapOf("id" to 1, "usernameNew" to "123", "genderNew" to 1), paramMap)
        // Update tb_user set username = '123' where id = 1

    }

    @Test
    fun testUpdate1_1() {
        val (sql, paramMap) = testUser.update()
            .set {
                it.username = "123"
                it.gender = 1
            }
            .by { it.id + it.username }
            .build()


        println(sql)
        println(paramMap)

        assertEquals("UPDATE tb_user SET username = :usernameNew, gender = :genderNew WHERE id = :id AND username = :username", sql)
        assertEquals(mapOf("id" to 1, "usernameNew" to "123", "genderNew" to 1, "username" to "test"), paramMap)
        // Update tb_user set username = '123' where id = 1

    }

    @Test
    fun testUpdate2() {
        val (sql, paramMap) = user.update { it.username + it.gender }
            .by { it.id }

        println(sql)
        println(paramMap)

        assertEquals("UPDATE tb_user SET username = :usernameNew, gender = :genderNew WHERE id = :id", sql)
        assertEquals(mapOf("id" to 1), paramMap)
        // Update tb_user set username = 'test' where id = 1

    }

    @Test
    fun testUpdate3() {
        val (sql, paramMap) = testUser.updateExcept { it.username }
            .by { it.id }

        println(sql)
        println(paramMap)

        assertEquals("UPDATE tb_user SET username = :usernameNew WHERE id = :id", sql)
        assertEquals(mapOf("id" to 1), paramMap)
        // Update tb_user set username = 'test' where id = 1
    }

    @Test
    fun testUpdate4() {
        val (sql, paramMap) = user.update()
            .set { it.gender = 1 }
            .by { it.id }

        assertEquals("UPDATE tb_user SET gender = :genderNew WHERE id = :id", sql)
        assertEquals(mapOf("id" to 1, "genderNew" to 1), paramMap)
        // Update tb_user set gender = 1 where id = 1
    }

    @Test
    fun testUpdate5() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id < 1 && it.id > 0 }.build()

        println(sql)
        println(paramMap)

        assertEquals("update tb_user set id = :id, username = :username where id < 1 and id > 0", sql)
        assertEquals(mapOf(), paramMap)
        // Update tb_user set id = 1, username = 1 where id < 1 and id > 0
    }
}