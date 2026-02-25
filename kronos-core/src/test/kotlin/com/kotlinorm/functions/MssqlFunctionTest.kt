package com.kotlinorm.functions

import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.functions.bundled.exts.MathFunctions.ceil
import com.kotlinorm.functions.bundled.exts.MathFunctions.ln
import com.kotlinorm.functions.bundled.exts.MathFunctions.mod
import com.kotlinorm.functions.bundled.exts.MathFunctions.rand
import com.kotlinorm.functions.bundled.exts.MathFunctions.trunc
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.functions.bundled.exts.StringFunctions.repeat
import com.kotlinorm.orm.select.select
import com.kotlinorm.testutils.MssqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * MSSQL-specific function tests
 * Tests functions that have different SQL generation for MSSQL (SQL Server)
 */
class MssqlFunctionTest : MssqlTestBase() {
    private val user by lazy { MysqlUser(1) }

    // Math functions with MSSQL-specific behavior
    @Test
    fun testCeilInSelect() {
        val (sql, _) = user.select { f.ceil(it.score) }.build()
        assertEquals("SELECT CEILING([score]) AS ceil FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testCeilInWhere() {
        val (sql, _) = user.select { it.id }.where { f.ceil(it.score) > 50 }.build()
        assertEquals("SELECT [id] FROM [tb_user] WHERE CEILING([score]) > :ceilMin AND [deleted] = 0", sql)
    }

    @Test
    fun testLnInSelect() {
        val (sql, _) = user.select { f.ln(it.score) }.build()
        assertEquals("SELECT LOG([score], EXP(1)) AS ln FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testLnInWhere() {
        val (sql, _) = user.select { it.id }.where { f.ln(it.score) > 2 }.build()
        assertEquals("SELECT [id] FROM [tb_user] WHERE LOG([score], EXP(1)) > :lnMin AND [deleted] = 0", sql)
    }

    @Test
    fun testRandInSelect() {
        val (sql, _) = user.select { f.rand() }.build()
        assertEquals("SELECT RAND() AS rand FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testRandInWhere() {
        val (sql, _) = user.select { it.id }.where { f.rand() > 0.5 }.build()
        assertEquals("SELECT [id] FROM [tb_user] WHERE RAND() > :randMin AND [deleted] = 0", sql)
    }

    @Test
    fun testModInSelect() {
        val (sql, _) = user.select { f.mod(it.score, 2) }.build()
        assertEquals("SELECT ([score] % 2) AS mod FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testModInWhere() {
        val (sql, _) = user.select { it.id }.where { f.mod(it.score, 2) == 0 }.build()
        assertEquals("...ROM tb_user WHERE [false] AND [deleted] = 0", sql)
    }

    @Test
    fun testTruncInSelect() {
        val (sql, _) = user.select { f.trunc(it.score, 2) }.build()
        assertEquals("SELECT ROUND([score], 2) AS trunc FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testTruncInWhere() {
        val (sql, _) = user.select { it.id }.where { f.trunc(it.score, 0) > 50 }.build()
        assertEquals("SELECT [id] FROM [tb_user] WHERE ROUND([score], 0) > :truncMin AND [deleted] = 0", sql)
    }

    // String functions with MSSQL-specific behavior
    @Test
    fun testLengthInSelect() {
        val (sql, _) = user.select { f.length(it.username) }.build()
        assertEquals("SELECT LEN([username]) AS length FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testLengthInWhere() {
        val (sql, _) = user.select { it.id }.where { f.length(it.username) > 5 }.build()
        assertEquals("SELECT [id] FROM [tb_user] WHERE LEN([username]) > :lengthMin AND [deleted] = 0", sql)
    }

    @Test
    fun testRepeatInSelect() {
        val (sql, _) = user.select { f.repeat(it.username, 3) }.build()
        assertEquals("SELECT REPLICATE([username], 3) AS repeat FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testRepeatInWhere() {
        val (sql, _) = user.select { it.id }.where { f.repeat("x", 3) == "xxx" }.build()
        assertEquals("SELECT [id] FROM [tb_user] WHERE false AND [deleted] = 0", sql)
    }
}
