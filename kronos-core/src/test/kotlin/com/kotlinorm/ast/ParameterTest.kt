package com.kotlinorm.ast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParameterTest {

    @Test
    fun testNamedParameter() {
        val param = Parameter.NamedParameter("userId")
        assertEquals("userId", param.name)
        assertTrue(param is Expression)
    }

    @Test
    fun testPositionalParameter() {
        val param = Parameter.PositionalParameter(1)
        assertEquals(1, param.index)
        assertTrue(param is Expression)
    }

    @Test
    fun testNamedParameterEquality() {
        val param1 = Parameter.NamedParameter("userId")
        val param2 = Parameter.NamedParameter("userId")
        val param3 = Parameter.NamedParameter("userName")
        
        assertEquals(param1, param2)
        assertTrue(param1 != param3)
    }

    @Test
    fun testPositionalParameterEquality() {
        val param1 = Parameter.PositionalParameter(1)
        val param2 = Parameter.PositionalParameter(1)
        val param3 = Parameter.PositionalParameter(2)
        
        assertEquals(param1, param2)
        assertTrue(param1 != param3)
    }

    @Test
    fun testMultipleNamedParameters() {
        val params = listOf(
            Parameter.NamedParameter("id"),
            Parameter.NamedParameter("name"),
            Parameter.NamedParameter("email")
        )
        
        assertEquals(3, params.size)
        assertEquals("id", params[0].name)
        assertEquals("name", params[1].name)
        assertEquals("email", params[2].name)
    }

    @Test
    fun testMultiplePositionalParameters() {
        val params = listOf(
            Parameter.PositionalParameter(0),
            Parameter.PositionalParameter(1),
            Parameter.PositionalParameter(2)
        )
        
        assertEquals(3, params.size)
        assertEquals(0, params[0].index)
        assertEquals(1, params[1].index)
        assertEquals(2, params[2].index)
    }
}
