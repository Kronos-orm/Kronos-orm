/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.expr.SqlWindow
import com.kotlinorm.syntax.expr.SqlWindowItem
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSetOperator
import java.math.BigDecimal
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateTableAsSelectCoverageTest {
    @Test
    fun inlinesEverySupportedOracleCtasParameterType() {
        val cases = listOf(
            LiteralCase("nullValue", null, "NULL"),
            LiteralCase("stringValue", "O'Brien", "'O''Brien'"),
            LiteralCase("charValue", 'Z', "'Z'"),
            LiteralCase("trueValue", true, "1"),
            LiteralCase("falseValue", false, "0"),
            LiteralCase("decimalValue", BigDecimal("123.4500"), "123.4500"),
            LiteralCase("numberValue", 42L, "42"),
            LiteralCase("enumValue", LiteralStatus.Active, "'Active'"),
            LiteralCase(
                "uuidValue",
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                "'123e4567-e89b-12d3-a456-426614174000'"
            ),
            LiteralCase("localDateValue", LocalDate.parse("2026-01-02"), "TO_DATE('2026-01-02', 'YYYY-MM-DD')"),
            LiteralCase("sqlDateValue", java.sql.Date.valueOf("2026-01-03"), "TO_DATE('2026-01-03', 'YYYY-MM-DD')"),
            LiteralCase("localTimeValue", LocalTime.parse("12:34:56.123456789"), "TIME '12:34:56.123456789'"),
            LiteralCase("sqlTimeValue", Time.valueOf("12:34:56"), "TIME '12:34:56'"),
            LiteralCase(
                "localDateTimeValue",
                LocalDateTime.parse("2026-01-02T03:04:05.123456789"),
                "TO_TIMESTAMP('2026-01-02 03:04:05.123456789', 'YYYY-MM-DD HH24:MI:SS.FF9')"
            ),
            LiteralCase(
                "sqlTimestampValue",
                Timestamp.valueOf("2026-01-03 04:05:06.987654321"),
                "TO_TIMESTAMP('2026-01-03 04:05:06.987654321', 'YYYY-MM-DD HH24:MI:SS.FF9')"
            ),
            LiteralCase(
                "offsetDateTimeValue",
                OffsetDateTime.parse("2026-01-04T05:06:07.111222333+08:00"),
                "TO_TIMESTAMP_TZ('2026-01-04 05:06:07.111222333 +08:00', 'YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM')"
            ),
            LiteralCase(
                "zonedDateTimeValue",
                ZonedDateTime.of(
                    LocalDateTime.parse("2026-01-05T06:07:08.222333444"),
                    ZoneId.of("Asia/Shanghai")
                ),
                "TO_TIMESTAMP_TZ('2026-01-05 06:07:08.222333444 +08:00', 'YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM')"
            ),
            LiteralCase(
                "instantValue",
                Instant.parse("2026-01-06T07:08:09.333444555Z"),
                "TO_TIMESTAMP_TZ('2026-01-06 07:08:09.333444555 +00:00', 'YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM')"
            ),
            LiteralCase("bytesValue", byteArrayOf(0, 15, 127), "HEXTORAW('000f7f')")
        )

        cases.forEach { case ->
            val rendered = oracleCtas(case.name, ifNotExists = false).toRenderedSql(
                SqlRenderContext(SqlDialect.Oracle, parameterValues = mapOf(case.name to case.value))
            )

            assertEquals(
                "CREATE TABLE \"VALUE_COPY\" AS SELECT ${case.expectedLiteral}",
                rendered.sql,
                case.name
            )
            assertEquals(emptyMap(), rendered.parameters, case.name)
            assertEquals(emptyList(), rendered.parameterNames, case.name)
        }
    }

    @Test
    fun rejectsMissingUnsupportedAndListOracleCtasParameters() {
        val missing = assertFailsWith<IllegalArgumentException> {
            oracleCtas("missing").toRenderedSql(SqlRenderContext(SqlDialect.Oracle))
        }
        assertEquals(
            "Oracle CREATE TABLE AS SELECT parameter 'missing' must be bound before rendering.",
            missing.message
        )

        val unsupported = assertFailsWith<IllegalArgumentException> {
            oracleCtas("unsupported").toRenderedSql(
                SqlRenderContext(SqlDialect.Oracle, parameterValues = mapOf("unsupported" to UnsupportedLiteral()))
            )
        }
        assertEquals(
            "Oracle CREATE TABLE AS SELECT parameter 'unsupported' has unsupported literal type " +
                "com.kotlinorm.syntax.render.CreateTableAsSelectCoverageTest.UnsupportedLiteral.",
            unsupported.message
        )

        val listParameter = SqlExpr.Parameter(SqlParameter.Named("ids"), expandAsList = true)
        val listStatement = SqlDdlStatement.CreateTableAsSelect(
            tableName = id("value_copy"),
            query = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(listParameter)))
        )
        val expanded = assertFailsWith<IllegalArgumentException> {
            listStatement.toRenderedSql(
                SqlRenderContext(SqlDialect.Oracle, parameterValues = mapOf("ids" to listOf(1, 2)))
            )
        }
        assertEquals(
            "Oracle CREATE TABLE AS SELECT does not support list parameter 'ids'.",
            expanded.message
        )
    }

    @Test
    fun rendersOracleCtasUsingClauseWhenParametersAreNotPrebound() {
        val statement = oracleCtas("name", ifNotExists = false)

        assertEquals(
            "BEGIN EXECUTE IMMEDIATE 'CREATE TABLE \"VALUE_COPY\" AS SELECT :name' USING :name; END;",
            statement.toSql(SqlDialect.Oracle)
        )
    }

    @Test
    fun preparesOracleCtasParametersForBatchRendering() {
        val batch = oracleCtas("value").toRenderedSqlBatch(
            SqlRenderContext(SqlDialect.Oracle, parameterValues = mapOf("value" to 7))
        )

        assertEquals(1, batch.statements.size)
        assertEquals(
            "BEGIN EXECUTE IMMEDIATE 'CREATE TABLE \"VALUE_COPY\" AS SELECT 7'; " +
                "EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;",
            batch.statements.single().sql
        )
        assertEquals(emptyMap(), batch.statements.single().parameters)
        assertEquals(emptyList(), batch.diagnostics)
    }

    @Test
    fun rendersSqlServerSetAndModifiedSelectCtasWithoutExistenceGuards() {
        val setQuery = SqlQuery.Set(
            left = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(col("id"))), from = listOf(table("active_user"))),
            operator = SqlSetOperator.Union(SqlQuantifier.All),
            right = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(col("id"))), from = listOf(table("archived_user")))
        )
        val modifiedSelect = SqlQuery.Select(
            quantifier = SqlQuantifier.Distinct,
            select = listOf(SqlSelectItem.Expr(col("department"))),
            from = listOf(table("employee")),
            where = col("active").eq(num("1")),
            groupBy = SqlGroup(items = listOf(SqlGroupingItem.Expr(col("department")))),
            having = SqlExpr.CountAsteriskFunc().gt(num("0")),
            window = listOf(SqlWindowItem(id("w"), SqlWindow(partitionBy = listOf(col("department"))))),
            qualify = col("department").eq(str("engineering")),
            orderBy = listOf(SqlOrderingItem(col("department"), SqlOrdering.Asc)),
            limit = SqlLimit.limit(3)
        )
        val offsetSelect = SqlQuery.Select(
            from = listOf(table("employee")),
            orderBy = listOf(SqlOrderingItem(col("id"), SqlOrdering.Desc)),
            limit = SqlLimit.limit(limit = 2, offset = 4)
        )

        assertEquals(
            "SELECT * INTO [combined_user] FROM ((SELECT [id] FROM [active_user]) UNION ALL " +
                "(SELECT [id] FROM [archived_user])) AS [__kronos_ctas_source]",
            SqlDdlStatement.CreateTableAsSelect(id("combined_user"), setQuery, ifNotExists = false)
                .toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "SELECT DISTINCT TOP (3) [department] INTO [department_copy] FROM [employee] WHERE [active] = 1 " +
                "GROUP BY [department] HAVING COUNT(*) > 0 WINDOW [w] AS (PARTITION BY [department]) " +
                "QUALIFY [department] = N'engineering' ORDER BY [department] ASC",
            SqlDdlStatement.CreateTableAsSelect(id("department_copy"), modifiedSelect, ifNotExists = false)
                .toSql(SqlDialect.SqlServer)
        )
        assertEquals(
            "SELECT * INTO [paged_employee] FROM [employee] ORDER BY [id] DESC " +
                "OFFSET 4 ROWS FETCH NEXT 2 ROWS ONLY",
            SqlDdlStatement.CreateTableAsSelect(id("paged_employee"), offsetSelect, ifNotExists = false)
                .toSql(SqlDialect.SqlServer)
        )
    }

    private fun oracleCtas(name: String, ifNotExists: Boolean = true): SqlDdlStatement.CreateTableAsSelect =
        SqlDdlStatement.CreateTableAsSelect(
            tableName = id("value_copy"),
            query = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(named(name)))),
            ifNotExists = ifNotExists
        )

    private data class LiteralCase(val name: String, val value: Any?, val expectedLiteral: String)

    private enum class LiteralStatus {
        Active
    }

    private class UnsupportedLiteral
}
