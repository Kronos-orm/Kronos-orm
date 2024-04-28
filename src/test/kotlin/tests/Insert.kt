package test.tests

import com.kotoframework.KotoApp
import com.kotoframework.beans.namingStrategy.LineHumpNamingStrategy
import com.kotoframework.orm.insert.insert
import org.junit.jupiter.api.Test
import tests.beans.User
import kotlin.test.assertEquals

class Insert {
    init {
        KotoApp.apply {
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