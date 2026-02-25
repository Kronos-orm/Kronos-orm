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

import com.kotlinorm.enums.JoinType

/**
 * JoinTable
 *
 * Represents a JOIN operation between two table references. Supports all JOIN types (INNER, LEFT,
 * RIGHT, FULL, CROSS) and nested JOINs.
 *
 * Format: left_table joinType right_table ON condition
 *
 * @property left The left table reference (can be a TableName, SubqueryTable, or nested JoinTable)
 * @property joinType The type of join (INNER, LEFT, RIGHT, FULL, CROSS)
 * @property right The right table reference (can be a TableName, SubqueryTable, or nested
 * JoinTable)
 * @property condition The join condition expression (ON clause, optional for CROSS JOIN)
 *
 * @author OUSC
 */
data class JoinTable(
        val left: TableReference,
        val joinType: JoinType,
        val right: TableReference,
        val condition: Expression? = null
) : TableReference
