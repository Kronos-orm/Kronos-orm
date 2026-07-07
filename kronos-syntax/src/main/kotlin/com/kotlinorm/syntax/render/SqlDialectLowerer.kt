/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlTable

interface SqlDialectLowerer {
    fun lowerStatement(statement: SqlStatement, dialect: SqlDialect): SqlStatement = statement

    fun lowerQuery(query: SqlQuery, dialect: SqlDialect): SqlQuery =
        lowerStatement(query, dialect) as? SqlQuery ?: query

    fun lowerExpr(expr: SqlExpr, dialect: SqlDialect): SqlExpr = expr

    fun lowerTable(table: SqlTable, dialect: SqlDialect): SqlTable = table

    companion object {
        val None: SqlDialectLowerer = object : SqlDialectLowerer {}

        fun composite(vararg lowerers: SqlDialectLowerer): SqlDialectLowerer =
            CompositeSqlDialectLowerer(lowerers.toList())
    }
}

class CompositeSqlDialectLowerer(
    private val lowerers: List<SqlDialectLowerer>
) : SqlDialectLowerer {
    override fun lowerStatement(statement: SqlStatement, dialect: SqlDialect): SqlStatement =
        lowerers.fold(statement) { current, lowerer -> lowerer.lowerStatement(current, dialect) }

    override fun lowerQuery(query: SqlQuery, dialect: SqlDialect): SqlQuery =
        lowerers.fold(query) { current, lowerer -> lowerer.lowerQuery(current, dialect) }

    override fun lowerExpr(expr: SqlExpr, dialect: SqlDialect): SqlExpr =
        lowerers.fold(expr) { current, lowerer -> lowerer.lowerExpr(current, dialect) }

    override fun lowerTable(table: SqlTable, dialect: SqlDialect): SqlTable =
        lowerers.fold(table) { current, lowerer -> lowerer.lowerTable(current, dialect) }
}
