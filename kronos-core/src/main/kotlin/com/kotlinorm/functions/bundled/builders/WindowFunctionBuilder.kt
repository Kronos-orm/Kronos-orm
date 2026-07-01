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

package com.kotlinorm.functions.bundled.builders

import com.kotlinorm.ast.Expression
import com.kotlinorm.ast.FunctionCall
import com.kotlinorm.ast.OrderByItem
import com.kotlinorm.ast.RenderContext
import com.kotlinorm.ast.SelectItemSourceScope
import com.kotlinorm.ast.WindowClause
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.SortType
import com.kotlinorm.interfaces.FunctionBuilder
import com.kotlinorm.interfaces.KronosDataSourceWrapper

/**
 * Renders ANSI window functions.
 */
object WindowFunctionBuilder : FunctionBuilder {
    private val all = arrayOf(
        DBType.Mysql, DBType.Postgres, DBType.SQLite, DBType.Oracle, DBType.Mssql
    )

    override val supportFunctionNames: (String) -> Array<DBType> = {
        when (it.lowercase()) {
            "rownumber", "row_number" -> all
            else -> emptyArray()
        }
    }

    override fun selectItemScope(functionName: String): SelectItemSourceScope = SelectItemSourceScope.WINDOW

    override fun transform(
        field: FunctionField,
        dataSource: KronosDataSourceWrapper,
        showTable: Boolean,
        showAlias: Boolean
    ): String {
        val alias = field.name.takeIf { showAlias && it != field.functionName }.orEmpty()
        return MathFunctionBuilder.buildAlias(
            "ROW_NUMBER()${field.over?.let { " OVER (${renderWindowClause(it)})" }.orEmpty()}",
            alias
        )
    }

    override fun transformAst(
        function: FunctionCall,
        context: RenderContext,
        renderExpression: (Expression, RenderContext) -> String
    ): String? {
        val functionName = when (function.functionName.lowercase()) {
            "rownumber", "row_number" -> "ROW_NUMBER"
            else -> return null
        }
        val over = function.over?.let {
            " OVER (${renderWindowClause(it, context, renderExpression)})"
        }.orEmpty()
        return "$functionName()$over"
    }

    private fun renderWindowClause(
        window: WindowClause,
        context: RenderContext,
        renderExpression: (Expression, RenderContext) -> String
    ): String {
        val partitionBy = window.partitionBy?.takeIf { it.isNotEmpty() }?.let { expressions ->
            "PARTITION BY ${expressions.joinToString(", ") { renderExpression(it, context) }}"
        }
        val orderBy = window.orderBy?.takeIf { it.isNotEmpty() }?.let { items ->
            "ORDER BY ${items.joinToString(", ") { renderOrderByItem(it, context, renderExpression) }}"
        }
        return listOfNotNull(partitionBy, orderBy).joinToString(" ")
    }

    private fun renderOrderByItem(
        item: OrderByItem,
        context: RenderContext,
        renderExpression: (Expression, RenderContext) -> String
    ): String {
        val direction = when (item.direction) {
            SortType.ASC -> "ASC"
            SortType.DESC -> "DESC"
        }
        return "${renderExpression(item.expression, context)} $direction"
    }

    private fun renderWindowClause(window: WindowClause): String {
        val partitionBy = window.partitionBy?.takeIf { it.isNotEmpty() }?.let { "PARTITION BY ${it.joinToString()}" }
        val orderBy = window.orderBy?.takeIf { it.isNotEmpty() }?.let { "ORDER BY ${it.joinToString()}" }
        return listOfNotNull(partitionBy, orderBy).joinToString(" ")
    }
}
