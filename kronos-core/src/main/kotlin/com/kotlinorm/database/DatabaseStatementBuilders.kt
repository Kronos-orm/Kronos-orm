/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.database

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlPrimaryKeyMode
import com.kotlinorm.syntax.token.SqlUnsafeToken

internal fun Field.toColumnDefinition(columnType: (KColumnType, Int, Int) -> String): SqlColumnDefinition =
    SqlColumnDefinition(
        name = SqlIdentifier.of(columnName),
        type = SqlType.UnsafeCustom(listOf(SqlUnsafeToken.Text(columnType(type, length, scale)))),
        nullable = nullable,
        primaryKey = when (primaryKey) {
            PrimaryKeyType.NOT -> SqlPrimaryKeyMode.NotPrimary
            PrimaryKeyType.IDENTITY -> SqlPrimaryKeyMode.Identity
            PrimaryKeyType.DEFAULT,
            PrimaryKeyType.UUID,
            PrimaryKeyType.SNOWFLAKE,
            PrimaryKeyType.CUSTOM -> SqlPrimaryKeyMode.Primary
        },
        defaultValue = defaultValue?.let { SqlExpr.UnsafeRaw(it.ifEmpty { "\"\"" }) }
    )

internal fun KTableIndex.toCreateIndexStatement(tableName: SqlIdentifier, ifNotExists: Boolean = false): SqlDdlStatement.CreateIndex {
    val normalizedType = type.uppercase()
    val normalizedMethod = method.uppercase()
    return SqlDdlStatement.CreateIndex(
        indexName = SqlIdentifier.of(name),
        tableName = tableName,
        columns = columns.map { SqlIdentifier.of(it) },
        unique = normalizedType == "UNIQUE" || normalizedMethod == "UNIQUE",
        type = type.takeUnless { it.isBlank() || it.equals("NORMAL", ignoreCase = true) || it.equals("UNIQUE", ignoreCase = true) },
        method = method.takeUnless {
            it.isBlank() ||
                it.equals("UNIQUE", ignoreCase = true) ||
                it.equals(type, ignoreCase = true)
        },
        concurrently = concurrently,
        ifNotExists = ifNotExists
    )
}

internal fun Field.sameColumnAttributesAs(other: Field, compareComment: Boolean = true): Boolean =
    nullable == other.nullable &&
        primaryKey.sameDatabasePrimaryKeyModeAs(other.primaryKey) &&
        defaultValue.orEmpty() == other.defaultValue.orEmpty() &&
        (!compareComment || kDoc.orEmpty() == other.kDoc.orEmpty())

private fun PrimaryKeyType.sameDatabasePrimaryKeyModeAs(other: PrimaryKeyType): Boolean =
    databasePrimaryKeyMode() == other.databasePrimaryKeyMode()

private fun PrimaryKeyType.databasePrimaryKeyMode(): String =
    when (this) {
        PrimaryKeyType.NOT -> "none"
        PrimaryKeyType.IDENTITY -> "identity"
        PrimaryKeyType.DEFAULT,
        PrimaryKeyType.UUID,
        PrimaryKeyType.SNOWFLAKE,
        PrimaryKeyType.CUSTOM -> "primary"
    }

internal fun Map<String, Any>.cell(name: String): Any? =
    entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

internal fun Any?.asInt(): Int = when (this) {
    is Int -> this
    is Long -> toInt()
    is Number -> toInt()
    is Boolean -> if (this) 1 else 0
    is String -> toIntOrNull() ?: 0
    else -> 0
}
