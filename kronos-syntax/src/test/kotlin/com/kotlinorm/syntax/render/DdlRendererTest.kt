/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlIndexDefinition
import com.kotlinorm.syntax.statement.SqlPrimaryKeyMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import kotlin.test.Test
import kotlin.test.assertEquals

class DdlRendererTest {
    @Test
    fun rendersCreateTableWithIndexes() {
        val create = SqlDdlStatement.CreateTable(
            tableName = id("user"),
            columns = listOf(
                SqlColumnDefinition(
                    name = id("id"),
                    type = SqlType.Int,
                    nullable = false,
                    primaryKey = SqlPrimaryKeyMode.Identity
                ),
                SqlColumnDefinition(
                    name = id("name"),
                    type = SqlType.Varchar(64)
                )
            ),
            indexes = listOf(SqlIndexDefinition(id("idx_user_name"), cols("name"), unique = true)),
            comment = "users",
            ifNotExists = true
        )

        assertEquals(
            """CREATE TABLE IF NOT EXISTS "user" ("id" INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY, "name" VARCHAR(64), UNIQUE INDEX "idx_user_name" ("name")) COMMENT 'users'""",
            create.toSql()
        )
    }

    @Test
    fun rendersTableAndIndexDdlOperations() {
        val query = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(col("id"))), from = listOf(table("user")))

        assertEquals(
            "CREATE TABLE IF NOT EXISTS \"user_copy\" AS SELECT \"id\" FROM \"user\"",
            SqlDdlStatement.CreateTableAsSelect(id("user_copy"), query).toSql()
        )
        assertEquals(
            """ALTER TABLE "user" ADD COLUMN "created_at" TIMESTAMP""",
            SqlDdlStatement.AlterTable.AddColumn(id("user"), SqlColumnDefinition(id("created_at"), SqlType.Timestamp())).toSql()
        )
        assertEquals(
            "ALTER TABLE \"user\" DROP COLUMN \"legacy\"",
            SqlDdlStatement.AlterTable.DropColumn(id("user"), id("legacy")).toSql()
        )
        assertEquals(
            """ALTER TABLE "user" ALTER COLUMN "name" VARCHAR(128)""",
            SqlDdlStatement.AlterTable.ModifyColumn(id("user"), SqlColumnDefinition(id("name"), SqlType.Varchar(128))).toSql()
        )
        assertEquals(
            """CREATE INDEX "idx_user_created" ON "user" ("created_at")""",
            SqlDdlStatement.CreateIndex(id("idx_user_created"), id("user"), cols("created_at")).toSql()
        )
        assertEquals(
            "DROP INDEX \"idx_user_created\" ON \"user\"",
            SqlDdlStatement.DropIndex(id("idx_user_created"), id("user")).toSql()
        )
        assertEquals(
            "DROP TABLE IF EXISTS \"user\"",
            SqlDdlStatement.DropTable(id("user"), ifExists = true).toSql()
        )
    }

    @Test
    fun rendersCommentAndMaintenanceDdlOperations() {
        assertEquals(
            """COMMENT ON TABLE "user" IS 'application users'""",
            SqlDdlStatement.CommentOnTable(id("user"), "application users").toSql()
        )
        assertEquals(
            """COMMENT ON COLUMN "user"."name" IS NULL""",
            SqlDdlStatement.CommentOnColumn(id("user"), id("name"), null).toSql()
        )
        assertEquals(
            """ALTER TABLE "user" COMMENT = 'application users'""",
            SqlDdlStatement.AlterTable.SetTableComment(id("user"), "application users").toSql()
        )
        assertEquals(
            """VACUUM "main" INTO 'backup.db'""",
            SqlDdlStatement.Vacuum(schemaName = id("main"), into = str("backup.db")).toSql()
        )
    }

    @Test
    fun ddlRenderingCanBeOverriddenByDialectRenderers() {
        val renderer = object : StandardSqlRenderer() {
            override fun renderDdl(statement: SqlDdlStatement): String = when (statement) {
                is SqlDdlStatement.Vacuum -> "DIALECT VACUUM"
                else -> super.renderDdl(statement)
            }
        }

        assertEquals("DIALECT VACUUM", renderer.renderStatement(SqlDdlStatement.Vacuum()))
    }

    @Test
    fun rendersSqlServerColumnDefaultChanges() {
        assertEquals(
            "ALTER TABLE [user] ADD DEFAULT 1 FOR [status]",
            SqlDdlStatement.AlterTable.AlterColumnDefault(id("user"), id("status"), num("1"))
                .toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "ALTER TABLE [user] DROP DEFAULT FOR [status]",
            SqlDdlStatement.AlterTable.AlterColumnDefault(id("user"), id("status"), null)
                .toSql(SqlDialect.SqlServer)
        )
    }
}
