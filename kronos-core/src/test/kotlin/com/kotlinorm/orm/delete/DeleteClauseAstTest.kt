package com.kotlinorm.orm.delete

import com.kotlinorm.ast.DeleteStatement
import com.kotlinorm.ast.TableName
import com.kotlinorm.beans.sample.databases.MysqlUser
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for DeleteClause AST-based SQL generation
 */
class DeleteClauseAstTest : MysqlTestBase() {

    private val user by lazy { MysqlUser(1) }

    @Test
    fun testToStatementBasic() {
        val deleteClause = user.delete().logic(false).by { it.id }
        val statement = deleteClause.toStatement()

        // Verify statement structure
        assertTrue(statement is DeleteStatement)
        assertTrue(statement.table is TableName)
        assertEquals("tb_user", (statement.table as TableName).table)
        assertNotNull(statement.where)
    }

    @Test
    fun testToStatementWithWhere() {
        val deleteClause = user.delete().logic(false).where {
            it.id > 10 && it.id < 100
        }
        val statement = deleteClause.toStatement()

        // Verify statement structure
        assertTrue(statement is DeleteStatement)
        assertNotNull(statement.where)
    }

    @Test
    fun testToStatementWithLogicDelete() {
        val deleteClause = user.delete().where {
            it.id.eq
        }
        val statement = deleteClause.toStatement()

        // Verify statement structure
        assertTrue(statement is DeleteStatement)
        assertNotNull(statement.where)
        // Logic delete should merge conditions with AND
    }
}
