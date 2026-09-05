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

package com.kotlinorm.interfaces

import com.kotlinorm.beans.parser.ParsedSql
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.syntax.statement.SqlStatement

/**
 * Minimal executable SQL unit handed from ORM planning to a data-source wrapper.
 *
 * [sql] and [paramMap] are the named-parameter representation. [parsed] must preserve
 * the one-to-one order of materialized JDBC values and parameter names so binding metadata
 * can be resolved by position. [statement] is optional planning context, not a substitute
 * for the materialized SQL contract.
 */
interface KAtomicTask {
    /**
     * Named-parameter SQL before JDBC positional materialization.
     * Implementations may make it mutable while planners finalize the statement.
     */
    val sql: String

    /**
     * Values keyed by names referenced from [sql].
     * A value may be `null`; wrappers must not infer its JDBC type from the runtime value.
     */
    val paramMap: Map<String, Any?>

    /**
     * Operation classification used by execution hooks and wrappers.
     * It describes intent independently from the SQL string's leading token.
     */
    val operationType: KOperationType

    /**
     * Structured statement that produced [sql], when retained by the planner.
     * Wrappers must execute [parsed] output rather than re-render this optional value.
     */
    val statement: SqlStatement?
        get() = null

    /**
     * Materializes this task for JDBC-style positional execution.
     *
     * @return SQL, values, and parameter names in the exact binding order
     */
    fun parsed(): ParsedSql
}
