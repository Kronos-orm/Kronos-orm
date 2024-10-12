package com.kotlinorm.plugins.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class IrKDocUtilTest {

    @Test
    fun testExtractOneLineComment1() {
        val sourceCode = """
            // this is some comment
            val a: String? = null,
        """.trimIndent().split("\n")
        val declarationRange = 1..1
        assertEquals(listOf("val a: String? = null,"), sourceCode.slice(declarationRange))
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractOneLineComment2() {
        val sourceCode = """
            val a: String? = null, // this is some comment
            @ColumnType(VARCHAR, 254)
            val b: String? = null,
        """.trimIndent().split("\n")
        val declarationRange = 0..0
        assertEquals(listOf("val a: String? = null, // this is some comment"), sourceCode.slice(declarationRange))
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
        val declarationRange2 = 1..2
        assertEquals(listOf("@ColumnType(VARCHAR, 254)", "val b: String? = null,"), sourceCode.slice(declarationRange2))
        assertEquals(null, extractDeclarationComment(sourceCode, declarationRange2))
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
        assertEquals(
            "this is some comment,this is some comment",
            extractDeclarationComment(sourceCode, declarationRange)
        )
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
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
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
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractOneLineComment6() {
        val sourceCode = """
             // this is some comment
            @Column("a_a")
            val a: List<String>? = 
                listOf("123")
        """.trimIndent().split("\n").map { it.trim() }
        val declarationRange = 1..3
        assertEquals(
            listOf("@Column(\"a_a\")", "val a: List<String>? =", "listOf(\"123\")"), sourceCode.slice(declarationRange)
        )
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractMultiLineComment1() {
        val sourceCode = """
             /* this is some comment */
            val a: String? = null,
        """.trimIndent().split("\n")
        val declarationRange = 1..1
        assertEquals(listOf("val a: String? = null,"), sourceCode.slice(declarationRange))
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractMultiLineComment2() {
        val sourceCode = """
            val a: String? = null, /* this is some comment */
        """.trimIndent().split("\n")
        val declarationRange = 0..0
        assertEquals(listOf("val a: String? = null, /* this is some comment */"), sourceCode.slice(declarationRange))
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
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
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
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
        assertEquals(
            "this is some comment,this is some comment",
            extractDeclarationComment(sourceCode, declarationRange)
        )
    }

    @Test
    fun testExtractDataClassComment1() {
        val sourceCode = """
            // this is some comment
            @Table("a")
            data class A(
                // this is another comment
                val a: String? = null,
            )
        """.trimIndent().split("\n").map { it.trim() }
        val declarationRange = 1..2
        assertEquals(listOf("@Table(\"a\")", "data class A("), sourceCode.slice(declarationRange))
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractDataClassComment2() {
        val sourceCode = """
            @Table("a")
            data class A( // this is some comment
                // this is another comment
                val a: String? = null,
                val b: String? = null,
            )
        """.trimIndent().split("\n").map { it.trim() }
        val declarationRange = 0..1
        assertEquals(
            listOf("@Table(\"a\")", "data class A( // this is some comment"),
            sourceCode.slice(declarationRange)
        )
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))

        val declarationRange2 = 3..3
        assertEquals(
            listOf("val a: String? = null,"), sourceCode.slice(declarationRange2)
        )
        assertEquals("this is another comment", extractDeclarationComment(sourceCode, declarationRange2))

        val declarationRange3 = 4..4
        assertEquals(
            listOf("val b: String? = null,"), sourceCode.slice(declarationRange3)
        )
        assertEquals(null, extractDeclarationComment(sourceCode, declarationRange3))
    }

    @Test
    fun testExtractDataClassComment3() {
        val sourceCode = """
            @Table("a")
             // this is some comment
            data class A(
        """.trimIndent().split("\n").map { it.trim() }

        val declarationRange = 0..2
        assertEquals(
            listOf("@Table(\"a\")", "// this is some comment", "data class A("), sourceCode.slice(declarationRange)
        )
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractDataClassComment4() {
        val sourceCode = """
            @Table("a")
             /** this is some comment */
            data class A(
        """.trimIndent().split("\n").map { it.trim() }

        val declarationRange = 0..2
        assertEquals(
            listOf("@Table(\"a\")", "/** this is some comment */", "data class A("), sourceCode.slice(declarationRange)
        )
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractDataClassComment5() {
        val sourceCode = """
             /** this is some comment */
            @Table("a")
            data class A(
        """.trimIndent().split("\n").map { it.trim() }

        val declarationRange = 0..2
        assertEquals(
            listOf("/** this is some comment */", "@Table(\"a\")", "data class A("), sourceCode.slice(declarationRange)
        )
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
    }

    @Test
    fun testExtractDataClassComment6() {
        val sourceCode = """
            import com.kotlinorm.orm.annotations.Table
            
             /** 
              * this is some comment
              */
            @Table("a")
            data class A(
        """.trimIndent().split("\n").map { it.trim() }

        val declarationRange = 5..6
        assertEquals(
            listOf("@Table(\"a\")", "data class A("), sourceCode.slice(declarationRange)
        )
        assertEquals("this is some comment", extractDeclarationComment(sourceCode, declarationRange))
    }
}