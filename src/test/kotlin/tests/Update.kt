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
            }
            .by { it.id }
            .build()

        assertEquals("update tb_user set username = :username where id = :id", sql)
        assertEquals(mapOf("id" to 1, "username" to "123"), paramMap)
        // Update tb_user set username = '123' where id = 1

    }

    @Test
    fun testUpdate2() {
        val (sql, paramMap) = testUser.update { it.username + it.gender }
            .by { it.id }

        assertEquals("update tb_user set username = username + gender where id = :id and delete = 0", sql)
        assertEquals(mapOf("id" to 1), paramMap)
        // Update tb_user set username = 'test' where id = 1

    }

    @Test
    fun testUpdate3() {
        val (sql, paramMap) = testUser.updateExcept { it.username }
            .by { it.id }

        assertEquals("update tb_user set gender = gender where id = :id and delete = 0", sql)
        assertEquals(mapOf("id" to 1), paramMap)
        // Update tb_user set username = 'test' where id = 1
    }

    @Test
    fun testUpdate4() {
        val (sql, paramMap) = user.update()
            .set { it.gender = 1 }
            .by { it.id }

        assertEquals("update tb_user set gender = :gender where id = :id and delete = 0", sql)
        assertEquals(mapOf("id" to 1, "gender" to 1), paramMap)
        // Update tb_user set gender = 1 where id = 1
    }

    @Test
    fun testUpdate5() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id < 1 && it.id > 0 }

        assertEquals("update tb_user set id = id + username where id < 1 and id > 0 and delete = 0", sql)
        assertEquals(mapOf(), paramMap)
        // Update tb_user set id = 1, username = 1 where id < 1 and id > 0
    }
}