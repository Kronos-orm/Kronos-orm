/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.statement

import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.expr.SqlExpr

sealed interface SqlQuery : SqlStatement {
    val lock: SqlLock?

    data class Select(
        val quantifier: SqlQuantifier? = null,
        val select: List<SqlSelectItem> = emptyList(),
        val from: List<SqlTable> = emptyList(),
        val where: SqlExpr? = null,
        val groupBy: SqlGroup? = null,
        val having: SqlExpr? = null,
        val orderBy: List<SqlOrderingItem> = emptyList(),
        val limit: SqlLimit? = null,
        override val lock: SqlLock? = null
    ) : SqlQuery

    data class Set(
        val left: SqlQuery,
        val operator: SqlSetOperator,
        val right: SqlQuery,
        val orderBy: List<SqlOrderingItem> = emptyList(),
        val limit: SqlLimit? = null,
        override val lock: SqlLock? = null
    ) : SqlQuery

    data class Values(
        val values: List<List<SqlExpr>>,
        override val lock: SqlLock? = null
    ) : SqlQuery {
        init {
            require(values.isNotEmpty()) { "VALUES query requires at least one row." }
            require(values.all { it.isNotEmpty() }) { "VALUES query rows must not be empty." }
        }
    }

    data class With(
        val withRecursive: Boolean = false,
        val withItems: List<SqlWithItem>,
        val query: SqlQuery,
        override val lock: SqlLock? = null
    ) : SqlQuery {
        init {
            require(withItems.isNotEmpty()) { "WITH query requires at least one item." }
        }
    }
}

