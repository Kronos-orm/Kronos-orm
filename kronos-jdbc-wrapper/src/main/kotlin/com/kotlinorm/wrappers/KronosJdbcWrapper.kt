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

import com.kotlinorm.beans.parser.ParsedSql
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

/**
 * JDBC implementation of the Kronos data-source wrapper.
 *
 * A wrapper owns immutable database identity and mutable configuration, snapshots that
 * configuration for each statement handle, binds parsed parameters, and maps result rows
 * through physical readers followed by the single logical `ValueCodec` decode boundary.
 * Connections are opened per operation unless [transact] establishes a thread-local
 * transaction; nested transactions reuse the active connection.
 *
 * @param dataSource JDBC data source used to obtain connections
 * @param databaseType optional explicit dialect; when `null`, the database product and URL are detected
 * @param configure configuration block applied after automatic database-plugin customization
 */
class KronosJdbcWrapper @JvmOverloads constructor(
    val dataSource: DataSource,
    databaseType: DBType? = null,
    configure: KronosJdbcConfig.() -> Unit = {}
) : KronosDataSourceWrapper {
    override val url: String
    override val userName: String
    override val dbType: DBType
    val identity: KronosDatabaseIdentity
    val config: KronosJdbcConfig

    private val transactionContext = ThreadLocal<KronosJdbcTransaction?>()

    init {
        dataSource.connection.use { connection ->
            val metaData = connection.metaData
            val productName = metaData.databaseProductName.orEmpty()
            val detectedType = databaseType ?: KronosJdbcPlugins.detectDbType(productName, metaData.url.orEmpty())
            url = metaData.url.orEmpty()
            userName = metaData.userName.orEmpty()
            dbType = detectedType
            identity = KronosDatabaseIdentity(
                dbType = detectedType,
                databaseProductName = productName,
                url = url,
                userName = userName,
                driverName = metaData.driverName.orEmpty()
            )
            config = KronosJdbcConfig(detectedType, productName, url, identity.driverName).also { cfg ->
                KronosJdbcPlugins.autoPlugin(detectedType)?.install(cfg)
                cfg.configure()
            }
        }
    }

    /**
     * Executes a query and decodes all rows according to the task's complete target type.
     *
     * @return decoded rows in result-set order; an empty list when no rows are returned
     */
    override fun toList(task: KAtomicQueryTask): List<Any?> =
        query(task) { resultSet, context ->
            KronosResultMappers.toList(resultSet, task, context)
        }

    /**
     * Executes a query and returns its first decoded row.
     *
     * @return first row, or `null` when the result set is empty
     */
    override fun first(task: KAtomicQueryTask): Any? =
        toList(task).firstOrNull()

    /**
     * Executes one mutation task and optionally reads generated keys requested by the task.
     *
     * @return JDBC-reported affected-row count
     */
    override fun update(task: KAtomicActionTask): Int {
        val parsed = task.parsed()
        return withHandle { handle ->
            val context = handle.context(
                originalSql = parsed.originalSql,
                jdbcSql = parsed.jdbcSql,
                params = parsed.jdbcParamList,
                parameterNames = parsed.jdbcParameterNames,
                operationType = task.operationType,
                stash = task.stash
            )
            executeWithTranslation(context) {
                val returnGeneratedKeys = shouldReturnGeneratedKeys(task)
                handle.prepareStatement(context, returnGeneratedKeys).use { statement ->
                    context.config.arguments.bind(statement, parsed.jdbcParamList, context)
                    val start = System.nanoTime()
                    val affected = statement.executeUpdate()
                    context.elapsedNanos = System.nanoTime() - start
                    context.affectedRows = affected
                    if (returnGeneratedKeys) {
                        readGeneratedKeys(statement, context, task)
                    }
                    handle.collectWarnings(context, statement.warnings)
                    statement.clearWarnings()
                    affected
                }
            }
        }
    }

    /**
     * Executes all parameter maps in a batch task using one prepared statement.
     *
     * @return one JDBC update count per batch entry; empty when the task has no entries
     */
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
        val parsedSqls = task.parsedSqlArr()
        val jdbcSql = parsedSqls.firstOrNull()?.jdbcSql
        val paramLists = parsedSqls.map { it.jdbcParamList }
        if (paramLists.isEmpty()) return IntArray(0)
        val sql = jdbcSql ?: task.sql
        return withHandle { handle ->
            val context = handle.context(
                originalSql = task.sql,
                jdbcSql = sql,
                params = emptyArray(),
                parameterNames = parsedSqls.first().jdbcParameterNames,
                operationType = task.operationType,
                stash = task.stash
            )
            context.attributes["batchSize"] = paramLists.size
            executeWithTranslation(context) {
                handle.prepareStatement(context, returnGeneratedKeys = false).use { statement ->
                    paramLists.forEach { params ->
                        context.config.arguments.bind(statement, params, context)
                        statement.addBatch()
                    }
                    val start = System.nanoTime()
                    val counts = statement.executeBatch()
                    context.elapsedNanos = System.nanoTime() - start
                    context.affectedRows = counts.sumOf { if (it > 0) it else 0 }
                    handle.collectWarnings(context, statement.warnings)
                    statement.clearWarnings()
                    counts
                }
            }
        }
    }

    /**
     * Runs [block] inside a transaction with optional isolation and timeout settings.
     *
     * A successful block is committed. Any thrown exception triggers rollback, with rollback
     * failures attached as suppressed exceptions; the original exception is then rethrown.
     * Nested calls reuse the active transaction and do not commit independently.
     *
     * @param isolation isolation level to apply for the transaction, or `null` to preserve the connection default
     * @param timeout transaction timeout in seconds, or `null` for no wrapper deadline
     * @param block transaction-scoped operations
     * @return the value returned by [block]
     */
    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?
    ): Any? {
        transactionContext.get()?.let { active ->
            return TransactionScope(active.connection).block()
        }

        val connection = dataSource.connection
        val originalAutoCommit = connection.autoCommit
        val originalIsolation = connection.transactionIsolation
        val deadline = timeout?.let { System.currentTimeMillis() + it * 1000L }
        transactionContext.set(KronosJdbcTransaction(connection, deadline))
        try {
            connection.autoCommit = false
            if (isolation != null) connection.transactionIsolation = isolation.level
            val result = TransactionScope(connection).block()
            connection.commit()
            return result
        } catch (throwable: Throwable) {
            runCatching { connection.rollback() }.exceptionOrNull()?.let { throwable.addSuppressed(it) }
            throw throwable
        } finally {
            transactionContext.remove()
            runCatching {
                if (isolation != null) connection.transactionIsolation = originalIsolation
                connection.autoCommit = originalAutoCommit
            }
            runCatching { connection.close() }
        }
    }

    private fun <T> query(task: KAtomicQueryTask, mapper: (ResultSet, KronosStatementContext) -> T): T {
        val parsed = task.parsed()
        return withHandle { handle ->
            val context = handle.context(
                originalSql = parsed.originalSql,
                jdbcSql = parsed.jdbcSql,
                params = parsed.jdbcParamList,
                parameterNames = parsed.jdbcParameterNames,
                operationType = task.operationType,
                stash = task.stash
            )
            executeWithTranslation(context) {
                handle.prepareStatement(context, returnGeneratedKeys = false).use { statement ->
                    context.config.arguments.bind(statement, parsed.jdbcParamList, context)
                    val start = System.nanoTime()
                    statement.executeQuery().use { resultSet ->
                        val value = mapper(resultSet, context)
                        handle.collectWarnings(context, resultSet.warnings)
                        resultSet.clearWarnings()
                        context.elapsedNanos = System.nanoTime() - start
                        handle.collectWarnings(context, statement.warnings)
                        statement.clearWarnings()
                        value
                    }
                }
            }
        }
    }

    private inline fun <T> withHandle(block: (KronosJdbcHandle) -> T): T {
        val activeTransaction = transactionContext.get()
        val connection = activeTransaction?.connection ?: dataSource.connection
        val handle = KronosJdbcHandle(
            connection = connection,
            config = config.snapshot(),
            shouldClose = activeTransaction == null,
            transactionDeadlineMillis = activeTransaction?.deadlineMillis
        )
        return handle.use(block)
    }

    private inline fun <T> executeWithTranslation(context: KronosStatementContext, block: () -> T): T {
        return try {
            block()
        } catch (exception: SQLException) {
            throw context.config.exceptionTranslator.translate(context.jdbcSql, context.params, exception)
        }
    }

    private fun shouldReturnGeneratedKeys(task: KAtomicActionTask): Boolean =
        task.operationType == KOperationType.INSERT &&
            task.generatedKeyField != null

    private fun readGeneratedKeys(
        statement: PreparedStatement,
        context: KronosStatementContext,
        task: KAtomicActionTask
    ) {
        statement.generatedKeys.use { keys ->
            while (keys.next()) {
                context.generatedKeys.add(context.config.columnMappers.readJdbcValue(keys, 1, context))
            }
        }
        if (context.generatedKeys.isEmpty()) return

        task.generatedKeys.clear()
        task.generatedKeys.addAll(context.generatedKeys)
        val firstKey = context.generatedKeys.first()
        val lastInsertId = when (firstKey) {
            is Number -> firstKey.toLong()
            else -> firstKey?.toString()?.toLongOrNull()
        }
        if (lastInsertId != null) {
            task.lastInsertId = lastInsertId
        }
    }

    private fun KronosJdbcPlugin.install(config: KronosJdbcConfig) {
        customize(config)
        config.loadedPlugins.add(name)
    }

    private data class KronosJdbcTransaction(
        val connection: Connection,
        val deadlineMillis: Long?
    )
}
