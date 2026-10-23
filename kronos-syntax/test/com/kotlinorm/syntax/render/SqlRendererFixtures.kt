/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlAssignmentTarget
import com.kotlinorm.syntax.statement.SqlUpdateSetPair
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias

internal fun table(name: String, alias: String? = null): SqlTable.Ident =
    SqlTable.Ident(name = name, alias = alias?.let { SqlTableAlias(it) })

internal fun col(table: String?, column: String): SqlExpr.Column =
    SqlExpr.Column(tableName = table, columnName = column)

internal fun col(column: String): SqlExpr.Column =
    SqlExpr.Column(columnName = column)

internal fun num(value: String): SqlExpr.NumberLiteral =
    SqlExpr.NumberLiteral(value)

internal fun str(value: String): SqlExpr.StringLiteral =
    SqlExpr.StringLiteral(value)

internal fun bool(value: Boolean): SqlExpr.BooleanLiteral =
    SqlExpr.BooleanLiteral(value)

internal fun named(name: String): SqlExpr.Parameter =
    SqlExpr.Parameter(SqlParameter.Named(name))

internal fun id(vararg parts: String): SqlIdentifier =
    SqlIdentifier.of(*parts)

internal fun cols(vararg names: String): List<SqlIdentifier> =
    names.map { id(it) }

internal fun set(column: String, value: SqlExpr): SqlUpdateSetPair =
    SqlUpdateSetPair(SqlAssignmentTarget.Column(id(column)), value)

internal fun SqlExpr.eq(other: SqlExpr): SqlExpr.Binary =
    SqlExpr.Binary(this, SqlBinaryOperator.Equal, other)

internal fun SqlExpr.gt(other: SqlExpr): SqlExpr.Binary =
    SqlExpr.Binary(this, SqlBinaryOperator.GreaterThan, other)

internal fun SqlExpr.lt(other: SqlExpr): SqlExpr.Binary =
    SqlExpr.Binary(this, SqlBinaryOperator.LessThan, other)
