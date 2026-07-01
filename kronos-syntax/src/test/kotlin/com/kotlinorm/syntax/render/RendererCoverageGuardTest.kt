/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.*
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.limit.SqlFetch
import com.kotlinorm.syntax.limit.SqlFetchMode
import com.kotlinorm.syntax.limit.SqlFetchUnit
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlNullsOrdering
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.*
import com.kotlinorm.syntax.table.*
import com.kotlinorm.syntax.token.SqlUnsafeToken
import kotlin.test.Test
import kotlin.test.assertEquals

class RendererCoverageGuardTest {
    @Test
    fun rendersRemainingScalarExpressionVariants() {
        assertEquals("NULL", SqlExpr.NullLiteral.toSql())
        assertEquals("FALSE", bool(false).toSql())
        assertEquals("DATE '2026-07-02'", SqlExpr.TimeLiteral(SqlTimeType.Date, "2026-07-02").toSql())
        assertEquals("TIME WITH TIME ZONE '12:30:00'", SqlExpr.TimeLiteral(SqlTimeType.Time(SqlTimeZoneMode.WithTimeZone), "12:30:00").toSql())
        assertEquals("INTERVAL '1-2' YEAR TO MONTH", SqlExpr.IntervalLiteral("1-2", SqlIntervalField.To(SqlTimeUnit.Year, SqlTimeUnit.Month)).toSql())
        assertEquals("ARRAY[1, 2]", SqlExpr.Array(listOf(num("1"), num("2"))).toSql())
        assertEquals("?", SqlExpr.Parameter(SqlParameter.Positional(1)).toSql())
        assertEquals("-1", SqlExpr.Unary(SqlUnaryOperator.Negative, num("1")).toSql())
        assertEquals("NOT(TRUE)", SqlExpr.Unary(SqlUnaryOperator.Not, bool(true)).toSql())
        assertEquals("~\"flags\"", SqlExpr.Unary(SqlUnaryOperator.BitwiseNot, col("flags")).toSql())
        assertEquals("ABS(\"amount\")", SqlExpr.Unary(SqlUnaryOperator.UnsafeCustom(listOf(SqlUnsafeToken.Text("ABS"))), col("amount")).toSql())
        assertEquals("(CURRENT_USER)", SqlExpr.UnsafeCustom(listOf(SqlUnsafeToken.Text("CURRENT_USER"))).toSql())
        assertEquals("CURRENT_DATE", SqlExpr.IdentFunc("CURRENT_DATE").toSql())
        assertEquals("SUBSTRING(\"name\" FROM 1 FOR 2)", SqlExpr.SubstringFunc(col("name"), num("1"), num("2")).toSql())
        assertEquals("\"a\" IS NOT DISTINCT FROM \"b\"", SqlExpr.Binary(col("a"), SqlBinaryOperator.IsDistinctFrom(withNot = true), col("b")).toSql())
        assertEquals("\"a\" IS NULL", SqlExpr.Binary(col("a"), SqlBinaryOperator.Is(), SqlExpr.NullLiteral).toSql())
        assertEquals("\"path\" REGEXP '^[a-z]+$'", SqlExpr.Binary(col("path"), SqlBinaryOperator.Regexp, str("^[a-z]+$")).toSql())
        assertEquals("\"mask\" & 4", SqlExpr.Binary(col("mask"), SqlBinaryOperator.BitwiseAnd, num("4")).toSql())
        assertEquals("\"mask\" << 1", SqlExpr.Binary(col("mask"), SqlBinaryOperator.BitwiseLeftShift, num("1")).toSql())
        assertEquals("\"a\" ## \"b\"", SqlExpr.Binary(col("a"), SqlBinaryOperator.UnsafeCustom(listOf(SqlUnsafeToken.Text("##"))), col("b")).toSql())
    }

