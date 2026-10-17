/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr

internal actual fun oracleDdlLiteral(value: Any?, parameterName: String): SqlExpr = when (value) {
    null -> SqlExpr.NullLiteral
    is String -> SqlExpr.StringLiteral(value)
    is Char -> SqlExpr.StringLiteral(value.toString())
    is Boolean -> SqlExpr.NumberLiteral(if (value) "1" else "0")
    is Number -> SqlExpr.NumberLiteral(value.toString())
    is Enum<*> -> SqlExpr.StringLiteral(value.name)
    is ByteArray -> SqlExpr.Function(
        name = SqlIdentifier.of("HEXTORAW"),
        args = listOf(SqlExpr.StringLiteral(value.joinToString("") { byte -> byte.toUByte().toString(16).padStart(2, '0') }))
    )
    else -> throw IllegalArgumentException(
        "Oracle CREATE TABLE AS SELECT parameter '$parameterName' has unsupported literal type ${value::class.qualifiedName}."
    )
}
