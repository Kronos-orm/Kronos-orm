/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RenderedSqlContextTest {
    @Test
    fun collectsNamedParametersInSqlOrderAndBindsValues() {
        val where = SqlExpr.Binary(
            SqlExpr.Binary(col("id"), SqlBinaryOperator.Equal, named("id")),
            SqlBinaryOperator.And,
            SqlExpr.Binary(col("name"), SqlBinaryOperator.Equal, named("name"))
        )
        val query = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(named("id"))),
            from = listOf(table("user")),
            where = where
        )

        val rendered = query.toRenderedSql(
            SqlRenderContext()
                .bind("id", 1)
                .bindAll(mapOf("name" to "Ada"))
        )

        assertEquals("""SELECT :id FROM "user" WHERE "id" = :id AND "name" = :name""", rendered.sql)
        assertEquals(listOf("id", "id", "name"), rendered.parameterNames)
        assertEquals(mapOf("id" to 1, "name" to "Ada"), rendered.parameters)
        assertEquals(listOf(1, 1, "Ada"), rendered.orderedParameters)
    }

    @Test
    fun ignoresParameterLikeTextInsideStringAndQuotedIdentifier() {
        val query = SqlQuery.Select(
            select = listOf(
                SqlSelectItem.Expr(str(":literal")),
                SqlSelectItem.Expr(SqlExpr.Column(columnName = ":quoted")),
                SqlSelectItem.Expr(named("actual"))
            )
        )

        val rendered = query.toRenderedSql(SqlRenderContext(parameterValues = mapOf("actual" to 7)))

        assertEquals("""SELECT ':literal', ":quoted", :actual""", rendered.sql)
        assertEquals(listOf("actual"), rendered.parameterNames)
        assertEquals(mapOf("actual" to 7), rendered.parameters)
    }

    @Test
    fun strictBindingReportsMissingParameters() {
        val error = assertFailsWith<UnboundSqlParameterException> {
            named("missing").toRenderedSql(SqlRenderContext(strictParameterBinding = true))
        }

        assertEquals(listOf("missing"), error.parameterNames)
    }

    @Test
    fun supplementsAstParameterNamesWithUnsafeRawParametersForCompatibility() {
        val where = SqlExpr.Binary(
            named("id"),
            SqlBinaryOperator.Equal,
            SqlExpr.UnsafeRaw("coalesce(:fallback, 0)")
        )

        val rendered = where.toRenderedSql(
            SqlRenderContext(parameterValues = mapOf("id" to 1, "fallback" to 2))
        )

        assertEquals(":id = coalesce(:fallback, 0)", rendered.sql)
        assertEquals(listOf("id", "fallback"), rendered.parameterNames)
        assertEquals(listOf(1, 2), rendered.orderedParameters)
    }

    @Test
    fun rendersPlannerBatchAndRejectsMultiStatementSingleRender() {
        val first = SqlDmlStatement.Update(
            table = table("user"),
            setPairs = listOf(set("name", named("name"))),
            where = col("id").eq(named("id"))
        )
        val second = SqlDmlStatement.Delete(
            table = table("user_audit"),
            where = col("user_id").eq(named("id"))
        )
        val planner = SqlDialectPlanner { _, _ -> SqlPlan(listOf(first, second)) }
        val context = SqlRenderContext(
            planner = planner,
            parameterValues = mapOf("name" to "Ada", "id" to 1)
        )

        val error = assertFailsWith<MultiStatementSqlPlanException> {
            first.toRenderedSql(context)
        }
        val batch = first.toRenderedSqlBatch(context)

        assertEquals(2, error.statementCount)
        assertEquals("""UPDATE "user" SET "name" = :name WHERE "id" = :id""", batch.statements[0].sql)
        assertEquals("""DELETE FROM "user_audit" WHERE "user_id" = :id""", batch.statements[1].sql)
        assertEquals(listOf("name", "id"), batch.statements[0].parameterNames)
        assertEquals(listOf("id"), batch.statements[1].parameterNames)
    }
}
