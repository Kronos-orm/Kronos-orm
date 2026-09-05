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

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.beans.task

import com.kotlinorm.annotations.InternalKronosApi
import com.kotlinorm.beans.parser.NamedParameterUtils.parseSqlStatement
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.ConflictingResultColumnLabels
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.syntax.statement.SqlQuery
import kotlin.reflect.KType

/**
 * Immutable execution metadata for one query, apart from the mutable SQL text and stash.
 *
 * [targetType] is the logical result type consumed by the data-source wrapper. The
 * [resultColumns] map carries projection metadata to the single decode boundary; its keys
 * are planned result labels and therefore may not differ only by case. [stash] transports
 * operation-local binding metadata and is forwarded unchanged to the JDBC statement context.
 *
 * [listParameterOccurrences] identifies named-parameter occurrences that may expand to
 * multiple JDBC positions. It is interpreted together with [paramMap] by [parsed].
 *
 * @property sql named-parameter SQL to execute
 * @property paramMap values keyed by SQL parameter name
 * @property operationType query operation classification
 * @property statement optional structured query that produced [sql]
 * @property targetType complete logical result type, including generic arguments and nullability
 * @property stash mutable operation-local metadata shared with the data-source wrapper
 * @property resultColumns exact planned result labels and their logical decode metadata
 * @property listParameterOccurrences named-parameter occurrence indexes eligible for expansion
 * @throws ConflictingResultColumnLabels when distinct metadata keys differ only by case
 */
data class KronosAtomicQueryTask(
    override var sql: String,
    override val paramMap: Map<String, Any?> = mapOf(),
    override val operationType: KOperationType = KOperationType.SELECT,
    override val statement: SqlQuery? = null,
    override val targetType: KType,
    override val stash: MutableMap<String, Any?> = mutableMapOf(),
    @InternalKronosApi
    override val resultColumns: Map<String, ResultColumnMetadata> = emptyMap(),
    val listParameterOccurrences: Set<Int> = emptySet()
) : KAtomicQueryTask {

    init {
        val labelsByCase = resultColumns.keys.groupBy(String::lowercase)
        val conflict = labelsByCase.values.firstOrNull { it.size > 1 }
        if (conflict != null) throw ConflictingResultColumnLabels(conflict)
    }

    /**
     * Materializes named SQL and parameters into the exact JDBC SQL, value order, and
     * parameter-name order used by binding.
     *
     * @return parsed JDBC statement metadata for this query
     */
    override fun parsed() = parseSqlStatement(sql, paramMap, listParameterOccurrences)
}
