package com.kotlinorm.orm.update

import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlAssignmentTarget
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UpdateClauseBehaviorTest : MysqlTestBase() {

    @Test
    fun `test build constructs Update syntax statement`() {
        val statement = TestUser(1, "test")
            .update()
            .set { it.username = "newName" }
            .by { it.id }
            .build()
            .singleStatement() as SqlDmlStatement.Update

        assertEquals("tb_user", statement.table.name)
        assertTrue(statement.setPairs.isNotEmpty(), "Update statement should have SET pairs")
        assertNotNull(statement.where, "Update statement should have WHERE clause")
    }

    @Test
    fun `test build includes SET clause assignments`() {
        val statement = TestUser(1, "test")
            .update()
            .set {
                it.username = "newName"
                it.gender = 1
            }
            .by { it.id }
            .build()
            .singleStatement() as SqlDmlStatement.Update

        assertTrue(statement.setPairs.size >= 2, "Should have at least 2 assignments")
        statement.setPairs.forEach { assignment ->
            assertTrue(assignment.target is SqlAssignmentTarget.Column)
            assertTrue(assignment.value is SqlExpr.Parameter)
            val parameter = (assignment.value as SqlExpr.Parameter).parameter
            assertTrue(parameter is SqlParameter.Named)
        }
    }

    @Test
    fun `test increment operation generates BinaryExpression with ADD`() {
        val statement = TestUser(1, "test")
            .update()
            .set {
                it.gender += 10
            }
            .by { it.id }
            .build()
            .singleStatement() as SqlDmlStatement.Update

        val genderAssignment = statement.setPairs.find { it.columnName() == "gender" }
        assertNotNull(genderAssignment, "Should have gender assignment")

        val binaryExpr = genderAssignment.value as? SqlExpr.Binary
        assertNotNull(binaryExpr, "Increment should generate SqlExpr.Binary")
        assertEquals(SqlBinaryOperator.Plus, binaryExpr.operator)
        assertTrue(binaryExpr.left is SqlExpr.Column)
        assertTrue(binaryExpr.right is SqlExpr.Parameter)
    }

    @Test
    fun `test decrement operation generates BinaryExpression with SUBTRACT`() {
        val statement = TestUser(1, "test")
            .update()
            .set {
                it.gender -= 5
            }
            .by { it.id }
            .build()
            .singleStatement() as SqlDmlStatement.Update

        val genderAssignment = statement.setPairs.find { it.columnName() == "gender" }
        assertNotNull(genderAssignment, "Should have gender assignment")

        val binaryExpr = genderAssignment.value as? SqlExpr.Binary
        assertNotNull(binaryExpr, "Decrement should generate SqlExpr.Binary")
        assertEquals(SqlBinaryOperator.Minus, binaryExpr.operator)
        assertTrue(binaryExpr.left is SqlExpr.Column)
        assertTrue(binaryExpr.right is SqlExpr.Parameter)
    }

    @Test
    fun `test WHERE clause is syntax expression`() {
        val statement = TestUser(1, "test")
            .update()
            .set { it.username = "newName" }
            .where { it.id == 1 && it.username == "test" }
            .build()
            .singleStatement() as SqlDmlStatement.Update

        val where = assertNotNull(statement.where, "Update statement should have WHERE clause")
        assertTrue(where is SqlExpr.Binary)
        assertEquals(SqlBinaryOperator.And, where.operator)
    }

    private fun com.kotlinorm.beans.task.KronosActionTask.singleStatement() =
        atomicTasks.single().statement

    private fun com.kotlinorm.syntax.statement.SqlUpdateSetPair.columnName(): String? =
        (target as? SqlAssignmentTarget.Column)?.identifier?.last
}
