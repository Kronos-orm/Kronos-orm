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

package com.kotlinorm

import com.kotlinorm.Kronos.strictSetValue
import com.kotlinorm.beans.UnsupportedTypeException
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.Oracle
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.createInstance
import com.kotlinorm.utils.getTypeSafeValue
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.reflect.KClass

/**
 * Kronos Basic Jdbc Wrapper
 *
 * A wrapper class for JDBC data sources that implements the `KronosDataSourceWrapper` interface.
 * @author: OUSC
 **/
class KronosBasicWrapper(val dataSource: DataSource) : KronosDataSourceWrapper {
    override val url: String
    override val userName: String
    override val dbType: DBType

    init {
        dataSource.connection.use { conn ->
            url = conn.metaData.url
            dbType = DBType.fromName(conn.metaData.databaseProductName)
            userName = conn.metaData.userName ?: ""
        }
    }

    /**
     * Executes a query task and returns the result as a list of maps.
     *
     * @param task The query task to be executed.
     * @return A list of maps where each map represents a row in the result set,
     *         with column names as keys and their corresponding values as map values.
     */
    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> =
        executeQuery(task) { toMapList() }

    /**
     * Executes a query task and returns the result as a list of objects.
     *
     * @param task The query task to be executed.
     * @param kClass The Kotlin class of the target object type.
     * @param isKPojo A flag indicating whether the target type is a KPojo.
     * @param superTypes A list of super types for the target object type.
     * @return A list of objects of the specified type.
     *         If `isKPojo` is true, the objects are created as KPojo instances.
     *         Otherwise, they are created as plain objects.
     */
    @Suppress("UNCHECKED_CAST")
    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> = executeQuery(task) {
        if (isKPojo) toKPojoList(kClass as KClass<KPojo>)
        else toObjectList(kClass)
    }

    /**
     * Executes a query task and returns the result as a map.
     *
     * @param task The query task to be executed.
     * @return A map representing the first row in the result set,
     *         with column names as keys and their corresponding values as map values,
     *         or `null` if the result set is empty.
     */
    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? =
        executeQuery(task) { toMapList().firstOrNull() }

    /**
     * Executes a query task and returns the result as a single object.
     *
     * @param task The query task to be executed.
     * @param kClass The Kotlin class of the target object type.
     * @param isKPojo A flag indicating whether the target type is a KPojo.
     * @param superTypes A list of super types for the target object type.
     * @return An object of the specified type, or `null` if the result set is empty.
     *         If `isKPojo` is true, the object is created as a KPojo instance.
     *         Otherwise, it is created as a plain object.
     * @throws UnsupportedTypeException If the target type is a KPojo and its fields cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): Any? = executeQuery(task) {
        if (isKPojo) toKPojoList(kClass as KClass<KPojo>).firstOrNull()
        else singleObject(kClass)
    }

    /**
     * Executes an update task and returns the number of rows affected.
     *
     * @param task The update task to be executed.
     * @return The number of rows affected by the update operation.
     */
    override fun update(task: KAtomicActionTask): Int = executeUpdate(task)

    /**
     * Executes a batch update task and returns an array of update counts.
     *
     * @param task The batch update task to be executed.
     * @return An array of integers, where each element represents the number of rows
     *         affected by each batch operation.
     */
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = executeBatchUpdate(task)

    /**
    * Executes a transactional block of code within a database connection.
    *
    * The method ensures that the block of code is executed within a transaction.
    * If the block completes successfully, the transaction is committed. If an
    * exception occurs, the transaction is rolled back and the exception is rethrown.
    *
    * @param block The block of code to be executed within the transaction.
    * @return The result of the block execution, or `null` if the block returns `null`.
    * @throws Exception If an exception occurs during the execution of the block,
    *                   it is caught, the transaction is rolled back, and the exception is rethrown.
    */
   override fun transact(block: () -> Any?): Any? = dataSource.connection.use { conn ->
       conn.autoCommit = false
       var committed = false
       try {
           val result = block()
           conn.commit()
           committed = true
           result
       } finally {
           if (!committed) {
               try {
                   conn.rollback()
               } catch (_: java.sql.SQLException) {
                   // ignore rollback failure
               }
           }
       }
   }

