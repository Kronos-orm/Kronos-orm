/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.validate

import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.statement.SqlConflictTarget
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlIndexDefinition
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlPrimaryKeyMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlReturning
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlTableConstraint
import com.kotlinorm.syntax.statement.SqlUpsertAction
import com.kotlinorm.syntax.table.SqlTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SqlSyntaxValidatorDmlDdlTest {
    @Test
    fun reportsInsertUpdateAndUpsertDiagnostics() {
        val insert = SqlDmlStatement.Insert(
            table = table("user"),
            columns = cols("id", "id"),
            mode = SqlInsertMode.Values(listOf(listOf(num("1"))))
        )
        val update = SqlDmlStatement.Update(
            table = table("user"),
            setPairs = listOf(set("name", str("Ada")), set("name", str("Grace")))
        )
        val upsert = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id", "name"),
            values = listOf(num("1")),
            primaryKeys = cols("id", "missing"),
            action = SqlUpsertAction.Update(emptyList())
        )

        val codes = (SqlSyntaxValidator.validate(insert) +
            SqlSyntaxValidator.validate(update) +
            SqlSyntaxValidator.validate(upsert)).map { it.code }.toSet()

        assertEquals(
            setOf(
                "insert.columns.duplicate",
                "insert.values.arity",
                "update.set.duplicate",
                "upsert.values.arity",
                "upsert.update.empty",
                "upsert.column.unknown"
            ),
            codes
        )
    }

    @Test
    fun reportsUpsertConflictTargetDiagnostics() {
        val duplicateAndUnknown = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id", "name"),
            values = listOf(num("1"), str("Ada")),
            primaryKeys = cols("id", "id"),
            action = SqlUpsertAction.Update(
                setPairs = listOf(
                    set("name", str("Ada")),
                    set("name", str("Grace")),
                    set("missing", str("unknown"))
                ),
                where = col("active").eq(bool(true))
            ),
            returning = SqlReturning(listOf(SqlSelectItem.Expr(col("id"))))
        )
        val emptyTarget = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id"),
            values = listOf(num("1")),
            primaryKeys = emptyList(),
            conflictTarget = SqlConflictTarget(),
            action = SqlUpsertAction.DoNothing
        )

        val codes = (SqlSyntaxValidator.validate(duplicateAndUnknown) +
            SqlSyntaxValidator.validate(emptyTarget)).map { it.code }.toSet()

        assertEquals(
            setOf(
                "upsert.primary.duplicate",
                "upsert.update.duplicate",
                "upsert.column.unknown",
                "upsert.target.empty"
            ),
            codes
        )
    }

    @Test
    fun acceptsConstraintConflictTargetWithoutPrimaryKeys() {
        val upsert = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = cols("id"),
            values = listOf(num("1")),
            primaryKeys = emptyList(),
            conflictTarget = SqlConflictTarget(constraintName = id("uk_user_id")),
            action = SqlUpsertAction.DoNothing
        )

        assertEquals(emptyList(), SqlSyntaxValidator.validate(upsert))
    }

    @Test
    fun reportsCreateTableDiagnostics() {
        val create = SqlDdlStatement.CreateTable(
            tableName = id("user"),
            columns = listOf(
                SqlColumnDefinition(id("id"), SqlType.Int, primaryKey = SqlPrimaryKeyMode.Primary),
                SqlColumnDefinition(id("id"), SqlType.Long, primaryKey = SqlPrimaryKeyMode.Identity)
            ),
            indexes = listOf(SqlIndexDefinition(id("idx_missing"), cols("missing")))
        )

        val codes = SqlSyntaxValidator.validate(create).map { it.code }.toSet()

        assertEquals(
            setOf(
                "ddl.column.duplicate",
                "ddl.primary.multiple",
                "ddl.index.column.unknown"
            ),
            codes
        )
    }

    @Test
    fun traversesRemainingDdlBranchesWithoutDiagnostics() {
        val source = SqlQuery.Select(select = listOf(SqlSelectItem.Expr(num("1"))))
        val statements = listOf(
            SqlDdlStatement.CreateTableAsSelect(id("copy_user"), source),
            SqlDdlStatement.AlterTable.RenameColumn(id("user"), id("nickname"), id("display_name")),
            SqlDdlStatement.AlterTable.RenameTable(id("user"), id("app_user")),
            SqlDdlStatement.AlterTable.AlterColumnDefault(id("user"), id("name"), str("anonymous")),
            SqlDdlStatement.AlterTable.AlterColumnDefault(id("user"), id("name"), null),
            SqlDdlStatement.AlterTable.AlterColumnNullable(id("user"), id("name"), nullable = false),
            SqlDdlStatement.AlterTable.SetTableComment(id("user"), "users"),
            SqlDdlStatement.DropTable(id("old_user"), ifExists = true),
            SqlDdlStatement.AddConstraint(id("user"), SqlTableConstraint.PrimaryKey(columns = cols("id"))),
            SqlDdlStatement.AddConstraint(id("user"), SqlTableConstraint.Unique(columns = cols("email"))),
            SqlDdlStatement.AddConstraint(id("user"), SqlTableConstraint.Check(condition = col("age").eq(num("18")))),
            SqlDdlStatement.AddConstraint(
                tableName = id("orders"),
                constraint = SqlTableConstraint.ForeignKey(
                    columns = cols("user_id"),
                    referencedTable = id("user"),
                    referencedColumns = cols("id")
                )
            ),
            SqlDdlStatement.DropConstraint(id("user"), id("pk_user")),
            SqlDdlStatement.CommentOnTable(id("user"), "users"),
            SqlDdlStatement.CommentOnColumn(id("user"), id("name"), "display name"),
            SqlDdlStatement.Vacuum(into = str("backup.db")),
            SqlDdlStatement.SqlServerExtendedPropertyComment(id("user"), comment = "users"),
            SqlDdlStatement.SqlServerDropDefaultConstraint(id("user"), id("name"))
        )

        assertEquals(emptyList(), statements.flatMap { SqlSyntaxValidator.validate(it) })
    }

    @Test
    fun throwsWhenValidationFindsErrors() {
        val invalid = SqlDmlStatement.Insert(
            table = table("user"),
            columns = cols("id", "name"),
            mode = SqlInsertMode.Values(listOf(listOf(num("1"))))
        )

        val error = assertFailsWith<SqlValidationException> {
            SqlSyntaxValidator.validateOrThrow(invalid)
        }

        assertEquals(listOf("insert.values.arity"), error.diagnostics.map { it.code })
    }

    private fun table(name: String): SqlTable.Ident =
        SqlTable.Ident(name)

    private fun num(value: String): SqlExpr.NumberLiteral =
        SqlExpr.NumberLiteral(value)

    private fun str(value: String): SqlExpr.StringLiteral =
        SqlExpr.StringLiteral(value)

    private fun bool(value: Boolean): SqlExpr.BooleanLiteral =
        SqlExpr.BooleanLiteral(value)

    private fun col(column: String): SqlExpr.Column =
        SqlExpr.Column(columnName = column)

    private fun SqlExpr.eq(other: SqlExpr): SqlExpr.Binary =
        SqlExpr.Binary(this, com.kotlinorm.syntax.expr.SqlBinaryOperator.Equal, other)
}
