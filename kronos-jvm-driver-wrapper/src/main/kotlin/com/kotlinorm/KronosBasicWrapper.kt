/**
 * Copyright 2022-2024 kronos-orm
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

import com.kotlinorm.Kronos.defaultLogger
import com.kotlinorm.Kronos.strictSetValue
import com.kotlinorm.beans.UnsupportedTypeException
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.logging.KLogMessage.Companion.kMsgOf
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.enums.ColorPrintCode.Companion.Red
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.Oracle
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.Extensions.safeMapperTo
import com.kotlinorm.utils.getTypeSafeValue
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

/**
 * Kronos Basic Jdbc Wrapper
 *
 * A wrapper class for JDBC data sources that implements the `KronosDataSourceWrapper` interface.
 * @author: OUSC
 **/
class KronosBasicWrapper(private val dataSource: DataSource) : KronosDataSourceWrapper {
    private var _metaUrl: String
    private var _metaDbType: DBType

    override val url: String
        get() = _metaUrl

    override val dbType: DBType
        get() = _metaDbType

    init {
        val conn = dataSource.connection
        _metaUrl = conn.metaData.url
        _metaDbType = DBType.fromName(conn.metaData.databaseProductName)
            ?: throw UnsupportedTypeException("Unsupported database type [${conn.metaData.databaseProductName}].")
        conn.close()
    }

