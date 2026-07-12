/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlConflictTarget
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlLock
import com.kotlinorm.syntax.statement.SqlLockWaitMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlReturning
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemSource
import com.kotlinorm.syntax.statement.SqlUpsertAction
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IdentifierAndDmlEnhancementTest {
    @Test
    fun rendersStructuredIdentifiersForColumnsTablesAliasesAndAsterisks() {
        val qualifiedTable = SqlTable.Ident(
            name = "ignored",
            identifier = SqlIdentifier.of("tenant_a", "orders"),
            alias = SqlTableAlias(
                alias = "ignored_alias",
                identifier = SqlIdentifier.of("o"),
                columnIdentifiers = listOf(SqlIdentifier.of("order_id"))
            )
        )
        val qualifiedColumn = SqlExpr.Column(
            columnName = "ignored",
            qualifier = SqlIdentifier.of("tenant_a", "orders"),
            identifier = SqlIdentifier.of("id")
        )
        val asterisk = SqlSelectItem.Asterisk(qualifier = SqlIdentifier.of("o"))

        assertEquals(
            """"tenant_a"."orders" AS "o" ("order_id")""",
            qualifiedTable.toSql()
        )
        assertEquals(""""tenant_a"."orders"."id"""", qualifiedColumn.toSql())
        assertEquals(""""o".*""", asterisk.let { SqlQuery.Select(select = listOf(it)).toSql() }.removePrefix("SELECT "))
    }

    @Test
    fun selectItemSourceKeepsStructuredDefaultIdentifiers() {
        val source = SqlSelectItemSource(tableName = "user", columnName = "created_at")

        assertEquals(SqlIdentifier.of("user"), source.qualifier)
        assertEquals(SqlIdentifier.of("created_at"), source.identifier)
    }

    @Test
    fun rendersReturningForInsertUpdateDeleteAndUpsert() {
        val returning = SqlReturning(listOf(SqlSelectItem.Expr(col("id"))))
        val insert = SqlDmlStatement.Insert(
            table = table("user"),
            columns = cols("id", "name"),
            mode = SqlInsertMode.Values(listOf(listOf(num("1"), str("Ada")))),
            returning = returning
        )
        val update = SqlDmlStatement.Update(
            table = table("user"),
            setPairs = listOf(set("name", str("Grace"))),
            returning = returning
        )
        val delete = SqlDmlStatement.Delete(
            table = table("user"),
            where = col("id").eq(num("1")),
            returning = returning
        )
        val upsert = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id", "email"),
            values = listOf(num("1"), str("a@b.test")),
            primaryKeys = emptyList(),
            conflictTarget = SqlConflictTarget(constraintName = id("user_email_key")),
            action = SqlUpsertAction.DoNothing,
            returning = returning
        )

        assertEquals(
            """INSERT INTO "user" ("id", "name") VALUES (1, 'Ada') RETURNING "id"""",
            insert.toSql()
        )
        assertEquals(
            """UPDATE "user" SET "name" = 'Grace' RETURNING "id"""",
            update.toSql()
        )
        assertEquals(
            """DELETE FROM "user" WHERE "id" = 1 RETURNING "id"""",
            delete.toSql()
        )
        assertEquals(
            """INSERT INTO "user" ("id", "email") VALUES (1, 'a@b.test') ON CONFLICT ON CONSTRAINT "user_email_key" DO NOTHING RETURNING "id"""",
            upsert.toSql(SqlDialect.PostgreSql)
        )
    }

    @Test
    fun rendersLockTargetsAndWaitMode() {
        val query = SqlQuery.Select(
            from = listOf(table("user", "u")),
            lock = SqlLock.Update(
                waitMode = SqlLockWaitMode.NoWait,
                targets = listOf(SqlIdentifier.of("u"))
            )
        )

        assertEquals(
            """SELECT * FROM "user" AS "u" FOR UPDATE OF "u" NOWAIT""",
            query.toSql()
        )
    }

    @Test
    fun rendersDialectDoNothingUpsertForms() {
        val upsert = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id", "email"),
            values = listOf(num("1"), str("a@b.test")),
            primaryKeys = cols("id"),
            action = SqlUpsertAction.DoNothing
        )

        assertEquals(
            "INSERT INTO `user` (`id`, `email`) VALUES (1, 'a@b.test') ON DUPLICATE KEY UPDATE `id` = `id`",
            upsert.toSql(SqlDialect.MySql)
        )
        assertEquals(
            """INSERT OR IGNORE INTO "user" ("id", "email") VALUES (1, 'a@b.test')""",
            upsert.toSql(SqlDialect.SQLite)
        )
    }

    @Test
    fun rendersInsertSubqueryWithReturning() {
        val insert = SqlDmlStatement.Insert(
            table = table("user_archive"),
            columns = cols("id", "name"),
            mode = SqlInsertMode.Subquery(
                SqlQuery.Select(
                    select = listOf(SqlSelectItem.Expr(col("id")), SqlSelectItem.Expr(col("name"))),
                    from = listOf(table("user")),
                    where = col("active").eq(num("1"))
                )
            ),
            returning = SqlReturning(listOf(SqlSelectItem.Expr(col("id"))))
        )

        assertEquals(
            """INSERT INTO "user_archive" ("id", "name") SELECT "id", "name" FROM "user" WHERE "active" = 1 RETURNING "id"""",
            insert.toSql()
        )
    }

    @Test
    fun rendersStandardMergeDoNothingAndUpdateWhereBranches() {
        val doNothing = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id", "name"),
            values = listOf(num("1"), str("Ada")),
            primaryKeys = cols("id"),
            action = SqlUpsertAction.DoNothing
        )
        val updateWhere = doNothing.copy(
            action = SqlUpsertAction.Update(
                setPairs = listOf(set("name", SqlExpr.ExcludedColumn(id("name")))),
                where = col("active").eq(num("1"))
            )
        )

        assertEquals(
            """MERGE INTO "user" AS "t1" USING (SELECT 1 AS "id", 'Ada' AS "name") AS "t2" ON ("t1"."id" = "t2"."id") WHEN NOT MATCHED THEN INSERT ("id", "name") VALUES (1, 'Ada')""",
            doNothing.toSql()
        )
        assertEquals(
            """MERGE INTO "user" AS "t1" USING (SELECT 1 AS "id", 'Ada' AS "name") AS "t2" ON ("t1"."id" = "t2"."id") WHEN MATCHED THEN UPDATE SET "t1"."name" = "t2"."name" WHERE "active" = 1 WHEN NOT MATCHED THEN INSERT ("id", "name") VALUES (1, 'Ada')""",
            updateWhere.toSql()
        )
    }

    @Test
    fun rejectsInvalidDmlModelShapesWithExactMessages() {
        assertEquals(
            "INSERT VALUES requires at least one row.",
            assertFailsWith<IllegalArgumentException> { SqlInsertMode.Values(emptyList()) }.message
        )
        assertEquals(
            "INSERT VALUES rows must not be empty.",
            assertFailsWith<IllegalArgumentException> { SqlInsertMode.Values(listOf(emptyList())) }.message
        )
        assertEquals(
            "RETURNING requires at least one item.",
            assertFailsWith<IllegalArgumentException> { SqlReturning(emptyList()) }.message
        )
        assertEquals(
            "UPDATE requires at least one SET pair.",
            assertFailsWith<IllegalArgumentException> { SqlDmlStatement.Update(table("user"), emptyList()) }.message
        )
        assertEquals(
            "UPSERT requires at least one column.",
            assertFailsWith<IllegalArgumentException> {
                SqlDmlStatement.Upsert(table("user"), emptyList(), listOf(num("1")), emptyList())
            }.message
        )
        assertEquals(
            "UPSERT requires at least one value.",
            assertFailsWith<IllegalArgumentException> {
                SqlDmlStatement.Upsert(table("user"), cols("id"), emptyList(), cols("id"))
            }.message
        )
    }
}
