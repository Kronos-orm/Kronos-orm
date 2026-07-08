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
    val attributes: MutableMap<String, Any?> = mutableMapOf()
    val generatedKeys: MutableList<Any?> = mutableListOf()
    val warnings: MutableList<SQLWarning> = mutableListOf()
    var affectedRows: Int? = null
    var returnedRows: Int? = null
    var elapsedNanos: Long = 0L

    fun remainingTransactionTimeoutSeconds(nowMillis: Long = System.currentTimeMillis()): Int? {
        val deadline = transactionDeadlineMillis ?: return null
        val remainingMillis = deadline - nowMillis
        if (remainingMillis <= 0) return 1
        return ((remainingMillis + 999L) / 1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
