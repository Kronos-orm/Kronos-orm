package com.kotlinorm.beans.task
import com.kotlinorm.beans.dsl.Field
// SQLite binds NULL without a JDBC SQL-type code.
internal actual fun Iterable<Field>.jdbcNullParameterTypeHints(parameterValues: Map<String, Any?>): Map<String, Int> = emptyMap()

internal actual fun Field.jdbcNullType(): Int? = null
