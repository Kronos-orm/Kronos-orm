package com.kotlinorm.orm.database

import com.kotlinorm.beans.dsw.NoneDataSourceWrapper.dbType
import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException

object Utils {
    fun getDBNameFromUrl(url: String): String {
        return when (dbType) {
            DBType.Mysql -> url.split("?").first().split("//")[1]
            DBType.SQLite -> url.split("//").last()
            DBType.Oracle -> url.split("@").last()
            DBType.Mssql -> url.split("//").last().split(";").first()
            DBType.Postgres -> url.split("//").last().split("/").first()
            else -> throw UnsupportedDatabaseTypeException()
        }
    }
}