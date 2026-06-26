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
    data class ColumnSelectItem(val column: ColumnReference, val alias: String?) : SelectItem()

    /**
     * ExpressionSelectItem
     *
     * Represents an expression in the SELECT clause (e.g., function calls, arithmetic operations).
     *
     * @property expression The expression to select
     * @property alias Optional alias for the expression
     */
    data class ExpressionSelectItem(val expression: Expression, val alias: String?) : SelectItem()

    /**
     * AllColumnsSelectItem
     *
     * Represents SELECT * or SELECT table.* in the SELECT clause.
     *
     * @property table Optional table name or alias for table.*, null for *
     */
    data class AllColumnsSelectItem(val table: String?) : SelectItem()
}
