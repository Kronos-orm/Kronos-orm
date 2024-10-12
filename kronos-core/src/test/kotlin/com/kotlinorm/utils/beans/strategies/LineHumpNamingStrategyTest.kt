package com.kotlinorm.utils.beans.strategies

import com.kotlinorm.beans.strategies.LineHumpNamingStrategy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LineHumpNamingStrategyTest {
    @Test
    fun testHump2Line() {
        val strategy = LineHumpNamingStrategy
        assertEquals("hello_world", strategy.k2db("helloWorld"))
        assertEquals("hello_world", strategy.k2db("HelloWorld"))
        assertEquals("hello_world_hello", strategy.k2db("helloWorldHello"))
        assertEquals("hello_world_hello", strategy.k2db("HelloWorldHello"))
        assertEquals("hello", strategy.k2db("hello"))
        assertEquals("hello", strategy.k2db("Hello"))
        assertEquals("h", strategy.k2db("h"))
        assertEquals("h", strategy.k2db("H"))
        assertEquals("3", strategy.k2db("3"))
        assertEquals("3hello", strategy.k2db("3Hello"))
        assertEquals("3ab_hello", strategy.k2db("3AbHello"))
        assertEquals("3abhello", strategy.k2db("3ABHello"))
        assertEquals("q3ab_hello", strategy.k2db("Q3AbHello"))
        assertEquals("q3abhello", strategy.k2db("Q3ABHello"))
        assertEquals("", strategy.k2db(""))
    }

    @Test
    fun testLine2Hump() {
        val strategy = LineHumpNamingStrategy
        assertEquals("helloWorld", strategy.db2k("hello_world"))
        assertEquals("HelloWorld", strategy.db2k("Hello_world"))
        assertEquals("helloWorldHello", strategy.db2k("hello_world_hello"))
        assertEquals("HelloWorldHello", strategy.db2k("Hello_world_hello"))
        assertEquals("hello", strategy.db2k("hello"))
        assertEquals("Hello", strategy.db2k("Hello"))
        assertEquals("h", strategy.db2k("h"))
        assertEquals("H", strategy.db2k("H"))
        assertEquals("3", strategy.db2k("3"))
        assertEquals("3Hello", strategy.db2k("3_hello"))
        assertEquals("3AbHello", strategy.db2k("3_ab_hello"))
        assertEquals("3Abhello", strategy.db2k("3_abhello"))
        assertEquals("q3AbHello", strategy.db2k("q3_ab_hello"))
        assertEquals("q3Abhello", strategy.db2k("q3_abhello"))
        assertEquals("", strategy.db2k(""))
    }
}