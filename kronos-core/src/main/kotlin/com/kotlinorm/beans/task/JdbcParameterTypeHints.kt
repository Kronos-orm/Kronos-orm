/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.beans.task

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.utils.KTypeKey
import java.sql.Types
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Transfers JDBC null-type hints from ORM planning to statement binding through a task stash.
 *
 * Hints are keyed by named SQL parameter and contain constants from [Types]. Consumers
 * ignore malformed stash entries rather than failing query execution. Expanded list
 * parameters may reuse the unsuffixed source parameter's hint at the binding boundary.
 */
object JdbcParameterTypeHints {
    /** Stash key reserved for a `Map<String, Int>` of JDBC SQL type constants. */
    const val STASH_KEY: String = "kronos.jdbc.parameterTypeHints"

    /**
     * Reads valid parameter type hints from [stash].
     *
     * Non-string keys and non-numeric values are discarded. Numeric values are converted
     * to [Int], matching the JDBC `Types` representation.
     *
     * @param stash operation-local task metadata; `null` is treated as empty
     * @return an immutable empty map when no valid hints exist, otherwise the valid entries
     */
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

    /**
     * Creates a task stash containing [hints], omitting the reserved entry when empty.
     *
     * @return a new mutable stash suitable for an atomic task
     */
    fun stashFor(hints: Map<String, Int>): MutableMap<String, Any?> =
        if (hints.isEmpty()) mutableMapOf() else mutableMapOf(STASH_KEY to hints)
}

/**
 * Derives JDBC type hints only for declared fields whose supplied parameter value is null.
 * Missing and non-null parameters do not contribute entries.
 */
internal fun Iterable<Field>.jdbcNullParameterTypeHints(parameterValues: Map<String, Any?>): Map<String, Int> =
    mapNotNull { field ->
        if (!parameterValues.containsKey(field.name) || parameterValues[field.name] != null) {
            return@mapNotNull null
        }
        field.jdbcNullType()?.let { field.name to it }
    }.toMap()

/**
 * Maps field storage metadata to the JDBC type used by `PreparedStatement.setNull`.
 *
 * Undefined column types fall back to the declared Kotlin type when it has an unambiguous
 * JDBC representation. `null` means the binder must use the generic JDBC null type.
 */
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
        KColumnType.UNDEFINED -> kType?.unambiguousJdbcType()
    }

private fun KType.unambiguousJdbcType(): Int? {
    val key = KTypeKey.from(this, ignoreTopLevelNullability = true)
    return standardJdbcTypes[key] ?: optionalJdbcTypes?.get(key)
}

private inline fun <reified T> typeKey(): KTypeKey =
    KTypeKey.from(typeOf<T>(), ignoreTopLevelNullability = true)

private val standardJdbcTypes = mapOf(
    typeKey<Boolean>() to Types.BIT,
    typeKey<Byte>() to Types.TINYINT,
    typeKey<Short>() to Types.SMALLINT,
    typeKey<Int>() to Types.INTEGER,
    typeKey<Long>() to Types.BIGINT,
    typeKey<Float>() to Types.FLOAT,
    typeKey<Double>() to Types.DOUBLE,
    typeKey<String>() to Types.VARCHAR,
    typeKey<Char>() to Types.VARCHAR,
    typeKey<java.util.UUID>() to Types.VARCHAR,
    typeKey<ByteArray>() to Types.VARBINARY,
    typeKey<java.time.LocalDate>() to Types.DATE,
    typeKey<java.time.LocalTime>() to Types.TIME,
    typeKey<java.time.LocalDateTime>() to Types.TIMESTAMP,
    typeKey<java.time.Instant>() to Types.TIMESTAMP,
    typeKey<java.time.ZonedDateTime>() to Types.TIMESTAMP,
    typeKey<java.time.OffsetDateTime>() to Types.TIMESTAMP
)

private val optionalJdbcTypes: Map<KTypeKey, Int>? by lazy {
    try {
        mapOf(
            typeKey<java.sql.Date>() to Types.DATE,
            typeKey<java.sql.Time>() to Types.TIME,
            typeKey<java.sql.Timestamp>() to Types.TIMESTAMP
        )
    } catch (_: LinkageError) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
