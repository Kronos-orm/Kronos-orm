/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.orm.join

import com.kotlinorm.Kronos
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.testfixtures.cascade.onetoone.Car
import com.kotlinorm.testfixtures.cascade.onetoone.CarDetails
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.utils.KPojoFactory
import com.kotlinorm.utils.createKPojo
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SelectFromBehaviorTest : MysqlTestBase() {
    @Test
    fun `build retains the recursive query statement and result column types`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            leftJoin { user.id == relation.id2 }
                .select { [user.username, relation.id2] }
        }
        val task = query.build().atomicTask

        assertEquals(query.toSqlQuery(), task.statement)
        assertEquals(typeOf<String?>(), task.resultColumns["username"]?.type)
        assertEquals(typeOf<Int?>(), task.resultColumns["id2"]?.type)
    }

    @Test
    fun `relation operations are immutable and preserve reusable raw sources`() {
        val initial = TestUser().join(UserRelation()) { _, _ -> this }
        val left = initial.leftJoin { user -> user.id == 1 }
        val right = initial.rightJoin { user -> user.id == 2 }

        assertNotEquals(left.joinState.current, right.joinState.current)
        assertFailsWith<IllegalStateException> { initial.select { it.id } }
        left.select { it.id }
        right.select { it.id }
    }

    @Test
    fun `selectable join materialization passes the exact selected KType to a user factory`() {
        val selectedType = typeOf<TestUser?>()
        var factoryType: KType? = null
        val registration = Kronos.registerKPojoFactory(typeOf<TestUser>(), KPojoFactory { requestedType ->
            factoryType = requestedType
            TestUser(id = 17)
        })

        try {
            val (_, root) = tableSelectableJoinState(
                UserRelation(),
                typeOf<UserRelation>(),
                typeOf<UserRelation?>(),
                NullableTestUserQuery(selectedType)
            )

            assertSame(selectedType, factoryType)
            assertEquals(17, root.id)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `select refuses incomplete multi-source trees`() {
        assertFailsWith<IllegalStateException> {
            TestUser().join(UserRelation(), JoinTreeC()) { user, relation, _ ->
                leftJoin { user.id == relation.id2 }
                    .select { user.id }
            }
        }
    }

    @Test
    fun `relation refuses calls after every pending operand is consumed`() {
        assertFailsWith<IllegalStateException> {
            TestUser().join(UserRelation()) { user, relation ->
                leftJoin { user.id == relation.id2 }
                    .rightJoin { user.id == relation.id2 }
            }
        }
    }

    @Test
    fun `page view does not mutate the reusable joined query`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            leftJoin { user.id == relation.id2 }
                .select { user.id }
        }

        val baseSql = query.build().atomicTask.sql
        val pageSql = query.page(2, 5).build().atomicTask.sql

        assertEquals(baseSql, query.build().atomicTask.sql)
        assertNotEquals(baseSql, pageSql)
    }

    @Test
    fun `terminal mapping methods preserve explicit and selected result contracts`() {
        val query = joinQuery()
        val selectedType = query.selectedType
        val nullableSelectedType = query.nullableSelectedType
        val user = TestUser(id = 2, username = "typed")
        val row = mapOf<String, Any>("id" to 1, "gender" to 1)
        val scalar = mapOf<String, Any>("total" to 1)
        val selectedRow = createKPojo(selectedType).fromMapData<KPojo>(row)
        assertNotEquals(typeOf<TestUser>(), selectedType)
        assertEquals(selectedType.withNullability(true), nullableSelectedType)
        val wrapper = CapturingMysqlWrapper(
            mapRows = listOf(row),
            typedRows = listOf(user),
            selectedRows = listOf(selectedRow),
            mapResult = scalar,
            objectResult = user,
            selectedObjectResult = selectedRow
        )

        assertEquals(listOf(row), joinQuery().toMapList(wrapper))
        assertEquals(scalar, joinQuery().toMap(wrapper))
        assertEquals(scalar, joinQuery().toMapOrNull(wrapper))
        assertEquals(listOf(user), joinQuery().toList<TestUser>(wrapper))
        val defaultList = joinQuery().toList(wrapper)
        assertEquals(selectedType, defaultList.single().__kType)
        assertSelectedValues(defaultList.single())
        assertEquals(user, joinQuery().first<TestUser>(wrapper))
        val defaultOne = joinQuery().first(wrapper)
        assertEquals(selectedType, defaultOne.__kType)
        assertSelectedValues(defaultOne)
        assertEquals(user, joinQuery().firstOrNull<TestUser>(wrapper))
        val defaultOneOrNull = joinQuery().firstOrNull(wrapper)
        assertEquals(selectedType, defaultOneOrNull!!.__kType)
        assertSelectedValues(defaultOneOrNull)

        assertEquals(
            listOf(
                QueryCall.ToList(typeOf<Map<String, Any?>>()),
                QueryCall.First(typeOf<Map<String, Any?>>()),
                QueryCall.First(typeOf<Map<String, Any?>?>()),
                QueryCall.ToList(typeOf<TestUser>()),
                QueryCall.ToList(selectedType),
                QueryCall.First(typeOf<TestUser>()),
                QueryCall.First(selectedType),
                QueryCall.First(typeOf<TestUser?>()),
                QueryCall.First(nullableSelectedType)
            ),
            wrapper.calls
        )
    }

    @Test
    fun `toMapOrNull returns null and default first reports the generated SQL`() {
        val nullMapWrapper = CapturingMysqlWrapper(mapResult = null)
        assertEquals(null, joinQuery().toMapOrNull(nullMapWrapper))
        assertEquals(
            listOf<QueryCall>(QueryCall.First(typeOf<Map<String, Any?>?>())),
            nullMapWrapper.calls
        )

        val emptyWrapper = CapturingMysqlWrapper(objectResult = null)
        val error = assertFailsWith<NoSuchElementException> { joinQuery().first(emptyWrapper) }

        assertEquals(
            "No result found for query: SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender` " +
                "FROM `tb_user` LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` " +
                "WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 LIMIT 1",
            error.message
        )
        assertEquals(
            listOf<QueryCall>(QueryCall.First(joinQuery().selectedType)),
            emptyWrapper.calls
        )
    }

    @Test
    fun `joined query modifiers keep optional paths observable`() {
        val query = joinQuery()

        assertSame(query, query.limit(-1))
        assertSame(query, query.patch("id" to 9).lock(null).cascade(false).where(null))
        assertFailsWith<EmptyFieldsException> { query.orderBy(null) }
        assertFailsWith<EmptyFieldsException> { query.groupBy(null) }
        assertFailsWith<EmptyFieldsException> { query.having(null) }
        assertFailsWith<EmptyFieldsException> { query.by(null) }

        val filtered = TestUser(id = 4).join(UserRelation(id2 = 4)) { user, relation ->
            leftJoin { user.id == relation.id2 }
                .select { [user.id, relation.gender] }
                .by { user.id }
                .where(null)
                .having { relation.gender == 1 }
                .groupBy { user.id }
                .orderBy { user.id.desc() }
        }

        val sql = filtered.build().atomicTask.sql
        assertTrue(sql.contains("GROUP BY"))
        assertTrue(sql.contains("HAVING"))
        assertTrue(sql.contains("ORDER BY"))
    }

    @Test
    fun `joined cascade fields accept class qualified references`() {
        val query = CarDetails().join(Car()) { details, car ->
            leftJoin { details.carId == car.id }
                .select { details.id }
                .cascade { [CarDetails::car] }
        }

        assertEquals(true, query.context.cascadeEnabled)
        assertEquals(setOf("car"), query.context.cascadeAllowed!!.map { it.name }.toSet())
    }

    @Test
    fun `default terminal APIs and select all cursor retain their contracts`() {
        val query = joinQuery()
        val selectedType = query.selectedType
        val selectedRow = createKPojo(selectedType).fromMapData<KPojo>(mapOf("id" to 1, "gender" to 1))
        val mapRow = mapOf<String, Any>("id" to 1, "gender" to 1)
        val user = TestUser(id = 1, username = "typed")
        val wrapper = CapturingMysqlWrapper(
            mapRows = listOf(mapRow),
            typedRows = listOf(user),
            selectedRows = listOf(selectedRow),
            mapResult = mapRow,
            objectResult = user,
            selectedObjectResult = selectedRow
        )
        val previousDataSource = Kronos.dataSource

        try {
            Kronos.dataSource = { wrapper }
            assertSame(query, query.single())
            assertSame(query, query.db(query.state.sources.last() to "archive"))
            assertSame(query, query.lock())
            assertEquals(listOf(mapRow), query.toMapList())
            assertEquals(mapRow, query.toMap())
            assertEquals(mapRow, query.toMapOrNull())
            assertEquals(listOf(user), query.toList<TestUser>())
            assertEquals(listOf(selectedRow), query.toList())
            assertEquals(user, query.first<TestUser>())
            val selected = query.first()
            assertEquals(1, selected.id)
            assertEquals(1, selected.gender)
            assertEquals(user, query.firstOrNull<TestUser>())
            val selectedNullable = query.firstOrNull()
            assertEquals(1, selectedNullable?.id)
            assertEquals(1, selectedNullable?.gender)
            assertFailsWith<EmptyFieldsException> { query.having() }
            assertFailsWith<EmptyFieldsException> { query.cascade { } }

            val selectAll = TestUser().join(UserRelation()) { source, relation ->
                innerJoin { source.id == relation.id2 }
                    .select()
                    .orderBy { source.id.asc() }
            }
            val cursorTask = selectAll.cursor(pageSize = 1).build().atomicTask
            assertTrue(cursorTask.sql.contains("ORDER BY"))
        } finally {
            Kronos.dataSource = previousDataSource
        }
    }

    private fun joinQuery() =
        TestUser(id = 1).join(UserRelation(id = 1, username = "test", gender = 1, id2 = 1)) { user, relation ->
            leftJoin { user.id == relation.id2 }
                .select { [user.id, relation.gender] }
        }

    private fun assertSelectedValues(row: KPojo) {
        assertEquals(1, row["id"])
        assertEquals(1, row["gender"])
    }
}

