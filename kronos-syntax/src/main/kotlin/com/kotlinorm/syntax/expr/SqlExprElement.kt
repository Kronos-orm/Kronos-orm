/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.expr

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.statement.SqlQuery

sealed interface SqlInRightOperand : SqlNode {
    data class Values(val items: List<SqlExpr>) : SqlInRightOperand {
        init {
            require(items.isNotEmpty()) { "IN values require at least one expression." }
        }
    }

    data class Subquery(val query: SqlQuery) : SqlInRightOperand
}

data class SqlCaseBranch(
    val `when`: SqlExpr,
    val then: SqlExpr
) : SqlNode

data class SqlTrim(
    val mode: SqlTrimMode? = null,
    val value: SqlExpr? = null
) : SqlNode

enum class SqlTrimMode {
    Leading,
    Trailing,
    Both
}

sealed interface SqlEncoding : SqlNode {
    object Utf8 : SqlEncoding

    object Utf16 : SqlEncoding

    object Utf32 : SqlEncoding

    data class UnsafeCustom(val tokens: List<com.kotlinorm.syntax.token.SqlUnsafeToken>) : SqlEncoding
}

enum class SqlJsonNodeType {
    Value,
    Object,
    Array,
    Scalar
}

enum class SqlJsonUniquenessMode {
    With,
    Without
}

data class SqlJsonPassing(
    val expr: SqlExpr,
    val alias: String
) : SqlNode

enum class SqlJsonNullConstructor {
    Null,
    Absent
}

sealed interface SqlJsonQueryWrapperBehavior : SqlNode {
    data class With(
        val mode: SqlJsonQueryWrapperBehaviorMode? = null,
        val withArray: Boolean = false
    ) : SqlJsonQueryWrapperBehavior

    data class Without(
        val withArray: Boolean = false
    ) : SqlJsonQueryWrapperBehavior
}

enum class SqlJsonQueryWrapperBehaviorMode {
    Conditional,
    Unconditional
}

data class SqlJsonQueryQuotesBehavior(
    val mode: SqlJsonQueryQuotesBehaviorMode,
    val withOnScalarString: Boolean = false
) : SqlNode

enum class SqlJsonQueryQuotesBehaviorMode {
    Keep,
    Omit
}

sealed interface SqlJsonQueryEmptyBehavior : SqlNode {
    object Error : SqlJsonQueryEmptyBehavior
    object Null : SqlJsonQueryEmptyBehavior
    object EmptyObject : SqlJsonQueryEmptyBehavior
    object EmptyArray : SqlJsonQueryEmptyBehavior
    data class Default(val expr: SqlExpr) : SqlJsonQueryEmptyBehavior
}

sealed interface SqlJsonQueryErrorBehavior : SqlNode {
    object Error : SqlJsonQueryErrorBehavior
    object Null : SqlJsonQueryErrorBehavior
    object EmptyObject : SqlJsonQueryErrorBehavior
    object EmptyArray : SqlJsonQueryErrorBehavior
    data class Default(val expr: SqlExpr) : SqlJsonQueryErrorBehavior
}

sealed interface SqlJsonValueEmptyBehavior : SqlNode {
    object Error : SqlJsonValueEmptyBehavior
    object Null : SqlJsonValueEmptyBehavior
    data class Default(val expr: SqlExpr) : SqlJsonValueEmptyBehavior
}

sealed interface SqlJsonValueErrorBehavior : SqlNode {
    object Error : SqlJsonValueErrorBehavior
    object Null : SqlJsonValueErrorBehavior
    data class Default(val expr: SqlExpr) : SqlJsonValueErrorBehavior
}

enum class SqlJsonExistsErrorBehavior {
    Error,
    True,
    False,
    Unknown
}

data class SqlJsonInput(
    val encoding: SqlEncoding? = null
) : SqlNode

data class SqlJsonOutputFormat(
    val encoding: SqlEncoding? = null
) : SqlNode

data class SqlJsonOutput(
    val type: SqlType,
    val format: SqlJsonOutputFormat? = null
) : SqlNode

data class SqlJsonObjectItem(
    val key: SqlExpr,
    val value: SqlExpr
) : SqlNode

data class SqlJsonArrayItem(
    val value: SqlExpr,
    val input: SqlJsonInput? = null
) : SqlNode

enum class SqlListAggCountMode {
    With,
    Without
}

sealed interface SqlListAggOnOverflow : SqlNode {
    object Error : SqlListAggOnOverflow

    data class Truncate(
        val filler: SqlExpr? = null,
        val countMode: SqlListAggCountMode? = null
    ) : SqlListAggOnOverflow
}

enum class SqlMatchPhase {
    Final,
    Running
}
