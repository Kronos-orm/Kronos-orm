/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.validate

import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlJsonQueryEmptyBehavior
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlGraphPattern
import com.kotlinorm.syntax.table.SqlGraphPatternTerm
import com.kotlinorm.syntax.table.SqlGraphQuantifier
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlJsonColumn
import com.kotlinorm.syntax.table.SqlMatchRecognize
import com.kotlinorm.syntax.table.SqlRowPattern
import com.kotlinorm.syntax.table.SqlRowPatternDefineItem
import com.kotlinorm.syntax.table.SqlRowPatternSkipMode
import com.kotlinorm.syntax.table.SqlRowPatternSubsetItem
import com.kotlinorm.syntax.table.SqlRowPatternTerm
import com.kotlinorm.syntax.table.SqlTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlSyntaxValidatorAdvancedTableTest {
    @Test
    fun reportsCrossJoinAndMatchRecognizeDiagnostics() {
        val crossJoin = SqlTable.Join(
            left = table("a"),
            joinType = SqlJoinType.Cross,
            right = table("b"),
            condition = SqlJoinCondition.On(col("a", "id").eq(col("b", "id")))
        )
        val matchRecognize = SqlTable.Ident(
            name = "events",
            matchRecognize = SqlMatchRecognize(
                rowPattern = SqlRowPattern(
                    afterMatchMode = SqlRowPatternSkipMode.To("Z"),
                    pattern = SqlRowPatternTerm.Then(
                        SqlRowPatternTerm.Pattern("A"),
                        SqlRowPatternTerm.Pattern("B")
                    ),
                    subset = listOf(SqlRowPatternSubsetItem("S", listOf("Z"))),
                    define = listOf(
                        SqlRowPatternDefineItem("A", bool(true)),
                        SqlRowPatternDefineItem("A", bool(true))
                    )
                )
            )
        )

        val codes = (SqlSyntaxValidator.validate(crossJoin) +
            SqlSyntaxValidator.validate(matchRecognize)).map { it.code }.toSet()

        assertTrue("join.cross.condition" in codes)
        assertTrue("match.define.duplicate" in codes)
        assertTrue("match.define.missing" in codes)
        assertTrue("match.subset.unknown" in codes)
        assertTrue("match.skip.unknown" in codes)
    }

    @Test
    fun traversesJsonAndGraphTablesWithoutDiagnostics() {
        val jsonTable = SqlTable.Json(
            expr = col("payload"),
            path = str("$"),
            columns = listOf(
                SqlJsonColumn.Column(
                    name = "name",
                    type = SqlType.Varchar(64),
                    path = str("$.name"),
                    onEmpty = SqlJsonQueryEmptyBehavior.Default(str("unknown"))
                )
            )
        )
        val graphTable = SqlTable.Graph(
            name = "g",
            patterns = listOf(
                SqlGraphPattern(
                    term = SqlGraphPatternTerm.Quantified(
                        term = SqlGraphPatternTerm.Vertex(where = col("age").gt(num("18"))),
                        quantifier = SqlGraphQuantifier.Between(start = num("1"), end = num("3"))
                    )
                )
            ),
            columns = listOf(SqlSelectItem.Expr(col("id")))
        )

        assertEquals(emptyList(), SqlSyntaxValidator.validate(jsonTable))
        assertEquals(emptyList(), SqlSyntaxValidator.validate(graphTable))
    }

    private fun table(name: String): SqlTable.Ident =
        SqlTable.Ident(name)

    private fun col(column: String): SqlExpr.Column =
        SqlExpr.Column(columnName = column)

    private fun col(table: String, column: String): SqlExpr.Column =
        SqlExpr.Column(tableName = table, columnName = column)

    private fun num(value: String): SqlExpr.NumberLiteral =
        SqlExpr.NumberLiteral(value)

    private fun str(value: String): SqlExpr.StringLiteral =
        SqlExpr.StringLiteral(value)

    private fun bool(value: Boolean): SqlExpr.BooleanLiteral =
        SqlExpr.BooleanLiteral(value)

    private fun SqlExpr.eq(other: SqlExpr): SqlExpr.Binary =
        SqlExpr.Binary(this, SqlBinaryOperator.Equal, other)

    private fun SqlExpr.gt(other: SqlExpr): SqlExpr.Binary =
        SqlExpr.Binary(this, SqlBinaryOperator.GreaterThan, other)
}
