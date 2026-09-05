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

package com.kotlinorm.wrappers

import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import java.sql.SQLWarning

/**
 * Per-execution state shared by parameter binders, JDBC plugins, result readers, and diagnostics.
 *
 * [params] and [parameterNames] use the same positional order and must have equal size for
 * named-parameter executions. Expanded list parameters therefore repeat their materialized name
 * once per JDBC value. [stash] is the originating task's operation-local metadata and may be
 * `null` for callers that do not support it.
 *
 * @property originalSql named-parameter SQL before JDBC materialization
 * @property jdbcSql positional SQL sent to the driver
 * @property params materialized JDBC values in binding order
 * @property parameterNames materialized source names aligned one-to-one with [params]
 * @property operationType operation classification used by plugins and diagnostics
 * @property dbType configured database dialect
 * @property databaseProductName product name reported by JDBC metadata
 * @property config immutable-for-this-operation configuration snapshot
 * @property stash optional task metadata retained by reference
 * @property transactionDeadlineMillis absolute transaction deadline, or `null` when unlimited
 */
class KronosStatementContext(
    val originalSql: String,
    val jdbcSql: String,
    val params: List<Any?>,
    val parameterNames: List<String>,
    val operationType: KOperationType,
    val dbType: DBType,
    val databaseProductName: String,
    val config: KronosJdbcConfig,
    val stash: MutableMap<String, Any?>? = null,
    val transactionDeadlineMillis: Long? = null
) {
    /** Extension state scoped to this execution and not copied back into the task stash. */
    val attributes: MutableMap<String, Any?> = mutableMapOf()

    /** Raw generated keys collected from the JDBC statement in driver order. */
    val generatedKeys: MutableList<Any?> = mutableListOf()

    /** Complete JDBC warning chains collected from result sets and statements. */
    val warnings: MutableList<SQLWarning> = mutableListOf()

    /** JDBC-reported affected-row count, or `null` until an action completes. */
    var affectedRows: Int? = null

    /** Number of rows emitted by result mapping, or `null` until a query completes. */
    var returnedRows: Int? = null

    /** Measured JDBC execution and mapping duration in nanoseconds. */
    var elapsedNanos: Long = 0L

    /**
     * Computes a positive JDBC query timeout from the transaction deadline.
     *
     * The value rounds partial seconds up. An expired deadline returns one second because JDBC
     * uses zero to mean no timeout, which would accidentally disable enforcement.
     *
     * @param nowMillis clock value used to evaluate the deadline
     * @return remaining whole seconds rounded up, or `null` when no deadline exists
     */
    fun remainingTransactionTimeoutSeconds(nowMillis: Long = System.currentTimeMillis()): Int? {
        val deadline = transactionDeadlineMillis ?: return null
        val remainingMillis = deadline - nowMillis
        if (remainingMillis <= 0) return 1
        return ((remainingMillis + 999L) / 1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
