/**
 * Copyright 2022-2025 kronos-orm
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

package com.kotlinorm.ast

import com.kotlinorm.database.mssql.MssqlSqlRenderer
import com.kotlinorm.database.mysql.MysqlSqlRenderer
import com.kotlinorm.database.oracle.OracleSqlRenderer
import com.kotlinorm.database.postgres.PostgresqlSqlRenderer
import com.kotlinorm.database.sqlite.SqliteSqlRenderer
import com.kotlinorm.enums.SortType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * UnionStatementTest
 *
 * Tests for UnionStatement AST node construction and rendering across all database types.
 *
 * @author OUSC
 */
class UnionStatementTest {

    @Test
    fun `Simple UNION with two SELECT statements`() {
        val select1 = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(null, null, "id"),
                    "id"
                ),
                SelectItem.ColumnSelectItem(
                    ColumnReference(null, null, "name"),
                    "name"
                )
            ),
            from = TableName(table = "users"),
            where = BinaryExpression(
                left = ColumnReference(null, null, "id"),
                operator = SqlOperator.EQUAL,
                right = Parameter.NamedParameter("id1")
            )
        )

        val select2 = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(null, null, "id"),
                    "id"
                ),
                SelectItem.ColumnSelectItem(
                    ColumnReference(null, null, "name"),
                    "name"
                )
            ),
            from = TableName(table = "users"),
            where = BinaryExpression(
                left = ColumnReference(null, null, "id"),
                operator = SqlOperator.EQUAL,
                right = Parameter.NamedParameter("id2")
            )
        )

        val unionStatement = UnionStatement(
            queries = listOf(select1, select2),
            unionAll = false
        )

        val renderer = MysqlSqlRenderer()
        val result = renderer.render(unionStatement)

        assertTrue(result.sql.contains("UNION"), "Should contain UNION keyword")
        assertTrue(result.sql.contains("SELECT"), "Should contain SELECT keyword")
        assertTrue(!result.sql.contains("UNION ALL"), "Should not contain UNION ALL")
    }

    @Test
    fun `UNION ALL with two SELECT statements`() {
        val select1 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "users")
        )

        val select2 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "customers")
        )

        val unionStatement = UnionStatement(
            queries = listOf(select1, select2),
            unionAll = true
        )

        val renderer = MysqlSqlRenderer()
        val result = renderer.render(unionStatement)

        assertTrue(result.sql.contains("UNION ALL"), "Should contain UNION ALL keyword")
    }

    @Test
    fun `UNION with three SELECT statements`() {
        val select1 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "users")
        )

        val select2 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "customers")
        )

        val select3 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "employees")
        )

        val unionStatement = UnionStatement(
            queries = listOf(select1, select2, select3),
            unionAll = false
        )

        val renderer = MysqlSqlRenderer()
        val result = renderer.render(unionStatement)

        val unionCount = result.sql.split("UNION").size - 1
        assertEquals(2, unionCount, "Should have 2 UNION keywords for 3 queries")
    }

    @Test
    fun `UNION with ORDER BY`() {
        val select1 = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(null, null, "id"),
                    "id"
                )
            ),
            from = TableName(table = "users")
        )

        val select2 = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(null, null, "id"),
                    "id"
                )
            ),
            from = TableName(table = "customers")
        )

        val unionStatement = UnionStatement(
            queries = listOf(select1, select2),
            unionAll = false,
            orderBy = listOf(
                OrderByItem(
                    expression = ColumnReference(null, null, "id"),
                    direction = SortType.ASC
                )
            )
        )

        val renderer = MysqlSqlRenderer()
        val result = renderer.render(unionStatement)

        assertTrue(result.sql.contains("ORDER BY"), "Should contain ORDER BY clause")
        assertTrue(result.sql.contains("ASC"), "Should contain ASC direction")
    }

    @Test
    fun `UNION with LIMIT`() {
        val select1 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "users")
        )

        val select2 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "customers")
        )

        val unionStatement = UnionStatement(
            queries = listOf(select1, select2),
            unionAll = false,
            limit = LimitClause(limit = 10, offset = null)
        )

        val renderer = MysqlSqlRenderer()
        val result = renderer.render(unionStatement)

        assertTrue(result.sql.contains("LIMIT"), "Should contain LIMIT clause")
    }

    @Test
    fun `UNION with ORDER BY and LIMIT`() {
        val select1 = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(null, null, "id"),
                    "id"
                )
            ),
            from = TableName(table = "users")
        )

        val select2 = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(null, null, "id"),
                    "id"
                )
            ),
            from = TableName(table = "customers")
        )

        val unionStatement = UnionStatement(
            queries = listOf(select1, select2),
            unionAll = false,
            orderBy = listOf(
                OrderByItem(
                    expression = ColumnReference(null, null, "id"),
                    direction = SortType.DESC
                )
            ),
            limit = LimitClause(limit = 20, offset = 10)
        )

        val renderer = MysqlSqlRenderer()
        val result = renderer.render(unionStatement)

        assertTrue(result.sql.contains("ORDER BY"), "Should contain ORDER BY clause")
        assertTrue(result.sql.contains("LIMIT"), "Should contain LIMIT clause")
        assertTrue(result.sql.indexOf("ORDER BY") < result.sql.indexOf("LIMIT"), 
            "ORDER BY should come before LIMIT")
    }

    @Test
    fun `All renderers can render UNION statement`() {
        val renderers = listOf(
            MysqlSqlRenderer(),
            PostgresqlSqlRenderer(),
            OracleSqlRenderer(),
            MssqlSqlRenderer(),
            SqliteSqlRenderer()
        )

        val select1 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "users")
        )

        val select2 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "customers")
        )

        val unionStatement = UnionStatement(
            queries = listOf(select1, select2),
            unionAll = false
        )

        renderers.forEach { renderer ->
            val result = renderer.render(unionStatement)
            assertTrue(result.sql.contains("UNION"), "Should contain UNION keyword")
            assertTrue(result.sql.contains("SELECT"), "Should contain SELECT keyword")
            assertTrue(result.sql.isNotEmpty(), "SQL should not be empty")
        }
    }

    @Test
    fun `MySQL UNION with LIMIT uses correct syntax`() {
        val select1 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "users")
        )

        val select2 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "customers")
        )

        val unionStatement = UnionStatement(
            queries = listOf(select1, select2),
            unionAll = false,
            limit = LimitClause(limit = 10, offset = 5)
        )

        val renderer = MysqlSqlRenderer()
        val result = renderer.render(unionStatement)

        assertTrue(result.sql.contains("LIMIT"), "Should contain LIMIT")
    }

    @Test
    fun `PostgreSQL UNION with LIMIT uses correct syntax`() {
        val select1 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "users")
        )

        val select2 = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "customers")
        )

        val unionStatement = UnionStatement(
            queries = listOf(select1, select2),
            unionAll = false,
            limit = LimitClause(limit = 10, offset = 5)
        )

        val renderer = PostgresqlSqlRenderer()
        val result = renderer.render(unionStatement)

        assertTrue(result.sql.contains("LIMIT 10 OFFSET 5"), 
            "PostgreSQL should use LIMIT count OFFSET offset syntax")
    }

    @Test
    fun `UNION with JOIN in one of the queries`() {
        val select1 = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(null, "u", "id"),
                    "id"
                )
            ),
            from = TableName(table = "users", alias = "u")
        )

        val select2 = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(null, "u", "id"),
                    "id"
                )
            ),
            from = JoinTable(
                left = TableName(table = "users", alias = "u"),
                joinType = com.kotlinorm.enums.JoinType.LEFT_JOIN,
                right = TableName(table = "orders", alias = "o"),
                condition = BinaryExpression(
                    left = ColumnReference(null, "u", "id"),
                    operator = SqlOperator.EQUAL,
                    right = ColumnReference(null, "o", "user_id")
                )
            )
        )

        val unionStatement = UnionStatement(
            queries = listOf(select1, select2),
            unionAll = false
        )

        val renderer = MysqlSqlRenderer()
        val result = renderer.render(unionStatement)

        assertTrue(result.sql.contains("UNION"), "Should contain UNION")
        assertTrue(result.sql.contains("LEFT JOIN"), "Should contain LEFT JOIN")
    }
}
