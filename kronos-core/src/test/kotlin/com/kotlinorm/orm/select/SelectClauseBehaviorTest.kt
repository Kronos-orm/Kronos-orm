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

        assertEquals(typeOf<Int?>(), task.resultColumnTypes["id"])
        assertEquals(typeOf<Int?>(), task.resultColumnTypes["ID"])
        assertEquals(typeOf<String?>(), task.resultColumnTypes["username"])
        assertFalse("score" in task.resultColumnTypes)
    }

    @Test
    fun `select clause uses projection class as selected type`() {
        val wrapper = CapturingMysqlWrapper()
        val clause = TestUser().select(UserRelation::class) { it.username }

        clause.toList(wrapper)

        assertEquals(typeOf<UserRelation>(), clause.selectedType)
        assertEquals(
            listOf<QueryCall>(QueryCall.ToList(typeOf<UserRelation>())),
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
    fun `whole source and minus projections keep generated type aligned with selected columns`() {
        val allDirect = TestUser().select { it }
        val allLiteral = TestUser().select { [it] }
        val excludedDirect = TestUser().select { it - it.id }
        val excludedLiteral = TestUser().select { [it - it.id] }
        val sourceColumns = listOf("id", "username", "score", "gender", "createTime", "updateTime", "deleted")
        val excludedColumns = sourceColumns - "id"

        assertEquals(sourceColumns, (allDirect.toSqlQuery() as SqlQuery.Select).projectionOutputNames())
        assertEquals(sourceColumns, (allLiteral.toSqlQuery() as SqlQuery.Select).projectionOutputNames())
        assertEquals(excludedColumns, (excludedDirect.toSqlQuery() as SqlQuery.Select).projectionOutputNames())
        assertEquals(excludedColumns, (excludedLiteral.toSqlQuery() as SqlQuery.Select).projectionOutputNames())
        listOf(allDirect, allLiteral, excludedDirect, excludedLiteral).forEach { clause ->
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
    fun `group order having page and limit callbacks mutate complete select context`() {
        val clause = TestUser(7, "neo")
            .select { [it.id, it.username] }
            .where()
            .groupBy { [it.gender] }
            .having { it.gender == 1 }
            .orderBy { [it.id.desc(), it.username.asc()] }
        clause.withTotal().page(2, 10)

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

private sealed interface QueryCall {
    data class ToList(val targetType: KType) : QueryCall
    data class First(val targetType: KType) : QueryCall
}
