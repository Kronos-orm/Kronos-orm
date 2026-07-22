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
import com.kotlinorm.beans.parser.ParsedSql
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KBatchTask
import com.kotlinorm.syntax.statement.SqlStatement

/**
 * Batch execution metadata for one SQL shape and zero or more parameter maps.
 *
 * Every entry in [paramMapArr] is parsed independently with the same SQL and list-expansion
 * occurrence metadata. Callers must ensure all parsed entries produce a compatible JDBC SQL
 * shape before passing the task to a wrapper. [parsed] is intentionally unsupported because
 * a batch has multiple parameter sequences; use [parsedSqlArr] for lossless metadata.
 *
 * @property sql named-parameter SQL shared by every batch entry
 * @property paramMapArr parameter maps in execution order; `null` represents an empty batch
 * @property operationType action operation classification
 * @property statement optional structured statement that produced [sql]
 * @property stash mutable operation-local metadata shared with the data-source wrapper
 * @property generatedKeyRequest generated-key request metadata retained from the source task
 * @property generatedKeys raw generated values when a wrapper supports them for this batch
 * @property lastInsertId numeric representation of the first generated key, when available
 * @property listParameterOccurrences named-parameter occurrence indexes eligible for expansion
 */
data class KronosAtomicBatchTask(
    override val sql: String,
    override val paramMapArr: Array<Map<String, Any?>>? = null,
    override val operationType: KOperationType = KOperationType.UPDATE,
    override val statement: SqlStatement? = null,
    override val stash: MutableMap<String, Any?> = mutableMapOf(),
    override var generatedKeyRequest: GeneratedKeyRequest? = null,
    override val generatedKeys: MutableList<Any?> = mutableListOf(),
    override var lastInsertId: Long? = null,
    val listParameterOccurrences: Set<Int> = emptySet()
) : KAtomicActionTask, KBatchTask {

    @Deprecated("Please use 'paramMapArr' instead.")
    override val paramMap: Map<String, Any?> = mapOf()

    override fun parsed(): ParsedSql {
        throw UnsupportedOperationException("Please use `parsedArr()` instead of `parsed()`")
    }

    /**
     * Returns the legacy batch representation of shared JDBC SQL and ordered value arrays.
     *
     * @return shared JDBC SQL, or `null` for an empty batch, paired with each value array
     */
    fun parsedArr() = parsedSqlArr().let {
        Pair(it.firstOrNull()?.jdbcSql, it.map { parsedSql -> parsedSql.jdbcParamList })
    }

    /**
     * Parses every parameter map while preserving JDBC parameter names alongside values.
     *
     * @return parsed statements in the same order as [paramMapArr], or an empty list
     */
    fun parsedSqlArr(): List<ParsedSql> =
        (paramMapArr ?: arrayOf()).map { parseSqlStatement(sql, it, listParameterOccurrences) }

    /**
     * Checks if this object is equal to another object.
     *
     * @param other the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KronosAtomicBatchTask

        if (sql != other.sql) return false
        if (paramMapArr != null) {
            if (other.paramMapArr == null) return false
            if (!paramMapArr.contentEquals(other.paramMapArr)) return false
        } else if (other.paramMapArr != null) return false
        if (operationType != other.operationType) return false

        return true
    }

    /**
     * Calculates the hash code value for the object.
     *
     * @return the hash code value of the object.
     */
    override fun hashCode(): Int {
        var result = sql.hashCode()
        result = 31 * result + (paramMapArr?.contentHashCode() ?: 0)
        result = 31 * result + operationType.hashCode()
        return result
    }
}
