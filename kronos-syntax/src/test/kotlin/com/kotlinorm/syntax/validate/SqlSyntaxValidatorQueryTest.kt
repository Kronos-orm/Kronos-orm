/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.validate

import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.limit.SqlFetch
import com.kotlinorm.syntax.limit.SqlFetchMode
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlWithItem
import com.kotlinorm.syntax.table.SqlTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlSyntaxValidatorQueryTest {
    @Test
    fun reportsSelectHavingFetchAndValuesDiagnostics() {
        val query = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(col("id"))),
            having = SqlExpr.Binary(col("id"), SqlBinaryOperator.GreaterThan, num("0")),
            limit = SqlLimit(fetch = SqlFetch(limit = num("2"), mode = SqlFetchMode.WithTies))
        )
        val values = SqlQuery.Values(listOf(listOf(num("1")), listOf(num("2"), num("3"))))

        val queryCodes = SqlSyntaxValidator.validate(query).map { it.code }.toSet()
        val valuesCodes = SqlSyntaxValidator.validate(values).map { it.code }.toSet()

        assertTrue("select.having.without.group" in queryCodes)
        assertTrue("select.fetch.with.ties.without.order" in queryCodes)
        assertEquals(setOf("values.row.arity"), valuesCodes)
    }

    @Test
    fun acceptsFetchWithTiesWhenOrderByIsPresent() {
        val query = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(col("id"))),
            orderBy = listOf(SqlOrderingItem(col("id"), SqlOrdering.Asc)),
            limit = SqlLimit(fetch = SqlFetch(limit = num("2"), mode = SqlFetchMode.WithTies))
        )

        assertEquals(emptyList(), SqlSyntaxValidator.validate(query))
    }

    @Test
    fun reportsDuplicateCteNamesAndColumns() {
        val itemQuery = SqlQuery.Select(from = listOf(SqlTable.Ident("user")))
        val query = SqlQuery.With(
            withItems = listOf(
                SqlWithItem("u", listOf("id", "id"), itemQuery),
                SqlWithItem("u", listOf("name"), itemQuery)
            ),
            query = itemQuery
        )

        val codes = SqlSyntaxValidator.validate(query).map { it.code }.toSet()

        assertTrue("with.duplicate.name" in codes)
        assertTrue("with.column.duplicate" in codes)
    }

    private fun col(column: String): SqlExpr.Column =
        SqlExpr.Column(columnName = column)

    private fun num(value: String): SqlExpr.NumberLiteral =
        SqlExpr.NumberLiteral(value)
}
