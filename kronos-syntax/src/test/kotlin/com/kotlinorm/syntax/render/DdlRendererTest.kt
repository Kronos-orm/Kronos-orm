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
            tableName = "user",
            columns = listOf(
                SqlColumnDefinition(
                    name = "id",
                    type = SqlType.Int,
                    nullable = false,
                    primaryKey = SqlPrimaryKeyMode.Identity
                ),
                SqlColumnDefinition(
                    name = "name",
                    type = SqlType.Varchar(64)
                )
            ),
            indexes = listOf(SqlIndexDefinition("idx_user_name", listOf("name"), unique = true)),
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
            SqlDdlStatement.CreateTableAsSelect("user_copy", query).toSql()
        )
        assertEquals(
            """ALTER TABLE "user" ADD COLUMN "created_at" TIMESTAMP""",
            SqlDdlStatement.AlterTable.AddColumn("user", SqlColumnDefinition("created_at", SqlType.Timestamp())).toSql()
        )
        assertEquals(
            "ALTER TABLE \"user\" DROP COLUMN \"legacy\"",
            SqlDdlStatement.AlterTable.DropColumn("user", "legacy").toSql()
        )
        assertEquals(
            """ALTER TABLE "user" ALTER COLUMN "name" VARCHAR(128)""",
            SqlDdlStatement.AlterTable.ModifyColumn("user", SqlColumnDefinition("name", SqlType.Varchar(128))).toSql()
        )
        assertEquals(
            """CREATE INDEX "idx_user_created" ON "user" ("created_at")""",
            SqlDdlStatement.CreateIndex("idx_user_created", "user", listOf("created_at")).toSql()
        )
        assertEquals(
            "DROP INDEX \"idx_user_created\" ON \"user\"",
            SqlDdlStatement.DropIndex("idx_user_created", "user").toSql()
        )
        assertEquals(
            "DROP TABLE IF EXISTS \"user\"",
            SqlDdlStatement.DropTable("user", ifExists = true).toSql()
        )
    }
}
