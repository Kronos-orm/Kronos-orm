/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.table

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlJsonExistsErrorBehavior
import com.kotlinorm.syntax.expr.SqlJsonOutputFormat
import com.kotlinorm.syntax.expr.SqlJsonQueryEmptyBehavior
import com.kotlinorm.syntax.expr.SqlJsonQueryErrorBehavior
import com.kotlinorm.syntax.expr.SqlJsonQueryQuotesBehavior
import com.kotlinorm.syntax.expr.SqlJsonQueryWrapperBehavior
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem

/**
 * sqala-style table source tree. Advanced SQL table constructs are modelled explicitly.
 */
sealed interface SqlTable : SqlNode {
    data class Ident(
        val name: String,
        val alias: SqlTableAlias? = null,
        val periodForMode: SqlTablePeriodForMode? = null,
        val matchRecognize: SqlMatchRecognize? = null,
        val sample: SqlTableSample? = null,
        val identifier: SqlIdentifier = SqlIdentifier.of(name)
    ) : SqlTable

    data class Func(
        val withLateral: Boolean = false,
        val name: String,
        val args: List<SqlExpr> = emptyList(),
        val withOrdinality: Boolean = false,
        val alias: SqlTableAlias? = null,
        val matchRecognize: SqlMatchRecognize? = null,
        val identifier: SqlIdentifier = SqlIdentifier.of(name)
    ) : SqlTable

    data class Subquery(
        val withLateral: Boolean = false,
        val query: SqlQuery,
        val alias: SqlTableAlias? = null,
        val matchRecognize: SqlMatchRecognize? = null
    ) : SqlTable

    data class Join(
        val left: SqlTable,
        val joinType: SqlJoinType,
        val right: SqlTable,
        val condition: SqlJoinCondition? = null
    ) : SqlTable

    data class Json(
        val withLateral: Boolean = false,
        val expr: SqlExpr,
        val path: SqlExpr,
        val pathAlias: String? = null,
        val passingItems: List<com.kotlinorm.syntax.expr.SqlJsonPassing> = emptyList(),
        val columns: List<SqlJsonColumn>,
        val onError: SqlJsonErrorBehavior? = null,
        val alias: SqlTableAlias? = null,
        val matchRecognize: SqlMatchRecognize? = null
    ) : SqlTable {
        init {
            require(columns.isNotEmpty()) { "JSON_TABLE requires at least one column." }
        }
    }

    data class Graph(
        val withLateral: Boolean = false,
        val name: String,
        val matchMode: SqlGraphMatchMode? = null,
        val patterns: List<SqlGraphPattern>,
        val where: SqlExpr? = null,
        val rowsMode: SqlGraphRowsMode? = null,
        val columns: List<SqlSelectItem>,
        val exportMode: SqlGraphExportMode? = null,
        val alias: SqlTableAlias? = null,
        val matchRecognize: SqlMatchRecognize? = null
    ) : SqlTable {
        init {
            require(patterns.isNotEmpty()) { "GRAPH_TABLE requires at least one pattern." }
            require(columns.isNotEmpty()) { "GRAPH_TABLE requires at least one output column." }
        }
    }
}

data class SqlTableAlias(
    val alias: String,
    val columnAliases: List<String> = emptyList(),
    val identifier: SqlIdentifier = SqlIdentifier.of(alias),
    val columnIdentifiers: List<SqlIdentifier> = columnAliases.map { SqlIdentifier.of(it) }
) : SqlNode

sealed interface SqlTablePeriodForMode : SqlNode {
    data class SystemTimeAsOf(val expr: SqlExpr) : SqlTablePeriodForMode

    data class SystemTimeBetween(
        val mode: SqlTablePeriodBetweenMode? = null,
        val start: SqlExpr,
        val end: SqlExpr
    ) : SqlTablePeriodForMode

    data class SystemTimeFrom(val from: SqlExpr, val to: SqlExpr) : SqlTablePeriodForMode
}

enum class SqlTablePeriodBetweenMode {
    Asymmetric,
    Symmetric
}

data class SqlTableSample(
    val mode: SqlTableSampleMode,
    val percentage: SqlExpr,
    val repeatable: SqlExpr? = null
) : SqlNode

enum class SqlTableSampleMode {
    Bernoulli,
    System
}

sealed interface SqlJsonColumn : SqlNode {
    data class Ordinality(val name: String) : SqlJsonColumn

    data class Column(
        val name: String,
        val type: SqlType,
        val format: SqlJsonOutputFormat? = null,
        val path: SqlExpr? = null,
        val wrapper: SqlJsonQueryWrapperBehavior? = null,
        val quotes: SqlJsonQueryQuotesBehavior? = null,
        val onEmpty: SqlJsonQueryEmptyBehavior? = null,
        val onError: SqlJsonQueryErrorBehavior? = null
    ) : SqlJsonColumn

    data class Exists(
        val name: String,
        val type: SqlType,
        val path: SqlExpr? = null,
        val onError: SqlJsonExistsErrorBehavior? = null
    ) : SqlJsonColumn

    data class Nested(
        val path: SqlExpr,
        val pathAlias: String? = null,
        val columns: List<SqlJsonColumn>
    ) : SqlJsonColumn {
        init {
            require(columns.isNotEmpty()) { "Nested JSON column requires at least one column." }
        }
    }
}

enum class SqlJsonErrorBehavior {
    Error,
    Empty,
    EmptyArray
}
