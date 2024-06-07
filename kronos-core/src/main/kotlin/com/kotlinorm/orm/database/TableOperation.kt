package com.kotlinorm.orm.database

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.Utils.getDBNameFromUrl
import com.kotlinorm.orm.delete.DeleteClause
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.execute
import kotlin.reflect.full.createInstance

class TableOperation(val wrapper: KronosDataSourceWrapper) {

    // val ds = Kronos.dataSource
    //
    //data class User(): KPojo
    //
    //// 1.  return if the table exists
    //ds.table.exist<User>() // return Kotlin.Boolean
    //// 2. create a table
    //ds.table.create<User>()
    //// 3. confirm to delete a table
    //ds.table.delete<User>().confirm()
    //// 4. confirm to sync the structure from kotlin code to database
    //ds.table.structureSync<User>().confirm()

    /**
     * 检查数据库中是否存在指定的表。
     *
     * 该函数通过创建指定类型的实例，获取表名，然后根据不同的数据库类型构造查询SQL，
     * 查询数据库中是否已有该表。支持的数据库类型包括MySQL、Oracle、PostgreSQL、Microsoft SQL Server、SQLite。
     * 对于不支持的数据库类型（DB2、Sybase、H2、OceanBase、DM8），目前尚未实现。
     *
     * @param T 泛型参数，约束为KPojo的子类，用于动态获取表名。
     * @return 如果数据库中存在该表，则返回true；否则返回false。
     */
    inline fun <reified T : KPojo> exists(): Boolean {
        // 获取数据源实例
        val dataSource = wrapper.orDefault()
        // 创建指定类型的实例，用于获取表名
        val instance = T::class.createInstance()
        // 通过实例获取表名
        // 表名
        val kronosTableName = instance.kronosTableName()
        // 根据不同的数据库类型构造查询SQL和参数映射
        val (sql, paramMap) = when (dataSource.dbType) {
            DBType.Mysql ->
                // MySQL查询表存在的SQL
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = :tableName AND table_schema = :tableSchema" to mapOf(
                    "tableName" to kronosTableName,
                    "tableSchema" to getDBNameFromUrl(dataSource.url)
                )

            DBType.Oracle ->
                // Oracle查询表存在的SQL
                "select count(*) from user_tables where table_name =upper(:tableName)" to mapOf(
                    "tableName" to kronosTableName
                )

            DBType.Postgres ->
                // PostgreSQL查询表存在的SQL
                "select count(*) from pg_class where relname = 'tablename';" to mapOf(
                    "tableName" to kronosTableName
                )

            DBType.Mssql ->
                // Microsoft SQL Server查询表存在的SQL
                "select count(1) from sys.objects where name = :tableName" to mapOf(
                    "tableName" to kronosTableName
                )

            DBType.SQLite ->
                // SQLite查询表存在的SQL
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = :tableName" to mapOf(
                    "tableName" to kronosTableName
                )

