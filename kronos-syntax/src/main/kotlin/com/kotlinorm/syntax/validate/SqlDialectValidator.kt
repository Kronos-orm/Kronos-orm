/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.validate

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.render.SqlDialect
import com.kotlinorm.syntax.render.SqlDialectFamily
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlQuery

interface SqlDialectValidator {
    fun validate(node: SqlNode, dialect: SqlDialect): List<SqlValidationDiagnostic>

    fun validateOrThrow(node: SqlNode, dialect: SqlDialect) {
        val errors = validate(node, dialect).filter { it.severity == SqlValidationSeverity.Error }
        if (errors.isNotEmpty()) throw SqlValidationException(errors)
    }

    companion object {
        val None: SqlDialectValidator = object : SqlDialectValidator {
            override fun validate(node: SqlNode, dialect: SqlDialect): List<SqlValidationDiagnostic> = emptyList()
        }

        val Default: SqlDialectValidator = DefaultSqlDialectValidator

        fun composite(vararg validators: SqlDialectValidator): SqlDialectValidator =
            CompositeSqlDialectValidator(validators.toList())
    }
}

class CompositeSqlDialectValidator(
    private val validators: List<SqlDialectValidator>
) : SqlDialectValidator {
    override fun validate(node: SqlNode, dialect: SqlDialect): List<SqlValidationDiagnostic> =
        validators.flatMap { it.validate(node, dialect) }
}

object DefaultSqlDialectValidator : SqlDialectValidator {
    override fun validate(node: SqlNode, dialect: SqlDialect): List<SqlValidationDiagnostic> = buildList {
        when (node) {
            is SqlQuery -> validateQuery(node, dialect)
            is SqlDmlStatement -> validateDml(node, dialect)
        }
    }

    private fun MutableList<SqlValidationDiagnostic>.validateQuery(query: SqlQuery, dialect: SqlDialect) {
        if (dialect.family == SqlDialectFamily.SQLite && query.lock != null) {
            error("dialect.lock.unsupported", "SQLite does not support row-level SELECT locks.")
        }
        if (query is SqlQuery.Select && dialect.family == SqlDialectFamily.SqlServer && query.limit != null && query.orderBy.isEmpty()) {
            error("dialect.sqlserver.limit.requires.order", "SQL Server OFFSET/FETCH pagination requires ORDER BY.")
        }
    }

    private fun MutableList<SqlValidationDiagnostic>.validateDml(statement: SqlDmlStatement, dialect: SqlDialect) {
        val hasReturning = when (statement) {
            is SqlDmlStatement.Delete -> statement.returning != null
            is SqlDmlStatement.Insert -> statement.returning != null
            is SqlDmlStatement.Update -> statement.returning != null
            is SqlDmlStatement.Upsert -> statement.returning != null
            is SqlDmlStatement.Truncate -> false
        }
        if (hasReturning && dialect.family in setOf(SqlDialectFamily.MySql, SqlDialectFamily.SqlServer)) {
            error("dialect.returning.unsupported", "${dialect.family} does not support RETURNING in this syntax layer.")
        }
    }

    private fun MutableList<SqlValidationDiagnostic>.error(code: String, message: String) {
        add(SqlValidationDiagnostic(code, message, SqlValidationSeverity.Error))
    }
}
