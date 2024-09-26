package com.kotlinorm

import com.kotlinorm.plugins.utils.extractPropertyComment
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CommentExtractTest {

    @Test
    fun testExtractOneLineComment1() {
        val sourceCode = """
            // this is some comment
            val a: String? = null,
        """.trimIndent().split("\n")
        val declarationRange = 1..1
        assertEquals(listOf("val a: String? = null,"), sourceCode.slice(declarationRange))
        assertEquals("this is some comment", extractPropertyComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractOneLineComment2() {
        val sourceCode = """
            val a: String? = null, // this is some comment
        """.trimIndent().split("\n")
        val declarationRange = 0..0
        assertEquals(listOf("val a: String? = null, // this is some comment"), sourceCode.slice(declarationRange))
        assertEquals("this is some comment", extractPropertyComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractOneLineComment3() {
        val sourceCode = """
            // this is some comment,
            // this is some comment
            val a: String? = null,
        """.trimIndent().split("\n")
        val declarationRange = 2..2
        assertEquals(listOf("val a: String? = null,"), sourceCode.slice(declarationRange))
        assertEquals("this is some comment,this is some comment", extractPropertyComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractOneLineComment4() {
        val sourceCode = """
            val a: List<String>? = // this is some comment
                listOf("123")
        """.trimIndent().split("\n").map { it.trim() }
        val declarationRange = 0..1
        assertEquals(
            listOf("val a: List<String>? = // this is some comment", "listOf(\"123\")"),
            sourceCode.slice(declarationRange)
        )
        assertEquals("this is some comment", extractPropertyComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractOneLineComment5() {
        val sourceCode = """
            val a: List<String>? = 
                listOf("123") // this is some comment
        """.trimIndent().split("\n").map { it.trim() }
        val declarationRange = 0..1
        assertEquals(
            listOf("val a: List<String>? =", "listOf(\"123\") // this is some comment"),
            sourceCode.slice(declarationRange)
        )
        assertEquals("this is some comment", extractPropertyComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractMultiLineComment1() {
        val sourceCode = """
             /* this is some comment */
            val a: String? = null,
        """.trimIndent().split("\n")
        val declarationRange = 1..1
        assertEquals(listOf("val a: String? = null,"), sourceCode.slice(declarationRange))
        assertEquals("this is some comment", extractPropertyComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractMultiLineComment2() {
        val sourceCode = """
            val a: String? = null, /* this is some comment */
        """.trimIndent().split("\n")
        val declarationRange = 0..0
        assertEquals(listOf("val a: String? = null, /* this is some comment */"), sourceCode.slice(declarationRange))
        assertEquals("this is some comment", extractPropertyComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractMultiLineComment3() {
        val sourceCode = """
            /* 
             * this is some comment
             */
            val a: String? = null,
        """.trimIndent().split("\n")
        val declarationRange = 3..3
        assertEquals(listOf("val a: String? = null,"), sourceCode.slice(declarationRange))
        assertEquals("this is some comment", extractPropertyComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractMultiLineComment4() {
        val sourceCode = """
            /* 
               this is some comment,
             * this is some comment
             */
            val a: String? = null,
        """.trimIndent().split("\n")
        val declarationRange = 4..4
        assertEquals(listOf("val a: String? = null,"), sourceCode.slice(declarationRange))
        assertEquals("this is some comment,this is some comment", extractPropertyComment(sourceCode, declarationRange))
    }
}