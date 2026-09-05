/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.validate

import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlCaseBranch
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlIntervalField
import com.kotlinorm.syntax.expr.SqlJsonArrayItem
import com.kotlinorm.syntax.expr.SqlJsonInput
import com.kotlinorm.syntax.expr.SqlJsonObjectItem
import com.kotlinorm.syntax.expr.SqlJsonOutput
import com.kotlinorm.syntax.expr.SqlJsonPassing
import com.kotlinorm.syntax.expr.SqlJsonQueryEmptyBehavior
import com.kotlinorm.syntax.expr.SqlJsonQueryErrorBehavior
import com.kotlinorm.syntax.expr.SqlJsonValueEmptyBehavior
import com.kotlinorm.syntax.expr.SqlJsonValueErrorBehavior
import com.kotlinorm.syntax.expr.SqlListAggCountMode
import com.kotlinorm.syntax.expr.SqlListAggOnOverflow
import com.kotlinorm.syntax.expr.SqlMatchPhase
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.expr.SqlQuantifiedComparisonOperator
import com.kotlinorm.syntax.expr.SqlSubqueryQuantifier
import com.kotlinorm.syntax.expr.SqlTimeType
import com.kotlinorm.syntax.expr.SqlTimeUnit
import com.kotlinorm.syntax.expr.SqlTrim
import com.kotlinorm.syntax.expr.SqlTrimMode
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.expr.SqlUnaryOperator
import com.kotlinorm.syntax.expr.SqlWindow
import com.kotlinorm.syntax.expr.SqlWindowFrame
import com.kotlinorm.syntax.expr.SqlWindowFrameBound
import com.kotlinorm.syntax.expr.SqlWindowFrameUnit
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.token.SqlUnsafeToken
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlSyntaxValidatorExpressionTraversalTest {
    @Test
    fun traversesEveryExpressionShape() {
        val scalarQuery = SqlQuery.Select(select = listOf(com.kotlinorm.syntax.statement.SqlSelectItem.Expr(num("1"))))
        val valuesQuery = SqlQuery.Values(listOf(listOf(num("1"))))
        val orderBy = listOf(SqlOrderingItem(col("name"), SqlOrdering.Desc))
        val jsonOutput = SqlJsonOutput(SqlType.Array(SqlType.UnsafeCustom(listOf(SqlUnsafeToken.Expr(num("1"))))))
        val expressions = listOf(
            col("id"),
            SqlExpr.NullLiteral,
            str("Ada"),
            num("1"),
            bool(true),
            SqlExpr.TimeLiteral(SqlTimeType.Timestamp(), "2026-01-01 00:00:00"),
            SqlExpr.IntervalLiteral("1", SqlIntervalField.Single(SqlTimeUnit.Day)),
            SqlExpr.Tuple(listOf(num("1"), num("2"))),
            SqlExpr.Array(listOf(num("1"), num("2"))),
            SqlExpr.Parameter(SqlParameter.Named("id")),
            SqlExpr.Unary(SqlUnaryOperator.Not, bool(false)),
            col("id").eq(num("1")),
            SqlExpr.JsonTest(col("payload")),
            SqlExpr.In(col("id"), SqlInRightOperand.Values(listOf(num("1"), num("2")))),
            SqlExpr.In(col("id"), SqlInRightOperand.Subquery(valuesQuery)),
            SqlExpr.Between(col("age"), num("18"), num("30")),
            SqlExpr.Like(col("name"), str("A%"), escape = str("\\")),
            SqlExpr.SimilarTo(col("name"), str("(Ada|Grace)%"), escape = str("\\")),
            SqlExpr.Case(listOf(SqlCaseBranch(col("active"), str("yes"))), default = str("no")),
            SqlExpr.SimpleCase(col("role"), listOf(SqlCaseBranch(str("admin"), num("1"))), default = num("0")),
            SqlExpr.Coalesce(listOf(col("name"), str("unknown"))),
            SqlExpr.NullIf(col("name"), str("")),
            SqlExpr.Cast(col("payload"), SqlType.Json),
            SqlExpr.Window(
                expr = SqlExpr.Function(name = id("SUM"), args = listOf(col("amount"))),
                window = SqlWindow(
                    partitionBy = listOf(col("tenant_id")),
                    orderBy = orderBy,
                    frame = SqlWindowFrame.Between(
                        unit = SqlWindowFrameUnit.Rows,
                        start = SqlWindowFrameBound.Preceding(num("1")),
                        end = SqlWindowFrameBound.Following(num("1"))
                    )
                )
            ),
            SqlExpr.Subquery(scalarQuery),
            SqlExpr.ExistsPredicate(scalarQuery),
            SqlExpr.QuantifiedComparisonPredicate(col("amount"), SqlQuantifiedComparisonOperator.GreaterThan, SqlSubqueryQuantifier.Any, scalarQuery),
            SqlExpr.Grouping(listOf(col("tenant_id"))),
            SqlExpr.IdentFunc("CURRENT_USER"),
            SqlExpr.SubstringFunc(col("name"), num("1"), num("3")),
            SqlExpr.TrimFunc(col("name"), SqlTrim(SqlTrimMode.Both, str(" "))),
            SqlExpr.OverlayFunc(col("name"), str("x"), num("1"), num("1")),
            SqlExpr.PositionFunc(str("a"), col("name")),
            SqlExpr.ExtractFunc(SqlTimeUnit.Year, col("created_at")),
            SqlExpr.JsonSerializeFunc(col("payload"), jsonOutput),
            SqlExpr.JsonParseFunc(col("payload"), SqlJsonInput()),
            SqlExpr.JsonQueryFunc(
                expr = col("payload"),
                path = str("$.items"),
                passingItems = listOf(SqlJsonPassing(num("1"), "one")),
                output = jsonOutput,
                onEmpty = SqlJsonQueryEmptyBehavior.Default(str("[]")),
                onError = SqlJsonQueryErrorBehavior.Default(str("[]"))
            ),
            SqlExpr.JsonValueFunc(
                expr = col("payload"),
                path = str("$.name"),
                passingItems = listOf(SqlJsonPassing(str("Ada"), "name")),
                output = SqlJsonOutput(SqlType.Varchar(64)),
                onEmpty = SqlJsonValueEmptyBehavior.Default(str("unknown")),
                onError = SqlJsonValueErrorBehavior.Default(str("unknown"))
            ),
            SqlExpr.JsonObjectFunc(
                items = listOf(SqlJsonObjectItem(str("name"), col("name"))),
                output = jsonOutput
            ),
            SqlExpr.JsonArrayFunc(
                items = listOf(SqlJsonArrayItem(col("name"), SqlJsonInput())),
                output = jsonOutput
            ),
            SqlExpr.JsonExistsFunc(col("payload"), str("$.name"), passingItems = listOf(SqlJsonPassing(str("Ada"), "name"))),
            SqlExpr.CountAsteriskFunc(filter = col("active")),
            SqlExpr.JsonObjectAggFunc(SqlJsonObjectItem(col("id"), col("name")), output = jsonOutput, filter = col("active")),
            SqlExpr.JsonArrayAggFunc(SqlJsonArrayItem(col("name")), orderBy = orderBy, output = jsonOutput, filter = col("active")),
            SqlExpr.ListAggFunc(
                expr = col("name"),
                separator = str(","),
                onOverflow = SqlListAggOnOverflow.Truncate(filler = str("..."), countMode = SqlListAggCountMode.With),
                withinGroup = orderBy,
                filter = col("active")
            ),
            SqlExpr.NullsTreatmentFunc("LAG", args = listOf(col("name"))),
            SqlExpr.NthValueFunc(col("name"), num("2")),
            SqlExpr.Function(name = id("MAX"), args = listOf(col("age")), orderBy = orderBy, withinGroup = orderBy, filter = col("active")),
            SqlExpr.MatchPhase(SqlMatchPhase.Running, col("price")),
            SqlExpr.UnsafeCustom(listOf(SqlUnsafeToken.Text("LOWER"), SqlUnsafeToken.Identifier("name"), SqlUnsafeToken.Expr(col("name"))))
        )

        expressions.forEach { expression ->
            assertEquals(emptyList(), SqlSyntaxValidator.validate(expression), "Expected no diagnostics for $expression")
        }
    }

    @Test
    fun traversesEverySqlTypeShape() {
        val types = listOf(
            SqlType.Varchar(),
            SqlType.Int,
            SqlType.Long,
            SqlType.Float,
            SqlType.Double,
            SqlType.Decimal(10 to 2),
            SqlType.Date,
            SqlType.Timestamp(),
            SqlType.Time(),
            SqlType.Json,
            SqlType.Boolean,
            SqlType.Interval,
            SqlType.Geometry,
            SqlType.Point,
            SqlType.LineString,
            SqlType.Polygon,
            SqlType.MultiPoint,
            SqlType.MultiLineString,
            SqlType.MultiPolygon,
            SqlType.GeometryCollection,
            SqlType.Array(SqlType.Int),
            SqlType.Named("uuid"),
            SqlType.UnsafeCustom(listOf(SqlUnsafeToken.Expr(num("1"))))
        )

        types.forEach { type ->
            assertEquals(emptyList(), SqlSyntaxValidator.validate(SqlExpr.Cast(num("1"), type)), "Expected no diagnostics for $type")
        }
    }

    private fun col(column: String): SqlExpr.Column =
        SqlExpr.Column(columnName = column)

    private fun num(value: String): SqlExpr.NumberLiteral =
        SqlExpr.NumberLiteral(value)

    private fun str(value: String): SqlExpr.StringLiteral =
        SqlExpr.StringLiteral(value)

    private fun bool(value: Boolean): SqlExpr.BooleanLiteral =
        SqlExpr.BooleanLiteral(value)

    private fun SqlExpr.eq(other: SqlExpr): SqlExpr.Binary =
        SqlExpr.Binary(this, SqlBinaryOperator.Equal, other)
}
