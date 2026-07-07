package com.kotlinorm.orm.delete

import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeleteClauseBehaviorTest : MysqlTestBase() {

    private val user by lazy { TestUser(1) }

    @Test
    fun testBuildPhysicalDeleteStatementBasic() {
        val statement = user.delete()
            .logic(false)
            .by { it.id }
            .build()
            .singleStatement() as SqlDmlStatement.Delete

        assertEquals("tb_user", statement.table.name)
        assertNotNull(statement.where)
    }

    @Test
    fun testBuildPhysicalDeleteStatementWithWhere() {
        val statement = user.delete()
            .logic(false)
            .where {
                it.id > 10 && it.id < 100
            }
            .build()
            .singleStatement() as SqlDmlStatement.Delete

        assertNotNull(statement.where)
    }

    @Test
    fun testBuildLogicDeleteStatement() {
        val statement = user.delete()
            .where {
                it.id.eq
            }
            .build()
            .singleStatement() as SqlDmlStatement.Update

        assertEquals("tb_user", statement.table.name)
        assertTrue(statement.setPairs.isNotEmpty())
        assertNotNull(statement.where)
    }

    private fun com.kotlinorm.beans.task.KronosActionTask.singleStatement() =
        atomicTasks.single().statement
}
