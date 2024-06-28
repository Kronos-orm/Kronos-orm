package com.kotlinorm.interfaces

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.sql.SqlManager.columnCreateDefSql
import com.kotlinorm.sql.SqlManager.indexCreateDefSql
import com.kotlinorm.sql.SqlManager.sqlColumnType

interface DatabasesSupport {
    fun getColumnType(type: KColumnType, length: Int): String

    fun getColumnCreateSql(dbType: DBType, column: Field): String =
        "${
            column.columnName
        }${
            " ${sqlColumnType(dbType, column.type, column.length)}"
        }${
            if (column.nullable) "" else " NOT NULL"
        }${
            if (column.primaryKey) " PRIMARY KEY" else ""
        }${
            if (column.identity) " AUTO_INCREMENT" else ""
        }${
            if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
        }"

    fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex) =
        "CREATE ${index.type} INDEX ${index.name} ON $tableName (${index.columns.joinToString(",")}) USING ${index.method}"

    fun getTableCreateSqlList(
        dbType: DBType,
        tableName: String,
        columns: List<Field>,
        indexes: List<KTableIndex>
    ): List<String> {
        val columnsSql = columns.joinToString(",") { columnCreateDefSql(dbType, it) }
        val indexesSql = indexes.map { indexCreateDefSql(dbType, tableName, it) }
        return listOf(
            "CREATE TABLE IF NOT EXISTS $tableName ($columnsSql)",
            *indexesSql.toTypedArray()
        )
    }
}