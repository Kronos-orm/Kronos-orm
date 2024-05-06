package tests

import com.kotoframework.Kronos
import com.kotoframework.beans.namingStrategy.LineHumpNamingStrategy
import com.kotoframework.orm.select.select
import org.junit.jupiter.api.Test
import tests.beans.User
import kotlin.test.assertEquals

class Select {
    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
        }
    }

    val user = User(1)

    @Test
    fun testSelect() {

        val (sql, paramMap) = user.select { it.id + it.username + it.gender }

        assertEquals("select id, username, gender from tb_user where deleted = 0", sql)
        assertEquals(mapOf(), paramMap)
    }

    @Test
    fun testSelect2() {
        val (sql, paramMap) = user.select { it.id }.page(1, 10).withTotal()

        assertEquals("select id from tb_user where deleted = 0 limit 10 offset 0", sql)
        assertEquals(mapOf(), paramMap)
    }

    @Test
    fun testSelect3() {
        val (sql, paramMap) = User()
            .select { it.username }
            .where { it.id > 10 }
            .distinct()
            .groupBy { it.id }
            .orderBy { it.id.desc + it.username.asc }
            .having { it.id.eq }

        assertEquals(
            "select distinct username from tb_user where id > :idMin group by id order by id desc, username asc",
            sql
        )
        assertEquals(mapOf("idMin" to 10, "id" to 1), paramMap)
    }
}