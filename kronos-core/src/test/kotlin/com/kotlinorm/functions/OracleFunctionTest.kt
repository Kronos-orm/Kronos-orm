package com.kotlinorm.functions

import com.kotlinorm.beans.sample.database.MysqlUser
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
    private val user by lazy { MysqlUser(1) }

    // Math functions with Oracle-specific behavior
    @Test
    fun testRandInSelect() {
        val (sql, _) = user.select { f.rand() }.build()
        assertEquals("""SELECT DBMS_RANDOM.VALUE AS rand FROM "tb_user" WHERE "id" = :id AND "deleted" = 0""", sql)
    }

    @Test
    fun testModInSelect() {
        val (sql, _) = user.select { f.mod(it.score, 2) }.build()
        assertEquals("""SELECT MOD("score", 2) AS mod FROM "tb_user" WHERE "id" = :id AND "deleted" = 0""", sql)
    }

    @Test
    fun testModInWhere() {
        val (sql, _) = user.select { it.id }.where { f.mod(it.score, 2) == 0 }.build()
        assertEquals("""SELECT "id" FROM "tb_user" WHERE MOD("score", 2) = :mod AND "deleted" = 0""", sql)
    }

    @Test
    fun testTruncInSelect() {
        val (sql, _) = user.select { f.trunc(it.score, 2) }.build()
        assertEquals("""SELECT TRUNC("score", 2) AS trunc FROM "tb_user" WHERE "id" = :id AND "deleted" = 0""", sql)
    }

    @Test
    fun testTruncInWhere() {
        val (sql, _) = user.select { it.id }.where { f.trunc(it.score, 0) > 50 }.build()
        assertEquals("""SELECT "id" FROM "tb_user" WHERE TRUNC("score", 0) > :truncMin AND "deleted" = 0""", sql)
    }

    // String functions with Oracle-specific behavior
    @Test
    fun testJoinInSelect() {
        val (sql, _) = user.select { f.join(", ", it.username, it.username) }.build()
        assertEquals("""SELECT "username" || ', ' || "username" AS join FROM "tb_user" WHERE "id" = :id AND "deleted" = 0""", sql)
    }

    @Test
    fun testJoinInWhere() {
        val (sql, _) = user.select { it.id }.where { f.join("-", it.username, it.username) == "admin-admin" }.build()
        assertEquals("""SELECT "id" FROM "tb_user" WHERE "username" || '-' || "username" = :join AND "deleted" = 0""", sql)
    }

    @Test
    fun testLeftInSelect() {
        val (sql, _) = user.select { f.left(it.username, 5) }.build()
        assertEquals("""SELECT SUBSTR("username", 1, 5) AS left FROM "tb_user" WHERE "id" = :id AND "deleted" = 0""", sql)
    }

    @Test
    fun testLeftInWhere() {
        val (sql, _) = user.select { it.id }.where { f.left(it.username, 3) == "adm" }.build()
        assertEquals("""SELECT "id" FROM "tb_user" WHERE SUBSTR("username", 1, 3) = :left AND "deleted" = 0""", sql)
    }

    @Test
    fun testRightInSelect() {
        val (sql, _) = user.select { f.right(it.username, 5) }.build()
        assertEquals("""SELECT SUBSTR("username", -5) AS right FROM "tb_user" WHERE "id" = :id AND "deleted" = 0""", sql)
    }

    @Test
    fun testRightInWhere() {
        val (sql, _) = user.select { it.id }.where { f.right(it.username, 3) == "min" }.build()
        assertEquals("""SELECT "id" FROM "tb_user" WHERE SUBSTR("username", -3) = :right AND "deleted" = 0""", sql)
    }

    @Test
    fun testRepeatInSelect() {
        val (sql, _) = user.select { f.repeat(it.username, 3) }.build()
        assertEquals("""SELECT RPAD("username", 3 * LENGTH("username"), "username") AS repeat FROM "tb_user" WHERE "id" = :id AND "deleted" = 0""", sql)
    }

    @Test
    fun testRepeatInWhere() {
        val (sql, _) = user.select { it.id }.where { f.repeat("x", 3) == "xxx" }.build()
        assertEquals("""SELECT "id" FROM "tb_user" WHERE RPAD('x', 3 * LENGTH('x'), 'x') = :repeat AND "deleted" = 0""", sql)
    }
}
