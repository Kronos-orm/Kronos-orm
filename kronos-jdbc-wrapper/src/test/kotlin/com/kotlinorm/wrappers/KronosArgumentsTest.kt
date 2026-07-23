/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.wrappers

import com.kotlinorm.beans.task.JdbcParameterTypeHints
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.time.Instant
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.reflect.typeOf

class KronosArgumentsTest {
    @Test
    fun `null binding uses jdbc type hints when present and generic null otherwise`() {
        val bindings = mutableListOf<NullBinding>()
        val statement = recordingStatement(bindings)
        val context = KronosStatementContext(
            originalSql = "INSERT INTO events VALUES (:localDate, :localTime, :localDateTime, :rawValue)",
            jdbcSql = "INSERT INTO events VALUES (?, ?, ?, ?)",
            params = listOf(null, null, null, null),
            parameterNames = listOf("localDate", "localTime", "localDateTime", "rawValue"),
            operationType = KOperationType.INSERT,
            dbType = DBType.Mssql,
            databaseProductName = "Microsoft SQL Server",
            config = KronosJdbcConfig(DBType.Mssql, "Microsoft SQL Server", "jdbc:sqlserver://localhost", "mssql"),
            stash = JdbcParameterTypeHints.stashFor(
                mapOf(
                    "localDate" to Types.DATE,
                    "localTime" to Types.TIME,
                    "localDateTime" to Types.TIMESTAMP
                )
            )
        )

        KronosArgumentRegistry.defaults().bind(statement, arrayOf(null, null, null, null), context)

        assertEquals(
            listOf(
                NullBinding(1, Types.DATE),
                NullBinding(2, Types.TIME),
                NullBinding(3, Types.TIMESTAMP),
                NullBinding(4, Types.NULL)
            ),
            bindings
        )
    }

    @Test
    fun `jdbc wrapper update binds typed temporal nulls from action task stash`() {
        val bindings = mutableListOf<NullBinding>()
        val statement = recordingStatement(bindings)
        val dataSource = dataSource(statement)
        val sql = "INSERT INTO events VALUES (:localDate, :localTime, :localDateTime, :rawValue)"

        val task = KronosAtomicActionTask(
            sql = sql,
            paramMap = mapOf(
                "localDate" to null,
                "localTime" to null,
                "localDateTime" to null,
                "rawValue" to null
            ),
            operationType = KOperationType.INSERT,
            stash = JdbcParameterTypeHints.stashFor(
                mapOf(
                    "localDate" to Types.DATE,
                    "localTime" to Types.TIME,
                    "localDateTime" to Types.TIMESTAMP
                )
            )
        )

        KronosJdbcWrapper(dataSource, DBType.Mssql).update(task)

        assertEquals(
            listOf(
                NullBinding(1, Types.DATE),
                NullBinding(2, Types.TIME),
                NullBinding(3, Types.TIMESTAMP),
                NullBinding(4, Types.NULL)
            ),
            bindings
        )
    }

    @Test
    fun `jdbc wrapper query keeps typed null hints for expanded list parameters`() {
        val bindings = mutableListOf<NullBinding>()
        val statement = recordingQueryStatement(bindings)
        val jdbcSql = "SELECT id FROM events WHERE event_date IN (?, ?) AND raw_value = ?"
        val task = KronosAtomicQueryTask(
            sql = "SELECT id FROM events WHERE event_date IN (:eventDates) AND raw_value = :rawValue",
            paramMap = mapOf("eventDates" to listOf(null, null), "rawValue" to null),
            targetType = typeOf<Any?>(),
            stash = JdbcParameterTypeHints.stashFor(mapOf("eventDates" to Types.DATE)),
            listParameterOccurrences = setOf(0)
        )

        KronosJdbcWrapper(queryDataSource(statement, jdbcSql), DBType.Mssql).toList(task)

        assertEquals(
            listOf(
                NullBinding(1, Types.DATE),
                NullBinding(2, Types.DATE),
                NullBinding(3, Types.NULL)
            ),
            bindings
        )
    }

    @Test
    fun `generated keys remain raw physical values`() {
        val rawKey = GeneratedKeyPayload("driver-value")
        val statement = generatedKeyStatement(rawKey)
        val task = KronosAtomicActionTask(
            sql = "INSERT INTO events DEFAULT VALUES",
            operationType = KOperationType.INSERT,
            generatedKeyField = Field("id", tableName = "events")
        )

        KronosJdbcWrapper(generatedKeyDataSource(statement), DBType.H2).update(task)

        assertSame(rawKey, task.generatedKeys.single())
        assertEquals(null, task.lastInsertId)
    }

    @Test
    fun `binder keeps physical temporal handling but does not encode enum names`() {
        val bindings = mutableListOf<ValueBinding>()
        val statement = proxy(PreparedStatement::class.java) { method, args ->
            when (method.name) {
                "setObject", "setTimestamp" -> {
                    bindings += ValueBinding(method.name, args?.get(0) as Int, args[1])
                    null
                }
                else -> defaultValue(method.returnType)
            }
        }
        val localDate = LocalDate.of(2026, 7, 21)
        val instant = Instant.parse("2026-07-21T01:02:03Z")

        KronosArgumentRegistry.defaults().bind(
            statement,
            arrayOf(ArgumentStatus.READY, localDate, instant),
            statementContext()
        )

        assertEquals(
            listOf(
                ValueBinding("setObject", 1, ArgumentStatus.READY),
                ValueBinding("setObject", 2, localDate),
                ValueBinding("setTimestamp", 3, java.sql.Timestamp.from(instant))
            ),
            bindings
        )
    }

    private data class NullBinding(val position: Int, val sqlType: Int)

