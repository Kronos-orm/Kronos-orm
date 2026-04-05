package com.kotlinorm.functions

import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.functions.bundled.exts.MathFunctions.abs
import com.kotlinorm.functions.bundled.exts.MathFunctions.add
import com.kotlinorm.functions.bundled.exts.MathFunctions.ceil
import com.kotlinorm.functions.bundled.exts.MathFunctions.div
import com.kotlinorm.functions.bundled.exts.MathFunctions.exp
import com.kotlinorm.functions.bundled.exts.MathFunctions.floor
import com.kotlinorm.functions.bundled.exts.MathFunctions.greatest
import com.kotlinorm.functions.bundled.exts.MathFunctions.least
import com.kotlinorm.functions.bundled.exts.MathFunctions.ln
import com.kotlinorm.functions.bundled.exts.MathFunctions.log
import com.kotlinorm.functions.bundled.exts.MathFunctions.mod
import com.kotlinorm.functions.bundled.exts.MathFunctions.mul
import com.kotlinorm.functions.bundled.exts.MathFunctions.pi
import com.kotlinorm.functions.bundled.exts.MathFunctions.rand
import com.kotlinorm.functions.bundled.exts.MathFunctions.round
import com.kotlinorm.functions.bundled.exts.MathFunctions.sign
import com.kotlinorm.functions.bundled.exts.MathFunctions.sqrt
import com.kotlinorm.functions.bundled.exts.MathFunctions.sub
import com.kotlinorm.functions.bundled.exts.MathFunctions.trunc
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.avg
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.max
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.min
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
import com.kotlinorm.functions.bundled.exts.StringFunctions.concat
import com.kotlinorm.functions.bundled.exts.StringFunctions.join
import com.kotlinorm.functions.bundled.exts.StringFunctions.left
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.functions.bundled.exts.StringFunctions.lower
import com.kotlinorm.functions.bundled.exts.StringFunctions.ltrim
import com.kotlinorm.functions.bundled.exts.StringFunctions.repeat
import com.kotlinorm.functions.bundled.exts.StringFunctions.replace
import com.kotlinorm.functions.bundled.exts.StringFunctions.reverse
import com.kotlinorm.functions.bundled.exts.StringFunctions.right
import com.kotlinorm.functions.bundled.exts.StringFunctions.rtrim
import com.kotlinorm.functions.bundled.exts.StringFunctions.substr
import com.kotlinorm.functions.bundled.exts.StringFunctions.trim
import com.kotlinorm.functions.bundled.exts.StringFunctions.upper
import com.kotlinorm.orm.select.select
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper.Companion.sampleMysqlJdbcWrapper
import com.kotlinorm.testutils.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * MySQL function tests - covers all functions in SELECT clause
 */
class MysqlFunctionTest : MysqlTestBase() {

    private val user by lazy { MysqlUser(1) }

