/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.functions

import com.kotlinorm.beans.dsl.KronosFunctionExpr
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.orm.sql.toSqlExpr
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlWindow

object KronosFunctionExpressions {
    private val qualifyFieldArgs = ThreadLocal.withInitial { false }

    fun <T> withQualifiedFieldArgs(block: () -> T): T {
        val previous = qualifyFieldArgs.get()
        qualifyFieldArgs.set(true)
        return try {
            block()
        } finally {
            qualifyFieldArgs.set(previous)
        }
    }

    fun call(functionName: String, args: List<SqlExpr> = emptyList()): KronosFunctionExpr {
        operatorExpr(functionName, args)?.let {
            return KronosFunctionExpr(expr = it, functionName = functionName)
        }
        val sqlName = when (functionName.lowercase()) {
            "rownumber" -> "ROW_NUMBER"
            "groupconcat" -> "GROUP_CONCAT"
            "join" -> "CONCAT_WS"
            in builtInUppercaseFunctions -> functionName.uppercase()
            else -> functionName
        }
        return KronosFunctionExpr(
            expr = SqlExpr.Function(name = SqlIdentifier.of(sqlName), args = args),
            functionName = functionName
        )
    }

    fun callArgs(functionName: String, args: List<Any?> = emptyList()): KronosFunctionExpr =
        call(functionName, args.map { it.asFunctionArgExpr() })

    fun callWindowArgs(
        functionName: String,
        args: List<Any?> = emptyList(),
        window: SqlWindow? = null
    ): KronosFunctionExpr {
        val function = call(functionName, args.map { it.asFunctionArgExpr() })
        return window?.let { function.copy(expr = SqlExpr.Window(function.expr, it)) } ?: function
    }

    private fun Any?.asFunctionArgExpr(): SqlExpr = when (this) {
        is SqlExpr -> this
        is KronosFunctionExpr -> expr
        is Field -> toSqlExpr(useTableAlias = qualifyFieldArgs.get())
        null -> SqlExpr.NullLiteral
        is String -> SqlExpr.StringLiteral(this)
        is Boolean -> SqlExpr.BooleanLiteral(this)
        is Number -> SqlExpr.NumberLiteral(toString())
        is Char -> SqlExpr.StringLiteral(toString())
        else -> SqlExpr.StringLiteral(toString())
    }

    private fun operatorExpr(functionName: String, args: List<SqlExpr>): SqlExpr? {
        val operator = when (functionName.lowercase()) {
            "add" -> SqlBinaryOperator.Plus
            "sub" -> SqlBinaryOperator.Minus
            "mul" -> SqlBinaryOperator.Times
            "div" -> SqlBinaryOperator.Div
            "mod" -> SqlBinaryOperator.Mod
            else -> return null
        }
        if (args.size < 2) return null
        return args.drop(1).fold(args.first()) { left, right -> SqlExpr.Binary(left, operator, right) }
    }

    private val builtInUppercaseFunctions = setOf(
        "count", "sum", "avg", "max", "min",
        "abs", "ceil", "floor", "exp", "greatest", "least", "ln", "log", "pi", "rand", "round", "sign", "sqrt",
        "trunc", "upper", "lower", "replace", "reverse", "trim", "ltrim", "rtrim", "concat", "repeat", "right",
        "left", "substr", "length", "any", "all"
    )
}
