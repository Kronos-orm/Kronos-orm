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

import com.kotlinorm.database.ConflictResolver

/**
 * InsertStatement
 *
 * Represents an INSERT SQL statement in the AST.
 *
 * @property table The table reference to insert into
 * @property columns List of column references to insert into
 * @property values List of value assignments corresponding to columns
 * @property conflictResolver Optional conflict resolution strategy (ON CONFLICT, ON DUPLICATE KEY
 * UPDATE, etc.)
 *
 * @author OUSC
 */
data class InsertStatement(
        val table: TableReference,
        val columns: List<ColumnReference>,
        val values: List<Expression>,
        val conflictResolver: ConflictResolver? = null
) : Statement