    @Test
    fun rendersRemainingJsonBehaviorVariants() {
        assertEquals(
            "JSON_QUERY(\"payload\", '$' WITHOUT ARRAY WRAPPER OMIT QUOTES NULL ON EMPTY DEFAULT 'err' ON ERROR)",
            SqlExpr.JsonQueryFunc(
                expr = col("payload"),
                path = str("$"),
                wrapper = SqlJsonQueryWrapperBehavior.Without(withArray = true),
                quotes = SqlJsonQueryQuotesBehavior(SqlJsonQueryQuotesBehaviorMode.Omit),
                onEmpty = SqlJsonQueryEmptyBehavior.Null,
                onError = SqlJsonQueryErrorBehavior.Default(str("err"))
            ).toSql()
        )
        assertEquals(
            "JSON_QUERY(\"payload\", '$' WITH UNCONDITIONAL WRAPPER EMPTY OBJECT ON EMPTY EMPTY OBJECT ON ERROR)",
            SqlExpr.JsonQueryFunc(
                expr = col("payload"),
                path = str("$"),
                wrapper = SqlJsonQueryWrapperBehavior.With(SqlJsonQueryWrapperBehaviorMode.Unconditional),
                onEmpty = SqlJsonQueryEmptyBehavior.EmptyObject,
                onError = SqlJsonQueryErrorBehavior.EmptyObject
            ).toSql()
        )
        assertEquals("JSON_VALUE(\"payload\", '$' ERROR ON EMPTY DEFAULT 'x' ON ERROR)", SqlExpr.JsonValueFunc(col("payload"), str("$"), onEmpty = SqlJsonValueEmptyBehavior.Error, onError = SqlJsonValueErrorBehavior.Default(str("x"))).toSql())
        assertEquals("JSON_EXISTS(\"payload\", '$' TRUE ON ERROR)", SqlExpr.JsonExistsFunc(col("payload"), str("$"), onError = SqlJsonExistsErrorBehavior.True).toSql())
        assertEquals("JSON_EXISTS(\"payload\", '$' UNKNOWN ON ERROR)", SqlExpr.JsonExistsFunc(col("payload"), str("$"), onError = SqlJsonExistsErrorBehavior.Unknown).toSql())
        assertEquals("JSON_OBJECT(NULL ON NULL)", SqlExpr.JsonObjectFunc(nullConstructor = SqlJsonNullConstructor.Null).toSql())
        assertEquals("JSON_ARRAY(RETURNING JSON)", SqlExpr.JsonArrayFunc(output = SqlJsonOutput(SqlType.Json)).toSql())
    }

    @Test
    fun rendersRemainingTypeWindowAndGroupingVariants() {
        assertEquals(
            "CAST(\"x\" AS POINT)",
            SqlExpr.Cast(col("x"), SqlType.Point).toSql()
        )
        assertEquals(
            "CAST(\"x\" AS NUMERIC(12, 4))",
            SqlExpr.Cast(col("x"), SqlType.Named("NUMERIC", listOf(12, 4))).toSql()
        )
        assertEquals(
            "CAST(\"x\" AS (CUSTOM TYPE))",
            SqlExpr.Cast(col("x"), SqlType.UnsafeCustom(listOf(SqlUnsafeToken.Text("CUSTOM"), SqlUnsafeToken.Text("TYPE")))).toSql()
        )
        assertEquals(
            "SUM(\"amount\") OVER (ORDER BY \"created_at\" ASC RANGE 1 PRECEDING EXCLUDE TIES)",
            SqlExpr.Window(
                SqlExpr.GeneralFunc(name = "SUM", args = listOf(col("amount"))),
                SqlWindow(
                    orderBy = listOf(SqlOrderingItem(col("created_at"))),
                    frame = SqlWindowFrame.Start(
                        unit = SqlWindowFrameUnit.Range,
                        start = SqlWindowFrameBound.Preceding(num("1")),
                        excludeMode = SqlWindowFrameExcludeMode.Ties
                    )
                )
            ).toSql()
        )
        assertEquals(
            """GROUP BY DISTINCT CUBE("a", "b"), ROLLUP("c"), GROUPING SETS("d", ())""",
            SqlGroup(
                quantifier = SqlQuantifier.Distinct,
                items = listOf(
                    SqlGroupingItem.Cube(listOf(col("a"), col("b"))),
                    SqlGroupingItem.Rollup(listOf(col("c"))),
                    SqlGroupingItem.GroupingSets(listOf(SqlGroupingItem.Expr(col("d")), SqlGroupingItem.EmptyGroup))
                )
            ).let { SqlQuery.Select(from = listOf(table("t")), groupBy = it).toSql().substringAfter("SELECT * FROM \"t\" ") }
        )
    }

