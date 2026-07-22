/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.token

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr

/**
 * Explicit unsafe extension point for SQL syntax that the structured tree does not model yet.
 *
 * This mirrors sqala's unsafe custom token idea and is intentionally more structured than a raw
 * SQL string: callers can still separate literal text, identifiers, and nested expressions.
 */
sealed interface SqlUnsafeToken : SqlNode {
    data class Text(val value: String) : SqlUnsafeToken

    data class Identifier(val value: String) : SqlUnsafeToken

    data class Expr(val value: SqlExpr) : SqlUnsafeToken
}
