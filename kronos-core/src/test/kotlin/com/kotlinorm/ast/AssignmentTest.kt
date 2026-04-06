package com.kotlinorm.ast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssignmentTest {

    @Test
    fun testAssignmentCreation() {
        val column = ColumnReference(tableAlias = "users", columnName = "name")
        val value = Literal.StringLiteral("John")
        val assignment = Assignment(column, value)
        
        assertEquals(column, assignment.column)
        assertEquals(value, assignment.value)
    }

    @Test
    fun testAssignmentWithParameter() {
        val column = ColumnReference(tableAlias = "users", columnName = "age")
        val value = Parameter.NamedParameter("userAge")
        val assignment = Assignment(column, value)
        
        assertTrue(assignment.column is ColumnReference)
        assertTrue(assignment.value is Parameter.NamedParameter)
    }

    @Test
    fun testAssignmentWithExpression() {
        val column = ColumnReference(tableAlias = "products", columnName = "price")
        val value = BinaryExpression(
            ColumnReference(tableAlias = "products", columnName = "price"),
            SqlOperator.MULTIPLY,
            Literal.NumberLiteral("1.1")
        )
        val assignment = Assignment(column, value)
        
        assertTrue(assignment.value is BinaryExpression)
    }

    @Test
    fun testAssignmentEquality() {
        val column = ColumnReference(tableAlias = "users", columnName = "status")
        val value = Literal.NumberLiteral("1")
        val assignment1 = Assignment(column, value)
        val assignment2 = Assignment(column, value)
        
        assertEquals(assignment1, assignment2)
    }

    @Test
    fun testMultipleAssignments() {
        val assignments = listOf(
            Assignment(ColumnReference(tableAlias = "users", columnName = "name"), Literal.StringLiteral("Alice")),
            Assignment(ColumnReference(tableAlias = "users", columnName = "age"), Literal.NumberLiteral("30")),
            Assignment(ColumnReference(tableAlias = "users", columnName = "email"), Parameter.NamedParameter("email"))
        )
        
        assertEquals(3, assignments.size)
        assertTrue(assignments[0].value is Literal.StringLiteral)
        assertTrue(assignments[1].value is Literal.NumberLiteral)
        assertTrue(assignments[2].value is Parameter.NamedParameter)
    }
}
