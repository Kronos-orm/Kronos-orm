/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.order

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr

data class SqlOrderingItem(
    val expr: SqlExpr,
    val ordering: SqlOrdering? = null,
    val nullsOrdering: SqlNullsOrdering? = null
) : SqlNode

enum class SqlOrdering {
    Asc,
    Desc
}

enum class SqlNullsOrdering {
    First,
    Last
}
