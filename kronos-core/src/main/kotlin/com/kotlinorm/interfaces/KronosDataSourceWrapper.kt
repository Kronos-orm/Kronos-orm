package com.kotlinorm.interfaces

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.NoDataSourceException
import kotlin.reflect.KClass

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

    /**
     * Executes a SQL query and returns the results as a list of maps, with each map representing a row of the result set.
     * Each key in the map corresponds to a column name, and the associated value is the value of that column for the row.
     *
     * @param task The [KronosAtomicTask] that contains the SQL query to be executed and the parameters to be bound to the query.
     * @return A list of maps, where each map represents a row of the result set. Each key in the map is a column name, and the
     *         associated value is the value of that column for that row. If the query returns no rows, this method returns an
     *         empty list.
     * @throws NoDataSourceException If there is no data source configured for the current environment.
     */
    fun forList(task: KronosAtomicTask): List<Map<String, Any>>

    /**
     * Executes a SQL query and returns the results as a list of objects. The type of objects in the list is determined
     * by the specified class type parameter (`kClass`). Each object in the list represents a row of the result set,
     * instantiated or converted according to the `kClass` parameter.
     *
     * @param task The [KronosAtomicTask] that contains the SQL query to be executed and the parameters to be bound to the query.
     * @param kClass The Kotlin class (`KClass`) that specifies the type of objects to be returned in the list. The method
     *               will attempt to instantiate or convert each row of the result set into an instance of this class.
     * @return A list of objects of the type specified by `kClass`, where each object represents a row of the result set.
     *         If the query returns no rows, or if instantiation/conversion to the specified type fails, this method
     *         returns an empty list.
     * @throws InstantiationException If there is an error instantiating objects of the specified type from the query
     *                                results, such as due to a mismatch between the query result structure and the
     *                                expected class fields or constructor parameters.
     * @throws NoDataSourceException If there is no data source configured for the current environment.
     */
    fun forList(
        task: KronosAtomicTask,
        kClass: KClass<*>
    ): List<Any>

    /**
     * Executes a SQL query and returns the result as a map, with each entry representing a column of the single row
     * in the result set. The key in the map corresponds to a column name, and the associated value is the value of
     * that column for the row.
     *
     * @param task The [KronosAtomicTask] that contains the SQL query to be executed and the parameters to be bound to the query.
     * @return A map representing a single row of the result set, where each key is a column name and the associated
     *         value is the value of that column for that row. If the query returns no rows, this method returns `null`.
     * @throws NoDataSourceException If there is no data source configured for the current environment.
     */
    fun forMap(task: KronosAtomicTask): Map<String, Any>?

    /**
     * Executes a SQL query and attempts to convert the result set's single row into an instance of the specified Java class.
     * The conversion aims to map the columns of the result set to the fields of the specified class, based on their names and types.
     *
     * @param task The [KronosAtomicTask] that contains the SQL query to be executed and the parameters to be bound to the query.
     * @return An object of the specified type that represents the converted row of the result set. If the query results in no rows,
     *         or if the conversion cannot be successfully performed (due to type mismatches, missing fields, etc.), this method
     *         may return `null` or throw a relevant exception.
     * @throws NoDataSourceException If there is no data source configured for the current environment.
     */
    fun forObject(
        task: KronosAtomicTask,
        kClass: KClass<*>
    ): Any?

    /**
     * Executes an SQL update operation (such as INSERT, UPDATE, or DELETE) using the provided SQL string and a map of parameters.
     * This method is designed to facilitate the execution of dynamic SQL operations where the specific parameters can vary depending on runtime conditions.
     *
     * @param task The [KronosAtomicTask] that contains the SQL statement to be executed and the parameters to be bound to the update.
     * @return The number of database rows affected by the execution of the SQL statement. This can be used to verify the success and impact of the
     *         update operation, with a return value of 0 indicating that no rows were affected.
     * @throws NoDataSourceException If there is no data source configured for the current environment.
     */
    fun update(task: KronosAtomicTask): Int

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
}