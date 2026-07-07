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
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.plugins.LastInsertIdPlugin
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.reflect.KClass

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

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> =
        query(task) { resultSet, context ->
            KronosResultMappers.toMapList(resultSet, context)
        }

    @Suppress("UNCHECKED_CAST")
    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> =
        query(task) { resultSet, context ->
            if (isKPojo) {
                KronosResultMappers.toKPojoList(resultSet, kClass as KClass<out KPojo>, context)
            } else {
                KronosResultMappers.toObjectList(resultSet, kClass, superTypes, context)
            }
        }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? =
        forList(task).firstOrNull()

    @Suppress("UNCHECKED_CAST")
    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): Any? =
        query(task) { resultSet, context ->
            if (isKPojo) {
                KronosResultMappers.toKPojoList(resultSet, kClass as KClass<out KPojo>, context).firstOrNull()
            } else {
                KronosResultMappers.toObjectList(resultSet, kClass, superTypes, context).firstOrNull()
            }
        }

    override fun update(task: KAtomicActionTask): Int {
        val parsed = task.parsed()
        return withHandle { handle ->
            val context = handle.context(
                originalSql = parsed.originalSql,
                jdbcSql = parsed.jdbcSql,
                params = parsed.jdbcParamList,
                parameterNames = parsed.parameterNames,
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

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
        val (jdbcSql, paramLists) = task.parsedArr()
        if (paramLists.isEmpty()) return IntArray(0)
        val sql = jdbcSql ?: task.sql
        return withHandle { handle ->
            val context = handle.context(
                originalSql = task.sql,
                jdbcSql = sql,
                params = emptyArray(),
                parameterNames = emptyList(),
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
                parameterNames = parsed.parameterNames,
                operationType = task.operationType
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
            task.stash["useIdentity"] == true &&
            (task.stash["queryId"] == true || task.stash["queryId"] == null && LastInsertIdPlugin.enabled)

    private fun readGeneratedKeys(
        statement: PreparedStatement,
        context: KronosStatementContext,
        task: KAtomicActionTask
    ) {
        statement.generatedKeys.use { keys ->
            while (keys.next()) {
                context.generatedKeys.add(context.config.columnMappers.map(keys, 1, Any::class, emptyList(), context))
            }
        }
        if (context.generatedKeys.isEmpty()) return

        task.stash["generatedKeys"] = context.generatedKeys.toList()
        val firstKey = context.generatedKeys.first()
        val lastInsertId = when (firstKey) {
            is Number -> firstKey.toLong()
            else -> firstKey?.toString()?.toLongOrNull()
        }
        if (lastInsertId != null) {
            task.stash["lastInsertId"] = lastInsertId
            task.stash["queryId"] = false
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
