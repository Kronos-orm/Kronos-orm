package test.tests

import com.kotoframework.Kronos
import com.kotoframework.beans.namingStrategy.LineHumpNamingStrategy
import com.kotoframework.orm.insert.InsertClause.Companion.build
import com.kotoframework.orm.insert.insert
import com.kotoframework.orm.update.UpdateClause.Companion.build
import com.kotoframework.orm.update.UpdateClause.Companion.where
import com.kotoframework.orm.update.update
import com.kotoframework.utils.execute
import com.kotoframework.utils.toAsyncTask
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
    private val testUser = User(1, "test")

    @Test
    fun testInsert() {
        val (sql, paramMap) = user.insert().build()
        assertEquals("INSERT INTO `tb_user` (`id`) VALUES (:id)", sql)
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testNew(){
        arrayOf(user, testUser).insert().build()

        // 组批和任务队列，此测试未来需拆开
        listOf(
            listOf(user, testUser).insert().build(),
            testUser.insert().build(),
            testUser.insert().build(),
            testUser.insert().build()
        )
            .toAsyncTask()
            .execute()
    }

}