package com.kotlinorm.utils

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.ConditionType
import kotlin.test.Test
import kotlin.test.assertEquals

class ConditionBuilderTest {

    @Test
    fun test() {
        val condition = "id".eq(1)

        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())

        val expect = "id = :id"
        val paramMap = mapOf("id" to 1)
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test2() {
        val condition = Criteria(
            field = Field("id"),
            not = true,
            type = ConditionType.EQUAL,
            value = 1
        )

        val expect = "id != :id"
        val paramMap = mapOf("id" to 1)
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test3() {
        val condition = Criteria(
            field = Field("username"),
            type = ConditionType.LIKE,
            value = 1
        )

        val expect = "username LIKE :username"
        val paramMap = mapOf("username" to 1)
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test4() {
        val condition = Criteria(
            field = Field("username"),
            not = true,
            type = ConditionType.LIKE,
            value = 1
        )

        val expect = "username NOT LIKE :username"
        val paramMap = mapOf("username" to 1)
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test5() {
        val condition = Criteria(
            field = Field("id"),
            type = ConditionType.LT,
            value = 1
        )

        val expect = "id < :idMax"
        val paramMap = mapOf("idMax" to 1)
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test6() {
        val condition = Criteria(
            field = Field("id"),
            type = ConditionType.LE,
            value = 1
        )

        val expect = "id <= :idMax"
        val paramMap = mapOf("idMax" to 1)
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test7() {
        val condition = Criteria(
            field = Field("id"),
            type = ConditionType.GT,
            value = 1
        )

        val expect = "id > :idMin"
        val paramMap = mapOf("idMin" to 1)
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test8() {
        val condition = Criteria(
            field = Field("id"),
            type = ConditionType.GE,
            value = 1
        )

        val expect = "id >= :idMin"
        val paramMap = mapOf("idMin" to 1)
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test9() {
        val condition = Criteria(
            field = Field("id"),
            type = ConditionType.BETWEEN,
            value = 1.rangeTo(10)
        )

        val expect = "id BETWEEN :idMin AND :idMax"
        val paramMap = mapOf("idMin" to 1, "idMax" to 10)
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test10() {
        val condition = Criteria(
            field = Field("id"),
            not = true,
            type = ConditionType.BETWEEN,
            value = 1.rangeTo(10)
        )

        val expect = "id NOT BETWEEN :idMin AND :idMax"
        val paramMap = mapOf("idMin" to 1, "idMax" to 10)
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test11() {
        val condition = Criteria(
            field = Field("id"),
            type = ConditionType.ISNULL
        )

        val expect = "id IS NULL"
        val paramMap = emptyMap<String, Any>()
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test12() {
        val condition = Criteria(
            field = Field("id"),
            type = ConditionType.ISNULL,
            not = true
        )

        val expect = "id IS NOT NULL"
        val paramMap = emptyMap<String, Any>()
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test13() {
        val condition = Criteria(
            type = ConditionType.SQL,
            value = "id = 1"
        )

        val expect = "id = 1"
        val paramMap = emptyMap<String, Any>()
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test14() {
        val condition = Criteria(
            type = ConditionType.AND,
            children = listOf(
                Criteria(
                    field = Field("id"),
                    type = ConditionType.EQUAL,
                    value = 1
                ),
                Criteria(
                    field = Field("gender"),
                    type = ConditionType.EQUAL,
                    value = 0
                )
            ).toMutableList()
        )

        val expect = "id = :id AND gender = :gender"
        val paramMap = mapOf("id" to 1 , "gender" to 0)
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test15() {
        val condition = Criteria(
            type = ConditionType.OR,
            children = listOf(
                Criteria(
                    field = Field("id"),
                    type = ConditionType.EQUAL,
                    value = 1
                ),
                Criteria(
                    field = Field("gender"),
                    type = ConditionType.EQUAL,
                    value = 0
                )
            ).toMutableList()
        )

        val expect = "id = :id OR gender = :gender"
        val paramMap = mapOf("id" to 1 , "gender" to 0)
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test16() {
        val condition = Criteria(
            field = Field("id"),
            type = ConditionType.IN,
            value = listOf(1,2,3)
        )

        val expect = "id IN (:idList)"
        val paramMap = mapOf("idList" to listOf(1,2,3))
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test17() {
        val condition = Criteria(
            field = Field("id"),
            not = true,
            type = ConditionType.IN,
            value = listOf(1,2,3)
        )

        val expect = "id NOT IN (:idList)"
        val paramMap = mapOf("idList" to listOf(1,2,3))
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

    @Test
    fun test18() {
        val condition = "id".eq(1) or "id".eq(2)

        val expect = "id = id OR id = id@2"
        val paramMap = mapOf("id" to 1 , "id@2" to 2)
        val (sql, paramMap2) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2.toMap())
    }

}