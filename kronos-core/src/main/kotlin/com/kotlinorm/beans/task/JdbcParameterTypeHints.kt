/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.beans.task

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.KColumnType
import java.sql.Types

object JdbcParameterTypeHints {
    const val STASH_KEY: String = "kronos.jdbc.parameterTypeHints"

    fun from(stash: Map<String, Any?>?): Map<String, Int> {
        val value = stash?.get(STASH_KEY) as? Map<*, *> ?: return emptyMap()
        return value.mapNotNull { (key, sqlType) ->
            val name = key as? String ?: return@mapNotNull null
            val type = when (sqlType) {
                is Int -> sqlType
                is Number -> sqlType.toInt()
                else -> return@mapNotNull null
            }
            name to type
        }.toMap()
    }

    fun stashFor(hints: Map<String, Int>): MutableMap<String, Any?> =
        if (hints.isEmpty()) mutableMapOf() else mutableMapOf(STASH_KEY to hints)
}

internal fun Iterable<Field>.jdbcNullParameterTypeHints(parameterValues: Map<String, Any?>): Map<String, Int> =
    mapNotNull { field ->
        if (!parameterValues.containsKey(field.name) || parameterValues[field.name] != null) {
            return@mapNotNull null
        }
        field.jdbcNullType()?.let { field.name to it }
    }.toMap()

internal fun Field.jdbcNullType(): Int? =
    when (type) {
        KColumnType.BIT -> Types.BIT
        KColumnType.TINYINT -> Types.TINYINT
        KColumnType.SMALLINT,
        KColumnType.YEAR -> Types.SMALLINT
        KColumnType.INT,
        KColumnType.MEDIUMINT,
        KColumnType.SERIAL -> Types.INTEGER
        KColumnType.BIGINT -> Types.BIGINT
        KColumnType.REAL -> Types.REAL
        KColumnType.FLOAT -> Types.FLOAT
        KColumnType.DOUBLE -> Types.DOUBLE
        KColumnType.DECIMAL,
        KColumnType.NUMERIC -> Types.DECIMAL
        KColumnType.CHAR -> Types.CHAR
        KColumnType.NCHAR -> Types.NCHAR
        KColumnType.VARCHAR,
        KColumnType.JSON,
        KColumnType.ENUM,
        KColumnType.SET,
        KColumnType.UUID,
        KColumnType.XML -> Types.VARCHAR
        KColumnType.NVARCHAR -> Types.NVARCHAR
        KColumnType.TEXT,
        KColumnType.MEDIUMTEXT,
        KColumnType.LONGTEXT,
        KColumnType.CLOB -> Types.LONGVARCHAR
        KColumnType.NCLOB -> Types.NCLOB
        KColumnType.DATE -> Types.DATE
        KColumnType.TIME -> Types.TIME
        KColumnType.DATETIME,
        KColumnType.TIMESTAMP -> Types.TIMESTAMP
        KColumnType.BINARY -> Types.BINARY
        KColumnType.VARBINARY -> Types.VARBINARY
        KColumnType.LONGVARBINARY,
        KColumnType.BLOB,
        KColumnType.MEDIUMBLOB,
        KColumnType.LONGBLOB -> Types.LONGVARBINARY
        KColumnType.GEOMETRY,
        KColumnType.POINT,
        KColumnType.LINESTRING -> Types.VARBINARY
        KColumnType.UNDEFINED -> when (kClass?.qualifiedName) {
            "kotlin.Boolean" -> Types.BIT
            "kotlin.Byte" -> Types.TINYINT
            "kotlin.Short" -> Types.SMALLINT
            "kotlin.Int" -> Types.INTEGER
            "kotlin.Long" -> Types.BIGINT
            "kotlin.Float" -> Types.FLOAT
            "kotlin.Double" -> Types.DOUBLE
            "kotlin.String",
            "kotlin.Char",
            "java.util.UUID" -> Types.VARCHAR
            "kotlin.ByteArray" -> Types.VARBINARY
            "java.time.LocalDate",
            "java.sql.Date" -> Types.DATE
            "java.time.LocalTime",
            "java.sql.Time" -> Types.TIME
            "java.time.LocalDateTime",
            "java.time.Instant",
            "java.time.ZonedDateTime",
            "java.time.OffsetDateTime",
            "java.sql.Timestamp" -> Types.TIMESTAMP
            else -> null
        }
    }
