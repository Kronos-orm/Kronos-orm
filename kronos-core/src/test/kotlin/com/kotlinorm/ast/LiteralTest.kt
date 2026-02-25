package com.kotlinorm.ast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LiteralTest {

    @Test
    fun testStringLiteral() {
        val literal = Literal.StringLiteral("test")
        assertEquals("test", literal.value)
        assertTrue(literal is Expression)
    }

    @Test
    fun testNumberLiteral() {
        val literal = Literal.NumberLiteral("123.45")
        assertEquals("123.45", literal.value)
        assertTrue(literal is Expression)
    }

    @Test
    fun testBooleanLiteralTrue() {
        val literal = Literal.BooleanLiteral(true)
        assertEquals(true, literal.value)
        assertTrue(literal is Expression)
    }

    @Test
    fun testBooleanLiteralFalse() {
        val literal = Literal.BooleanLiteral(false)
        assertEquals(false, literal.value)
        assertTrue(literal is Expression)
    }

    @Test
    fun testNullLiteral() {
        val literal = Literal.NullLiteral
        assertNotNull(literal)
        assertTrue(literal is Expression)
    }

    @Test
    fun testDateLiteral() {
        val literal = Literal.DateLiteral("2024-01-01")
        assertEquals("2024-01-01", literal.value)
        assertTrue(literal is Expression)
    }

    @Test
    fun testTimeLiteral() {
        val literal = Literal.TimeLiteral("12:30:45")
        assertEquals("12:30:45", literal.value)
        assertTrue(literal is Expression)
    }

    @Test
    fun testTimestampLiteral() {
        val literal = Literal.TimestampLiteral("2024-01-01 12:30:45")
        assertEquals("2024-01-01 12:30:45", literal.value)
        assertTrue(literal is Expression)
    }

    @Test
    fun testStringLiteralEquality() {
        val literal1 = Literal.StringLiteral("test")
        val literal2 = Literal.StringLiteral("test")
        val literal3 = Literal.StringLiteral("other")
        
        assertEquals(literal1, literal2)
        assertTrue(literal1 != literal3)
    }

    @Test
    fun testNumberLiteralEquality() {
        val literal1 = Literal.NumberLiteral("100")
        val literal2 = Literal.NumberLiteral("100")
        val literal3 = Literal.NumberLiteral("200")
        
        assertEquals(literal1, literal2)
        assertTrue(literal1 != literal3)
    }

    @Test
    fun testBooleanLiteralEquality() {
        val literal1 = Literal.BooleanLiteral(true)
        val literal2 = Literal.BooleanLiteral(true)
        val literal3 = Literal.BooleanLiteral(false)
        
        assertEquals(literal1, literal2)
        assertTrue(literal1 != literal3)
    }

    @Test
    fun testNullLiteralSingleton() {
        val literal1 = Literal.NullLiteral
        val literal2 = Literal.NullLiteral
        
        assertTrue(literal1 === literal2)
    }
}
