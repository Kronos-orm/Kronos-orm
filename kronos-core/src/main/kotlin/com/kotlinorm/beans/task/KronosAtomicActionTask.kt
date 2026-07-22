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

package com.kotlinorm.beans.task

import com.kotlinorm.beans.parser.NamedParameterUtils.parseSqlStatement
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.syntax.statement.SqlStatement

/**
 * Execution metadata for one insert, update, or delete statement.
 *
 * [stash] carries operation-local binding hints to the data-source wrapper. Generated-key
 * output remains raw JDBC data: when [generatedKeyRequest] is non-null for an insert, the
 * wrapper replaces [generatedKeys] with the returned values and derives [lastInsertId]
 * from the first numeric key when possible.
 *
 * @property sql named-parameter SQL to execute
 * @property paramMap values keyed by SQL parameter name
 * @property operationType action operation classification
 * @property statement optional structured statement that produced [sql]
 * @property stash mutable operation-local metadata shared with the data-source wrapper
 * @property generatedKeyRequest requested generated-key assignment, or `null` when disabled
 * @property generatedKeys raw generated values returned by the JDBC driver
 * @property lastInsertId numeric representation of the first generated key, when available
 * @property listParameterOccurrences named-parameter occurrence indexes eligible for expansion
 */
data class KronosAtomicActionTask(
    override var sql: String,
    override val paramMap: Map<String, Any?> = mapOf(),
    override val operationType: KOperationType = KOperationType.UPDATE,
    override val statement: SqlStatement? = null,
    override val stash: MutableMap<String, Any?> = mutableMapOf(),
    override var generatedKeyRequest: GeneratedKeyRequest? = null,
    override val generatedKeys: MutableList<Any?> = mutableListOf(),
    override var lastInsertId: Long? = null,
    val listParameterOccurrences: Set<Int> = emptySet()
) : KAtomicActionTask {

    /**
     * Materializes named SQL and parameters into the exact JDBC SQL, value order, and
     * parameter-name order used by binding.
     *
     * @return parsed JDBC statement metadata for this action
     */
    override fun parsed() = parseSqlStatement(sql, paramMap, listParameterOccurrences)
}
