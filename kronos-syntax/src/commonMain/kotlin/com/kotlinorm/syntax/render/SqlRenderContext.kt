/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.validate.SqlDialectValidator
import com.kotlinorm.syntax.validate.SqlValidationSeverity

data class RenderedSql(
    val sql: String,
    val parameters: Map<String, Any?> = emptyMap(),
    val parameterNames: List<String> = parameters.keys.toList(),
    val listParameterOccurrences: Set<Int> = emptySet()
) {
    val orderedParameters: List<Any?>
        get() = parameterNames.map { parameters[it] }
}

data class SqlRenderContext(
    val dialect: SqlDialect = SqlDialect.Standard,
    val parameterValues: Map<String, Any?> = emptyMap(),
    val strictParameterBinding: Boolean = false,
    val lowerer: SqlDialectLowerer = SqlDialectLowerer.None,
    @ExperimentalKronosSyntaxApi
    val planner: SqlDialectPlanner = SqlDialectPlanner.None,
    val validator: SqlDialectValidator = SqlDialectValidator.None,
    val validateBeforeRender: Boolean = true
) {
    fun bind(name: String, value: Any?): SqlRenderContext =
        copy(parameterValues = parameterValues + (name to value))

    fun bindAll(values: Map<String, Any?>): SqlRenderContext =
        copy(parameterValues = parameterValues + values)
}

class UnboundSqlParameterException(
    val parameterNames: List<String>
) : IllegalArgumentException("Unbound SQL parameter(s): ${parameterNames.joinToString(", ")}")

internal fun renderedSql(
    sql: String,
    context: SqlRenderContext,
    astParameterNames: List<String>? = null,
    listParameterOccurrences: Set<Int> = emptySet()
): RenderedSql {
    val names = mergeParameterNames(
        astParameterNames = astParameterNames,
        renderedParameterNames = SqlRenderedParameterScanner.collectNamedParameters(sql)
    )
    val missing = names.distinct().filterNot { it in context.parameterValues }
    if (context.strictParameterBinding && missing.isNotEmpty()) {
        throw UnboundSqlParameterException(missing)
    }
    val parameters = names.distinct().associateWith { context.parameterValues[it] }
    return RenderedSql(
        sql = sql,
        parameters = parameters,
        parameterNames = names,
        listParameterOccurrences = listParameterOccurrences
    )
}

private fun mergeParameterNames(astParameterNames: List<String>?, renderedParameterNames: List<String>): List<String> {
    if (astParameterNames == null) return renderedParameterNames
    val names = astParameterNames.toMutableList()
    val remainingAstCounts = astParameterNames.groupingBy { it }.eachCount().toMutableMap()
    for (renderedName in renderedParameterNames) {
        val remaining = remainingAstCounts[renderedName] ?: 0
        if (remaining > 0) {
            remainingAstCounts[renderedName] = remaining - 1
        } else {
            names += renderedName
        }
    }
    return names
}

internal fun throwOnDialectErrors(context: SqlRenderContext, diagnostics: List<com.kotlinorm.syntax.validate.SqlValidationDiagnostic>) {
    if (!context.validateBeforeRender) return
    val errors = diagnostics.filter { it.severity == SqlValidationSeverity.Error }
    if (errors.isNotEmpty()) {
        throw com.kotlinorm.syntax.validate.SqlValidationException(errors)
    }
}

internal object SqlRenderedParameterScanner {
    fun collectNamedParameters(sql: String): List<String> {
        val names = mutableListOf<String>()
        var index = 0
        var inSingleQuote = false
        var inDoubleQuote = false
        while (index < sql.length) {
            val char = sql[index]
            when {
                char == '\'' && !inDoubleQuote -> {
                    inSingleQuote = !inSingleQuote
                    index++
                }
                char == '"' && !inSingleQuote -> {
                    inDoubleQuote = !inDoubleQuote
                    index++
                }
                char == ':' && !inSingleQuote && !inDoubleQuote && index + 1 < sql.length && sql[index + 1] != ':' -> {
                    val start = index + 1
                    var end = start
                    while (end < sql.length && isParameterNameChar(sql[end])) end++
                    if (end > start && isParameterNameStart(sql[start])) {
                        names += sql.substring(start, end)
                        index = end
                    } else {
                        index++
                    }
                }
                else -> index++
            }
        }
        return names
    }

    private fun isParameterNameStart(char: Char): Boolean =
        char == '_' || char.isLetter()

    private fun isParameterNameChar(char: Char): Boolean =
        char == '_' || char.isLetterOrDigit()
}
