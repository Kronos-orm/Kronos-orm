/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.expr

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.token.SqlUnsafeToken

/**
 * sqala-style expression tree with Kronos compatibility extensions.
 */
sealed interface SqlExpr : SqlNode {
    data class Column(
        val tableName: String? = null,
        val columnName: String
    ) : SqlExpr

    object NullLiteral : SqlExpr

    data class StringLiteral(val string: String) : SqlExpr

    data class NumberLiteral(val number: String) : SqlExpr

    data class BooleanLiteral(val boolean: Boolean) : SqlExpr

    data class TimeLiteral(val type: SqlTimeType, val time: String) : SqlExpr

    data class IntervalLiteral(val value: String, val field: SqlIntervalField) : SqlExpr

    data class Tuple(val items: List<SqlExpr>) : SqlExpr {
        init {
            require(items.isNotEmpty()) { "Tuple expression requires at least one item." }
        }
    }

    data class Array(val items: List<SqlExpr>) : SqlExpr

    data class Parameter(val parameter: SqlParameter) : SqlExpr

    data class Unary(val operator: SqlUnaryOperator, val expr: SqlExpr) : SqlExpr

    data class Binary(
        val left: SqlExpr,
        val operator: SqlBinaryOperator,
        val right: SqlExpr
    ) : SqlExpr

    data class JsonTest(
        val expr: SqlExpr,
        val nodeType: SqlJsonNodeType? = null,
        val uniquenessMode: SqlJsonUniquenessMode? = null,
        val withNot: Boolean = false
    ) : SqlExpr

    data class In(
        val expr: SqlExpr,
        val `in`: SqlInRightOperand,
        val withNot: Boolean = false
    ) : SqlExpr

    data class Between(
        val expr: SqlExpr,
        val start: SqlExpr,
        val end: SqlExpr,
        val withNot: Boolean = false
    ) : SqlExpr

    data class Like(
        val expr: SqlExpr,
        val pattern: SqlExpr,
        val escape: SqlExpr? = null,
        val withNot: Boolean = false,
        val caseInsensitive: Boolean = false
    ) : SqlExpr

    data class SimilarTo(
        val expr: SqlExpr,
        val pattern: SqlExpr,
        val escape: SqlExpr? = null,
        val withNot: Boolean = false
    ) : SqlExpr

    data class Case(
        val branches: List<SqlCaseBranch>,
        val default: SqlExpr? = null
    ) : SqlExpr {
        init {
            require(branches.isNotEmpty()) { "CASE expression requires at least one WHEN branch." }
        }
    }

    data class SimpleCase(
        val expr: SqlExpr,
        val branches: List<SqlCaseBranch>,
        val default: SqlExpr? = null
    ) : SqlExpr {
        init {
            require(branches.isNotEmpty()) { "Simple CASE expression requires at least one WHEN branch." }
        }
    }

    data class Coalesce(val items: List<SqlExpr>) : SqlExpr {
        init {
            require(items.isNotEmpty()) { "COALESCE requires at least one expression." }
        }
    }

    data class NullIf(val expr: SqlExpr, val test: SqlExpr) : SqlExpr

    data class Cast(val expr: SqlExpr, val type: SqlType) : SqlExpr

    data class Window(val expr: SqlExpr, val window: SqlWindow) : SqlExpr

    data class Subquery(val query: SqlQuery) : SqlExpr

    data class ExistsPredicate(
        val query: SqlQuery,
        val withNot: Boolean = false
    ) : SqlExpr

    data class QuantifiedComparisonPredicate(
        val expr: SqlExpr,
        val operator: SqlQuantifiedComparisonOperator,
        val quantifier: SqlSubqueryQuantifier,
        val query: SqlQuery
    ) : SqlExpr

    data class Grouping(val items: List<SqlExpr>) : SqlExpr {
        init {
            require(items.isNotEmpty()) { "GROUPING requires at least one expression." }
        }
    }

    data class IdentFunc(val name: String) : SqlExpr

    data class SubstringFunc(
        val expr: SqlExpr,
        val from: SqlExpr,
        val `for`: SqlExpr? = null
    ) : SqlExpr

    data class TrimFunc(
        val expr: SqlExpr,
        val trim: SqlTrim? = null
    ) : SqlExpr

    data class OverlayFunc(
        val expr: SqlExpr,
        val placing: SqlExpr,
        val from: SqlExpr,
        val `for`: SqlExpr? = null
    ) : SqlExpr