    private data class ValueBinding(val method: String, val position: Int, val value: Any?)

    private fun statementContext() = KronosStatementContext(
        originalSql = "INSERT INTO events VALUES (?, ?, ?)",
        jdbcSql = "INSERT INTO events VALUES (?, ?, ?)",
        params = emptyList(),
        parameterNames = emptyList(),
        operationType = KOperationType.INSERT,
        dbType = DBType.H2,
        databaseProductName = "H2",
        config = KronosJdbcConfig(DBType.H2, "H2", "jdbc:h2:mem:test", "H2")
    )

    private fun dataSource(statement: PreparedStatement): DataSource {
        val metadata = proxy(DatabaseMetaData::class.java) { method, _ ->
            when (method.name) {
                "getDatabaseProductName" -> "Microsoft SQL Server"
                "getURL" -> "jdbc:sqlserver://localhost"
                "getUserName" -> "sa"
                "getDriverName" -> "mssql"
                else -> defaultValue(method.returnType)
            }
        }
        val connection = proxy(Connection::class.java) { method, args ->
            when (method.name) {
                "getMetaData" -> metadata
                "prepareStatement" -> if (args?.firstOrNull() == "INSERT INTO events VALUES (?, ?, ?, ?)") {
                    statement
                } else {
                    error("Unexpected SQL: ${args?.firstOrNull()}")
                }
                else -> defaultValue(method.returnType)
            }
        }
        return proxy(DataSource::class.java) { method, _ ->
            when (method.name) {
                "getConnection" -> connection
                else -> defaultValue(method.returnType)
            }
        }
    }

    private fun recordingStatement(bindings: MutableList<NullBinding>): PreparedStatement =
        proxy(PreparedStatement::class.java) { method, args ->
            when (method.name) {
                "setNull" -> {
                    bindings += NullBinding(args?.get(0) as Int, args[1] as Int)
                    null
                }
                "executeUpdate" -> 1
                else -> defaultValue(method.returnType)
            }
        }

    private fun recordingQueryStatement(bindings: MutableList<NullBinding>): PreparedStatement {
        val emptyResultSet = proxy(ResultSet::class.java) { method, _ ->
            when (method.name) {
                "next" -> false
                else -> defaultValue(method.returnType)
            }
        }
        return proxy(PreparedStatement::class.java) { method, args ->
            when (method.name) {
                "setNull" -> {
                    bindings += NullBinding(args?.get(0) as Int, args[1] as Int)
                    null
                }
                "executeQuery" -> emptyResultSet
                else -> defaultValue(method.returnType)
            }
        }
    }

    private fun queryDataSource(statement: PreparedStatement, expectedSql: String): DataSource {
        val metadata = proxy(DatabaseMetaData::class.java) { method, _ ->
            when (method.name) {
                "getDatabaseProductName" -> "Microsoft SQL Server"
                "getURL" -> "jdbc:sqlserver://localhost"
                "getUserName" -> "sa"
                "getDriverName" -> "mssql"
                else -> defaultValue(method.returnType)
            }
        }
        val connection = proxy(Connection::class.java) { method, args ->
            when (method.name) {
                "getMetaData" -> metadata
                "prepareStatement" -> if (args?.firstOrNull() == expectedSql) statement else {
                    error("Unexpected SQL: ${args?.firstOrNull()}")
                }
                else -> defaultValue(method.returnType)
            }
        }
        return proxy(DataSource::class.java) { method, _ ->
            when (method.name) {
                "getConnection" -> connection
                else -> defaultValue(method.returnType)
            }
        }
    }

    private fun generatedKeyStatement(rawKey: Any): PreparedStatement {
        var nextCall = 0
        val keys = proxy(ResultSet::class.java) { method, args ->
            when (method.name) {
                "next" -> nextCall++ == 0
                "getObject" -> if (args?.firstOrNull() == 1) rawKey else null
                else -> defaultValue(method.returnType)
            }
        }
        return proxy(PreparedStatement::class.java) { method, _ ->
            when (method.name) {
                "executeUpdate" -> 1
                "getGeneratedKeys" -> keys
                else -> defaultValue(method.returnType)
            }
        }
    }

    private fun generatedKeyDataSource(statement: PreparedStatement): DataSource {
        val metadata = proxy(DatabaseMetaData::class.java) { method, _ ->
            when (method.name) {
                "getDatabaseProductName" -> "H2"
                "getURL" -> "jdbc:h2:mem:generated-keys"
                "getUserName" -> "sa"
                "getDriverName" -> "H2"
                else -> defaultValue(method.returnType)
            }
        }
        val connection = proxy(Connection::class.java) { method, _ ->
            when (method.name) {
                "getMetaData" -> metadata
                "prepareStatement" -> statement
                else -> defaultValue(method.returnType)
            }
        }
        return proxy(DataSource::class.java) { method, _ ->
            when (method.name) {
                "getConnection" -> connection
                else -> defaultValue(method.returnType)
            }
        }
    }

    private fun <T> proxy(type: Class<T>, handler: (Method, Array<Any?>?) -> Any?): T {
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
            when {
                method.declaringClass == Any::class.java && method.name == "toString" -> "${type.simpleName}Proxy"
                method.declaringClass == Any::class.java && method.name == "hashCode" -> 0
                method.declaringClass == Any::class.java && method.name == "equals" -> false
                else -> handler(method, args)
            }
        } as T
    }

    private fun defaultValue(type: Class<*>): Any? =
        when (type) {
            java.lang.Boolean.TYPE -> false
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            java.lang.Character.TYPE -> 0.toChar()
            java.lang.Void.TYPE -> null
            else -> null
        }
}

private enum class ArgumentStatus {
    READY
}

private data class GeneratedKeyPayload(val value: String)