    /**
     * Executes a query task and processes the result set using a custom result handler.
     *
     * @param T The type of the result produced by the result handler.
     * @param task The query task to be executed, containing the SQL statement and parameters.
     * @param resultHandler A lambda function that processes the `ResultSet` and produces a result of type `T`.
     * @return The result produced by the `resultHandler` after processing the `ResultSet`.
     */
    private inline fun <T> executeQuery(
        task: KAtomicQueryTask,
        resultHandler: ResultSet.() -> T
    ): T {
        val (sql, params) = task.parsed()
        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql, oracleOptions()).use { ps ->
                ps.setParameters(params)
                ps.executeQuery().use { rs ->
                    resultHandler(rs).also {
                        if (dbType == Oracle.type) rs.beforeFirst()
                    }
                }
            }
        }
    }

    /**
     * Executes an update task and returns the number of rows affected.
     *
     * @param task The update task to be executed, containing the SQL statement and parameters.
     * @return The number of rows affected by the update operation.
     */
    private fun executeUpdate(task: KAtomicActionTask): Int {
        val (sql, params) = task.parsed()
        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setParameters(params)
                ps.executeUpdate()
            }
        }
    }

    /**
     * Executes a batch update task and returns an array of update counts.
     *
     * @param task The batch update task to be executed, containing the SQL statement and a list of parameter arrays.
     * @return An array of integers, where each element represents the number of rows
     *         affected by each batch operation.
     */
    private fun executeBatchUpdate(task: KronosAtomicBatchTask): IntArray {
        val (sql, paramList) = task.parsedArr()
        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                paramList.forEach { params ->
                    ps.setParameters(params)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    /**
     * Sets the parameters for a `PreparedStatement` using the provided array of values.
     *
     * @param params An array of parameter values to be set in the `PreparedStatement`.
     *               Each value in the array corresponds to a parameter in the SQL statement,
     *               with the index in the array matching the parameter index (1-based).
     */
    private fun PreparedStatement.setParameters(params: Array<Any?>) {
        var i = 0 // JDBC parameters are 1-based
        while (i < params.size) {
            when (val value = params[i]) {
                null -> setNull(++i, java.sql.Types.NULL)
                is String -> setString(++i, value)
                else -> setObject(++i, value)
            }
        }
    }

    /**
     * Provides Oracle-specific options for `PreparedStatement`.
     *
     * @return An array of integers representing the Oracle-specific options:
     *         - `ResultSet.TYPE_SCROLL_INSENSITIVE`: Allows scrolling through the result set in a non-sensitive manner.
     *         - `ResultSet.CONCUR_READ_ONLY`: Indicates that the result set is read-only.
     *         If the database type is not Oracle, returns an empty array.
     */
    private fun oracleOptions() = if (dbType == Oracle.type) {
        intArrayOf(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
    } else intArrayOf()

    /**
     * Converts the current `ResultSet` into a list of maps.
     *
     * Each map represents a row in the result set, where the keys are column names
     * and the values are the corresponding column values.
     *
     * @return A list of maps, with each map representing a row in the result set.
     *         If the database type is Oracle, the result set is processed using
     *         `handleOracleResultSet` to handle Oracle-specific behavior.
     */
    private fun ResultSet.toMapList(): List<Map<String, Any>> {
        return if (dbType == Oracle.type) handleOracleResultSet { createMapFromResultSet() }
        else generateSequence { if (next()) createMapFromResultSet() else null }.toList()
    }

    /**
     * Converts the current `ResultSet` into a list of `KPojo` objects of the specified class.
     *
     * This method retrieves the column mappings for the given `KPojo` class from the cache.
     * If the database type is Oracle, it processes the `ResultSet` using `handleOracleResultSet`
     * to handle Oracle-specific behavior. Otherwise, it generates a sequence of `KPojo` objects
     * by iterating through the `ResultSet`.
     *
     * @param kClass The Kotlin class of the `KPojo` type to be created.
     * @return A list of `KPojo` objects created from the `ResultSet`.
     * @throws UnsupportedTypeException If the column mappings for the specified `KPojo` class
     *                                  cannot be found in the cache.
     */
    private fun ResultSet.toKPojoList(kClass: KClass<KPojo>): List<KPojo> {
        val columns =
            fieldsMapCache[kClass] ?: throw UnsupportedTypeException("Cannot find fields in ${kClass.simpleName}")
        return if (dbType == Oracle.type) handleOracleResultSet { createKPojo(kClass, columns) }
        else generateSequence { if (next()) createKPojo(kClass, columns) else null }.toList()
    }

    /**
     * Converts the current `ResultSet` into a list of objects of the specified class.
     *
     * This method iterates through the `ResultSet` and creates objects of the specified class
     * using the `getObjectValue` method. Each object corresponds to a row in the result set.
     *
     * @param kClass The Kotlin class of the target object type.
     * @return A list of objects of the specified type, where each object represents a row in the result set.
     */
    private fun ResultSet.toObjectList(kClass: KClass<*>): List<Any> =
        generateSequence { if (next()) getObjectValue(1, kClass) else null }.toList()

    /**
     * Retrieves a single object from the current `ResultSet`.
     *
     * This method moves the cursor to the next row in the `ResultSet` and retrieves
     * the value of the first column, converting it into an object of the specified class.
     *
     * @param kClass The Kotlin class of the target object type.
     * @return An object of the specified type if the `ResultSet` has a next row, or `null` if the `ResultSet` is empty.
     */
    private fun ResultSet.singleObject(kClass: KClass<*>): Any? =
        if (next()) getObjectValue(1, kClass) else null

    /**
     * Processes an Oracle-specific `ResultSet` to handle `LONG` column types and returns a list of items.
     *
     * This method addresses the Oracle-specific issue where `LONG` column streams may be prematurely closed.
     * It first processes rows with `LONG` columns, then reprocesses them to handle other column types.
     *
     * @param T The type of items to be created from the `ResultSet`.
     * @param createItem A lambda function that creates an item of type `T` from a set of column indices.
     *                   The set of column indices indicates which columns to process.
     * @return A list of items of type `T` created from the `ResultSet`.
     */
    private inline fun <T> ResultSet.handleOracleResultSet(crossinline createItem: (Set<Int>) -> T): List<T> {
        // fix for Oracle: ORA 17027 Stream has already been closed
        val longColumns = (1..metaData.columnCount)
            .filter { metaData.getColumnTypeName(it).equals("LONG", true) }
            .toSet()

        val items = mutableListOf<T>()
        while (next()) {
            items.add(createItem(longColumns))
        }
        beforeFirst()

        var index = 0
        while (next()) {
            items[index] = createItem((1..metaData.columnCount).toSet() - longColumns)
            index++
        }
        return items
    }

    /**
     * Creates a map from the current row of the `ResultSet`.
     *
     * This method iterates through all columns in the current row of the `ResultSet`,
     * using the column labels as keys and their corresponding values as map values.
     * Null values are filtered out from the resulting map.
     *
     * @return A map where the keys are column labels and the values are the corresponding
     *         non-null column values from the current row of the `ResultSet`.
     */
    private fun ResultSet.createMapFromResultSet(): Map<String, Any> {
        return (1..metaData.columnCount).associate { i ->
            metaData.getColumnLabel(i) to getObject(i)
        }.filterValues { it != null }
    }

    /**
     * Creates a `KPojo` instance from the current row of the `ResultSet`.
     *
     * This method iterates through all columns in the current row of the `ResultSet`,
     * mapping column labels to their corresponding fields in the `KPojo` class.
     * The field values are set using either strict or type-safe value assignment,
     * depending on the `strictSetValue` flag.
     *
     * @param kClass The Kotlin class of the `KPojo` type to be created.
     * @param columns A map of column labels to `Field` objects, representing the fields
     *                of the `KPojo` class and their metadata.
     * @return A `KPojo` instance populated with values from the current row of the `ResultSet`.
     */
    private fun ResultSet.createKPojo(kClass: KClass<KPojo>, columns: Map<String, Field>): KPojo {
        return kClass.createInstance().apply {
            for (i in 1..metaData.columnCount) {
                val label = metaData.getColumnLabel(i)
                columns[label]?.let { field ->
                    this[label] = getObjectValue(
                        i, field.kClass!!, field.superTypes
                    )
                }
            }
        }
    }

    /**
     * Retrieves an object from the current `ResultSet` at the specified position.
     *
     * This method retrieves the value at the given column position in the `ResultSet`
     * and converts it into an object of the specified class. The conversion behavior
     * depends on the `strictSetValue` flag:
     * - If `strictSetValue` is `true`, the value is retrieved using the `getObject` method
     *   with the target class.
     * - If `strictSetValue` is `false`, the value is retrieved using the `getTypeSafeValue` method,
     *   which performs type-safe conversion based on the class name and super types.
     *
     * @param position The 1-based column index in the `ResultSet` to retrieve the value from.
     * @param kClass The Kotlin class of the target object type.
     * @return The value at the specified column position, converted to the specified type.
     */
    private fun ResultSet.getObjectValue(position: Int, kClass: KClass<*>, superTypes: List<String> = listOf()): Any? =
        if (strictSetValue) {
            getObject(position, kClass.java)
        } else {
            getObject(position)?.let {
                getTypeSafeValue(kClass.qualifiedName!!, it, superTypes)
            }
        }
}