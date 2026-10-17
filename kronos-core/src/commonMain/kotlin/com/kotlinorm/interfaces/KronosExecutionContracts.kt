/*
 * Copyright 2022-2026 kronos-orm
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

import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.utils.StaticTypeInfo

/**
 * Platform-neutral database identity supplied by an execution adapter.
 *
 * [databaseName] is the adapter's native name (for example a file path or an
 * in-memory database name). It is deliberately not derived from a JDBC URL.
 * JVM adapters may retain [url] and [userName] for compatibility diagnostics;
 * Native adapters can leave them `null`.
 */
data class KronosDatabaseIdentity(
    val dbType: DBType,
    val databaseName: String,
    val driverName: String = "",
    val url: String? = null,
    val userName: String? = null
)

/** Transaction modes understood by platform database adapters. */
enum class KronosTransactionMode {
    /** Let the adapter use its default transaction mode. */
    DEFAULT,
    /** SQLite deferred transaction mode. */
    DEFERRED,
    /** SQLite immediate transaction mode. */
    IMMEDIATE,
    /** SQLite exclusive transaction mode. */
    EXCLUSIVE
}

/** Platform-neutral transaction options. */
data class KronosTransactionOptions(
    val mode: KronosTransactionMode = KronosTransactionMode.DEFAULT,
    val timeoutSeconds: Int? = null
) {
    init {
        require(timeoutSeconds == null || timeoutSeconds >= 0) {
            "Transaction timeout must be non-negative."
        }
    }
}

/** Opaque savepoint handle owned by one transaction scope. */
interface KronosSavepointToken

/**
 * Savepoint operations exposed to a transaction callback.
 *
 * The token type intentionally carries no driver-specific state. A wrapper
 * validates ownership and lifecycle before applying an operation.
 */
interface KronosTransactionScope {
    fun savepoint(name: String? = null): KronosSavepointToken

    fun rollbackToSavepoint(savepoint: KronosSavepointToken)

    fun releaseSavepoint(savepoint: KronosSavepointToken)
}

/** Static mapping metadata supplied by a planner to a row mapper. */
data class KronosResultColumnMetadata(
    val typeInfo: StaticTypeInfo? = null,
    val storage: ValueStorage = ValueStorage.NONE,
    val fieldName: String? = null,
    val columnLabel: String? = null
)

/** Common portion shared by query, action and batch execution plans. */
interface KronosExecutionPlan {
    val sql: String
    val parameters: Map<String, Any?>
    val operationType: KOperationType

    /** Parameter names in positional binding order, when already materialized. */
    val parameterOrder: List<String>
        get() = emptyList()
}

/** Common query plan consumed by a platform execution adapter. */
data class KronosQueryExecutionPlan(
    override val sql: String,
    override val parameters: Map<String, Any?> = emptyMap(),
    override val operationType: KOperationType = KOperationType.SELECT,
    val targetType: StaticTypeInfo? = null,
    val resultColumns: Map<String, KronosResultColumnMetadata> = emptyMap(),
    override val parameterOrder: List<String> = emptyList()
) : KronosExecutionPlan

/** Request for one atomic generated-key capture at the adapter boundary. */
data class KronosGeneratedKeyRequest(
    val fieldName: String,
    val capture: Boolean = true
) {
    init {
        require(fieldName.isNotBlank()) { "Generated key field name must not be blank." }
    }
}

/** Common single-statement mutation plan. */
data class KronosActionExecutionPlan(
    override val sql: String,
    override val parameters: Map<String, Any?> = emptyMap(),
    override val operationType: KOperationType = KOperationType.UPDATE,
    val generatedKey: KronosGeneratedKeyRequest? = null,
    override val parameterOrder: List<String> = emptyList()
) : KronosExecutionPlan

/** Common batch mutation plan. */
data class KronosBatchExecutionPlan(
    override val sql: String,
    val entries: List<Map<String, Any?>> = emptyList(),
    override val operationType: KOperationType = KOperationType.UPDATE,
    val generatedKey: KronosGeneratedKeyRequest? = null,
    override val parameterOrder: List<String> = emptyList()
) : KronosExecutionPlan {
    override val parameters: Map<String, Any?>
        get() = entries.firstOrNull() ?: emptyMap()
}

/** Result of one mutation, including keys captured by the same adapter operation. */
data class KronosExecutionResult(
    val affectedRows: Int = 0,
    val generatedKeys: List<Any?> = emptyList(),
    val lastInsertId: Long? = null
)

/** Presence-aware first-row result for mappers whose result itself may be `null`. */
sealed interface KronosExecutionFirstResult<out T> {
    data object Empty : KronosExecutionFirstResult<Nothing>

    data class Present<T>(val value: T) : KronosExecutionFirstResult<T>
}

/** Mapper used while the platform cursor owns the live physical row. */
typealias KronosPhysicalRowMapper<T> = (KronosPhysicalRow) -> T

/**
 * Common execution boundary implemented by an application-owned platform adapter.
 *
 * Query row mappers run while the adapter's cursor is active. Mutation methods
 * return generated keys captured inside the same statement/connection boundary;
 * callers must not emulate that contract with a second query.
 */
interface KronosExecutionDataSource {
    val identity: KronosDatabaseIdentity

    fun <T> toList(
        plan: KronosQueryExecutionPlan,
        mapper: KronosPhysicalRowMapper<T>
    ): List<T>

    fun <T> first(
        plan: KronosQueryExecutionPlan,
        mapper: KronosPhysicalRowMapper<T>
    ): KronosExecutionFirstResult<T>

    fun execute(plan: KronosActionExecutionPlan): KronosExecutionResult

    fun executeBatch(plan: KronosBatchExecutionPlan): List<KronosExecutionResult>

    fun <T> transact(
        options: KronosTransactionOptions = KronosTransactionOptions(),
        block: KronosTransactionScope.() -> T
    ): T
}
