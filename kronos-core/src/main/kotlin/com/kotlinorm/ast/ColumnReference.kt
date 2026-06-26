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
 * ColumnReference
 *
 * Represents a reference to a column in SQL, which can include a table alias or schema prefix. This
 * is a type of Expression used to reference columns in SELECT, WHERE, ORDER BY, etc.
 *
 * @property database Optional database name (for cross-database queries)
 * @property tableAlias Optional table alias or table name prefix
 * @property columnName The name of the column
 *
 * @author OUSC
 */
data class ColumnReference(
    val database: String? = null,
    val tableAlias: String?, 
    val columnName: String
) : Expression
