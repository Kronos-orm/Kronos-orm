/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm.orm.join

import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testfixtures.cascade.onetoone.Car
import com.kotlinorm.testfixtures.cascade.onetoone.CarDetails
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SelectFromBehaviorTest : MysqlTestBase() {

    @Test
    fun `build carries join syntax query statement`() {
        val selectFrom = TestUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { [user.id, user.username, relation.gender] }
        }

        val task = selectFrom.build()

        assertEquals(selectFrom.toSqlQuery(), task.atomicTask.statement)
    }

    @Test
    fun `join selectable currently keeps source class as selected type`() {
        val wrapper = CapturingMysqlWrapper()
        val selectFrom = TestUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { [user.id, relation.gender] }
        }

        selectFrom.queryList(wrapper)

        assertEquals(TestUser::class, selectFrom.selectedKClass)
        assertEquals(
            listOf<QueryCall>(QueryCall.ForListTyped(TestUser::class, true, emptyList<String>())),
            wrapper.calls
        )
    }

    @Test
    fun `option methods mutate join context directly`() {
        val selectFrom = joinQuery()

        selectFrom.cascade(false)
        selectFrom.patch("id" to 7)
        selectFrom.db(TestUser() to "archive")

        assertEquals(false, selectFrom.context.cascadeEnabled)
        assertEquals(7, selectFrom.context.paramMap["id"])
        assertEquals(mapOf("tb_user" to "archive"), selectFrom.context.databaseOfTable)
    }

    @Test
    fun `field callbacks update join context conditions and cascade scope`() {
        val selectFrom = joinQuery()

        selectFrom.by { [it.id, it.username] }

        assertEquals(
            listOf("Binary", "Binary"),
            listOf(
                selectFrom.context.where!!::class.simpleName,
                selectFrom.context.joinables.single().condition!!::class.simpleName
            )
        )
    }

    @Test
    fun `on where and having callbacks materialize join conditions and parameters`() {
        val selectFrom = TestUser(7, "neo").join(UserRelation(1, "test", 1, 1)) { user, relation ->
            select { [user.id, relation.gender] }
        }

        selectFrom.on { it.id == 7 }
        selectFrom.patch("username" to "neo")
        selectFrom.where()
        selectFrom.having { it.username == "neo" }

        assertEquals(1, selectFrom.context.joinables.size)
        assertEquals("Binary", selectFrom.context.joinables.single().condition!!::class.simpleName)
        assertEquals("Binary", selectFrom.context.where!!::class.simpleName)
        assertEquals("Binary", selectFrom.context.having!!::class.simpleName)
        assertEquals(mapOf<String, Any?>("id" to 7, "username" to "neo"), selectFrom.context.paramMap)
        assertEquals(true, selectFrom.context.havingEnabled)
    }

    @Test
    fun `valid group order distinct paging and limit callbacks mutate join context`() {
        val selectFrom = joinQuery()

        selectFrom.groupBy { [it.id, it.username] }
        selectFrom.orderBy { it.id.desc() }
        selectFrom.distinct()
        selectFrom.page(3, 20)
        selectFrom.limit(5)

        assertEquals(true, selectFrom.context.groupEnabled)
        assertEquals(setOf("id", "username"), selectFrom.context.groupByFields.map { it.name }.toSet())
        assertEquals(true, selectFrom.context.orderEnabled)
        assertEquals(listOf("FieldItem"), selectFrom.context.orderByItems.map { it::class.simpleName })
        assertEquals(true, selectFrom.context.distinctEnabled)
        assertEquals(true, selectFrom.context.pageEnabled)
        assertEquals(20, selectFrom.context.pageSize)
        assertEquals(3, selectFrom.context.pageIndex)
        assertEquals(5, selectFrom.context.limitCapacity)
    }

    @Test
    fun `cascade reference callback records selected join cascade fields`() {
        val selectFrom = CarDetails().join(Car()) { details, car ->
            leftJoin(car) { details.carId == car.id }
            select { details.id }
        }

        selectFrom.cascade { [CarDetails::car] }

        assertEquals(true, selectFrom.context.cascadeEnabled)
        assertEquals(setOf("car"), selectFrom.context.cascadeAllowed!!.map { it.name }.toSet())
    }

    @Test
    fun `null join field callbacks fail fast`() {
        assertEquals(
            listOf(
                EmptyFieldsException::class,
                EmptyFieldsException::class,
                EmptyFieldsException::class,
                EmptyFieldsException::class,
                EmptyFieldsException::class,
                EmptyFieldsException::class
            ),
            listOf(
                assertFailsWith<EmptyFieldsException> { joinQuery().on(null) }::class,
                assertFailsWith<EmptyFieldsException> { joinQuery().by(null) }::class,
                assertFailsWith<EmptyFieldsException> { joinQuery().cascade(null) }::class,
                assertFailsWith<EmptyFieldsException> { joinQuery().having(null) }::class,
                assertFailsWith<EmptyFieldsException> { joinQuery().groupBy(null) }::class,
                assertFailsWith<EmptyFieldsException> { joinQuery().orderBy(null) }::class
            )
        )
    }

    @Test
    fun `terminal query methods route to wrapper with expected result contracts`() {
        val user = TestUser(2, "typed")
        val row = mapOf<String, Any>("id" to 1, "username" to "row")
        val scalar = mapOf<String, Any>("total" to 1)
        val wrapper = CapturingMysqlWrapper(
            mapRows = listOf(row),
            typedRows = listOf(user),
            mapResult = scalar,
            objectResult = user
        )

        assertEquals(listOf(row), joinQuery().query(wrapper))
        assertEquals(scalar, joinQuery().queryMap(wrapper))
        assertEquals(scalar, joinQuery().queryMapOrNull(wrapper))
        assertEquals(listOf(user), joinQuery().queryList<TestUser>(wrapper, true, listOf("Marker")))
        val defaultList: List<TestUser> = joinQuery().queryList(wrapper)
        assertEquals(listOf(user), defaultList)
        assertEquals(user, joinQuery().queryOne<TestUser>(wrapper, true, listOf("Marker")))
        val defaultOne: TestUser = joinQuery().queryOne(wrapper)
        assertEquals(user, defaultOne)
        assertEquals(user, joinQuery().queryOneOrNull<TestUser>(wrapper, true, listOf("Marker")))
        val defaultOneOrNull: TestUser? = joinQuery().queryOneOrNull(wrapper)
        assertEquals(user, defaultOneOrNull)

        assertEquals(
            listOf<QueryCall>(
                QueryCall.ForListMap,
                QueryCall.ForMap,
                QueryCall.ForMap,
                QueryCall.ForListTyped(TestUser::class, true, listOf("kotlin.Any", "com.kotlinorm.interfaces.KPojo")),
                QueryCall.ForListTyped(TestUser::class, true, emptyList<String>()),
                QueryCall.ForObject(TestUser::class, true, listOf("kotlin.Any", "com.kotlinorm.interfaces.KPojo")),
                QueryCall.ForObject(TestUser::class, true, emptyList<String>()),
                QueryCall.ForObject(TestUser::class, true, listOf("kotlin.Any", "com.kotlinorm.interfaces.KPojo")),
                QueryCall.ForObject(TestUser::class, true, emptyList<String>())
            ),
            wrapper.calls
        )
    }

    @Test
    fun `queryMapOrNull returns null without throwing`() {
        val wrapper = CapturingMysqlWrapper(mapResult = null)

        assertEquals(null, joinQuery().queryMapOrNull(wrapper))
        assertEquals(listOf<QueryCall>(QueryCall.ForMap), wrapper.calls)
    }

    @Test
    fun `default queryOne reports no record when wrapper returns null`() {
        val wrapper = CapturingMysqlWrapper(objectResult = null)

        val error = assertFailsWith<NullPointerException> {
            val result: TestUser = joinQuery().queryOne(wrapper)
            result
        }

        assertEquals("No such record", error.message)
        assertEquals(
            listOf<QueryCall>(QueryCall.ForObject(TestUser::class, true, emptyList<String>())),
            wrapper.calls
        )
    }

    private fun joinQuery(): SelectFrom2<TestUser, UserRelation> =
        TestUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { [user.id, relation.gender] }
        }
}

private class CapturingMysqlWrapper(
    private val mapRows: List<Map<String, Any>> = emptyList(),
    private val typedRows: List<Any> = emptyList(),
    private val mapResult: Map<String, Any>? = null,
    private val objectResult: Any? = null
) : SampleMysqlJdbcWrapper() {
    val calls = mutableListOf<QueryCall>()

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
        calls += QueryCall.ForListMap
        return mapRows
    }

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> {
        calls += QueryCall.ForListTyped(kClass, isKPojo, superTypes)
        return typedRows
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? {
        calls += QueryCall.ForMap
        return mapResult
    }

    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): Any? {
        calls += QueryCall.ForObject(kClass, isKPojo, superTypes)
        return objectResult
    }
}

private sealed interface QueryCall {
    object ForListMap : QueryCall
    object ForMap : QueryCall
    data class ForListTyped(
        val kClass: KClass<*>,
        val isKPojo: Boolean,
        val superTypes: List<String>
    ) : QueryCall
    data class ForObject(
        val kClass: KClass<*>,
        val isKPojo: Boolean,
        val superTypes: List<String>
    ) : QueryCall
}
