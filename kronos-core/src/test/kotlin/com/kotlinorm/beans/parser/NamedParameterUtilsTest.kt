package com.kotlinorm.beans.parser

import com.kotlinorm.beans.parser.NamedParameterUtils.buildValueArray
import com.kotlinorm.beans.parser.NamedParameterUtils.parseSqlStatement
import com.kotlinorm.exceptions.InvalidDataAccessApiUsageException
import com.kotlinorm.exceptions.InvalidParameterException
import com.kotlinorm.interfaces.KPojo
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class NamedParameterUtilsTest {
    @Test
    fun parseSql() {
        val sql = "xxx :a yyyy :b :c :a zzzzz"
        val parsedSql = parseSqlStatement(sql)
        assertEquals("xxx ? yyyy ? ? ? zzzzz", parsedSql.jdbcSql)
        assertEquals(["a", "b", "c", "a"], parsedSql.parameterNames)
        assertEquals(4, parsedSql.totalParameterCount)
        assertEquals(3, parsedSql.namedParameterCount)

        val sql2 = "xxx &a yyyy ? zzzzz"
        val parsedSql2 = parseSqlStatement(sql2)
        assertEquals("xxx ? yyyy ? zzzzz", parsedSql2.jdbcSql)
        assertEquals(["a"], parsedSql2.parameterNames)
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
        assertContentEquals(parsedSql.parameterNames, ["p1", "p2"])
        assertEquals(expectedSql, parsedSql.jdbcSql)
    }

    @Test
    fun parseSqlStatementWithBracketDelimitedParameterNames() {
        val expectedSql = "select foo from bar where baz = b??z"
        val sql = "select foo from bar where baz = b:{p1}:{p2}z"

        val parsedSql = parseSqlStatement(sql)
        assertEquals(expectedSql, parsedSql.jdbcSql)
        assertContentEquals(parsedSql.parameterNames, ["p1", "p2"])
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
        assertContentEquals(parsedSql.parameterNames, ["p"])
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
        assertContentEquals(["ext"], parsedSql.parameterNames)
        assertEquals("SELECT ARRAY[?]", parsedSql.jdbcSql)
    }

    @Test
    fun paramNameWithNestedSquareBrackets() {
        data class GeneratedAlways(val id: String, val firstName: String, val lastName: String) : KPojo

        val records = [
            GeneratedAlways("1", "John", "Doe"),
            GeneratedAlways("2", "Jane", "Doe")
        ]

        val paramMap = mapOf("records" to records)

        val sql = "insert into GeneratedAlways (id, first_name, last_name) values " +
                "(:records[0].id, :records[0].firstName, :records[0].lastName), " +
                "(:records[1].id, :records[1].firstName, :records[1].lastName)"

        val parsedSql = parseSqlStatement(sql, paramMap)
        assertContentEquals(
            [
                "records[0].id", "records[0].firstName", "records[0].lastName",
                "records[1].id", "records[1].firstName", "records[1].lastName"
            ],
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
        assertContentEquals(["headers[id]"], parsedSql.parameterNames)

        val headers = mapOf(
            "id" to 1
        )

        val paramMap = mapOf("headers" to headers)

        assertEquals("insert into foos (id) values (?)", parseSqlStatement(sql, paramMap).jdbcSql)
        assertEquals(1, parseSqlStatement(sql, paramMap).jdbcParamList[0])

        val headerList = [1]

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
    fun parseSqlStatementWithExpandableListOccurrence() {
        val sql = "select * from `tb&user` where id in (:id)"
        val parsedSql = parseSqlStatement(
            sql,
            mapOf("id" to listOf(1, 2, 3)),
            listParameterOccurrences = setOf(0)
        )
        assertContentEquals(["id"], parsedSql.parameterNames)
        assertEquals("select * from `tb&user` where id in (?, ?, ?)", parsedSql.jdbcSql)
        assertContentEquals([1, 2, 3], parsedSql.jdbcParamList.toList())
    }

    @Test
    fun plainCollectionArrayPrimitiveArrayAndByteArrayBindAsSingleParameter() {
        val payload = byteArrayOf(0, 1, 2, 3, 127, -128)
        val plainValues = listOf(
            "ids" to listOf(1, 2, 3),
            "arrayIds" to arrayOf(1, 2),
            "intIds" to intArrayOf(1, 2),
            "longIds" to longArrayOf(1L, 2L),
            "payload" to payload,
        )

        plainValues.forEach { (name, value) ->
            val parsedSql = parseSqlStatement("select * from t where value in (:$name)", mapOf(name to value))
            assertEquals("select * from t where value in (?)", parsedSql.jdbcSql)
            assertEquals(1, parsedSql.jdbcParamList.size)
        }

        assertEquals(listOf(1, 2, 3), parseSqlStatement("select :ids", mapOf("ids" to listOf(1, 2, 3))).jdbcParamList.single())
        assertEquals(
            listOf(1, 2),
            (parseSqlStatement("select :arrayIds", mapOf("arrayIds" to arrayOf(1, 2))).jdbcParamList.single() as Array<*>).toList()
        )
        assertContentEquals(
            intArrayOf(1, 2),
            parseSqlStatement("select :intIds", mapOf("intIds" to intArrayOf(1, 2))).jdbcParamList.single() as IntArray
        )
        assertContentEquals(
            longArrayOf(1L, 2L),
            parseSqlStatement("select :longIds", mapOf("longIds" to longArrayOf(1L, 2L))).jdbcParamList.single() as LongArray
        )
        assertContentEquals(
            payload,
            parseSqlStatement("select :payload", mapOf("payload" to payload)).jdbcParamList.single() as ByteArray
        )
    }

    @Test
    fun repeatedExpandableListOccurrenceExpandsEachReference() {
        val parsedSql = parseSqlStatement(
            "select * from t where id in (:ids) or parent_id in (:ids)",
            mapOf("ids" to listOf(1, 2)),
            listParameterOccurrences = setOf(0, 1)
        )

        assertEquals("select * from t where id in (?, ?) or parent_id in (?, ?)", parsedSql.jdbcSql)
        assertContentEquals([1, 2, 1, 2], parsedSql.jdbcParamList.toList())
    }

    @Test
    fun emptyExpandableListOccurrenceFailsBeforeExecution() {
        val error = assertFailsWith<InvalidDataAccessApiUsageException> {
            parseSqlStatement(
                "select * from t where id in (:ids)",
                mapOf("ids" to emptyList<Int>()),
                listParameterOccurrences = setOf(0)
            )
        }

        assertEquals(
            "SQL list parameter occurrence 'ids' must contain at least one value. Handle empty lists before binding list parameters.",
            error.message
        )
    }

    @Test
    fun expandableListOccurrenceSupportsObjectAndPrimitiveArrays() {
        val arrayIds = arrayOf(1, 2)
        val intIds = intArrayOf(3, 4)
        val longIds = longArrayOf(5L, 6L)
        val parsedSql = parseSqlStatement(
            "select * from t where a in (:arrayIds) or b in (:intIds) or c in (:longIds)",
            mapOf("arrayIds" to arrayIds, "intIds" to intIds, "longIds" to longIds),
            listParameterOccurrences = setOf(0, 1, 2)
        )

        assertEquals("select * from t where a in (?, ?) or b in (?, ?) or c in (?, ?)", parsedSql.jdbcSql)
        assertContentEquals(listOf<Any?>(1, 2, 3, 4, 5L, 6L), parsedSql.jdbcParamList.toList())
    }

    @Test
    fun indexedPathAccessStillBindsOneScalarFromListAndArrays() {
        val sql = "select :headers[0], :arrayHeaders[1], :intHeaders[0], :longHeaders[1]"
        val parsedSql = parseSqlStatement(
            sql,
            mapOf(
                "headers" to listOf(10, 11),
                "arrayHeaders" to arrayOf(20, 21),
                "intHeaders" to intArrayOf(30, 31),
                "longHeaders" to longArrayOf(40L, 41L),
            )
        )

        assertEquals("select ?, ?, ?, ?", parsedSql.jdbcSql)
        assertContentEquals(listOf<Any?>(10, 21, 30, 41L), parsedSql.jdbcParamList.toList())
    }

    @Test
    fun nestedArrayInsideExpandableListOccurrenceBindsAsScalarElement() {
        val nested = arrayOf(1, 2)
        val parsedSql = parseSqlStatement(
            "select * from t where value in (:values)",
            mapOf("values" to listOf(nested, 3)),
            listParameterOccurrences = setOf(0)
        )

        assertEquals("select * from t where value in (?, ?)", parsedSql.jdbcSql)
        assertEquals(2, parsedSql.jdbcParamList.size)
        assertEquals(listOf(1, 2), (parsedSql.jdbcParamList[0] as Array<*>).toList())
        assertEquals(3, parsedSql.jdbcParamList[1])
    }

    @Test
    fun parseSqlStatementWithBackticks() {
        val sql = "select * from `tb&user` where id = :id"
        val parsedSql = parseSqlStatement(sql)
        assertContentEquals(["id"], parsedSql.parameterNames)
        assertEquals("select * from `tb&user` where id = ?", parsedSql.jdbcSql)
    }

    @Test
    fun parseSqlStatementHandlesEmptySql() {
        val sql = ""
        val parsedSql = parseSqlStatement(sql)
        assertEquals("", parsedSql.jdbcSql)
        assertEquals(0, parsedSql.namedParameterCount)
        assertEquals(0, parsedSql.totalParameterCount)
    }

    @Test
    fun parseSqlStatementHandlesSqlWithoutParameters() {
        val sql = "SELECT * FROM users"
        val parsedSql = parseSqlStatement(sql)
        assertEquals("SELECT * FROM users", parsedSql.jdbcSql)
        assertEquals(0, parsedSql.namedParameterCount)
        assertEquals(0, parsedSql.totalParameterCount)
    }

    @Test
    fun parseSqlStatementThrowsExceptionForUnclosedNamedParameter() {
        val sql = "SELECT * FROM users WHERE id = :id AND name = :{name"
        assertFailsWith<InvalidParameterException> { parseSqlStatement(sql) }
    }

    @Test
    fun parseSqlStatementHandlesNestedParameters() {
        val sql = "SELECT * FROM users WHERE data->'info'->>'name' = :name"
        val parsedSql = parseSqlStatement(sql)
        assertEquals("SELECT * FROM users WHERE data->'info'->>'name' = ?", parsedSql.jdbcSql)
        assertContentEquals(["name"], parsedSql.parameterNames)
    }

    @Test
    fun parseSqlStatementHandlesEscapedParameters() {
        val sql = "SELECT * FROM users WHERE id = \\:id AND name = :name"
        val parsedSql = parseSqlStatement(sql)
        assertEquals("SELECT * FROM users WHERE id = :id AND name = ?", parsedSql.jdbcSql)
        assertContentEquals(["name"], parsedSql.parameterNames)
    }

    @Test
    fun buildValueArrayHandlesNullValues() {
        val sql = "SELECT * FROM users WHERE id = :id AND name = :name"
        val paramMap = mapOf("id" to null, "name" to "John")
        val parsedSql = parseSqlStatement(sql, paramMap)
        val valueArray = buildValueArray(parsedSql, paramMap)
        assertContentEquals(arrayOf(null, "John"), valueArray)
    }

    @Test
    fun buildValueArrayThrowsExceptionForMixedPlaceholders() {
        val sql = "SELECT * FROM users WHERE id = :id AND name = ?"
        val paramMap = mapOf("id" to 1)
        val parsedSql = parseSqlStatement(sql, paramMap)
        assertFailsWith<InvalidDataAccessApiUsageException> { buildValueArray(parsedSql, paramMap) }
    }
}
