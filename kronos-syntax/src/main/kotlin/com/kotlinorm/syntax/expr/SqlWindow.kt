/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.expr

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.order.SqlOrderingItem

data class SqlWindow(
    val partitionBy: List<SqlExpr> = emptyList(),
    val orderBy: List<SqlOrderingItem> = emptyList(),
    val frame: SqlWindowFrame? = null,
    val existingWindowName: SqlIdentifier? = null
) : SqlNode

data class SqlWindowItem(
    val name: SqlIdentifier,
    val window: SqlWindow
) : SqlNode

sealed interface SqlWindowFrame : SqlNode {
    data class Start(
        val unit: SqlWindowFrameUnit,
        val start: SqlWindowFrameBound,
        val excludeMode: SqlWindowFrameExcludeMode? = null
    ) : SqlWindowFrame

    data class Between(
        val unit: SqlWindowFrameUnit,
        val start: SqlWindowFrameBound,
        val end: SqlWindowFrameBound,
        val excludeMode: SqlWindowFrameExcludeMode? = null
    ) : SqlWindowFrame
}

sealed interface SqlWindowFrameBound : SqlNode {
    object CurrentRow : SqlWindowFrameBound

    object UnboundedPreceding : SqlWindowFrameBound

    data class Preceding(val n: SqlExpr) : SqlWindowFrameBound

    object UnboundedFollowing : SqlWindowFrameBound

    data class Following(val n: SqlExpr) : SqlWindowFrameBound
}

enum class SqlWindowFrameUnit {
    Rows,
    Range,
    Groups
}

enum class SqlWindowFrameExcludeMode {
    CurrentRow,
    Group,
    Ties,
    NoOthers
}

enum class SqlWindowNullsMode {
    Respect,
    Ignore
}

enum class SqlNthValueFromMode {
    First,
    Last
}
