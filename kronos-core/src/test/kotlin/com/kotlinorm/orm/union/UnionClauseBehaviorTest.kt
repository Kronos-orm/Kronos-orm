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

package com.kotlinorm.orm.union

import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.unionAll
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class UnionClauseBehaviorTest : MysqlTestBase() {

    @Test
    fun `build carries set syntax query statement`() {
        val unionClause = union(
            TestUser().select(TestUser::class) { it.id }.where { it.id == 1 },
            TestUser().select(TestUser::class) { it.id }.where { it.id == 2 }
        )

        val task = unionClause.build()

        assertEquals(unionClause.toSqlQuery(), task.atomicTask.statement)
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
    fun `non positive union limit clears query tail limit`() {
        val task = baseUnion().limit(10).limit(0).build()

        assertEquals(
            "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0)",
            task.atomicTask.sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2), task.atomicTask.paramMap)
    }

    @Test
    fun `infix union all initializes and extends all set queries`() {
        val first = TestUser().select(TestUser::class) { it.id }.where { it.id == 1 }
        val second = TestUser().select(TestUser::class) { it.id }.where { it.id == 2 }
        val third = TestUser().select(TestUser::class) { it.id }.where { it.id == 3 }

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

        assertEquals(listOf(row), baseUnion().query(wrapper))
        assertEquals(scalar, baseUnion().queryMap(wrapper))
        assertEquals(scalar, baseUnion().queryMapOrNull(wrapper))
        assertEquals(listOf(user), baseUnion().queryList<TestUser>(wrapper, true, listOf("Marker")))
        assertEquals(user, baseUnion().queryOne<TestUser>(wrapper, true, listOf("Marker")))
        assertEquals(user, baseUnion().queryOneOrNull<TestUser>(wrapper, true, listOf("Marker")))

        assertEquals(
            listOf<UnionQueryCall>(
                UnionQueryCall.ForListMap,
                UnionQueryCall.ForMap,
                UnionQueryCall.ForMap,
                UnionQueryCall.ForListTyped(TestUser::class, true, listOf("kotlin.Any", "com.kotlinorm.interfaces.KPojo")),
                UnionQueryCall.ForObject(TestUser::class, true, listOf("kotlin.Any", "com.kotlinorm.interfaces.KPojo")),
                UnionQueryCall.ForObject(TestUser::class, true, listOf("kotlin.Any", "com.kotlinorm.interfaces.KPojo"))
            ),
            wrapper.calls
        )
    }

    private fun baseUnion() = union(
        TestUser().select(TestUser::class) { it.id }.where { it.id == 1 },
        TestUser().select(TestUser::class) { it.id }.where { it.id == 2 }
    )
}

private class CapturingUnionMysqlWrapper(
    private val mapRows: List<Map<String, Any>> = emptyList(),
    private val typedRows: List<Any> = emptyList(),
    private val mapResult: Map<String, Any>? = null,
    private val objectResult: Any? = null
) : SampleMysqlJdbcWrapper() {
    val calls = mutableListOf<UnionQueryCall>()

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
        calls += UnionQueryCall.ForListMap
        return mapRows
    }

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> {
        calls += UnionQueryCall.ForListTyped(kClass, isKPojo, superTypes)
        return typedRows
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? {
        calls += UnionQueryCall.ForMap
        return mapResult
    }

    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): Any? {
        calls += UnionQueryCall.ForObject(kClass, isKPojo, superTypes)
        return objectResult
    }
}

private sealed interface UnionQueryCall {
    object ForListMap : UnionQueryCall
    object ForMap : UnionQueryCall
    data class ForListTyped(
        val kClass: KClass<*>,
        val isKPojo: Boolean,
        val superTypes: List<String>
    ) : UnionQueryCall
    data class ForObject(
        val kClass: KClass<*>,
        val isKPojo: Boolean,
        val superTypes: List<String>
    ) : UnionQueryCall
}
