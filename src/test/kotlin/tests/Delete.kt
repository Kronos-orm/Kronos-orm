package tests

import com.kotoframework.KotoApp
import com.kotoframework.beans.namingStrategy.LineHumpNamingStrategy
import com.kotoframework.orm.delete.delete
import org.junit.jupiter.api.Test
import tests.beans.User
import kotlin.test.assertEquals

class Delete {
    init {
        KotoApp.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
        }
    }
    private val user = User(1)

    @Test
    fun testDelete() {
        val (sql, paramMap) = user.delete().by { it.id }
        //delete from tb_user where id = 1
        assertEquals("delete from tb_user where id = :id", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testDelete2() {
        val (sql, paramMap) = user.delete().logic().where()
        //delete from tb_user where id = 1 and deleted = 0
        assertEquals("delete from tb_user where id = :id and deleted = 0", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testDelete3() {
        val (sql, paramMap) = user.delete().logic().where {
            it.id > 10 && it.id < 100
        }
        //delete from tb_user where id > 10 and id < 100 and deleted = 0
        assertEquals("delete from tb_user where id > :idMin and id < :idMax and deleted = 0", sql)
        assertEquals(mapOf("idMin" to 10, "idMax" to 100), paramMap)
    }
}