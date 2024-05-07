package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.insert.InsertClause.Companion.build
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.toAsyncTask
import org.junit.jupiter.api.Test
import com.kotlinorm.orm.beans.User
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