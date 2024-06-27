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
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.DBHelper.convertToSqlColumnType
import com.kotlinorm.orm.database.DBHelper.getDBNameFromUrl
import com.kotlinorm.orm.delete.DeleteClause
import com.kotlinorm.utils.DataSourceUtil.orDefault
import java.math.BigDecimal
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
                "select count(*) from user_tables where table_name = :tableName" to mapOf(
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
                "SELECT COUNT(*)  as CNT FROM sqlite_master where type='table' and name= :tableName" to mapOf(
                    "tableName" to tableName
                )

            DBType.DB2, DBType.Sybase, DBType.H2, DBType.OceanBase, DBType.DM8 ->
                // 对于不支持的数据库类型，抛出异常
                throw NotImplementedError("Support for this database type is not implemented yet.")
        }

        // 执行查询，返回结果大于0表示表存在
        // 执行查询操作，返回结果大于0表示表存在
        println(sql)
        println(paramMap)
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
        val kronosColumns = instance.kronosColumns().filter { it.isColumn }
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
                            column.primaryKey,
                            column.identity
                        )
                    // 默认值
                    val defaultValue = if (column.defaultValue != null) "DEFAULT ${column.defaultValue}" else ""
                    "$columnName $columnType $defaultValue"
                }
                val indexDefinitions = kronosIndexes.map { index ->
                    "CREATE ${index.type.uppercase(Locale.getDefault())} INDEX ${index.name} ON $kronosTableName (${
                        index.columns.joinToString(
                            ", "
                        )
                    })"
                }
                // 执行创建表的SQL语句
                val sqls =
                    listOf("CREATE TABLE $kronosTableName (${columnDefinitions}) ") + indexDefinitions
                println(sqls)
                println(sqls.size)
                val result = sqls.sumOf { dataSource.update(KronosAtomicActionTask(it)) }
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
                        ).let {
                            // 主键及递增属性
                            if (column.primaryKey) {
                                "SERIAL PRIMARY KEY"
                            } else {
                                it
                            }
                        }
                    // 默认值
                    val defaultValue = if (column.defaultValue != null) "DEFAULT ${column.defaultValue}" else ""
                    "$columnName $columnType $defaultValue"
                }
                val indexDefinitions = kronosIndexes.map { index ->
                    "CREATE ${index.method.uppercase(Locale.getDefault())} INDEX ${index.name} ON $kronosTableName USING ${index.type}(${
                        index.columns.joinToString(
                            ","
                        )
                    });"
                }

                // 执行创建表的SQL语句
                val sqls =
                    (listOf("CREATE TABLE IF NOT EXISTS $kronosTableName (${columnDefinitions}) ;") + indexDefinitions)
                        .filter { it.isNotBlank() }
                println(sqls)
                val result = sqls.sumOf { dataSource.update(KronosAtomicActionTask(it)) }
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
                    val identity = if (column.identity) " IDENTITY" else " "
                    // 默认值
                    val defaultValue = if (column.defaultValue != null) "DEFAULT ${column.defaultValue}" else ""
                    "$columnName $columnType $identity $defaultValue"
                }
                val indexDefinitions = kronosIndexes.map { index ->
                    "CREATE ${index.method} ${index.type} INDEX [${index.name}]\nON [dbo].[$kronosTableName] ([${
                        index.columns.joinToString(
                            "],["
                        )
                    }]);".let {
                        if (index.type == "XML") {
                            "CREATE ${index.method} PRIMARY ${index.type} INDEX [${index.name}] ON [dbo].[$kronosTableName] ([${
                                index.columns.joinToString(
                                    "],["
                                )
                            }]);"
                        } else {
                            it
                        }
                    }
                }
                // 执行创建表的SQL语句
                val sqls = listOf(
                    """
                     IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[$kronosTableName]') AND type in (N'U')) 
                     BEGIN
                    CREATE TABLE [dbo].[$kronosTableName]
                    (
                        $columnDefinitions
                    );
                END;
                     """
                ) + indexDefinitions
                println(sqls)
                val result = sqls.sumOf { dataSource.update(KronosAtomicActionTask(it)) }
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
                    val identity = if (column.identity) " AUTOINCREMENT" else " "
                    // 默认值
                    val defaultValue = if (column.defaultValue != null) "DEFAULT ${column.defaultValue}" else ""
                    "$columnName $columnType $identity $defaultValue"
                }
                // 索引 CREATE INDEX "dfsdf"
                //ON "_tb_user_old_20240617" (
                //  "password"
                //);
                val kronosIndexSqls = kronosIndexes.map { index ->
                    val columnsWithTypes = index.columns.map { column ->
                        if (index.type.isNotEmpty())
                            "$column COLLATE ${index.type}"
                        else
                            column
                    }
                    val columnsString = columnsWithTypes.joinToString(", ") { it }
                    "CREATE ${index.method} INDEX IF NOT EXISTS ${index.name} ON $kronosTableName (${columnsString});"
                }
                // 执行创建表的SQL语句
                val sqls = listOf(
                    "CREATE TABLE IF NOT EXISTS $kronosTableName (${columnDefinitions}) ;"
                ) + kronosIndexSqls

                val result = sqls.sumOf { dataSource.update(KronosAtomicActionTask(it)) }
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
                val sql = """
                    BEGIN
                       EXECUTE IMMEDIATE 'DROP TABLE ${kronosTableName}';
                    EXCEPTION
                       WHEN OTHERS THEN
                          IF SQLCODE != -942 THEN
                             RAISE;
                          END IF;
                    END;
                """.trimIndent()
                println(sql)
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
                        col.primaryKey,
                        col.identity
                    ) != convertToSqlColumnType(
                dbType,
                tableColumn.type,
                tableColumn.length,
                tableColumn.nullable,
                tableColumn.primaryKey,
                tableColumn.identity
            )
        }.filter {
            // 筛掉不在current中的列
            if (it.columnName !in current.map { field -> field.columnName }) {
                return@filter false
            } else {
                return@filter true
            }
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
        val kronosColumns = instance.kronosColumns().map { originalColumn ->
            if (dataSource.dbType == DBType.Oracle) {
                originalColumn.columnName = originalColumn.columnName.uppercase()
            }
            originalColumn
        }
        // 从实例中获取索引(oracle 需要 转大写)
        val kronosIndexes = instance.kronosTableIndex()
        // 数据库类型
        val dbType = dataSource.dbType

        // 不存在就创建
        if (!queryTableExistence(kronosTableName)) {
            return createTable(instance)
        }

        // 获取实际表字段信息
        val tableColumns = getTableColumns(kronosTableName)
        // 获取实际表索引信息
        val tableIndexes = getTableIndexes(kronosTableName)
        println("实际表索引信息$tableIndexes")
        println("实体类列信息" + kronosColumns.map { it.columnName })
        println("实际表字段信息" + tableColumns.map { it.columnName })
        println("实体类列nullable信息" + kronosColumns.map { it.nullable })
        println("实际表字段nullable信息" + tableColumns.map { it.nullable })
        // 新增、修改、删除字段
        var (toAdd, toModified, toDelete) = differ(dbType, kronosColumns, tableColumns)
        println("新增字段" + toAdd.map { it.columnName })
        println("修改字段" + toModified.map { it.columnName })
        println("删除字段" + toDelete.map { it.columnName })
        // 需要新增与删除的索引
        val (toAddIndex, todeleteIndex) = differIndex(kronosIndexes, tableIndexes)
        println("toAddIndex:   $toAddIndex")
        println("todeleteIndex:   $todeleteIndex")
        // 根据数据库类型执行不同的创建/修改表语句
        when (dbType) {
            DBType.Mysql -> {
                val listOfSql =
                    // 删除索引 避免删除列出错
                    todeleteIndex.map {
                        "ALTER TABLE $kronosTableName DROP INDEX ${it.name};"
                    } + toAdd.map {
                        "ALTER TABLE $kronosTableName ADD COLUMN ${it.columnName} ${
                            convertToSqlColumnType(
                                DBType.Mysql,
                                it.type,
                                it.length,
                                it.nullable,
                                it.primaryKey
                            )
                        } ${if (it.identity) "AUTO_INCREMENT" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""};"
                    } + toModified.map { field ->
                        "ALTER TABLE $kronosTableName MODIFY COLUMN ${field.columnName} ${
                            convertToSqlColumnType(
                                DBType.Mysql,
                                field.type,
                                field.length,
                                field.nullable,
                                field.primaryKey
                            )
                        } ${if (field.identity) "AUTO_INCREMENT" else ""} ${if (field.defaultValue != null) "DEFAULT '${field.defaultValue}'" else ""};"
                            .let {
                                // 判断主键是否有改动 如果有 先删除主键 DROP PRIMARY KEY
                                if (
                                    it.contains("PRIMARY KEY")
                                ) {
                                    "ALTER TABLE $kronosTableName DROP PRIMARY KEY;$it"
                                } else {
                                    it
                                }
                            }
                    } + toDelete.map {
                        "ALTER TABLE $kronosTableName DROP COLUMN ${it.columnName};"
                    } + toAddIndex.map {
                        "ALTER TABLE $kronosTableName ADD  ${it.type} INDEX ${it.name} (`${it.columns.joinToString("`, `")}`) USING ${it.method};"
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
                // toModified 取消 分给toDelete和 toAdd
                toDelete = toModified + toDelete
                toAdd = toAdd + toModified
                val listOfSql =
                    // 删除索引 避免删除列出错
                    todeleteIndex.map {
                        "DROP INDEX \"${dataSource.userName}\".\"${it.name}\""
                    } + toDelete.map {
                        "ALTER TABLE $kronosTableName DROP COLUMN \"${it.columnName}\""
                    } + toAdd.map {
                        "ALTER TABLE $kronosTableName ADD ${it.columnName} ${
                            convertToSqlColumnType(
                                DBType.Oracle,
                                it.type,
                                it.length,
                                it.nullable,
                                it.primaryKey,
                                it.identity
                            )
                        } ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""}"
                    } + toAddIndex.map {
                        "CREATE  ${it.type} INDEX ${it.name} ON \"${dataSource.userName}\".\"${kronosTableName}\" (\"${
                            it.columns.map {
                                it.uppercase(
                                    Locale.getDefault()
                                )
                            }.joinToString("\",\"")
                        }\")"
                    }
                println("listOfSql$listOfSql")
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

            DBType.Postgres -> {
                val listOfSql =
                    // 删除索引 避免删除列出错
                    todeleteIndex.map {
                        "DROP INDEX \"public\".${it.name};"
                    } + toAdd.map {
                        "ALTER TABLE \"public\".$kronosTableName ADD COLUMN ${it.columnName} ${
                            convertToSqlColumnType(
                                DBType.Postgres,
                                it.type,
                                it.length,
                                it.nullable,
                                it.primaryKey
                            )
                        } ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""} ${if (it.nullable) "NOT NULL" else ""};"
                    } + toModified.map {
                        "ALTER TABLE \"public\".$kronosTableName ALTER COLUMN ${it.columnName} TYPE ${
                            convertToSqlColumnType(
                                DBType.Postgres,
                                it.type,
                                0,
                                true,
                                false
                            )
                        } ${if (it.defaultValue != null) ",AlTER COLUMN ${it.columnName} SET DEFAULT ${it.defaultValue}" else ""} ${
                            if (it.nullable) ",ALTER COLUMN ${it.columnName} DROP NOT NULL" else ",ALTER COLUMN ${it.columnName} SET NOT NULL"
                        }"
                    } + toDelete.map {
                        "ALTER TABLE \"public\".$kronosTableName DROP COLUMN ${it.columnName};"
                    } + toAddIndex.map {
                        "CREATE ${if (it.method == "UNIQUE") "UNIQUE" else ""} INDEX ${it.name} ON \"public\".$kronosTableName USING ${it.type} (\"${
                            it.columns.joinToString(
                                "\", \""
                            )
                        }\");"
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

            DBType.Mssql -> {
                val listOfSql =
                    // 先删索引 避免删除列出错
                    todeleteIndex.map {
                        "DROP INDEX [${it.name}] ON [dbo].[$kronosTableName];"
                    } + toDelete.map {
                        // 删除默认值约束
                        """
                            DECLARE @ConstraintName NVARCHAR(128);
                            SET @ConstraintName = (
                                SELECT name
                                FROM sys.default_constraints
                                WHERE parent_object_id = OBJECT_ID(N'dbo.${kronosTableName}') 
                                AND COL_NAME(parent_object_id, parent_column_id) = N'${it.name}' 
                            );

                            IF @ConstraintName IS NOT NULL
                            BEGIN
                                DECLARE @DropStmt NVARCHAR(MAX) = N'ALTER TABLE dbo.${kronosTableName} DROP CONSTRAINT ' + QUOTENAME(@ConstraintName);
                                EXEC sp_executesql @DropStmt;
                            END
                            ELSE
                            BEGIN
                                PRINT 'No default constraint found on the specified column.';
                            END
                        """.trimIndent()
                    } + toDelete.map {
                        "ALTER TABLE [dbo].[$kronosTableName] DROP COLUMN [${it.columnName}];"
                    } + toAdd.map {
                        "ALTER TABLE $kronosTableName ADD [${it.columnName}] ${it.type} ${if (it.length > 0 && it.type != "TINYINT") "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""} ${if (it.nullable) "" else "NOT NULL"};"
                    } + toAddIndex.map {
                        "CREATE ${it.type} INDEX [${it.name}] ON [dbo].[$kronosTableName] ([${it.columns.joinToString("],[")}]);"
                            .let { value ->
                                if (it.type == "XML") {
                                    "CREATE PRIMARY ${it.type} INDEX [${it.name}] ON [dbo].[$kronosTableName] ([${
                                        it.columns.joinToString(
                                            "],["
                                        )
                                    }]);"
                                } else
                                    value
                            }
                    } + toModified.map {
                        // 删除默认值约束
                        """
                            DECLARE @ConstraintName NVARCHAR(128);
                            SET @ConstraintName = (
                                SELECT name
                                FROM sys.default_constraints
                                WHERE parent_object_id = OBJECT_ID(N'dbo.${kronosTableName}') 
                                AND COL_NAME(parent_object_id, parent_column_id) = N'${it.name}' 
                            );

                            IF @ConstraintName IS NOT NULL
                            BEGIN
                                DECLARE @DropStmt NVARCHAR(MAX) = N'ALTER TABLE dbo.${kronosTableName} DROP CONSTRAINT ' + QUOTENAME(@ConstraintName);
                                EXEC sp_executesql @DropStmt;
                            END
                            ELSE
                            BEGIN
                                PRINT 'No default constraint found on the specified column.';
                            END
                        """.trimIndent()
                    } + toModified.map {
                        "ALTER TABLE [dbo].[${kronosTableName}] ALTER COLUMN [${it.columnName}] ${it.type} ${if (it.length > 0 && it.type != "TINYINT") "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.nullable) "" else "NOT NULL"}"
                    }
                println(listOfSql)
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
                val listOfSql =
                    // 先删索引 避免删除列出错
                    todeleteIndex.map {
                        "DROP INDEX ${it.name};"
                    } + toAdd.map {
                        "ALTER TABLE $kronosTableName ADD COLUMN ${it.columnName} ${it.type} ${if (it.length > 0) "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""} ${if (it.nullable) "" else "NOT NULL"};"
                    } + toModified.map {
                        "ALTER TABLE $kronosTableName MODIFY COLUMN ${it.columnName} ${it.type} ${if (it.length > 0) "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""} ${if (it.nullable) "" else "NOT NULL"};"
                    } + toDelete.map {
                        "ALTER TABLE $kronosTableName DROP COLUMN ${it.columnName};"
                    } + toAddIndex.map {
                        // CREATE INDEX "aaa" ON "tb_user" ("username" COLLATE RTRIM )  如果${it.type}不是空 需要 在每个column后面加 COLLATE ${it.type} (${it.columns.joinToString(",")})需要改
                        "CREATE ${it.method} INDEX ${it.name} ON $kronosTableName (${
                            it.columns.map { column ->
                                if (it.type.isNotEmpty())
                                    "$column COLLATE ${it.type}"
                                else
                                    column
                            }
                                .joinToString(",")
                        });"
                    }
                println(listOfSql)
                println(listOfSql.size)
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


    fun differIndex(
        kronosIndexes: MutableList<KTableIndex>,
        tableIndexes: MutableList<KTableIndex>
    ): Pair<MutableList<KTableIndex>, MutableList<KTableIndex>> {
        // 直接将kronosIndexes中的所有索引视为需要添加的
        val toAdd = kronosIndexes.toMutableList()

        // 直接将tableIndexes中的所有索引视为需要删除的
        val toDelete = tableIndexes.toMutableList()

        return toAdd to toDelete
    }

    // 获取表索引名称
    fun getTableIndexes(kronosTableName: String): MutableList<KTableIndex> {
        return when (wrapper.orDefault().dbType) {
            DBType.Mysql -> {
                val sql = """
                    SELECT 
                        INDEX_NAME AS name
                    FROM 
                     INFORMATION_SCHEMA.STATISTICS
                    WHERE 
                     TABLE_SCHEMA = DATABASE() AND 
                     TABLE_NAME = '${kronosTableName}' AND 
                      INDEX_NAME != 'PRIMARY'  
                """
                dataSource.forList(KronosAtomicQueryTask(sql)).map {
                    val name = it["name"] as String
                    KTableIndex(name, arrayOf(), "", "")
                }.toMutableList()
            }

            DBType.Oracle -> {
                val sql = """
                SELECT DISTINCT i.INDEX_NAME AS NAME
                FROM ALL_INDEXES i
                JOIN ALL_IND_COLUMNS ic ON i.INDEX_NAME = ic.INDEX_NAME
                WHERE i.TABLE_NAME = UPPER('${kronosTableName}') 
                AND i.OWNER = '${wrapper.orDefault().userName}'
                AND i.INDEX_NAME NOT LIKE UPPER('SYS_%')
                """
                dataSource.forList(KronosAtomicQueryTask(sql)).map {
                    val name = it["NAME"] as String
                    KTableIndex(name, arrayOf(), "", "")
                }.toMutableList()
            }

            DBType.SQLite -> {
                val sql = "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name = 'tb_user';"

                dataSource.forList(KronosAtomicQueryTask(sql)).map {
                    val name = it["name"] as String
                    KTableIndex(name, emptyArray(), "", "")

                }.toMutableList()
            }

            DBType.Mssql -> {
                val sql = """
                    SELECT 
                        name AS name
                    FROM 
                     sys.indexes
                    WHERE 
                     object_id = object_id('tb_user') AND 
                     name NOT LIKE 'PK__${kronosTableName}__%'  
                """
                dataSource.forList(KronosAtomicQueryTask(sql)).map {
                    val name = it["name"] as String
                    KTableIndex(name, arrayOf(), "", "")
                }.toMutableList()
            }

            DBType.Postgres -> {
                val sql = """
                    SELECT 
                        indexname AS name
                    FROM 
                        pg_indexes 
                    WHERE 
                        tablename = 'tb_user' AND 
                        schemaname = 'public' AND 
                        indexname NOT LIKE CONCAT(tablename, '_pkey');
                     """
                dataSource.forList(KronosAtomicQueryTask(sql)).map {
                    val name = it["name"] as String
                    KTableIndex(name, arrayOf(), "", "")
                }.toMutableList()
            }

            else -> throw NotImplementedError("Unsupported database types")
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
                 c.TABLE_NAME = '${tableName}'
                """

            DBType.Oracle -> """
                WITH RankedColumns AS (
                    SELECT 
                        cols.column_name AS COLUMN_NAME,
                        cols.data_type AS DATE_TYPE,
                        cols.data_length AS LENGTH,
                        cols.nullable AS IS_NULLABLE,
                        cols.data_default AS COLUMN_DEFAULT,
                        CASE WHEN cons.constraint_type = 'P' THEN '1' ELSE '0' END AS PRIMARY_KEY,
                        ROW_NUMBER() OVER (PARTITION BY cols.column_name ORDER BY CASE WHEN cons.constraint_type = 'P' THEN 0 ELSE 1 END, cons.constraint_type) AS rn
                    FROM 
                        all_tab_columns cols
                    LEFT JOIN 
                        all_cons_columns cons_cols 
                        ON cols.owner = cons_cols.owner AND cols.table_name = cons_cols.table_name AND cols.column_name = cons_cols.column_name
                    LEFT JOIN 
                        all_constraints cons 
                        ON cols.owner = cons.owner AND cons_cols.constraint_name = cons.constraint_name AND cons_cols.table_name = cons.table_name
                    WHERE 
                        cols.table_name = '${tableName}' AND cols.OWNER = '${wrapper.orDefault().userName}'
                )
                SELECT 
                    COLUMN_NAME, DATE_TYPE, LENGTH, IS_NULLABLE, COLUMN_DEFAULT, PRIMARY_KEY
                FROM 
                    RankedColumns
                WHERE 
                    rn = 1
            """.trimIndent()

            DBType.Postgres -> """        
                SELECT 
                    c.column_name AS COLUMN_NAME,
                    CASE 
                        WHEN c.data_type IN ('character varying', 'varchar') THEN 'VARCHAR'
                        WHEN c.data_type IN ('integer', 'int') THEN 'INT'
                        WHEN c.data_type IN ('bigint') THEN 'BIGINT'
                        WHEN c.data_type IN ('smallint') THEN 'TINYINT'
                        WHEN c.data_type IN ('decimal', 'numeric') THEN 'DECIMAL'
                        WHEN c.data_type IN ('double precision', 'real') THEN 'DOUBLE'
                        WHEN c.data_type IN ('boolean') THEN 'BOOLEAN'
                        WHEN c.data_type LIKE 'timestamp%' THEN 'TIMESTAMP'
                        WHEN c.data_type LIKE 'date' THEN 'DATE'
                        ELSE c.data_type -- 对于未列出的类型，保留原始详细类型
                    END AS DATA_TYPE,
                    COALESCE(c.character_maximum_length, c.numeric_precision) AS LENGTH,
                    c.is_nullable = 'YES' AS IS_NULLABLE,
                    c.column_default AS COLUMN_DEFAULT,
                    EXISTS (
                        SELECT 1 
                        FROM information_schema.key_column_usage kcu
                        INNER JOIN information_schema.table_constraints tc 
                            ON kcu.constraint_name = tc.constraint_name
                            AND kcu.constraint_schema = tc.constraint_schema
                        WHERE 
                            tc.constraint_type = 'PRIMARY KEY' AND
                            kcu.table_schema = c.table_schema AND 
                            kcu.table_name = c.table_name AND 
                            kcu.column_name = c.column_name
                    ) OR (c.column_name = 'id' AND c.data_type LIKE 'serial%') AS PRIMARY_KEY
                FROM 
                    information_schema.columns c
                WHERE 
                    c.table_schema = current_schema() AND 
                    c.table_name = '${tableName}'
            """.trimIndent()

            DBType.Mssql -> """
                     SELECT 
                        c.COLUMN_NAME, 
                        c.DATA_TYPE, 
                        CASE 
                            WHEN c.DATA_TYPE IN ('char', 'nchar', 'varchar', 'nvarchar') THEN c.CHARACTER_MAXIMUM_LENGTH
                            ELSE NULL  
                        END AS CHARACTER_MAXIMUM_LENGTH,
                        c.IS_NULLABLE,
                        c.COLUMN_DEFAULT,
                        CASE 
                            WHEN EXISTS (
                                SELECT 1 
                                FROM INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu
                                INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc 
                                    ON ccu.Constraint_Name = tc.Constraint_Name 
                                    AND tc.Constraint_Type = 'PRIMARY KEY'
                                WHERE ccu.COLUMN_NAME = c.COLUMN_NAME AND ccu.TABLE_NAME = c.TABLE_NAME
                            ) THEN 'YES' ELSE 'NO' 
                        END AS PRIMARY_KEY
                    FROM 
                        INFORMATION_SCHEMA.COLUMNS c
                    WHERE 
                        c.TABLE_CATALOG = DB_NAME() AND 
                        c.TABLE_NAME = '${tableName}'
            """.trimIndent()

            DBType.SQLite -> "PRAGMA table_info('${tableName}')"

            DBType.DB2, DBType.Sybase, DBType.H2, DBType.OceanBase, DBType.DM8 -> throw NotImplementedError("Unsupported database types")
        }
        println(sql)
        return wrapper.orDefault().forList(KronosAtomicQueryTask(sql)).map {
            when (wrapper.orDefault().dbType) {
                DBType.Mysql, DBType.Mssql -> Field(
                    columnName = it["COLUMN_NAME"].toString(),
                    type = it["DATA_TYPE"].toString().uppercase(Locale.getDefault()),
                    length = it["LENGTH"] as Int? ?: 0,
                    tableName = tableName,
                    nullable = it["IS_NULLABLE"] == "YES",
                    primaryKey = it["PRIMARY_KEY"] == "YES",
                    defaultValue = it["COLUMN_DEFAULT"] as String?
                    // 查不出来 identity = it["IDENTITY"] as Boolean? ?: false
                )

                DBType.Oracle -> Field(
                    columnName = it["COLUMN_NAME"].toString(),
                    type = it["DATE_TYPE"].toString().uppercase(Locale.getDefault()),
                    length = (it["LENGTH"] as BigDecimal? ?: BigDecimal.ZERO).toInt(),
                    tableName = tableName.uppercase(Locale.getDefault()),
                    nullable = it["IS_NULLABLE"] == "Y",
                    primaryKey = it["PRIMARY_KEY"] == 1,
                    defaultValue = it["COLUMN_DEFAULT"] as String?
                    // 查不出来 identity = it["IDENTITY"] as Boolean? ?: false
                )

                DBType.Postgres -> Field(
                    columnName = it["column_name"].toString(),
                    type = it["data_type"].toString().uppercase(Locale.getDefault()),
                    length = it["length"] as Int? ?: 0,
                    tableName = tableName,
                    nullable = it["is_nullable"] == true,
                    primaryKey = it["primary_key"] == true,
                    // 如果defaultValue =  "('tb_user_id_seq'::regclass)" 设置成 null
                    defaultValue = (it["column_default"] as String?).let { o ->
                        if (o == "nextval('tb_user_id_seq'::regclass)")
                            null
                        else o
                    }
                )

                DBType.SQLite -> Field(
                    columnName = it["name"].toString(),
                    type = it["type"].toString().split('(')[0].uppercase(Locale.getDefault()), // 处理类型
                    length = 0,
                    tableName = tableName,
                    nullable = it["notnull"] as Int == 0, // 直接使用notnull字段判断是否可空
                    primaryKey = it["pk"] as Int == 1,
                    defaultValue = it["dflt_value"] as String?
                )

                else -> throw NotImplementedError("Unsupported database types")
            }
        }
    }

}