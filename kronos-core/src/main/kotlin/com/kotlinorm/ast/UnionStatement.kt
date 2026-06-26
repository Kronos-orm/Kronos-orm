/**
 * Copyright 2022-2025 kronos-orm
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

import com.kotlinorm.enums.SortType

/**
 * UnionStatement
 *
 * Represents a SQL UNION statement that combines multiple SELECT statements.
 * Supports UNION and UNION ALL operations.
 *
 * Example SQL:
 * ```sql
 * SELECT id, name FROM users WHERE id = 1
 * UNION
 * SELECT id, name FROM customers WHERE id = 2
 * ORDER BY id
 * LIMIT 10
 * ```
 *
 * @property queries List of SELECT statements to union
 * @property unionAll If true, uses UNION ALL (includes duplicates), otherwise UNION (removes duplicates)
 * @property orderBy Optional ORDER BY clause applied to the entire union result
 * @property limit Optional LIMIT clause applied to the entire union result
 *
 * @author OUSC
 */
data class UnionStatement(
    val queries: List<SelectStatement>,
    val unionAll: Boolean = false,
    val orderBy: List<OrderByItem>? = null,
    val limit: LimitClause? = null
) : Statement
