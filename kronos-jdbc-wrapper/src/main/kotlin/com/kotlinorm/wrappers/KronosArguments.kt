/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.wrappers

import com.kotlinorm.beans.task.JdbcParameterTypeHints
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.PreparedStatement
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.Date

/**
 * Binds one already-materialized value to one JDBC parameter position.
 *
 * Implementations must bind only [position] and may use [context] for operation-local
 * metadata such as the SQL type hint for a null value. The position is one-based, as
 * required by [PreparedStatement]. Binding failures from the driver are propagated.
 */
fun interface KronosArgument {
    /**
     * Applies this binding strategy to a prepared statement.
     *
     * @param position one-based JDBC parameter position
     * @param statement statement receiving the value
     * @param context statement metadata and per-operation configuration
     */
    fun apply(position: Int, statement: PreparedStatement, context: KronosStatementContext)
}

/**
 * Selects a [KronosArgument] for one value before it is bound.
 *
 * Returning `null` declines the value and lets the registry try the next factory. A
 * factory should not perform logical serialization here; values are expected to have
 * already crossed the `ValueCodec` encode boundary before parameter binding.
 */
fun interface KronosArgumentFactory {
    /**
     * Builds a binding strategy, or declines the value.
     *
     * @param value value to bind; `null` represents SQL `NULL`
     * @param context statement metadata and per-operation configuration
     * @return a binding strategy, or `null` when this factory does not handle the value
     */
    fun build(value: Any?, context: KronosStatementContext): KronosArgument?
}

/**
 * Ordered registry of JDBC parameter binding strategies.
 *
 * Factories are tried in registration order and the first non-null strategy wins.
 * [register] prepends by default so an application or database plugin can override a
 * built-in binding. [copy] creates an independent registry for a statement handle.
 */
class KronosArgumentRegistry private constructor(
    private val factories: MutableList<KronosArgumentFactory>
) {
    constructor() : this(mutableListOf())

    /**
     * Adds a factory to this registry.
     *
     * @param factory factory to register
     * @param prepend whether the factory takes priority over existing factories
     */
    fun register(factory: KronosArgumentFactory, prepend: Boolean = true) {
        if (prepend) factories.add(0, factory) else factories.add(factory)
    }

    /**
     * Resolves the first binding strategy for [value].
     *
     * If every factory declines, the fallback calls `PreparedStatement.setObject` with
     * the original value. The returned argument is always non-null.
     *
     * @param value value to bind, including `null`
     * @param context statement metadata and per-operation configuration
     * @return selected binding strategy
     */
    fun argumentFor(value: Any?, context: KronosStatementContext): KronosArgument {
        return factories.firstNotNullOfOrNull { it.build(value, context) }
            ?: KronosArgument { position, statement, _ -> statement.setObject(position, value) }
    }

    /**
     * Binds all [values] in array order using one-based JDBC positions.
     *
     * @param statement statement receiving the values
     * @param values values corresponding to the statement's positional parameters
     * @param context statement metadata, including parameter names and null type hints
     */
    fun bind(statement: PreparedStatement, values: Array<Any?>, context: KronosStatementContext) {
        values.forEachIndexed { index, value ->
            argumentFor(value, context).apply(index + 1, statement, context)
        }
    }

    /**
     * Copies the factory order without sharing the mutable registration list.
     *
     * @return independently mutable registry with equivalent factories
     */
    fun copy(): KronosArgumentRegistry = KronosArgumentRegistry(factories.toMutableList())

    companion object {
        /**
         * Creates the default registry for common JVM values and SQL nulls.
         *
         * Null binding consults the operation's [JdbcParameterTypeHints] before falling
         * back to `Types.NULL`; known scalar and temporal values use typed JDBC setters,
         * and the final fallback delegates to `setObject`.
         *
         * @return a new registry; callers may add higher-priority factories
         */
        fun defaults(): KronosArgumentRegistry = KronosArgumentRegistry().apply {
            register(KronosArgumentFactory { value, _ ->
                if (value == null) KronosArgument { position, statement, context ->
                    statement.setNull(position, context.contextJdbcNullType(position) ?: Types.NULL)
                } else null
            }, prepend = false)
            register(KronosArgumentFactory { value, _ ->
                when (value) {
                    is String -> KronosArgument { position, statement, _ -> statement.setString(position, value) }
                    is Char -> KronosArgument { position, statement, _ -> statement.setString(position, value.toString()) }
                    is Boolean -> KronosArgument { position, statement, _ -> statement.setBoolean(position, value) }
                    is Byte -> KronosArgument { position, statement, _ -> statement.setByte(position, value) }
                    is Short -> KronosArgument { position, statement, _ -> statement.setShort(position, value) }
                    is Int -> KronosArgument { position, statement, _ -> statement.setInt(position, value) }
                    is Long -> KronosArgument { position, statement, _ -> statement.setLong(position, value) }
                    is Float -> KronosArgument { position, statement, _ -> statement.setFloat(position, value) }
                    is Double -> KronosArgument { position, statement, _ -> statement.setDouble(position, value) }
                    is BigDecimal -> KronosArgument { position, statement, _ -> statement.setBigDecimal(position, value) }
                    is BigInteger -> KronosArgument { position, statement, _ -> statement.setBigDecimal(position, value.toBigDecimal()) }
                    is ByteArray -> KronosArgument { position, statement, _ -> statement.setBytes(position, value) }
                    is java.sql.Date -> KronosArgument { position, statement, _ -> statement.setDate(position, value) }
                    is java.sql.Time -> KronosArgument { position, statement, _ -> statement.setTime(position, value) }
                    is java.sql.Timestamp -> KronosArgument { position, statement, _ -> statement.setTimestamp(position, value) }
                    is Date -> KronosArgument { position, statement, _ ->
                        statement.setTimestamp(position, java.sql.Timestamp(value.time))
                    }
                    is LocalDate -> KronosArgument { position, statement, _ -> statement.setObject(position, value) }
                    is LocalTime -> KronosArgument { position, statement, _ -> statement.setObject(position, value) }
                    is LocalDateTime -> KronosArgument { position, statement, _ -> statement.setObject(position, value) }
                    is Instant -> KronosArgument { position, statement, _ ->
                        statement.setTimestamp(position, java.sql.Timestamp.from(value))
                    }
                    is OffsetDateTime -> KronosArgument { position, statement, _ -> statement.setObject(position, value) }
                    else -> null
                }
            }, prepend = false)
            register(KronosArgumentFactory { value, _ ->
                KronosArgument { position, statement, _ -> statement.setObject(position, value) }
            }, prepend = false)
        }
    }
}

/**
 * Looks up an SQL type hint for a null JDBC parameter.
 *
 * Expanded list parameters use names suffixed with `@<index>`; when an exact hint is
 * absent, the unsuffixed base name is checked. Returning `null` deliberately leaves the
 * caller to use JDBC's generic `Types.NULL` fallback.
 */
private fun KronosStatementContext.contextJdbcNullType(position: Int): Int? {
    val parameterName = parameterNames.getOrNull(position - 1) ?: return null
    val hints = JdbcParameterTypeHints.from(stash)
    return hints[parameterName] ?: parameterName
        .substringBefore('@')
        .takeIf { it != parameterName }
        ?.let { hints[it] }
}
