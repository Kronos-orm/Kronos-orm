/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlLock
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlReturning
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.validate.SqlDialectValidator
import com.kotlinorm.syntax.validate.SqlValidationDiagnostic
import com.kotlinorm.syntax.validate.SqlValidationException
import com.kotlinorm.syntax.validate.SqlValidationSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DialectPipelineTest {
    @Test
    fun appliesQueryAndExpressionLowerersBeforeRendering() {
        val queryLowerer = object : SqlDialectLowerer {
            override fun lowerQuery(query: SqlQuery, dialect: SqlDialect): SqlQuery =
                if (query is SqlQuery.Select) query.copy(lock = null) else query
        }
        val exprLowerer = object : SqlDialectLowerer {
            override fun lowerExpr(expr: SqlExpr, dialect: SqlDialect): SqlExpr =
                if (expr == num("1")) num("2") else expr
        }

        val query = SqlQuery.Select(from = listOf(table("user")), lock = SqlLock.Update())
        val context = SqlRenderContext(lowerer = SqlDialectLowerer.composite(queryLowerer, exprLowerer))

        assertEquals("""SELECT * FROM "user"""", query.toSql(context))
        assertEquals("2", num("1").toSql(context))
    }

    @Test
    fun defaultDialectValidatorReportsUnsupportedDialectForms() {
        val sqliteLock = assertFailsWith<SqlValidationException> {
            SqlQuery.Select(lock = SqlLock.Update()).toRenderedSql(
                SqlRenderContext(dialect = SqlDialect.SQLite, validator = SqlDialectValidator.Default)
            )
        }
        val sqlServerLimit = assertFailsWith<SqlValidationException> {
            SqlQuery.Select(from = listOf(table("user")), limit = SqlLimit.limit(10)).toRenderedSql(
                SqlRenderContext(dialect = SqlDialect.SqlServer, validator = SqlDialectValidator.Default)
            )
        }
        val h2ShareLock = assertFailsWith<SqlValidationException> {
            SqlQuery.Select(lock = SqlLock.Share()).toRenderedSql(
                SqlRenderContext(dialect = SqlDialect.H2, validator = SqlDialectValidator.Default)
            )
        }

        assertEquals("dialect.lock.unsupported", sqliteLock.diagnostics.single().code)
        assertEquals("dialect.sqlserver.limit.requires.order", sqlServerLimit.diagnostics.single().code)
        assertEquals("dialect.lock.unsupported", h2ShareLock.diagnostics.single().code)
        assertEquals(
            emptyList(),
            SqlDialectValidator.Default.validate(SqlQuery.Select(lock = SqlLock.Update()), SqlDialect.H2)
        )
    }

    @Test
    fun defaultDialectValidatorReportsUnsupportedReturning() {
        val insert = SqlDmlStatement.Insert(
            table = table("user"),
            mode = SqlInsertMode.Values(listOf(listOf(num("1")))),
            returning = SqlReturning(listOf(SqlSelectItem.Expr(col("id"))))
        )

        val error = assertFailsWith<SqlValidationException> {
            insert.toRenderedSql(SqlRenderContext(dialect = SqlDialect.MySql, validator = SqlDialectValidator.Default))
        }

        assertEquals("dialect.returning.unsupported", error.diagnostics.single().code)
    }

    @Test
    fun defaultDialectValidatorRejectsReturningForEveryH2DmlStatement() {
        val returning = SqlReturning(listOf(SqlSelectItem.Expr(col("id"))))
        val statements = listOf(
            SqlDmlStatement.Insert(
                table = table("user"),
                mode = SqlInsertMode.Values(listOf(listOf(num("1")))),
                returning = returning
            ),
            SqlDmlStatement.Update(table("user"), setPairs = listOf(set("name", str("Ada"))), returning = returning),
            SqlDmlStatement.Delete(table("user"), returning = returning),
            SqlDmlStatement.Upsert(
                table = table("user"),
                columns = cols("id", "name"),
                values = listOf(num("1"), str("Ada")),
                primaryKeys = cols("id"),
                returning = returning
            )
        )

        val diagnostics = statements.map { statement ->
            assertFailsWith<SqlValidationException> {
                statement.toRenderedSql(SqlRenderContext(dialect = SqlDialect.H2, validator = SqlDialectValidator.Default))
            }.diagnostics.single().code
        }

        assertEquals(List(statements.size) { "dialect.returning.unsupported" }, diagnostics)
    }

    @Test
    fun defaultDialectValidatorAcceptsSupportedTopLevelForms() {
        val validator = SqlDialectValidator.Default
        val returning = SqlReturning(listOf(SqlSelectItem.Expr(col("id"))))

        val diagnostics = listOf(
            validator.validate(SqlQuery.Select(orderBy = listOf(com.kotlinorm.syntax.order.SqlOrderingItem(col("id"))), limit = SqlLimit.limit(10)), SqlDialect.SqlServer),
            validator.validate(SqlDmlStatement.Insert(table("user"), mode = SqlInsertMode.Values(listOf(listOf(num("1"))))), SqlDialect.MySql),
            validator.validate(SqlDmlStatement.Update(table("user"), listOf(set("name", str("Ada")))), SqlDialect.MySql),
            validator.validate(SqlDmlStatement.Delete(table("user")), SqlDialect.MySql),
            validator.validate(SqlDmlStatement.Truncate(table("user")), SqlDialect.MySql),
            validator.validate(SqlDmlStatement.Insert(table("user"), mode = SqlInsertMode.Values(listOf(listOf(num("1")))), returning = returning), SqlDialect.PostgreSql)
        ).flatten()

        assertEquals(emptyList(), diagnostics)
        validator.validateOrThrow(SqlQuery.Select(), SqlDialect.Standard)
        assertEquals(emptyList(), SqlDialectValidator.None.validate(SqlQuery.Select(), SqlDialect.SQLite))
    }

    @Test
    fun renderContextCanBypassDialectValidationErrors() {
        val sql = SqlQuery.Select(lock = SqlLock.Update()).toSql(
            SqlRenderContext(
                dialect = SqlDialect.SQLite,
                validator = SqlDialectValidator.Default,
                validateBeforeRender = false
            )
        )

        assertEquals("""SELECT * FOR UPDATE""", sql)
    }

    @Test
    fun compositeValidatorMergesDiagnostics() {
        val first = staticValidator("first")
        val second = staticValidator("second")

        val diagnostics = SqlDialectValidator.composite(first, second).validate(num("1"), SqlDialect.Standard)

        assertEquals(listOf("first", "second"), diagnostics.map { it.code })
    }

    private fun staticValidator(code: String): SqlDialectValidator = object : SqlDialectValidator {
        override fun validate(node: com.kotlinorm.syntax.SqlNode, dialect: SqlDialect): List<SqlValidationDiagnostic> =
            listOf(SqlValidationDiagnostic(code, code, SqlValidationSeverity.Warning))
    }
}