    @Test
    fun testMathFunctionAbs() {
        val (sql, _) = user.select { f.abs(it.score) }.build()
        assertEquals("SELECT ABS([score]) AS abs FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionCeiling() {
        val (sql, _) = user.select { f.ceil(it.score) }.build()
        assertEquals("SELECT CEILING([score]) AS ceil FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionFloor() {
        val (sql, _) = user.select { f.floor(it.score) }.build()
        assertEquals("SELECT FLOOR([score]) AS floor FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionExp() {
        val (sql, _) = user.select { f.exp(it.score) }.build()
        assertEquals("SELECT EXP([score]) AS exp FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionGreatest() {
        val (sql, _) = user.select { f.greatest(it.score, 100) }.build()
        assertEquals("", sql)
    }

    @Test
    fun testMathFunctionLeast() {
        val (sql, _) = user.select { f.least(it.score, 100) }.build()
        assertEquals("", sql)
    }

    @Test
    fun testMathFunctionLn() {
        val (sql, _) = user.select { f.ln(it.score) }.build()
        assertEquals("SELECT LOG([score], EXP(1)) AS ln FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionLog() {
        val (sql, _) = user.select { f.log(it.score, 10) }.build()
        assertEquals("SELECT LOG([score], 10) AS log FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionMod() {
        val (sql, _) = user.select { f.mod(it.score, 2) }.build()
        assertEquals("SELECT ([score] % 2) AS mod FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionPi() {
        val (sql, _) = user.select { f.pi() }.build()
        assertEquals("SELECT PI() AS pi FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionRand() {
        val (sql, _) = user.select { f.rand() }.build()
        assertEquals("SELECT RAND() AS rand FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionRound() {
        val (sql, _) = user.select { f.round(it.score, 2) }.build()
        assertEquals("SELECT ROUND([score], 2) AS round FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionSign() {
        val (sql, _) = user.select { f.sign(it.score) }.build()
        assertEquals("SELECT SIGN([score]) AS sign FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionSqrt() {
        val (sql, _) = user.select { f.sqrt(it.score) }.build()
        assertEquals("SELECT SQRT([score]) AS sqrt FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctiontrunc() {
        dataSource = { sampleMysqlJdbcWrapper }
        val (sql, _) = user.select { f.trunc(it.score, 2) }.build()
        assertEquals("SELECT TRUNCATE(`score`, 2) AS trunc FROM `tb_user` WHERE `id` = :id AND `deleted` = 0", sql)
    }

    @Test
    fun testMathFunctionAdd() {
        val (sql, _) = user.select { f.add(it.score, 10, 20) }.build()
        assertEquals("SELECT ([score] + 10 + 20) AS add FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionSub() {
        val (sql, _) = user.select { f.sub(it.score, 10) }.build()
        assertEquals("SELECT ([score] - 10) AS sub FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionMul() {
        val (sql, _) = user.select { f.mul(it.score, 2) }.build()
        assertEquals("SELECT ([score] * 2) AS mul FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testMathFunctionDiv() {
        val (sql, _) = user.select { f.div(it.score, 2) }.build()
        assertEquals("SELECT ([score] / 2) AS div FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionLength() {
        val (sql, _) = user.select { f.length(it.username) }.build()
        assertEquals("SELECT LEN([username]) AS length FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionUpper() {
        val (sql, _) = user.select { f.upper(it.username) }.build()
        assertEquals("SELECT UPPER([username]) AS upper FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionLower() {
        val (sql, _) = user.select { f.lower(it.username) }.build()
        assertEquals("SELECT LOWER([username]) AS lower FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionSubstr() {
        val (sql, _) = user.select { f.substr(it.username, 1, 5) }.build()
        assertEquals("SELECT SUBSTR([username], 1, 5) AS substr FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionReplace() {
        val (sql, _) = user.select { f.replace(it.username, "old", "new") }.build()
        assertEquals("SELECT REPLACE([username], 'old', 'new') AS replace FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionLeft() {
        val (sql, _) = user.select { f.left(it.username, 5) }.build()
        assertEquals("SELECT LEFT([username], 5) AS left FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionRight() {
        val (sql, _) = user.select { f.right(it.username, 5) }.build()
        assertEquals("SELECT RIGHT([username], 5) AS right FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionRepeat() {
        val (sql, _) = user.select { f.repeat(it.username, 3) }.build()
        assertEquals("SELECT REPLICATE([username], 3) AS repeat FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionReverse() {
        val (sql, _) = user.select { f.reverse(it.username) }.build()
        assertEquals("SELECT REVERSE([username]) AS reverse FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionTrim() {
        val (sql, _) = user.select { f.trim(it.username) }.build()
        assertEquals("SELECT TRIM([username]) AS trim FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionLtrim() {
        val (sql, _) = user.select { f.ltrim(it.username) }.build()
        assertEquals("SELECT LTRIM([username]) AS ltrim FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionRtrim() {
        val (sql, _) = user.select { f.rtrim(it.username) }.build()
        assertEquals("SELECT RTRIM([username]) AS rtrim FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionConcat() {
        val (sql, _) = user.select { f.concat(it.username, " - ", it.username) }.build()
        assertEquals("SELECT CONCAT([username], ' - ', [username]) AS concat FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionJoin() {
        val (sql, _) = user.select { f.join(", ", it.username, it.username) }.build()
        assertEquals("SELECT CONCAT_WS(', ', [username], [username]) AS join FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testPolymerizationFunctionCount() {
        val (sql, _) = user.select { f.count(1) }.build()
        assertEquals("SELECT COUNT(1) AS count FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testPolymerizationFunctionSum() {
        val (sql, _) = user.select { f.sum(it.score) }.build()
        assertEquals("SELECT SUM([score]) AS sum FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testPolymerizationFunctionAvg() {
        val (sql, _) = user.select { f.avg(it.score) }.build()
        assertEquals("SELECT AVG([score]) AS avg FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testPolymerizationFunctionMax() {
        val (sql, _) = user.select { f.max(it.score) }.build()
        assertEquals("SELECT MAX([score]) AS max FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testPolymerizationFunctionMin() {
        val (sql, _) = user.select { f.min(it.score) }.build()
        assertEquals("SELECT MIN([score]) AS min FROM [tb_user] WHERE [id] = :id AND [deleted] = 0", sql)
    }

    @Test
    fun testComplexFunctionCombination() {
        val (sql, _) = user.select {
            f.count(1) + f.sum(it.score) + f.avg(it.score) + f.max(it.score) + f.min(it.score)
        }.where {
            f.add(it.score, 10) > f.sub(it.score, 10) && f.length(it.username) > 5
        }.build()
        assertEquals("SELECT COUNT(1) AS count, SUM([score]) AS sum, AVG([score]) AS avg, MAX([score]) AS max, MIN([score]) AS min FROM [tb_user] WHERE ([score] + 10) > ([score] - 10) AND LEN([username]) > :lengthMin AND [deleted] = 0", sql)
    }

    // WHERE clause tests for math functions
    @Test
    fun testMathFunctionsInWhere() {
        val (sql, _) = user.select { it.id }
            .where { 
                f.abs(it.score) > 10 && 
                f.ceil(it.score) < 100 &&
                f.floor(it.score) > 0
            }.build()
        assertEquals("SELECT [id] FROM [tb_user] WHERE ABS([score]) > :absMin AND CEILING([score]) < :ceilMax AND FLOOR([score]) > :floorMin AND [deleted] = 0", sql)
    }

    @Test
    fun testArithmeticInWhere() {
        val (sql, _) = user.select { it.id }
            .where { 
                f.add(it.score, 10) > 100 &&
                f.sub(it.score, 5) < 50 &&
                f.mul(it.score, 2) > 100 &&
                f.div(it.score, 2) < 50
            }.build()
        assertEquals("SELECT [id] FROM [tb_user] WHERE ([score] + 10) > :addMin AND ([score] - 5) < :subMax AND ([score] * 2) > :mulMin AND ([score] / 2) < :divMax AND [deleted] = 0", sql)
    }

    @Test
    fun testStringFunctionsInWhere() {
        val (sql, _) = user.select { it.id }
            .where { 
                f.length(it.username) > 5
            }.build()
        assertEquals("SELECT id[ FROM [tb_user] WHERE LEN([username]) ", sql)
    }

    @Test
    fun testCountWithGroupBy() {
        val (sql, _) = user.select { f.count(1) + it.gender }
            .groupBy { it.gender }
            .build()
        assertEquals("SELECT COUNT(1) AS count, [gender] FROM [tb_user] WHERE [id] = :id AND [deleted] = 0 GROUP BY [gender]", sql)
    }

    @Test
    fun testSumWithGroupBy() {
        val (sql, _) = user.select { f.sum(it.score) + it.gender }
            .groupBy { it.gender }
            .build()
        assertEquals("SELECT SUM([score]) AS sum, [gender] FROM [tb_user] WHERE [id] = :id AND [deleted] = 0 GROUP BY [gender]", sql)
    }

    @Test
    fun testAvgWithGroupBy() {
        val (sql, _) = user.select { f.avg(it.score) + it.gender }
            .groupBy { it.gender }
            .build()
        assertEquals("SELECT AVG([score]) AS avg, [gender] FROM [tb_user] WHERE [id] = :id AND [deleted] = 0 GROUP BY [gender]", sql)
    }

    @Test
    fun testMaxWithGroupBy() {
        val (sql, _) = user.select { f.max(it.score) + it.gender }
            .groupBy { it.gender }
            .build()
        assertEquals("SELECT MAX(`score`) AS max, `gender` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0 GROUP BY `gender`", sql)
    }

    @Test
    fun testMinWithGroupBy() {
        val (sql, _) = user.select { f.min(it.score) + it.gender }
            .groupBy { it.gender }
            .build()
        assertEquals("SELECT MIN([score]) AS min, [gender] FROM [tb_user] WHERE [id] = :id AND [deleted] = 0 GROUP BY [gender]", sql)
    }

    @Test
    fun testCountWithHaving() {
        val (sql, _) = user.select { f.count(1) + it.gender }
            .groupBy { it.gender }
            .having { f.count(1) > 5 }
            .build()
        assertEquals("SELECT COUNT(1) AS count, [gender] FROM [tb_user] WHERE [id] = :id AND [deleted] = 0 GROUP BY [gender] HAVING COUNT(1) > :countMin", sql)
    }

    @Test
    fun testSumWithHaving() {
        val (sql, _) = user.select { f.sum(it.score) + it.gender }
            .groupBy { it.gender }
            .having { f.sum(it.score) > 100 }
            .build()
        assertEquals("SELECT SUM([score]) AS sum, [gender] FROM [tb_user] WHERE [id] = :id AND [deleted] = 0 GROUP BY [gender] HAVING SUM([score]) > :sumMin", sql)
    }

    @Test
    fun testAvgWithHaving() {
        val (sql, _) = user.select { f.avg(it.score) + it.gender }
            .groupBy { it.gender }
            .having { f.avg(it.score) > 50 }
            .build()
        assertEquals("SELECT AVG([score]) AS avg, [gender] FROM [tb_user] WHERE [id] = :id AND [deleted] = 0 GROUP BY [gender] HAVING AVG([score]) > :avgMin", sql)
    }

    @Test
    fun testMaxWithHaving() {
        val (sql, _) = user.select { f.max(it.score) + it.gender }
            .groupBy { it.gender }
            .having { f.max(it.score) > 90 }
            .build()
        assertEquals("SELECT MAX([score]) AS max, [gender] FROM [tb_user] WHERE [id] = :id AND [deleted] = 0 GROUP BY [gender] HAVING MAX([score]) > :maxMin", sql)
    }

    @Test
    fun testMinWithHaving() {
        val (sql, _) = user.select { f.min(it.score) + it.gender }
            .groupBy { it.gender }
            .having { f.min(it.score) < 10 }
            .build()
        assertEquals("SELECT MIN([score]) AS min, [gender] FROM [tb_user] WHERE [id] = :id AND [deleted] = 0 GROUP BY [gender] HAVING MIN([score]) < :minMax", sql)
    }

    @Test
    fun testMultipleAggregationsWithGroupByAndHaving() {
        val (sql, _) = user.select { 
            f.count(1) + f.sum(it.score) + f.avg(it.score) + it.gender 
        }
            .groupBy { it.gender }
            .having { f.count(1) > 5 && f.avg(it.score) > 50 }
            .build()
        assertEquals("SELECT COUNT(1) AS count, SUM([score]) AS sum, AVG([score]) AS avg, [gender] FROM [tb_user] WHERE [id] = :id AND [deleted] = 0 GROUP BY [gender] HAVING COUNT(1) > :countMin AND AVG([score]) > :avgMin", sql)
    }
}
