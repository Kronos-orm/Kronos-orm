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

import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KronosDataSourceWrapper

object DBHelper {
    fun getDBNameFromUrl(wrapper: KronosDataSourceWrapper): String {
        return when (wrapper.dbType) {
            DBType.Mysql -> wrapper.url.split("?").first().split("//")[1].split("/").last()
            DBType.SQLite -> wrapper.url.split("//").last()
            DBType.Oracle -> wrapper.url.split("@").last()
            DBType.Mssql -> wrapper.url.split("//").last().split(";").first()
            DBType.Postgres -> wrapper.url.split("//").last().split("/").first()
            else -> throw UnsupportedDatabaseTypeException()
        }
    }

    fun convertToSqlColumnType(
        dbType: DBType,
        type: String,
        length: Int,
        nullable: Boolean,
        primaryKey: Boolean
    ): String {
        return when (type) {
            "BIT" -> when (dbType) {
                DBType.Mysql -> "TINYINT(1)"
                DBType.Oracle -> "NUMBER(1)"
                DBType.Mssql -> "BIT"
                DBType.Postgres -> "BOOLEAN"
                DBType.SQLite -> "INTEGER"
                else -> throw RuntimeException("Unsupported database type for Boolean.")
            }

            "TINYINT" -> when (dbType) {
                DBType.Mysql -> "TINYINT"
                DBType.Oracle -> "NUMBER(3)"
                DBType.Mssql -> "TINYINT"
                DBType.Postgres -> "SMALLINT"
                DBType.SQLite -> "INTEGER"
                else -> throw RuntimeException("Unsupported database type for Byte.")
            }

            "SMALLINT" -> when (dbType) {
                DBType.Mysql -> "SMALLINT"
                DBType.Oracle -> "NUMBER(5)"
                DBType.Mssql -> "SMALLINT"
                DBType.Postgres -> "SMALLINT"
                DBType.SQLite -> "INTEGER"
                else -> throw RuntimeException("Unsupported database type for Short.")
            }

            "INT" -> when (dbType) {
                DBType.Mysql -> "INT"
                DBType.Oracle -> "NUMBER(10)"
                DBType.Mssql -> "INT"
                DBType.Postgres -> "INTEGER"
                DBType.SQLite -> "INTEGER"
                else -> throw RuntimeException("Unsupported database type for Int.")
            }

            "INTEGER" -> when (dbType) {
                DBType.SQLite -> "INTEGER"
                else -> throw RuntimeException("Unsupported database type for INTEGER.")
            }

            "BIGINT" -> when (dbType) {
                DBType.Mysql -> "BIGINT"
                DBType.Oracle -> "NUMBER(19)"
                DBType.Mssql -> "BIGINT"
                DBType.Postgres -> "BIGINT"
                DBType.SQLite -> "INTEGER"
                else -> throw RuntimeException("Unsupported database type for Long.")
            }

            "FLOAT" -> when (dbType) {
                DBType.Mysql -> "FLOAT"
                DBType.Oracle -> "FLOAT"
                DBType.Mssql -> "FLOAT"
                DBType.Postgres -> "REAL"
                DBType.SQLite -> "REAL"
                else -> throw RuntimeException("Unsupported database type for Float.")
            }

            "DOUBLE" -> when (dbType) {
                DBType.Mysql -> "DOUBLE"
                DBType.Oracle -> "DOUBLE"
                DBType.Mssql -> "FLOAT"
                DBType.Postgres -> "DOUBLE"
                DBType.SQLite -> "REAL"
                else -> throw RuntimeException("Unsupported database type for Double.")
            }

            "DECIMAL" -> when (dbType) {
                DBType.Mysql -> "DECIMAL"
                DBType.Oracle -> "NUMBER"
                DBType.Mssql -> "DECIMAL"
                DBType.Postgres -> "DECIMAL"
                DBType.SQLite -> "NUMERIC"
                else -> throw RuntimeException("Unsupported database type for BigDecimal.")
            }

            "CHAR" -> when (dbType) {
                DBType.Mysql -> "CHAR(1)"
                DBType.Oracle -> "CHAR"
                DBType.Mssql -> "CHAR"
                DBType.Postgres -> "CHAR"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for String.")
            }

            "VARCHAR" -> when (dbType) {
                DBType.Mysql -> "VARCHAR(${if (length == 0) 255 else length})"
                DBType.Oracle -> "VARCHAR(${if (length == 0) 255 else length})"
                DBType.Mssql -> "VARCHAR(${if (length == 0) 255 else length})"
                DBType.Postgres -> "VARCHAR(${if (length == 0) 255 else length})"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for String.")
            }

            "TEXT" -> when (dbType) {
                DBType.Mysql -> "TEXT"
                DBType.Oracle -> "CLOB"
                DBType.Mssql -> "TEXT"
                DBType.Postgres -> "TEXT"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for String.")
            }

            "LONGTEXT" -> when (dbType) {
                DBType.Mysql -> "TEXT"
                DBType.Oracle -> "CLOB"
                DBType.Mssql -> "TEXT"
                DBType.Postgres -> "TEXT"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for String.")
            }

            "DATE" -> when (dbType) {
                DBType.Mysql -> "DATE"
                DBType.Oracle -> "DATE"
                DBType.Mssql -> "DATE"
                DBType.Postgres -> "DATE"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for Date.")
            }

            "TIME" -> when (dbType) {
                DBType.Mysql -> "TIME"
                DBType.Oracle -> "DATE"
                DBType.Mssql -> "TIME"
                DBType.Postgres -> "TIME"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for Time.")
            }

            "DATETIME" -> when (dbType) {
                DBType.Mysql -> "DATETIME"
                DBType.Oracle -> "DATE"
                DBType.Mssql -> "DATETIME"
                DBType.Postgres -> "TIMESTAMP"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for Timestamp.")
            }

            "TIMESTAMP" -> when (dbType) {
                DBType.Mysql -> "TIMESTAMP"
                DBType.Oracle -> "DATE"
                DBType.Mssql -> "DATETIME"
                DBType.Postgres -> "TIMESTAMP"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for Timestamp.")
            }

            "BINARY" -> when (dbType) {
                DBType.Mysql -> "BINARY"
                DBType.Oracle -> "BLOB"
                DBType.Mssql -> "BINARY"
                DBType.Postgres -> "BYTEA"
                DBType.SQLite -> "BLOB"
                else -> throw RuntimeException("Unsupported database type for ByteArray.")
            }

            "VARBINARY" -> when (dbType) {
                DBType.Mysql -> "VARBINARY"
                DBType.Oracle -> "BLOB"
                DBType.Mssql -> "VARBINARY"
                DBType.Postgres -> "BYTEA"
                DBType.SQLite -> "BLOB"
                else -> throw RuntimeException("Unsupported database type for ByteArray.")
            }

            "LONGVARBINARY" -> when (dbType) {
                DBType.Mysql -> "LONGBLOB"
                DBType.Oracle -> "BLOB"
                DBType.Mssql -> "IMAGE"
                DBType.Postgres -> "BYTEA"
                DBType.SQLite -> "BLOB"
                else -> throw RuntimeException("Unsupported database type for ByteArray.")
            }

            "BLOB" -> when (dbType) {
                DBType.Mysql -> "BLOB"
                DBType.Oracle -> "BLOB"
                DBType.Mssql -> "IMAGE"
                DBType.Postgres -> "BYTEA"
                DBType.SQLite -> "BLOB"
                else -> throw RuntimeException("Unsupported database type for ByteArray.")
            }

            "CLOB" -> when (dbType) {
                DBType.Mysql -> "CLOB"
                DBType.Oracle -> "CLOB"
                DBType.Mssql -> "TEXT"
                DBType.Postgres -> "TEXT"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for String.")
            }

            "JSON" -> when (dbType) {
                DBType.Mysql -> "JSON"
                DBType.Oracle -> "JSON"
                DBType.Mssql -> "JSON"
                DBType.Postgres -> "JSON"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for String.")
            }

            "ENUM" -> when (dbType) {
                DBType.Mysql -> "ENUM"
                DBType.Oracle -> "ENUM"
                DBType.Mssql -> "ENUM"
                DBType.Postgres -> "TEXT"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for String.")
            }

            "NVARCHAR" -> when (dbType) {
                DBType.Mysql -> "NVARCHAR"
                DBType.Oracle -> "NVARCHAR"
                DBType.Mssql -> "NVARCHAR"
                DBType.Postgres -> "NVARCHAR"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for String.")
            }

            "NCHAR" -> when (dbType) {
                DBType.Mysql -> "NCHAR"
                DBType.Oracle -> "NCHAR"
                DBType.Mssql -> "NCHAR"
                DBType.Postgres -> "TEXT"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for String.")
            }

            "NCLOB" -> when (dbType) {
                DBType.Mysql -> "NCLOB"
                DBType.Oracle -> "NCLOB"
                DBType.Mssql -> "NTEXT"
                DBType.Postgres -> "TEXT"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for String.")
            }

            "UUID" -> when (dbType) {
                DBType.Mysql -> "CHAR"
                DBType.Oracle -> "CHAR"
                DBType.Mssql -> "CHAR"
                DBType.Postgres -> "UUID"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for UUID.")
            }

            "SERIAL" -> when (dbType) {
                DBType.Mysql -> "INT"
                DBType.Oracle -> "NUMBER"
                DBType.Mssql -> "INT"
                DBType.Postgres -> "SERIAL"
                DBType.SQLite -> "INTEGER"
                else -> throw RuntimeException("Unsupported database type for Serial.")
            }

            "Year" -> when (dbType) {
                DBType.Mysql -> "YEAR"
                DBType.Oracle -> "NUMBER"
                DBType.Mssql -> "INT"
                DBType.Postgres -> "INT"
                DBType.SQLite -> "INTEGER"
                else -> throw RuntimeException("Unsupported database type for Year.")
            }

            "MEDIUMINT" -> when (dbType) {
                DBType.Mysql -> "MEDIUMINT"
                DBType.Oracle -> "NUMBER(7)"
                DBType.Mssql -> "INT"
                DBType.Postgres -> "INTEGER"
                DBType.SQLite -> "INTEGER"
                else -> throw RuntimeException("Unsupported database type for MEDIUMINT.")
            }

            "NUMERIC" -> when (dbType) {
                DBType.Mysql -> "NUMERIC"
                DBType.Oracle -> "NUMBER"
                DBType.Mssql -> "DECIMAL"
                DBType.Postgres -> "DECIMAL"
                DBType.SQLite -> "NUMERIC"
                else -> throw RuntimeException("Unsupported database type for NUMERIC.")
            }

            "MEDIUMTEXT" -> when (dbType) {
                DBType.Mysql -> "MEDIUMTEXT"
                DBType.Oracle -> "CLOB"
                DBType.Mssql -> "TEXT"
                DBType.Postgres -> "TEXT"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for MEDIUMTEXT.")
            }

            "MEDIUMBLOB" -> when (dbType) {
                DBType.Mysql -> "MEDIUMBLOB"
                DBType.Oracle -> "BLOB"
                DBType.Mssql -> "IMAGE"
                DBType.Postgres -> "BYTEA"
                DBType.SQLite -> "BLOB"
                else -> throw RuntimeException("Unsupported database type for MEDIUMBLOB.")
            }

            "LONGBLOB" -> when (dbType) {
                DBType.Mysql -> "LONGBLOB"
                DBType.Oracle -> "BLOB"
                DBType.Mssql -> "IMAGE"
                DBType.Postgres -> "BYTEA"
                DBType.SQLite -> "BLOB"
                else -> throw RuntimeException("Unsupported database type for LONGBLOB.")
            }

            "SET" -> when (dbType) {
                DBType.Mysql -> "SET"
                DBType.Oracle -> "SET"
                DBType.Mssql -> "SET"
                DBType.Postgres -> "TEXT"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for SET.")
            }

            "GEOMETRY" -> when (dbType) {
                DBType.Mysql -> "GEOMETRY"
                DBType.Oracle -> "GEOMETRY"
                DBType.Mssql -> "GEOMETRY"
                DBType.Postgres -> "GEOMETRY"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException("Unsupported database type for GEOMETRY.")
            }

            "POINT" -> when (dbType) {
                DBType.Mysql -> "POINT"
                DBType.Oracle -> "POINT"
                DBType.Mssql -> "POINT"
                DBType.Postgres -> "POINT"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException()
            }

            "LINESTRING" -> when (dbType) {
                DBType.Mysql -> "LINESTRING"
                DBType.Oracle -> "LINESTRING"
                DBType.Mssql -> "LINESTRING"
                DBType.Postgres -> "LINESTRING"
                DBType.SQLite -> "TEXT"
                else -> throw RuntimeException()
            }
            // 这里可以继续添加其他类型的处理逻辑
            else -> throw RuntimeException("Unsupported type: $type")
        }.let {
            when {
                primaryKey -> it + " NOT NULL" + " PRIMARY KEY"
                !nullable -> it + " NOT NULL"
                else -> it
            }
        }
    }

}