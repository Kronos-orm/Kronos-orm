package com.kotoframework.interfaces

import com.kotoframework.enums.DBType
import com.kotoframework.exceptions.NoDataSourceException
import kotlin.reflect.KClass

interface KotoDataSourceWrapper {
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
     * @param sql The SQL query to be executed. This should be a valid SQL statement.
     * @param paramMap A map containing parameters to bind to the query. The keys should correspond to named parameters in the
     *                 SQL statement (if applicable), and the values will be used as the parameter values. If no parameters are
     *                 needed, this can be left as an empty map. Defaults to an empty map if not provided.
     * @return A list of maps, where each map represents a row of the result set. Each key in the map is a column name, and the
     *         associated value is the value of that column for that row. If the query returns no rows, this method returns an
     *         empty list.
     * @throws NoDataSourceException If there is no data source configured for the current environment.
     */
    fun forList(sql: String, paramMap: Map<String, Any?> = mapOf()): List<Map<String, Any>>

    /**
     * Executes a SQL query and returns the results as a list of objects. The type of objects in the list is determined
     * by the specified class type parameter (`kClass`). Each object in the list represents a row of the result set,
     * instantiated or converted according to the `kClass` parameter.
     *
     * @param sql The SQL query to be executed. This must be a valid SQL statement and should match the expected structure
     *            for the objects to be instantiated based on `kClass`.
     * @param paramMap A map containing parameters to bind to the query. The keys should correspond to named parameters in
     *                 the SQL statement, with the associated values used as the parameter values. This allows for dynamic
     *                 query execution with variable conditions.
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
        sql: String,
        paramMap: Map<String, Any?>,
        kClass: KClass<*>
    ): List<Any>

    /**
     * Executes a SQL query and returns the result as a map, with each entry representing a column of the single row
     * in the result set. The key in the map corresponds to a column name, and the associated value is the value of
     * that column for the row.
     *
     * @param sql The SQL query to be executed. This must be a valid SQL statement. The query is expected to return at
     *            most one row of data, as the result is mapped to a single Map instance.
     * @param paramMap A map containing parameters to bind to the query. The keys should correspond to named parameters
     *                 in the SQL statement, with the associated values used as the parameter values. This allows for
     *                 dynamic query execution with variable conditions. Defaults to an empty map if not provided.
     * @return A map representing a single row of the result set, where each key is a column name and the associated
     *         value is the value of that column for that row. If the query returns no rows, this method returns `null`.
     * @throws NoDataSourceException If there is no data source configured for the current environment.
     */
    fun forMap(sql: String, paramMap: Map<String, Any?> = mapOf()): Map<String, Any>?

    /**
     * Executes a SQL query and attempts to convert the result set's single row into an instance of the specified Java class.
     * The conversion aims to map the columns of the result set to the fields of the specified class, based on their names and types.
     *
     * @param sql The SQL query to be executed. It should be a valid SQL statement designed to return at most one row of data.
     *            The query's purpose is to fetch data that can be mapped to the structure of the specified Java class.
     * @param paramMap A map of parameters to be bound to the query. The map's keys should correspond to the named parameters
     *                 within the SQL statement, and the values should be the actual values to bind to these parameters.
     *                 This approach facilitates the execution of dynamic queries with varying conditions.
     * @return An object of the specified type that represents the converted row of the result set. If the query results in no rows,
     *         or if the conversion cannot be successfully performed (due to type mismatches, missing fields, etc.), this method
     *         may return `null` or throw a relevant exception.
     * @throws NoDataSourceException If there is no data source configured for the current environment.
     */
    fun forObject(
        sql: String,
        paramMap: Map<String, Any?>,
        kClass: KClass<*>
    ): Any?

    /**
     * Executes an SQL update operation (such as INSERT, UPDATE, or DELETE) using the provided SQL string and a map of parameters.
     * This method is designed to facilitate the execution of dynamic SQL operations where the specific parameters can vary depending on runtime conditions.
     *
     * @param sql The SQL statement to be executed. This statement should be a valid SQL command intended for updating data within the database.
     *            It can include named placeholders for parameters, which will be replaced by the actual values specified in the `paramMap`.
     * @param paramMap A map of parameters to be used with the SQL statement. Each key in the map corresponds to a named placeholder in the SQL
     *                 string, and each value is the actual value to be bound to that placeholder. This parameter is optional and defaults to an
     *                 empty map, allowing for SQL statements without parameters to be executed directly.
     * @return The number of database rows affected by the execution of the SQL statement. This can be used to verify the success and impact of the
     *         update operation, with a return value of 0 indicating that no rows were affected.
     * @throws NoDataSourceException If there is no data source configured for the current environment.
     */
    fun update(sql: String, paramMap: Map<String, Any?> = mapOf()): Int

    /**
     * Executes a batch of SQL update operations (such as INSERT, UPDATE, or DELETE) using a single SQL string and an array of parameter maps.
     * This method allows for efficient execution of multiple update operations, making it ideal for bulk data modifications.
     *
     * @param sql The SQL statement to be executed for each batch. This statement should be properly formatted for batch processing,
     *            typically including named placeholders for parameters. The same SQL statement is applied to each set of parameters
     *            in the `paramMaps` array.
     * @param paramMaps An array of maps, where each map contains parameters to be bound to the SQL statement for a single batch operation.
     *                  Each key in a map corresponds to a named placeholder in the SQL string, and each value is the actual value to be
     *                  bound to that placeholder. This parameter is optional and defaults to an empty array, allowing for SQL statements
     *                  without parameters to be executed in batch mode.
     * @return An array of integers, where each element represents the number of rows affected by the corresponding update operation in the batch.
     *         This can be used to verify the success and impact of each update operation within the batch.
     * @throws NoDataSourceException If there is no data source configured for the current environment.
     */
    fun batchUpdate(sql: String, paramMaps: Array<Map<String, Any?>>?): IntArray
}