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

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

@Table(name = "null_condition_user")
data class NullConditionUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

class SelectLiteralNullConditionBehaviorTest : MysqlTestBase() {
    @Test
    fun `literal null comparisons lower to null predicates`() {
        val isNull = NullConditionUser()
            .select()
            .where { it.name == null }
            .toSqlQuery() as SqlQuery.Select

        val isNotNull = NullConditionUser()
            .select()
            .where { it.name != null }
            .toSqlQuery() as SqlQuery.Select

        val reversedIsNull = NullConditionUser()
            .select()
            .where { null == it.name }
            .toSqlQuery() as SqlQuery.Select

        val reversedIsNotNull = NullConditionUser()
            .select()
            .where { null != it.name }
            .toSqlQuery() as SqlQuery.Select

        assertNullPredicate(isNull.where, withNot = false)
        assertNullPredicate(isNotNull.where, withNot = true)
        assertNullPredicate(reversedIsNull.where, withNot = false)
        assertNullPredicate(reversedIsNotNull.where, withNot = true)
    }

    @Test
    fun `dynamic null comparison still follows no value strategy`() {
        val name: String? = null

        val statement = NullConditionUser()
            .select()
            .where { it.name == name }
            .toSqlQuery() as SqlQuery.Select

        assertEquals(null, statement.where)
    }

    private fun assertNullPredicate(where: SqlExpr?, withNot: Boolean) {
        val binary = where as? SqlExpr.Binary
        val left = binary?.left as? SqlExpr.Column

        assertEquals("name", left?.columnName)
        assertEquals(SqlBinaryOperator.Is(withNot = withNot), binary?.operator)
        assertEquals(SqlExpr.NullLiteral, binary?.right)
    }
}
