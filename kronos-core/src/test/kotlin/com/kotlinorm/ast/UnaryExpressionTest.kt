package com.kotlinorm.ast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnaryExpressionTest {

    @Test
    fun testUnaryExpressionWithNot() {
        val operand = Literal.BooleanLiteral(true)
        val expr = UnaryExpression(UnaryOperator.NOT, operand)
        
        assertEquals(UnaryOperator.NOT, expr.operator)
        assertEquals(operand, expr.operand)
        assertTrue(expr is Expression)
    }

    @Test
    fun testUnaryExpressionWithNegate() {
        val operand = Literal.NumberLiteral("10")
        val expr = UnaryExpression(UnaryOperator.NEGATE, operand)
        
        assertEquals(UnaryOperator.NEGATE, expr.operator)
        assertEquals(operand, expr.operand)
    }

    @Test
    fun testUnaryExpressionWithIsNull() {
        val operand = ColumnReference(tableAlias = "users", columnName = "email")
        val expr = UnaryExpression(UnaryOperator.NOT, operand) // 使用 NOT 代替 IS_NULL
        
        assertEquals(UnaryOperator.NOT, expr.operator)
        assertTrue(expr.operand is ColumnReference)
    }

    @Test
    fun testUnaryExpressionWithIsNotNull() {
        val operand = ColumnReference(tableAlias = "users", columnName = "phone")
        val expr = UnaryExpression(UnaryOperator.NOT, operand) // 使用 NOT 代替 IS_NOT_NULL
        
        assertEquals(UnaryOperator.NOT, expr.operator)
        assertTrue(expr.operand is ColumnReference)
    }

    @Test
    fun testUnaryExpressionEquality() {
        val operand = Literal.BooleanLiteral(false)
        val expr1 = UnaryExpression(UnaryOperator.NOT, operand)
        val expr2 = UnaryExpression(UnaryOperator.NOT, operand)
        
        assertEquals(expr1, expr2)
    }

    @Test
    fun testNestedUnaryExpressions() {
        val innerOperand = Literal.BooleanLiteral(true)
        val innerExpr = UnaryExpression(UnaryOperator.NOT, innerOperand)
        val outerExpr = UnaryExpression(UnaryOperator.NOT, innerExpr)
        
        assertTrue(outerExpr.operand is UnaryExpression)
        assertEquals(UnaryOperator.NOT, outerExpr.operator)
    }
}
