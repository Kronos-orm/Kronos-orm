/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.beans.dsl

import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.testfixtures.entities.TestUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ProjectionNamesTest {
    @Test
    fun `keeps unique requested names`() {
        assertEquals(
            listOf("id", "name", "createdAt"),
            allocateProjectionNames(listOf("id", "name", "createdAt"))
        )
    }

    @Test
    fun `suffixes every repeated name`() {
        assertEquals(
            listOf("id", "id_1", "id_2"),
            allocateProjectionNames(listOf("id", "id", "id"))
        )
    }

    @Test
    fun `reserves explicit names before allocating suffixes`() {
        assertEquals(
            listOf("id", "id_2", "id_1"),
            allocateProjectionNames(listOf("id", "id", "id_1"))
        )
    }

    @Test
    fun `skips already used suffixes and keeps blank names`() {
        assertEquals(
            listOf(null, "", "id_1", "id", "id_2"),
            allocateProjectionNames(listOf(null, "", "id_1", "id", "id"))
        )
    }

    @Test
    fun `assigns unique names to field projection items`() {
        val items = listOf(
            KTableForSelect.ProjectionItem.FieldItem(Field(columnName = "id", name = "")),
            KTableForSelect.ProjectionItem.FieldItem(Field(columnName = "reserved", name = "id_1")),
            KTableForSelect.ProjectionItem.FieldItem(Field(columnName = "legacy_id", name = "id")),
        )

        val resolved = items.withUniqueOutputNames()

        assertEquals(
            listOf("id", "id_1", "id_2"),
            resolved.map { (it as KTableForSelect.ProjectionItem.FieldItem).outputName }
        )
    }

    @Test
    fun `preserves or rewrites SQL items according to their resolved output name`() {
        val expression = SqlExpr.Column(columnName = "score")
        val metadata = SqlSelectItemAliasMetadata(
            outputName = "score",
            expression = expression,
            scope = SqlSelectItemSourceScope.Selected
        )
        val first = SqlSelectItem.Expr(expression, alias = "score", metadata = metadata)
        val duplicate = SqlSelectItem.Expr(expression, alias = "score", metadata = metadata)
        val aliasOnly = SqlSelectItem.Expr(expression, alias = "aliasOnly")
        val unnamed = SqlSelectItem.Expr(expression)
        val asterisk = SqlSelectItem.Asterisk()
        val resolved = listOf(
            KTableForSelect.ProjectionItem.SelectItemValue(first),
            KTableForSelect.ProjectionItem.SelectItemValue(duplicate),
            KTableForSelect.ProjectionItem.SelectItemValue(aliasOnly),
            KTableForSelect.ProjectionItem.SelectItemValue(unnamed),
            KTableForSelect.ProjectionItem.SelectItemValue(asterisk),
        ).withUniqueOutputNames().map { (it as KTableForSelect.ProjectionItem.SelectItemValue).item }

        assertSame(first, resolved[0])
        assertEquals(
            SqlSelectItem.Expr(expression, alias = "score_1", metadata = metadata.copy(outputName = "score_1")),
            resolved[1]
        )
        assertEquals(aliasOnly, resolved[2])
        assertSame(unnamed, resolved[3])
        assertSame(asterisk, resolved[4])
    }

    @Test
    fun `aligns a stale SQL alias with its authoritative metadata output name`() {
        val expression = SqlExpr.Column(columnName = "score")
        val metadata = SqlSelectItemAliasMetadata(
            outputName = "score",
            expression = expression,
            scope = SqlSelectItemSourceScope.Selected
        )
        val stale = SqlSelectItem.Expr(expression, alias = "legacy_score", metadata = metadata)

        val resolved = listOf(
            KTableForSelect.ProjectionItem.SelectItemValue(stale)
        ).withUniqueOutputNames().single() as KTableForSelect.ProjectionItem.SelectItemValue

        assertEquals(
            SqlSelectItem.Expr(expression, alias = "score", metadata = metadata),
            resolved.item
        )
    }

    @Test
    fun `renames scalar subquery aliases together with their SQL item`() {
        val query = TestUser().select { it.id }
        val expression = SqlExpr.Subquery(query.toSqlQuery())
        val scalarItem = SqlSelectItem.Expr(expression, alias = "id")
        val resolved = listOf(
            KTableForSelect.ProjectionItem.FieldItem(Field(columnName = "id", name = "id")),
            KTableForSelect.ProjectionItem.ScalarSubqueryValue(query, "id", scalarItem),
        ).withUniqueOutputNames()

        val scalar = resolved[1] as KTableForSelect.ProjectionItem.ScalarSubqueryValue
        assertEquals("id_1", scalar.alias)
        assertEquals(SqlSelectItem.Expr(expression, alias = "id_1"), scalar.item)
    }

    @Test
    fun `aligns scalar SQL item aliases even when the scalar alias is already unique`() {
        val query = TestUser().select { it.id }
        val expression = SqlExpr.Subquery(query.toSqlQuery())
        val metadata = SqlSelectItemAliasMetadata(
            outputName = "latestId",
            expression = expression,
            scope = SqlSelectItemSourceScope.Selected
        )
        val scalar = KTableForSelect.ProjectionItem.ScalarSubqueryValue(
            query = query,
            alias = "latestId",
            item = SqlSelectItem.Expr(expression, alias = "legacyLatestId", metadata = metadata)
        )

        val resolved = listOf(scalar).withUniqueOutputNames().single() as
            KTableForSelect.ProjectionItem.ScalarSubqueryValue

        assertEquals("latestId", resolved.alias)
        assertEquals(
            SqlSelectItem.Expr(expression, alias = "latestId", metadata = metadata),
            resolved.item
        )
    }
}
