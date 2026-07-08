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

import java.sql.SQLException
import java.sql.SQLWarning

/**
 * Base unchecked exception used by [KronosJdbcWrapper].
 *
 * It keeps the SQL and positional parameters close to the failure, following the
 * exception translation shape used by mature JDBC layers.
 */
open class KronosJdbcException(
    message: String,
    val sql: String? = null,
    val params: List<Any?> = emptyList(),
    cause: Throwable? = null
) : RuntimeException(buildMessage(message, sql, params), cause)

class KronosBadSqlGrammarException(
    message: String,
    sql: String?,
    params: List<Any?>,
    cause: Throwable
) : KronosJdbcException(message, sql, params, cause)

class KronosDuplicateKeyException(
    message: String,
    sql: String?,
    params: List<Any?>,
    cause: Throwable
) : KronosJdbcException(message, sql, params, cause)

class KronosDataIntegrityViolationException(
    message: String,
    sql: String?,
    params: List<Any?>,
    cause: Throwable
) : KronosJdbcException(message, sql, params, cause)

class KronosConnectionException(
    message: String,
    sql: String?,
    params: List<Any?>,
    cause: Throwable
) : KronosJdbcException(message, sql, params, cause)

class KronosTransientDataAccessException(
    message: String,
    sql: String?,
    params: List<Any?>,
    cause: Throwable
) : KronosJdbcException(message, sql, params, cause)

class KronosUncategorizedSqlException(
    message: String,
    sql: String?,
    params: List<Any?>,
    cause: Throwable
) : KronosJdbcException(message, sql, params, cause)

class KronosSqlWarningException(
    warning: SQLWarning,
    sql: String?,
    params: List<Any?>
) : KronosJdbcException(
    "SQL warning: ${warning.message}",
    sql,
    params,
    warning
)

fun interface KronosSQLExceptionTranslator {
    fun translate(sql: String?, params: List<Any?>, exception: SQLException): KronosJdbcException
}

class SqlStateSQLExceptionTranslator : KronosSQLExceptionTranslator {
    override fun translate(sql: String?, params: List<Any?>, exception: SQLException): KronosJdbcException {
        val state = exception.sqlState.orEmpty()
        val code = exception.errorCode
        val message = exception.message ?: exception.javaClass.name
        return when {
            isDuplicateKey(state, code, message) ->
                KronosDuplicateKeyException(message, sql, params, exception)

            state.startsWith("08") ->
                KronosConnectionException(message, sql, params, exception)

            state.startsWith("22") || state.startsWith("23") ->
                KronosDataIntegrityViolationException(message, sql, params, exception)

            state.startsWith("40") || state.startsWith("HYT") ->
                KronosTransientDataAccessException(message, sql, params, exception)

            state.startsWith("42") ->
                KronosBadSqlGrammarException(message, sql, params, exception)

            else ->
                KronosUncategorizedSqlException(message, sql, params, exception)
        }
    }

    private fun isDuplicateKey(state: String, code: Int, message: String): Boolean {
        if (state == "23505" || state == "23000" && code in duplicateKeyCodes) return true
        if (code in duplicateKeyCodes) return true
        return message.contains("duplicate", ignoreCase = true) ||
            message.contains("unique constraint", ignoreCase = true)
    }

    private companion object {
        private val duplicateKeyCodes = setOf(
            1,      // Oracle ORA-00001
            1062,   // MySQL duplicate entry
            2601,   // SQL Server duplicated index key
            2627,   // SQL Server constraint violation
            23505   // PostgreSQL exposed by some wrapped drivers as vendor code
        )
    }
}

private fun buildMessage(message: String, sql: String?, params: List<Any?>): String {
    if (sql == null) return message
    return buildString {
        append(message)
        append("; SQL [")
        append(sql)
        append(']')
        if (params.isNotEmpty()) {
            append("; params ")
            append(params)
        }
    }
}
