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
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
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
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.types.ToFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SelectClauseSyntaxTest : MysqlTestBase() {

    @Test
    fun testToSqlQueryGeneratesCorrectSelect() {
        val statement = TestUser().select().toSqlQuery() as SqlQuery.Select

        assertEquals(
            SqlQuery.Select(
                select = mysqlUserSelectItems(),
                from = listOf(SqlTable.Ident("tb_user")),
                where = deletedFalse()
            ),
            statement
        )
    }

    @Test
    fun testToSqlQueryWithWhereClause() {
        val statement = TestUser().select().where { it.id == 1 }.toSqlQuery() as SqlQuery.Select

        assertEquals(
            SqlQuery.Select(
                select = mysqlUserSelectItems(),
                from = listOf(SqlTable.Ident("tb_user")),
                where = and(eq(tcol("tb_user", "id"), param("id")), deletedFalse())
            ),
            statement
        )
    }

    @Test
    fun `select with source values and no where does not add query by example conditions`() {
        val statement = TestUser(id = 1, username = "A").select().toSqlQuery() as SqlQuery.Select

        assertEquals(
            SqlQuery.Select(
                select = mysqlUserSelectItems(),
                from = listOf(SqlTable.Ident("tb_user")),
                where = deletedFalse()
            ),
            statement
        )
    }

    @Test
    fun `empty where adds query by example conditions for non null source values`() {
        val statement = TestUser(id = 1, username = "A").select().where().toSqlQuery() as SqlQuery.Select

        assertEquals(
            SqlQuery.Select(
                select = mysqlUserSelectItems(),
                from = listOf(SqlTable.Ident("tb_user")),
                where = and(
                    and(
                        eq(col("id"), param("id")),
                        eq(col("username"), param("username"))
                    ),
                    deletedFalse()
                )
            ),
            statement
        )
    }

    @Test
    fun `where keeps optional predicates at the api boundary`() {
        val optionalCondition: ToFilter<TestUser, Boolean?>? = null

        val optionalStatement = TestUser(id = 1).select().where(optionalCondition).toSqlQuery()
        val omittedStatement = TestUser(id = 1).select().where().toSqlQuery()

        assertEquals(omittedStatement, optionalStatement)
    }

    @Test
    fun `empty where allows no query by example fields`() {
        val statement = UserRelation().select().where().toSqlQuery() as SqlQuery.Select

        assertEquals(
            SqlQuery.Select(
                select = listOf(
                    selectItem("id"),
                    selectItem("username"),
                    selectItem("gender"),
                    selectItem("id2")
                ),
                from = listOf(SqlTable.Ident("user_relation"))
            ),
            statement
        )
    }

    @Test
    fun `lambda where ignores source values for this where call`() {
        val statement = TestUser(id = 1).select().where { it.username == "A" }.toSqlQuery() as SqlQuery.Select

        assertEquals(
            SqlQuery.Select(
                select = mysqlUserSelectItems(),
                from = listOf(SqlTable.Ident("tb_user")),
                where = and(
                    eq(tcol("tb_user", "username"), param("username")),
                    deletedFalse()
                )
            ),
            statement
        )
    }

    @Test
    fun `multiple where calls append conditions with and`() {
        val statement = TestUser(id = 1).select()
            .where()
            .where { it.username == "A" || it.score > 18 }
            .toSqlQuery() as SqlQuery.Select

        assertEquals(
            SqlQuery.Select(
                select = mysqlUserSelectItems(),
                from = listOf(SqlTable.Ident("tb_user")),
                where = and(
                    and(
                        eq(col("id"), param("id")),
                        or(
                            eq(tcol("tb_user", "username"), param("username")),
                            gt(tcol("tb_user", "score"), param("scoreMin"))
                        )
                    ),
                    deletedFalse()
                )
            ),
            statement
        )
    }

    @Test
    fun `multiple lambda where calls keep independent parameter names`() {
        val statement = TestUser().select()
            .where { it.id == 1 }
            .where { it.id == 2 }
            .toSqlQuery() as SqlQuery.Select

        assertEquals(
            SqlQuery.Select(
                select = mysqlUserSelectItems(),
                from = listOf(SqlTable.Ident("tb_user")),
                where = and(
                    and(
                        eq(tcol("tb_user", "id"), param("id")),
                        eq(tcol("tb_user", "id"), param("id@1"))
                    ),
                    deletedFalse()
                )
            ),
            statement
        )
    }

    @Test
    fun testToSqlQueryWithSelectFields() {
        val statement = TestUser().select(fields = { [it.id, it.username] }).toSqlQuery() as SqlQuery.Select

        assertEquals(listOf(selectItem("id"), selectItem("username")), statement.select)
    }

    @Test
    fun testAliasMetadataForSourceAndAliasedFields() {
        val statement = TestUser()
            .select(fields = { [it.id, it.username.alias("name")] })
            .toSqlQuery() as SqlQuery.Select

        val id = statement.findSelectOutput("id")
        val name = statement.findSelectOutput("name")

        assertNotNull(id)
        assertEquals(SqlSelectItemSourceScope.Source, id.scope)
        assertEquals("id", id.source?.columnName)
        assertTrue(id.expression is SqlExpr.Column)

        assertNotNull(name)
        assertEquals(SqlSelectItemSourceScope.Selected, name.scope)
        assertEquals("name", name.outputName)
        assertEquals("username", name.source?.columnName)
        assertTrue(name.expression is SqlExpr.Column)
    }

    @Test
    fun testAliasMetadataForFunctionSelectItem() {
        val statement = TestUser()
            .select(fields = { f.count(it.id).alias("total") })
            .toSqlQuery() as SqlQuery.Select

        val total = statement.findSelectOutput("total")

        assertNotNull(total)
        assertEquals(SqlSelectItemSourceScope.Aggregate, total.scope)
        assertEquals("total", total.outputName)
        assertTrue(total.expression is SqlExpr.Function)
        assertTrue(total.userReferenceable)
    }

    @Test
    fun testUnaliasedExpressionHasNoUserMetadata() {
        val statement = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(expr = SqlExpr.UnsafeRaw("COUNT(1)"))),
            from = listOf(SqlTable.Ident("tb_user"))
        )
        val item = statement.select.single() as SqlSelectItem.Expr

        assertEquals(null, item.alias)
        assertEquals(null, item.metadata)
    }

    @Test
    fun testToSqlQueryWithOrderBy() {
        val statement = TestUser()
            .select()
            .orderBy { [it.id.desc(), it.username.asc()] }
            .toSqlQuery() as SqlQuery.Select

        assertEquals(2, statement.orderBy.size)
        assertEquals(SqlOrdering.Desc, statement.orderBy[0].ordering)
        assertEquals(SqlOrdering.Asc, statement.orderBy[1].ordering)
    }

    @Test
    fun testToSqlQueryWithLimit() {
        val statement = TestUser().select().limit(10).toSqlQuery() as SqlQuery.Select

        assertNotNull(statement.limit)
        assertEquals(10, statement.limit!!.fetch!!.limit.numberLiteral())
        assertEquals(null, statement.limit!!.offset)
    }

    @Test
    fun testToSqlQueryWithLimitAndOffset() {
        val statement = TestUser().select().page(2, 10).build().atomicTask.statement as SqlQuery.Select

        assertNotNull(statement.limit)
        assertEquals(10, statement.limit!!.fetch!!.limit.numberLiteral())
        assertEquals(10, statement.limit!!.offset!!.numberLiteral())
    }

    @Test
    fun testToSqlQueryWithDistinct() {
        val statement = TestUser().select().distinct().toSqlQuery() as SqlQuery.Select

        assertEquals(SqlQuantifier.Distinct, statement.quantifier)
    }

    @Test
    fun testToSqlQueryWithGroupBy() {
        val statement = TestUser()
            .select(fields = { [it.gender] })
            .groupBy { [it.gender] }
            .toSqlQuery() as SqlQuery.Select

        assertEquals(
            SqlGroup(items = listOf(SqlGroupingItem.Expr(col("gender")))),
            statement.groupBy
        )
    }

    @Test
    fun testToSqlQueryWithHaving() {
        val statement = TestUser()
            .select(fields = { [it.gender] })
            .groupBy { [it.gender] }
            .having { it.gender == 1 }
            .toSqlQuery() as SqlQuery.Select

        assertEquals(eq(tcol("tb_user", "gender"), param("gender")), statement.having)
    }

    private fun SqlQuery.Select.findSelectOutput(outputName: String) =
        select.asSequence()
            .mapNotNull { (it as? SqlSelectItem.Expr)?.metadata }
            .firstOrNull { it.outputName == outputName }

    private fun SqlExpr.numberLiteral(): Int? = (this as? SqlExpr.NumberLiteral)?.number?.toIntOrNull()

    private fun mysqlUserSelectItems(): List<SqlSelectItem> = listOf(
        selectItem("id"),
        selectItem("username"),
        selectItem("score"),
        selectItem("gender"),
        selectItem(outputName = "createTime", columnName = "create_time", alias = "createTime"),
        selectItem(outputName = "updateTime", columnName = "update_time", alias = "updateTime"),
        selectItem("deleted")
    )

    private fun selectItem(
        outputName: String,
        columnName: String = outputName,
        alias: String? = null
    ): SqlSelectItem.Expr {
        val expr = col(columnName)
        return SqlSelectItem.Expr(
            expr = expr,
            alias = alias,
            metadata = SqlSelectItemAliasMetadata(
                outputName = outputName,
                expression = expr,
                scope = if (alias == null) SqlSelectItemSourceScope.Source else SqlSelectItemSourceScope.Selected,
                source = SqlSelectItemSource(columnName = columnName),
                userReferenceable = true
            )
        )
    }

    private fun deletedFalse(): SqlExpr =
        eq(col("deleted"), SqlExpr.NumberLiteral("0"))

    private fun and(left: SqlExpr, right: SqlExpr): SqlExpr =
        SqlExpr.Binary(left, SqlBinaryOperator.And, right)

    private fun or(left: SqlExpr, right: SqlExpr): SqlExpr =
        SqlExpr.Binary(left, SqlBinaryOperator.Or, right)

    private fun eq(left: SqlExpr, right: SqlExpr): SqlExpr =
        SqlExpr.Binary(left, SqlBinaryOperator.Equal, right)

    private fun gt(left: SqlExpr, right: SqlExpr): SqlExpr =
        SqlExpr.Binary(left, SqlBinaryOperator.GreaterThan, right)

    private fun col(columnName: String): SqlExpr =
        SqlExpr.Column(columnName = columnName)

    private fun tcol(tableName: String, columnName: String): SqlExpr =
        SqlExpr.Column(tableName = tableName, columnName = columnName)

    private fun param(name: String): SqlExpr =
        SqlExpr.Parameter(SqlParameter.Named(name))
}
