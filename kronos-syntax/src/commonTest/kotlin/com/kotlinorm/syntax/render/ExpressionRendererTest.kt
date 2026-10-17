/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlEncoding
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlJsonArrayItem
import com.kotlinorm.syntax.expr.SqlJsonExistsErrorBehavior
import com.kotlinorm.syntax.expr.SqlJsonInput
import com.kotlinorm.syntax.expr.SqlJsonNullConstructor
import com.kotlinorm.syntax.expr.SqlJsonObjectItem
import com.kotlinorm.syntax.expr.SqlJsonOutput
import com.kotlinorm.syntax.expr.SqlJsonOutputFormat
import com.kotlinorm.syntax.expr.SqlJsonPassing
import com.kotlinorm.syntax.expr.SqlJsonQueryEmptyBehavior
import com.kotlinorm.syntax.expr.SqlJsonQueryErrorBehavior
import com.kotlinorm.syntax.expr.SqlJsonQueryQuotesBehavior
import com.kotlinorm.syntax.expr.SqlJsonQueryQuotesBehaviorMode
import com.kotlinorm.syntax.expr.SqlJsonQueryWrapperBehavior
import com.kotlinorm.syntax.expr.SqlJsonQueryWrapperBehaviorMode
import com.kotlinorm.syntax.expr.SqlJsonUniquenessMode
import com.kotlinorm.syntax.expr.SqlJsonValueEmptyBehavior
import com.kotlinorm.syntax.expr.SqlJsonValueErrorBehavior
import com.kotlinorm.syntax.expr.SqlListAggCountMode
import com.kotlinorm.syntax.expr.SqlListAggOnOverflow
import com.kotlinorm.syntax.expr.SqlCaseBranch
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlJsonNodeType
import com.kotlinorm.syntax.expr.SqlNthValueFromMode
import com.kotlinorm.syntax.expr.SqlSubqueryQuantifier
import com.kotlinorm.syntax.expr.SqlTimeUnit
import com.kotlinorm.syntax.expr.SqlTrim
import com.kotlinorm.syntax.expr.SqlTrimMode
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.expr.SqlWindowNullsMode
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpressionRendererTest {
    @Test
    fun preservesBinaryOperatorPrecedence() {
        val a = col("a")
        val b = col("b")
        val c = col("c")

        assertEquals(
            "\"a\" + \"b\" * \"c\"",
            SqlExpr.Binary(a, SqlBinaryOperator.Plus, SqlExpr.Binary(b, SqlBinaryOperator.Times, c)).toSql()
        )
        assertEquals(
            "(\"a\" + \"b\") * \"c\"",
            SqlExpr.Binary(SqlExpr.Binary(a, SqlBinaryOperator.Plus, b), SqlBinaryOperator.Times, c).toSql()
        )
        assertEquals(
            "\"a\" AND (\"b\" OR \"c\")",
            SqlExpr.Binary(a, SqlBinaryOperator.And, SqlExpr.Binary(b, SqlBinaryOperator.Or, c)).toSql()
        )
    }

    @Test
    fun rendersJsonScalarFunctions() {
        assertEquals(
            """JSON_SERIALIZE("doc" RETURNING VARCHAR(128) FORMAT JSON ENCODING UTF8)""",
            SqlExpr.JsonSerializeFunc(
                expr = col("doc"),
                output = SqlJsonOutput(SqlType.Varchar(128), SqlJsonOutputFormat(SqlEncoding.Utf8))
            ).toSql()
        )
        assertEquals(
            """JSON("raw" FORMAT JSON ENCODING UTF16 WITH UNIQUE KEYS)""",
            SqlExpr.JsonParseFunc(
                expr = col("raw"),
                input = SqlJsonInput(SqlEncoding.Utf16),
                uniquenessMode = SqlJsonUniquenessMode.With
            ).toSql()
        )
        assertEquals(
            """JSON_QUERY("payload", '$.items' PASSING :tenant AS "tenant" RETURNING JSON WITH CONDITIONAL ARRAY WRAPPER KEEP QUOTES ON SCALAR STRING EMPTY ARRAY ON EMPTY ERROR ON ERROR)""",
            SqlExpr.JsonQueryFunc(
                expr = col("payload"),
                path = str("$.items"),
                passingItems = listOf(SqlJsonPassing(named("tenant"), "tenant")),
                output = SqlJsonOutput(SqlType.Json),
                wrapper = SqlJsonQueryWrapperBehavior.With(SqlJsonQueryWrapperBehaviorMode.Conditional, withArray = true),
                quotes = SqlJsonQueryQuotesBehavior(SqlJsonQueryQuotesBehaviorMode.Keep, withOnScalarString = true),
                onEmpty = SqlJsonQueryEmptyBehavior.EmptyArray,
                onError = SqlJsonQueryErrorBehavior.Error
            ).toSql()
        )
        assertEquals(
            """JSON_VALUE("payload", '$.age' RETURNING INTEGER DEFAULT 0 ON EMPTY NULL ON ERROR)""",
            SqlExpr.JsonValueFunc(
                expr = col("payload"),
                path = str("$.age"),
                output = SqlJsonOutput(SqlType.Int),
                onEmpty = SqlJsonValueEmptyBehavior.Default(num("0")),
                onError = SqlJsonValueErrorBehavior.Null
            ).toSql()
        )
        assertEquals(
            """JSON_EXISTS("payload", '$.active' FALSE ON ERROR)""",
            SqlExpr.JsonExistsFunc(
                expr = col("payload"),
                path = str("$.active"),
                onError = SqlJsonExistsErrorBehavior.False
            ).toSql()
        )
    }

    @Test
    fun rendersJsonConstructorsAndAggregates() {
        assertEquals(
            """JSON_OBJECT('id' VALUE "id", 'name' VALUE "name" ABSENT ON NULL WITH UNIQUE KEYS RETURNING JSON)""",
            SqlExpr.JsonObjectFunc(
                items = listOf(SqlJsonObjectItem(str("id"), col("id")), SqlJsonObjectItem(str("name"), col("name"))),
                nullConstructor = SqlJsonNullConstructor.Absent,
                uniquenessMode = SqlJsonUniquenessMode.With,
                output = SqlJsonOutput(SqlType.Json)
            ).toSql()
        )
        assertEquals(
            """JSON_ARRAY("id" FORMAT JSON ENCODING UTF8, "name" NULL ON NULL RETURNING JSON)""",
            SqlExpr.JsonArrayFunc(
                items = listOf(SqlJsonArrayItem(col("id"), SqlJsonInput(SqlEncoding.Utf8)), SqlJsonArrayItem(col("name"))),
                nullConstructor = SqlJsonNullConstructor.Null,
                output = SqlJsonOutput(SqlType.Json)
            ).toSql()
        )
        assertEquals(
            """JSON_OBJECTAGG("k" VALUE "v" NULL ON NULL WITHOUT UNIQUE KEYS RETURNING JSON) FILTER (WHERE "active" = TRUE)""",
            SqlExpr.JsonObjectAggFunc(
                item = SqlJsonObjectItem(col("k"), col("v")),
                nullConstructor = SqlJsonNullConstructor.Null,
                uniquenessMode = SqlJsonUniquenessMode.Without,
                output = SqlJsonOutput(SqlType.Json),
                filter = col("active").eq(bool(true))
            ).toSql()
        )
        assertEquals(
            """JSON_ARRAYAGG("payload" FORMAT JSON ORDER BY "created_at" DESC ABSENT ON NULL RETURNING JSON) FILTER (WHERE "active" = TRUE)""",
            SqlExpr.JsonArrayAggFunc(
                item = SqlJsonArrayItem(col("payload"), SqlJsonInput()),
                orderBy = listOf(SqlOrderingItem(col("created_at"), SqlOrdering.Desc)),
                nullConstructor = SqlJsonNullConstructor.Absent,
                output = SqlJsonOutput(SqlType.Json),
                filter = col("active").eq(bool(true))
            ).toSql()
        )
    }

    @Test
    fun rendersListAggAndArrayTypes() {
        assertEquals(
            """LISTAGG(DISTINCT "name", ', ' ON OVERFLOW TRUNCATE '...' WITH COUNT) WITHIN GROUP (ORDER BY "name" ASC)""",
            SqlExpr.ListAggFunc(
                quantifier = SqlQuantifier.Distinct,
                expr = col("name"),
                separator = str(", "),
                onOverflow = SqlListAggOnOverflow.Truncate(str("..."), SqlListAggCountMode.With),
                withinGroup = listOf(SqlOrderingItem(col("name")))
            ).toSql()
        )
        assertEquals(
            """CAST("tags" AS VARCHAR(64)[])""",
            SqlExpr.Cast(col("tags"), SqlType.Array(SqlType.Varchar(64))).toSql()
        )
    }

    @Test
    fun rendersPredicatesCasesAndSpecialExpressions() {
        val query = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(col("score"))), from = listOf(table("scores")))

        assertEquals("""(1, 2)""", SqlExpr.Tuple(listOf(num("1"), num("2"))).toSql())
        assertEquals(
            """"id" IN (1, 2)""",
            SqlExpr.In(col("id"), SqlInRightOperand.Values(listOf(num("1"), num("2")))).toSql()
        )
        assertEquals(
            """"age" NOT BETWEEN 18 AND 30""",
            SqlExpr.Between(col("age"), num("18"), num("30"), withNot = true).toSql()
        )
        assertEquals(
            """"name" NOT ILIKE 'A%' ESCAPE '!'""",
            SqlExpr.Like(col("name"), str("A%"), escape = str("!"), withNot = true, caseInsensitive = true).toSql()
        )
        assertEquals(
            """"name" SIMILAR TO '(Ada|Grace)%' ESCAPE '!'""",
            SqlExpr.SimilarTo(col("name"), str("(Ada|Grace)%"), escape = str("!")).toSql()
        )
        assertEquals(
            """CASE WHEN "age" > 18 THEN 'adult' ELSE 'minor' END""",
            SqlExpr.Case(
                branches = listOf(SqlCaseBranch(col("age").gt(num("18")), str("adult"))),
                default = str("minor")
            ).toSql()
        )
        assertEquals(
            """CASE "status" WHEN 'A' THEN 'active' ELSE 'inactive' END""",
            SqlExpr.SimpleCase(
                expr = col("status"),
                branches = listOf(SqlCaseBranch(str("A"), str("active"))),
                default = str("inactive")
            ).toSql()
        )
        assertEquals("""COALESCE("nickname", "name")""", SqlExpr.Coalesce(listOf(col("nickname"), col("name"))).toSql())
        assertEquals("""NULLIF("deleted", TRUE)""", SqlExpr.NullIf(col("deleted"), bool(true)).toSql())
        assertEquals("""GROUPING("tenant_id", "region")""", SqlExpr.Grouping(listOf(col("tenant_id"), col("region"))).toSql())
        assertEquals(
            """"payload" IS NOT JSON OBJECT WITHOUT UNIQUE KEYS""",
            SqlExpr.JsonTest(
                expr = col("payload"),
                nodeType = SqlJsonNodeType.Object,
                uniquenessMode = SqlJsonUniquenessMode.Without,
                withNot = true
            ).toSql()
        )
        assertEquals(
            """"score" > ALL (SELECT "score" FROM "scores")""",
            SqlExpr.QuantifiedComparisonPredicate(
                expr = col("score"),
                operator = com.kotlinorm.syntax.expr.SqlQuantifiedComparisonOperator.GreaterThan,
                quantifier = SqlSubqueryQuantifier.All,
                query = query
            ).toSql()
        )
    }

    @Test
    fun rendersStringTimeAndWindowSupportFunctions() {
        assertEquals(
            """TRIM(LEADING '_' FROM "name")""",
            SqlExpr.TrimFunc(col("name"), SqlTrim(SqlTrimMode.Leading, str("_"))).toSql()
        )
        assertEquals(
            """OVERLAY("name" PLACING 'x' FROM 2 FOR 1)""",
            SqlExpr.OverlayFunc(col("name"), str("x"), num("2"), num("1")).toSql()
        )
        assertEquals("""POSITION('a' IN "name")""", SqlExpr.PositionFunc(str("a"), col("name")).toSql())
        assertEquals("""EXTRACT(YEAR FROM "created_at")""", SqlExpr.ExtractFunc(SqlTimeUnit.Year, col("created_at")).toSql())
        assertEquals(
            """NTH_VALUE("amount", 2) FROM LAST IGNORE NULLS""",
            SqlExpr.NthValueFunc(col("amount"), num("2"), SqlNthValueFromMode.Last, SqlWindowNullsMode.Ignore).toSql()
        )
        assertEquals(
            """FIRST_VALUE("amount") RESPECT NULLS""",
            SqlExpr.NullsTreatmentFunc("FIRST_VALUE", listOf(col("amount")), SqlWindowNullsMode.Respect).toSql()
        )
    }
}
