package com.kotlinorm.functions

import com.kotlinorm.beans.sample.database.MysqlUser
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
    private val user by lazy { MysqlUser(1) }

    // Math functions with SQLite-specific behavior
    @Test
    fun testRandInSelect() {
        val (sql, _) = user.select { f.rand() }.build()
        assertEquals("""SELECT RANDOM() AS rand FROM "tb_user" WHERE "id" = :id AND "deleted" = 0""", sql)
    }

    @Test
    fun testRandInWhere() {
        val (sql, _) = user.select { it.id }.where { f.rand() > 0.5 }.build()
        assertEquals("""SELECT "id" FROM "tb_user" WHERE RANDOM() > :randMin AND "deleted" = 0""", sql)
    }

    @Test
    fun testModInSelect() {
        val (sql, _) = user.select { f.mod(it.score, 2) }.build()
        assertEquals("""SELECT ("score" % 2) AS mod FROM "tb_user" WHERE "id" = :id AND "deleted" = 0""", sql)
    }

    @Test
    fun testModInWhere() {
        val (sql, _) = user.select { it.id }.where { f.mod(it.score, 2) == 0 }.build()
        assertEquals("""SELECT "id" FROM "tb_user" WHERE false AND "deleted" = 0""", sql)
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
}
