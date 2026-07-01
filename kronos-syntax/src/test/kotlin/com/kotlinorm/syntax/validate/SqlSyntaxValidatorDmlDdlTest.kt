/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.validate

import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlIndexDefinition
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlPrimaryKeyMode
import com.kotlinorm.syntax.statement.SqlUpdateSetPair
import com.kotlinorm.syntax.table.SqlTable
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SqlSyntaxValidatorDmlDdlTest {
    @Test
    fun reportsInsertUpdateAndUpsertDiagnostics() {
        val insert = SqlDmlStatement.Insert(
            table = table("user"),
            columns = listOf("id", "id"),
            mode = SqlInsertMode.Values(listOf(listOf(num("1"))))
        )
        val update = SqlDmlStatement.Update(
            table = table("user"),
            setPairs = listOf(SqlUpdateSetPair("name", str("Ada")), SqlUpdateSetPair("name", str("Grace")))
        )
        val upsert = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = listOf("id", "name"),
            values = listOf(num("1")),
            primaryKeys = listOf("id", "missing"),
            updateColumns = emptyList()
        )

        val codes = (SqlSyntaxValidator.validate(insert) +
            SqlSyntaxValidator.validate(update) +
            SqlSyntaxValidator.validate(upsert)).map { it.code }.toSet()

        assertTrue("insert.columns.duplicate" in codes)
        assertTrue("insert.values.arity" in codes)
        assertTrue("update.set.duplicate" in codes)
        assertTrue("upsert.values.arity" in codes)
        assertTrue("upsert.update.empty" in codes)
        assertTrue("upsert.column.unknown" in codes)
    }

    @Test
    fun reportsCreateTableDiagnostics() {
        val create = SqlDdlStatement.CreateTable(
            tableName = "user",
            columns = listOf(
                SqlColumnDefinition("id", SqlType.Int, primaryKey = SqlPrimaryKeyMode.Primary),
                SqlColumnDefinition("id", SqlType.Long, primaryKey = SqlPrimaryKeyMode.Identity)
            ),
            indexes = listOf(SqlIndexDefinition("idx_missing", listOf("missing")))
        )

        val codes = SqlSyntaxValidator.validate(create).map { it.code }.toSet()

        assertTrue("ddl.column.duplicate" in codes)
        assertTrue("ddl.primary.multiple" in codes)
        assertTrue("ddl.index.column.unknown" in codes)
    }

    @Test
    fun throwsWhenValidationFindsErrors() {
        val invalid = SqlDmlStatement.Insert(
            table = table("user"),
            columns = listOf("id", "name"),
            mode = SqlInsertMode.Values(listOf(listOf(num("1"))))
        )

        val error = assertFailsWith<SqlValidationException> {
            SqlSyntaxValidator.validateOrThrow(invalid)
        }

        assertTrue(error.diagnostics.any { it.code == "insert.values.arity" })
    }

    private fun table(name: String): SqlTable.Ident =
        SqlTable.Ident(name)

    private fun num(value: String): SqlExpr.NumberLiteral =
        SqlExpr.NumberLiteral(value)

    private fun str(value: String): SqlExpr.StringLiteral =
        SqlExpr.StringLiteral(value)
}
