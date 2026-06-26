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
 * TableReference implementations
 *
 * Concrete implementations of TableReference interface for different table reference types.
 *
 * @author OUSC
 */
object TableReferenceImpl {
    /**
     * SimpleTableReference
     *
     * Represents a simple table name reference.
     *
     * @property tableName The name of the table
     * @property alias Optional table alias
     */
    data class SimpleTableReference(val tableName: String, val alias: String? = null) :
            TableReference

    /**
     * SubqueryTableReference
     *
     * Represents a subquery used as a table reference (derived table).
     *
     * @property subquery The SELECT statement used as a subquery
     * @property alias Required alias for the subquery (required by SQL)
     */
    data class SubqueryTableReference(val subquery: SelectStatement, val alias: String) :
            TableReference

    /**
     * JoinedTableReference
     *
     * Represents a table reference with JOIN clauses.
     *
     * @property baseTable The base table reference
     * @property joins List of JOIN clauses
     */
    data class JoinedTableReference(val baseTable: TableReference, val joins: List<Join>) :
            TableReference
}