            DBType.DB2 -> TODO() // DB2数据库的实现待完成
            DBType.Sybase -> TODO() // Sybase数据库的实现待完成
            DBType.H2 -> TODO() // H2数据库的实现待完成
            DBType.OceanBase -> TODO() // OceanBase数据库的实现待完成
            DBType.DM8 -> TODO() // DM8数据库的实现待完成
        }
        // 执行查询操作，返回结果大于0表示表存在
        val result = dataSource.update(
            KronosAtomicActionTask(
                sql, paramMap
            )
        )
        return result > 0
    }


    /**
     * 根据给定的KPojo类动态创建数据库表。
     *
     * 该函数通过反射机制获取KPojo类的实例和元数据，根据不同的数据库类型生成并执行相应的SQL语句，
     * 从而在数据库中创建对应的表。支持的数据库类型包括MySQL、Oracle、PostgreSQL、Microsoft SQL Server、
     * SQLite、DB2、Sybase、H2、OceanBase和DM8。
     *
     * @param <T> KPojo类的类型参数，必须继承自KPojo。
     * @return 创建的表的实例。
     */
    inline fun <reified T : KPojo> createTable(): Table {
        // 获取数据源实例
        val dataSource = wrapper.orDefault()
        // 通过反射创建KPojo类的实例
        val instance = T::class.createInstance()
        // 从实例中获取表名
        // 表名
        val kronosTableName = instance.kronosTableName()
        // 从实例中获取列定义
        // 字段相关
        val kronosColumns = instance.kronosColumns()
        // 根据不同的数据库类型，生成并执行创建表的SQL语句
        return when (dataSource.dbType) {
            DBType.Mysql -> {
                // 生成MySQL的列定义字符串
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = column.type
                    val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                    "$columnName $columnType $primaryKey"
                }
                // 执行创建表的SQL语句
                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName ($columnDefinitions)"
                dataSource.update(KronosAtomicActionTask(sql))

                Table(kronosTableName)
            }

            DBType.Oracle -> {
                // 生成Oracle的列定义字符串
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = column.type // PostgreSQL通常不需要额外类型转换
                    val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                    "$columnName $columnType $primaryKey"
                }
                // 执行创建表的SQL语句
                val sql = "CREATE TABLE ${kronosTableName.toUpperCase()} ($columnDefinitions)"
                dataSource.update(KronosAtomicActionTask(sql))
                Table(kronosTableName)
            }

            DBType.Postgres -> {
                // 生成PostgreSQL的列定义字符串
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = column.type // PostgreSQL通常不需要额外类型转换
                    val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                    "$columnName $columnType $primaryKey"
                }
                // 执行创建表的SQL语句
                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName ($columnDefinitions)"
                dataSource.update(KronosAtomicActionTask(sql))
                Table(kronosTableName)
            }

            DBType.Mssql -> {
                // 生成Microsoft SQL Server的列定义字符串
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = column.type // 可能需要根据MSSQL调整类型
                    val primaryKey =
                        if (column.primaryKey) "CONSTRAINT PK_$kronosTableName PRIMARY KEY CLUSTERED ($columnName)" else ""
                    "$columnName $columnType $primaryKey"
                }
                // 执行创建表的SQL语句
                val sql =
                    "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$kronosTableName') AND type in (N'U')) BEGIN CREATE TABLE $kronosTableName ($columnDefinitions) END"
                dataSource.update(KronosAtomicActionTask(sql))
                Table(kronosTableName)
            }

            DBType.SQLite -> {
                // 生成SQLite的列定义字符串
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = column.type
                    val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                    "$columnName $columnType $primaryKey"
                }
                // 执行创建表的SQL语句
                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName ($columnDefinitions)"
                dataSource.update(KronosAtomicActionTask(sql))
                Table(kronosTableName)
            }

            DBType.DB2 -> TODO()
            DBType.Sybase -> TODO()
            DBType.H2 -> TODO()
            DBType.OceanBase -> TODO()
            DBType.DM8 -> TODO()
        }
    }

    /**
     * 删除指定表的函数，根据不同的数据库类型执行相应的删除语句。
     * 该函数使用了reified关键字，允许在运行时获取类型T的信息。
     *
     * @param T 类型参数，必须继承自KPojo，表示要删除的表对应的实体类。
     * @return 返回DeleteClause实例，表示删除操作的结果。
     */
    inline fun <reified T : KPojo> deleteTable(): DeleteClause<KPojo> {
        // 获取数据源实例
        val dataSource = wrapper.orDefault()
        // 创建T类型的实例，用于获取表名
        val instance = T::class.createInstance()
        // 表名
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
            DBType.DB2 -> TODO()
            DBType.Sybase -> TODO()
            DBType.H2 -> TODO()
            DBType.OceanBase -> TODO()
            DBType.DM8 -> TODO()
        }
    }

    /**
     * 根据指定的表结构同步数据库表，如果表不存在则创建。
     * 该函数使用了reified关键字，允许在运行时获取类型T的信息。
     *
     * @param T 类型参数，必须继承自KPojo，表示要同步的表对应的实体类。
     * @return 返回Table实例，表示同步操作的结果。
     */
    inline fun <reified T : KPojo> structureSync(): Table {
        // 获取数据源实例
        val dataSource = wrapper.orDefault()
        // 创建T类型的实例，用于获取表名和列信息
        val instance = T::class.createInstance()
        // 表名
        // 表名
        val kronosTableName = instance.kronosTableName()
        // 列信息
        // 字段相关
        val kronosColumns = instance.kronosColumns()

        // 根据数据库类型执行不同的创建表语句
        when (dataSource.dbType) {
            DBType.Mysql -> {
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = column.type
                    val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                    "$columnName $columnType $primaryKey"
                }
                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName ($columnDefinitions)"
                dataSource.update(KronosAtomicActionTask(sql))
                return Table(kronosTableName)
            }
            DBType.Oracle -> {
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = column.type
                    val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                    "$columnName $columnType $primaryKey"
                }
                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName ($columnDefinitions)"
                dataSource.update(KronosAtomicActionTask(sql))
                return Table(kronosTableName)
            }
            DBType.Postgres -> {
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = column.type
                    val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                    "$columnName $columnType $primaryKey"
                }
                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName ($columnDefinitions)"
                dataSource.update(KronosAtomicActionTask(sql))
                return Table(kronosTableName)
            }
            DBType.Mssql -> {
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = column.type
                    val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                    "$columnName $columnType $primaryKey"
                }
                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName ($columnDefinitions)"
                dataSource.update(KronosAtomicActionTask(sql))
                return Table(kronosTableName)
            }
            DBType.SQLite -> {
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = column.type
                    val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                    "$columnName $columnType $primaryKey"
                }
                val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName ($columnDefinitions)"
                dataSource.update(KronosAtomicActionTask(sql))
                return Table(kronosTableName)
            }
            DBType.DB2 -> TODO()
            DBType.Sybase -> TODO()
            DBType.H2 -> TODO()
            DBType.OceanBase -> TODO()
            DBType.DM8 -> TODO()
        }
    }

}