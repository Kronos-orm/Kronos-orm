package com.kotlinorm.database.mysql

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getKotlinColumnType
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.*
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff

object MysqlSupport : DatabasesSupport {
    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            BIT -> "TINYINT(1)"
            TINYINT -> "TINYINT"
            SMALLINT -> "SMALLINT"
            INT, SERIAL -> "INT"
            MEDIUMINT -> "MEDIUMINT"
            BIGINT -> "BIGINT"
            REAL -> "REAL"
            FLOAT -> "FLOAT"
            DOUBLE -> "DOUBLE"
            DECIMAL -> "DECIMAL"
            NUMERIC -> "NUMERIC"
            CHAR, NCHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            VARCHAR, NVARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            TEXT, XML -> "TEXT"
            MEDIUMTEXT -> "MEDIUMTEXT"
            LONGTEXT -> "LONGTEXT"
            DATE -> "DATE"
            TIME -> "TIME"
            DATETIME -> "DATETIME"
            TIMESTAMP -> "TIMESTAMP"
            BINARY -> "BINARY"
            VARBINARY -> "VARBINARY"
            LONGVARBINARY, LONGBLOB -> "LONGBLOB"
            BLOB -> "BLOB"
            MEDIUMBLOB -> "MEDIUMBLOB"
            CLOB -> "CLOB"
            JSON -> "JSON"
            ENUM -> "ENUM"
            NCLOB -> "NCLOB"
            UUID -> "CHAR(36)"
            YEAR -> "YEAR"
            SET -> "SET"
            GEOMETRY -> "GEOMETRY"
            POINT -> "POINT"
            LINESTRING -> "LINESTRING"
            else -> "VARCHAR(255)"
        }
    }

    override fun getTableExistenceSql(dbType: DBType) =
        "SELECT COUNT(1) FROM information_schema.tables WHERE table_name = :tableName AND table_schema = :dbName"

    override fun getTableColumns(dataSource: KronosDataSourceWrapper, tableName: String): List<Field> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                SELECT 
                    c.COLUMN_NAME, 
                    c.DATA_TYPE, 
                    c.CHARACTER_MAXIMUM_LENGTH LENGTH, 
                    c.IS_NULLABLE,
                    c.COLUMN_DEFAULT,
                    (CASE WHEN c.EXTRA = 'auto_increment' THEN 'YES' ELSE 'NO' END) AS IDENTITY,
                    (CASE WHEN c.COLUMN_KEY = 'PRI' THEN 'YES' ELSE 'NO' END) AS PRIMARY_KEY
                FROM 
                    INFORMATION_SCHEMA.COLUMNS c
                WHERE 
                 c.TABLE_SCHEMA = DATABASE() AND 
                 c.TABLE_NAME = :tableName
            """.trimIndent(),
                mapOf("tableName" to tableName)
            )
        ).map {
            Field(
                columnName = it["COLUMN_NAME"].toString(),
                type = getKotlinColumnType(
                    DBType.Mysql,
                    it["DATA_TYPE"].toString(),
                    (it["LENGTH"] as Long? ?: 0).toInt()
                ),
                length = (it["LENGTH"] as Long? ?: 0).toInt(),
                tableName = tableName,
                nullable = it["IS_NULLABLE"] == "YES",
                primaryKey = it["PRIMARY_KEY"] == "YES",
                identity = it["IDENTITY"] == "YES",
                defaultValue = it["COLUMN_DEFAULT"] as String?
            )
        }
    }

    override fun getTableIndexes(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ): List<KTableIndex> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                SELECT DISTINCT
                    INDEX_NAME AS name
                FROM 
                 INFORMATION_SCHEMA.STATISTICS
                WHERE 
                 TABLE_SCHEMA = DATABASE() AND 
                 TABLE_NAME = :tableName AND 
                 INDEX_NAME != 'PRIMARY'  
                """, mapOf(
                    "tableName" to tableName
                )
            )
        ).map {
            KTableIndex(it["name"] as String, arrayOf(), "", "")
        }
    }

    override fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        columns: TableColumnDiff,
        indexes: TableIndexDiff,
    ): List<String> {
        return indexes.toDelete.map {
            "ALTER TABLE $tableName DROP INDEX ${it.name}"
        } + columns.toAdd.map {
            "ALTER TABLE $tableName ADD COLUMN ${
                columnCreateDefSql(
                    DBType.Mysql, it
                )
            }"
        } + columns.toModified.map {
            "ALTER TABLE $tableName MODIFY COLUMN ${
                columnCreateDefSql(
                    DBType.Mysql, it
                )
            } ${if (it.primaryKey) ", DROP PRIMARY KEY, ADD PRIMARY KEY (`${it.columnName}`)" else ""}"
        } + columns.toDelete.map {
            "ALTER TABLE $tableName DROP COLUMN ${it.columnName}"
        } + indexes.toAdd.map {
            "ALTER TABLE $tableName ADD ${it.type} INDEX ${it.name} (`${it.columns.joinToString("`, `")}`) USING ${it.method}"
        }
    }
}