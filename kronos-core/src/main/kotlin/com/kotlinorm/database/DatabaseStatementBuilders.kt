/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.database

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.DBType
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

internal fun Field.sameDefinitionAs(other: Field): Boolean =
    type == other.type &&
        length == other.length &&
        scale == other.scale &&
        nullable == other.nullable &&
        primaryKey.sameDatabasePrimaryKeyModeAs(other.primaryKey) &&
        defaultValue.orEmpty() == other.defaultValue.orEmpty() &&
        kDoc.orEmpty() == other.kDoc.orEmpty()

internal fun Field.sameDefinitionAs(other: Field, dbType: DBType): Boolean =
    when (dbType) {
        DBType.SQLite -> {
            sqliteStorageClass(type) == sqliteStorageClass(other.type) &&
                nullable == other.nullable &&
                primaryKey.sameDatabasePrimaryKeyModeAs(other.primaryKey) &&
                defaultValue.orEmpty() == other.defaultValue.orEmpty()
        }

        DBType.Oracle -> {
            oracleEquivalentType(type, length, scale, other.type, other.length, other.scale) &&
                nullable == other.nullable &&
                primaryKey.sameDatabasePrimaryKeyModeAs(other.primaryKey) &&
                defaultValue.orEmpty() == other.defaultValue.orEmpty() &&
                kDoc.orEmpty() == other.kDoc.orEmpty()
        }

        else -> sameDefinitionAs(other)
    }

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

private fun sqliteStorageClass(type: KColumnType): String = when (type) {
    KColumnType.BIT,
    KColumnType.TINYINT,
    KColumnType.SMALLINT,
    KColumnType.INT,
    KColumnType.MEDIUMINT,
    KColumnType.BIGINT,
    KColumnType.SERIAL,
    KColumnType.YEAR,
    KColumnType.SET -> "INTEGER"

    KColumnType.REAL,
    KColumnType.FLOAT,
    KColumnType.DOUBLE -> "REAL"

    KColumnType.DECIMAL,
    KColumnType.NUMERIC -> "NUMERIC"

    KColumnType.BINARY,
    KColumnType.VARBINARY,
    KColumnType.LONGVARBINARY,
    KColumnType.BLOB,
    KColumnType.MEDIUMBLOB,
    KColumnType.LONGBLOB -> "BLOB"

    else -> "TEXT"
}

private fun oracleEquivalentType(
    expectedType: KColumnType,
    expectedLength: Int,
    expectedScale: Int,
    currentType: KColumnType,
    currentLength: Int,
    currentScale: Int
): Boolean {
    if (expectedType != currentType || expectedScale != currentScale) return false
    val defaultLength = when (expectedType) {
        KColumnType.BIT -> 1
        KColumnType.TINYINT -> 3
        KColumnType.SMALLINT -> 5
        KColumnType.MEDIUMINT -> 7
        KColumnType.INT -> 10
        KColumnType.BIGINT -> 19
        KColumnType.DECIMAL,
        KColumnType.NUMERIC -> 10
        else -> null
    }
    return expectedLength == currentLength ||
        (expectedLength == 0 && defaultLength == currentLength)
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
