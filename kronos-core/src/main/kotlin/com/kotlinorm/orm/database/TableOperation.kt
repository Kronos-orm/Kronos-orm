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

    // 查看数据库表是否存在
    inline fun <reified T : KPojo> exists(): Boolean {
        val dataSource = wrapper.orDefault()
        val instance = T::class.createInstance()
        // 表名
        val kronosTableName = instance.kronosTableName()
        val (sql, paramMap) = when (dataSource.dbType) {
            DBType.Mysql ->
                ("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = :tableName AND table_schema = :tableSchema" to mapOf(
                    "tableName" to kronosTableName,
                    "tableSchema" to getDBNameFromUrl(dataSource.url)
                ))

            DBType.Oracle ->
                ("select count(*) from user_tables where table_name =upper(:tableName)" to mapOf(
                    "tableName" to kronosTableName
                ))


            DBType.Postgres ->
                ("select count(*) from pg_class where relname = 'tablename';" to mapOf(
                    "tableName" to kronosTableName
                ))

            DBType.Mssql ->
                ("select count(1) from sys.objects where name = :tableName" to mapOf(
                    "tableName" to kronosTableName
                ))

            DBType.SQLite ->
                ("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = :tableName" to mapOf(
                    "tableName" to kronosTableName
                ))

            DBType.DB2 -> TODO() // Implement for DB2
            DBType.Sybase -> TODO() // Implement for Sybase
            DBType.H2 -> TODO() // Implement for H2
            DBType.OceanBase -> TODO() // Implement for OceanBase
            DBType.DM8 -> TODO() // Implement for DM8
        }

        val result = dataSource.update(
            KronosAtomicActionTask(
                sql, paramMap
            )
        )

        return result > 0
    }

    inline fun <reified T : KPojo> createTable(): Table {

        val dataSource = wrapper.orDefault()
        val instance = T::class.createInstance()
        // 表名
        val kronosTableName = instance.kronosTableName()
        // 字段相关
        val kronosColumns = instance.kronosColumns()
//        val columnNames = kronosColumns.map {
//            it.columnName
//        }
//        val types = kronosColumns.map {
//            it.type
//        }
//        val primaryKeys = kronosColumns.map {
//            it.primaryKey
//        }
        return when (dataSource.dbType) {
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
                val sql = "CREATE TABLE ${kronosTableName.toUpperCase()} ($columnDefinitions)"
                dataSource.update(KronosAtomicActionTask(sql))
                return Table(kronosTableName)
            }

            DBType.Postgres -> {
                val columnDefinitions = kronosColumns.joinToString(", ") { column ->
                    val columnName = column.columnName
                    val columnType = column.type // PostgreSQL通常不需要额外类型转换
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
                    val columnType = column.type // 可能需要根据MSSQL调整类型
                    val primaryKey = if (column.primaryKey) "CONSTRAINT PK_$kronosTableName PRIMARY KEY CLUSTERED ($columnName)" else ""
                    "$columnName $columnType $primaryKey"
                }
                val sql = "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$kronosTableName') AND type in (N'U')) BEGIN CREATE TABLE $kronosTableName ($columnDefinitions) END"
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

    inline fun <reified T : KPojo> deleteTable(): DeleteClause<KPojo> {

        val dataSource = wrapper.orDefault()
        val instance = T::class.createInstance()
        // 表名
        val kronosTableName = instance.kronosTableName()
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
                val sql = "IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$kronosTableName') AND type in (N'U')) BEGIN DROP TABLE $kronosTableName END"
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

    inline fun <reified T : KPojo> structureSync(): Table {

        val dataSource = wrapper.orDefault()
        val instance = T::class.createInstance()
        // 表名
        val kronosTableName = instance.kronosTableName()
        // 字段相关
        val kronosColumns = instance.kronosColumns()

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