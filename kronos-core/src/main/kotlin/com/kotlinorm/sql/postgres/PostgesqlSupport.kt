package com.kotlinorm.sql.postgres

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.DatabasesSupport

object PostgesqlSupport : DatabasesSupport {
    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            KColumnType.BIT -> "BOOLEAN"
            KColumnType.TINYINT -> "SMALLINT"
            KColumnType.SMALLINT -> "SMALLINT"
            KColumnType.INT -> "INTEGER"
            KColumnType.MEDIUMINT -> "INTEGER"
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
            KColumnType.DATETIME -> "TIMESTAMP"
            KColumnType.TIMESTAMP -> "TIMESTAMP"
            KColumnType.BINARY -> "BYTEA"
            KColumnType.VARBINARY -> "BYTEA"
            KColumnType.LONGVARBINARY -> "BYTEA"
            KColumnType.BLOB -> "BYTEA"
            KColumnType.MEDIUMBLOB -> "BYTEA"
            KColumnType.LONGBLOB -> "BYTEA"
            KColumnType.CLOB -> "TEXT"
            KColumnType.JSON -> "JSON"
            KColumnType.ENUM -> "TEXT"
            KColumnType.NVARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCLOB -> "TEXT"
            KColumnType.UUID -> "UUID"
            KColumnType.SERIAL -> "SERIAL"
            KColumnType.YEAR -> "SMALLINT"
            KColumnType.SET -> "TEXT"
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
            if (column.identity) " SERIAL" else " ${getColumnType(column.type, column.length)}"
        }${
            if (column.nullable) "" else " NOT NULL"
        }${
            if (column.primaryKey) " PRIMARY KEY" else ""
        }${
            if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
        }"
    }

    override fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex): String {
        TODO("Not yet implemented")
    }
}