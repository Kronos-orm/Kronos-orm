package com.kotlinorm.beans.parser

import com.kotlinorm.beans.parser.NamedParameterUtils.buildValueArray
import com.kotlinorm.beans.parser.NamedParameterUtils.parseSqlStatement
import com.kotlinorm.exceptions.InvalidDataAccessApiUsageException
import com.kotlinorm.interfaces.KPojo
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class NamedParameterUtilsTest {
    @Test
    fun parseSql() {
        val sql = "xxx :a yyyy :b :c :a zzzzz"
        val parsedSql = parseSqlStatement(sql)
        assertEquals("xxx ? yyyy ? ? ? zzzzz", parsedSql.jdbcSql)
        assertEquals(listOf("a", "b", "c", "a"), parsedSql.parameterNames)
        assertEquals(4, parsedSql.totalParameterCount)
        assertEquals(3, parsedSql.namedParameterCount)

        val sql2 = "xxx &a yyyy ? zzzzz"
        val parsedSql2 = parseSqlStatement(sql2)
        assertEquals("xxx ? yyyy ? zzzzz", parsedSql2.jdbcSql)
        assertContains(parsedSql2.parameterNames, "a")
        assertEquals(2, parsedSql2.totalParameterCount)
        assertEquals(1, parsedSql2.namedParameterCount)

        val sql3 = "xxx &ä+:ö" + '\t' + ":ü%10 yyyy ? zzzzz"
        val parsedSql3 = parseSqlStatement(sql3)
        assertContentEquals(arrayOf("ä", "ö", "ü"), parsedSql3.parameterNames.toTypedArray())
    }

    @Test
    fun substituteNamedParameters() {
        val namedParams = mapOf(
            "a" to "a",
            "b" to "b",
            "c" to "c"
        )
        assertEquals("xxx ? ? ?", parseSqlStatement("xxx :a :b :c", namedParams).jdbcSql)
        assertEquals(
            "xxx ? ? ? xx ? ?",
            parseSqlStatement("xxx :a :b :c xx :a :a", namedParams).jdbcSql
        )
    }

    @Test
    fun convertParamMapToArray() {
        val namedParams = mapOf(
            "a" to "a",
            "b" to "b",
            "c" to "c"
        )
        assertEquals(3, parseSqlStatement("xxx :a :b :c", namedParams).jdbcParamList.size)
        assertEquals(5, parseSqlStatement("xxx :a :b :c xx :a :b", namedParams).jdbcParamList.size)
        assertEquals(5, parseSqlStatement("xxx :a :a :a xx :a :a", namedParams).jdbcParamList.size)
        assertEquals("b", parseSqlStatement("xxx :a :b :c xx :a :b", namedParams).jdbcParamList[4])
        assertFailsWith<InvalidDataAccessApiUsageException> {
            buildValueArray(
                parseSqlStatement("xxx :a :b ?"),
                namedParams
            )
        }
    }

    @Test
    fun substituteNamedParametersWithStringContainingQuotes() {
        val expectedSql = "select 'first name' from artists where id = ? and quote = 'exsqueeze me?'"
        val sql = "select 'first name' from artists where id = :id and quote = 'exsqueeze me?'"
        assertEquals(expectedSql, parseSqlStatement(sql).jdbcSql)
    }

    @Test
    fun testParseSqlStatementWithStringContainingQuotes() {
        val expectedSql = "select 'first name' from artists where id = ? and quote = 'exsqueeze me?'"
        val sql = "select 'first name' from artists where id = :id and quote = 'exsqueeze me?'"
        val parsedSql = parseSqlStatement(sql)
        assertEquals(expectedSql, parsedSql.jdbcSql)
    }

    @Test
    fun parseSqlContainingComments() {
        val sql1 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz -- :xx XX\n"
        val parsedSql1 = parseSqlStatement(sql1)
        assertEquals(
            "/*+ HINT */ xxx /* comment ? */ ? yyyy ? ? ? zzzzz -- :xx XX\n",
            parsedSql1.jdbcSql
        )
        val paramMap = mapOf(
            "a" to "a",
            "b" to "b",
            "c" to "c"
        )
        val params = parseSqlStatement(sql1, paramMap).jdbcParamList
        assertContentEquals(
            arrayOf("a", "b", "c", "a"),
            params
        )

        val sql2 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz -- :xx XX"
        val parsedSql2 = parseSqlStatement(sql2)
        assertEquals(
            "/*+ HINT */ xxx /* comment ? */ ? yyyy ? ? ? zzzzz -- :xx XX",
            parsedSql2.jdbcSql,
        )

        val sql3 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz /* :xx XX*"
        val parsedSql3 = parseSqlStatement(sql3)
        assertEquals(
            "/*+ HINT */ xxx /* comment ? */ ? yyyy ? ? ? zzzzz /* :xx XX*",
            parsedSql3.jdbcSql
        )

        val sql4 = "/*+ HINT */ xxx /* comment :a ? */ :a yyyy :b :c :a zzzzz /* :xx XX*"
        val parameters = mapOf("a" to "0")
        val parsedSql4 = parseSqlStatement(sql4, parameters)
        assertEquals(
            "/*+ HINT */ xxx /* comment :a ? */ ? yyyy ? ? ? zzzzz /* :xx XX*",
            parsedSql4.jdbcSql
        )
    }

    @Test
    fun parseSqlStatementWithPostgresCasting() {
        val expectedSql = "select 'first name' from artists where id = ? and birth_date=?::timestamp"
        val sql = "select 'first name' from artists where id = :id and birth_date=:birthDate::timestamp"
        val parsedSql = parseSqlStatement(sql)
        assertEquals(expectedSql, parsedSql.jdbcSql)
    }

    @Test
    fun parseSqlStatementWithPostgresContainedOperator() {
        val expectedSql =
            "select 'first name' from artists where info->'stat'->'albums' = ?? ? and '[\"1\",\"2\",\"3\"]'::jsonb ?? '4'"
        val sql =
            "select 'first name' from artists where info->'stat'->'albums' = ?? :album and '[\"1\",\"2\",\"3\"]'::jsonb ?? '4'"
        val parsedSql = parseSqlStatement(sql)

        assertEquals(1, parsedSql.totalParameterCount)
        assertEquals(expectedSql, parsedSql.jdbcSql)
    }

    @Test
    fun parseSqlStatementWithPostgresAnyArrayStringsExistsOperator() {
        val expectedSql = "select '[\"3\", \"11\"]'::jsonb ?| '{1,3,11,12,17}'::text[]"
        val sql = "select '[\"3\", \"11\"]'::jsonb ?| '{1,3,11,12,17}'::text[]"

        val parsedSql = parseSqlStatement(sql)
        assertEquals(0, parsedSql.totalParameterCount)
        assertEquals(expectedSql, parsedSql.jdbcSql)
    }

    @Test
    fun parseSqlStatementWithPostgresAllArrayStringsExistsOperator() {
        val expectedSql = "select '[\"3\", \"11\"]'::jsonb ?& '{1,3,11,12,17}'::text[] AND ? = 'Back in Black'"
        val sql = "select '[\"3\", \"11\"]'::jsonb ?& '{1,3,11,12,17}'::text[] AND :album = 'Back in Black'"

        val parsedSql = parseSqlStatement(sql)
        assertEquals(1, parsedSql.totalParameterCount)
        assertEquals(expectedSql, parsedSql.jdbcSql)
    }

    @Test
    fun parseSqlStatementWithEscapedColon() {
        val expectedSql = "select '0\\:0' as a, foo from bar where baz < DATE(? 23:59:59) and baz = ?"
        val sql = "select '0\\:0' as a, foo from bar where baz < DATE(:p1 23\\:59\\:59) and baz = :p2"

        val parsedSql = parseSqlStatement(sql)
        assertContentEquals(parsedSql.parameterNames, listOf("p1", "p2"))
        assertEquals(expectedSql, parsedSql.jdbcSql)
    }

    @Test
    fun parseSqlStatementWithBracketDelimitedParameterNames() {
        val expectedSql = "select foo from bar where baz = b??z"
        val sql = "select foo from bar where baz = b:{p1}:{p2}z"

        val parsedSql = parseSqlStatement(sql)
        assertEquals(expectedSql, parsedSql.jdbcSql)
        assertContentEquals(parsedSql.parameterNames, listOf("p1", "p2"))
    }

    @Test
    fun parseSqlStatementWithEmptyBracketsOrBracketsInQuotes() {
        val expectedSql = "select foo from bar where baz = b:{}z"
        val sql = "select foo from bar where baz = b:{}z"
        val parsedSql = parseSqlStatement(sql)
        assertEquals(expectedSql, parsedSql.jdbcSql)
        assertEquals(0, parsedSql.parameterNames.size)

        val expectedSql2 = "select foo from bar where baz = 'b:{p1}z'"
        val sql2 = "select foo from bar where baz = 'b:{p1}z'"

        val parsedSql2 = parseSqlStatement(sql2)
        assertEquals(expectedSql2, parsedSql2.jdbcSql)
        assertEquals(0, parsedSql2.parameterNames.size)
    }

    @Test
    fun parseSqlStatementWithSingleLetterInBrackets() {
        val expectedSql = "select foo from bar where baz = b?z"
        val sql = "select foo from bar where baz = b:{p}z"

        val parsedSql = parseSqlStatement(sql)
        assertEquals(expectedSql, parsedSql.jdbcSql)
        assertContentEquals(parsedSql.parameterNames, listOf("p"))
    }

    @Test
    fun parseSqlStatementWithLogicalAnd() {
        val expectedSql = "xxx & yyyy"
        val parsedSql = parseSqlStatement(expectedSql)
        assertEquals(expectedSql, parsedSql.jdbcSql)
    }

    @Test
    fun substituteNamedParametersWithLogicalAnd() {
        val expectedSql = "xxx & yyyy"
        val parsedSql = parseSqlStatement(expectedSql)
        assertEquals(expectedSql, parsedSql.jdbcSql)
    }

    @Test
    fun variableAssignmentOperator() {
        val expectedSql = "x := 1"
        val newSql: String = parseSqlStatement(expectedSql).jdbcSql
        assertEquals(expectedSql, newSql)
    }

    @Test
    fun parseSqlStatementWithSquareBracket() {
        val sql = "SELECT ARRAY[:ext]"
        val parsedSql = parseSqlStatement(sql)
        assertEquals(1, parsedSql.namedParameterCount)
        assertContentEquals(listOf("ext"), parsedSql.parameterNames)
        assertEquals("SELECT ARRAY[?]", parsedSql.jdbcSql)
    }

    @Test
    fun paramNameWithNestedSquareBrackets() {
        data class GeneratedAlways(val id: String, val firstName: String, val lastName: String) : KPojo

        val records = listOf(
            GeneratedAlways("1", "John", "Doe"),
            GeneratedAlways("2", "Jane", "Doe")
        )

        val paramMap = mapOf("records" to records)

        val sql = "insert into GeneratedAlways (id, first_name, last_name) values " +
                "(:records[0].id, :records[0].firstName, :records[0].lastName), " +
                "(:records[1].id, :records[1].firstName, :records[1].lastName)"

        val parsedSql = parseSqlStatement(sql, paramMap)
        assertContentEquals(
            listOf(
                "records[0].id", "records[0].firstName", "records[0].lastName",
                "records[1].id", "records[1].firstName", "records[1].lastName"
            ),
            parsedSql.parameterNames
        )
        assertEquals(
            "insert into GeneratedAlways (id, first_name, last_name) values (?, ?, ?), (?, ?, ?)",
            parsedSql.jdbcSql
        )
        assertEquals(6, parsedSql.jdbcParamList.size)
        assertContentEquals(
            arrayOf(
                "1", "John", "Doe",
                "2", "Jane", "Doe"
            ),
            parsedSql.jdbcParamList
        )
    }

    @Test
    fun namedParamMapReference() {
        val sql = "insert into foos (id) values (:headers[id])"
        val parsedSql = parseSqlStatement(sql)
        assertEquals(1, parsedSql.namedParameterCount)
        assertContentEquals(listOf("headers[id]"), parsedSql.parameterNames)

        val headers = mapOf(
            "id" to 1
        )

        val paramMap = mapOf("headers" to headers)

        assertEquals("insert into foos (id) values (?)", parseSqlStatement(sql, paramMap).jdbcSql)
        assertEquals(1, parseSqlStatement(sql, paramMap).jdbcParamList[0])

        val headerList = listOf(1)

        val sq2 = "insert into foos (id) values (:headers[0])"

        val paramMap2 = mapOf("headers" to headerList)
        val parsedSql2 = parseSqlStatement(sq2, paramMap2)

        assertEquals("insert into foos (id) values (?)", parsedSql2.jdbcSql)
        assertEquals(1, parsedSql2.jdbcParamList[0])

        val headerArr = arrayOf(1)

        val paramMap3 = mapOf("headers" to headerArr)
        val parsedSql3 = parseSqlStatement(sq2, paramMap3)

        assertEquals("insert into foos (id) values (?)", parsedSql3.jdbcSql)
        assertEquals(1, parsedSql3.jdbcParamList[0])
    }

    @Test
    fun parseSqlStatementWithBrackets() {
        val sql = "select * from `tb&user` where id in (:id)"
        val parsedSql = parseSqlStatement(sql, mapOf("id" to listOf(1)))
        assertContentEquals(listOf("id"), parsedSql.parameterNames)
        assertEquals("select * from `tb&user` where id in (?)", parsedSql.jdbcSql)
        assertEquals(listOf(1), parsedSql.jdbcParamList[0])
    }

    @Test
    fun parseSqlStatementWithBackticks() {
        val sql = "select * from `tb&user` where id = :id"
        val parsedSql = parseSqlStatement(sql)
        assertContentEquals(listOf("id"), parsedSql.parameterNames)
        assertEquals("select * from `tb&user` where id = ?", parsedSql.jdbcSql)
    }
}