    @Test
    fun rendersRemainingTableAndStatementVariants() {
        assertEquals(
            "SELECT * FROM \"orders\" FOR SYSTEM_TIME BETWEEN SYMMETRIC TIMESTAMP '2026-01-01' AND TIMESTAMP '2026-02-01'",
            SqlQuery.Select(
                from = listOf(
                    SqlTable.Ident(
                        name = "orders",
                        periodForMode = SqlTablePeriodForMode.SystemTimeBetween(
                            mode = SqlTablePeriodBetweenMode.Symmetric,
                            start = SqlExpr.TimeLiteral(SqlTimeType.Timestamp(), "2026-01-01"),
                            end = SqlExpr.TimeLiteral(SqlTimeType.Timestamp(), "2026-02-01")
                        )
                    )
                )
            ).toSql()
        )
        assertEquals(
            "SELECT * FROM \"orders\" FOR SYSTEM_TIME FROM TIMESTAMP '2026-01-01' TO TIMESTAMP '2026-02-01'",
            SqlQuery.Select(
                from = listOf(
                    SqlTable.Ident(
                        name = "orders",
                        periodForMode = SqlTablePeriodForMode.SystemTimeFrom(
                            from = SqlExpr.TimeLiteral(SqlTimeType.Timestamp(), "2026-01-01"),
                            to = SqlExpr.TimeLiteral(SqlTimeType.Timestamp(), "2026-02-01")
                        )
                    )
                )
            ).toSql()
        )
        assertEquals(
            "SELECT * FROM \"a\" INNER JOIN (\"b\" RIGHT JOIN \"c\" USING (\"id\")) ON \"a\".\"id\" = \"b\".\"id\"",
            SqlQuery.Select(
                from = listOf(
                    SqlTable.Join(
                        left = table("a"),
                        joinType = SqlJoinType.Inner,
                        right = SqlTable.Join(table("b"), SqlJoinType.Right, table("c"), SqlJoinCondition.Using(listOf("id"))),
                        condition = SqlJoinCondition.On(col("a", "id").eq(col("b", "id")))
                    )
                )
            ).toSql()
        )
        assertEquals(
            "INSERT INTO \"user\" SELECT \"id\" FROM \"staging_user\"",
            SqlDmlStatement.Insert(
                table = table("user"),
                mode = SqlInsertMode.Subquery(SqlQuery.Select(select = listOf(SqlSelectItem.Expr(col("id"))), from = listOf(table("staging_user"))))
            ).toSql()
        )
        assertEquals(
            "SELECT \"id\" FROM \"user\" LIMIT 10 OFFSET 20",
            SqlQuery.Select(
                select = listOf(SqlSelectItem.Expr(col("id"))),
                from = listOf(table("user")),
                limit = SqlLimit(offset = num("20"), fetch = SqlFetch(num("10"), SqlFetchUnit.RowCount, SqlFetchMode.Only))
            ).toSql(SqlDialect.PostgreSql)
        )
        assertEquals(
            "FETCH NEXT 50 PERCENT ROWS WITH TIES",
            SqlLimit(fetch = SqlFetch(num("50"), SqlFetchUnit.Percentage, SqlFetchMode.WithTies)).let {
                SqlQuery.Select(from = listOf(table("user")), limit = it).toSql().substringAfter("SELECT * FROM \"user\" ")
            }
        )
    }

    @Test
    fun rendersRemainingGraphAndMatchRecognizeVariants() {
        val graph = SqlTable.Graph(
            name = "g",
            matchMode = SqlGraphMatchMode.Different(SqlGraphDifferentMode.EdgeBindings),
            patterns = listOf(
                SqlGraphPattern(
                    "alt",
                    SqlGraphPatternTerm.Alternation(
                        SqlGraphPatternTerm.Edge(SqlGraphSymbol.LeftArrow, rightSymbol = SqlGraphSymbol.Tilde),
                        SqlGraphPatternTerm.Quantified(
                            SqlGraphPatternTerm.Vertex("v", SqlGraphLabel.Percent),
                            SqlGraphQuantifier.Between(num("1"), num("2"))
                        )
                    )
                )
            ),
            rowsMode = SqlGraphRowsMode.Vertex("v", inPaths = listOf("alt")),
            columns = listOf(SqlSelectItem.Expr(col("v", "id"), "id")),
            exportMode = SqlGraphExportMode.AllSingletons(listOf("skip"))
        )
        assertEquals(
            "GRAPH_TABLE(\"g\" MATCH DIFFERENT EDGE BINDINGS \"alt\" = <-[]~ |+| (\"v\" IS %){1,2} ONE ROW PER VERTEX (\"v\") IN (\"alt\") COLUMNS (\"v\".\"id\" AS \"id\") EXPORT ALL SINGLETONS EXCEPT (\"skip\"))",
            graph.toSql()
        )

        val recognize = SqlMatchRecognize(
            rowsMode = SqlRecognizePatternRowsMode.AllRows(SqlRecognizePatternEmptyMatchMode.ShowEmptyMatches),
            rowPattern = SqlRowPattern(
                afterMatchMode = SqlRowPatternSkipMode.ToFirst("A"),
                strategy = SqlRowPatternStrategy.Initial,
                pattern = SqlRowPatternTerm.Or(
                    SqlRowPatternTerm.Pattern("A", SqlRowPatternQuantifier.Question(withQuestion = true)),
                    SqlRowPatternTerm.Pattern("B", SqlRowPatternQuantifier.Quantity(num("2")))
                ),
                define = listOf(SqlRowPatternDefineItem("A", col("amount").gt(num("1"))))
            )
        )
        assertEquals(
            "MATCH_RECOGNIZE(ALL ROWS PER MATCH SHOW EMPTY MATCHES AFTER MATCH SKIP TO FIRST \"A\" INITIAL PATTERN (\"A\"?? | \"B\"{2}) DEFINE \"A\" AS \"amount\" > 1)",
            renderTable(SqlTable.Ident("events", matchRecognize = recognize)).substringAfter("\"events\" ")
        )
    }

    private fun renderTable(table: SqlTable): String =
        StandardSqlRenderer().renderTable(table)
}
