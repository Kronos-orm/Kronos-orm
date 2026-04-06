package com.kotlinorm.orm.insert

import com.kotlinorm.ast.ColumnReference
import com.kotlinorm.ast.Parameter
import com.kotlinorm.ast.TableName
import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for InsertClause AST generation (toStatement method)
 */
class InsertClauseAstTest : MysqlTestBase() {

    @Test
    fun testToStatementGeneratesCorrectAst() {
        val user = MysqlUser(1)
        val insertClause = user.insert()
        
        // Call toStatement to get the AST
        val statement = insertClause.toStatement()
        
        // Verify table reference
        assertNotNull(statement.table)
        assertTrue(statement.table is TableName)
        val tableName = statement.table as TableName
        assertEquals("tb_user", tableName.table)
        
        // Verify columns are present
        assertTrue(statement.columns.isNotEmpty())
        assertTrue(statement.columns.all { it is ColumnReference })
        
        // Verify values are parameters
        assertTrue(statement.values.isNotEmpty())
        assertTrue(statement.values.all { it is Parameter.NamedParameter })
        
        // Verify columns and values have same size
        assertEquals(statement.columns.size, statement.values.size)
        
        // Verify no conflict resolver (not supported in InsertClause)
        assertEquals(null, statement.conflictResolver)
    }

    @Test
    fun testToStatementIncludesExpectedColumns() {
        val user = MysqlUser(1)
        val insertClause = user.insert()
        
        val statement = insertClause.toStatement()
        
        // Expected columns for MysqlUser
        val expectedColumns = setOf("id", "username", "score", "gender", "create_time", "update_time", "deleted")
        val actualColumns = statement.columns.map { it.columnName }.toSet()
        
        assertEquals(expectedColumns, actualColumns)
    }
}
