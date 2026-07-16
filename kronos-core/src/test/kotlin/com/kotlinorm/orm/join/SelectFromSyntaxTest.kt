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
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlSelectItemSource
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectFromSyntaxTest : MysqlTestBase() {

    @Test
    fun testToSqlQueryGeneratesCorrectJoinTree() {
        val statement = TestUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { [user.id, user.username, relation.gender] }
        }.toSqlQuery() as SqlQuery.Select

        assertEquals(
            leftJoin(
                left = table("tb_user"),
                right = table("user_relation"),
                condition = eq(tcol("tb_user", "id"), tcol("user_relation", "id2"))
            ),
            statement.from.single()
        )
    }

    @Test
    fun testToSqlQueryWithMultipleJoins() {
        val statement = TestUser(1).join(
            UserRelation(1, "test1", 1, 1),
            UserRelation(2, "test2", 1, 1)
        ) { user, relation1, relation2 ->
            leftJoin(relation1) { user.id == relation1.id2 }
            leftJoin(relation2) { user.id == relation2.id2 }
            select { [user.id, relation1.gender, relation2.gender] }
        }.toSqlQuery() as SqlQuery.Select

        assertEquals(
            leftJoin(
                left = leftJoin(
                    left = table("tb_user"),
                    right = table("user_relation", "user_relation__k2"),
                    condition = eq(tcol("tb_user", "id"), tcol("user_relation__k2", "id2"))
                ),
                right = table("user_relation", "user_relation__k3"),
                condition = eq(tcol("tb_user", "id"), tcol("user_relation__k3", "id2"))
            ),
            statement.from.single()
        )
    }

    @Test
    fun testToSqlQueryWithWhereClause() {
        val statement = TestUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { [user.id, user.username] }
        }.toSqlQuery() as SqlQuery.Select

        assertEquals(rootIdAndNotDeleted(), statement.where)
    }

    @Test
    fun `repeated join where clauses keep colliding parameter values distinct`() {
        val task = TestUser().join(UserRelation()) { user, relation ->
            innerJoin(relation) { user.id == relation.id2 }
            select { user.id }
            where { user.id == 10 }
            where { relation.id == 20 }
        }.build().atomicTask

        assertEquals(
            "SELECT `tb_user`.`id` AS `id` FROM `tb_user` INNER JOIN `user_relation` " +
                "ON `tb_user`.`id` = `user_relation`.`id2` WHERE `tb_user`.`id` = :id AND " +
                "`user_relation`.`id` = :id@1 AND `tb_user`.`deleted` = 0",
            task.sql
        )
        assertEquals(mapOf("id" to 10, "id@1" to 20), task.paramMap)
    }

    @Test
    fun testToSqlQuerySelectListWithTableAliases() {
        val statement = TestUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { [user.id, user.username, relation.gender] }
        }.toSqlQuery() as SqlQuery.Select

        assertEquals(
            listOf(
                selectItem(tableName = "tb_user", outputName = "id", columnName = "id"),
                selectItem(tableName = "tb_user", outputName = "username", columnName = "username"),
                selectItem(tableName = "user_relation", outputName = "gender", columnName = "gender")
            ),
            statement.select
        )
    }

    @Test
    fun testToSqlQueryJoinConditionWithTableAliases() {
        val statement = TestUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id }
        }.toSqlQuery() as SqlQuery.Select

        val join = statement.from.single() as SqlTable.Join
        val on = join.condition as SqlJoinCondition.On
        val condition = on.condition as SqlExpr.Binary

        assertEquals(SqlBinaryOperator.Equal, condition.operator)
        val left = condition.left as SqlExpr.Column
        val right = condition.right as SqlExpr.Column
        assertEquals(tcol("tb_user", "id"), left)
        assertEquals(tcol("user_relation", "id2"), right)
    }

    @Test
    fun testToSqlQueryWithRightJoin() {
        assertJoinType(SqlJoinType.Right) { user, relation ->
            rightJoin(relation) { user.id == relation.id2 }
            select { user.id }
        }
    }

    @Test
    fun testToSqlQueryWithInnerJoin() {
        assertJoinType(SqlJoinType.Inner) { user, relation ->
            innerJoin(relation) { user.id == relation.id2 }
            select { user.id }
        }
    }

    @Test
    fun testToSqlQueryWithCrossJoin() {
        assertJoinType(SqlJoinType.Cross) { user, relation ->
            crossJoin(relation) { user.id == relation.id2 }
            select { user.id }
        }
    }

    @Test
    fun testToSqlQueryWithFullJoin() {
        assertJoinType(SqlJoinType.Full) { user, relation ->
            fullJoin(relation) { user.id == relation.id2 }
            select { user.id }
        }
    }

    @Test
    fun testToSqlQueryWithOrderBy() {
        val statement = TestUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id }
            orderBy { user.id.desc() }
        }.toSqlQuery() as SqlQuery.Select

        assertEquals(SqlOrdering.Desc, statement.orderBy.single().ordering)
    }

    @Test
    fun testToSqlQueryWithLimit() {
        val statement = TestUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id }
            limit(10)
        }.toSqlQuery() as SqlQuery.Select

        assertEquals(10, statement.limit!!.fetch!!.limit.numberLiteral())
    }

    @Test
    fun testToSqlQueryWithGroupBy() {
        val statement = TestUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { [user.id, relation.gender] }
            groupBy { relation.gender }
        }.toSqlQuery() as SqlQuery.Select

        assertEquals(
            SqlGroup(items = listOf(SqlGroupingItem.Expr(tcol("user_relation", "gender")))),
            statement.groupBy
        )
    }

    @Test
    fun testToSqlQueryWithDistinct() {
        val statement = TestUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id }
            distinct()
        }.toSqlQuery() as SqlQuery.Select

        assertEquals(SqlQuantifier.Distinct, statement.quantifier)
    }

    private fun assertJoinType(
        expected: SqlJoinType,
        block: SelectFrom2<TestUser, UserRelation>.(TestUser, UserRelation) -> Unit
    ) {
        val statement = TestUser(1)
            .join(UserRelation(1, "test", 1, 1), block)
            .toSqlQuery() as SqlQuery.Select
        val join = statement.from.single() as SqlTable.Join
        assertEquals(expected, join.joinType)
    }

    private fun SqlExpr.numberLiteral(): Int? = (this as? SqlExpr.NumberLiteral)?.number?.toIntOrNull()

    private fun leftJoin(left: SqlTable, right: SqlTable, condition: SqlExpr): SqlTable.Join =
        SqlTable.Join(
            left = left,
            joinType = SqlJoinType.Left,
            right = right,
            condition = SqlJoinCondition.On(condition)
        )

    private fun table(name: String, alias: String? = null): SqlTable.Ident =
        SqlTable.Ident(name = name, alias = alias?.let(::SqlTableAlias))

    private fun rootIdAndNotDeleted(): SqlExpr =
        and(
            eq(tcol("tb_user", "id"), param("id")),
            eq(tcol("tb_user", "deleted"), SqlExpr.NumberLiteral("0"))
        )

    private fun selectItem(tableName: String, outputName: String, columnName: String): SqlSelectItem.Expr {
        val expr = tcol(tableName, columnName)
        return SqlSelectItem.Expr(
            expr = expr,
            alias = outputName,
            metadata = SqlSelectItemAliasMetadata(
                outputName = outputName,
                expression = expr,
                scope = SqlSelectItemSourceScope.Selected,
                source = SqlSelectItemSource(tableName = tableName, columnName = columnName),
                userReferenceable = true
            )
        )
    }

    private fun and(left: SqlExpr, right: SqlExpr): SqlExpr =
        SqlExpr.Binary(left, SqlBinaryOperator.And, right)

    private fun eq(left: SqlExpr, right: SqlExpr): SqlExpr =
        SqlExpr.Binary(left, SqlBinaryOperator.Equal, right)

    private fun tcol(tableName: String, columnName: String): SqlExpr.Column =
        SqlExpr.Column(tableName = tableName, columnName = columnName)

    private fun param(name: String): SqlExpr =
        SqlExpr.Parameter(SqlParameter.Named(name))
}
