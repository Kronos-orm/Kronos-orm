package com.kotlinorm.functions

import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.functions.bundled.exts.MathFunctions.mod
import com.kotlinorm.functions.bundled.exts.MathFunctions.rand
import com.kotlinorm.functions.bundled.exts.MathFunctions.trunc
import com.kotlinorm.orm.select.select
import com.kotlinorm.testutils.SqliteTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * SQLite-specific function tests
 * Test functions that have different SQL generation for SQLite
 */
class SqliteFunctionTest : SqliteTestBase() {
    private val user by lazy { TestUser(1) }

    // Math functions with SQLite-specific behavior
    @Test
    fun testRandInSelect() {
        val (sql, _) = user.select { f.rand().alias("rand") }.build()
        assertEquals("""SELECT (RANDOM() / 9223372036854775808.0 + 0.5) AS rand FROM "tb_user" WHERE "deleted" = 0""", sql)
    }

    @Test
    fun testRandInWhere() {
        val (sql, _) = user.select { it.id }.where { f.rand() > 0.5 }.build()
        assertEquals("""SELECT "id" FROM "tb_user" WHERE (RANDOM() / 9223372036854775808.0 + 0.5) > :randMin AND "deleted" = 0""", sql)
    }

    @Test
    fun testModInSelect() {
        val (sql, _) = user.select { (it.score % 2).alias("mod") }.build()
        assertEquals("""SELECT ("score" % 2) AS mod FROM "tb_user" WHERE "deleted" = 0""", sql)
    }

    @Test
    fun testModInWhere() {
        val (sql, _) = user.select { it.id }.where { it.score % 2 == 0 }.build()
        assertEquals("""SELECT "id" FROM "tb_user" WHERE ("score" % 2) = :mod AND "deleted" = 0""", sql)
    }

    @Test
    fun testTruncInSelect() {
        val (sql, _) = user.select { f.trunc(it.score, 2).alias("trunc") }.build()
        assertEquals("""SELECT CAST("score" * POWER(10, 2) AS INTEGER) / POWER(10, 2) AS trunc FROM "tb_user" WHERE "deleted" = 0""", sql)
    }

    @Test
    fun testTruncInWhere() {
        val (sql, _) = user.select { it.id }.where { f.trunc(it.score, 0) > 50 }.build()
        assertEquals("""SELECT "id" FROM "tb_user" WHERE (CAST("score" * POWER(10, 0) AS INTEGER) / POWER(10, 0)) > :truncMin AND "deleted" = 0""", sql)
    }
}
