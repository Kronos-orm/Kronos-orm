/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.parser.NoneDataSourceWrapper
import com.kotlinorm.beans.subquery.SubqueryOrder
import com.kotlinorm.beans.subquery.SubqueryUser
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.where
import com.kotlinorm.orm.pagination.toCursor
import com.kotlinorm.orm.pagination.CursorUnstableUser
import com.kotlinorm.orm.pagination.CursorSpec
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.expr.SqlQuantifiedComparisonOperator
import com.kotlinorm.syntax.expr.SqlSubqueryQuantifier
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlSelectItemSource
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SampleSqlServerJdbcWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SelectFromPlannerCoverageTest : MysqlTestBase() {
    @Test
    fun `join and where parameters with the same name remain distinct`() {
        val task = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 && relation.id == 3 }
                .select { user.id }
                .where { user.id == 4 }
        }.build().atomicTask

        assertEquals(setOf(3, 4), task.paramMap.values.toSet())
        assertEquals(2, task.paramMap.keys.size)
    }

    @Test
    fun `planner keeps only referenced parameters in SQL syntax traversal order`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 && relation.id == 3 }
                .select { user.id }
                .where { user.id == 4 }
                .having { user.username == "neo" }
        }
        query.context.projectionItems = query.context.projectionItems +
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(parameter("projection_value"), alias = "projectionValue")
            )
        query.context.orderByItems = listOf(
            KTableForSort.SortItem.ExpressionItem(parameter("order_value"), SqlOrdering.Asc)
        )
        query.context.parameterValues.putAll(
            linkedMapOf(
                "projection_value" to 1,
                "order_value" to 5,
                "unused_value" to 6
            )
        )

        val plan = query.toSqlQueryPlan()

        assertEquals(
            linkedMapOf(
                "projection_value" to 1,
                "id@1" to 3,
                "id" to 4,
                "username" to "neo",
                "order_value" to 5
            ),
            plan.parameters
        )
    }

    @Test
    fun `qualifier rewriting updates value operands and preserves nested query scopes`() {
        val source = SqlExpr.Column(tableName = "source_table", columnName = "id")
        val nested = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(source)),
            from = listOf(SqlTable.Ident("source_table")),
            where = SqlExpr.Binary(source, SqlBinaryOperator.Equal, parameter("nested"))
        )
        val replacement = SqlExpr.Column(
            tableName = "source_alias",
            columnName = "id",
            qualifier = SqlIdentifier.of("source_alias")
        )

        assertEquals(
            listOf(
                SqlExpr.In(
                    replacement,
                    SqlInRightOperand.Values(listOf(replacement))
                ),
                SqlExpr.In(
                    replacement,
                    SqlInRightOperand.Subquery(nested)
                ),
                SqlExpr.QuantifiedComparisonPredicate(
                    expr = replacement,
                    operator = SqlQuantifiedComparisonOperator.GreaterThan,
                    quantifier = SqlSubqueryQuantifier.Any,
                    query = nested
                ),
                SqlExpr.Subquery(nested),
                SqlExpr.ExistsPredicate(nested)
            ),
            listOf(
                SqlExpr.In(source, SqlInRightOperand.Values(listOf(source))),
                SqlExpr.In(source, SqlInRightOperand.Subquery(nested)),
                SqlExpr.QuantifiedComparisonPredicate(
                    expr = source,
                    operator = SqlQuantifiedComparisonOperator.GreaterThan,
                    quantifier = SqlSubqueryQuantifier.Any,
                    query = nested
                ),
                SqlExpr.Subquery(nested),
                SqlExpr.ExistsPredicate(nested)
            ).map { it.rewriteJoinQualifiers(mapOf("source_table" to "source_alias")) }
        )
    }

    @Test
    fun `derived source rewriting updates known qualifiers without guessing unknown columns`() {
        val orders = SubqueryOrder().select { [it.userId, it.status] }.where { it.status == 40 }
        val query = SubqueryUser().join(orders) { user, order ->
            leftJoin { user.id == order.userId }.select { user.id }
        }
        val derivedPojo = query.state.leaves.last().pojo
        val capturedQualifier = query.state.qualifierFor(derivedPojo)
        val sourceColumn = SqlExpr.Column(tableName = capturedQualifier, columnName = "status")
        val selectedColumn = SqlExpr.Column(columnName = "computed")
        query.context.projectionItems = listOf(
            KTableForSelect.ProjectionItem.SelectItemValue(SqlSelectItem.Asterisk(capturedQualifier)),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(
                    expr = sourceColumn,
                    alias = "known",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "known",
                        expression = sourceColumn,
                        scope = SqlSelectItemSourceScope.Source,
                        source = SqlSelectItemSource(tableName = capturedQualifier, columnName = "status")
                    )
                )
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(SqlExpr.Column(columnName = "unqualified"), alias = "unqualified")
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(
                    expr = selectedColumn,
                    alias = "computed",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "computed",
                        expression = selectedColumn,
                        scope = SqlSelectItemSourceScope.Selected,
                        source = SqlSelectItemSource(columnName = "computed")
                    )
                )
            )
        )

        val select = (query.toSqlQuery() as SqlQuery.Select).select
        val qualified = SqlExpr.Column("q", "status", SqlIdentifier.of("q"))

        assertEquals(
            listOf(
                SqlSelectItem.Asterisk("q", SqlIdentifier.of("q")),
                SqlSelectItem.Expr(
                    expr = qualified,
                    alias = "known",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "known",
                        expression = qualified,
                        scope = SqlSelectItemSourceScope.Source,
                        source = SqlSelectItemSource("q", "status", SqlIdentifier.of("q"))
                    )
                ),
                SqlSelectItem.Expr(
                    SqlExpr.Column(columnName = "unqualified"),
                    alias = "unqualified"
                ),
                SqlSelectItem.Expr(
                    expr = selectedColumn,
                    alias = "computed",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "computed",
                        expression = selectedColumn,
                        scope = SqlSelectItemSourceScope.Selected,
                        source = SqlSelectItemSource(columnName = "computed")
                    )
                )
            ),
            select
        )
    }

    @Test
    fun `unqualified projection is qualified only when its field belongs to the derived source`() {
        val orders = SubqueryOrder().select { [it.userId, it.status.alias("rankStatus")] }
        val query = SubqueryUser().join(orders) { user, order ->
            leftJoin { user.id == order.userId }.select { user.id }
        }
        query.context.projectionItems = listOf(
            KTableForSelect.ProjectionItem.FieldItem(
                field = Field(columnName = "rankStatus", name = "rankStatus"),
                expr = SqlExpr.Column(columnName = "rankStatus"),
                outputName = "rankStatus"
            ),
            KTableForSelect.ProjectionItem.FieldItem(
                field = Field(columnName = "name", name = "name"),
                expr = SqlExpr.Column(columnName = "name"),
                outputName = "name"
            )
        )

        val selectItems = (query.toSqlQuery() as SqlQuery.Select).select.map { assertIs<SqlSelectItem.Expr>(it) }

        assertEquals(
            listOf(
                SqlExpr.Column(tableName = "q", columnName = "rankStatus", qualifier = SqlIdentifier.of("q")),
                SqlExpr.Column(columnName = "name")
            ),
            selectItems.map(SqlSelectItem.Expr::expr)
        )
    }

    @Test
    fun `self join cursor uses distinct hidden labels token keys and after parameters`() {
        val left = JoinIdentityRow()
        val right = JoinIdentityRow()
        val query = left.join(right) { first, second ->
            innerJoin { first.parentId == second.id }
                .select { first.parentId }
                .orderBy { [first.id.asc(), second.id.asc()] }
        }

        val firstPage = query.cursor(pageSize = 1).build().atomicTask
        assertEquals(
            "SELECT `join_identity_row__k1`.`parent_id` AS `parentId`, " +
                "`join_identity_row__k1`.`id` AS `__kronos_cursor_id`, " +
                "`join_identity_row__k2`.`id` AS `__kronos_cursor_id_1` " +
                "FROM `join_identity_row` AS `join_identity_row__k1` " +
                "INNER JOIN `join_identity_row` AS `join_identity_row__k2` " +
                "ON `join_identity_row__k1`.`parent_id` = `join_identity_row__k2`.`id` " +
                "ORDER BY `join_identity_row__k1`.`id` ASC, `join_identity_row__k2`.`id` ASC LIMIT 2",
            firstPage.sql
        )

        val cursor = mapOf<String, Any?>(
            "join_identity_row__k1.id" to 10,
            "join_identity_row__k2.id" to 20
        ).toCursor()
        val nextPage = query.cursor(pageSize = 1, after = cursor).build().atomicTask

        assertEquals(
            linkedMapOf(
                "cursor_join_identity_row__k1_id" to 10,
                "cursor_join_identity_row__k1_id@1" to 10,
                "cursor_join_identity_row__k2_id" to 20
            ),
            nextPage.paramMap
        )
    }

    @Test
    fun `cursor binds generated context order field without duplicating root stable key`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { [user.id, relation.gender] }
        }
        query.context.orderByItems = listOf(
            KTableForSort.SortItem.FieldItem(
                field = Field(columnName = "id", name = "id"),
                ordering = SqlOrdering.Asc,
                expr = SqlExpr.Column(columnName = "id")
            )
        )

        val firstPage = query.cursor(pageSize = 1).build().atomicTask
        val statement = assertIs<SqlQuery.Select>(firstPage.statement)

        assertEquals(
            "SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender`, " +
                "`user_relation`.`id` AS `__kronos_cursor_id` FROM `tb_user` " +
                "INNER JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` " +
                "WHERE `tb_user`.`deleted` = 0 ORDER BY `id` ASC, `user_relation`.`id` ASC LIMIT 2",
            firstPage.sql
        )
        assertEquals(
            listOf(
                SqlExpr.Column(columnName = "id"),
                SqlExpr.Column(
                    tableName = "user_relation",
                    columnName = "id",
                    qualifier = SqlIdentifier.of("user_relation")
                )
            ),
            statement.orderBy.map { it.expr }
        )

        val cursor = mapOf<String, Any?>(
            "tb_user.id" to 10,
            "user_relation.id" to 20
        ).toCursor()
        val nextPage = query.cursor(pageSize = 1, after = cursor).build().atomicTask

        assertEquals(
            linkedMapOf(
                "cursor_tb_user_id" to 10,
                "cursor_tb_user_id@1" to 10,
                "cursor_user_relation_id" to 20
            ),
            nextPage.paramMap
        )
        assertTrue(nextPage.sql.contains("`tb_user`.`id` > :cursor_tb_user_id"))
        assertTrue(nextPage.sql.contains("`user_relation`.`id` > :cursor_user_relation_id"))
    }

    @Test
    fun `right-side logic delete predicate remains in join on`() {
        val statement = TestUser().join(TestUser()) { left, right ->
            leftJoin { left.id == right.id }
                .select { left.username }
        }.toSqlQuery() as SqlQuery.Select

        val join = assertIs<SqlTable.Join>(statement.from.single())
        val on = assertIs<SqlJoinCondition.On>(join.condition)
        val expression = assertIs<SqlExpr.Binary>(on.condition)
        assertEquals(SqlBinaryOperator.And, expression.operator)
    }

    @Test
    fun `cross join remains conditionless while logic-delete filters stay in query predicate`() {
        val statement = TestUser().join(TestUser()) { _, _ ->
            crossJoin().select { it.username }
        }.toSqlQuery() as SqlQuery.Select

        val join = assertIs<SqlTable.Join>(statement.from.single())
        assertNull(join.condition)
        assertTrue(statement.where != null)
    }

    @Test
    fun `total count plan drops ordering and limit`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            leftJoin { user.id == relation.id2 }
                .select { user.id }
                .orderBy { user.id.desc() }
                .limit(5)
        }
        val statement = query.buildTotalCountTask().atomicTask.statement as SqlQuery.Select

        assertTrue(statement.orderBy.isEmpty())
        assertNull(statement.limit)
    }

    @Test
    fun `sql server offset page derives deterministic root primary-key ordering`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            leftJoin { user.id == relation.id2 }
                .select { user.username }
        }
        val statement = query.page(2, 5)
            .toSqlQuery(SampleSqlServerJdbcWrapper) as SqlQuery.Select

        assertFalse(statement.orderBy.isEmpty())
        assertEquals(5, statement.limit?.fetch?.limit?.let { (it as SqlExpr.NumberLiteral).number.toInt() })
    }

    @Test
    fun `offset page planning without a data source remains dialect neutral`() {
        val query = SourceIdentityCustomer().join(SourceIdentityInvoice()) { customer, invoice ->
            leftJoin { customer.id == invoice.customerId }
                .select { customer.id }
        }

        val statement = query.page(2, 5).toSqlQuery(NoneDataSourceWrapper) as SqlQuery.Select

        assertTrue(statement.orderBy.isEmpty())
        assertEquals(5, statement.limit?.fetch?.limit?.let { (it as SqlExpr.NumberLiteral).number.toInt() })
        assertEquals(5, statement.limit?.offset?.let { (it as SqlExpr.NumberLiteral).number.toInt() })
    }

    @Test
    fun `joined planner materializes scalar subquery projections`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { user.id }
        }
        val scalar = TestUser()
            .select { it.id }
            .where { it.id == 7 }
        val table = KTableForSelect<TestUser>()
        table.addScalarSubquery(scalar, "scalarId")
        query.context.projectionItems = table.projectionItems

        val statement = query.toSqlQuery() as SqlQuery.Select
        val scalarItem = statement.select
            .filterIsInstance<SqlSelectItem.Expr>()
            .first { it.alias == "scalarId" }
        assertIs<SqlExpr.Subquery>(scalarItem.expr)

        val countStatement = query.buildTotalCountTask().atomicTask.statement as SqlQuery.Select
        assertTrue(countStatement.select.any { (it as? SqlSelectItem.Expr)?.alias == "scalarId" })

        query.context.orderByItems = listOf(
            KTableForSort.SortItem.SelectableItem(scalar, SqlOrdering.Desc)
        )
        val ordered = query.toSqlQuery() as SqlQuery.Select
        assertIs<SqlExpr.Subquery>(ordered.orderBy.single().expr)
    }

    @Test
    fun `joined planner renders derived leaves and rejects sql server pages without root keys`() {
        val orders = SubqueryOrder()
            .select { [it.userId, it.status] }
            .where { it.status == 40 }
        val derived = SubqueryUser().join(orders) { user, order ->
            leftJoin { user.id == order.userId }
                .select { user.id }
        }

        val derivedStatement = derived.toSqlQuery() as SqlQuery.Select
        assertTrue(derivedStatement.from.any { it is SqlTable.Join })

        val noKey = CursorUnstableUser().join(UserRelation()) { row, relation ->
            innerJoin { row.score == relation.id2 }
                .select { row.score }
        }
        assertFailsWith<IllegalStateException> {
            noKey.page(2, 5).toSqlQuery(SampleSqlServerJdbcWrapper)
        }
    }

    @Test
    fun `joined planner validates missing and null cursor values`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { user.username }
                .orderBy { [user.id.desc(), relation.id.asc()] }
        }

        val missing = mapOf<String, Any?>("tb_user.id" to 10).toCursor()
        assertFailsWith<IllegalArgumentException> {
            query.cursor(pageSize = 1, after = missing).build()
        }

        val nullValue = mapOf<String, Any?>("tb_user.id" to null, "user_relation.id" to 20).toCursor()
        assertFailsWith<IllegalStateException> {
            query.cursor(pageSize = 1, after = nullValue).build()
        }
    }

    @Test
    fun `joined context resolves qualifier and conjunction fallbacks`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { user.id }
        }
        val context = query.context
        val left = SqlExpr.Column(columnName = "left")
        val right = SqlExpr.Column(columnName = "right")

        assertEquals(left, context.and(left, null))
        assertEquals(right, context.and(null, right))
        assertIs<SqlExpr.Binary>(context.and(left, right))
        assertEquals(null, context.andAll(emptyList()))
        assertEquals(left, context.andAll(listOf(left)))

        val relation = query.state.sources[1]
        context.setDatabase(relation, "archive")
        assertEquals(
            SqlIdentifier.of("archive", "user_relation"),
            context.qualifierIdentifierFor("user_relation")
        )
        assertEquals(SqlIdentifier.of("unknown"), context.qualifierIdentifierFor("unknown"))
        context.sourceValue(Field(tableName = "user_relation", columnName = "id", name = "id"))
        context.sourceValue(Field(tableName = "unknown", columnName = "id", name = "id"))

        assertEquals("id", context.bindParameter("id", 1))
        assertEquals("id@1", context.bindParameter("id", 2))
    }

    @Test
    fun `cursor rejects full joins as unstable outer sources`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            fullJoin { user.id == relation.id2 }
                .select { user.id }
                .orderBy { user.id.asc() }
        }

        assertFailsWith<IllegalArgumentException> { query.cursor(pageSize = 1) }
    }

    @Test
    fun `joined context copies optional state and advances suffixed parameters`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { user.id }
        }
        val source = query.context
        val selected = source.selectedFields.first()
        source.cascadeAllowed = setOf(selected)
        source.cascadeSelectedProps = setOf(selected)
        source.cursorSpec = CursorSpec(
            mapOf<String, Any?>("tb_user.id" to 1).toCursor(),
            pageSize = 2
        )
        source.andHaving(
            SqlExpr.Binary(
                SqlExpr.Column(columnName = "score"),
                SqlBinaryOperator.GreaterThan,
                parameter("score")
            ),
            mapOf("score" to 10)
        )

        assertEquals("id@2", source.bindParameter("id@2", 2))
        assertEquals("id@3", source.bindParameter("id@2", 3))

        val page = query.page(pageIndex = 1, pageSize = 2)

        assertIs<SqlQuery.Select>(page.toSqlQuery())
        assertEquals(setOf(selected), source.cascadeAllowed)
        assertEquals(setOf(selected), source.cascadeSelectedProps)
    }

    @Test
    fun `joined planner resolves selected order aliases and suffixed collisions`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { relation.id == 1 && relation.id == 2 && relation.id == 3 }
                .select { user.id }
        }
        val aliasField = Field(tableName = "tb_user", columnName = "username", name = "displayName")
        val sourceField = Field(tableName = "tb_user", columnName = "id", name = "id")
        query.context.registerProjectionItems(
            listOf(
                KTableForSelect.ProjectionItem.FieldItem(
                    aliasField,
                    SqlExpr.Column(tableName = "tb_user", columnName = "username")
                ),
                KTableForSelect.ProjectionItem.FieldItem(sourceField)
            ),
            listOf(aliasField, sourceField)
        )
        query.context.orderByItems = listOf(
            KTableForSort.SortItem.FieldItem(Field(columnName = "displayName", name = "displayName"), SqlOrdering.Asc),
            KTableForSort.SortItem.FieldItem(sourceField, SqlOrdering.Desc)
        )
        query.context.parameterValues["id@2"] = 4
        query.context.andWhere(
            SqlExpr.Binary(
                SqlExpr.Column(tableName = "tb_user", columnName = "id"),
                SqlBinaryOperator.Equal,
                parameter("id@2")
            )
        )

        val plan = query.toSqlQueryPlan()
        val statement = plan.query as SqlQuery.Select

        assertEquals(SqlExpr.Column(columnName = "displayName"), statement.orderBy.first().expr)
        assertTrue(plan.parameters.containsKey("id@3"))
    }

    @Test
    fun `cursor rejects expression order items before planning`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { user.id }
                .orderBy { addSortExpression(SqlExpr.NumberLiteral("1")) }
        }

        assertFailsWith<IllegalArgumentException> { query.cursor(pageSize = 1) }
    }

    @Test
    fun `cursor hidden labels reserve every projection output shape`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { user.username }
        }
        val root = query.state.sources[0]
        val relation = query.state.sources[1]
        val rootId = query.context.metadataFor(root).allColumns
            .first { it.name == "id" }
            .copy(tableName = query.context.qualifierFor(root))
        val relationId = query.context.metadataFor(relation).allColumns
            .first { it.name == "id" }
            .copy(tableName = query.context.qualifierFor(relation))
        val scalar = TestUser().select { it.id }
        val selectedExpr = SqlExpr.NumberLiteral("1")

        query.context.selectAll = false
        query.context.selectedFieldsByOutputName = emptyMap()
        query.context.projectionItems = listOf(
            KTableForSelect.ProjectionItem.FieldItem(
                field = Field(columnName = "reserved", name = "reserved"),
                outputName = "__KRONOS_CURSOR_ID"
            ),
            KTableForSelect.ProjectionItem.FieldItem(
                field = Field(columnName = "fallback", name = "")
            ),
            KTableForSelect.ProjectionItem.ScalarSubqueryValue(
                query = scalar,
                alias = "scalarValue",
                item = SqlSelectItem.Expr(SqlExpr.Subquery(scalar.toSqlQuery()), alias = "scalarValue")
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(selectedExpr, alias = "explicitAlias")
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(
                    expr = selectedExpr,
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "metadataAlias",
                        expression = selectedExpr,
                        scope = SqlSelectItemSourceScope.Selected
                    )
                )
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(SqlSelectItem.Expr(selectedExpr)),
            KTableForSelect.ProjectionItem.SelectItemValue(SqlSelectItem.Asterisk())
        )
        query.context.orderByItems = listOf(
            KTableForSort.SortItem.FieldItem(rootId, SqlOrdering.Asc),
            KTableForSort.SortItem.FieldItem(relationId, SqlOrdering.Asc)
        )

        query.context.prepareCursorOrder()

        assertEquals(
            listOf("__kronos_cursor_id_1", "__kronos_cursor_id_2"),
            query.context.cursorOnlySelectFields.map { it.second }
        )
    }

    private fun parameter(name: String): SqlExpr.Parameter =
        SqlExpr.Parameter(SqlParameter.Named(name))
}
