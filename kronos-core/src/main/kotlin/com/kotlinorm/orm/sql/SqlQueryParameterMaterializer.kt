/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.sql

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.inspect.SqlNodeRewriter
import com.kotlinorm.syntax.statement.SqlQuery

internal data class SqlQueryPlan(
    val query: SqlQuery,
    val parameters: Map<String, Any?>
)

internal fun KSelectable<*>.materializeSqlQuery(
    parameterValues: MutableMap<String, Any?>,
    parameterCounter: MutableMap<String, Int> = mutableMapOf(),
    wrapper: KronosDataSourceWrapper? = null
): SqlQuery {
    val plan = toSqlQueryPlan(wrapper)
    if (plan.parameters.isEmpty()) return plan.query

    val renames = mutableMapOf<String, String>()
    plan.parameters.forEach { (name, value) ->
        val uniqueName = uniqueParameterName(name, parameterValues, parameterCounter)
        parameterValues[uniqueName] = value
        if (uniqueName != name) {
            renames[name] = uniqueName
        }
    }

    return plan.query.renameNamedParameters(renames)
}

private fun uniqueParameterName(
    name: String,
    parameterValues: Map<String, Any?>,
    parameterCounter: MutableMap<String, Int>
): String {
    val (baseName, existingSuffix) = splitParameterSuffix(name)
    if (name !in parameterValues) {
        parameterCounter[baseName] = maxOf(parameterCounter.getOrDefault(baseName, 0), existingSuffix ?: 0)
        return name
    }

    var count = maxOf(parameterCounter.getOrDefault(baseName, 0), existingSuffix ?: 0)
    var candidate: String
    do {
        count++
        candidate = "$baseName@$count"
    } while (candidate in parameterValues)
    parameterCounter[baseName] = count
    return candidate
}

private val parameterSuffixRegex = Regex("""^(.+)@(\d+)$""")

private fun splitParameterSuffix(name: String): Pair<String, Int?> {
    val match = parameterSuffixRegex.matchEntire(name) ?: return name to null
    return match.groupValues[1] to match.groupValues[2].toInt()
}

internal fun SqlQuery.renameNamedParameters(renames: Map<String, String>): SqlQuery {
    if (renames.isEmpty()) return this
    return namedParameterRenamer(renames).rewriteQuery(this)
}

internal fun SqlExpr.renameNamedParameters(renames: Map<String, String>): SqlExpr {
    if (renames.isEmpty()) return this
    return namedParameterRenamer(renames).rewriteExpr(this)
}

private fun namedParameterRenamer(renames: Map<String, String>): SqlNodeRewriter =
    object : SqlNodeRewriter {
        override fun rewriteExpr(expr: SqlExpr): SqlExpr =
            when (expr) {
                is SqlExpr.Parameter -> {
                    val named = expr.parameter as? SqlParameter.Named
                    val newName = named?.let { renames[it.name] }
                    if (newName == null) {
                        expr
                    } else {
                        SqlExpr.Parameter(SqlParameter.Named(newName))
                    }
                }
                is SqlExpr.In -> expr.copy(
                    expr = rewriteExpr(expr.expr),
                    `in` = when (val operand = expr.`in`) {
                        is SqlInRightOperand.Values -> operand.copy(items = operand.items.map(::rewriteExpr))
                        is SqlInRightOperand.Subquery -> operand.copy(query = rewriteQuery(operand.query))
                    }
                )
                else -> super.rewriteExpr(expr)
            }
    }
