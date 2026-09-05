/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.group

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.quantifier.SqlQuantifier

data class SqlGroup(
    val quantifier: SqlQuantifier? = null,
    val items: List<SqlGroupingItem>
) : SqlNode {
    init {
        require(items.isNotEmpty()) { "GROUP BY requires at least one grouping item." }
    }
}

sealed interface SqlGroupingItem : SqlNode {
    object EmptyGroup : SqlGroupingItem

    data class Expr(val item: SqlExpr) : SqlGroupingItem

    data class Cube(val items: List<SqlExpr>) : SqlGroupingItem {
        init {
            require(items.isNotEmpty()) { "CUBE requires at least one expression." }
        }
    }

    data class Rollup(val items: List<SqlExpr>) : SqlGroupingItem {
        init {
            require(items.isNotEmpty()) { "ROLLUP requires at least one expression." }
        }
    }

    data class GroupingSets(val items: List<SqlGroupingItem>) : SqlGroupingItem {
        init {
            require(items.isNotEmpty()) { "GROUPING SETS requires at least one item." }
        }
    }
}

