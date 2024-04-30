package com.kotoframework.enums

enum class DBType {
    Mysql,
    Oracle,
    Postgres,
    Mssql,
    SQLite,
    DB2,
    Sybase,
    H2,
    OceanBase,
    DM8;

    companion object {
        fun fromName(name: String) = entries.firstOrNull { it.name.uppercase() == name.uppercase() }
    }
}