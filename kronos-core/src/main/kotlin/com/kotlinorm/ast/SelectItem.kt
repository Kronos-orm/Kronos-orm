/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *     http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kotlinorm.ast

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.functions.FunctionManager

enum class SelectItemSourceScope {
    SOURCE,
    SELECTED,
    AGGREGATE,
    WINDOW,
    UNKNOWN
}

data class SelectItemAliasMetadata(
    val outputName: String,
    val expression: Expression,
    val scope: SelectItemSourceScope,
    val sourceField: Field? = null,
    val userReferenceable: Boolean = true
)

/**
 * SelectItem
 *
 * Sealed class representing items in the SELECT clause of a SQL query. Each SELECT item can be a
 * column reference, an expression, or all columns.
 *
 * @author OUSC
 */
sealed class SelectItem {
    /**
     * ColumnSelectItem
     *
     * Represents a column reference in the SELECT clause.
     *
     * @property column The column reference
     * @property alias Optional alias for the column
     */
    data class ColumnSelectItem(
        val column: ColumnReference,
        val alias: String?,
        val metadata: SelectItemAliasMetadata? = null
    ) : SelectItem()

    /**
     * ExpressionSelectItem
     *
     * Represents an expression in the SELECT clause (e.g., function calls, arithmetic operations).
     *
     * @property expression The expression to select
     * @property alias Optional alias for the expression
     */
    data class ExpressionSelectItem(
        val expression: Expression,
        val alias: String?,
        val metadata: SelectItemAliasMetadata? = null
    ) : SelectItem()

    /**
     * AllColumnsSelectItem
     *
     * Represents SELECT * or SELECT table.* in the SELECT clause.
     *
     * @property table Optional table name or alias for table.*, null for *
     */
    data class AllColumnsSelectItem(val table: String?) : SelectItem()

    fun aliasMetadata(index: Int): SelectItemAliasMetadata? {
        return when (this) {
            is ColumnSelectItem -> metadata ?: SelectItemAliasMetadata(
                outputName = alias ?: column.columnName,
                expression = column,
                scope = if (alias == null) SelectItemSourceScope.SOURCE else SelectItemSourceScope.SELECTED,
                sourceField = null,
                userReferenceable = true
            )

            is ExpressionSelectItem -> metadata ?: SelectItemAliasMetadata(
                outputName = alias ?: "__kronos_expr_$index",
                expression = expression,
                scope = if (alias == null) SelectItemSourceScope.UNKNOWN else expression.defaultScope(),
                sourceField = null,
                userReferenceable = alias != null
            )

            is AllColumnsSelectItem -> null
        }
    }
}

private fun Expression.defaultScope(): SelectItemSourceScope {
    return when (this) {
        is ColumnReference -> SelectItemSourceScope.SELECTED
        is FunctionCall -> FunctionManager.getSelectItemScope(functionName)
        else -> SelectItemSourceScope.UNKNOWN
    }
}
