/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.statement

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr

sealed interface SqlSelectItem : SqlNode {
    data class Asterisk(
        val tableName: String? = null,
        val qualifier: SqlIdentifier? = tableName?.let { SqlIdentifier.of(it) }
    ) : SqlSelectItem

    data class Expr(
        val expr: SqlExpr,
        val alias: String? = null,
        val metadata: SqlSelectItemAliasMetadata? = null
    ) : SqlSelectItem
}

enum class SqlSelectItemSourceScope {
    Source,
    Selected,
    Aggregate,
    Window,
    Unknown
}

data class SqlSelectItemAliasMetadata(
    val outputName: String,
    val expression: SqlExpr,
    val scope: SqlSelectItemSourceScope,
    val source: SqlSelectItemSource? = null,
    val userReferenceable: Boolean = true
) : SqlNode

data class SqlSelectItemSource(
    val tableName: String? = null,
    val columnName: String,
    val qualifier: SqlIdentifier? = tableName?.let { SqlIdentifier.of(it) },
    val identifier: SqlIdentifier = SqlIdentifier.of(columnName)
) : SqlNode
