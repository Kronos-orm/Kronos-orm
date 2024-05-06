package test.tests

import com.kotoframework.Kronos
import com.kotoframework.beans.namingStrategy.LineHumpNamingStrategy
import com.kotoframework.orm.insert.insert
import org.junit.jupiter.api.Test
import tests.beans.User
import kotlin.test.assertEquals

class Insert {
    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
        }
    }
    val user = User(1)

    @Test
    fun testInsert() {
        val (sql, paramMap) = user.insert()
        assertEquals(sql, "insert into tb_user (id) values (:id)")
        assertEquals(paramMap, mapOf("id" to 1))
    }
}