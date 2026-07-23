/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlTimeType
import com.kotlinorm.syntax.expr.SqlTimeZoneMode
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlNullsOrdering
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlServerExtendedPropertyOperation
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import kotlin.test.Test
import kotlin.test.assertEquals

class DialectRendererTest {
    @Test
    fun rendersSqalaStyleUpsertPerDialect() {
        val upsert = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id", "name"),
            values = listOf(num("1"), str("Ada")),
            primaryKeys = cols("id")
        )

        assertEquals(
            "INSERT INTO `user` (`id`, `name`) VALUES (1, 'Ada') ON DUPLICATE KEY UPDATE `name` = VALUES (`name`)",
            upsert.toSql(SqlDialect.MySql)
        )
        assertEquals(
            """INSERT INTO "user" ("id", "name") VALUES (1, 'Ada') ON CONFLICT ("id") DO UPDATE SET "name" = EXCLUDED."name"""",
            upsert.toSql(SqlDialect.PostgreSql)
        )
        assertEquals(
            """INSERT INTO "user" ("id", "name") VALUES (1, 'Ada') ON CONFLICT ("id") DO UPDATE SET "name" = EXCLUDED."name"""",
            upsert.toSql(SqlDialect.SQLite)
        )
    }

    @Test
    fun rendersH2MergeWithValuesSourceAndQuotedAliases() {
        val upsert = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id", "name"),
            values = listOf(named("id"), named("name")),
            primaryKeys = cols("id")
        )
        val aliasQuery = SqlQuery.Select(
            select = listOf(
                SqlSelectItem.Expr(
                    SqlExpr.Function(id("LENGTH"), args = listOf(col("name"))),
                    alias = "nameLength"
                )
            ),
            from = listOf(table("user"))
        )
        val modifyColumn = SqlDdlStatement.AlterTable.ModifyColumn(
            tableName = id("user"),
            column = SqlColumnDefinition(id("id"), SqlType.Int, nullable = false, primaryKey = com.kotlinorm.syntax.statement.SqlPrimaryKeyMode.Primary)
        )

        assertEquals(
            "MERGE INTO \"user\" AS \"t1\" USING (VALUES (:id, :name)) AS \"t2\" (\"id\", \"name\") ON (\"t1\".\"id\" = \"t2\".\"id\") WHEN MATCHED THEN UPDATE SET \"t1\".\"name\" = \"t2\".\"name\" WHEN NOT MATCHED THEN INSERT (\"id\", \"name\") VALUES (\"t2\".\"id\", \"t2\".\"name\")",
            upsert.toSql(SqlDialect.H2)
        )
        assertEquals(
            "SELECT LENGTH(\"name\") AS \"nameLength\" FROM \"user\"",
            aliasQuery.toSql(SqlDialect.H2)
        )
        assertEquals(
            "ALTER TABLE \"user\" ALTER COLUMN \"id\" INTEGER NOT NULL",
            modifyColumn.toSql(SqlDialect.H2)
        )
    }

    @Test
    fun rendersDialectSpecificLimitValuesAndFunctions() {
        val query = SqlQuery.Select(
            from = listOf(table("user")),
            limit = SqlLimit.limit(limit = 10, offset = 5)
        )
        val values = SqlQuery.Values(listOf(listOf(num("1"), str("Ada")), listOf(num("2"), str("Grace"))))
        val concat = SqlExpr.Binary(str("a"), SqlBinaryOperator.Concat, str("b"))
        val listAgg = SqlExpr.ListAggFunc(
            expr = col("name"),
            separator = str(","),
            withinGroup = listOf(SqlOrderingItem(col("name"), SqlOrdering.Asc))
        )

        assertEquals("SELECT * FROM `user` LIMIT 10 OFFSET 5", query.toSql(SqlDialect.MySql))
        assertEquals("""SELECT * FROM "user" LIMIT 10 OFFSET 5""", query.toSql(SqlDialect.PostgreSql))
        assertEquals("""SELECT * FROM "user" LIMIT 10 OFFSET 5""", query.toSql(SqlDialect.SQLite))
        assertEquals("VALUES ROW(1, 'Ada'), ROW(2, 'Grace')", values.toSql(SqlDialect.MySql))
        assertEquals("CONCAT('a', 'b')", concat.toSql(SqlDialect.MySql))
        assertEquals("GROUP_CONCAT(`name` ORDER BY `name` ASC SEPARATOR ',')", listAgg.toSql(SqlDialect.MySql))
        assertEquals("""STRING_AGG("name", ',' ORDER BY "name" ASC)""", listAgg.toSql(SqlDialect.PostgreSql))
        assertEquals("""GROUP_CONCAT("name", ',' ORDER BY "name" ASC)""", listAgg.toSql(SqlDialect.SQLite))
    }

    @Test
    fun rendersDialectSpecificDdlTypes() {
        val create = SqlDdlStatement.CreateTable(
            tableName = id("account"),
            columns = listOf(
                SqlColumnDefinition(id("id"), SqlType.Int, nullable = false),
                SqlColumnDefinition(id("name"), SqlType.Varchar(64)),
                SqlColumnDefinition(id("created_at"), SqlType.Timestamp())
            )
        )

        assertEquals(
            "CREATE TABLE `account` (`id` SIGNED NOT NULL, `name` CHAR(64), `created_at` DATETIME)",
            create.toSql(SqlDialect.MySql)
        )
        assertEquals(
            """CREATE TABLE "account" ("id" INTEGER NOT NULL, "name" VARCHAR(64), "created_at" TIMESTAMP)""",
            create.toSql(SqlDialect.PostgreSql)
        )
    }

    @Test
    fun rendersDialectSpecificDdlCommentsAndMaintenanceStatements() {
        assertEquals(
            "ALTER TABLE `user` COMMENT = 'application users'",
            SqlDdlStatement.CommentOnTable(id("user"), "application users").toSql(SqlDialect.MySql)
        )
        assertEquals(
            "ALTER TABLE `user` COMMENT = ''",
            SqlDdlStatement.AlterTable.SetTableComment(id("user"), null).toSql(SqlDialect.MySql)
        )
        assertEquals(
            """COMMENT ON COLUMN "user"."name" IS 'display name'""",
            SqlDdlStatement.CommentOnColumn(id("user"), id("name"), "display name").toSql(SqlDialect.PostgreSql)
        )
        assertEquals(
            """COMMENT ON TABLE "user" IS 'application users'""",
            SqlDdlStatement.AlterTable.SetTableComment(id("user"), "application users").toSql(SqlDialect.PostgreSql)
        )
        assertEquals(
            """COMMENT ON TABLE "USER" IS 'application users'""",
            SqlDdlStatement.AlterTable.SetTableComment(id("user"), "application users").toSql(SqlDialect.Oracle)
        )
        assertEquals(
            """VACUUM "main" INTO 'backup.db'""",
            SqlDdlStatement.Vacuum(schemaName = id("main"), into = str("backup.db")).toSql(SqlDialect.SQLite)
        )
        assertEquals(
            "EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'application users', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'user'",
            SqlDdlStatement.CommentOnTable(id("dbo", "user"), "application users").toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'display name', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'user', @level2type = N'COLUMN', @level2name = N'name'",
            SqlDdlStatement.CommentOnColumn(id("dbo", "user"), id("name"), "display name").toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "EXEC sys.sp_dropextendedproperty @name = N'MS_Description', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'user'",
            SqlDdlStatement.CommentOnTable(id("dbo", "user"), null).toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "EXEC sys.sp_updateextendedproperty @name = N'MS_Description', @value = N'new display name', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'user', @level2type = N'COLUMN', @level2name = N'name'",
            SqlDdlStatement.SqlServerExtendedPropertyComment(
                tableName = id("dbo", "user"),
                columnName = id("name"),
                comment = "new display name",
                operation = SqlServerExtendedPropertyOperation.Update
            ).toSql(SqlDialect.SqlServer)
        )
    }

    @Test
    fun rendersOracleAndSqlServerDialectSpecificForms() {
        val oracleTimestamp = SqlExpr.TimeLiteral(SqlTimeType.Timestamp(), "2026-01-01 12:30:00")
        val oracleTimestampWithTimeZone = SqlExpr.TimeLiteral(
            SqlTimeType.Timestamp(SqlTimeZoneMode.WithTimeZone),
            "2026-01-01 12:30:00.000000000 +08:00"
        )
        val oracleDate = SqlExpr.TimeLiteral(SqlTimeType.Date, "2026-01-01")
        val sqlServerTimestamp = SqlExpr.TimeLiteral(
            SqlTimeType.Timestamp(SqlTimeZoneMode.WithTimeZone),
            "2026-01-01 12:30:00 +08:00"
        )
        val sqlServerTimestampWithoutTimeZone = SqlExpr.TimeLiteral(SqlTimeType.Timestamp(), "2026-01-01 12:30:00")
        val sqlServerDate = SqlExpr.TimeLiteral(SqlTimeType.Date, "2026-01-01")
        val sqlServerTime = SqlExpr.TimeLiteral(SqlTimeType.Time(), "12:30:00")
        val listAgg = SqlExpr.ListAggFunc(
            expr = col("name"),
            separator = str(","),
            withinGroup = listOf(SqlOrderingItem(col("name"), SqlOrdering.Asc))
        )
        val sqlServerOrder = SqlQuery.Select(
            orderBy = listOf(SqlOrderingItem(col("name"), SqlOrdering.Asc, SqlNullsOrdering.Last))
        )

        assertEquals(
            """TO_TIMESTAMP('2026-01-01 12:30:00', 'YYYY-MM-DD HH24:MI:SS.FF9')""",
            oracleTimestamp.toSql(SqlDialect.Oracle)
        )
        assertEquals(
            """TO_TIMESTAMP_TZ('2026-01-01 12:30:00.000000000 +08:00', 'YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM')""",
            oracleTimestampWithTimeZone.toSql(SqlDialect.Oracle)
        )
        assertEquals("""TO_DATE('2026-01-01', 'YYYY-MM-DD')""", oracleDate.toSql(SqlDialect.Oracle))
        assertEquals(
            """"USER" "U"("ID")""",
            SqlTable.Ident("user", alias = SqlTableAlias("u", listOf("id"))).toSql(SqlDialect.Oracle)
        )
        assertEquals(
            """"USER" "U"""",
            SqlTable.Ident("user", alias = SqlTableAlias("u")).toSql(SqlDialect.Oracle)
        )
        assertEquals(
            """CREATE TABLE "ACCOUNT" ("NAME" VARCHAR(4000), "SCORE" INT)""",
            SqlDdlStatement.CreateTable(
                tableName = id("account"),
                columns = listOf(
                    SqlColumnDefinition(id("name"), SqlType.Varchar()),
                    SqlColumnDefinition(id("score"), SqlType.Long)
                )
            ).toSql(SqlDialect.Oracle)
        )
        assertEquals("N'Ada'", str("Ada").toSql(SqlDialect.SqlServer))
        assertEquals(
            "CAST(N'2026-01-01 12:30:00 +08:00' AS DATETIMEOFFSET)",
            sqlServerTimestamp.toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "CAST(N'2026-01-01 12:30:00' AS DATETIME2)",
            sqlServerTimestampWithoutTimeZone.toSql(SqlDialect.SqlServer)
        )
        assertEquals("CAST(N'2026-01-01' AS DATE)", sqlServerDate.toSql(SqlDialect.SqlServer))
        assertEquals("TIME '12:30:00'", sqlServerTime.toSql(SqlDialect.SqlServer))
        assertEquals(
            "STRING_AGG([name], N',') WITHIN GROUP (ORDER BY [name] ASC)",
            listAgg.toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "SELECT * ORDER BY CASE WHEN [name] IS NULL THEN 1 ELSE 0 END ASC, [name] ASC",
            sqlServerOrder.toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "SELECT * ORDER BY CASE WHEN [name] IS NULL THEN 1 ELSE 0 END DESC, [name] DESC",
            SqlQuery.Select(orderBy = listOf(SqlOrderingItem(col("name"), SqlOrdering.Desc, SqlNullsOrdering.First))).toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "SELECT * ORDER BY [name] DESC",
            SqlQuery.Select(orderBy = listOf(SqlOrderingItem(col("name"), SqlOrdering.Desc, SqlNullsOrdering.Last))).toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "CREATE TABLE [account] ([name] NVARCHAR(32), [created_at] DATETIME2)",
            SqlDdlStatement.CreateTable(
                tableName = id("account"),
                columns = listOf(
                    SqlColumnDefinition(id("name"), SqlType.Varchar(32)),
                    SqlColumnDefinition(id("created_at"), SqlType.Timestamp())
                )
            ).toSql(SqlDialect.SqlServer)
        )
    }
}
