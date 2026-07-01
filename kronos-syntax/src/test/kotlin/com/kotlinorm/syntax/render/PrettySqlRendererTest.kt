/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlNullsOrdering
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlUpdateSetPair
import com.kotlinorm.syntax.statement.SqlWithItem
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import kotlin.test.Test
import kotlin.test.assertEquals

class PrettySqlRendererTest {
    @Test
    fun rendersPrettySelectWithClauses() {
        val query = SqlQuery.Select(
            quantifier = SqlQuantifier.Distinct,
            select = listOf(
                SqlSelectItem.Expr(col("u", "id"), alias = "id"),
                SqlSelectItem.Expr(SqlExpr.CountAsteriskFunc(), alias = "cnt")
            ),
            from = listOf(table("user", "u")),
            where = col("u", "age").gt(named("age")),
            groupBy = SqlGroup(items = listOf(SqlGroupingItem.Expr(col("u", "id")))),
            having = SqlExpr.CountAsteriskFunc().gt(num("0")),
            orderBy = listOf(SqlOrderingItem(col("cnt"), SqlOrdering.Desc, SqlNullsOrdering.Last)),
            limit = SqlLimit.limit(limit = 10, offset = 5)
        )

        assertEquals(
            """
            SELECT DISTINCT
              "u"."id" AS "id",
              COUNT(*) AS "cnt"
            FROM
              "user" AS "u"
            WHERE
              "u"."age" > :age
            GROUP BY
              "u"."id"
            HAVING
              COUNT(*) > 0
            ORDER BY
              "cnt" DESC NULLS LAST
            OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY
            """.trimIndent(),
            query.toPrettySql()
        )
    }

    @Test
    fun rendersPrettyWithAndSubqueryTables() {
        val values = SqlQuery.Values(listOf(listOf(num("1")), listOf(num("2"))))
        val subquery = SqlTable.Subquery(
            query = SqlQuery.Select(
                select = listOf(SqlSelectItem.Expr(col("id"))),
                from = listOf(table("user"))
            ),
            alias = SqlTableAlias("u")
        )
        val query = SqlQuery.With(
            withRecursive = true,
            withItems = listOf(SqlWithItem("ids", listOf("id"), values)),
            query = SqlQuery.Select(from = listOf(subquery))
        )

        assertEquals(
            """
            WITH RECURSIVE
              "ids" ("id") AS (
                VALUES (1), (2)
              )
            SELECT
              *
            FROM
              (
                SELECT
                  "id"
                FROM
                  "user"
              ) AS "u"
            """.trimIndent(),
            query.toPrettySql()
        )
    }

    @Test
    fun rendersPrettyDmlAndDialectUpsert() {
        val insert = SqlDmlStatement.Insert(
            table = table("user"),
            columns = listOf("id", "name"),
            mode = SqlInsertMode.Values(listOf(listOf(num("1"), str("Ada")), listOf(num("2"), str("Grace"))))
        )
        val update = SqlDmlStatement.Update(
            table = table("user"),
            setPairs = listOf(SqlUpdateSetPair("name", str("Ada"))),
            where = col("id").eq(num("1"))
        )
        val upsert = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = listOf("id", "name"),
            values = listOf(num("1"), str("Ada")),
            primaryKeys = listOf("id"),
            updateColumns = listOf("name")
        )

        assertEquals(
            """
            INSERT INTO "user" ("id", "name")
            VALUES
              (1, 'Ada'),
              (2, 'Grace')
            """.trimIndent(),
            insert.toPrettySql()
        )
        assertEquals(
            """
            UPDATE "user"
            SET
              "name" = 'Ada'
            WHERE
              "id" = 1
            """.trimIndent(),
            update.toPrettySql()
        )
        assertEquals(
            """
            INSERT INTO `user` (`id`, `name`)
            VALUES (1, 'Ada')
            ON DUPLICATE KEY UPDATE
              `name` = VALUES (`name`)
            """.trimIndent(),
            upsert.toPrettySql(SqlDialect.MySql)
        )
    }
}
