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

package com.kotlinorm.orm.union

import com.kotlinorm.beans.parser.NoneDataSourceWrapper
import com.kotlinorm.exceptions.InvalidDataAccessApiUsageException
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.unionAll
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import com.kotlinorm.wrappers.SampleSqlServerJdbcWrapper
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class UnionClauseBehaviorTest : MysqlTestBase() {

    @Test
    fun `build carries set syntax query statement`() {
        val unionClause = union(
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 }
        )

        val task = unionClause.build()

        assertEquals(unionClause.toSqlQuery(), task.atomicTask.statement)
        assertEquals(typeOf<Int?>(), task.atomicTask.resultColumns["id"]?.type)
    }

    @Test
    fun `order limit and all mutate union query tail`() {
        val task = baseUnion()
            .all()
            .orderBy("id" to SqlOrdering.Desc, "username" to SqlOrdering.Asc)
            .limit(5, 10)
            .build()

        assertEquals(
            "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION ALL (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0) ORDER BY `id` DESC, `username` ASC LIMIT 5 OFFSET 10",
            task.atomicTask.sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2), task.atomicTask.paramMap)
    }

    @Test
    fun `zero union limit remains an explicit empty result limit`() {
        val task = baseUnion().limit(10).limit(0).build()

        assertEquals(
            "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0) LIMIT 0",
            task.atomicTask.sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2), task.atomicTask.paramMap)
    }

    @Test
    fun `union planning without a datasource keeps the dialect neutral limit`() {
        val statement = union(
            UserRelation().select<UserRelation, UserRelation> { it.id },
            UserRelation().select<UserRelation, UserRelation> { it.id }
        ).limit(5, 10).toSqlQuery(NoneDataSourceWrapper)

        val set = assertIs<SqlQuery.Set>(statement)
        assertEquals(5, set.limit?.fetch?.limit?.numberLiteral())
        assertEquals(10, set.limit?.offset?.numberLiteral())
    }

    @Test
    fun `sql server union limit without ordering remains rejected`() {
        val error = assertFailsWith<InvalidDataAccessApiUsageException> {
            baseUnion().limit(5).toSqlQuery(SampleSqlServerJdbcWrapper)
        }

        assertEquals(
            "SQL Server union limit() requires orderBy() because OFFSET/FETCH cannot be rendered without ORDER BY.",
            error.message
        )
    }

    @Test
    fun `infix union all initializes and extends all set queries`() {
        val first = TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 }
        val second = TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 }
        val third = TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 3 }

        val task = (first unionAll second unionAll third).build()

        assertEquals(
            "((SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION ALL (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0)) UNION ALL (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@2 AND `deleted` = 0)",
            task.atomicTask.sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2, "id@2" to 3), task.atomicTask.paramMap)
    }

    @Test
    fun `terminal union query methods route to wrapper with expected contracts`() {
        val user = TestUser(2, "typed")
        val row = mapOf<String, Any>("id" to 1)
        val scalar = mapOf<String, Any>("total" to 1)
        val wrapper = CapturingUnionMysqlWrapper(
            mapRows = listOf(row),
            typedRows = listOf(user),
            mapResult = scalar,
            objectResult = user
        )

        assertEquals(listOf(row), baseUnion().toMapList(wrapper))
        assertEquals(scalar, baseUnion().toMap(wrapper))
        assertEquals(scalar, baseUnion().toMapOrNull(wrapper))
        assertEquals(listOf(user), baseUnion().toList<TestUser>(wrapper))
        assertEquals(user, baseUnion().first<TestUser>(wrapper))
        val defaultOne = baseUnion().first(wrapper)
        assertEquals(user.id, defaultOne.id)
        assertEquals(user, baseUnion().firstOrNull<TestUser>(wrapper))
        val defaultOneOrNull = baseUnion().firstOrNull(wrapper)
        assertEquals(user.id, defaultOneOrNull?.id)

        assertEquals(
            listOf<UnionQueryCall>(
                UnionQueryCall.ToList(typeOf<Map<String, Any?>>()),
                UnionQueryCall.First(typeOf<Map<String, Any?>>()),
                UnionQueryCall.First(typeOf<Map<String, Any?>?>()),
                UnionQueryCall.ToList(typeOf<TestUser>()),
                UnionQueryCall.First(typeOf<TestUser>()),
                UnionQueryCall.First(typeOf<TestUser>()),
                UnionQueryCall.First(typeOf<TestUser?>()),
                UnionQueryCall.First(typeOf<TestUser?>())
            ),
            wrapper.calls
        )
    }

    private fun baseUnion() = union(
        TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
        TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 }
    )

    private fun SqlExpr.numberLiteral(): Int? = (this as? SqlExpr.NumberLiteral)?.number?.toIntOrNull()
}

private class CapturingUnionMysqlWrapper(
    private val mapRows: List<Map<String, Any>> = emptyList(),
    private val typedRows: List<Any?> = emptyList(),
    private val mapResult: Map<String, Any>? = null,
    private val objectResult: Any? = null
) : SampleMysqlJdbcWrapper() {
    val calls = mutableListOf<UnionQueryCall>()

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        calls += UnionQueryCall.ToList(task.targetType)
        return if (task.targetType == typeOf<Map<String, Any?>>()) mapRows else typedRows
    }

    override fun first(task: KAtomicQueryTask): Any? {
        calls += UnionQueryCall.First(task.targetType)
        return if (task.targetType == typeOf<Map<String, Any?>>() ||
            task.targetType == typeOf<Map<String, Any?>?>()
        ) {
            mapResult
        } else {
            objectResult
        }
    }
}

private sealed interface UnionQueryCall {
    data class ToList(val targetType: KType) : UnionQueryCall
    data class First(val targetType: KType) : UnionQueryCall
}
