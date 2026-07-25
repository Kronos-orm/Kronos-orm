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

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.orm.select

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testfixtures.cascade.onetoone.Car
import com.kotlinorm.testfixtures.cascade.onetoone.CarDetails
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosRow
import com.kotlinorm.interfaces.KronosRowFirstResult
import com.kotlinorm.interfaces.KronosRowMappingDataSourceWrapper
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlLock
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class SelectClauseBehaviorTest : MysqlTestBase() {

    @Test
    fun `build carries select syntax query statement`() {
        val clause = TestUser().select().where { it.id == 1 }

        val task = clause.build()

        assertEquals(clause.toSqlQuery(), task.atomicTask.statement)
    }

    @Test
    fun `build carries generated projection column types for map mapping`() {
        val task = TestUser()
            .select { [it.id, it.username] }
            .build()
            .atomicTask

        val id = task.resultColumns["id"]
        assertEquals(typeOf<Int?>(), id?.type)
        assertEquals("id", id?.field?.name)
        assertEquals("id", id?.field?.columnName)
        assertEquals(ValueStorage.NONE, id?.storage)
        assertEquals("id", id?.columnLabel)
        assertFalse("ID" in task.resultColumns)

        val username = task.resultColumns["username"]
        assertEquals(typeOf<String?>(), username?.type)
        assertEquals("username", username?.field?.name)
        assertEquals("username", username?.field?.columnName)
        assertEquals(ValueStorage.NONE, username?.storage)
        assertEquals("username", username?.columnLabel)
        assertFalse("score" in task.resultColumns)
    }

    @Test
    fun `select clause uses projection KType as selected type`() {
        val wrapper = CapturingMysqlWrapper()
        val clause = TestUser().select<TestUser, UserRelation>(typeOf<UserRelation>()) { it.username }

        clause.toList(wrapper)

        assertEquals(typeOf<UserRelation>(), clause.selectedType)
        assertEquals(typeOf<UserRelation?>(), clause.nullableSelectedType)
        assertEquals(
            listOf<QueryCall>(QueryCall.ToList(typeOf<UserRelation>())),
            wrapper.calls
        )
    }

    @Test
    fun `select projection defaults to the reified result KType`() {
        val clause = TestUser().select<TestUser, UserRelation> { it.username }

        assertEquals(typeOf<UserRelation>(), clause.selectedType)
        assertEquals(typeOf<UserRelation?>(), clause.nullableSelectedType)
    }

    @Test
    fun `select rejects a projection KType that does not match the reified result`() {
        assertFailsWith<IllegalArgumentException> {
            TestUser().select<TestUser, UserRelation>(typeOf<TestUser>()) { it.username }
        }
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
    fun `collection projection forms expand source and append alias in order`() {
        val projections = linkedMapOf(
            "literal" to TestUser().select { [it, it.id.alias("literalId")] },
            "list" to TestUser().select { listOf<Any?>(it, it.id.alias("listId")) },
            "array" to TestUser().select { arrayOf<Any?>(it, it.id.alias("arrayId")) },
            "mutableList" to TestUser().select { mutableListOf<Any?>(it, it.id.alias("mutableId")) },
            "set" to TestUser().select { setOf<Any?>(it, it.id.alias("setId")) },
        )
        val sourceColumns = listOf("id", "username", "score", "gender", "createTime", "updateTime", "deleted")

        assertEquals(
            linkedMapOf(
                "literal" to sourceColumns + "literalId",
                "list" to sourceColumns + "listId",
                "array" to sourceColumns + "arrayId",
                "mutableList" to sourceColumns + "mutableId",
                "set" to sourceColumns + "setId",
            ),
            projections.mapValues { (_, clause) ->
                (clause.toSqlQuery() as SqlQuery.Select).projectionOutputNames()
            }
        )
        projections.values.forEach { clause ->
            assertNotEquals(typeOf<TestUser>(), clause.selectedType)
        }
    }

    @Test
    fun `identity source projections keep source type while minus projections use generated type`() {
        val allDirect = TestUser().select { it }
        val allLiteral = TestUser().select { [it] }
        val allList = TestUser().select { listOf<Any?>(it) }
        val allArray = TestUser().select { arrayOf<Any?>(it) }
        val allMutableList = TestUser().select { mutableListOf<Any?>(it) }
        val allSet = TestUser().select { setOf<Any?>(it) }
        val excludedDirect = TestUser().select { it - it.id }
        val excludedLiteral = TestUser().select { [it - it.id] }
        val sourceColumns = listOf("id", "username", "score", "gender", "createTime", "updateTime", "deleted")
        val excludedColumns = sourceColumns - "id"

        listOf(allDirect, allLiteral, allList, allArray, allMutableList, allSet).forEach { clause ->
            assertEquals(sourceColumns, (clause.toSqlQuery() as SqlQuery.Select).projectionOutputNames())
            assertEquals(typeOf<TestUser>(), clause.selectedType)
        }
        assertEquals(excludedColumns, (excludedDirect.toSqlQuery() as SqlQuery.Select).projectionOutputNames())
        assertEquals(excludedColumns, (excludedLiteral.toSqlQuery() as SqlQuery.Select).projectionOutputNames())
        listOf(excludedDirect, excludedLiteral).forEach { clause ->
            assertNotEquals(typeOf<TestUser>(), clause.selectedType)
        }
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
    fun `group order and having stay on base while page state is isolated`() {
        val clause = TestUser(7, "neo")
            .select { [it.id, it.username] }
            .where()
            .groupBy { [it.gender] }
            .having { it.gender == 1 }
            .orderBy { [it.id.desc(), it.username.asc()] }
        val page = clause.page(2, 10)

        assertEquals(setOf("id", "username"), clause.context.selectedFields.map { it.name }.toSet())
        assertEquals(listOf("gender"), clause.context.groupByItems.map { it.toString().substringAfter("columnName=").substringBefore(",") })
        assertEquals("Binary", clause.context.where!!::class.simpleName)
        assertEquals("Binary", clause.context.having!!::class.simpleName)
        assertEquals(listOf("FieldItem", "FieldItem"), clause.context.orderByItems.map { it::class.simpleName })
        assertEquals(null, clause.context.limit)
        assertEquals(SqlLimit.limit(10, 10), (page.toSqlQuery() as SqlQuery.Select).limit)
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

        assertEquals(listOf(row), TestUser().select().toMapList(wrapper))
        assertEquals(scalar, TestUser().select().toMap(wrapper))
        assertEquals(scalar, TestUser().select().toMapOrNull(wrapper))
        assertEquals(listOf(user), TestUser().select().toList<TestUser>(wrapper))
        val defaultList: List<TestUser> = TestUser().select().toList(wrapper)
        assertEquals(listOf(user), defaultList)
        assertEquals(user, TestUser().select().first<TestUser>(wrapper))
        val defaultOne: TestUser = TestUser().select().first(wrapper)
        assertEquals(user, defaultOne)
        assertEquals(user, TestUser().select().firstOrNull<TestUser>(wrapper))
        val defaultOneOrNull: TestUser? = TestUser().select().firstOrNull(wrapper)
        assertEquals(user, defaultOneOrNull)
        val selectable: KSelectable<TestUser> = TestUser().select()
        val selectableOne = selectable.first(wrapper)
        assertEquals(user.id, selectableOne.id)
        assertEquals(user, selectable.first<TestUser>(wrapper))
        val selectableOneOrNull = selectable.firstOrNull(wrapper)
        assertEquals(user.id, selectableOneOrNull?.id)
        assertEquals(user, selectable.firstOrNull<TestUser>(wrapper))

        assertEquals(
            listOf<QueryCall>(
                QueryCall.ToList(typeOf<Map<String, Any?>>()),
                QueryCall.First(typeOf<Map<String, Any?>>()),
                QueryCall.First(typeOf<Map<String, Any?>?>()),
                QueryCall.ToList(typeOf<TestUser>()),
                QueryCall.ToList(typeOf<TestUser>()),
                QueryCall.First(typeOf<TestUser>()),
                QueryCall.First(typeOf<TestUser>()),
                QueryCall.First(typeOf<TestUser?>()),
                QueryCall.First(typeOf<TestUser?>()),
                QueryCall.First(typeOf<TestUser>()),
                QueryCall.First(typeOf<TestUser>()),
                QueryCall.First(typeOf<TestUser?>()),
                QueryCall.First(typeOf<TestUser?>())
            ),
            wrapper.calls
        )
    }

    @Test
    fun `queryMapOrNull returns null without throwing`() {
        val wrapper = CapturingMysqlWrapper(mapResult = null)

        assertEquals(null, TestUser().select().toMapOrNull(wrapper))
        assertEquals(listOf<QueryCall>(QueryCall.First(typeOf<Map<String, Any?>?>())), wrapper.calls)
    }

    @Test
    fun `selectable row mapper delegates to the row capable wrapper and prepares first results`() {
        val wrapper = RowMappingMysqlWrapper()
        val query = TestUser().select { it.username }

        assertEquals(listOf(0), query.toList<Int>(wrapper) { it.rowNumber })
        assertEquals(0, query.first<Int>(wrapper) { it.rowNumber })
        assertEquals(0, query.firstOrNull<Int>(wrapper) { it.rowNumber })

        assertEquals(
            listOf(null, SqlLimit.limit(1), SqlLimit.limit(1)),
            wrapper.mapperTasks.map { (it.statement as SqlQuery.Select).limit }
        )
    }

    @Test
    fun `default first reports no record when wrapper returns null`() {
        val wrapper = CapturingMysqlWrapper(objectResult = null)

        val error = assertFailsWith<NoSuchElementException> {
            val result: TestUser = TestUser().select().first(wrapper)
            result
        }

        assertEquals(
            "No result found for query: SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `deleted` = 0 LIMIT 1",
            error.message
        )
        assertEquals(
            listOf<QueryCall>(QueryCall.First(typeOf<TestUser>())),
            wrapper.calls
        )
    }
}

private fun SqlQuery.Select.projectionOutputNames(): List<String> =
    select.mapNotNull { item ->
        val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
        expression.metadata?.outputName
            ?: expression.alias
            ?: (expression.expr as? SqlExpr.Column)?.columnName
    }

private class CapturingMysqlWrapper(
    private val mapRows: List<Map<String, Any>> = emptyList(),
    private val typedRows: List<Any> = emptyList(),
    private val mapResult: Map<String, Any>? = null,
    private val objectResult: Any? = null
) : SampleMysqlJdbcWrapper() {
    val calls = mutableListOf<QueryCall>()

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        calls += QueryCall.ToList(task.targetType)
        return if (task.targetType == typeOf<Map<String, Any?>>()) mapRows else typedRows
    }

    override fun first(task: KAtomicQueryTask): Any? {
        calls += QueryCall.First(task.targetType)
        return if (task.targetType == typeOf<Map<String, Any?>>() || task.targetType == typeOf<Map<String, Any?>?>()) {
            mapResult
        } else {
            objectResult
        }
    }
}

private class RowMappingMysqlWrapper : SampleMysqlJdbcWrapper(), KronosRowMappingDataSourceWrapper {
    val mapperTasks = mutableListOf<KAtomicQueryTask>()

    override fun <T> toList(task: KAtomicQueryTask, mapper: (KronosRow) -> T): List<T> {
        mapperTasks += task
        return listOf(mapper(StaticKronosRow))
    }

    override fun <T> first(
        task: KAtomicQueryTask,
        mapper: (KronosRow) -> T
    ): KronosRowFirstResult<T> {
        mapperTasks += task
        return KronosRowFirstResult.Present(mapper(StaticKronosRow))
    }
}

private object StaticKronosRow : KronosRow() {
    override val rowNumber: Int = 0

    override fun get(position: Int, targetType: KType): Any? =
        throw UnsupportedOperationException("This test row only exposes rowNumber")

    override fun get(label: String, targetType: KType): Any? =
        throw UnsupportedOperationException("This test row only exposes rowNumber")
}

private sealed interface QueryCall {
    data class ToList(val targetType: KType) : QueryCall
    data class First(val targetType: KType) : QueryCall
}
