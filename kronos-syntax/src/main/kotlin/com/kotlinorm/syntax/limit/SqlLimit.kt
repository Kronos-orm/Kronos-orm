/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.limit

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr

data class SqlLimit(
    val offset: SqlExpr? = null,
    val fetch: SqlFetch? = null
) : SqlNode {
    companion object {
        fun limit(limit: Int, offset: Int? = null): SqlLimit = SqlLimit(
            offset = offset?.let { SqlExpr.NumberLiteral(it.toString()) },
            fetch = SqlFetch(
                limit = SqlExpr.NumberLiteral(limit.toString()),
                unit = SqlFetchUnit.RowCount,
                mode = SqlFetchMode.Only
            )
        )
    }
}

enum class SqlFetchUnit {
    RowCount,
    Percentage
}

enum class SqlFetchMode {
    Only,
    WithTies
}

data class SqlFetch(
    val limit: SqlExpr,
    val unit: SqlFetchUnit = SqlFetchUnit.RowCount,
    val mode: SqlFetchMode = SqlFetchMode.Only
) : SqlNode
