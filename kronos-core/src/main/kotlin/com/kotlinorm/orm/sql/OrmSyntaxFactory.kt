/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.sql

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlSelectItemSource
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.syntax.statement.SqlSelectItem

internal fun Field.toSqlExpr(useTableAlias: Boolean = false): SqlExpr {
    return SqlExpr.Column(
        tableName = tableName.takeIf { useTableAlias && it.isNotBlank() },
        columnName = columnName
    )
}

internal fun Field.toSqlParameterEq(parameterName: String, useTableAlias: Boolean = false): SqlExpr =
    SqlExpr.Binary(
        toSqlExpr(useTableAlias),
        SqlBinaryOperator.Equal,
        SqlExpr.Parameter(SqlParameter.Named(parameterName))
    )

internal fun Field.toSqlSelectItem(useTableAlias: Boolean = false): SqlSelectItem.Expr {
    val expr = toSqlExpr(useTableAlias)
    val alias = when {
        name != columnName -> name
        else -> null
    }
    return SqlSelectItem.Expr(
        expr = expr,
        alias = alias,
        metadata = SqlSelectItemAliasMetadata(
            outputName = alias ?: columnName,
            expression = expr,
            scope = if (alias == null) SqlSelectItemSourceScope.Source else SqlSelectItemSourceScope.Selected,
            source = SqlSelectItemSource(tableName = tableName.takeIf { useTableAlias && it.isNotBlank() }, columnName = columnName),
            userReferenceable = true
        )
    )
}

internal fun Any?.toSqlLiteralExpr(): SqlExpr = when (this) {
    null -> SqlExpr.NullLiteral
    is SqlExpr -> this
    is String -> SqlExpr.StringLiteral(this)
    is Boolean -> SqlExpr.BooleanLiteral(this)
    is Number -> SqlExpr.NumberLiteral(toString())
    is Char -> SqlExpr.StringLiteral(toString())
    else -> SqlExpr.StringLiteral(toString())
}
