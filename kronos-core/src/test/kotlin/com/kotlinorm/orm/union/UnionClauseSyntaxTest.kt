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
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSetOperator
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UnionClauseSyntaxTest : MysqlTestBase() {

    @Test
    fun testToSqlQueryBuildsUnionSetStatement() {
        val statement = union(
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 }
        ).toSqlQuery()

        val set = assertIs<SqlQuery.Set>(statement)
        assertIs<SqlQuery.Select>(set.left)
        assertIs<SqlQuery.Select>(set.right)
        assertEquals(SqlSetOperator.Union(), set.operator)
        assertEquals(emptyList(), set.orderBy)
        assertEquals(null, set.limit)
    }

    @Test
    fun testAllUsesAllQuantifierInSetOperator() {
        val statement = union(
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 }
        ).all().toSqlQuery()

        val set = assertIs<SqlQuery.Set>(statement)
        assertEquals(SqlSetOperator.Union(SqlQuantifier.All), set.operator)
    }

    @Test
    fun testMultipleQueriesNestAsLeftAssociativeSetStatement() {
        val statement = union(
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 3 }
        ).toSqlQuery()

        val outer = assertIs<SqlQuery.Set>(statement)
        val inner = assertIs<SqlQuery.Set>(outer.left)
        assertIs<SqlQuery.Select>(inner.left)
        assertIs<SqlQuery.Select>(inner.right)
        assertIs<SqlQuery.Select>(outer.right)
        assertEquals(SqlSetOperator.Union(), inner.operator)
        assertEquals(SqlSetOperator.Union(), outer.operator)
    }

    @Test
    fun testLimitIsStoredOnSetStatement() {
        val statement = union(
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 1 },
            TestUser().select<TestUser, TestUser> { it.id }.where { it.id == 2 }
        ).limit(10, 5).toSqlQuery()

        val set = assertIs<SqlQuery.Set>(statement)
        assertEquals(10, set.limit!!.fetch!!.limit.numberLiteral())
        assertEquals(5, set.limit!!.offset!!.numberLiteral())
    }

    private fun SqlExpr.numberLiteral(): Int? = (this as? SqlExpr.NumberLiteral)?.number?.toIntOrNull()
}
