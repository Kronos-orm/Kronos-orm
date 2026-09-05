/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlMatchPhase
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.table.SqlMatchRecognize
import com.kotlinorm.syntax.table.SqlRecognizeMeasureItem
import com.kotlinorm.syntax.table.SqlRecognizePatternEmptyMatchMode
import com.kotlinorm.syntax.table.SqlRecognizePatternRowsMode
import com.kotlinorm.syntax.table.SqlRowPattern
import com.kotlinorm.syntax.table.SqlRowPatternDefineItem
import com.kotlinorm.syntax.table.SqlRowPatternQuantifier
import com.kotlinorm.syntax.table.SqlRowPatternSkipMode
import com.kotlinorm.syntax.table.SqlRowPatternStrategy
import com.kotlinorm.syntax.table.SqlRowPatternSubsetItem
import com.kotlinorm.syntax.table.SqlRowPatternTerm
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchRecognizeRendererTest {
    @Test
    fun rendersMatchRecognizeClauseOnTableSources() {
        val matchRecognize = SqlMatchRecognize(
            partitionBy = listOf(col("s", "symbol")),
            orderBy = listOf(SqlOrderingItem(col("s", "ts"))),
            measures = listOf(SqlRecognizeMeasureItem(SqlExpr.MatchPhase(SqlMatchPhase.Final, col("price")), "final_price")),
            rowsMode = SqlRecognizePatternRowsMode.AllRows(SqlRecognizePatternEmptyMatchMode.WithUnmatchedRows),
            rowPattern = SqlRowPattern(
                afterMatchMode = SqlRowPatternSkipMode.ToLast("B"),
                strategy = SqlRowPatternStrategy.Seek,
                pattern = SqlRowPatternTerm.Then(
                    left = SqlRowPatternTerm.Pattern("A"),
                    right = SqlRowPatternTerm.Pattern("B", SqlRowPatternQuantifier.Plus(withQuestion = true))
                ),
                subset = listOf(SqlRowPatternSubsetItem("X", listOf("A", "B"))),
                define = listOf(
                    SqlRowPatternDefineItem("A", col("price").gt(num("100"))),
                    SqlRowPatternDefineItem("B", col("price").lt(col("A", "price")))
                )
            ),
            alias = SqlTableAlias("mr")
        )
        val table = SqlTable.Ident(name = "sales", alias = SqlTableAlias("s"), matchRecognize = matchRecognize)

        assertEquals(
            "sales".let {
                "\"$it\" AS \"s\" MATCH_RECOGNIZE(PARTITION BY \"s\".\"symbol\" ORDER BY \"s\".\"ts\" ASC MEASURES FINAL \"price\" AS \"final_price\" ALL ROWS PER MATCH WITH UNMATCHED ROWS AFTER MATCH SKIP TO LAST \"B\" SEEK PATTERN (\"A\" \"B\"+?) SUBSET \"X\" = (\"A\", \"B\") DEFINE \"A\" AS \"price\" > 100, \"B\" AS \"price\" < \"A\".\"price\") AS \"mr\""
            },
            table.toSql()
        )
    }

    @Test
    fun rendersRowPatternAnchorsPermuteExclusionAndReluctantRanges() {
        val matchRecognize = SqlMatchRecognize(
            rowsMode = SqlRecognizePatternRowsMode.OneRow,
            rowPattern = SqlRowPattern(
                pattern = SqlRowPatternTerm.Then(
                    left = SqlRowPatternTerm.Circumflex(),
                    right = SqlRowPatternTerm.Then(
                        left = SqlRowPatternTerm.Permute(
                            terms = listOf(
                                SqlRowPatternTerm.Pattern("A", SqlRowPatternQuantifier.Between(num("1"), num("3"), withQuestion = true)),
                                SqlRowPatternTerm.Exclusion(SqlRowPatternTerm.Pattern("B"))
                            )
                        ),
                        right = SqlRowPatternTerm.Dollar()
                    )
                ),
                define = listOf(SqlRowPatternDefineItem("A", col("amount").gt(num("0"))))
            )
        )

        assertEquals(
            "MATCH_RECOGNIZE(ONE ROW PER MATCH PATTERN (^ PERMUTE(\"A\"{1,3}?, {-\"B\"-}) \$) DEFINE \"A\" AS \"amount\" > 0)",
            renderTable(SqlTable.Ident("t", matchRecognize = matchRecognize)).substringAfter("\"t\" ")
        )
    }

    private fun renderTable(table: SqlTable): String =
        StandardSqlRenderer().renderTable(table)
}
