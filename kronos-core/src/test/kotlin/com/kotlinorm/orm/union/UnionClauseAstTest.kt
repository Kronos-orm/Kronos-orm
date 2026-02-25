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

import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.orm.select.select
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for UnionClause build logic
 * 
 * Note: UnionClause doesn't have a toStatement() method that returns UnionStatement AST.
 * Instead, it calls build() on each selectable and combines the SQL strings.
 * This test verifies the parameter naming and SQL combination logic.
 */
class UnionClauseAstTest : MysqlTestBase() {

    @Test
    fun testParameterNamingWithTwoQueries() {
        val unionClause = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 }
        )
        
        val task = unionClause.build()
        
        // Verify parameter names use @ suffix for uniqueness
        assertTrue(task.atomicTask.paramMap.containsKey("id"))
        assertTrue(task.atomicTask.paramMap.containsKey("id@1"))
        assertEquals(1, task.atomicTask.paramMap["id"])
        assertEquals(2, task.atomicTask.paramMap["id@1"])
    }

    @Test
    fun testParameterNamingWithThreeQueries() {
        val unionClause = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 },
            MysqlUser().select().where { it.id == 3 }
        )
        
        val task = unionClause.build()
        
        // Verify parameter names use @ suffix for uniqueness
        assertTrue(task.atomicTask.paramMap.containsKey("id"))
        assertTrue(task.atomicTask.paramMap.containsKey("id@1"))
        assertTrue(task.atomicTask.paramMap.containsKey("id@2"))
        assertEquals(1, task.atomicTask.paramMap["id"])
        assertEquals(2, task.atomicTask.paramMap["id@1"])
        assertEquals(3, task.atomicTask.paramMap["id@2"])
    }

    @Test
    fun testUnionKeywordInSql() {
        val unionClause = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 }
        )
        
        val task = unionClause.build()
        
        // Verify SQL contains UNION keyword
        assertTrue(task.atomicTask.sql.contains("UNION"))
        assertFalse(task.atomicTask.sql.contains("UNION ALL"))
    }

    @Test
    fun testUnionAllKeywordInSql() {
        val unionClause = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 }
        ).all()
        
        val task = unionClause.build()
        
        // Verify SQL contains UNION ALL keyword
        assertTrue(task.atomicTask.sql.contains("UNION ALL"))
    }

    @Test
    fun testInfixUnionParameterNaming() {
        val unionClause = MysqlUser().select().where { it.id == 1 }
            .union(MysqlUser().select().where { it.id == 2 })
        
        val task = unionClause.build()
        
        // Verify parameter names use @ suffix
        assertTrue(task.atomicTask.paramMap.containsKey("id"))
        assertTrue(task.atomicTask.paramMap.containsKey("id@1"))
    }

    @Test
    fun testInfixUnionAllParameterNaming() {
        val unionClause = MysqlUser().select().where { it.id == 1 }
            .unionAll(MysqlUser().select().where { it.id == 2 })
        
        val task = unionClause.build()
        
        // Verify parameter names use @ suffix
        assertTrue(task.atomicTask.paramMap.containsKey("id"))
        assertTrue(task.atomicTask.paramMap.containsKey("id@1"))
        
        // Verify SQL contains UNION ALL
        assertTrue(task.atomicTask.sql.contains("UNION ALL"))
    }

    @Test
    fun testUnionWithLimit() {
        val unionClause = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 }
        ).limit(10)
        
        val task = unionClause.build()
        
        // Verify SQL contains LIMIT clause
        assertTrue(task.atomicTask.sql.contains("LIMIT 10"))
    }

    @Test
    fun testUnionWithLimitAndOffset() {
        val unionClause = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 }
        ).limit(10, 5)
        
        val task = unionClause.build()
        
        // Verify SQL contains LIMIT and OFFSET clauses
        assertTrue(task.atomicTask.sql.contains("LIMIT 10"))
        assertTrue(task.atomicTask.sql.contains("OFFSET 5"))
    }

    @Test
    fun testUnionWithMultipleParametersPerQuery() {
        val unionClause = union(
            MysqlUser().select().where { it.id == 1 && it.username == "user1" },
            MysqlUser().select().where { it.id == 2 && it.username == "user2" }
        )
        
        val task = unionClause.build()
        
        // Verify all parameters are present with correct naming
        assertTrue(task.atomicTask.paramMap.containsKey("id"))
        assertTrue(task.atomicTask.paramMap.containsKey("username"))
        assertTrue(task.atomicTask.paramMap.containsKey("id@1"))
        assertTrue(task.atomicTask.paramMap.containsKey("username@1"))
        
        assertEquals(1, task.atomicTask.paramMap["id"])
        assertEquals("user1", task.atomicTask.paramMap["username"])
        assertEquals(2, task.atomicTask.paramMap["id@1"])
        assertEquals("user2", task.atomicTask.paramMap["username@1"])
    }

    @Test
    fun testSqlContainsParentheses() {
        val unionClause = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 }
        )
        
        val task = unionClause.build()
        
        // Verify each SELECT is wrapped in parentheses
        val sql = task.atomicTask.sql
        assertTrue(sql.startsWith("(SELECT"))
        assertTrue(sql.contains(") UNION (SELECT"))
        assertTrue(sql.endsWith(")"))
    }
}
