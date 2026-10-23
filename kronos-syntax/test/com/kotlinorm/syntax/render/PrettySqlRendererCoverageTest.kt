/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlNullsOrdering
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlLock
import com.kotlinorm.syntax.statement.SqlLockWaitMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSetOperator
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlTable
import kotlin.test.Test
import kotlin.test.assertEquals

class PrettySqlRendererCoverageTest {
    @Test
    fun rendersPrettySetQueryWithOrderingLimitAndLock() {
        val left = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(col("id"))), from = listOf(table("user")))
        val right = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(col("id"))), from = listOf(table("archive")))
        val query = SqlQuery.Set(
            left = left,
            operator = SqlSetOperator.Except(SqlQuantifier.Distinct),
            right = right,
            orderBy = listOf(SqlOrderingItem(col("id"), SqlOrdering.Asc, SqlNullsOrdering.First)),
            limit = SqlLimit.limit(5),
            lock = SqlLock.Update(SqlLockWaitMode.NoWait)
        )

        assertEquals(
            """
            (
              SELECT
                "id"
              FROM
                "user"
            )
            EXCEPT DISTINCT
            (
              SELECT
                "id"
              FROM
                "archive"
            )
            ORDER BY
              "id" ASC NULLS FIRST
            FETCH NEXT 5 ROWS ONLY
            FOR UPDATE NOWAIT
            """.trimIndent(),
            query.toPrettySql()
        )
    }

    @Test
    fun rendersPrettyJoinUsingGroupedAsteriskAndDelete() {
        val joined = SqlTable.Join(
            left = table("user"),
            joinType = SqlJoinType.Inner,
            right = table("role"),
            condition = SqlJoinCondition.Using(listOf("role_id"))
        )
        val query = SqlQuery.Select(
            select = listOf(SqlSelectItem.Asterisk("user")),
            from = listOf(joined),
            groupBy = SqlGroup(
                quantifier = SqlQuantifier.Distinct,
                items = listOf(
                    SqlGroupingItem.Cube(listOf(col("tenant_id"))),
                    SqlGroupingItem.Rollup(listOf(col("region"))),
                    SqlGroupingItem.GroupingSets(listOf(SqlGroupingItem.EmptyGroup, SqlGroupingItem.Expr(col("tenant_id"))))
                )
            )
        )
        val delete = SqlDmlStatement.Delete(table("user"), where = col("inactive").eq(bool(true)))

        assertEquals(
            """
            SELECT
              "user".*
            FROM
              "user"
              INNER JOIN "role"
              USING ("role_id")
            GROUP BY DISTINCT
              CUBE("tenant_id"),
              ROLLUP("region"),
              GROUPING SETS((), "tenant_id")
            """.trimIndent(),
            query.toPrettySql()
        )
        assertEquals(
            """
            DELETE FROM "user"
            WHERE
              "inactive" = TRUE
            """.trimIndent(),
            delete.toPrettySql()
        )
    }

    @Test
    fun rendersPrettyUpsertForStandardPostgresqlSqliteAndFallbackStatements() {
        val upsert = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id", "name"),
            values = listOf(num("1"), str("Ada")),
            primaryKeys = cols("id")
        )
        val createIndex = SqlDdlStatement.CreateIndex(id("idx_user_name"), id("user"), cols("name"))

        assertEquals(
            """
            MERGE INTO "user" AS "t1"
            USING (
            SELECT
              1 AS "id",
              'Ada' AS "name"
            ) AS "t2"
            ON ("t1"."id" = "t2"."id")
            WHEN MATCHED THEN UPDATE SET
              "t1"."name" = "t2"."name"
            WHEN NOT MATCHED THEN INSERT ("id", "name")
            VALUES (1, 'Ada')
            """.trimIndent(),
            upsert.toPrettySql()
        )
        assertEquals(
            """
            INSERT INTO "user" ("id", "name")
            VALUES (1, 'Ada')
            ON CONFLICT ("id") DO UPDATE SET
              "name" = EXCLUDED."name"
            """.trimIndent(),
            upsert.toPrettySql(SqlDialect.PostgreSql)
        )
        assertEquals(
            """
            INSERT OR REPLACE INTO "user" ("id", "name")
            VALUES (1, 'Ada')
            """.trimIndent(),
            upsert.toPrettySql(SqlDialect.SQLite)
        )
        assertEquals("""CREATE INDEX "idx_user_name" ON "user" ("name")""", createIndex.toPrettySql())
    }
}
