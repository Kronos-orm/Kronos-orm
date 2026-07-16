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
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.limit.SqlFetch
import com.kotlinorm.syntax.limit.SqlFetchMode
import com.kotlinorm.syntax.limit.SqlFetchUnit
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlNullsOrdering
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlAssignmentTarget
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlColumnPosition
import com.kotlinorm.syntax.statement.SqlConflictTarget
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlReturning
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlServerExtendedPropertyOperation
import com.kotlinorm.syntax.statement.SqlUpsertAction
import com.kotlinorm.syntax.statement.SqlUpdateSetPair
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.token.SqlUnsafeToken
import kotlin.test.Test
import kotlin.test.assertEquals

class DialectRendererCoverageTest {
    @Test
    fun rendersMysqlSpecificBranches() {
        val upsertDoNothing = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id", "name"),
            values = listOf(num("1"), str("Ada")),
            primaryKeys = cols("id"),
            action = SqlUpsertAction.DoNothing,
            returning = SqlReturning(listOf(SqlSelectItem.Expr(col("id"))))
        )
        val upsertExplicit = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id", "name"),
            values = listOf(num("1"), str("Ada")),
            primaryKeys = cols("id"),
            action = SqlUpsertAction.Update(
                listOf(
                    SqlUpdateSetPair(SqlAssignmentTarget.Column(id("name")), SqlExpr.ExcludedColumn(id("name"))),
                    SqlUpdateSetPair(SqlAssignmentTarget.Column(id("updated_at")), SqlExpr.Function(id("NOW")))
                )
            )
        )

        assertEquals(
            "INSERT INTO `user` (`id`, `name`) VALUES (1, 'Ada') ON DUPLICATE KEY UPDATE `id` = `id` RETURNING `id`",
            upsertDoNothing.toSql(SqlDialect.MySql)
        )
        assertEquals(
            "INSERT INTO `user` (`id`, `name`) VALUES (1, 'Ada') ON DUPLICATE KEY UPDATE `name` = VALUES (`name`), `updated_at` = NOW()",
            upsertExplicit.toSql(SqlDialect.MySql)
        )
        assertEquals(
            "ALTER TABLE `user` ADD COLUMN `age` SIGNED FIRST",
            SqlDdlStatement.AlterTable.AddColumn(
                tableName = id("user"),
                column = SqlColumnDefinition(id("age"), SqlType.Int),
                position = SqlColumnPosition.First
            ).toSql(SqlDialect.MySql)
        )
        assertEquals(
            "ALTER TABLE `user` MODIFY COLUMN `age` SIGNED AFTER `name`",
            SqlDdlStatement.AlterTable.ModifyColumn(
                tableName = id("user"),
                column = SqlColumnDefinition(id("age"), SqlType.Int),
                position = SqlColumnPosition.After(id("name"))
            ).toSql(SqlDialect.MySql)
        )
        assertEquals(
            "SELECT * FROM `user` LIMIT 9223372036854775807 OFFSET 5",
            SqlQuery.Select(from = listOf(table("user")), limit = SqlLimit(offset = num("5"))).toSql(SqlDialect.MySql)
        )
        assertEquals(
            "SELECT * ORDER BY CASE WHEN `name` IS NULL THEN 1 ELSE 0 END DESC, `name` DESC",
            orderBy(SqlOrdering.Desc, SqlNullsOrdering.First).toSql(SqlDialect.MySql)
        )
        assertEquals("TRUNCATE(1.9)", SqlExpr.Function(id("TRUNC"), args = listOf(num("1.9"))).toSql(SqlDialect.MySql))
        assertEquals(
            "CONCAT('a', 'b', 'c')",
            SqlExpr.Function(
                id("CONCAT"),
                args = listOf(str("a"), SqlExpr.Function(id("CONCAT"), args = listOf(str("b"), str("c"))))
            ).toSql(SqlDialect.MySql)
        )
        assertEquals("false", SqlExpr.BooleanLiteral(false).toSql(SqlDialect.MySql))
        assertEquals("NOT(`id` <=> 1)", SqlExpr.Binary(col("id"), SqlBinaryOperator.IsDistinctFrom(false), num("1")).toSql(SqlDialect.MySql))
    }

    @Test
    fun rendersPostgresAndSqliteSpecificBranches() {
        val constrainedUpsert = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id", "name"),
            values = listOf(num("1"), str("Ada")),
            primaryKeys = cols("id"),
            conflictTarget = SqlConflictTarget(constraintName = id("user_name_key")),
            action = SqlUpsertAction.DoNothing
        )
        val filteredUpsert = constrainedUpsert.copy(
            conflictTarget = SqlConflictTarget(columns = cols("id"), where = col("active").eq(bool(true))),
            action = SqlUpsertAction.Update(
                setPairs = listOf(SqlUpdateSetPair(SqlAssignmentTarget.Column(id("name")), SqlExpr.ExcludedColumn(id("name")))),
                where = col("active").eq(bool(true))
            )
        )

        assertEquals(
            """INSERT INTO "user" ("id", "name") VALUES (1, 'Ada') ON CONFLICT ON CONSTRAINT "user_name_key" DO NOTHING""",
            constrainedUpsert.toSql(SqlDialect.PostgreSql)
        )
        assertEquals(
            """INSERT INTO "user" ("id", "name") VALUES (1, 'Ada') ON CONFLICT ("id") WHERE "active" = TRUE DO UPDATE SET "name" = EXCLUDED."name" WHERE "active" = TRUE""",
            filteredUpsert.toSql(SqlDialect.PostgreSql)
        )
        assertEquals(
            "SELECT * OFFSET 2 ROWS FETCH NEXT 50 PERCENT ROWS WITH TIES",
            SqlQuery.Select(
                limit = SqlLimit(
                    offset = num("2"),
                    fetch = SqlFetch(num("50"), SqlFetchUnit.Percentage, SqlFetchMode.WithTies)
                )
            ).toSql(SqlDialect.PostgreSql)
        )
        assertEquals("""SUBSTRING("name" FROM 1 FOR 2)""", SqlExpr.Function(id("SUBSTR"), args = listOf(col("name"), num("1"), num("2"))).toSql(SqlDialect.PostgreSql))
        assertEquals("""SUBSTRING("name" FROM -3)""", SqlExpr.Function(id("RIGHT"), args = listOf(col("name"), num("3"))).toSql(SqlDialect.PostgreSql))
        assertEquals("RIGHT()", SqlExpr.Function(id("RIGHT")).toSql(SqlDialect.PostgreSql))
        assertEquals(
            """INSERT OR IGNORE INTO "user" ("id", "name") VALUES (1, 'Ada')""",
            constrainedUpsert.toSql(SqlDialect.SQLite)
        )
        assertEquals(
            """SELECT * FROM "user" LIMIT 9223372036854775807 OFFSET 4""",
            SqlQuery.Select(from = listOf(table("user")), limit = SqlLimit(offset = num("4"))).toSql(SqlDialect.SQLite)
        )
        assertEquals("RANDOM()", SqlExpr.Function(id("RAND"), args = listOf(num("1"))).toSql(SqlDialect.SQLite))
    }

    @Test
    fun rendersOracleSpecificBranches() {
        assertEquals("MOD(7, 3)", SqlExpr.Binary(num("7"), SqlBinaryOperator.Mod, num("3")).toSql(SqlDialect.Oracle))
        assertEquals("DBMS_RANDOM.VALUE", SqlExpr.Function(id("RAND")).toSql(SqlDialect.Oracle))
        assertEquals("'a' || ',' || 'b' || ',' || 'c'", SqlExpr.Function(id("JOIN"), args = listOf(str(","), str("a"), str("b"), str("c"))).toSql(SqlDialect.Oracle))
        assertEquals("''", SqlExpr.Function(id("JOIN"), args = listOf(str(","))).toSql(SqlDialect.Oracle))
        assertEquals("SUBSTR(\"NAME\", 1, 2)", SqlExpr.Function(id("LEFT"), args = listOf(col("name"), num("2"))).toSql(SqlDialect.Oracle))
        assertEquals("SUBSTR(\"NAME\", -2)", SqlExpr.Function(id("RIGHT"), args = listOf(col("name"), num("2"))).toSql(SqlDialect.Oracle))
        assertEquals("LEFT()", SqlExpr.Function(id("LEFT")).toSql(SqlDialect.Oracle))
        assertEquals("RPAD('x', 3 * LENGTH('x'), 'x')", SqlExpr.Function(id("REPEAT"), args = listOf(str("x"), num("3"))).toSql(SqlDialect.Oracle))
        assertEquals("REPEAT()", SqlExpr.Function(id("REPEAT")).toSql(SqlDialect.Oracle))
        assertEquals("TIME '12:30:00'", SqlExpr.TimeLiteral(SqlTimeType.Time(), "12:30:00").toSql(SqlDialect.Oracle))
        assertEquals(
            """ALTER TABLE "USER" MODIFY("NAME" VARCHAR(64) DEFAULT 'Ada')""",
            SqlDdlStatement.AlterTable.ModifyColumn(id("user"), SqlColumnDefinition(id("name"), SqlType.Varchar(64), defaultValue = str("Ada"))).toSql(SqlDialect.Oracle)
        )
        assertEquals(
            """SELECT "ID", COUNT("ID") AS TOTAL FROM "KT_INTEGRATION_USER" "U" WHERE "U"."STATUS" = :status ORDER BY "ID" ASC""",
            SqlQuery.Select(
                select = listOf(
                    SqlSelectItem.Expr(col("id")),
                    SqlSelectItem.Expr(SqlExpr.Function(id("COUNT"), args = listOf(col("id"))), "total")
                ),
                from = listOf(table("kt_integration_user", "u")),
                where = col("u", "status").eq(named("status")),
                orderBy = listOf(SqlOrderingItem(col("id"), SqlOrdering.Asc))
            ).toSql(SqlDialect.Oracle)
        )
    }

    @Test
    fun rendersSqlServerSpecificBranches() {
        assertEquals(
            "SELECT TOP (1) COUNT(1) AS total FROM [kt_integration_user]",
            SqlQuery.Select(
                select = listOf(SqlSelectItem.Expr(SqlExpr.Function(id("COUNT"), args = listOf(num("1"))), "total")),
                from = listOf(table("kt_integration_user")),
                limit = SqlLimit.limit(1)
            ).toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[user]') AND type in (N'U')) BEGIN CREATE TABLE [dbo].[user] ([id] INTEGER IDENTITY NOT NULL PRIMARY KEY); END",
            SqlDdlStatement.CreateTable(
                tableName = id("dbo", "user"),
                columns = listOf(SqlColumnDefinition(id("id"), SqlType.Int, nullable = false, primaryKey = com.kotlinorm.syntax.statement.SqlPrimaryKeyMode.Identity)),
                ifNotExists = true
            ).toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "DROP INDEX [idx_user_name] ON [user]",
            SqlDdlStatement.DropIndex(id("idx_user_name"), id("user")).toSql(SqlDialect.SqlServer)
        )
        assertEquals("CEILING(1.2)", SqlExpr.Function(id("CEIL"), args = listOf(num("1.2"))).toSql(SqlDialect.SqlServer))
        assertEquals("LOG(10, EXP(1))", SqlExpr.Function(id("LN"), args = listOf(num("10"))).toSql(SqlDialect.SqlServer))
        assertEquals("LN()", SqlExpr.Function(id("LN")).toSql(SqlDialect.SqlServer))
        assertEquals("LEN([name])", SqlExpr.Function(id("LENGTH"), args = listOf(col("name"))).toSql(SqlDialect.SqlServer))
        assertEquals("REPLICATE('x', 3)", SqlExpr.Function(id("REPEAT"), args = listOf(str("x"), num("3"))).toSql(SqlDialect.SqlServer))
        assertEquals("ROUND(1.25, 1)", SqlExpr.Function(id("TRUNC"), args = listOf(num("1.25"), num("1"))).toSql(SqlDialect.SqlServer))
        assertEquals(
            "EXEC sys.sp_dropextendedproperty @name = N'MS_Description', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'user'",
            SqlDdlStatement.SqlServerExtendedPropertyComment(
                tableName = id("dbo", "user"),
                comment = null,
                operation = SqlServerExtendedPropertyOperation.Drop
            ).toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            """
            DECLARE @ConstraintName NVARCHAR(128);
            SET @ConstraintName = (
                SELECT dc.name
                FROM sys.default_constraints dc
                INNER JOIN sys.columns c ON c.default_object_id = dc.object_id
                INNER JOIN sys.tables t ON t.object_id = c.object_id
                INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
                WHERE s.name = N'dbo'
                    AND t.name = N'user'
                    AND c.name = N'name'
            );
            IF @ConstraintName IS NOT NULL
                BEGIN
                    DECLARE @DropStmt NVARCHAR(MAX) = N'ALTER TABLE [dbo].[user] DROP CONSTRAINT ' + QUOTENAME(@ConstraintName);
                    EXEC sp_executesql @DropStmt;
                END
            """.trimIndent(),
            SqlDdlStatement.SqlServerDropDefaultConstraint(id("dbo", "user"), id("name")).toSql(SqlDialect.SqlServer)
        )
    }

    @Test
    fun rendersBooleanLiteralsSafelyInPredicatePositions() {
        val selectFalse = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(col("id"))),
            from = listOf(table("user")),
            where = bool(false)
        )
        assertEquals("FALSE", bool(false).toSql(SqlDialect.SqlServer))
        assertEquals("FALSE", bool(false).toSql(SqlDialect.Oracle))
        assertEquals("SELECT [id] FROM [user] WHERE 1 = 0", selectFalse.toSql(SqlDialect.SqlServer))
        assertEquals("""SELECT "ID" FROM "USER" WHERE 1 = 0""", selectFalse.toSql(SqlDialect.Oracle))
        assertEquals("""SELECT "id" FROM "user" WHERE FALSE""", selectFalse.toSql(SqlDialect.PostgreSql))

        val havingTrue = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(SqlExpr.Function(id("COUNT"), args = listOf(num("1"))))),
            from = listOf(table("user")),
            having = bool(true)
        )
        assertEquals("SELECT COUNT(1) FROM [user] HAVING 1 = 1", havingTrue.toSql(SqlDialect.SqlServer))

        val logicalPredicate = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(col("id"))),
            from = listOf(table("user")),
            where = SqlExpr.Binary(
                bool(false),
                SqlBinaryOperator.Or,
                SqlExpr.Unary(com.kotlinorm.syntax.expr.SqlUnaryOperator.Not, bool(true))
            )
        )
        assertEquals(
            "SELECT [id] FROM [user] WHERE 1 = 0 OR NOT(1 = 1)",
            logicalPredicate.toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            """SELECT "ID" FROM "USER" WHERE 1 = 0 OR NOT(1 = 1)""",
            logicalPredicate.toSql(SqlDialect.Oracle)
        )

        val joined = SqlQuery.Select(
            from = listOf(
                SqlTable.Join(
                    left = table("user", "u"),
                    joinType = SqlJoinType.Inner,
                    right = table("role", "r"),
                    condition = SqlJoinCondition.On(bool(true))
                )
            )
        )
        assertEquals(
            "SELECT * FROM [user] AS [u] INNER JOIN [role] AS [r] ON 1 = 1",
            joined.toSql(SqlDialect.SqlServer)
        )

        assertEquals(
            "DELETE FROM [user] WHERE 1 = 0",
            SqlDmlStatement.Delete(table("user"), where = bool(false)).toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "UPDATE [user] SET [active] = 1 WHERE 1 = 1",
            SqlDmlStatement.Update(table("user"), listOf(set("active", num("1"))), where = bool(true)).toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "COUNT(*) FILTER (WHERE 1 = 0)",
            SqlExpr.CountAsteriskFunc(filter = bool(false)).toSql(SqlDialect.SqlServer)
        )

        val filteredMerge = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id", "name"),
            values = listOf(num("1"), str("Ada")),
            primaryKeys = cols("id"),
            action = SqlUpsertAction.Update(
                setPairs = listOf(SqlUpdateSetPair(SqlAssignmentTarget.Column(id("name")), SqlExpr.ExcludedColumn(id("name")))),
                where = bool(false)
            )
        )
        assertEquals(
            "MERGE INTO [user] AS [t1] USING (SELECT 1 AS [id], N'Ada' AS [name]) AS [t2] ON ([t1].[id] = [t2].[id]) WHEN MATCHED THEN UPDATE SET [t1].[name] = [t2].[name] WHERE 1 = 0 WHEN NOT MATCHED THEN INSERT ([id], [name]) VALUES (1, N'Ada');",
            filteredMerge.toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            """MERGE INTO "USER" "T1" USING (SELECT 1 AS "ID", 'Ada' AS "NAME") "T2" ON ("T1"."ID" = "T2"."ID") WHEN MATCHED THEN UPDATE SET "T1"."NAME" = "T2"."NAME" WHERE 1 = 0 WHEN NOT MATCHED THEN INSERT ("ID", "NAME") VALUES (1, 'Ada')""",
            filteredMerge.toSql(SqlDialect.Oracle)
        )

        val filteredOnConflictUpsert = filteredMerge.copy(
            conflictTarget = SqlConflictTarget(columns = cols("id"), where = bool(true))
        )
        assertEquals(
            """INSERT INTO "user" ("id", "name") VALUES (1, 'Ada') ON CONFLICT ("id") WHERE TRUE DO UPDATE SET "name" = EXCLUDED."name" WHERE FALSE""",
            filteredOnConflictUpsert.toSql(SqlDialect.PostgreSql)
        )
        assertEquals(
            """INSERT INTO "user" ("id", "name") VALUES (1, 'Ada') ON CONFLICT ("id") WHERE TRUE DO UPDATE SET "name" = EXCLUDED."name" WHERE FALSE""",
            filteredOnConflictUpsert.toSql(SqlDialect.SQLite)
        )
    }

    @Test
    fun rendersStandardBranchesStillUsedByDialects() {
        val tokens = listOf(SqlUnsafeToken.Text("MATCH"), SqlUnsafeToken.Identifier("name"), SqlUnsafeToken.Expr(str("Ada")))

        assertEquals("(MATCH \"name\" 'Ada')", SqlExpr.UnsafeCustom(tokens).toSql())
        assertEquals("MATCH \"name\" 'Ada'", UnsafeTokenSqlRenderer().renderToken(SqlUnsafeToken.Text("MATCH \"name\" 'Ada'")))
        assertEquals("COUNT(\"user\".*) FILTER (WHERE \"active\" = TRUE)", SqlExpr.CountAsteriskFunc("user", col("active").eq(bool(true))).toSql())
        assertEquals("INTERVAL '2' DAY TO SECOND", SqlExpr.IntervalLiteral("2", com.kotlinorm.syntax.expr.SqlIntervalField.To(com.kotlinorm.syntax.expr.SqlTimeUnit.Day, com.kotlinorm.syntax.expr.SqlTimeUnit.Second)).toSql())
        assertEquals("\"score\" >= ALL (SELECT \"score\" FROM \"user\")", quantified(com.kotlinorm.syntax.expr.SqlQuantifiedComparisonOperator.GreaterThanEqual).toSql())
        assertEquals("\"score\" < ANY (SELECT \"score\" FROM \"user\")", quantified(com.kotlinorm.syntax.expr.SqlQuantifiedComparisonOperator.LessThan).copy(quantifier = com.kotlinorm.syntax.expr.SqlSubqueryQuantifier.Any).toSql())
        assertEquals(
            "SELECT * FOR SHARE OF \"user\" SKIP LOCKED",
            SqlQuery.Select(
                lock = com.kotlinorm.syntax.statement.SqlLock.Share(
                    targets = listOf(id("user")),
                    waitMode = com.kotlinorm.syntax.statement.SqlLockWaitMode.SkipLocked
                )
            ).toSql()
        )
    }

    private fun orderBy(ordering: SqlOrdering, nullsOrdering: SqlNullsOrdering) =
        SqlQuery.Select(orderBy = listOf(SqlOrderingItem(col("name"), ordering, nullsOrdering)))

    private fun quantified(operator: com.kotlinorm.syntax.expr.SqlQuantifiedComparisonOperator) =
        SqlExpr.QuantifiedComparisonPredicate(
            expr = col("score"),
            operator = operator,
            quantifier = com.kotlinorm.syntax.expr.SqlSubqueryQuantifier.All,
            query = SqlQuery.Select(
                select = listOf(SqlSelectItem.Expr(col("score"))),
                from = listOf(table("user"))
            )
        )
}
