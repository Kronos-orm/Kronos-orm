package com.kotlinorm.orm.database

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.Utils.convertToSqliteColumnType
import com.kotlinorm.orm.database.Utils.getDBNameFromUrl
import com.kotlinorm.orm.delete.DeleteClause
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.execute
import java.util.*
import javax.activation.DataSource
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
     * 使用 reified 关键字支持泛型的类型擦除，使得可以在运行时获取泛型类型 T 的信息。
     * 该方法主要用于 KPojo 类型的实例，通过创建一个实例来获取表名，然后检查表是否存在。
     *
     * @param T 泛型类型，继承自 KPojo，用于获取表名。
     * @return Boolean 表示表是否存在的布尔值。
     */
    inline fun <reified T : KPojo> exists(): Boolean {
        // 获取数据源实例
        val dataSource = wrapper.orDefault()
        // 创建指定类型的实例，用于获取表名
        val instance = T::class.createInstance()
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
        // 获取数据源实例
        // 根据不同的数据库类型构造查询SQL和参数映射
        val dataSource = wrapper.orDefault()
        // 根据数据库类型构造 SQL 和参数映射
        val (sql, paramMap) = when (dataSource.dbType) {
            DBType.Mysql ->
                // MySQL 的查询 SQL，统计表信息表中指定表名和数据库名的行数
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = :tableName AND table_schema = :tableSchema" to mapOf(
                    "tableName" to tableName,
                    "tableSchema" to getDBNameFromUrl(dataSource.url)
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
        val kronosTableName = instance.kronosTableName()
        // 从实例中获取列定义
        val kronosColumns = instance.kronosColumns()
        // 根据不同的数据库类型，生成并执行创建表的SQL语句
        return when (dataSource.dbType) {
            DBType.Mysql -> {
                // 生成MySQL的列定义字符串
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = convertToSqliteColumnType(DBType.Mysql,column.type)
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
                    val columnType = convertToSqliteColumnType(DBType.Oracle,column.type) // PostgreSQL通常不需要额外类型转换
                    val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                    "$columnName $columnType $primaryKey"
                }
                // 执行创建表的SQL语句
                val sql = "CREATE TABLE ${kronosTableName.uppercase(Locale.getDefault())} ($columnDefinitions)"
                dataSource.update(KronosAtomicActionTask(sql))
                Table(kronosTableName)
            }

            DBType.Postgres -> {
                // 生成PostgreSQL的列定义字符串
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = convertToSqliteColumnType(DBType.Postgres,column.type) // PostgreSQL通常不需要额外类型转换
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
                    val columnType = convertToSqliteColumnType(DBType.Mssql,column.type) // 可能需要根据MSSQL调整类型
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
                    val columnType = convertToSqliteColumnType(DBType.SQLite,column.type)
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
     * 根据指定的表结构同步（更新）数据库表，如果表不存在则创建，表如果存在，数据对不上则修改。
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
        val kronosTableName = instance.kronosTableName()
        // 列信息
        // 字段相关
        val kronosColumns = instance.kronosColumns()

        // 根据数据库类型执行不同的创建/修改表语句
        return when (dataSource.dbType) {
            DBType.Mysql -> {
                // 不存在就创建
                if (!queryTableExistence(kronosTableName)) {
                    val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                        val columnName = column.columnName
                        val columnType = convertToSqliteColumnType(DBType.Mysql,column.type)
                        val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                        "$columnName $columnType $primaryKey"
                    }
                    // 执行创建表的SQL语句
                    val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName ($columnDefinitions)"
                    dataSource.update(KronosAtomicActionTask(sql))
                }
                // 存在就检查是否能对应上
                if (queryTableExistence(kronosTableName)) {
                    // 通过反射创建KPojo类的实例
                    val tableStructure = T::class.createInstance()
                    // 获取Fields
                    val fields = tableStructure.kronosColumns()
                    // 获取表字段
                    val tableColumns = fields.map { it.columnName }
                    // 获取实体字段
                    val entityColumns = fields.map { it.name }
                    // 获取新增字段
                    val newColumns = entityColumns.filter { !tableColumns.contains(it) }
                    // 获取删除字段
                    val deleteColumns = tableColumns.filter { !entityColumns.contains(it) }
                    if (newColumns.isNotEmpty()) {
                        // 创建新字段
                        val newColumnDefinitions = newColumns.joinToString(", ") { column ->
                            val columnName = column
                            val columnType = kronosColumns.first { it.columnName == column }.type
                            val primaryKey =
                                if (kronosColumns.first { it.columnName == column }.primaryKey) "PRIMARY KEY" else ""
                            "$columnName $columnType $primaryKey"
                        }
                        val sql = "ALTER TABLE $kronosTableName ADD COLUMN $newColumnDefinitions"
                        dataSource.update(KronosAtomicActionTask(sql))
                    }
                    if (deleteColumns.isNotEmpty()) {
                        // 删除旧字段
                        val deleteColumnDefinitions = deleteColumns.joinToString(", ") { column ->
                            val columnName = column
                            "$columnName"
                        }
                        val sql = "ALTER TABLE $kronosTableName DROP COLUMN $deleteColumnDefinitions"
                        dataSource.update(KronosAtomicActionTask(sql))
                    }
                }
                Table(kronosTableName)
            }

            DBType.Oracle -> {
                // 不存在就创建
                if (!queryTableExistence(kronosTableName)) {
                    val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                        val columnName = column.columnName
                        val columnType = convertToSqliteColumnType(DBType.Oracle,column.type)
                        val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                        "$columnName $columnType $primaryKey"
                    }
                    // 执行创建表的SQL语句
                    val sql = "CREATE TABLE $kronosTableName ($columnDefinitions)"
                    dataSource.update(KronosAtomicActionTask(sql))
                }
                // 存在就检查是否能对应上
                if (queryTableExistence(kronosTableName)) {
                    // 通过反射创建KPojo类的实例
                    val tableStructure = T::class.createInstance()
                    // 获取Fields
                    val fields = tableStructure.kronosColumns()
                    // 获取表字段
                    val tableColumns = fields.map { it.columnName }
                    // 获取实体字段
                    val entityColumns = fields.map { it.name }

                    // 获取新增字段并逐个添加
                    val newColumns = entityColumns.filter { !tableColumns.contains(it) }
                    newColumns.forEach { column ->
                        val columnDef = kronosColumns.first { it.columnName == column }
                        val sql =
                            "ALTER TABLE $kronosTableName ADD COLUMN ${columnDef.columnName} ${columnDef.type} ${if (columnDef.primaryKey) "PRIMARY KEY" else ""}"
                        dataSource.update(KronosAtomicActionTask(sql))
                    }

                    // 获取删除字段并逐个删除
                    val deleteColumns = tableColumns.filter { !entityColumns.contains(it) }
                    deleteColumns.forEach { column ->
                        val sql = "ALTER TABLE $kronosTableName DROP COLUMN $column"
                        dataSource.update(KronosAtomicActionTask(sql))
                    }
                }
                Table(kronosTableName)
            }

            DBType.Postgres -> {
                if (!queryTableExistence(kronosTableName)) {
                    val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                        val columnName = column.columnName
                        val columnType = convertToSqliteColumnType(DBType.Postgres,column.type)
                        val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                        "$columnName $columnType $primaryKey"
                    }
                    val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName ($columnDefinitions)"
                    dataSource.update(KronosAtomicActionTask(sql))
                }
                if (queryTableExistence(kronosTableName)) {
                    val tableStructure = T::class.createInstance()
                    val fields = tableStructure.kronosColumns()
                    val tableColumns = fields.map { it.columnName }
                    val entityColumns = fields.map { it.name }

                    val newColumns = entityColumns.filter { !tableColumns.contains(it) }
                    newColumns.forEach { column ->
                        val columnDef = kronosColumns.first { it.columnName == column }
                        val sql =
                            "ALTER TABLE $kronosTableName ADD COLUMN ${columnDef.columnName} ${columnDef.type} ${if (columnDef.primaryKey) "PRIMARY KEY" else ""}"
                        dataSource.update(KronosAtomicActionTask(sql))
                    }

                    val deleteColumns = tableColumns.filter { !entityColumns.contains(it) }
                    deleteColumns.forEach { column ->
                        val sql = "ALTER TABLE $kronosTableName DROP COLUMN IF EXISTS $column"
                        dataSource.update(KronosAtomicActionTask(sql))
                    }
                }
                Table(kronosTableName)
            }


            DBType.Mssql -> {
                if (!queryTableExistence(kronosTableName)) {
                    val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                        val columnName = column.columnName
                        val columnType = convertToSqliteColumnType(DBType.Mssql,column.type)
                        "$columnName $columnType NULL" // MSSQL 默认允许NULL
                    }
                    val primaryKeySql = kronosColumns.filter { it.primaryKey }
                        .joinToString(",\n") { "CONSTRAINT PK_${kronosTableName}_$it.columnName PRIMARY KEY CLUSTERED ($it.columnName)" }

                    val sql = """
                    IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$kronosTableName') AND type in (N'U'))
                    BEGIN
                         CREATE TABLE $kronosTableName (
                              $columnDefinitions
                             ${primaryKeySql.takeIf { it.isNotBlank() } ?: ""}
                         );
                    END;
                """.trimIndent()
                    dataSource.update(KronosAtomicActionTask(sql))
                }
                if (queryTableExistence(kronosTableName)) {
                    val tableStructure = T::class.createInstance()
                    val fields = tableStructure.kronosColumns()
                    val tableColumns = fields.map { it.columnName }
                    val entityColumns = fields.map { it.name }

                    // 添加新列
                    val newColumns = entityColumns.filter { !tableColumns.contains(it) }
                    newColumns.forEach { column ->
                        val columnDef = kronosColumns.first { it.columnName == column }
                        val sql =
                            "ALTER TABLE $kronosTableName ADD COLUMN ${columnDef.columnName} ${columnDef.type} NULL"
                        dataSource.update(KronosAtomicActionTask(sql))
                    }

                    // 删除列（SQL Server 支持直接 DROP COLUMN）
                    val deleteColumns = tableColumns.filter { !entityColumns.contains(it) }
                    deleteColumns.forEach { column ->
                        val sql = "ALTER TABLE $kronosTableName DROP COLUMN $column"
                        dataSource.update(KronosAtomicActionTask(sql))
                    }
                }
                Table(kronosTableName)
            }

            DBType.SQLite -> {
                if (!queryTableExistence(kronosTableName)) {
                    val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                        val columnName = column.columnName
                        val columnType = convertToSqliteColumnType(DBType.SQLite,column.type)
                        val primaryKey = if (column.primaryKey) "PRIMARY KEY" else ""
                        "$columnName $columnType $primaryKey"
                    }
                    val sql = "CREATE TABLE IF NOT EXISTS $kronosTableName ($columnDefinitions)"
                    dataSource.update(KronosAtomicActionTask(sql))
                }
                if (queryTableExistence(kronosTableName)) {
                    val tableStructure = T::class.createInstance()
                    val fields = tableStructure.kronosColumns()
                    val tableColumns = fields.map { it.columnName }
                    val entityColumns = fields.map { it.name }

                    val newColumns = entityColumns.filter { !tableColumns.contains(it) }
                    newColumns.forEach { column ->
                        val columnDef = kronosColumns.first { it.columnName == column }
                        val sql =
                            "ALTER TABLE $kronosTableName ADD COLUMN ${columnDef.columnName} ${columnDef.type} ${if (columnDef.primaryKey) "PRIMARY KEY" else ""}"
                        dataSource.update(KronosAtomicActionTask(sql))
                    }
                    // SQLite在较新版本中支持DROP COLUMN，但老版本可能不支持
                    val deleteColumns = tableColumns.filter { !entityColumns.contains(it) }
                    deleteColumns.forEach { column ->
                        val sql = "ALTER TABLE $kronosTableName DROP COLUMN $column"
                        dataSource.update(KronosAtomicActionTask(sql))
                    }
                }
                Table(kronosTableName)
            }

            DBType.DB2 -> TODO()
            DBType.Sybase -> TODO()
            DBType.H2 -> TODO()
            DBType.OceanBase -> TODO()
            DBType.DM8 -> TODO()
        }
    }

}