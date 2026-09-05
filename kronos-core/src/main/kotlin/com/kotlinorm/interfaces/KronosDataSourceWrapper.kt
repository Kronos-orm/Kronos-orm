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

package com.kotlinorm.interfaces

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.database.SqlManager
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.exceptions.NoDataSourceException
import com.kotlinorm.syntax.render.SqlDialect

/**
 * Kronos Data Source Wrapper.
 *
 * The Kronos Data Source Wrapper is an interface that provides a set of methods for executing SQL queries and updates
 * against a database. It serves as a bridge between the application and the underlying database, allowing the application
 * to interact with the database in a safe and efficient manner.
 *
 * @see com.kotlinorm.beans.parser.NoneDataSourceWrapper
 */
interface KronosDataSourceWrapper {
    /**
     * The URL for database requests.
     *
     * This URL is used as the endpoint for all database interactions within the application.
     * It is a read-only property, meaning once it is initialized, the value cannot be changed
     * during the application's runtime. This ensures a consistent base URL for all database
     * operations, enhancing security and stability.
     */
    val url: String

    val userName: String

    /**
     * The type of database being used.
     *
     * This property defines the database system that the application will connect to.
     * Being an enum, the possible values are limited to the predefined database types,
     * ensuring that only supported databases can be selected.
     *
     * The database types include:
     * - Mysql
     * - Oracle
     * - Postgres
     * - Mssql
     * - SQLite
     * - DB2
     * - Sybase
     * - H2
     * - OceanBase
     * - DM8
     *
     * This is a read-only property, which prevents changing the database type
     * after it has been set, thereby promoting immutability and consistency within the application.
     */
    val dbType: DBType

    val sqlDialect: SqlDialect
        get() = SqlManager.dialectOf(dbType)

    fun toList(task: KAtomicQueryTask): List<Any?>

    fun first(task: KAtomicQueryTask): Any?

    /**
     * Executes an SQL update operation (such as INSERT, UPDATE, or DELETE) using the provided SQL string and a map of parameters.
     * This method is designed to facilitate the execution of dynamic SQL operations where the specific parameters can vary depending on runtime conditions.
     *
     * @param task The [KAtomicActionTask] that contains the SQL statement to be executed and the parameters to be bound to the update.
     * @return The number of database rows affected by the execution of the SQL statement. This can be used to verify the success and impact of the
     *         update operation, with a return value of 0 indicating that no rows were affected.
     * @throws NoDataSourceException If there is no data source configured for the current environment.
     */
    fun update(task: KAtomicActionTask): Int

    /**
     * Executes a batch of SQL update operations (such as INSERT, UPDATE, or DELETE) using a single SQL string and an array of parameter maps.
     * This method allows for efficient execution of multiple update operations, making it ideal for bulk data modifications.
     *
     * @param task The [KronosAtomicBatchTask] that contains the SQL statement to be executed and the parameters to be bound to the update.
     * @return An array of integers, where each element represents the number of rows affected by the corresponding update operation in the batch.
     *         This can be used to verify the success and impact of each update operation within the batch.
     * @throws NoDataSourceException If there is no data source configured for the current environment.
     */
    fun batchUpdate(task: KronosAtomicBatchTask): IntArray

    /**
     * Executes a block of code within a database transaction.
     *
     * The block is executed as a [TransactionScope] receiver, providing access to savepoint operations.
     * If the block completes successfully, the transaction is committed.
     * If an exception occurs, the transaction is rolled back and the exception is rethrown.
     *
     * @param isolation The transaction isolation level, or `null` to use the connection default.
     * @param timeout The transaction timeout in seconds, or `null` for no timeout.
     * @param block The block of code to execute within the transaction.
     * @return The result of the block execution.
     */
    fun transact(
        isolation: TransactionIsolation? = null,
        timeout: Int? = null,
        block: TransactionScope.() -> Any?
    ): Any?
}
