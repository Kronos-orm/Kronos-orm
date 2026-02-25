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

import com.kotlinorm.ast.*
import com.kotlinorm.beans.sample.UserRelation
import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.enums.JoinType
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for SelectFrom AST generation (toStatement method)
 * Tests JOIN query AST structure
 */
class SelectFromAstTest : MysqlTestBase() {

    @Test
    fun testToStatementGeneratesCorrectAst() {
        val selectFrom = MysqlUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id + user.username + relation.gender }
        }
        
        // Call toStatement to get the AST
        val statement = selectFrom.toStatement()
        
        // Verify FROM clause is a JoinTable
        assertNotNull(statement.from)
        assertTrue(statement.from is JoinTable, "FROM should be JoinTable for JOIN queries")
        
        val joinTable = statement.from as JoinTable
        
        // Verify left table is the main table
        assertTrue(joinTable.left is TableName)
        val leftTable = joinTable.left as TableName
        assertEquals("tb_user", leftTable.table)
        
        // Verify right table is the joined table
        assertTrue(joinTable.right is TableName)
        val rightTable = joinTable.right as TableName
        assertEquals("user_relation", rightTable.table)
        
        // Verify join type
        assertEquals(JoinType.LEFT_JOIN, joinTable.joinType)
        
        // Verify join condition exists
        assertNotNull(joinTable.condition)
        assertTrue(joinTable.condition is Expression)
    }

    @Test
    fun testToStatementWithMultipleJoins() {
        val selectFrom = MysqlUser(1).join(
            UserRelation(1, "test1", 1, 1),
            UserRelation(2, "test2", 1, 1)
        ) { user, relation1, relation2 ->
            leftJoin(relation1) { user.id == relation1.id2 }
            leftJoin(relation2) { user.id == relation2.id2 }
            select { user.id + relation1.gender + relation2.gender }
        }
        
        val statement = selectFrom.toStatement()
        
        // Verify FROM clause is a nested JoinTable
        assertTrue(statement.from is JoinTable)
        
        val outerJoin = statement.from as JoinTable
        // The outer join's left side should also be a JoinTable (nested joins)
        assertTrue(outerJoin.left is JoinTable, "Multiple joins should create nested JoinTable structure")
        
        val innerJoin = outerJoin.left as JoinTable
        // The innermost left should be the main table
        assertTrue(innerJoin.left is TableName)
        val mainTable = innerJoin.left as TableName
        assertEquals("tb_user", mainTable.table)
    }

    @Test
    fun testToStatementWithWhereClause() {
        val selectFrom = MysqlUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id + user.username }
        }
        
        val statement = selectFrom.toStatement()
        
        // Verify WHERE clause exists (should include the id=1 condition from MysqlUser(1))
        assertNotNull(statement.where)
        assertTrue(statement.where is Expression)
    }

    @Test
    fun testToStatementSelectListWithTableAliases() {
        val selectFrom = MysqlUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id + user.username + relation.gender }
        }
        
        val statement = selectFrom.toStatement()
        
        // Verify select list contains items from both tables
        assertEquals(3, statement.selectList.size)
        
        // All items should be ExpressionSelectItem for JOIN queries (with table aliases)
        assertTrue(statement.selectList.all { it is SelectItem.ExpressionSelectItem })
        
        // Verify aliases
        val aliases = statement.selectList.map { (it as SelectItem.ExpressionSelectItem).alias }
        assertTrue(aliases.contains("id"))
        assertTrue(aliases.contains("username"))
        assertTrue(aliases.contains("gender"))
    }

    @Test
    fun testToStatementJoinConditionWithTableAliases() {
        val selectFrom = MysqlUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id }
        }
        
        val statement = selectFrom.toStatement()
        
        val joinTable = statement.from as JoinTable
        val joinCondition = joinTable.condition
        
        // Verify join condition is a binary expression
        assertNotNull(joinCondition)
        assertTrue(joinCondition is BinaryExpression)
        
        val binaryExpr = joinCondition as BinaryExpression
        assertEquals(SqlOperator.EQUAL, binaryExpr.operator)
        
        // Both sides should be ColumnReference with table aliases
        assertTrue(binaryExpr.left is ColumnReference)
        assertTrue(binaryExpr.right is ColumnReference)
        
        val leftCol = binaryExpr.left as ColumnReference
        val rightCol = binaryExpr.right as ColumnReference
        
        // Verify table aliases are present
        assertNotNull(leftCol.tableAlias, "Left column should have table alias in JOIN condition")
        assertNotNull(rightCol.tableAlias, "Right column should have table alias in JOIN condition")
    }

    @Test
    fun testToStatementWithRightJoin() {
        val selectFrom = MysqlUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            rightJoin(relation) { user.id == relation.id2 }
            select { user.id }
        }
        
        val statement = selectFrom.toStatement()
        
        val joinTable = statement.from as JoinTable
        assertEquals(JoinType.RIGHT_JOIN, joinTable.joinType)
    }

    @Test
    fun testToStatementWithInnerJoin() {
        val selectFrom = MysqlUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            innerJoin(relation) { user.id == relation.id2 }
            select { user.id }
        }
        
        val statement = selectFrom.toStatement()
        
        val joinTable = statement.from as JoinTable
        assertEquals(JoinType.INNER_JOIN, joinTable.joinType)
    }

    @Test
    fun testToStatementWithCrossJoin() {
        val selectFrom = MysqlUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            crossJoin(relation) { user.id == relation.id2 }
            select { user.id }
        }
        
        val statement = selectFrom.toStatement()
        
        val joinTable = statement.from as JoinTable
        assertEquals(JoinType.CROSS_JOIN, joinTable.joinType)
    }

    @Test
    fun testToStatementWithFullJoin() {
        val selectFrom = MysqlUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            fullJoin(relation) { user.id == relation.id2 }
            select { user.id }
        }
        
        val statement = selectFrom.toStatement()
        
        val joinTable = statement.from as JoinTable
        assertEquals(JoinType.FULL_JOIN, joinTable.joinType)
    }

    @Test
    fun testToStatementWithOrderBy() {
        val selectFrom = MysqlUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id }
            orderBy { user.id.desc() }
        }
        
        val statement = selectFrom.toStatement()
        
        // Verify ORDER BY clause exists
        assertNotNull(statement.orderBy)
        assertTrue(statement.orderBy!!.isNotEmpty())
        
        val orderByItem = statement.orderBy!![0]
        assertEquals(com.kotlinorm.enums.SortType.DESC, orderByItem.direction)
    }

    @Test
    fun testToStatementWithLimit() {
        val selectFrom = MysqlUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id }
            limit(10)
        }
        
        val statement = selectFrom.toStatement()
        
        // Verify LIMIT clause exists
        assertNotNull(statement.limit)
        assertEquals(10, statement.limit!!.limit)
    }

    @Test
    fun testToStatementWithGroupBy() {
        val selectFrom = MysqlUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id + relation.gender }
            groupBy { relation.gender }
        }
        
        val statement = selectFrom.toStatement()
        
        // Verify GROUP BY clause exists
        assertNotNull(statement.groupBy)
        assertTrue(statement.groupBy!!.isNotEmpty())
    }

    @Test
    fun testToStatementWithDistinct() {
        val selectFrom = MysqlUser(1).join(UserRelation(1, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id }
            distinct()
        }
        
        val statement = selectFrom.toStatement()
        
        // Verify DISTINCT flag is set
        assertTrue(statement.distinct)
    }
}
