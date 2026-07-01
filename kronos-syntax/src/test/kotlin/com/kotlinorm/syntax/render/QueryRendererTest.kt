/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlTimeType
import com.kotlinorm.syntax.expr.SqlWindow
import com.kotlinorm.syntax.expr.SqlWindowFrame
import com.kotlinorm.syntax.expr.SqlWindowFrameBound
import com.kotlinorm.syntax.expr.SqlWindowFrameUnit
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlNullsOrdering
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSetOperator
import com.kotlinorm.syntax.statement.SqlWithItem
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.syntax.table.SqlTablePeriodForMode
import com.kotlinorm.syntax.table.SqlTableSample
import com.kotlinorm.syntax.table.SqlTableSampleMode
import kotlin.test.Test
import kotlin.test.assertEquals

class QueryRendererTest {
    @Test
    fun rendersSelectJoinAggregationAndPagination() {
        val user = table("user", "u")
        val orders = table("orders", "o")
        val joined = SqlTable.Join(
            left = user,
            joinType = SqlJoinType.Left,
            right = orders,
            condition = SqlJoinCondition.On(col("u", "id").eq(col("o", "user_id")))
        )
        val query = SqlQuery.Select(
            quantifier = SqlQuantifier.Distinct,
            select = listOf(
                SqlSelectItem.Expr(col("u", "id")),
                SqlSelectItem.Expr(SqlExpr.CountAsteriskFunc(), alias = "cnt")
            ),
            from = listOf(joined),
            where = SqlExpr.Binary(col("u", "age"), SqlBinaryOperator.GreaterThanEqual, named("age")),
            groupBy = SqlGroup(items = listOf(SqlGroupingItem.Expr(col("u", "id")))),
            having = SqlExpr.Binary(SqlExpr.CountAsteriskFunc(), SqlBinaryOperator.GreaterThan, num("0")),
            orderBy = listOf(SqlOrderingItem(col("cnt"), SqlOrdering.Desc, SqlNullsOrdering.Last)),
            limit = SqlLimit.limit(limit = 10, offset = 5)
        )

        assertEquals(
            """SELECT DISTINCT "u"."id", COUNT(*) AS "cnt" FROM "user" AS "u" LEFT JOIN "orders" AS "o" ON "u"."id" = "o"."user_id" WHERE "u"."age" >= :age GROUP BY "u"."id" HAVING COUNT(*) > 0 ORDER BY "cnt" DESC NULLS LAST OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY""",
            query.toSql()
        )
    }

    @Test
    fun rendersWithSetValuesAndSubqueryPredicates() {
        val values = SqlQuery.Values(
            listOf(
                listOf(num("1"), str("Ada")),
                listOf(num("2"), str("Grace"))
            )
        )
        val activeUsers = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(col("id"))),
            from = listOf(table("user")),
            where = SqlExpr.In(expr = col("id"), `in` = SqlInRightOperand.Subquery(values))
        )
        val archivedUsers = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(col("id"))),
            from = listOf(table("archived_user"))
        )
        val union = SqlQuery.Set(
            left = activeUsers,
            operator = SqlSetOperator.Union(SqlQuantifier.All),
            right = archivedUsers
        )
        val with = SqlQuery.With(
            withRecursive = false,
            withItems = listOf(SqlWithItem("ids", listOf("id", "name"), values)),
            query = union
        )

        assertEquals(
            """WITH "ids" ("id", "name") AS (VALUES (1, 'Ada'), (2, 'Grace')) (SELECT "id" FROM "user" WHERE "id" IN (VALUES (1, 'Ada'), (2, 'Grace'))) UNION ALL (SELECT "id" FROM "archived_user")""",
            with.toSql()
        )
    }

    @Test
    fun rendersWindowFunctions() {
        val expression = SqlExpr.Window(
            expr = SqlExpr.GeneralFunc(name = "ROW_NUMBER"),
            window = SqlWindow(
                partitionBy = listOf(col("u", "tenant_id")),
                orderBy = listOf(SqlOrderingItem(col("u", "created_at"), SqlOrdering.Desc)),
                frame = SqlWindowFrame.Between(
                    unit = SqlWindowFrameUnit.Rows,
                    start = SqlWindowFrameBound.UnboundedPreceding,
                    end = SqlWindowFrameBound.CurrentRow
                )
            )
        )

        assertEquals(
            """ROW_NUMBER() OVER (PARTITION BY "u"."tenant_id" ORDER BY "u"."created_at" DESC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)""",
            expression.toSql()
        )
    }

    @Test
    fun rendersTemporalSampleFunctionAndSubqueryTables() {
        val temporal = SqlTable.Ident(
            name = "orders",
            periodForMode = SqlTablePeriodForMode.SystemTimeAsOf(SqlExpr.TimeLiteral(SqlTimeType.Timestamp(), "2026-01-01 00:00:00")),
            alias = SqlTableAlias("o"),
            sample = SqlTableSample(SqlTableSampleMode.Bernoulli, num("10"), repeatable = num("42"))
        )
        val function = SqlTable.Func(
            withLateral = true,
            name = "unnest",
            args = listOf(col("o", "tags")),
            withOrdinality = true,
            alias = SqlTableAlias("tagged", listOf("tag", "ord"))
        )
        val subquery = SqlTable.Subquery(
            query = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(col("id"))), from = listOf(temporal)),
            alias = SqlTableAlias("recent")
        )
        val query = SqlQuery.Select(from = listOf(SqlTable.Join(subquery, SqlJoinType.Cross, function)))

        assertEquals(
            """SELECT * FROM (SELECT "id" FROM "orders" FOR SYSTEM_TIME AS OF TIMESTAMP '2026-01-01 00:00:00' AS "o" TABLESAMPLE BERNOULLI (10) REPEATABLE (42)) AS "recent" CROSS JOIN LATERAL unnest("o"."tags") WITH ORDINALITY AS "tagged" ("tag", "ord")""",
            query.toSql()
        )
    }
}
