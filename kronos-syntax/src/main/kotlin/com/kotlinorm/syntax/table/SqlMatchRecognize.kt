/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.table

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrderingItem

data class SqlMatchRecognize(
    val partitionBy: List<SqlExpr> = emptyList(),
    val orderBy: List<SqlOrderingItem> = emptyList(),
    val measures: List<SqlRecognizeMeasureItem> = emptyList(),
    val rowsMode: SqlRecognizePatternRowsMode? = null,
    val rowPattern: SqlRowPattern,
    val alias: SqlTableAlias? = null
) : SqlNode

data class SqlRecognizeMeasureItem(
    val expr: SqlExpr,
    val alias: String
) : SqlNode

sealed interface SqlRecognizePatternRowsMode : SqlNode {
    object OneRow : SqlRecognizePatternRowsMode

    data class AllRows(
        val emptyMatchMode: SqlRecognizePatternEmptyMatchMode? = null
    ) : SqlRecognizePatternRowsMode
}

enum class SqlRecognizePatternEmptyMatchMode {
    ShowEmptyMatches,
    OmitEmptyMatches,
    WithUnmatchedRows
}

data class SqlRowPattern(
    val afterMatchMode: SqlRowPatternSkipMode? = null,
    val strategy: SqlRowPatternStrategy? = null,
    val pattern: SqlRowPatternTerm,
    val subset: List<SqlRowPatternSubsetItem> = emptyList(),
    val define: List<SqlRowPatternDefineItem>
) : SqlNode {
    init {
        require(define.isNotEmpty()) { "MATCH_RECOGNIZE DEFINE requires at least one item." }
    }
}

sealed interface SqlRowPatternSkipMode : SqlNode {
    object ToNextRow : SqlRowPatternSkipMode

    object PastLastRow : SqlRowPatternSkipMode

    data class ToFirst(val name: String) : SqlRowPatternSkipMode

    data class ToLast(val name: String) : SqlRowPatternSkipMode

    data class To(val name: String) : SqlRowPatternSkipMode
}

enum class SqlRowPatternStrategy {
    Initial,
    Seek
}

sealed interface SqlRowPatternQuantifier : SqlNode {
    val withQuestion: Boolean

    data class Asterisk(override val withQuestion: Boolean = false) : SqlRowPatternQuantifier

    data class Plus(override val withQuestion: Boolean = false) : SqlRowPatternQuantifier

    data class Question(override val withQuestion: Boolean = false) : SqlRowPatternQuantifier

    data class Between(
        val start: SqlExpr? = null,
        val end: SqlExpr? = null,
        override val withQuestion: Boolean = false
    ) : SqlRowPatternQuantifier

    data class Quantity(val quantity: SqlExpr) : SqlRowPatternQuantifier {
        override val withQuestion: Boolean = false
    }
}

sealed interface SqlRowPatternTerm : SqlNode {
    val quantifier: SqlRowPatternQuantifier?

    data class Pattern(
        val name: String,
        override val quantifier: SqlRowPatternQuantifier? = null
    ) : SqlRowPatternTerm

    data class Circumflex(
        override val quantifier: SqlRowPatternQuantifier? = null
    ) : SqlRowPatternTerm

    data class Dollar(
        override val quantifier: SqlRowPatternQuantifier? = null
    ) : SqlRowPatternTerm

    data class Exclusion(
        val term: SqlRowPatternTerm,
        override val quantifier: SqlRowPatternQuantifier? = null
    ) : SqlRowPatternTerm

    data class Permute(
        val terms: List<SqlRowPatternTerm>,
        override val quantifier: SqlRowPatternQuantifier? = null
    ) : SqlRowPatternTerm {
        init {
            require(terms.isNotEmpty()) { "PERMUTE requires at least one row pattern term." }
        }
    }

    data class Then(
        val left: SqlRowPatternTerm,
        val right: SqlRowPatternTerm,
        override val quantifier: SqlRowPatternQuantifier? = null
    ) : SqlRowPatternTerm

    data class Or(
        val left: SqlRowPatternTerm,
        val right: SqlRowPatternTerm,
        override val quantifier: SqlRowPatternQuantifier? = null
    ) : SqlRowPatternTerm
}

data class SqlRowPatternDefineItem(
    val name: String,
    val expr: SqlExpr
) : SqlNode

data class SqlRowPatternSubsetItem(
    val name: String,
    val patternNames: List<String>
) : SqlNode {
    init {
        require(patternNames.isNotEmpty()) { "SUBSET requires at least one pattern name." }
    }
}

