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

package com.kotlinorm.ast

import com.kotlinorm.database.mssql.MssqlSqlRenderer
import com.kotlinorm.database.mysql.MysqlSqlRenderer
import com.kotlinorm.database.oracle.OracleSqlRenderer
import com.kotlinorm.database.postgres.PostgresqlSqlRenderer
import com.kotlinorm.database.sqlite.SqliteSqlRenderer
import org.junit.Test
import kotlin.test.assertTrue

/**
 * SqlRendererSmokeTest
 *
 * Basic smoke tests to verify all SQL renderers are properly configured
 * and can render simple statements.
 *
 * @author OUSC
 */
class SqlRendererSmokeTest {

    @Test
    fun `MySQL renderer uses backtick quotes`() {
        val renderer = MysqlSqlRenderer()
        val statement = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(database = null, tableAlias = null, columnName = "id"),
                    null
                )
            ),
            from = TableName(table = "users")
        )
        
        val result = renderer.render(statement)
        
        assertTrue(result.sql.contains("`users`"), "MySQL should use backticks for table names")
        assertTrue(result.sql.contains("`id`"), "MySQL should use backticks for column names")
    }

    @Test
    fun `PostgreSQL renderer uses double quotes`() {
        val renderer = PostgresqlSqlRenderer()
        val statement = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(database = null, tableAlias = null, columnName = "id"),
                    null
                )
            ),
            from = TableName(table = "users")
        )
        
        val result = renderer.render(statement)
        
        assertTrue(result.sql.contains("\"users\""), "PostgreSQL should use double quotes for table names")
        assertTrue(result.sql.contains("\"id\""), "PostgreSQL should use double quotes for column names")
    }

    @Test
    fun `Oracle renderer uses double quotes`() {
        val renderer = OracleSqlRenderer()
        val statement = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(database = null, tableAlias = null, columnName = "id"),
                    null
                )
            ),
            from = TableName(table = "users")
        )
        
        val result = renderer.render(statement)
        
        assertTrue(result.sql.contains("\"users\""), "Oracle should use double quotes for table names")
        assertTrue(result.sql.contains("\"id\""), "Oracle should use double quotes for column names")
    }

    @Test
    fun `SQL Server renderer uses square brackets`() {
        val renderer = MssqlSqlRenderer()
        val statement = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(database = null, tableAlias = null, columnName = "id"),
                    null
                )
            ),
            from = TableName(table = "users")
        )
        
        val result = renderer.render(statement)
        
        assertTrue(result.sql.contains("[users]") || result.sql.contains("[dbo].[users]"), 
            "SQL Server should use square brackets for table names")
        assertTrue(result.sql.contains("[id]"), "SQL Server should use square brackets for column names")
    }

    @Test
    fun `SQLite renderer uses double quotes`() {
        val renderer = SqliteSqlRenderer()
        val statement = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(
                    ColumnReference(database = null, tableAlias = null, columnName = "id"),
                    null
                )
            ),
            from = TableName(table = "users")
        )
        
        val result = renderer.render(statement)
        
        assertTrue(result.sql.contains("\"users\""), "SQLite should use double quotes for table names")
        assertTrue(result.sql.contains("\"id\""), "SQLite should use double quotes for column names")
    }

    @Test
    fun `All renderers can render basic SELECT statement`() {
        val renderers = listOf(
            MysqlSqlRenderer(),
            PostgresqlSqlRenderer(),
            OracleSqlRenderer(),
            MssqlSqlRenderer(),
            SqliteSqlRenderer()
        )
        
        val statement = SelectStatement(
            selectList = mutableListOf(
                SelectItem.AllColumnsSelectItem(null)
            ),
            from = TableName(table = "users")
        )
        
        renderers.forEach { renderer ->
            val result = renderer.render(statement)
            assertTrue(result.sql.contains("SELECT"), "Should contain SELECT keyword")
            assertTrue(result.sql.contains("*"), "Should contain * for all columns")
            assertTrue(result.sql.contains("FROM"), "Should contain FROM keyword")
            assertTrue(result.sql.isNotEmpty(), "SQL should not be empty")
        }
    }

    @Test
    fun `MySQL LIMIT syntax`() {
        val renderer = MysqlSqlRenderer()
        val statement = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "users"),
            limit = LimitClause(10, 20)
        )
        
        val result = renderer.render(statement)
        
        assertTrue(result.sql.contains("LIMIT 10 OFFSET 20"), 
            "MySQL should use LIMIT count OFFSET offset syntax (SQL standard)")
    }

    @Test
    fun `PostgreSQL LIMIT OFFSET syntax`() {
        val renderer = PostgresqlSqlRenderer()
        val statement = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "users"),
            limit = LimitClause(10, 20)
        )
        
        val result = renderer.render(statement)
        
        assertTrue(result.sql.contains("LIMIT 10 OFFSET 20"), 
            "PostgreSQL should use LIMIT count OFFSET offset syntax")
    }

    @Test
    fun `Oracle FETCH FIRST syntax`() {
        val renderer = OracleSqlRenderer()
        val statement = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "users"),
            limit = LimitClause(10, 20)
        )
        
        val result = renderer.render(statement)
        
        assertTrue(result.sql.contains("OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY"), 
            "Oracle should use OFFSET/FETCH NEXT syntax")
    }

    @Test
    fun `SQL Server FETCH NEXT syntax`() {
        val renderer = MssqlSqlRenderer()
        val statement = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "users"),
            limit = LimitClause(10, 20)
        )
        
        val result = renderer.render(statement)
        
        assertTrue(result.sql.contains("OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY"), 
            "SQL Server should use OFFSET/FETCH NEXT syntax")
    }

    @Test
    fun `SQLite LIMIT OFFSET syntax`() {
        val renderer = SqliteSqlRenderer()
        val statement = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "users"),
            limit = LimitClause(10, 20)
        )
        
        val result = renderer.render(statement)
        
        assertTrue(result.sql.contains("LIMIT 10 OFFSET 20"), 
            "SQLite should use LIMIT count OFFSET offset syntax")
    }
}
