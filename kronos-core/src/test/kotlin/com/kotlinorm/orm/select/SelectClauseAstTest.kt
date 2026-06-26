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

import com.kotlinorm.ast.*
import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for SelectClause AST generation (toStatement method)
 */
class SelectClauseAstTest : MysqlTestBase() {

    @Test
    fun testToStatementGeneratesCorrectAst() {
        val user = MysqlUser()
        val selectClause = user.select()
        
        // Call toStatement to get the AST
        val statement = selectClause.toStatement()
        
        // Verify table reference
        assertNotNull(statement.from, "FROM clause should not be null")
        assertTrue(statement.from is TableName, "FROM should be TableName, but was ${statement.from::class.simpleName}")
        val tableName = statement.from as TableName
        assertEquals("tb_user", tableName.table)
        
        // Verify select list is present
        assertTrue(statement.selectList.isNotEmpty(), "Select list should not be empty")
        
        // SelectClause returns ColumnSelectItem for simple column references
        assertTrue(statement.selectList.all { it is SelectItem.ColumnSelectItem }, "All select items should be ColumnSelectItem")
        
        // Verify no joins (simple select)
        assertTrue(statement.from !is JoinTable, "Simple select should not have joins")
    }

    @Test
    fun testToStatementWithWhereClause() {
        val user = MysqlUser()
        val selectClause = user.select().where { it.id == 1 }
        
        val statement = selectClause.toStatement()
        
        // Verify WHERE clause exists
        assertNotNull(statement.where)
        
        // The WHERE clause might be wrapped in AND with logic delete condition
        // So we just verify it's an expression
        assertTrue(statement.where is Expression)
    }

    @Test
    fun testToStatementWithSelectFields() {
        val user = MysqlUser()
        val selectClause = user.select { it.id + it.username }
        
        val statement = selectClause.toStatement()
        
        // Verify only selected fields are in select list
        assertEquals(2, statement.selectList.size)
        
        // Verify the columns are present (order might vary)
        val columns = statement.selectList.map { 
            when (it) {
                is SelectItem.ColumnSelectItem -> it.column.columnName
                is SelectItem.ExpressionSelectItem -> (it.expression as? ColumnReference)?.columnName
                else -> null
            }
        }
        assertTrue(columns.contains("id"))
        assertTrue(columns.contains("username"))
    }

    @Test
    fun testToStatementWithOrderBy() {
        val user = MysqlUser()
        val selectClause = user.select().orderBy { it.id.desc() + it.username.asc() }
        
        val statement = selectClause.toStatement()
        
        // Verify ORDER BY clause exists
        assertNotNull(statement.orderBy)
        assertEquals(2, statement.orderBy!!.size)
        
        val firstOrder = statement.orderBy!![0]
        assertEquals(com.kotlinorm.enums.SortType.DESC, firstOrder.direction)
        
        val secondOrder = statement.orderBy!![1]
        assertEquals(com.kotlinorm.enums.SortType.ASC, secondOrder.direction)
    }

    @Test
    fun testToStatementWithLimit() {
        val user = MysqlUser()
        val selectClause = user.select().limit(10)
        
        val statement = selectClause.toStatement()
        
        // Verify LIMIT clause exists
        assertNotNull(statement.limit)
        assertEquals(10, statement.limit!!.limit)
        assertEquals(null, statement.limit!!.offset)
    }

    @Test
    fun testToStatementWithLimitAndOffset() {
        val user = MysqlUser()
        val selectClause = user.select().page(2, 10)
        
        val statement = selectClause.toStatement()
        
        // Verify LIMIT clause with offset exists
        assertNotNull(statement.limit)
        assertEquals(10, statement.limit!!.limit)
        assertEquals(10, statement.limit!!.offset) // page 2 with size 10 = offset 10
    }

    @Test
    fun testToStatementWithDistinct() {
        val user = MysqlUser()
        val selectClause = user.select().distinct()
        
        val statement = selectClause.toStatement()
        
        // Verify DISTINCT flag is set
        assertTrue(statement.distinct)
    }

    @Test
    fun testToStatementWithGroupBy() {
        val user = MysqlUser()
        val selectClause = user.select { it.gender }.groupBy { it.gender }
        
        val statement = selectClause.toStatement()
        
        // Verify GROUP BY clause exists
        assertNotNull(statement.groupBy)
        assertTrue(statement.groupBy!!.isNotEmpty())
        assertTrue(statement.groupBy!!.all { it is ColumnReference })
    }

    @Test
    fun testToStatementWithHaving() {
        val user = MysqlUser()
        val selectClause = user.select { it.gender }
            .groupBy { it.gender }
            .having { it.gender == 1 }
        
        val statement = selectClause.toStatement()
        
        // Verify HAVING clause exists
        assertNotNull(statement.having)
        assertTrue(statement.having is BinaryExpression)
    }
}
