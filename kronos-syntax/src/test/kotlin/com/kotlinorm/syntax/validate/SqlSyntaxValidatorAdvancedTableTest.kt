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
import com.kotlinorm.syntax.table.SqlGraphSymbol
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlJsonColumn
import com.kotlinorm.syntax.table.SqlMatchRecognize
import com.kotlinorm.syntax.table.SqlRecognizeMeasureItem
import com.kotlinorm.syntax.table.SqlRowPattern
import com.kotlinorm.syntax.table.SqlRowPatternDefineItem
import com.kotlinorm.syntax.table.SqlRowPatternQuantifier
import com.kotlinorm.syntax.table.SqlRowPatternSkipMode
import com.kotlinorm.syntax.table.SqlRowPatternSubsetItem
import com.kotlinorm.syntax.table.SqlRowPatternTerm
import com.kotlinorm.syntax.table.SqlTable
import kotlin.test.Test
import kotlin.test.assertEquals

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

        assertEquals(
            setOf(
                "join.cross.condition",
                "match.define.duplicate",
                "match.define.missing",
                "match.subset.unknown",
                "match.skip.unknown"
            ),
            codes
        )
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
                ),
                SqlJsonColumn.Exists(
                    name = "has_name",
                    type = SqlType.Boolean,
                    path = str("$.name")
                ),
                SqlJsonColumn.Nested(
                    path = str("$.items"),
                    columns = listOf(SqlJsonColumn.Ordinality("item_no"))
                )
            )
        )
        val graphTable = SqlTable.Graph(
            name = "g",
            patterns = listOf(
                SqlGraphPattern(
                    term = SqlGraphPatternTerm.And(
                        left = SqlGraphPatternTerm.Quantified(
                            term = SqlGraphPatternTerm.Vertex(where = col("age").gt(num("18"))),
                            quantifier = SqlGraphQuantifier.Between(start = num("1"), end = num("3"))
                        ),
                        right = SqlGraphPatternTerm.Or(
                            left = SqlGraphPatternTerm.Edge(
                                leftSymbol = SqlGraphSymbol.Dash,
                                where = col("weight").gt(num("0")),
                                rightSymbol = SqlGraphSymbol.RightArrow
                            ),
                            right = SqlGraphPatternTerm.Alternation(
                                left = SqlGraphPatternTerm.Quantified(
                                    term = SqlGraphPatternTerm.Vertex(name = "b"),
                                    quantifier = SqlGraphQuantifier.Quantity(num("2"))
                                ),
                                right = SqlGraphPatternTerm.Quantified(
                                    term = SqlGraphPatternTerm.Vertex(name = "c"),
                                    quantifier = SqlGraphQuantifier.Asterisk
                                )
                            )
                        )
                    )
                ),
                SqlGraphPattern(
                    term = SqlGraphPatternTerm.Quantified(
                        term = SqlGraphPatternTerm.Vertex(name = "d"),
                        quantifier = SqlGraphQuantifier.Plus
                    )
                ),
                SqlGraphPattern(
                    term = SqlGraphPatternTerm.Quantified(
                        term = SqlGraphPatternTerm.Vertex(name = "e"),
                        quantifier = SqlGraphQuantifier.Question
                    )
                )
            ),
            columns = listOf(SqlSelectItem.Expr(col("id")))
        )

        assertEquals(emptyList(), SqlSyntaxValidator.validate(jsonTable))
        assertEquals(emptyList(), SqlSyntaxValidator.validate(graphTable))
    }

    @Test
    fun traversesRowPatternVariantsWithoutDiagnostics() {
        val pattern = SqlRowPatternTerm.Then(
            left = SqlRowPatternTerm.Exclusion(
                term = SqlRowPatternTerm.Pattern(
                    name = "A",
                    quantifier = SqlRowPatternQuantifier.Between(start = num("1"))
                ),
                quantifier = SqlRowPatternQuantifier.Question()
            ),
            right = SqlRowPatternTerm.Or(
                left = SqlRowPatternTerm.Permute(
                    terms = listOf(
                        SqlRowPatternTerm.Pattern("B", SqlRowPatternQuantifier.Quantity(num("2"))),
                        SqlRowPatternTerm.Circumflex(SqlRowPatternQuantifier.Plus())
                    ),
                    quantifier = SqlRowPatternQuantifier.Asterisk()
                ),
                right = SqlRowPatternTerm.Dollar(SqlRowPatternQuantifier.Between(end = num("3")))
            )
        )
        val skipModes = listOf(
            SqlRowPatternSkipMode.ToFirst("A"),
            SqlRowPatternSkipMode.ToLast("B"),
            SqlRowPatternSkipMode.To("A"),
            SqlRowPatternSkipMode.ToNextRow,
            SqlRowPatternSkipMode.PastLastRow,
            null
        )
        val tables = skipModes.mapIndexed { index, skipMode ->
            SqlTable.Ident(
                name = "events_$index",
                matchRecognize = SqlMatchRecognize(
                    measures = listOf(SqlRecognizeMeasureItem(col("price"), "price")),
                    rowPattern = SqlRowPattern(
                        afterMatchMode = skipMode,
                        pattern = pattern,
                        subset = listOf(SqlRowPatternSubsetItem("S", listOf("A", "B"))),
                        define = listOf(
                            SqlRowPatternDefineItem("A", bool(true)),
                            SqlRowPatternDefineItem("B", bool(true))
                        )
                    )
                )
            )
        }

        assertEquals(emptyList(), tables.flatMap { SqlSyntaxValidator.validate(it) })
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
