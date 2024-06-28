package com.kotlinorm.sql.mssql

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.DatabasesSupport

object MssqlSupport : DatabasesSupport {
    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            KColumnType.BIT -> "BIT"
            KColumnType.TINYINT -> "TINYINT"
            KColumnType.SMALLINT -> "SMALLINT"
            KColumnType.INT -> "INT"
            KColumnType.MEDIUMINT -> "INT"
            KColumnType.BIGINT -> "BIGINT"
            KColumnType.REAL -> "REAL"
            KColumnType.FLOAT -> "FLOAT"
            KColumnType.DOUBLE -> "DOUBLE"
            KColumnType.DECIMAL -> "DECIMAL"
            KColumnType.NUMERIC -> "NUMERIC"
            KColumnType.CHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.VARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.TEXT -> "TEXT"
            KColumnType.MEDIUMTEXT -> "TEXT"
            KColumnType.LONGTEXT -> "TEXT"
            KColumnType.DATE -> "DATE"
            KColumnType.TIME -> "TIME"
            KColumnType.DATETIME -> "DATETIME"
            KColumnType.TIMESTAMP -> "TIMESTAMP"
            KColumnType.BINARY -> "BINARY"
            KColumnType.VARBINARY -> "VARBINARY"
            KColumnType.LONGVARBINARY -> "IMAGE"
            KColumnType.BLOB -> "IMAGE"
            KColumnType.MEDIUMBLOB -> "IMAGE"
            KColumnType.LONGBLOB -> "IMAGE"
            KColumnType.CLOB -> "TEXT"
            KColumnType.JSON -> "JSON"
            KColumnType.ENUM -> "ENUM"
            KColumnType.NVARCHAR -> "NVARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCHAR -> "NCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCLOB -> "NTTEXT"
            KColumnType.UUID -> "CHAR(36)"
            KColumnType.SERIAL -> "INT"
            KColumnType.YEAR -> "INT"
            KColumnType.SET -> "SET"
            KColumnType.GEOMETRY -> "GEOMETRY"
            KColumnType.POINT -> "POINT"
            KColumnType.LINESTRING -> "LINESTRING"
            KColumnType.XML -> "XML"
            else -> "NVARCHAR(255)"
        }
    }

    override fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex): String {
        return "CREATE ${index.method}${
            if (index.type == "XML") {
                " PRIMARY"
            } else ""
        } ${index.type} INDEX [${index.name}] ON [dbo].[$tableName] ([${
            index.columns.joinToString(
                "],["
            )
        }])"
    }

    //"IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[$kronosTableName]') AND type in (N'U')) BEGIN CREATE TABLE [dbo].[$kronosTableName]($columnDefinitions)"


    override fun getTableCreateSqlList(
        dbType: DBType, tableName: String, columns: List<Field>, indexes: List<KTableIndex>
    ): List<String> {
        val columnsSql = columns.joinToString(",") { getColumnCreateSql(dbType, it) }
        val indexesSql = indexes.map { getIndexCreateSql(dbType, tableName, it) }
        return listOf(
            "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[$tableName]') AND type in (N'U')) BEGIN CREATE TABLE [dbo].[$tableName]($columnsSql) go",
            *indexesSql.toTypedArray()
        )
    }
}