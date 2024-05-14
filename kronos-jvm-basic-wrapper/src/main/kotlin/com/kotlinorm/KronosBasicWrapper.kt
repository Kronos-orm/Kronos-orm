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
import com.kotlinorm.beans.UnsupportedTypeException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.beans.logging.KLogMessage.Companion.logMessageOf
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.enums.ColorPrintCode
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.Extensions.transformToKPojo
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.reflect.KClass

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
     * @param task the [KronosAtomicTask] containing the SQL query and parameters
     * @return a list of maps representing the rows returned by the query
     * @throws [SQLException] if an error occurs while executing the query
     */
    override fun forList(task: KronosAtomicTask): List<Map<String, Any>> {
        val (sql, paramList) = task.parsed()
        val conn = dataSource.connection
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            ps = conn.prepareStatement(sql)
            paramList.forEachIndexed { index, any ->
                ps.setObject(index + 1, any)
            }
            rs = ps.executeQuery()
            val metaData = rs.metaData
            val columnCount = metaData.columnCount
            val list = mutableListOf<Map<String, Any>>()
            while (rs.next()) {
                val map = mutableMapOf<String, Any>()
                for (i in 1..columnCount) {
                    if (rs.getObject(i) != null) {
                        map[metaData.getColumnName(i)] = rs.getObject(i)
                    }
                }
                list.add(map)
            }
            return list
        } catch (e: SQLException) {
            defaultLogger(this).error(
                logMessageOf(
                    "Failed to execute query，${e.message}.",
                    ColorPrintCode.RED.toArray()
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
     * @param task the [KronosAtomicTask] containing the SQL query and parameters
     * @param kClass the class of the objects to retrieve
     * @return a list of objects of the specified class
     * @throws [SQLException] if an error occurs while executing the query
     */
    override fun forList(task: KronosAtomicTask, kClass: KClass<*>): List<Any> {
        return if (kClass.java.isAssignableFrom(KPojo::class.java)) {
            forList(task).map { it.transformToKPojo(kClass) }
        } else {
            val (sql, paramList) = task.parsed()
            val conn = dataSource.connection
            var ps: PreparedStatement? = null
            var rs: ResultSet? = null
            val list = mutableListOf<Any>()
            try {
                ps = conn.prepareStatement(sql)
                paramList.forEachIndexed { index, any ->
                    ps.setObject(index + 1, any)
                }
                rs = ps.executeQuery()
                while (rs.next()) {
                    list.add(rs.getObject(1, kClass.java))
                }
            } catch (e: SQLException) {
                defaultLogger(this).error(
                    logMessageOf(
                        "Failed to execute query，${e.message}.",
                        ColorPrintCode.RED.toArray()
                    ).endl().toArray()
                )
                throw e
            } finally {
                rs?.close()
                ps?.close()
                conn.close()
            }
            return list
        }
    }

    /**
     * Retrieves a single row from the database as a map of column names to values.
     *
     * @param task the [KronosAtomicTask] containing the SQL query and parameters
     * @return a map of column names to values, or null if no rows are returned
     * @throws [SQLException] if an error occurs while executing the query
     */
    override fun forMap(task: KronosAtomicTask): Map<String, Any>? {
        val (sql, paramList) = task.parsed()
        val conn = dataSource.connection
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            ps = conn.prepareStatement(sql)
            paramList.forEachIndexed { index, any ->
                ps.setObject(index + 1, any)
            }
            rs = ps.executeQuery()
            val list = mutableListOf<Map<String, Any>>()
            val metaData = rs.metaData
            val columnCount = metaData.columnCount
            while (rs.next()) {
                val map = mutableMapOf<String, Any>()
                for (i in 1..columnCount) {
                    map[metaData.getColumnName(i)] = rs.getObject(i)
                }
                list.add(map)
            }
            return list.firstOrNull()
        } catch (e: SQLException) {
            defaultLogger(this).error(
                logMessageOf(
                    "Failed to execute query，${e.message}.",
                    ColorPrintCode.RED.toArray()
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
     * @param task The [KronosAtomicTask] used to query the database.
     * @param kClass The class of the object to be retrieved.
     * @return The retrieved object, or null if no object is found.
     * @throws [UnsupportedTypeException] If the provided class is not supported.
     * @throws [SQLException] If an error occurs while executing the query.
     */
    override fun forObject(task: KronosAtomicTask, kClass: KClass<*>): Any? {
        val map = forMap(task)
        val clazz = kClass.java
        return if (String::class.java == kClass.java) {
            map?.values?.firstOrNull()?.toString()
        } else if (KPojo::class.java.isAssignableFrom(kClass.java)) {
            map?.transformToKPojo(kClass)
        } else if (clazz.name == "java.lang.Integer") {
            map?.values?.firstOrNull()?.toString()?.toInt()
        } else if (clazz.name == "java.lang.Long") {
            map?.values?.firstOrNull()?.toString()?.toLong()
        } else if (clazz.name == "java.lang.Double") {
            map?.values?.firstOrNull()?.toString()?.toDouble()
        } else if (clazz.name == "java.lang.Float") {
            map?.values?.firstOrNull()?.toString()?.toFloat()
        } else if (clazz.name == "java.lang.Boolean") {
            map?.values?.firstOrNull()?.toString()?.toBoolean()
        } else if (clazz.name == "java.lang.Short") {
            map?.values?.firstOrNull()?.toString()?.toShort()
        } else if (clazz.name == "java.lang.Byte") {
            map?.values?.firstOrNull()?.toString()?.toByte()
        } else if (clazz.name == "java.lang.String") {
            map?.values?.firstOrNull()?.toString()
        } else if (clazz.name == "java.util.Date") {
            map?.values?.firstOrNull()?.toString()?.toLong()?.let { java.util.Date(it) }
        } else {
            try {
                map?.values?.firstOrNull()?.toString()?.let { clazz.cast(it) }
            } catch (e: Exception) {
                throw UnsupportedTypeException("Unsupported type: ${clazz.name}").apply {
                    addSuppressed(e)
                }
            }
        }
    }

    /**
     * Updates the database with the provided task.
     *
     * @param task the [KronosAtomicTask] to be executed
     * @return the number of rows affected by the update operation
     * @throws [SQLException] if an error occurs while executing the update
     */
    override fun update(task: KronosAtomicTask): Int {
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
                logMessageOf(
                    "Failed to execute update operation, ${e.message}.", ColorPrintCode.RED.toArray()
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
     * @param task the [KronosAtomicBatchTask] containing the SQL query and parameter lists
     * @return an array of integers representing the number of rows affected by each update operation
     * @throws [SQLException] if an error occurs while executing the batch update
     */
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
        val (sql, paramList) = task.parsed()
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
                logMessageOf(
                    "Failed to execute batch update operation, ${e.message}.", ColorPrintCode.RED.toArray()
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
}