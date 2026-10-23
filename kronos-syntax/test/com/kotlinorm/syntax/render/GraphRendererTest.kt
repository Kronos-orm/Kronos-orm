/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlGraphExportMode
import com.kotlinorm.syntax.table.SqlGraphLabel
import com.kotlinorm.syntax.table.SqlGraphMatchMode
import com.kotlinorm.syntax.table.SqlGraphPattern
import com.kotlinorm.syntax.table.SqlGraphPatternTerm
import com.kotlinorm.syntax.table.SqlGraphRepeatableMode
import com.kotlinorm.syntax.table.SqlGraphRowsMode
import com.kotlinorm.syntax.table.SqlGraphSymbol
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphRendererTest {
    @Test
    fun rendersGraphTableWithMatchRowsColumnsAndExportMode() {
        val patternTerm = SqlGraphPatternTerm.And(
            SqlGraphPatternTerm.And(
                SqlGraphPatternTerm.Vertex("a", SqlGraphLabel.Label("Person")),
                SqlGraphPatternTerm.Edge(
                    leftSymbol = SqlGraphSymbol.Dash,
                    name = "e",
                    label = SqlGraphLabel.Label("KNOWS"),
                    where = col("e", "since").gt(num("2020")),
                    rightSymbol = SqlGraphSymbol.RightArrow
                )
            ),
            SqlGraphPatternTerm.Vertex("b", SqlGraphLabel.Label("Person"))
        )
        val table = SqlTable.Graph(
            withLateral = true,
            name = "social",
            matchMode = SqlGraphMatchMode.Repeatable(SqlGraphRepeatableMode.Element),
            patterns = listOf(SqlGraphPattern("p", patternTerm)),
            where = col("a", "active").eq(bool(true)),
            rowsMode = SqlGraphRowsMode.Step("a", "e", "b", inPaths = listOf("p")),
            columns = listOf(
                SqlSelectItem.Expr(col("a", "id"), alias = "from_id"),
                SqlSelectItem.Expr(col("b", "id"), alias = "to_id")
            ),
            exportMode = SqlGraphExportMode.NoSingletons,
            alias = SqlTableAlias("gt")
        )

        assertEquals(
            "LATERAL GRAPH_TABLE(\"social\" MATCH REPEATABLE ELEMENT \"p\" = (\"a\" IS \"Person\") -[\"e\" IS \"KNOWS\" WHERE \"e\".\"since\" > 2020]-> (\"b\" IS \"Person\") WHERE \"a\".\"active\" = TRUE ONE ROW PER STEP (\"a\", \"e\", \"b\") IN (\"p\") COLUMNS (\"a\".\"id\" AS from_id, \"b\".\"id\" AS to_id) EXPORT NO SINGLETONS) AS \"gt\"",
            table.toSql()
        )
    }

    @Test
    fun rendersGraphAlternationLabelsQuantifiersAndSingletonExports() {
        val personOrCompany = SqlGraphLabel.Or(SqlGraphLabel.Label("Person"), SqlGraphLabel.Label("Company"))
        val quantified = SqlGraphPatternTerm.Quantified(
            SqlGraphPatternTerm.Or(
                SqlGraphPatternTerm.Vertex("src", SqlGraphLabel.And(personOrCompany, SqlGraphLabel.Not(SqlGraphLabel.Percent))),
                SqlGraphPatternTerm.Vertex("dst", SqlGraphLabel.Label("Account"))
            ),
            com.kotlinorm.syntax.table.SqlGraphQuantifier.Plus
        )
        val table = SqlTable.Graph(
            name = "network",
            patterns = listOf(SqlGraphPattern(term = quantified)),
            rowsMode = SqlGraphRowsMode.Vertex("src"),
            columns = listOf(SqlSelectItem.Expr(col("src", "id"), alias = "id")),
            exportMode = SqlGraphExportMode.Singletons(listOf("src"))
        )

        assertEquals(
            "GRAPH_TABLE(\"network\" MATCH ((\"src\" IS (\"Person\" | \"Company\") & !(%)) | (\"dst\" IS \"Account\"))+ ONE ROW PER VERTEX (\"src\") COLUMNS (\"src\".\"id\" AS id) EXPORT SINGLETONS (\"src\"))",
            table.toSql()
        )
    }
}
