package com.kotlinorm.functions

import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.functions.bundled.exts.MathFunctions.mod
import com.kotlinorm.functions.bundled.exts.MathFunctions.rand
import com.kotlinorm.functions.bundled.exts.MathFunctions.trunc
import com.kotlinorm.functions.bundled.exts.StringFunctions.join
import com.kotlinorm.functions.bundled.exts.StringFunctions.left
import com.kotlinorm.functions.bundled.exts.StringFunctions.repeat
import com.kotlinorm.functions.bundled.exts.StringFunctions.right
import com.kotlinorm.orm.select.select
import com.kotlinorm.testutils.OracleTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Oracle-specific function tests
 * Tests functions that have different SQL generation for Oracle
 */
class OracleFunctionTest : OracleTestBase() {
    private val user by lazy { TestUser(1) }

    // Math functions with Oracle-specific behavior
    @Test
    fun testRandInSelect() {
        val (sql, _) = user.select { f.rand().alias("rand") }.build()
        assertEquals("""SELECT DBMS_RANDOM.VALUE AS RAND FROM "TB_USER" WHERE "DELETED" = 0""", sql)
    }

    @Test
    fun testModInSelect() {
        val (sql, _) = user.select { (it.score % 2).alias("mod") }.build()
        assertEquals("""SELECT MOD("SCORE", 2) AS MOD FROM "TB_USER" WHERE "DELETED" = 0""", sql)
    }

    @Test
    fun testModInWhere() {
        val (sql, _) = user.select { it.id }.where { it.score % 2 == 0 }.build()
        assertEquals("""SELECT "ID" FROM "TB_USER" WHERE MOD("SCORE", 2) = :mod AND "DELETED" = 0""", sql)
    }

    @Test
    fun testTruncInSelect() {
        val (sql, _) = user.select { f.trunc(it.score, 2).alias("trunc") }.build()
        assertEquals("""SELECT TRUNC("SCORE", 2) AS TRUNC FROM "TB_USER" WHERE "DELETED" = 0""", sql)
    }

    @Test
    fun testTruncInWhere() {
        val (sql, _) = user.select { it.id }.where { f.trunc(it.score, 0) > 50 }.build()
        assertEquals("""SELECT "ID" FROM "TB_USER" WHERE TRUNC("SCORE", 0) > :truncMin AND "DELETED" = 0""", sql)
    }

    // String functions with Oracle-specific behavior
    @Test
    fun testJoinInSelect() {
        val (sql, _) = user.select { f.join(", ", it.username, it.username).alias("join") }.build()
        assertEquals("""SELECT "USERNAME" || ', ' || "USERNAME" AS JOIN FROM "TB_USER" WHERE "DELETED" = 0""", sql)
    }

    @Test
    fun testJoinInWhere() {
        val (sql, _) = user.select { it.id }.where { f.join("-", it.username, it.username) == "admin-admin" }.build()
        assertEquals("""SELECT "ID" FROM "TB_USER" WHERE "USERNAME" || '-' || "USERNAME" = :join AND "DELETED" = 0""", sql)
    }

    @Test
    fun testLeftInSelect() {
        val (sql, _) = user.select { f.left(it.username, 5).alias("left") }.build()
        assertEquals("""SELECT SUBSTR("USERNAME", 1, 5) AS LEFT FROM "TB_USER" WHERE "DELETED" = 0""", sql)
    }

    @Test
    fun testLeftInWhere() {
        val (sql, _) = user.select { it.id }.where { f.left(it.username, 3) == "adm" }.build()
        assertEquals("""SELECT "ID" FROM "TB_USER" WHERE SUBSTR("USERNAME", 1, 3) = :left AND "DELETED" = 0""", sql)
    }

    @Test
    fun testRightInSelect() {
        val (sql, _) = user.select { f.right(it.username, 5).alias("right") }.build()
        assertEquals("""SELECT SUBSTR("USERNAME", -5) AS RIGHT FROM "TB_USER" WHERE "DELETED" = 0""", sql)
    }

    @Test
    fun testRightInWhere() {
        val (sql, _) = user.select { it.id }.where { f.right(it.username, 3) == "min" }.build()
        assertEquals("""SELECT "ID" FROM "TB_USER" WHERE SUBSTR("USERNAME", -3) = :right AND "DELETED" = 0""", sql)
    }

    @Test
    fun testRepeatInSelect() {
        val (sql, _) = user.select { f.repeat(it.username, 3).alias("repeat") }.build()
        assertEquals("""SELECT RPAD("USERNAME", 3 * LENGTH("USERNAME"), "USERNAME") AS REPEAT FROM "TB_USER" WHERE "DELETED" = 0""", sql)
    }

    @Test
    fun testRepeatInWhere() {
        val (sql, _) = user.select { it.id }.where { f.repeat("x", 3) == "xxx" }.build()
        assertEquals("""SELECT "ID" FROM "TB_USER" WHERE RPAD('x', 3 * LENGTH('x'), 'x') = :repeat AND "DELETED" = 0""", sql)
    }
}
