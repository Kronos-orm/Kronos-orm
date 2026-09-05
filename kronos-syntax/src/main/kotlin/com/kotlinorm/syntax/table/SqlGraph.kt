/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.table

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr

sealed interface SqlGraphRowsMode : SqlNode {
    object Match : SqlGraphRowsMode

    data class Vertex(
        val name: String,
        val inPaths: List<String> = emptyList()
    ) : SqlGraphRowsMode

    data class Step(
        val vertex1: String,
        val edge: String,
        val vertex2: String,
        val inPaths: List<String> = emptyList()
    ) : SqlGraphRowsMode
}

sealed interface SqlGraphExportMode : SqlNode {
    data class AllSingletons(val exceptPatterns: List<String>) : SqlGraphExportMode {
        init {
            require(exceptPatterns.isNotEmpty()) { "EXPORT ALL SINGLETONS EXCEPT requires at least one pattern." }
        }
    }

    data class Singletons(val patterns: List<String>) : SqlGraphExportMode {
        init {
            require(patterns.isNotEmpty()) { "EXPORT SINGLETONS requires at least one pattern." }
        }
    }

    object NoSingletons : SqlGraphExportMode
}

sealed interface SqlGraphMatchMode : SqlNode {
    data class Repeatable(val mode: SqlGraphRepeatableMode) : SqlGraphMatchMode

    data class Different(val mode: SqlGraphDifferentMode) : SqlGraphMatchMode
}

enum class SqlGraphRepeatableMode {
    Element,
    ElementBindings,
    Elements
}

enum class SqlGraphDifferentMode {
    Edge,
    EdgeBindings,
    Edges
}

data class SqlGraphPattern(
    val name: String? = null,
    val term: SqlGraphPatternTerm
) : SqlNode

sealed interface SqlGraphPatternTerm : SqlNode {
    data class Quantified(
        val term: SqlGraphPatternTerm,
        val quantifier: SqlGraphQuantifier
    ) : SqlGraphPatternTerm

    data class Vertex(
        val name: String? = null,
        val label: SqlGraphLabel? = null,
        val where: SqlExpr? = null
    ) : SqlGraphPatternTerm

    data class Edge(
        val leftSymbol: SqlGraphSymbol,
        val name: String? = null,
        val label: SqlGraphLabel? = null,
        val where: SqlExpr? = null,
        val rightSymbol: SqlGraphSymbol
    ) : SqlGraphPatternTerm

    data class And(
        val left: SqlGraphPatternTerm,
        val right: SqlGraphPatternTerm
    ) : SqlGraphPatternTerm

    data class Or(
        val left: SqlGraphPatternTerm,
        val right: SqlGraphPatternTerm
    ) : SqlGraphPatternTerm

    data class Alternation(
        val left: SqlGraphPatternTerm,
        val right: SqlGraphPatternTerm
    ) : SqlGraphPatternTerm
}

sealed interface SqlGraphLabel : SqlNode {
    data class Label(val name: String) : SqlGraphLabel

    object Percent : SqlGraphLabel

    data class Not(val label: SqlGraphLabel) : SqlGraphLabel

    data class And(val left: SqlGraphLabel, val right: SqlGraphLabel) : SqlGraphLabel

    data class Or(val left: SqlGraphLabel, val right: SqlGraphLabel) : SqlGraphLabel
}

sealed interface SqlGraphQuantifier : SqlNode {
    object Asterisk : SqlGraphQuantifier

    object Question : SqlGraphQuantifier

    object Plus : SqlGraphQuantifier

    data class Between(
        val start: SqlExpr? = null,
        val end: SqlExpr? = null
    ) : SqlGraphQuantifier

    data class Quantity(val quantity: SqlExpr) : SqlGraphQuantifier
}

enum class SqlGraphSymbol {
    Dash,
    Tilde,
    LeftArrow,
    RightArrow,
    LeftTildeArrow,
    RightTildeArrow
}