private class NullableTestUserQuery(
    override val selectedType: KType
) : KSelectable<TestUser>(TestUser()) {
    override val nullableSelectedType: KType = selectedType

    override fun build(wrapper: com.kotlinorm.interfaces.KronosDataSourceWrapper?): KronosQueryTask =
        error("Not used by this factory-path test")

    override fun toSqlQueryPlan(wrapper: com.kotlinorm.interfaces.KronosDataSourceWrapper?): SqlQueryPlan =
        error("Not used by this factory-path test")
}

private class CapturingMysqlWrapper(
    private val mapRows: List<Map<String, Any>> = emptyList(),
    private val typedRows: List<Any?> = emptyList(),
    private val selectedRows: List<Any?> = emptyList(),
    private val mapResult: Map<String, Any>? = null,
    private val objectResult: Any? = null,
    private val selectedObjectResult: Any? = null
) : SampleMysqlJdbcWrapper() {
    val calls = mutableListOf<QueryCall>()

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        calls += QueryCall.ToList(task.targetType)
        return when {
            task.targetType == typeOf<Map<String, Any?>>() -> mapRows
            task.targetType.classifier == TestUser::class -> typedRows
            else -> selectedRows
        }
    }

    override fun first(task: KAtomicQueryTask): Any? {
        calls += QueryCall.First(task.targetType)
        return if (task.targetType == typeOf<Map<String, Any?>>() ||
            task.targetType == typeOf<Map<String, Any?>?>()
        ) {
            mapResult
        } else if (task.targetType.classifier == TestUser::class) {
            objectResult
        } else {
            selectedObjectResult
        }
    }
}

private sealed interface QueryCall {
    data class ToList(val targetType: KType) : QueryCall
    data class First(val targetType: KType) : QueryCall
}
