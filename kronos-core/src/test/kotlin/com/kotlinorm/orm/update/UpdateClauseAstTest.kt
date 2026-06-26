package com.kotlinorm.orm.update

import com.kotlinorm.ast.Assignment
import com.kotlinorm.ast.BinaryExpression
import com.kotlinorm.ast.ColumnReference
import com.kotlinorm.ast.Parameter
import com.kotlinorm.ast.SqlOperator
import com.kotlinorm.ast.UpdateStatement
import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test UpdateClause AST generation
 * 
 * Validates Requirements 5.1, 5.2, 5.4:
 * - UpdateClause constructs UpdateStatement AST internally
 * - toStatement() returns complete UpdateStatement with SET and WHERE clauses
 * - Increment/decrement operations generate appropriate BinaryExpression nodes
 */
class UpdateClauseAstTest : MysqlTestBase() {

    @Test
    fun `test toStatement constructs UpdateStatement AST`() {
        // Requirement 5.1: UpdateClause SHALL construct an UpdateStatement AST internally
        val user = MysqlUser(1, "test")
        val updateClause = user.update()
            .set { it.username = "newName" }
            .by { it.id }

        val statement = updateClause.toStatement(null)

        // Verify statement is UpdateStatement
        assertTrue(statement is UpdateStatement, "toStatement should return UpdateStatement")
        
        // Verify table reference
        assertNotNull(statement.table, "UpdateStatement should have table reference")
        
        // Verify assignments exist
        assertTrue(statement.assignments.isNotEmpty(), "UpdateStatement should have assignments")
        
        // Verify WHERE clause exists
        assertNotNull(statement.where, "UpdateStatement should have WHERE clause")
    }

    @Test
    fun `test toStatement includes SET clause assignments`() {
        // Requirement 5.2: toStatement() SHALL return complete UpdateStatement with SET and WHERE clauses
        val user = MysqlUser(1, "test")
        val updateClause = user.update()
            .set { 
                it.username = "newName"
                it.gender = 1
            }
            .by { it.id }

        val statement = updateClause.toStatement(null)

        // Verify multiple assignments
        assertTrue(statement.assignments.size >= 2, "Should have at least 2 assignments (username, gender)")
        
        // Verify assignments are Assignment nodes
        statement.assignments.forEach { assignment ->
            assertTrue(assignment is Assignment, "Each assignment should be Assignment node")
            assertTrue(assignment.column is ColumnReference, "Assignment column should be ColumnReference")
            assertTrue(assignment.value is Parameter, "Assignment value should be Parameter for simple assignments")
        }
    }

    @Test
    fun `test increment operation generates BinaryExpression with ADD`() {
        // Requirement 5.4: increment SHALL generate BinaryExpression(column + value)
        val user = MysqlUser(1, "test")
        val updateClause = user.update()
            .set { 
                it.gender += 10  // Increment operation
            }
            .by { it.id }

        val statement = updateClause.toStatement(null)

        // Find the gender assignment
        val genderAssignment = statement.assignments.find { 
            it.column.columnName == "gender" 
        }
        
        assertNotNull(genderAssignment, "Should have gender assignment")
        
        // Verify it's a BinaryExpression with ADD operator
        assertTrue(
            genderAssignment.value is BinaryExpression,
            "Increment should generate BinaryExpression"
        )
        
        val binaryExpr = genderAssignment.value as BinaryExpression
        assertEquals(
            SqlOperator.ADD,
            binaryExpr.operator,
            "Increment should use ADD operator"
        )
        
        // Verify left operand is column reference
        assertTrue(
            binaryExpr.left is ColumnReference,
            "Left operand should be column reference"
        )
        
        // Verify right operand is parameter
        assertTrue(
            binaryExpr.right is Parameter,
            "Right operand should be parameter"
        )
    }

    @Test
    fun `test decrement operation generates BinaryExpression with SUBTRACT`() {
        // Requirement 5.4: decrement SHALL generate BinaryExpression(column - value)
        val user = MysqlUser(1, "test")
        val updateClause = user.update()
            .set { 
                it.gender -= 5  // Decrement operation
            }
            .by { it.id }

        val statement = updateClause.toStatement(null)

        // Find the gender assignment
        val genderAssignment = statement.assignments.find { 
            it.column.columnName == "gender" 
        }
        
        assertNotNull(genderAssignment, "Should have gender assignment")
        
        // Verify it's a BinaryExpression with SUBTRACT operator
        assertTrue(
            genderAssignment.value is BinaryExpression,
            "Decrement should generate BinaryExpression"
        )
        
        val binaryExpr = genderAssignment.value as BinaryExpression
        assertEquals(
            SqlOperator.SUBTRACT,
            binaryExpr.operator,
            "Decrement should use SUBTRACT operator"
        )
        
        // Verify left operand is column reference
        assertTrue(
            binaryExpr.left is ColumnReference,
            "Left operand should be column reference"
        )
        
        // Verify right operand is parameter
        assertTrue(
            binaryExpr.right is Parameter,
            "Right operand should be parameter"
        )
    }

    @Test
    fun `test WHERE clause is Expression`() {
        // Requirement 5.2: UpdateStatement should have WHERE clause as Expression
        val user = MysqlUser(1, "test")
        val updateClause = user.update()
            .set { it.username = "newName" }
            .where { it.id == 1 && it.username == "test" }

        val statement = updateClause.toStatement(null)

        // Verify WHERE clause exists and is Expression
        assertNotNull(statement.where, "UpdateStatement should have WHERE clause")
        
        // WHERE clause with AND should be BinaryExpression
        assertTrue(
            statement.where is BinaryExpression,
            "WHERE clause with AND should be BinaryExpression"
        )
        
        val whereExpr = statement.where as BinaryExpression
        assertEquals(
            SqlOperator.AND,
            whereExpr.operator,
            "Multiple conditions should be combined with AND"
        )
    }
}
