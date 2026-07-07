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

fun interface KronosArgument {
    fun apply(position: Int, statement: PreparedStatement, context: KronosStatementContext)
}

fun interface KronosArgumentFactory {
    fun build(value: Any?, context: KronosStatementContext): KronosArgument?
}

class KronosArgumentRegistry private constructor(
    private val factories: MutableList<KronosArgumentFactory>
) {
    constructor() : this(mutableListOf())

    fun register(factory: KronosArgumentFactory, prepend: Boolean = true) {
        if (prepend) factories.add(0, factory) else factories.add(factory)
    }

    fun argumentFor(value: Any?, context: KronosStatementContext): KronosArgument {
        return factories.firstNotNullOfOrNull { it.build(value, context) }
            ?: KronosArgument { position, statement, _ -> statement.setObject(position, value) }
    }

    fun bind(statement: PreparedStatement, values: Array<Any?>, context: KronosStatementContext) {
        values.forEachIndexed { index, value ->
            argumentFor(value, context).apply(index + 1, statement, context)
        }
    }

    fun copy(): KronosArgumentRegistry = KronosArgumentRegistry(factories.toMutableList())

    companion object {
        fun defaults(): KronosArgumentRegistry = KronosArgumentRegistry().apply {
            register(KronosArgumentFactory { value, _ ->
                if (value == null) KronosArgument { position, statement, _ ->
                    statement.setNull(position, Types.NULL)
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
                    is Enum<*> -> KronosArgument { position, statement, _ -> statement.setString(position, value.name) }
                    else -> null
                }
            }, prepend = false)
            register(KronosArgumentFactory { value, _ ->
                KronosArgument { position, statement, _ -> statement.setObject(position, value) }
            }, prepend = false)
        }
    }
}
