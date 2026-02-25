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

import com.kotlinorm.database.mysql.MysqlSqlRenderer
import com.kotlinorm.database.postgres.PostgresqlSqlRenderer
import com.kotlinorm.database.mssql.MssqlSqlRenderer
import com.kotlinorm.database.oracle.OracleSqlRenderer
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test function call rendering with FunctionManager integration
 */
class FunctionCallRenderTest {
    
    private fun createSelect(expr: Expression): SelectStatement {
        return SelectStatement(
            selectList = mutableListOf(SelectItem.ExpressionSelectItem(expr, null)),
            from = TableName(table = "users", alias = null)
        )
    }
    
    @Test
    fun testMathOperatorAdd() {
        val renderer = MysqlSqlRenderer()
        val addFunc = FunctionCall(
            functionName = "add",
            arguments = listOf(Literal.NumberLiteral("10"), Literal.NumberLiteral("20"))
        )
        val result = renderer.render(createSelect(addFunc))
        assertTrue(result.sql.contains("(10 + 20)"), "Expected infix operator, got: ${result.sql}")
    }
    
    @Test
    fun testCeilFunctionMySQL() {
        val renderer = MysqlSqlRenderer()
        val ceilFunc = FunctionCall(
            functionName = "ceil",
            arguments = listOf(Literal.NumberLiteral("12.3"))
        )
        val result = renderer.render(createSelect(ceilFunc))
        assertTrue(result.sql.contains("CEIL(12.3)"), "Expected CEIL for MySQL, got: ${result.sql}")
    }
    
    @Test
    fun testCeilFunctionMSSQL() {
        val renderer = MssqlSqlRenderer()
        val ceilFunc = FunctionCall(
            functionName = "ceil",
            arguments = listOf(Literal.NumberLiteral("12.3"))
        )
        val result = renderer.render(createSelect(ceilFunc))
        assertTrue(result.sql.contains("CEILING(12.3)"), "Expected CEILING for SQL Server, got: ${result.sql}")
    }
    
    @Test
    fun testLnFunctionMySQL() {
        val renderer = MysqlSqlRenderer()
        val lnFunc = FunctionCall(
            functionName = "ln",
            arguments = listOf(Literal.NumberLiteral("2"))
        )
        val result = renderer.render(createSelect(lnFunc))
        assertTrue(result.sql.contains("LN(2)"), "Expected LN for MySQL, got: ${result.sql}")
    }
    
    @Test
    fun testLnFunctionMSSQL() {
        val renderer = MssqlSqlRenderer()
        val lnFunc = FunctionCall(
            functionName = "ln",
            arguments = listOf(Literal.NumberLiteral("2"))
        )
        val result = renderer.render(createSelect(lnFunc))
        assertTrue(result.sql.contains("LOG(2, EXP(1))"), "Expected LOG(x, EXP(1)) for SQL Server, got: ${result.sql}")
    }
    
    @Test
    fun testRandFunctionMySQL() {
        val renderer = MysqlSqlRenderer()
        val randFunc = FunctionCall(functionName = "rand")
        val result = renderer.render(createSelect(randFunc))
        assertTrue(result.sql.contains("RAND()"), "Expected RAND() for MySQL, got: ${result.sql}")
    }
    
    @Test
    fun testRandFunctionPostgres() {
        val renderer = PostgresqlSqlRenderer()
        val randFunc = FunctionCall(functionName = "rand")
        val result = renderer.render(createSelect(randFunc))
        assertTrue(result.sql.contains("RANDOM()"), "Expected RANDOM() for PostgreSQL, got: ${result.sql}")
    }
    
    @Test
    fun testRandFunctionOracle() {
        val renderer = OracleSqlRenderer()
        val randFunc = FunctionCall(functionName = "rand")
        val result = renderer.render(createSelect(randFunc))
        assertTrue(result.sql.contains("DBMS_RANDOM.VALUE"), "Expected DBMS_RANDOM.VALUE for Oracle, got: ${result.sql}")
    }
    
    @Test
    fun testModFunctionMySQL() {
        val renderer = MysqlSqlRenderer()
        val modFunc = FunctionCall(
            functionName = "mod",
            arguments = listOf(Literal.NumberLiteral("10"), Literal.NumberLiteral("3"))
        )
        val result = renderer.render(createSelect(modFunc))
        assertTrue(result.sql.contains("(10 % 3)"), "Expected % operator for MySQL, got: ${result.sql}")
    }
    
    @Test
    fun testModFunctionOracle() {
        val renderer = OracleSqlRenderer()
        val modFunc = FunctionCall(
            functionName = "mod",
            arguments = listOf(Literal.NumberLiteral("10"), Literal.NumberLiteral("3"))
        )
        val result = renderer.render(createSelect(modFunc))
        assertTrue(result.sql.contains("MOD(10, 3)"), "Expected MOD function for Oracle, got: ${result.sql}")
    }
}
