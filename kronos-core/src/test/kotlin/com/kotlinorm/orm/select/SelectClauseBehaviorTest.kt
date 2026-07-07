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

package com.kotlinorm.orm.select

import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testfixtures.cascade.onetoone.Car
import com.kotlinorm.testfixtures.cascade.onetoone.CarDetails
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.statement.SqlLock
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SelectClauseBehaviorTest : MysqlTestBase() {

    @Test
    fun `build carries select syntax query statement`() {
        val clause = TestUser().select().where { it.id == 1 }

        val task = clause.build()

        assertEquals(clause.toSqlQuery(), task.atomicTask.statement)
    }

    @Test
    fun `select clause uses projection class as selected type`() {
        val wrapper = CapturingMysqlWrapper()
        val clause = TestUser().select(UserRelation::class) { it.username }

        clause.queryList(wrapper)

        assertEquals(UserRelation::class, clause.selectedKClass)
        assertEquals(
            listOf<QueryCall>(QueryCall.ForListTyped(UserRelation::class, true, emptyList<String>())),
            wrapper.calls
        )
    }

    @Test
    fun `option methods mutate select context directly`() {
        val clause = TestUser()
            .select()
            .cascade(false)
            .patch("id" to 7)
            .lock(SqlLock.Update())
            .db("archive")

        assertEquals(false, clause.context.cascadeEnabled)
        assertEquals(mapOf<String, Any?>("id" to 7), clause.context.patchValues)
        assertEquals(7, clause.context.sourceValues["id"])
        assertEquals(SqlLock.Update(), clause.context.lock)
        assertEquals("archive", clause.context.databaseName)
    }

    @Test
    fun `field callbacks update select context conditions and cascade scope`() {
        val clause = TestUser(7, "neo")
            .select()
            .by { [it.id, it.username] }

        assertEquals(mapOf<String, Any?>("id" to 7, "username" to "neo"), clause.context.parameterValues)
        assertEquals("Binary", clause.context.where!!::class.simpleName)
    }

    @Test
    fun `empty where uses non null source values and excludes logic delete field`() {
        val (_, paramMap) = TestUser(id = 7, username = "neo", deleted = true)
            .select()
            .where()
            .build()

        assertEquals(
            linkedMapOf<String, Any?>("id" to 7, "username" to "neo"),
            paramMap
        )
    }

    @Test
    fun `empty where excludes cascade fields from query by example values`() {
        val (_, paramMap) = CarDetails(id = 3, carId = 7, car = Car(id = 7), deleted = true)
            .select()
            .where()
            .build()

        assertEquals(
            linkedMapOf<String, Any?>("id" to 3, "carId" to 7),
            paramMap
        )
    }

    @Test
    fun `multiple where calls keep parameter values from each condition`() {
        val (_, paramMap) = TestUser()
            .select()
            .where { it.id == 1 }
            .where { it.id == 2 }
            .build()

        assertEquals(
            linkedMapOf<String, Any?>("id" to 1, "id@1" to 2),
            paramMap
        )
    }

    @Test
    fun `group order having page and limit callbacks mutate complete select context`() {
        val clause = TestUser(7, "neo")
            .select { [it.id, it.username] }
            .where()
            .groupBy { [it.gender] }
            .having { it.gender == 1 }
            .orderBy { [it.id.desc(), it.username.asc()] }
            .page(2, 10)

        assertEquals(setOf("id", "username"), clause.context.selectedFields.map { it.name }.toSet())
        assertEquals(listOf("gender"), clause.context.groupByItems.map { it.toString().substringAfter("columnName=").substringBefore(",") })
        assertEquals("Binary", clause.context.where!!::class.simpleName)
        assertEquals("Binary", clause.context.having!!::class.simpleName)
        assertEquals(listOf("FieldItem", "FieldItem"), clause.context.orderByItems.map { it::class.simpleName })
        assertEquals(SqlLimit.limit(10, 10), clause.context.limit)
    }

    @Test
    fun `limit ignores non positive values and records positive fetch`() {
        val clause = TestUser().select().limit(0).limit(-1)

        assertEquals(null, clause.context.limit)
        assertEquals(SqlLimit.limit(3), clause.limit(3).context.limit)
    }

    @Test
    fun `cascade reference callback records selected cascade fields`() {
        val clause = CarDetails()
            .select()
            .cascade { [CarDetails::car] }

        assertEquals(true, clause.context.cascadeEnabled)
        assertEquals(setOf("car"), clause.context.cascadeAllowed!!.map { it.name }.toSet())
    }

    @Test
    fun `null select field callbacks fail fast`() {
        assertEquals(
            listOf(
                EmptyFieldsException::class,
                EmptyFieldsException::class,
                EmptyFieldsException::class,
                EmptyFieldsException::class,
                EmptyFieldsException::class
            ),
            listOf(
                assertFailsWith<EmptyFieldsException> { TestUser().select().by(null) }::class,
                assertFailsWith<EmptyFieldsException> { TestUser().select().cascade(null) }::class,
                assertFailsWith<EmptyFieldsException> { TestUser().select().having(null) }::class,
                assertFailsWith<EmptyFieldsException> { TestUser().select().groupBy(null) }::class,
                assertFailsWith<EmptyFieldsException> { TestUser().select().orderBy(null) }::class
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

        assertEquals(listOf(row), TestUser().select().query(wrapper))
        assertEquals(scalar, TestUser().select().queryMap(wrapper))
        assertEquals(scalar, TestUser().select().queryMapOrNull(wrapper))
        assertEquals(listOf(user), TestUser().select().queryList<TestUser>(wrapper, true, listOf("Marker")))
        val defaultList: List<TestUser> = TestUser().select().queryList(wrapper)
        assertEquals(listOf(user), defaultList)
        assertEquals(user, TestUser().select().queryOne<TestUser>(wrapper, true, listOf("Marker")))
        val defaultOne: TestUser = TestUser().select().queryOne(wrapper)
        assertEquals(user, defaultOne)
        assertEquals(user, TestUser().select().queryOneOrNull<TestUser>(wrapper, true, listOf("Marker")))
        val defaultOneOrNull: TestUser? = TestUser().select().queryOneOrNull(wrapper)
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

        assertEquals(null, TestUser().select().queryMapOrNull(wrapper))
        assertEquals(listOf<QueryCall>(QueryCall.ForMap), wrapper.calls)
    }

    @Test
    fun `default queryOne reports no record when wrapper returns null`() {
        val wrapper = CapturingMysqlWrapper(objectResult = null)

        val error = assertFailsWith<NullPointerException> {
            val result: TestUser = TestUser().select().queryOne(wrapper)
            result
        }

        assertEquals("No such record", error.message)
        assertEquals(
            listOf<QueryCall>(QueryCall.ForObject(TestUser::class, true, emptyList<String>())),
            wrapper.calls
        )
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
