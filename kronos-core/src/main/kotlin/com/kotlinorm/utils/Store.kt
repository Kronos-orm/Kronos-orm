package com.kotlinorm.utils

import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException

fun getTableInfo(dbType: DBType , tableName: String): String {
    return when (dbType) {
        DBType.Mysql -> "show full fields from `${tableName}`"
        DBType.SQLite -> "PRAGMA table_info($tableName)"
        DBType.Oracle -> "SELECT COLUMN_NAME as Field, DATA_TYPE as Type FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '$tableName'"
        DBType.Mssql -> "SELECT COLUMN_NAME as Field, DATA_TYPE as Type FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '$tableName'"
        DBType.Postgres -> "SELECT COLUMN_NAME as Field, DATA_TYPE as Type FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '$tableName'"
        else -> throw UnsupportedDatabaseTypeException()
    }
}