/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlEncoding
import com.kotlinorm.syntax.expr.SqlJsonExistsErrorBehavior
import com.kotlinorm.syntax.expr.SqlJsonOutputFormat
import com.kotlinorm.syntax.expr.SqlJsonPassing
import com.kotlinorm.syntax.expr.SqlJsonQueryEmptyBehavior
import com.kotlinorm.syntax.expr.SqlJsonQueryErrorBehavior
import com.kotlinorm.syntax.expr.SqlJsonQueryQuotesBehavior
import com.kotlinorm.syntax.expr.SqlJsonQueryQuotesBehaviorMode
import com.kotlinorm.syntax.expr.SqlJsonQueryWrapperBehavior
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.table.SqlJsonColumn
import com.kotlinorm.syntax.table.SqlJsonErrorBehavior
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonTableRendererTest {
    @Test
    fun rendersJsonTableWithOrdinalityColumnsExistsAndNestedColumns() {
        val jsonTable = SqlTable.Json(
            withLateral = true,
            expr = col("doc"),
            path = str("$.items[*]"),
            pathAlias = "items",
            passingItems = listOf(SqlJsonPassing(named("tenant"), "tenant")),
            columns = listOf(
                SqlJsonColumn.Ordinality("ord"),
                SqlJsonColumn.Column(
                    name = "sku",
                    type = SqlType.Varchar(32),
                    format = SqlJsonOutputFormat(SqlEncoding.Utf8),
                    path = str("$.sku"),
                    wrapper = SqlJsonQueryWrapperBehavior.Without(withArray = false),
                    quotes = SqlJsonQueryQuotesBehavior(SqlJsonQueryQuotesBehaviorMode.Omit),
                    onEmpty = SqlJsonQueryEmptyBehavior.Default(str("missing")),
                    onError = SqlJsonQueryErrorBehavior.Null
                ),
                SqlJsonColumn.Exists(
                    name = "has_discount",
                    type = SqlType.Boolean,
                    path = str("$.discount"),
                    onError = SqlJsonExistsErrorBehavior.False
                ),
                SqlJsonColumn.Nested(
                    path = str("$.prices[*]"),
                    pathAlias = "p",
                    columns = listOf(
                        SqlJsonColumn.Column(
                            name = "amount",
                            type = SqlType.Decimal(10 to 2),
                            path = str("$.amount")
                        )
                    )
                )
            ),
            onError = SqlJsonErrorBehavior.EmptyArray,
            alias = SqlTableAlias("jt")
        )

        assertEquals(
            "LATERAL JSON_TABLE(\"doc\", '$.items[*]' AS \"items\" PASSING :tenant AS \"tenant\" COLUMNS (\"ord\" FOR ORDINALITY, \"sku\" VARCHAR(32) FORMAT JSON ENCODING UTF8 PATH '$.sku' WITHOUT WRAPPER OMIT QUOTES DEFAULT 'missing' ON EMPTY NULL ON ERROR, \"has_discount\" BOOLEAN EXISTS PATH '$.discount' FALSE ON ERROR, NESTED PATH '$.prices[*]' AS \"p\" COLUMNS (\"amount\" DECIMAL(10, 2) PATH '$.amount')) EMPTY ARRAY ON ERROR) AS \"jt\"",
            jsonTable.toSql()
        )
    }
}
