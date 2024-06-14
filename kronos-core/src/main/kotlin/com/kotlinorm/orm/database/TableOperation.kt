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

package com.kotlinorm.orm.database

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.DBHelper.convertToSqlColumnType
import com.kotlinorm.orm.database.DBHelper.getDBNameFromUrl
import com.kotlinorm.orm.delete.DeleteClause
import com.kotlinorm.utils.DataSourceUtil.orDefault
import java.util.*
import kotlin.reflect.full.createInstance

class TableOperation(val wrapper: KronosDataSourceWrapper) {

    val dataSource by lazy { wrapper.orDefault() }

    /**
     * Checks whether the specified table exists in the database.
     * Use the reified keyword to support type erasure of generics, so that information about the generic type T can be obtained at runtime.
     * This method is mainly used for instances of the KPojo type. It creates an instance to get the table name and then checks whether the table exists.
     *
     * 检查数据库中是否存在指定的表。
     * 使用 reified 关键字支持泛型的类型擦除，使得可以在运行时获取泛型类型 T 的信息。
     * 该方法主要用于 KPojo 类型的实例，通过创建一个实例来获取表名，然后检查表是否存在。
     *
     * @param T generic type, inherited from KPojo, used to get the table name.
     * 泛型类型，继承自 KPojo，用于获取表名。
     * @return Boolean, Boolean value indicating whether the table exists.
     * 表示表是否存在的布尔值。
     */
    inline fun <reified T : KPojo> exists(instance: T = T::class.createInstance()): Boolean {
        // 通过实例获取表名
        val kronosTableName = instance.kronosTableName()
        // 调用查询方法，检查表是否存在
        return queryTableExistence(kronosTableName)
    }

    /**
     * 查询数据库中指定表是否存在。
     * 根据不同的数据库类型构造并执行相应的 SQL 查询，返回查询结果表示表是否存在。
     *
     * @param tableName String 表名。
     * @return Boolean 表示表是否存在的布尔值。
     */
    fun queryTableExistence(tableName: String): Boolean {
        // 根据数据库类型构造 SQL 和参数映射
        val (sql, paramMap) = when (dataSource.dbType) {
            DBType.Mysql ->
                // MySQL 的查询 SQL，统计表信息表中指定表名和数据库名的行数
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = :tableName AND table_schema = :tableSchema" to mapOf(
                    "tableName" to tableName,
                    "tableSchema" to getDBNameFromUrl(dataSource)
                )

            DBType.Oracle ->
                // Oracle 的查询 SQL，统计 user_tables 表中指定表名（大写）的行数
                "select count(*) from user_tables where table_name =upper(:tableName)" to mapOf(
                    "tableName" to tableName
                )

            DBType.Postgres ->
                // PostgreSQL 的查询 SQL，统计 pg_class 表中指定表名的行数
                "select count(*) from pg_class where relname = :tableName;" to mapOf(
                    "tableName" to tableName
                )

            DBType.Mssql ->
                // SQL Server 的查询 SQL，统计 sys.objects 表中指定对象名的行数
                "select count(1) from sys.objects where name = :tableName" to mapOf(
                    "tableName" to tableName
                )

            DBType.SQLite ->
                // SQLite 的查询 SQL，统计 sqlite_master 表中类型为 table 且名称指定的行数
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = :tableName" to mapOf(
                    "tableName" to tableName
                )

