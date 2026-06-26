/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kotlinorm.functions

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.UnSupportedFunctionException
import com.kotlinorm.functions.bundled.builders.MathFunctionBuilder
import com.kotlinorm.functions.bundled.builders.PolymerizationFunctionBuilder
import com.kotlinorm.functions.bundled.builders.PostgresFunctionBuilder
import com.kotlinorm.functions.bundled.builders.StringFunctionBuilder
import com.kotlinorm.interfaces.FunctionBuilder
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import com.kotlinorm.wrappers.SampleOracleJdbcWrapper
import com.kotlinorm.wrappers.SamplePostgresJdbcWrapper
import com.kotlinorm.wrappers.SampleSqlServerJdbcWrapper
import com.kotlinorm.wrappers.SampleSqliteJdbcWrapper
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FunctionBuildersTest : MysqlTestBase() {
    private val mysql = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
    private val postgres = SamplePostgresJdbcWrapper()
    private val oracle = SampleOracleJdbcWrapper
    private val mssql = SampleSqlServerJdbcWrapper
    private val sqlite = SampleSqliteJdbcWrapper
    private fun fp(c: String, t: String = ""): Pair<Field?, Any?> = Pair(Field(columnName = c, name = c, tableName = t), null)
    private fun vp(v: Any?): Pair<Field?, Any?> = Pair(null, v)
    private fun ff(n: String, f: List<Pair<Field?, Any?>> = listOf(), a: String = n) = FunctionField(n, f).also { it.columnName = a; it.name = a }

    // buildAlias
    @Test fun testBuildAliasWithAlias() = assertEquals("X AS a", MathFunctionBuilder.buildAlias("X", "a"))
    @Test fun testBuildAliasEmpty() = assertEquals("X", MathFunctionBuilder.buildAlias("X", ""))

    // buildField
    @Test fun testBuildFieldCol() = assertEquals("`score`", MathFunctionBuilder.buildField(fp("score"), mysql, false))
    @Test fun testBuildFieldStr() = assertEquals("'hi'", MathFunctionBuilder.buildField(vp("hi"), mysql, false))
    @Test fun testBuildFieldNum() = assertEquals("42", MathFunctionBuilder.buildField(vp(42), mysql, false))

    // buildFields
    @Test fun testBuildFieldsAlias() = assertEquals("ABS(`s`) AS a", MathFunctionBuilder.buildFields("ABS", "a", listOf(fp("s")), mysql, false))
    @Test fun testBuildFieldsNoAlias() = assertEquals("ABS(`s`)", MathFunctionBuilder.buildFields("ABS", "", listOf(fp("s")), mysql, false))
    @Test fun testBuildFieldsMulti() = assertEquals("G(`a`, `b`, 1)", MathFunctionBuilder.buildFields("G", "", listOf(fp("a"), fp("b"), vp(1)), mysql, false))

    // buildOperations
    @Test fun testBuildOpsAdd() = assertEquals("(`a` + `b`)", MathFunctionBuilder.buildOperations("+", "", listOf(fp("a"), fp("b")), mysql, false))
    @Test fun testBuildOpsAlias() = assertEquals("(`a` - 10) AS d", MathFunctionBuilder.buildOperations("-", "d", listOf(fp("a"), vp(10)), mysql, false))

    // MathFunctionBuilder.transform
    @Test fun testMathAbs() = assertEquals("ABS(`s`) AS abs", MathFunctionBuilder.transform(ff("abs", listOf(fp("s"))), mysql, false, true))
    @Test fun testMathAbsNoAlias() = assertEquals("ABS(`s`)", MathFunctionBuilder.transform(ff("abs", listOf(fp("s"))), mysql, false, false))
    @Test fun testMathCeilMy() = assertEquals("CEIL(`s`) AS ceil", MathFunctionBuilder.transform(ff("ceil", listOf(fp("s"))), mysql, false, true))
    @Test fun testMathCeilMs() = assertEquals("CEILING([s]) AS ceil", MathFunctionBuilder.transform(ff("ceil", listOf(fp("s"))), mssql, false, true))
    @Test fun testMathFloor() = assertEquals("FLOOR(`s`) AS floor", MathFunctionBuilder.transform(ff("floor", listOf(fp("s"))), mysql, false, true))
    @Test fun testMathExp() = assertEquals("EXP(`s`) AS exp", MathFunctionBuilder.transform(ff("exp", listOf(fp("s"))), mysql, false, true))
    @Test fun testMathSign() = assertEquals("SIGN(`s`) AS sign", MathFunctionBuilder.transform(ff("sign", listOf(fp("s"))), mysql, false, true))
    @Test fun testMathSqrt() = assertEquals("SQRT(`s`) AS sqrt", MathFunctionBuilder.transform(ff("sqrt", listOf(fp("s"))), mysql, false, true))
    @Test fun testMathRound() = assertEquals("ROUND(`s`, 2) AS round", MathFunctionBuilder.transform(ff("round", listOf(fp("s"), vp(2))), mysql, false, true))
    @Test fun testMathPi() = assertEquals("PI() AS pi", MathFunctionBuilder.transform(ff("pi", listOf()), mysql, false, true))
    @Test fun testMathLog() = assertEquals("LOG(`s`, 10) AS log", MathFunctionBuilder.transform(ff("log", listOf(fp("s"), vp(10))), mysql, false, true))
    @Test fun testMathGreatest() = assertEquals("GREATEST(`a`, `b`) AS greatest", MathFunctionBuilder.transform(ff("greatest", listOf(fp("a"), fp("b"))), mysql, false, true))
    @Test fun testMathLeast() = assertEquals("LEAST(`a`, `b`) AS least", MathFunctionBuilder.transform(ff("least", listOf(fp("a"), fp("b"))), mysql, false, true))
    @Test fun testMathLnMy() = assertEquals("LN(`s`) AS ln", MathFunctionBuilder.transform(ff("ln", listOf(fp("s"))), mysql, false, true))
    @Test fun testMathLnMs() = assertEquals("LOG([s], 'EXP(1)') AS ln", MathFunctionBuilder.transform(ff("ln", listOf(fp("s"))), mssql, false, true))
    @Test fun testMathRandMy() = assertEquals("RAND() AS rand", MathFunctionBuilder.transform(ff("rand", listOf()), mysql, false, true))
    @Test fun testMathRandOr() = assertEquals("DBMS_RANDOM.VALUE AS rand", MathFunctionBuilder.transform(ff("rand", listOf()), oracle, false, true))
    @Test fun testMathRandSl() = assertEquals("RANDOM() AS rand", MathFunctionBuilder.transform(ff("rand", listOf()), sqlite, false, true))
    @Test fun testMathRandPg() = assertEquals("RANDOM() AS rand", MathFunctionBuilder.transform(ff("rand", listOf()), postgres, false, true))
    @Test fun testMathTruncMy() = assertEquals("TRUNCATE(`s`, 2) AS trunc", MathFunctionBuilder.transform(ff("trunc", listOf(fp("s"), vp(2))), mysql, false, true))
    @Test fun testMathTruncMs() = assertEquals("ROUND([s], 2) AS trunc", MathFunctionBuilder.transform(ff("trunc", listOf(fp("s"), vp(2))), mssql, false, true))
    @Test fun testMathTruncPg() = assertEquals("TRUNC(\"s\", 2) AS trunc", MathFunctionBuilder.transform(ff("trunc", listOf(fp("s"), vp(2))), postgres, false, true))
    @Test fun testMathAdd() = assertEquals("(`a` + `b`) AS add", MathFunctionBuilder.transform(ff("add", listOf(fp("a"), fp("b"))), mysql, false, true))
    @Test fun testMathSub() = assertEquals("(`a` - `b`) AS sub", MathFunctionBuilder.transform(ff("sub", listOf(fp("a"), fp("b"))), mysql, false, true))
    @Test fun testMathMul() = assertEquals("(`a` * `b`) AS mul", MathFunctionBuilder.transform(ff("mul", listOf(fp("a"), fp("b"))), mysql, false, true))
    @Test fun testMathDiv() = assertEquals("(`a` / 2) AS div", MathFunctionBuilder.transform(ff("div", listOf(fp("a"), vp(2))), mysql, false, true))
    @Test fun testMathMod() = assertEquals("MOD(`a`, 3) AS mod", MathFunctionBuilder.transform(ff("mod", listOf(fp("a"), vp(3))), mysql, false, true))

    // StringFunctionBuilder.transform
    @Test fun testStrUpper() = assertEquals("UPPER(`n`) AS upper", StringFunctionBuilder.transform(ff("upper", listOf(fp("n"))), mysql, false, true))
    @Test fun testStrLower() = assertEquals("LOWER(`n`) AS lower", StringFunctionBuilder.transform(ff("lower", listOf(fp("n"))), mysql, false, true))
    @Test fun testStrLenMy() = assertEquals("LENGTH(`n`) AS length", StringFunctionBuilder.transform(ff("length", listOf(fp("n"))), mysql, false, true))
    @Test fun testStrLenMs() = assertEquals("LEN([n]) AS length", StringFunctionBuilder.transform(ff("length", listOf(fp("n"))), mssql, false, true))
    @Test fun testStrTrim() = assertEquals("TRIM(`n`) AS trim", StringFunctionBuilder.transform(ff("trim", listOf(fp("n"))), mysql, false, true))
    @Test fun testStrLtrim() = assertEquals("LTRIM(`n`) AS ltrim", StringFunctionBuilder.transform(ff("ltrim", listOf(fp("n"))), mysql, false, true))
    @Test fun testStrRtrim() = assertEquals("RTRIM(`n`) AS rtrim", StringFunctionBuilder.transform(ff("rtrim", listOf(fp("n"))), mysql, false, true))
    @Test fun testStrReverse() = assertEquals("REVERSE(`n`) AS reverse", StringFunctionBuilder.transform(ff("reverse", listOf(fp("n"))), mysql, false, true))
    @Test fun testStrReplace() = assertEquals("REPLACE(`n`, 'a', 'b') AS replace", StringFunctionBuilder.transform(ff("replace", listOf(fp("n"), vp("a"), vp("b"))), mysql, false, true))
    @Test fun testStrConcat() = assertEquals("CONCAT(`a`, `b`) AS concat", StringFunctionBuilder.transform(ff("concat", listOf(fp("a"), fp("b"))), mysql, false, true))
    @Test fun testStrNoAlias() = assertEquals("UPPER(`n`)", StringFunctionBuilder.transform(ff("upper", listOf(fp("n"))), mysql, false, false))
    @Test fun testStrSubstrMy() = assertEquals("SUBSTR(`n`, 2, 3) AS substr", StringFunctionBuilder.transform(ff("substr", listOf(fp("n"), vp(2), vp(3))), mysql, false, true))
    @Test fun testStrSubstrPg() = assertEquals("SUBSTRING(\"n\" FROM 2 FOR 3) AS substr", StringFunctionBuilder.transform(ff("substr", listOf(fp("n"), vp(2), vp(3))), postgres, false, true))
    @Test fun testStrLeftMy() = assertEquals("LEFT(`n`, 3) AS left", StringFunctionBuilder.transform(ff("left", listOf(fp("n"), vp(3))), mysql, false, true))
    @Test fun testStrLeftOr() = assertEquals("SUBSTR(\"n\", 1, 3) AS left", StringFunctionBuilder.transform(ff("left", listOf(fp("n"), vp(3))), oracle, false, true))
    @Test fun testStrLeftPg() = assertEquals("SUBSTRING(\"n\" FROM 1 FOR 3) AS left", StringFunctionBuilder.transform(ff("left", listOf(fp("n"), vp(3))), postgres, false, true))
    @Test fun testStrRightMy() = assertEquals("RIGHT(`n`, 2) AS right", StringFunctionBuilder.transform(ff("right", listOf(fp("n"), vp(2))), mysql, false, true))
    @Test fun testStrRightOr() = assertEquals("SUBSTR(\"n\", -2) AS right", StringFunctionBuilder.transform(ff("right", listOf(fp("n"), vp(2))), oracle, false, true))
    @Test fun testStrRightPg() = assertEquals("SUBSTRING(\"n\" FROM -2) AS right", StringFunctionBuilder.transform(ff("right", listOf(fp("n"), vp(2))), postgres, false, true))
    @Test fun testStrRepeatMy() = assertEquals("REPEAT(`n`, 3) AS repeat", StringFunctionBuilder.transform(ff("repeat", listOf(fp("n"), vp(3))), mysql, false, true))
    @Test fun testStrRepeatMs() = assertEquals("REPLICATE([n], 3) AS repeat", StringFunctionBuilder.transform(ff("repeat", listOf(fp("n"), vp(3))), mssql, false, true))
    @Test
    fun testStrRepeatOr() {
        val result = StringFunctionBuilder.transform(ff("repeat", listOf(fp("n"), vp(3))), oracle, false, true)
        assertTrue(result.contains("RPAD"))
        assertTrue(result.contains("LENGTH"))
    }
    @Test fun testStrJoinMy() = assertEquals("CONCAT_WS(`a`, `b`) AS join", StringFunctionBuilder.transform(ff("join", listOf(vp(", "), fp("a"), fp("b"))), mysql, false, true))
    @Test
    fun testStrJoinOr() {
        val result = StringFunctionBuilder.transform(ff("join", listOf(vp(", "), fp("a"), fp("b"))), oracle, false, true)
        assertTrue(result.contains("||"))
    }

    // PolymerizationFunctionBuilder.transform
    @Test fun testPolyCount() = assertEquals("COUNT(`id`) AS count", PolymerizationFunctionBuilder.transform(ff("count", listOf(fp("id"))), mysql, false, true))
    @Test fun testPolySum() = assertEquals("SUM(`a`) AS sum", PolymerizationFunctionBuilder.transform(ff("sum", listOf(fp("a"))), mysql, false, true))
    @Test fun testPolyAvg() = assertEquals("AVG(`s`) AS avg", PolymerizationFunctionBuilder.transform(ff("avg", listOf(fp("s"))), mysql, false, true))
    @Test fun testPolyMax() = assertEquals("MAX(`p`) AS max", PolymerizationFunctionBuilder.transform(ff("max", listOf(fp("p"))), mysql, false, true))
    @Test fun testPolyMin() = assertEquals("MIN(`p`) AS min", PolymerizationFunctionBuilder.transform(ff("min", listOf(fp("p"))), mysql, false, true))
    @Test fun testPolyNoAlias() = assertEquals("COUNT(`id`)", PolymerizationFunctionBuilder.transform(ff("count", listOf(fp("id"))), mysql, false, false))
    @Test fun testPolyGcMy() = assertEquals("GROUP_CONCAT(`n`) AS gc", PolymerizationFunctionBuilder.transform(ff("groupConcat", listOf(fp("n")), "gc"), mysql, false, true))
    @Test fun testPolyGcPg() = assertEquals("STRING_AGG(\"n\") AS gc", PolymerizationFunctionBuilder.transform(ff("groupConcat", listOf(fp("n")), "gc"), postgres, false, true))
    @Test fun testPolyGcMs() = assertEquals("STRING_AGG([n]) AS gc", PolymerizationFunctionBuilder.transform(ff("groupConcat", listOf(fp("n")), "gc"), mssql, false, true))
    @Test fun testPolyGcOr() = assertEquals("GROUP_CONCAT(\"n\") AS gc", PolymerizationFunctionBuilder.transform(ff("groupConcat", listOf(fp("n")), "gc"), oracle, false, true))
    @Test fun testPolyGcSl() = assertEquals("GROUP_CONCAT(\"n\") AS gc", PolymerizationFunctionBuilder.transform(ff("groupConcat", listOf(fp("n")), "gc"), sqlite, false, true))

    // PostgresFunctionBuilder
    @Test
    fun testPgAny() {
        val r = PostgresFunctionBuilder.transform(ff("any", listOf(fp("id"))), postgres, false, true)
        assertTrue(r.contains("ANY"))
        assertTrue(r.contains("ARRAY"))
    }
    @Test
    fun testPgAll() {
        val r = PostgresFunctionBuilder.transform(ff("all", listOf(fp("id"))), postgres, false, true)
        assertTrue(r.contains("ALL"))
        assertTrue(r.contains("ARRAY"))
    }
    @Test fun testPgSupportAny() { assertTrue(PostgresFunctionBuilder.support("any", DBType.Postgres)); assertFalse(PostgresFunctionBuilder.support("any", DBType.Mysql)) }
    @Test fun testPgSupportAll() { assertTrue(PostgresFunctionBuilder.support("all", DBType.Postgres)); assertFalse(PostgresFunctionBuilder.support("all", DBType.Mysql)) }
    @Test fun testPgSupportUnknown() = assertFalse(PostgresFunctionBuilder.support("xyz", DBType.Postgres))

    // support(FunctionField)
    @Test fun testSupportFFPoly() = assertTrue(PolymerizationFunctionBuilder.support(ff("count"), DBType.Mysql))
    @Test fun testSupportFFMath() = assertTrue(MathFunctionBuilder.support(ff("abs"), DBType.Mysql))
    @Test fun testSupportFFStr() = assertTrue(StringFunctionBuilder.support(ff("upper"), DBType.Oracle))
    @Test fun testSupportFFNo() = assertFalse(PolymerizationFunctionBuilder.support(ff("xyz"), DBType.Mysql))

    // FunctionManager.getBuiltFunctionField
    @Test fun testFmCount() = assertEquals("COUNT(`id`) AS count", FunctionManager.getBuiltFunctionField(ff("count", listOf(fp("id"))), mysql))
    @Test fun testFmAbs() = assertEquals("ABS(`s`) AS abs", FunctionManager.getBuiltFunctionField(ff("abs", listOf(fp("s"))), mysql))
    @Test fun testFmUpper() = assertEquals("UPPER(`n`) AS upper", FunctionManager.getBuiltFunctionField(ff("upper", listOf(fp("n"))), mysql))
    @Test
    fun testFmUnsupported() {
        assertFailsWith<UnSupportedFunctionException> {
            FunctionManager.getBuiltFunctionField(ff("nonexistent_xyz", listOf(fp("x"))), mysql)
        }
    }

    // FunctionManager.registerFunctionBuilder
    @Test
    fun testFmRegister() {
        val custom = object : FunctionBuilder {
            override val supportFunctionNames: (String) -> Array<DBType> = { if (it == "customTestFunc") arrayOf(DBType.Mysql) else emptyArray() }
            override fun transform(field: FunctionField, dataSource: KronosDataSourceWrapper, showTable: Boolean, showAlias: Boolean) =
                "CUSTOM(${field.fields.joinToString(", ") { it.first?.columnName ?: it.second.toString() }})"
        }
        FunctionManager.registerFunctionBuilder(custom)
        assertEquals("CUSTOM(x)", FunctionManager.getBuiltFunctionField(ff("customTestFunc", listOf(fp("x"))), mysql))
    }

    // Support coverage
    @Test
    fun testMathAllNames() {
        listOf("abs","ceil","floor","exp","round","sign","sqrt","trunc","add","sub","mul","div","ln","log","mod","pi","rand").forEach {
            assertTrue(MathFunctionBuilder.support(it, DBType.Mysql), "$it on MySQL")
        }
    }
    @Test
    fun testMathGreatestLeastCommon() {
        listOf("greatest","least").forEach { fn ->
            assertTrue(MathFunctionBuilder.support(fn, DBType.Mysql))
            assertTrue(MathFunctionBuilder.support(fn, DBType.Postgres))
            assertFalse(MathFunctionBuilder.support(fn, DBType.SQLite))
            assertFalse(MathFunctionBuilder.support(fn, DBType.Mssql))
        }
    }
    @Test
    fun testStrAllNames() {
        listOf("length","upper","lower","substr","replace","left","right","repeat","reverse","trim","ltrim","rtrim","concat","join").forEach {
            assertTrue(StringFunctionBuilder.support(it, DBType.Mysql), "$it on MySQL")
        }
    }
    @Test
    fun testPolyAllNames() {
        listOf("count","sum","avg","max","min","groupConcat").forEach {
            assertTrue(PolymerizationFunctionBuilder.support(it, DBType.Mysql), "$it on MySQL")
        }
    }
    @Test
    fun testUnknownNotSupported() {
        assertFalse(MathFunctionBuilder.support("foobar", DBType.Mysql))
        assertFalse(StringFunctionBuilder.support("foobar", DBType.Mysql))
        assertFalse(PolymerizationFunctionBuilder.support("foobar", DBType.Mysql))
    }
}
