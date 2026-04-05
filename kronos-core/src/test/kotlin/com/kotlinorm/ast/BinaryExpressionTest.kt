package com.kotlinorm.ast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BinaryExpressionTest {

    @Test
    fun testBinaryExpressionCreation() {
        val left = Literal.NumberLiteral("10")
        val right = Literal.NumberLiteral("20")
        val expr = BinaryExpression(left, SqlOperator.ADD, right)
        
        assertEquals(left, expr.left)
        assertEquals(SqlOperator.ADD, expr.operator)
        assertEquals(right, expr.right)
        assertTrue(expr is Expression)
    }

    @Test
    fun testBinaryExpressionEquality() {
        val left = Literal.NumberLiteral("10")
        val right = Literal.NumberLiteral("20")
        val expr1 = BinaryExpression(left, SqlOperator.ADD, right)
        val expr2 = BinaryExpression(left, SqlOperator.ADD, right)
        
        assertEquals(expr1, expr2)
    }

    @Test
    fun testBinaryExpressionWithDifferentOperators() {
        val left = Literal.NumberLiteral("10")
        val right = Literal.NumberLiteral("20")
        
        val plusExpr = BinaryExpression(left, SqlOperator.ADD, right)
        val minusExpr = BinaryExpression(left, SqlOperator.SUBTRACT, right)
        val multiplyExpr = BinaryExpression(left, SqlOperator.MULTIPLY, right)
        val divideExpr = BinaryExpression(left, SqlOperator.DIVIDE, right)
        
        assertTrue(plusExpr != minusExpr)
        assertTrue(minusExpr != multiplyExpr)
        assertTrue(multiplyExpr != divideExpr)
    }

    @Test
    fun testBinaryExpressionWithColumnReferences() {
        val left = ColumnReference(tableAlias = "users", columnName = "age")
        val right = Literal.NumberLiteral("18")
        val expr = BinaryExpression(left, SqlOperator.GREATER_THAN, right)
        
        assertTrue(expr.left is ColumnReference)
        assertTrue(expr.right is Literal.NumberLiteral)
        assertEquals(SqlOperator.GREATER_THAN, expr.operator)
    }

    @Test
    fun testNestedBinaryExpressions() {
        val a = Literal.NumberLiteral("1")
        val b = Literal.NumberLiteral("2")
        val c = Literal.NumberLiteral("3")
        
        val innerExpr = BinaryExpression(a, SqlOperator.ADD, b)
        val outerExpr = BinaryExpression(innerExpr, SqlOperator.MULTIPLY, c)
        
        assertTrue(outerExpr.left is BinaryExpression)
        assertEquals(c, outerExpr.right)
    }

    @Test
    fun testBinaryExpressionWithParameters() {
        val left = ColumnReference(tableAlias = "users", columnName = "name")
        val right = Parameter.NamedParameter("userName")
        val expr = BinaryExpression(left, SqlOperator.EQUAL, right)
        
        assertTrue(expr.left is ColumnReference)
        assertTrue(expr.right is Parameter.NamedParameter)
    }
}