            DBType.DB2, DBType.Sybase, DBType.H2, DBType.OceanBase, DBType.DM8 ->
                // 对于不支持的数据库类型，抛出异常
                throw NotImplementedError("Support for this database type is not implemented yet.")
        }

        // 执行查询，返回结果大于0表示表存在
        // 执行查询操作，返回结果大于0表示表存在
        val result = dataSource.forObject(
            KronosAtomicQueryTask(
                sql, paramMap
            ),
            Int::class
        ) as Int
        return result > 0
    }

    /**
     * Dynamically create a database table based on a given KPojo class.
     *
     * 根据给定的KPojo类动态创建数据库表。
     *
     * This function obtains the instance and metadata of the KPojo class through the reflection mechanism, generates and executes the corresponding SQL statement according to different database types,
     * thereby creating the corresponding table in the database. Supported database types include MySQL, Oracle, PostgreSQL, Microsoft SQL Server,
     * SQLite, DB2, Sybase, H2, OceanBase, and DM8.
     *
     * 该函数通过反射机制获取KPojo类的实例和元数据，根据不同的数据库类型生成并执行相应的SQL语句，
     * 从而在数据库中创建对应的表。支持的数据库类型包括MySQL、Oracle、PostgreSQL、Microsoft SQL Server、
     * SQLite、DB2、Sybase、H2、OceanBase和DM8。
     *
     * @param <T> The type parameter of the KPojo class, which must be inherited from KPojo.
     *
     * KPojo类的类型参数，必须继承自KPojo
     * @return The instance of the created table.
     *
     * 创建的表的实例。
     */
    inline fun <reified T : KPojo> createTable(instance: T = T::class.createInstance()): Boolean {
        // 从实例中获取表名
        val kronosTableName = instance.kronosTableName()
        // 从实例中获取列定义
        val kronosColumns = instance.kronosColumns()
        // 从实例中获取索引
        val kronosIndexes = instance.kronosTableIndex()
        // 根据不同的数据库类型，生成并执行创建表的SQL语句
        when (dataSource.dbType) {
            DBType.Mysql -> {
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = convertToSqlColumnType(
                        DBType.Mysql,
                        column.type,
                        column.length,
                        column.nullable,
                        column.primaryKey
                    )
                    val identity = if (column.identity) " AUTO_INCREMENT" else " "
                    val defaultValue = if (column.defaultValue != null) "DEFAULT ${column.defaultValue}" else ""
                    "$columnName $columnType$identity$defaultValue"
                }

                val indexDefinitions = kronosIndexes.joinToString(",\n") { index ->
                    "${index.type.uppercase(Locale.getDefault())} KEY `${index.name}` (`${index.columns.joinToString("`, `")}`) USING ${index.method}"
                }.let {
                    if (it.isNotEmpty()) {
                        ",$it"
                    } else {
                        ""
                    }
                }

                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName (${columnDefinitions}\n${indexDefinitions}) ;"
                println(sql)
                val result = dataSource.update(KronosAtomicActionTask(sql))
                return result > 0
            }

            DBType.Oracle -> {
                // 生成Oracle的列定义字符串
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    // 列名
                    val columnName = column.columnName
                    // 列类型
                    val columnType =
                        convertToSqlColumnType(
                            DBType.Oracle,
                            column.type,
                            column.length,
                            column.nullable,
                            column.primaryKey
                        ) // PostgreSQL通常不需要额外类型转换
                    // 主键递增属性
                    val identity = if (column.identity) " AUTO_INCREMENT" else " "
                    // 默认值
                    val defaultValue = if (column.defaultValue != null) "DEFAULT ${column.defaultValue}" else ""
                    "$columnName $columnType $identity $defaultValue"
                }
                val indexDefinitions = kronosIndexes.joinToString(",\n") { index ->
                    "${index.type.uppercase(Locale.getDefault())} KEY `${index.name}` (`${index.columns.joinToString("`, `")}`) USING ${index.method}"
                }.let {
                    if (it.isNotEmpty()) {
                        ",$it"
                    } else {
                        ""
                    }
                }
                // 执行创建表的SQL语句
                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName (${columnDefinitions}\n${indexDefinitions}) ;"
                val result = dataSource.update(KronosAtomicActionTask(sql))
                return result > 0
            }

            DBType.Postgres -> {
                // 生成PostgreSQL的列定义字符串
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    // 列名
                    val columnName = column.columnName
                    // 列类型
                    val columnType =
                        convertToSqlColumnType(
                            DBType.Postgres,
                            column.type,
                            column.length,
                            column.nullable,
                            column.primaryKey
                        ) // PostgreSQL通常不需要额外类型转换
                    // 主键及递增属性
                    val identity = if (column.identity) " AUTO_INCREMENT" else " "
                    // 默认值
                    val defaultValue = if (column.defaultValue != null) "DEFAULT ${column.defaultValue}" else ""
                    "$columnName $columnType $identity $defaultValue"
                }
                val indexDefinitions = kronosIndexes.joinToString(",\n") { index ->
                    "${index.type.uppercase(Locale.getDefault())} KEY `${index.name}` (`${index.columns.joinToString("`, `")}`) USING ${index.method}"
                }.let {
                    if (it.isNotEmpty()) {
                        ",$it"
                    } else {
                        ""
                    }
                }

                // 执行创建表的SQL语句
                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName (${columnDefinitions}\n${indexDefinitions}) ;"
                val result = dataSource.update(KronosAtomicActionTask(sql))
                return result > 0
            }

            DBType.Mssql -> {
                // 生成Microsoft SQL Server的列定义字符串
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    // 列名
                    val columnName = column.columnName
                    // 列类型
                    val columnType =
                        convertToSqlColumnType(
                            DBType.Mssql,
                            column.type,
                            column.length,
                            column.nullable,
                            column.primaryKey
                        ) // 可能需要根据MSSQL调整类型
                    // 主键及递增属性
                    val identity = if (column.identity) " AUTO_INCREMENT" else " "
                    // 默认值
                    val defaultValue = if (column.defaultValue != null) "DEFAULT ${column.defaultValue}" else ""
                    "$columnName $columnType $identity $defaultValue"
                }
                val indexDefinitions = kronosIndexes.joinToString(",\n") { index ->
                    "${index.type.uppercase(Locale.getDefault())} KEY `${index.name}` (`${index.columns.joinToString("`, `")}`) USING ${index.method}"
                }.let {
                    if (it.isNotEmpty()) {
                        ",$it"
                    } else {
                        ""
                    }
                }
                // 执行创建表的SQL语句
                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName (${columnDefinitions}\n${indexDefinitions}) ;"
                val result = dataSource.update(KronosAtomicActionTask(sql))
                return result > 0
            }

            DBType.SQLite -> {
                // 生成SQLite的列定义字符串
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    // 列名
                    val columnName = column.columnName
                    // 列类型
                    val columnType =
                        convertToSqlColumnType(
                            DBType.SQLite,
                            column.type,
                            column.length,
                            column.nullable,
                            column.primaryKey
                        )
                    // 主键属性
                    val identity = if (column.identity) " AUTO_INCREMENT" else " "
                    // 默认值
                    val defaultValue = if (column.defaultValue != null) "DEFAULT ${column.defaultValue}" else ""
                    "$columnName $columnType $identity $defaultValue"
                }
                val indexDefinitions = kronosIndexes.joinToString(",\n") { index ->
                    "${index.type.uppercase(Locale.getDefault())} KEY `${index.name}` (`${index.columns.joinToString("`, `")}`) USING ${index.method}"
                }.let {
                    if (it.isNotEmpty()) {
                        ",$it"
                    } else {
                        ""
                    }
                }
                // 执行创建表的SQL语句
                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName (${columnDefinitions}\n${indexDefinitions}) ;"
                val result = dataSource.update(KronosAtomicActionTask(sql))
                return result > 0
            }

            DBType.DB2, DBType.Sybase, DBType.H2, DBType.OceanBase, DBType.DM8 -> throw NotImplementedError("Unsupported database types")
        }
    }

    /**
     * Function to drop the specified table, and execute the corresponding delete statement according to different database types.
     * This function uses the reified keyword to allow information about type T to be obtained at runtime.
     * 删除指定表的函数，根据不同的数据库类型执行相应的删除语句。
     * 该函数使用了reified关键字，允许在运行时获取类型T的信息。
     *
     * @param T type parameter, which must be inherited from KPojo, indicating the entity class corresponding to the table to be deleted.
     * 类型参数，必须继承自KPojo，表示要删除的表对应的实体类。
     * @return Returns the DeleteClause instance, indicating the result of the delete operation.
     * 返回DeleteClause实例，表示删除操作的结果。
     */
    inline fun <reified T : KPojo> dropTable(instance: T = T::class.createInstance()): DeleteClause<KPojo> {
        // 表名
        val kronosTableName = instance.kronosTableName()
        // 根据数据库类型执行不同的删除语句
        return when (dataSource.dbType) {
            DBType.Mysql -> {
                val sql = "DROP TABLE IF EXISTS $kronosTableName"
                dataSource.update(KronosAtomicActionTask(sql))
                DeleteClause(instance)
            }

            DBType.Oracle -> {
                val sql = "DROP TABLE $kronosTableName"
                dataSource.update(KronosAtomicActionTask(sql))
                DeleteClause(instance)
            }

            DBType.Postgres -> {
                val sql = "DROP TABLE IF EXISTS $kronosTableName"
                dataSource.update(KronosAtomicActionTask(sql))
                DeleteClause(instance)
            }

            DBType.Mssql -> {
                val sql =
                    "IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$kronosTableName') AND type in (N'U')) BEGIN DROP TABLE $kronosTableName END"
                dataSource.update(KronosAtomicActionTask(sql))
                DeleteClause(instance)
            }

            DBType.SQLite -> {
                val sql = "DROP TABLE IF EXISTS $kronosTableName"
                dataSource.update(KronosAtomicActionTask(sql))
                DeleteClause(instance)
            }

            DBType.DB2, DBType.Sybase, DBType.H2, DBType.OceanBase, DBType.DM8 -> throw NotImplementedError("Unsupported database types")
        }
    }

    fun differ(
        dbType: DBType,
        expect: List<Field>,
        current: List<Field>
    ): Triple<List<Field>, List<Field>, List<Field>> {
        val toAdd = expect.filter { col -> col.columnName !in current.map { it.columnName } }
        val toModified = expect.filter { col ->
            val tableColumn = current.find { col.columnName == it.columnName }
            tableColumn == null ||
                    /* col.primaryKey != tableColumn.primaryKey ||*/
                    col.defaultValue != tableColumn.defaultValue ||
                    convertToSqlColumnType(
                        dbType,
                        col.type,
                        col.length,
                        col.nullable,
                        col.primaryKey
                    ) != convertToSqlColumnType(
                dbType,
                tableColumn.type,
                tableColumn.length,
                tableColumn.nullable,
                tableColumn.primaryKey
            )
        }
        val toDelete = current.filter { col -> col.columnName !in expect.map { it.columnName } }
        return Triple(toAdd, toModified, toDelete)
    }


    /**
     * Synchronize (update) the database table according to the specified table structure. If the table does not exist, create it. If the table exists, modify it if the data does not match.
     * This function uses the reified keyword to allow information about type T to be obtained at runtime.
     *
     * 根据指定的表结构同步（更新）数据库表，如果表不存在则创建，表如果存在，数据对不上则修改。
     * 该函数使用了reified关键字，允许在运行时获取类型T的信息。
     *
     * @param T type parameter, which must inherit from KPojo, indicating the entity class corresponding to the table to be synchronized.
     *
     * 类型参数，必须继承自KPojo，表示要同步的表对应的实体类
     *
     * @return Returns the Table instance, indicating the result of the synchronization operation.
     *
     * 返回Table实例，表示同步操作的结果。
     */
    inline fun <reified T : KPojo> structureSync(instance: T = T::class.createInstance()): Boolean {
        // 获取数据源实例
        val dataSource = wrapper.orDefault()
        // 表名
        val kronosTableName = instance.kronosTableName()
        // 实体类列信息
        val kronosColumns = instance.kronosColumns()
        // 数据库类型
        val dbType = dataSource.dbType

        // 不存在就创建
        if (!queryTableExistence(kronosTableName)) {
            return createTable(instance)
        }

        // 获取实际表字段信息
        val tableColumns = getTableColumns(kronosTableName)
        val (toAdd, toModified, toDelete) = differ(dbType, kronosColumns, tableColumns)

        // 根据数据库类型执行不同的创建/修改表语句
        when (dbType) {
            DBType.Mysql -> {
                val listOfSql = toAdd.map {
                    "ALTER TABLE $kronosTableName ADD COLUMN ${it.columnName} ${
                        convertToSqlColumnType(
                            DBType.Mysql,
                            it.type,
                            it.length,
                            it.nullable,
                            it.primaryKey
                        )
                    } ${if (it.identity) "AUTO_INCREMENT" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""};"
                } + toModified.map {
                    "ALTER TABLE $kronosTableName MODIFY COLUMN ${it.columnName} ${
                        convertToSqlColumnType(
                            DBType.Mysql,
                            it.type,
                            it.length,
                            it.nullable,
                            it.primaryKey
                        )
                    } ${if (it.identity) "AUTO_INCREMENT" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""};"
                        .let {
                            // 判断主键是否有改动 如果有 先删除主键 DROP PRIMARY KEY
                            if (
                                it.contains("PRIMARY KEY")
                            ) {
                                "ALTER TABLE $kronosTableName DROP PRIMARY KEY;" + it
                            } else {
                                it
                            }
                        }
                } + toDelete.map {
                    "ALTER TABLE $kronosTableName DROP COLUMN ${it.columnName};"
                }
                println(listOfSql)
                if (listOfSql.isNotEmpty()) {
                    listOfSql.forEach {
                        dataSource.update(KronosAtomicActionTask(it))
                    }
                    println("return true")
                    return true
                } else {
                    println("return false")
                    return false
                }
            }

            DBType.Oracle -> {
                val listOfSql = toAdd.map {
                    "ALTER TABLE $kronosTableName ADD ${it.columnName} ${it.type} ${if (it.length > 0) "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""};"
                } + toModified.map {
                    "ALTER TABLE $kronosTableName MODIFY COLUMN ${it.columnName} ${it.type} ${if (it.length > 0) "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.defaultValue != null) "DEFAULT ' ${it.defaultValue}'" else ""};"
                } + toDelete.map {
                    "ALTER TABLE $kronosTableName DROP COLUMN ${it.columnName};"
                }
                if (listOfSql.isNotEmpty()) {
                    listOfSql.forEach {
                        dataSource.update(KronosAtomicActionTask(it))
                    }
                    return true
                } else {
                    return false
                }
            }

            DBType.Postgres -> {
                val listOfSql = toAdd.map {
                    "ALTER TABLE $kronosTableName ADD COLUMN ${it.columnName} ${it.type} ${if (it.length > 0) "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""};"
                } + toModified.map {
                    "ALTER TABLE $kronosTableName MODIFY COLUMN ${it.columnName} ${it.type} ${if (it.length > 0) "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""};"
                } + toDelete.map {
                    "ALTER TABLE $kronosTableName DROP COLUMN ${it.columnName};"
                }
                if (listOfSql.isNotEmpty()) {
                    listOfSql.forEach {
                        dataSource.update(KronosAtomicActionTask(it))
                    }
                    return true
                } else {
                    return false
                }
            }

            DBType.Mssql -> {
                val listOfSql = toAdd.map {
                    "ALTER TABLE $kronosTableName ADD COLUMN ${it.columnName} ${it.type} ${if (it.length > 0) "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""};"
                } + toModified.map {
                    "ALTER TABLE $kronosTableName MODIFY COLUMN ${it.columnName} ${it.type} ${if (it.length > 0) "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""};"
                } + toDelete.map {
                    "ALTER TABLE $kronosTableName DROP COLUMN ${it.columnName};"
                }
                if (listOfSql.isNotEmpty()) {
                    listOfSql.forEach {
                        dataSource.update(KronosAtomicActionTask(it))
                    }
                    return true
                } else {
                    return false
                }
            }

            DBType.SQLite -> {
                val listOfSql = toAdd.map {
                    "ALTER TABLE $kronosTableName ADD COLUMN ${it.columnName} ${it.type} ${if (it.length > 0) "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""};"
                } + toModified.map {
                    "ALTER TABLE $kronosTableName MODIFY COLUMN ${it.columnName} ${it.type} ${if (it.length > 0) "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""};"
                } + toDelete.map {
                    "ALTER TABLE $kronosTableName DROP COLUMN ${it.columnName};"
                }
                if (listOfSql.isNotEmpty()) {
                    listOfSql.forEach {
                        dataSource.update(KronosAtomicActionTask(it))
                    }
                    return true
                } else {
                    return false
                }
            }

            DBType.DB2, DBType.Sybase, DBType.H2, DBType.OceanBase, DBType.DM8 -> throw NotImplementedError("Unsupported database types")
        }
    }

    /**
     * Get the column names of the specified table.
     *
     * 获取指定表的列名。
     *
     * @param tableName String, the name of the table.
     *
     * 表名。
     *
     * @return List<Field>, a list of column field.
     *
     * 列名列表。
     */
    fun getTableColumns(tableName: String): List<Field> {
        val sql = when (wrapper.orDefault().dbType) {
            DBType.Mysql -> """
                SELECT 
                    c.COLUMN_NAME, 
                    c.DATA_TYPE, 
                    c.CHARACTER_MAXIMUM_LENGTH, 
                    c.IS_NULLABLE,
                    c.COLUMN_DEFAULT,
                    (CASE WHEN c.COLUMN_KEY = 'PRI' THEN 'YES' ELSE 'NO' END) AS PRIMARY_KEY
                FROM 
                    INFORMATION_SCHEMA.COLUMNS c
                WHERE 
                 c.TABLE_SCHEMA = DATABASE() AND 
                 c.TABLE_NAME = '${tableName}';
                """

            DBType.Oracle -> "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, FROM ALL_TAB_COLUMNS WHERE TABLE_NAME = '$tableName'"
            DBType.Postgres -> "SELECT COLUMN_NAME,DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '$tableName'"
            DBType.Mssql -> "SELECT COLUMN_NAME,DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME= '$tableName'"
            DBType.SQLite -> "SELECT name, type, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE FROM pragma_table_info('$tableName')"
            DBType.DB2, DBType.Sybase, DBType.H2, DBType.OceanBase, DBType.DM8 -> throw NotImplementedError("Unsupported database types")
        }
        return wrapper.orDefault().forList(KronosAtomicQueryTask(sql)).map {
            Field(
                columnName = it["COLUMN_NAME"].toString(),
                type = it["DATA_TYPE"].toString().uppercase(Locale.getDefault()),
                length = it["LENGTH"] as Int? ?: 0,
                tableName = tableName,
                nullable = it["IS_NULLABLE"] == "YES",
                primaryKey = it["PRIMARY_KEY"] == "YES",
                defaultValue = it["COLUMN_DEFAULT"] as String?
                // 查不出来 identity = it["IDENTITY"] as Boolean? ?: false
            )
        }
    }

}