/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.validate

import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.limit.SqlFetch
import com.kotlinorm.syntax.limit.SqlFetchMode
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSetOperator
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlSyntaxValidatorStatementTraversalTest {
    @Test
    fun traversesDmlAndDdlStatementBranches() {
        val source = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(col("id"))), from = listOf(table("source")))
        val statements = listOf(
            SqlDmlStatement.Delete(table("user"), where = col("id").eq(num("1"))),
            SqlDmlStatement.Truncate(table("audit_log")),
            SqlDmlStatement.Insert(table("copy"), columns = listOf("id"), mode = SqlInsertMode.Subquery(source)),
            SqlDdlStatement.CreateTableAsSelect("copy", source),
            SqlDdlStatement.AlterTable.AddColumn("user", SqlColumnDefinition("nickname", SqlType.Varchar(32), defaultValue = str("n/a"))),
            SqlDdlStatement.AlterTable.ModifyColumn("user", SqlColumnDefinition("nickname", SqlType.Varchar(128))),
            SqlDdlStatement.AlterTable.DropColumn("user", "nickname"),
            SqlDdlStatement.DropTable("old_user", ifExists = true),
            SqlDdlStatement.CreateIndex("idx_user_name", "user", listOf("name", "name")),
            SqlDdlStatement.DropIndex("idx_user_name", "user")
        )

        val diagnostics = statements.flatMap { SqlSyntaxValidator.validate(it) }

        assertEquals(listOf("ddl.index.column.duplicate"), diagnostics.map { it.code })
    }

    @Test
    fun reportsSetFetchWithTiesAndEmptyLimitWarnings() {
        val left = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(col("id"))), from = listOf(table("user")))
        val right = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(col("id"))), from = listOf(table("archive")))
        val set = SqlQuery.Set(
            left = left,
            operator = SqlSetOperator.Union(SqlQuantifier.All),
            right = right,
            limit = SqlLimit(fetch = SqlFetch(num("1"), mode = SqlFetchMode.WithTies))
        )
        val emptyLimit = SqlQuery.Select(limit = SqlLimit())

        val codes = (SqlSyntaxValidator.validate(set) + SqlSyntaxValidator.validate(emptyLimit)).map { it.code }.toSet()

        assertTrue("set.fetch.with.ties.without.order" in codes)
        assertTrue("select.limit.empty" in codes)
    }

    @Test
    fun traversesFunctionSubqueryAndUsingJoinTables() {
        val subquery = SqlTable.Subquery(
            query = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(col("id"))), from = listOf(table("user")))
        )
        val function = SqlTable.Func(name = "unnest", args = listOf(col("tags")))
        val join = SqlTable.Join(
            left = subquery,
            joinType = SqlJoinType.Inner,
            right = function,
            condition = SqlJoinCondition.Using(listOf("id"))
        )
        val query = SqlQuery.Select(
            select = listOf(SqlSelectItem.Asterisk("u")),
            from = listOf(join),
            orderBy = listOf(SqlOrderingItem(col("id"), SqlOrdering.Asc))
        )

        assertEquals(emptyList(), SqlSyntaxValidator.validate(query))
    }

    private fun table(name: String): SqlTable.Ident =
        SqlTable.Ident(name)

    private fun col(column: String): SqlExpr.Column =
        SqlExpr.Column(columnName = column)

    private fun num(value: String): SqlExpr.NumberLiteral =
        SqlExpr.NumberLiteral(value)

    private fun str(value: String): SqlExpr.StringLiteral =
        SqlExpr.StringLiteral(value)

    private fun SqlExpr.eq(other: SqlExpr): SqlExpr.Binary =
        SqlExpr.Binary(this, SqlBinaryOperator.Equal, other)
}
