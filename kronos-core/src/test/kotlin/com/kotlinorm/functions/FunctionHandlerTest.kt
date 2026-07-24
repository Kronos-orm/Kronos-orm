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

package com.kotlinorm.functions

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlBuiltinFunction
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlWindow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class FunctionHandlerTest {

    @Test
    fun testAsThrowsException() {
        val exception = assertFailsWith<UnsupportedOperationException> {
            FunctionHandler.alias("alias")
        }
        assertEquals("You will never want to alias an empty function handle.", exception.message)
    }

    @Test
    fun `kronos function expressions stay syntax native`() {
        val count = KronosFunctionExpressions.callArgs("count", listOf(1))
        assertEquals(
            SqlExpr.Function(
                name = SqlIdentifier.of("COUNT"),
                args = listOf(SqlExpr.NumberLiteral("1")),
                builtinFunction = SqlBuiltinFunction.Count
            ),
            count.expr
        )

        val add = KronosFunctionExpressions.callArgs("add", listOf(1, 2, 3))
        assertEquals(
            SqlExpr.Binary(
                SqlExpr.Binary(
                    SqlExpr.NumberLiteral("1"),
                    SqlBinaryOperator.Plus,
                    SqlExpr.NumberLiteral("2")
                ),
                SqlBinaryOperator.Plus,
                SqlExpr.NumberLiteral("3")
            ),
            add.expr
        )

        val window = SqlWindow(partitionBy = listOf(SqlExpr.Column(columnName = "user_id")))
        val rowNumber = KronosFunctionExpressions.callWindowArgs("rowNumber", window = window)
        val windowExpr = assertIs<SqlExpr.Window>(rowNumber.expr)
        val rowNumberFunction = windowExpr.expr as SqlExpr.Function
        assertEquals(SqlIdentifier.of("ROW_NUMBER"), rowNumberFunction.name)
        assertEquals(SqlBuiltinFunction.RowNumber, rowNumberFunction.builtinFunction)
        assertEquals(window, windowExpr.window)
    }
}
