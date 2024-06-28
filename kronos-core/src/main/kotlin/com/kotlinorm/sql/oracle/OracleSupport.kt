package com.kotlinorm.sql.oracle

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.DatabasesSupport
import java.util.*

object OracleSupport : DatabasesSupport {
    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            KColumnType.BIT -> "NUMBER(1)"
            KColumnType.TINYINT -> "NUMBER"
            KColumnType.SMALLINT -> "NUMBER(5)"
            KColumnType.INT -> "NUMBER"
            KColumnType.MEDIUMINT -> "NUMBER(7)"
            KColumnType.BIGINT -> "NUMBER(19)"
            KColumnType.REAL -> "REAL"
            KColumnType.FLOAT -> "FLOAT"
            KColumnType.DOUBLE -> "DOUBLE"
            KColumnType.DECIMAL -> "DECIMAL"
            KColumnType.NUMERIC -> "NUMERIC"
            KColumnType.CHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.VARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.TEXT -> "CLOB"
            KColumnType.MEDIUMTEXT -> "CLOB"
            KColumnType.LONGTEXT -> "CLOB"
            KColumnType.DATE -> "DATE"
            KColumnType.TIME -> "DATE"
            KColumnType.DATETIME -> "DATE"
            KColumnType.TIMESTAMP -> "TIMESTAMP"
            KColumnType.BINARY -> "BLOB"
            KColumnType.VARBINARY -> "BLOB"
            KColumnType.LONGVARBINARY -> "BLOB"
            KColumnType.BLOB -> "BLOB"
            KColumnType.MEDIUMBLOB -> "BLOB"
            KColumnType.LONGBLOB -> "LONGBLOB"
            KColumnType.CLOB -> "CLOB"
            KColumnType.JSON -> "JSON"
            KColumnType.ENUM -> "ENUM"
            KColumnType.NVARCHAR -> "NVARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCHAR -> "NCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCLOB -> "NCLOB"
            KColumnType.UUID -> "CHAR(36)"
            KColumnType.SERIAL -> "NUMBER"
            KColumnType.YEAR -> "NUMBER"
            KColumnType.SET -> "SET"
            KColumnType.GEOMETRY -> "GEOMETRY"
            KColumnType.POINT -> "POINT"
            KColumnType.LINESTRING -> "LINESTRING"
            KColumnType.XML -> "XML"
            else -> "VARCHAR(255)"
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String {
        return "${
            column.columnName
        }${
            " ${getColumnType(column.type, column.length)}"
        }${
            if (column.nullable) "" else " NOT NULL"
        }${
            if (column.primaryKey) " PRIMARY KEY" else ""
        }${
            if (column.identity) " GENERATED ALWAYS AS IDENTITY" else ""
        }${
            if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
        }"
    }

    override fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex) =
        "CREATE ${index.type.uppercase(Locale.getDefault())} INDEX ${index.name} ON $tableName (${
            index.columns.joinToString(
                ", "
            )
        })"

    override fun getTableCreateSqlList(
        dbType: DBType,
        tableName: String,
        columns: List<Field>,
        indexes: List<KTableIndex>
    ): List<String> {
        val columnsSql = columns.joinToString(",") { getColumnCreateSql(dbType, it) }
        val indexesSql = indexes.map { getIndexCreateSql(dbType, tableName, it) }
        return listOf(
            "CREATE TABLE $tableName ($columnsSql)",
            *indexesSql.toTypedArray()
        )
    }
}