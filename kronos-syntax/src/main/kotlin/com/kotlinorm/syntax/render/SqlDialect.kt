/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

data class SqlDialect(
    val leftQuote: String = "\"",
    val rightQuote: String = "\"",
    val standardEscapeStrings: Boolean = true,
    val limitStyle: SqlLimitStyle = SqlLimitStyle.Fetch,
    val family: SqlDialectFamily = SqlDialectFamily.Standard,
    val nativeBooleanValues: Boolean = false,
    val timestampParametersAsSqlTimestamp: Boolean = true,
    val datetimeParametersAsSqlTimestamp: Boolean = false
) {
    companion object {
        val Standard = SqlDialect()
        val MySql = SqlDialect("`", "`", standardEscapeStrings = false, limitStyle = SqlLimitStyle.LimitOffset, family = SqlDialectFamily.MySql)
        val PostgreSql = SqlDialect(
            limitStyle = SqlLimitStyle.LimitOffset,
            family = SqlDialectFamily.PostgreSql,
            nativeBooleanValues = true,
            datetimeParametersAsSqlTimestamp = true
        )
        val SQLite = SqlDialect(limitStyle = SqlLimitStyle.LimitOffset, family = SqlDialectFamily.SQLite)
        val Oracle = SqlDialect(family = SqlDialectFamily.Oracle)
        val SqlServer = SqlDialect("[", "]", family = SqlDialectFamily.SqlServer)
    }
}

enum class SqlDialectFamily {
    Standard,
    MySql,
    PostgreSql,
    SQLite,
    Oracle,
    SqlServer
}

enum class SqlLimitStyle {
    Fetch,
    LimitOffset
}
