/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.inspect

import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlParameterCollectorCoverageTest {
    @Test
    fun tracksOnlyExpandedNamedParameterOccurrences() {
        val firstNamed = SqlExpr.Parameter(SqlParameter.Named("first"))
        val positional = SqlExpr.Parameter(SqlParameter.Positional(0), expandAsList = true)
        val firstExpanded = SqlExpr.Parameter(SqlParameter.Named("ids"), expandAsList = true)
        val secondNamed = SqlExpr.Parameter(SqlParameter.Named("status"))
        val secondExpanded = SqlExpr.Parameter(SqlParameter.Named("roles"), expandAsList = true)
        val query = SqlQuery.Values(
            values = listOf(listOf(firstNamed, positional, firstExpanded, secondNamed, secondExpanded))
        )

        assertEquals(
            listOf(
                SqlParameter.Named("first"),
                SqlParameter.Positional(0),
                SqlParameter.Named("ids"),
                SqlParameter.Named("status"),
                SqlParameter.Named("roles")
            ),
            SqlParameterCollector.collectParameters(query)
        )
        assertEquals(listOf("first", "ids", "status", "roles"), SqlParameterCollector.collectNamedParameters(query))
        assertEquals(setOf(1, 3), SqlParameterCollector.collectListExpansionOccurrences(query))
        assertEquals(
            listOf("ids"),
            SqlParameterCollector.collectNamedParameters(SqlSelectItem.Expr(firstExpanded))
        )
    }
}
