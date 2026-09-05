/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.validate.SqlValidationDiagnostic

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalKronosSyntaxApi

@ExperimentalKronosSyntaxApi
data class SqlPlan(
    val statements: List<SqlStatement>,
    val diagnostics: List<SqlValidationDiagnostic> = emptyList()
) {
    init {
        require(statements.isNotEmpty()) { "SQL plan requires at least one statement." }
    }

    companion object {
        fun single(statement: SqlStatement, diagnostics: List<SqlValidationDiagnostic> = emptyList()): SqlPlan =
            SqlPlan(listOf(statement), diagnostics)
    }
}

@ExperimentalKronosSyntaxApi
fun interface SqlDialectPlanner {
    fun plan(statement: SqlStatement, dialect: SqlDialect): SqlPlan

    companion object {
        val None: SqlDialectPlanner = SqlDialectPlanner { statement, _ -> SqlPlan.single(statement) }

        fun composite(vararg planners: SqlDialectPlanner): SqlDialectPlanner =
            CompositeSqlDialectPlanner(planners.toList())
    }
}

@ExperimentalKronosSyntaxApi
class CompositeSqlDialectPlanner(
    private val planners: List<SqlDialectPlanner>
) : SqlDialectPlanner {
    override fun plan(statement: SqlStatement, dialect: SqlDialect): SqlPlan =
        planners.fold(SqlPlan.single(statement)) { current, planner ->
            val plans = current.statements.map { planner.plan(it, dialect) }
            val plannedStatements = plans.flatMap { it.statements }
            val plannedDiagnostics = plans.flatMap { it.diagnostics }
            SqlPlan(plannedStatements, current.diagnostics + plannedDiagnostics)
        }
}
