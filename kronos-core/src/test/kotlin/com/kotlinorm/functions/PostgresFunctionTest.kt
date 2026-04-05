package com.kotlinorm.functions

import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.functions.bundled.exts.MathFunctions.mod
import com.kotlinorm.functions.bundled.exts.MathFunctions.rand
import com.kotlinorm.functions.bundled.exts.MathFunctions.trunc
import com.kotlinorm.functions.bundled.exts.StringFunctions.left
import com.kotlinorm.functions.bundled.exts.StringFunctions.right
import com.kotlinorm.functions.bundled.exts.StringFunctions.substr
import com.kotlinorm.orm.select.select
import com.kotlinorm.testutils.PostgresTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PostgreSQL-specific function tests
 * Tests functions that have different SQL generation for PostgreSQL
 */
class PostgresFunctionTest : PostgresTestBase() {
    private val user by lazy { MysqlUser(1) }

    // Math functions with PostgreSQL-specific behavior
    @Test
    fun testRandInSelect() {
        val (sql, _) = user.select { f.rand() }.build()
        assertEquals("SELECT RAND() AS rand FROM `tb_user` WHERE `id` = :id AND `deleted` = 0", sql)
    }

    @Test
    fun testRandInWhere() {
        val (sql, _) = user.select { it.id }.where { f.rand() > 0.5 }.build()
        assertEquals("SELECT `id` FROM `tb_user` WHERE RAND() > :randMin AND `deleted` = 0", sql)
    }

    @Test
    fun testModInSelect() {
        val (sql, _) = user.select { f.mod(it.score, 2) }.build()
        assertEquals("SELECT (`score` % 2) AS mod FROM `tb_user` WHERE `id` = :id AND `deleted` = 0", sql)
    }

    @Test
    fun testModInWhere() {
        val (sql, _) = user.select { it.id }.where { f.mod(it.score, 2) == 0 }.build()
        assertEquals("SELECT `id` FROM `tb_user` WHERE false AND `deleted` = 0", sql)
    }

    @Test
    fun testTruncInSelect() {
        val (sql, _) = user.select { f.trunc(it.score, 2) }.build()
        assertEquals("SELECT TRUNCATE(`score`, 2) AS trunc FROM `tb_user` WHERE `id` = :id AND `deleted` = 0", sql)
    }

    @Test
    fun testTruncInWhere() {
        val (sql, _) = user.select { it.id }.where { f.trunc(it.score, 0) > 50 }.build()
        assertEquals("SELECT `id` FROM `tb_user` WHERE TRUNCATE(`score`, 0) > :truncMin AND `deleted` = 0", sql)
    }

    // String functions with PostgreSQL-specific behavior
    @Test
    fun testSubstrInSelect() {
        val (sql, _) = user.select { f.substr(it.username, 1, 5) }.build()
        assertEquals("SELECT SUBSTR(`username`, 1, 5) AS substr FROM `tb_user` WHERE `id` = :id AND `deleted` = 0", sql)
    }

    @Test
    fun testSubstrInWhere() {
        val (sql, _) = user.select { it.id }.where { f.substr(it.username, 1, 3) == "adm" }.build()
        assertEquals("SELECT `id` FROM `tb_user` WHERE false AND `deleted` = 0", sql)
    }

    @Test
    fun testLeftInSelect() {
        val (sql, _) = user.select { f.left(it.username, 5) }.build()
        assertEquals("SELECT LEFT(`username`, 5) AS left FROM `tb_user` WHERE `id` = :id AND `deleted` = 0", sql)
    }

    @Test
    fun testLeftInWhere() {
        val (sql, _) = user.select { it.id }.where { f.left(it.username, 3) == "adm" }.build()
        assertEquals("SELECT `id` FROM `tb_user` WHERE false AND `deleted` = 0", sql)
    }

    @Test
    fun testRightInSelect() {
        val (sql, _) = user.select { f.right(it.username, 5) }.build()
        assertEquals("SELECT RIGHT(`username`, 5) AS right FROM `tb_user` WHERE `id` = :id AND `deleted` = 0", sql)
    }

    @Test
    fun testRightInWhere() {
        val (sql, _) = user.select { it.id }.where { f.right(it.username, 3) == "min" }.build()
        assertEquals("SELECT `id` FROM `tb_user` WHERE false AND `deleted` = 0", sql)
    }
}
