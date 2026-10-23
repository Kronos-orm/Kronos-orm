/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.expr

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.render.SqlDialect
import com.kotlinorm.syntax.render.toSql
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlBuiltinFunctionLookupTest {
    @Test
    fun portableAverageAliasResolvesCaseInsensitivelyAndRendersSqlAverage() {
        for (alias in listOf("avgDouble", "AVGDOUBLE", "AvgDouble")) {
            val function = requireNotNull(SqlBuiltinFunction.fromDslName(alias))
            assertEquals(SqlBuiltinFunction.Average, function)
            val expression = SqlExpr.Function(
                name = SqlIdentifier.of(function.standardSqlName),
                args = listOf(SqlExpr.NumberLiteral("1")),
                builtinFunction = function
            )
            assertEquals("AVG(1)", expression.toSql(SqlDialect.Standard))
            assertEquals("AVG(1)", expression.toSql(SqlDialect.SQLite))
        }
    }

    @Test
    fun ordinaryNamesRetainTheirBuiltinAndUnknownNamesRemainCustom() {
        assertEquals(SqlBuiltinFunction.Average, SqlBuiltinFunction.fromDslName("avg"))
        assertEquals(SqlBuiltinFunction.Sum, SqlBuiltinFunction.fromDslName("SUM"))
        assertNull(SqlBuiltinFunction.fromDslName("customAverage"))
        assertNull(SqlBuiltinFunction.fromDslName("avgDouble "))
    }
}