    /**
     * Retrieves a list of maps from the database based on the provided task.
     *
     * @param task the [KAtomicQueryTask] containing the SQL query and parameters
     * @return a list of maps representing the rows returned by the query
     * @throws [SQLException] if an error occurs while executing the query
     */
    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
        val (sql, paramList) = task.parsed()
        val conn = dataSource.connection
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            ps = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
            paramList.forEachIndexed { index, any ->
                ps.setObject(index + 1, any)
            }
            rs = ps.executeQuery()
            return rs.toList()
        } catch (e: SQLException) {
            defaultLogger(this).error(
                kMsgOf(
                    "Failed to execute query，${e.message}.", Red
                ).endl().toArray()
            )
            throw e
        } finally {
            rs?.close()
            ps?.close()
            conn.close()
        }
    }

    /**
     * Retrieves a list of objects of the specified class from the database based on the given task.
     *
     * @param task the [KAtomicQueryTask] containing the SQL query and parameters
     * @param kClass the class of the objects to retrieve
     * @return a list of objects of the specified class
     * @throws [SQLException] if an error occurs while executing the query
     */
    override fun forList(task: KAtomicQueryTask, kClass: KClass<*>): List<Any> {
        return if (KPojo::class.isSuperclassOf(kClass)) {
            @Suppress("UNCHECKED_CAST") forList(task).map { it.safeMapperTo(kClass as KClass<KPojo>) }
        } else {
            val (sql, paramList) = task.parsed()
            val conn = dataSource.connection
            var ps: PreparedStatement? = null
            var rs: ResultSet? = null
            try {
                ps = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
                paramList.forEachIndexed { index, any ->
                    ps.setObject(index + 1, any)
                }
                rs = ps.executeQuery()
                return rs.toList(kClass.java)
            } catch (e: SQLException) {
                defaultLogger(this).error(
                    kMsgOf(
                        "Failed to execute query，${e.message}.", Red
                    ).endl().toArray()
                )
                throw e
            } finally {
                rs?.close()
                ps?.close()
                conn.close()
            }
        }
    }

    /**
     * Retrieves a single row from the database as a map of column names to values.
     *
     * @param task the [KAtomicQueryTask] containing the SQL query and parameters
     * @return a map of column names to values, or null if no rows are returned
     * @throws [SQLException] if an error occurs while executing the query
     */
    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? {
        val (sql, paramList) = task.parsed()
        val conn = dataSource.connection
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            ps = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
            paramList.forEachIndexed { index, any ->
                ps.setObject(index + 1, any)
            }
            rs = ps.executeQuery()
            return rs.toList().firstOrNull()
        } catch (e: SQLException) {
            defaultLogger(this).error(
                kMsgOf(
                    "Failed to execute query，${e.message}.", Red
                ).endl().toArray()
            )
            throw e
        } finally {
            rs?.close()
            ps?.close()
            conn.close()
        }
    }

    /**
     * Retrieves an object from the database based on the provided task and class.
     *
     * @param task The [KAtomicQueryTask] used to query the database.
     * @param kClass The class of the object to be retrieved.
     * @return The retrieved object, or null if no object is found.
     * @throws [UnsupportedTypeException] If the provided class is not supported.
     * @throws [SQLException] If an error occurs while executing the query.
     */
    override fun forObject(task: KAtomicQueryTask, kClass: KClass<*>): Any? {
        val map = forMap(task)
        return if (KPojo::class.isSuperclassOf(kClass)) {
            @Suppress("UNCHECKED_CAST") map?.safeMapperTo(kClass as KClass<KPojo>)
        } else if (map?.values?.firstOrNull() == null) {
            null
        } else {
            if (strictSetValue) {
                map.values.first()
            } else {
                getTypeSafeValue(
                    kClass.qualifiedName!!,
                    kClass.supertypes.map { it.toString() },
                    map.values.first(),
                    null,
                    map.values.first()::class
                )
            }
        }
    }

    /**
     * Updates the database with the provided task.
     *
     * @param task the [KAtomicActionTask] to be executed
     * @return the number of rows affected by the update operation
     * @throws [SQLException] if an error occurs while executing the update
     */
    override fun update(task: KAtomicActionTask): Int {
        val (sql, paramList) = task.parsed()
        val conn = dataSource.connection
        var ps: PreparedStatement? = null
        try {
            ps = conn.prepareStatement(sql)
            paramList.forEachIndexed { index, any ->
                ps.setObject(index + 1, any)
            }
            return ps.executeUpdate()
        } catch (e: SQLException) {
            defaultLogger(this).error(
                kMsgOf(
                    "Failed to execute update operation, ${e.message}.", Red
                ).endl().toArray()
            )
            throw e
        } finally {
            ps?.close()
            conn.close()
        }
    }

    /**
     * Executes a batch update operation using the provided [KronosAtomicBatchTask].
     *
     * @param task the [KAtomicActionTask] containing the SQL query and parameter lists
     * @return an array of integers representing the number of rows affected by each update operation
     * @throws [SQLException] if an error occurs while executing the batch update
     */
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
        val (sql, paramList) = task.parsedArr()
        val conn = dataSource.connection
        var ps: PreparedStatement? = null
        val result: IntArray
        try {
            ps = conn.prepareStatement(sql)
            paramList.forEach { paramMap ->
                paramMap.forEachIndexed { index, any ->
                    ps.setObject(index + 1, any)
                }
                ps.addBatch()
            }
            result = ps.executeBatch()
        } catch (e: SQLException) {
            defaultLogger(this).error(
                kMsgOf(
                    "Failed to execute batch update operation, ${e.message}.", Red
                ).endl().toArray()
            )
            throw e
        } finally {
            ps?.close()
            conn.close()
        }
        return result
    }

    companion object {
        /**
         * Perform database operations in a transaction
         *
         * @param T the return type
         * @param dataSource the data source
         * @param block the block of code to execute in the transaction
         * @return T the result of the block
         */
        inline fun <reified T> transact(dataSource: DataSource, block: (DataSource) -> T): T {
            val res: T?
            val conn = dataSource.connection
            conn.autoCommit = false
            try {
                res = block(dataSource)
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.close()
            }
            return res!!
        }
    }

    private fun ResultSet.toList(javaClass: Class<*>? = null): List<Map<String, Any>> {
        val meta = metaData
        val columnCount = meta.columnCount
        val list = mutableListOf<MutableMap<String, Any>>()
        if (dbType == Oracle.type) {
            // fix for Oracle: ORA 17027 Stream has already been closed
            val indexOfLong = mutableListOf<Int>()
            for (i in 1..meta.columnCount) {
                val columnTypeName = meta.getColumnTypeName(i)
                if (columnTypeName.uppercase() == "LONG") {
                    indexOfLong.add(i)
                }
            }
            while (next()) {
                val mapOfLong = mutableMapOf<String, Any>()
                for (i in 1..meta.columnCount) {
                    if (indexOfLong.contains(i)) {
                        if (javaClass != null) {
                            getObject(i, javaClass)
                        } else {
                            getObject(i)
                        }.let {
                            mapOfLong[meta.getColumnLabel(i)] = it
                        }
                    }
                }
                list.add(mapOfLong)
            }
            beforeFirst()
            var idx = 0
            while (next()) {
                val mapOfOther = mutableMapOf<String, Any>()
                for (i in 1..meta.columnCount) {
                    if (!indexOfLong.contains(i)) {
                        if (javaClass != null) {
                            getObject(i, javaClass)
                        } else {
                            getObject(i)
                        }.let {
                            mapOfOther[meta.getColumnLabel(i)] = it
                        }
                    }
                }
                list[idx++] += mapOfOther
            }
        } else {
            while (next()) {
                val map = mutableMapOf<String, Any>()
                for (i in 1..columnCount) {
                    getObject(i)?.let {
                        map[meta.getColumnLabel(i)] = it
                    }
                }
                list.add(map)
            }
        }
        return list
    }
}