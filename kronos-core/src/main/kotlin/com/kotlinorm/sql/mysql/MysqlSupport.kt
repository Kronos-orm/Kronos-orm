package com.kotlinorm.sql.mysql

import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.DatabasesSupport

object MysqlSupport : DatabasesSupport {
    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            KColumnType.BIT -> "TINYINT(1)"
            KColumnType.TINYINT -> "TINYINT"
            KColumnType.SMALLINT -> "SMALLINT"
            KColumnType.INT -> "INT"
            KColumnType.MEDIUMINT -> "MEDIUMINT"
            KColumnType.BIGINT -> "BIGINT"
            KColumnType.REAL -> "REAL"
            KColumnType.FLOAT -> "FLOAT"
            KColumnType.DOUBLE -> "DOUBLE"
            KColumnType.DECIMAL -> "DECIMAL"
            KColumnType.NUMERIC -> "NUMERIC"
            KColumnType.CHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.VARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.TEXT -> "TEXT"
            KColumnType.MEDIUMTEXT -> "MEDIUMTEXT"
            KColumnType.LONGTEXT -> "LONGTEXT"
            KColumnType.DATE -> "DATE"
            KColumnType.TIME -> "TIME"
            KColumnType.DATETIME -> "DATETIME"
            KColumnType.TIMESTAMP -> "TIMESTAMP"
            KColumnType.BINARY -> "BINARY"
            KColumnType.VARBINARY -> "VARBINARY"
            KColumnType.LONGVARBINARY -> "LONGBLOB"
            KColumnType.BLOB -> "BLOB"
            KColumnType.MEDIUMBLOB -> "MEDIUMBLOB"
            KColumnType.LONGBLOB -> "LONGBLOB"
            KColumnType.CLOB -> "CLOB"
            KColumnType.JSON -> "JSON"
            KColumnType.ENUM -> "ENUM"
            KColumnType.NVARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCLOB -> "NCLOB"
            KColumnType.UUID -> "CHAR(36)"
            KColumnType.SERIAL -> "INT"
            KColumnType.YEAR -> "YEAR"
            KColumnType.SET -> "SET"
            KColumnType.GEOMETRY -> "GEOMETRY"
            KColumnType.POINT -> "POINT"
            KColumnType.LINESTRING -> "LINESTRING"
            KColumnType.XML -> "TEXT"
            else -> "VARCHAR(255)"
        }
    }
}