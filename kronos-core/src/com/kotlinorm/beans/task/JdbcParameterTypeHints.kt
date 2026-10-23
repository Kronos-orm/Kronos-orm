package com.kotlinorm.beans.task
import com.kotlinorm.beans.dsl.Field
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

internal expect fun Iterable<Field>.jdbcNullParameterTypeHints(parameterValues: Map<String, Any?>): Map<String, Int>

internal expect fun Field.jdbcNullType(): Int?
