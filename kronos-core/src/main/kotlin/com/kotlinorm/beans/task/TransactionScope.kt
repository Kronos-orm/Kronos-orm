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

import java.sql.Connection
import java.sql.Savepoint

/**
 * Transaction Scope.
 *
 * Provides the receiver context for transactional blocks, exposing savepoint operations.
 * When used with a JDBC-based wrapper (e.g. [com.kotlinorm.KronosBasicWrapper]),
 * the [connection] is set to the active transaction connection, enabling savepoint management.
 *
 * @property connection The underlying JDBC connection for the transaction, or `null` if not available.
 * @author OUSC
 */
class TransactionScope(internal val connection: Connection? = null) {
    /**
     * Creates a savepoint with the given name in the current transaction.
     *
     * @param name The name of the savepoint.
     * @return The created [Savepoint].
     * @throws UnsupportedOperationException If no JDBC connection is available.
     */
    fun savepoint(name: String): Savepoint =
        requireConnection().setSavepoint(name)

    /**
     * Rolls back the transaction to the specified savepoint.
     *
     * @param savepoint The savepoint to roll back to.
     * @throws UnsupportedOperationException If no JDBC connection is available.
     */
    fun rollbackToSavepoint(savepoint: Savepoint) =
        requireConnection().rollback(savepoint)

    /**
     * Releases the specified savepoint from the current transaction.
     *
     * @param savepoint The savepoint to release.
     * @throws UnsupportedOperationException If no JDBC connection is available.
     */
    fun releaseSavepoint(savepoint: Savepoint) =
        requireConnection().releaseSavepoint(savepoint)

    private fun requireConnection(): Connection =
        connection ?: throw UnsupportedOperationException(
            "Savepoint operations require a JDBC-based KronosDataSourceWrapper (e.g. KronosBasicWrapper)"
        )
}