    data class PositionFunc(val expr: SqlExpr, val inExpr: SqlExpr) : SqlExpr

    data class ExtractFunc(val unit: SqlTimeUnit, val expr: SqlExpr) : SqlExpr

    data class JsonSerializeFunc(
        val expr: SqlExpr,
        val output: SqlJsonOutput? = null
    ) : SqlExpr

    data class JsonParseFunc(
        val expr: SqlExpr,
        val input: SqlJsonInput? = null,
        val uniquenessMode: SqlJsonUniquenessMode? = null
    ) : SqlExpr

    data class JsonQueryFunc(
        val expr: SqlExpr,
        val path: SqlExpr,
        val passingItems: List<SqlJsonPassing> = emptyList(),
        val output: SqlJsonOutput? = null,
        val wrapper: SqlJsonQueryWrapperBehavior? = null,
        val quotes: SqlJsonQueryQuotesBehavior? = null,
        val onEmpty: SqlJsonQueryEmptyBehavior? = null,
        val onError: SqlJsonQueryErrorBehavior? = null
    ) : SqlExpr

    data class JsonValueFunc(
        val expr: SqlExpr,
        val path: SqlExpr,
        val passingItems: List<SqlJsonPassing> = emptyList(),
        val output: SqlJsonOutput? = null,
        val onEmpty: SqlJsonValueEmptyBehavior? = null,
        val onError: SqlJsonValueErrorBehavior? = null
    ) : SqlExpr

    data class JsonObjectFunc(
        val items: List<SqlJsonObjectItem> = emptyList(),
        val nullConstructor: SqlJsonNullConstructor? = null,
        val uniquenessMode: SqlJsonUniquenessMode? = null,
        val output: SqlJsonOutput? = null
    ) : SqlExpr

    data class JsonArrayFunc(
        val items: List<SqlJsonArrayItem> = emptyList(),
        val nullConstructor: SqlJsonNullConstructor? = null,
        val output: SqlJsonOutput? = null
    ) : SqlExpr

    data class JsonExistsFunc(
        val expr: SqlExpr,
        val path: SqlExpr,
        val passingItems: List<SqlJsonPassing> = emptyList(),
        val onError: SqlJsonExistsErrorBehavior? = null
    ) : SqlExpr

    data class CountAsteriskFunc(
        val tableName: String? = null,
        val filter: SqlExpr? = null
    ) : SqlExpr

    data class JsonObjectAggFunc(
        val item: SqlJsonObjectItem,
        val nullConstructor: SqlJsonNullConstructor? = null,
        val uniquenessMode: SqlJsonUniquenessMode? = null,
        val output: SqlJsonOutput? = null,
        val filter: SqlExpr? = null
    ) : SqlExpr

    data class JsonArrayAggFunc(
        val item: SqlJsonArrayItem,
        val orderBy: List<SqlOrderingItem> = emptyList(),
        val nullConstructor: SqlJsonNullConstructor? = null,
        val output: SqlJsonOutput? = null,
        val filter: SqlExpr? = null
    ) : SqlExpr

    data class ListAggFunc(
        val quantifier: SqlQuantifier? = null,
        val expr: SqlExpr,
        val separator: SqlExpr,
        val onOverflow: SqlListAggOnOverflow? = null,
        val withinGroup: List<SqlOrderingItem> = emptyList(),
        val filter: SqlExpr? = null
    ) : SqlExpr

    data class NullsTreatmentFunc(
        val name: String,
        val args: List<SqlExpr> = emptyList(),
        val nullsMode: SqlWindowNullsMode? = null
    ) : SqlExpr

    data class NthValueFunc(
        val expr: SqlExpr,
        val row: SqlExpr,
        val fromMode: SqlNthValueFromMode? = null,
        val nullsMode: SqlWindowNullsMode? = null
    ) : SqlExpr

    data class GeneralFunc(
        val quantifier: SqlQuantifier? = null,
        val name: String,
        val args: List<SqlExpr> = emptyList(),
        val orderBy: List<SqlOrderingItem> = emptyList(),
        val withinGroup: List<SqlOrderingItem> = emptyList(),
        val filter: SqlExpr? = null
    ) : SqlExpr

    data class MatchPhase(val phase: SqlMatchPhase, val expr: SqlExpr) : SqlExpr

    data class UnsafeCustom(val tokens: List<SqlUnsafeToken>) : SqlExpr
}

enum class SqlSubqueryQuantifier {
    Any,
    All,
    Some
}
