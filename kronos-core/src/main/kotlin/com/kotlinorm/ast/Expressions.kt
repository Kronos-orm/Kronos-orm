/** Copyright 2022-2025 kronos-orm */
package com.kotlinorm.ast

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field

/** Expressions tree for SQL */

// Basics

data class ColumnRef(
        val tableAlias: String? = null,
        val column: String,
        // Optional: original DSL field if available
        val sourceField: Field? = null
) : Expression

data class Literal<T : Any?>(val value: T) : Expression

/** Named parameter for named SQL support */
data class NamedParam(val name: String) : Expression

/** Positional parameter support if needed */
data class PositionalParam(val index: Int) : Expression

// Operators and function calls

data class UnaryOp(val op: String, val expr: Expression) : Expression

data class BinaryOp(val left: Expression, val op: String, val right: Expression) : Expression

/** functionName(args...) */
data class FuncCall(
        val name: String,
        val args: List<Expression> = emptyList(),
        val distinct: Boolean = false
) : Expression

/** CASE WHEN ... THEN ... [ELSE ...] END */
data class CaseWhen(
        val whens: List<Pair<Expression, Expression>>,
        val elseExpr: Expression? = null
) : Expression

/** expr BETWEEN a AND b */
data class Between(
        val expr: Expression,
        val from: Expression,
        val to: Expression,
        val not: Boolean = false
) : Expression

/** expr IN (values...) or expr IN (subquery) */
sealed interface InList : SqlNode

data class InValues(val values: List<Expression>) : InList

data class InSubquery(val query: SelectStatement) : InList

data class InOp(val expr: Expression, val list: InList, val not: Boolean = false) : Expression

/** EXISTS (subquery) */
data class Exists(val subquery: SelectStatement, val not: Boolean = false) : Expression

/** CAST(expr AS type) */
data class Cast(val expr: Expression, val typeName: String) : Expression

/**
 * Raw SQL expression hook to carry the original string when structured info is not available. Use
 * sparingly; primary purpose is bridging existing string-based clauses.
 */
data class RawSqlExpr(val sql: String) : Expression

/**
 * Criteria expression - represents complex conditions Bridge for existing Criteria DSL when full
 * expression breakdown is not yet available.
 */
data class CriteriaExpr(val criteria: Criteria) : Expression
