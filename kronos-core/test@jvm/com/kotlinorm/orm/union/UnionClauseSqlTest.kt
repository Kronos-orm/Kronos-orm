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
import com.kotlinorm.orm.select.select
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class UnionClauseSqlTest : MysqlTestBase() {

    @Test
    fun testParameterNamingWithTwoQueries() {
        val unionClause = union(
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 }
        )

        val task = unionClause.build()

        assertEquals(
            "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0)",
            task.atomicTask.sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2), task.atomicTask.paramMap)
    }

    @Test
    fun testParameterNamingWithThreeQueries() {
        val unionClause = union(
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 3 }
        )

        val task = unionClause.build()

        assertEquals(
            "((SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0)) UNION (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@2 AND `deleted` = 0)",
            task.atomicTask.sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2, "id@2" to 3), task.atomicTask.paramMap)
    }

    @Test
    fun testUnionKeywordInSql() {
        val unionClause = union(
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 }
        )

        val task = unionClause.build()

        assertEquals(
            "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0)",
            task.atomicTask.sql
        )
    }

    @Test
    fun testUnionAllKeywordInSql() {
        val unionClause = union(
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 }
        ).all()

        val task = unionClause.build()

        assertEquals(
            "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION ALL (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0)",
            task.atomicTask.sql
        )
    }

    @Test
    fun testInfixUnionParameterNaming() {
        val unionClause = TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 }
            .union(TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 })

        val task = unionClause.build()

        assertEquals(
            "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0)",
            task.atomicTask.sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2), task.atomicTask.paramMap)
    }

    @Test
    fun testInfixUnionAllParameterNaming() {
        val unionClause = TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 }
            .unionAll(TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 })

        val task = unionClause.build()

        assertEquals(
            "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION ALL (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0)",
            task.atomicTask.sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2), task.atomicTask.paramMap)
    }

    @Test
    fun testUnionWithLimit() {
        val unionClause = union(
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 }
        ).limit(10)

        val task = unionClause.build()

        assertEquals(
            "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0) LIMIT 10",
            task.atomicTask.sql
        )
    }

    @Test
    fun testUnionWithLimitAndOffset() {
        val unionClause = union(
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 }
        ).limit(10, 5)

        val task = unionClause.build()

        assertEquals(
            "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0) LIMIT 10 OFFSET 5",
            task.atomicTask.sql
        )
    }

    @Test
    fun testUnionWithMultipleParametersPerQuery() {
        val unionClause = union(
            TestUser().select<TestUser, TestUser> { [it.id, it.username] }.where {
                it.id == 1 && it.username == "user1"
            },
            TestUser().select<TestUser, TestUser> { [it.id, it.username] }.where {
                it.id == 2 && it.username == "user2"
            }
        )

        val task = unionClause.build()

        assertEquals(
            "(SELECT `id`, `username` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `tb_user`.`username` = :username AND `deleted` = 0) UNION (SELECT `id`, `username` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `tb_user`.`username` = :username@1 AND `deleted` = 0)",
            task.atomicTask.sql
        )
        assertEquals(
            mapOf("id" to 1, "username" to "user1", "id@1" to 2, "username@1" to "user2"),
            task.atomicTask.paramMap
        )
    }

    @Test
    fun testSqlParentheses() {
        val unionClause = union(
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 }
        )

        val task = unionClause.build()

        assertEquals(
            "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0)",
            task.atomicTask.sql
        )
    }
}
