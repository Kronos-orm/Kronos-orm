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

import com.kotlinorm.enums.PessimisticLock

/**
 * Marker interfaces for SQL AST
 */
sealed interface SqlNode
sealed interface Statement : SqlNode
sealed interface Expression : SqlNode
sealed interface TableSource : SqlNode

/**
 * Common enums and DTOs used by the AST
 */
@Suppress("EnumEntryName")
enum class JoinType { inner, left, right, full, cross, lateral }

@Suppress("EnumEntryName")
enum class SetOperator { union, unionAll, intersect, except }

@Suppress("EnumEntryName")
enum class OrderDirection { asc, desc }

@Suppress("EnumEntryName")
enum class NullsOrder { first, last, defaultOrder }

/**
 * Table and identifier modeling
 */

data class Identifier(
    val name: String,
    val quoted: Boolean = false
) : SqlNode

/**
 * A reference to a fully qualified table name.
 */
data class TableName(
    val database: String? = null,
    val schema: String? = null,
    val table: String,
    val alias: String? = null
) : TableSource

/**
 * A subquery used as a table source: (select ...) as alias
 */
data class SubquerySource(
    val query: SelectStatement,
    val alias: String
) : TableSource

/**
 * JOIN between two table sources.
 */
data class Join(
    val left: TableSource,
    val right: TableSource,
    val type: JoinType = JoinType.inner,
    val on: Expression? = null
) : TableSource

/**
 * Raw table source placeholder for complex join SQL strings that are not yet parsed.
 */
data class RawTableSource(
    val sql: String
) : TableSource

/**
 * CTE (WITH clause)
 */
data class CommonTableExpression(
    val name: String,
    val query: SelectStatement,
    val columns: List<String> = emptyList()
) : SqlNode

/**
 * ORDER BY item
 */
data class OrderItem(
    val expression: Expression,
    val direction: OrderDirection = OrderDirection.asc,
    val nulls: NullsOrder = NullsOrder.defaultOrder
) : SqlNode

/**
 * SELECT projection item: expr [AS alias]
 */
data class ProjectionItem(
    val expression: Expression,
    val alias: String? = null
) : SqlNode

/**
 * Lock mode for SELECT statements (reuse PessimisticLock in core when present)
 */

data class Locking(
    val lock: PessimisticLock,
    val ofTables: List<String> = emptyList()
) : SqlNode
