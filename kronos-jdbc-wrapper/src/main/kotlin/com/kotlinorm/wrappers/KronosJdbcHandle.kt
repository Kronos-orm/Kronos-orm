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

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLWarning
import java.sql.Statement

/**
 * Per-operation connection handle backed by a snapshot of wrapper configuration.
 *
 * The handle closes its connection only when it owns that connection; transaction-bound
 * handles leave the shared connection open. Statement timeout is the smaller of the configured
 * query timeout and the remaining transaction deadline.
 */
internal class KronosJdbcHandle(
    val connection: Connection,
    val config: KronosJdbcConfig,
    private val shouldClose: Boolean,
    private val transactionDeadlineMillis: Long?
) : AutoCloseable {
    /**
     * Creates the statement context shared by binding, execution hooks, and result reading.
     *
     * [params] and [parameterNames] must have the same positional order. [stash] is retained
     * by reference so operation-local metadata is visible at the JDBC boundary.
     */
    fun context(
        originalSql: String,
        jdbcSql: String,
        params: Array<Any?>,
        parameterNames: List<String>,
        operationType: com.kotlinorm.enums.KOperationType,
        stash: MutableMap<String, Any?>? = null
    ): KronosStatementContext =
        KronosStatementContext(
            originalSql = originalSql,
            jdbcSql = jdbcSql,
            params = params.toList(),
            parameterNames = parameterNames,
            operationType = operationType,
            dbType = config.dbType,
            databaseProductName = config.databaseProductName,
            config = config,
            stash = stash,
            transactionDeadlineMillis = transactionDeadlineMillis
        )

    /**
     * Prepares and configures one statement for [context].
     *
     * Generated-key statements use JDBC's generated-key overload; other statements honor the
     * configured result-set type, concurrency, and optional holdability.
     */
    fun prepareStatement(
        context: KronosStatementContext,
        returnGeneratedKeys: Boolean,
        generatedKeyColumn: String? = null
    ): PreparedStatement {
        val statement = if (returnGeneratedKeys) {
            generatedKeyColumn?.let { column ->
                connection.prepareStatement(context.jdbcSql, arrayOf(column))
            } ?: connection.prepareStatement(context.jdbcSql, Statement.RETURN_GENERATED_KEYS)
        } else {
            val holdability = config.resultSet.holdability
            if (holdability != null) {
                connection.prepareStatement(
                    context.jdbcSql,
                    config.resultSet.type,
                    config.resultSet.concurrency,
                    holdability
                )
            } else if (config.resultSet.type != ResultSet.TYPE_FORWARD_ONLY ||
                config.resultSet.concurrency != ResultSet.CONCUR_READ_ONLY
            ) {
                connection.prepareStatement(context.jdbcSql, config.resultSet.type, config.resultSet.concurrency)
            } else {
                connection.prepareStatement(context.jdbcSql)
            }
        }
        configureStatement(statement, context)
        return statement
    }

    /**
     * Appends an entire JDBC warning chain and enforces the configured warning policy.
     *
     * @throws KronosSqlWarningException when warning policy is `THROW` and any warning exists
     */
    fun collectWarnings(context: KronosStatementContext, warning: SQLWarning?) {
        var current = warning
        while (current != null) {
            context.warnings.add(current)
            current = current.nextWarning
        }
        if (config.warningPolicy == KronosSqlWarningPolicy.THROW && context.warnings.isNotEmpty()) {
            throw KronosSqlWarningException(context.warnings.first(), context.jdbcSql, context.params)
        }
    }

    override fun close() {
        if (shouldClose) connection.close()
    }

    private fun configureStatement(statement: PreparedStatement, context: KronosStatementContext) {
        config.statement.fetchSize?.let { statement.fetchSize = it }
        config.statement.maxRows?.let { statement.maxRows = it }
        queryTimeout(context)?.let { statement.queryTimeout = it }
        config.statement.poolable?.let { statement.isPoolable = it }
        config.statement.escapeProcessing?.let { statement.setEscapeProcessing(it) }
        if (config.statement.closeOnCompletion) {
            runCatching { statement.closeOnCompletion() }
        }
    }

    private fun queryTimeout(context: KronosStatementContext): Int? {
        val configured = config.statement.queryTimeoutSeconds
        val transaction = context.remainingTransactionTimeoutSeconds()
        return when {
            configured == null -> transaction
            transaction == null -> configured
            else -> minOf(configured, transaction)
        }
    }
}
