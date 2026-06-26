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

import com.kotlinorm.enums.PessimisticLock

/**
 * SelectStatement
 *
 * Represents a complete SELECT SQL statement in the AST. Contains all clauses that can appear in a
 * SELECT query. All fields are mutable to allow direct modification without copy().
 *
 * @property selectList List of items to select (columns, expressions, etc.)
 * @property from The table or tables to select from
 * @property where Optional WHERE clause expression
 * @property groupBy Optional list of expressions for GROUP BY clause
 * @property having Optional HAVING clause expression (used with GROUP BY)
 * @property orderBy Optional list of order by items for ORDER BY clause
 * @property limit Optional LIMIT clause for pagination
 * @property distinct Whether to use DISTINCT keyword
 * @property lock Optional pessimistic lock type (FOR UPDATE, FOR SHARE, etc.)
 *
 * @author OUSC
 */
class SelectStatement(
        var selectList: MutableList<SelectItem> = mutableListOf(),
        var from: TableReference,
        var where: Expression? = null,
        var groupBy: MutableList<Expression>? = null,
        var having: Expression? = null,
        var orderBy: MutableList<OrderByItem>? = null,
        var limit: LimitClause? = null,
        var distinct: Boolean = false,
        var lock: PessimisticLock? = null
) : Statement
