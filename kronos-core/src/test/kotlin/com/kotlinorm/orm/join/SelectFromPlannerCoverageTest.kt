/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.join

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KJoinable
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.SourceIdentityScope
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.expr.SqlQuantifiedComparisonOperator
import com.kotlinorm.syntax.expr.SqlSubqueryQuantifier
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlSelectItemSource
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testutils.initializeCoreSqlTestDefaults
import com.kotlinorm.utils.resolveRuntimeMetadata
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import com.kotlinorm.wrappers.SampleSqlServerJdbcWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SelectFromPlannerCoverageTest {

    @Test
    fun `sql server paging falls back to primary key ordering`() {
        initializeCoreSqlTestDefaults()
        val context = plannerContext(PlannerEntity())
        context.pageEnabled = true
        context.pageIndex = 2
        context.pageSize = 5

        val query = SelectFromPlanner(context).plan(SampleSqlServerJdbcWrapper).query as SqlQuery.Select

        assertEquals(SqlLimit.limit(5, 5), query.limit)
        assertEquals(
            listOf(SqlOrderingItem(SqlExpr.Column(tableName = "planner_entity", columnName = "id"))),
            query.orderBy
        )
    }

    @Test
    fun `sql server paging honors explicit order and non-offset limits`() {
        initializeCoreSqlTestDefaults()
        val orderedContext = plannerContext(PlannerEntity())
        val valueField = orderedContext.allFields.first { it.name == "value" }
        orderedContext.pageEnabled = true
        orderedContext.pageIndex = 1
        orderedContext.pageSize = 4
        orderedContext.orderEnabled = true
        orderedContext.orderByItems = listOf(KTableForSort.SortItem.FieldItem(valueField, SqlOrdering.Desc))

        val ordered = SelectFromPlanner(orderedContext)
            .plan(SampleSqlServerJdbcWrapper)
            .query as SqlQuery.Select

        assertEquals(SqlLimit.limit(4, 0), ordered.limit)
        assertEquals(
            listOf(
                SqlOrderingItem(
                    expr = SqlExpr.Column(tableName = "planner_entity", columnName = "value"),
                    ordering = SqlOrdering.Desc
                )
            ),
            ordered.orderBy
        )

        val limitedContext = plannerContext(PlannerEntity())
        limitedContext.limitCapacity = 3
        val limited = SelectFromPlanner(limitedContext)
            .plan(SampleSqlServerJdbcWrapper)
            .query as SqlQuery.Select
        assertEquals(SqlLimit.limit(3), limited.limit)
        assertEquals(emptyList(), limited.orderBy)

        val negativeLimitContext = plannerContext(PlannerEntity())
        negativeLimitContext.limitCapacity = -1
        val negativeLimit = SelectFromPlanner(negativeLimitContext)
            .plan(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper)
            .query as SqlQuery.Select
        assertEquals(null, negativeLimit.limit)
        assertEquals(emptyList(), negativeLimit.orderBy)
    }

    @Test
    fun `sql server paging without a deterministic field fails with actionable message`() {
        initializeCoreSqlTestDefaults()
        val context = SelectFromContext<PlannerEntityWithoutPrimaryKey, PlannerEntityWithoutPrimaryKey, PlannerEntityWithoutPrimaryKey>(
            PlannerEntityWithoutPrimaryKey()
        )
        context.pageEnabled = true
        context.pageIndex = 1
        context.pageSize = 10

        val error = assertFailsWith<IllegalStateException> {
            SelectFromPlanner(context).plan(SampleSqlServerJdbcWrapper)
        }

        assertEquals(
            "SQL Server page() requires orderBy() when no primary key is available for deterministic ordering.",
            error.message
        )
    }

    @Test
    fun `planner collects only referenced parameters in syntax traversal order`() {
        initializeCoreSqlTestDefaults()
        val root = PlannerEntity()
        val joined = PlannerJoinedEntity()
        val context = plannerContext(root)
        context.setSources(root, joined)
        context.joinables += KJoinable(
            tableName = joined.__tableName,
            joinType = SqlJoinType.Inner,
            kClass = joined.resolveRuntimeMetadata().kClass,
            kPojo = joined,
            condition = parameterPredicate(joined.__tableName, "id", "join_value")
        )
        context.where = parameterPredicate(root.__tableName, "id", "where_value")
        context.having = parameterPredicate(root.__tableName, "value", "having_value")
        context.havingEnabled = true
        context.projectionItems += KTableForSelect.ProjectionItem.SelectItemValue(
            SqlSelectItem.Expr(parameter("projection_value"), alias = "projection_value")
        )
        context.orderEnabled = true
        context.orderByItems = listOf(
            KTableForSort.SortItem.ExpressionItem(parameter("order_value"), SqlOrdering.Asc)
        )
        context.paramMap.putAll(
            linkedMapOf(
                "join_value" to 1,
                "where_value" to 2,
                "having_value" to 3,
                "projection_value" to 4,
                "order_value" to 5,
                "unused_value" to 6
            )
        )

        val plan = SelectFromPlanner(context).plan(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper)

        assertEquals(
            linkedMapOf(
                "join_value" to 1,
                "where_value" to 2,
                "having_value" to 3,
                "projection_value" to 4,
                "order_value" to 5
            ),
            plan.parameters
        )
    }

    @Test
    fun `projection rewriting updates aliases metadata and value operands but preserves subquery scopes`() {
        initializeCoreSqlTestDefaults()
        val root = PlannerEntity()
        val nested = PlannerEntity()
        val context = plannerContext(root)
        val tableName = root.__tableName
        val sourceAlias = "planner_entity__k1"
        val externalTable = "external_table"
        context.withSourceScope {
            SourceIdentityScope.withFrame(SourceIdentityScope.frame(listOf(nested))) {
                assertEquals(sourceAlias, SourceIdentityScope.resolveTableName(root, tableName))
            }
        }
        context.databaseOfTable[tableName] = "archive"
        context.databaseOfTable[externalTable] = "catalog"
        context.allFields = emptyList()

        val rootColumn = SqlExpr.Column(tableName = tableName, columnName = "id")
        val externalColumn = SqlExpr.Column(tableName = externalTable, columnName = "code")
        val nestedQuery = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(rootColumn)),
            from = listOf(SqlTable.Ident(tableName))
        )
        context.projectionItems += listOf(
            KTableForSelect.ProjectionItem.SelectItemValue(SqlSelectItem.Asterisk(tableName)),
            KTableForSelect.ProjectionItem.SelectItemValue(SqlSelectItem.Asterisk()),
            KTableForSelect.ProjectionItem.SelectItemValue(SqlSelectItem.Asterisk(externalTable)),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(
                    expr = rootColumn,
                    alias = "root_id",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "root_id",
                        expression = rootColumn,
                        scope = SqlSelectItemSourceScope.Source,
                        source = SqlSelectItemSource(tableName = tableName, columnName = "id")
                    )
                )
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(SqlSelectItem.Expr(rootColumn)),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(
                    expr = SqlExpr.Column(columnName = "computed"),
                    alias = "computed",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "computed",
                        expression = SqlExpr.Column(columnName = "computed"),
                        scope = SqlSelectItemSourceScope.Selected,
                        source = SqlSelectItemSource(columnName = "computed")
                    )
                )
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(
                    expr = externalColumn,
                    alias = "external_code",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "external_code",
                        expression = externalColumn,
                        scope = SqlSelectItemSourceScope.Source,
                        source = SqlSelectItemSource(tableName = externalTable, columnName = "code")
                    )
                )
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(
                    SqlExpr.In(rootColumn, SqlInRightOperand.Values(listOf(rootColumn, externalColumn))),
                    alias = "in_values"
                )
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(
                    SqlExpr.In(rootColumn, SqlInRightOperand.Subquery(nestedQuery)),
                    alias = "in_subquery"
                )
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(
                    SqlExpr.QuantifiedComparisonPredicate(
                        expr = rootColumn,
                        operator = SqlQuantifiedComparisonOperator.GreaterThan,
                        quantifier = SqlSubqueryQuantifier.Any,
                        query = nestedQuery
                    ),
                    alias = "quantified"
                )
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(SqlExpr.Subquery(nestedQuery), alias = "subquery")
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(SqlExpr.ExistsPredicate(nestedQuery), alias = "exists_query")
            )
        )

        val query = SelectFromPlanner(context)
            .plan(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper)
            .query as SqlQuery.Select

        val aliasedRoot = SqlExpr.Column(tableName = sourceAlias, columnName = "id")
        val qualifiedExternal = SqlExpr.Column(
            tableName = externalTable,
            columnName = "code",
            qualifier = SqlIdentifier.of("catalog", externalTable)
        )
        val qualifiedNestedQuery = nestedQuery.copy(
            select = listOf(
                SqlSelectItem.Expr(
                    rootColumn.copy(qualifier = SqlIdentifier.of("archive", tableName))
                )
            )
        )
        assertEquals(
            listOf(
                SqlSelectItem.Asterisk(sourceAlias),
                SqlSelectItem.Asterisk(),
                SqlSelectItem.Asterisk(externalTable, SqlIdentifier.of("catalog", externalTable)),
                SqlSelectItem.Expr(
                    expr = aliasedRoot,
                    alias = "root_id",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "root_id",
                        expression = aliasedRoot,
                        scope = SqlSelectItemSourceScope.Source,
                        source = SqlSelectItemSource(tableName = sourceAlias, columnName = "id")
                    )
                ),
                SqlSelectItem.Expr(aliasedRoot),
                SqlSelectItem.Expr(
                    expr = SqlExpr.Column(columnName = "computed"),
                    alias = "computed",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "computed",
                        expression = SqlExpr.Column(columnName = "computed"),
                        scope = SqlSelectItemSourceScope.Selected,
                        source = SqlSelectItemSource(columnName = "computed")
                    )
                ),
                SqlSelectItem.Expr(
                    expr = qualifiedExternal,
                    alias = "external_code",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "external_code",
                        expression = qualifiedExternal,
                        scope = SqlSelectItemSourceScope.Source,
                        source = SqlSelectItemSource(
                            tableName = externalTable,
                            columnName = "code",
                            qualifier = SqlIdentifier.of("catalog", externalTable)
                        )
                    )
                ),
                SqlSelectItem.Expr(
                    SqlExpr.In(
                        aliasedRoot,
                        SqlInRightOperand.Values(listOf(aliasedRoot, qualifiedExternal))
                    ),
                    alias = "in_values"
                ),
                SqlSelectItem.Expr(
                    SqlExpr.In(aliasedRoot, SqlInRightOperand.Subquery(qualifiedNestedQuery)),
                    alias = "in_subquery"
                ),
                SqlSelectItem.Expr(
                    SqlExpr.QuantifiedComparisonPredicate(
                        expr = aliasedRoot,
                        operator = SqlQuantifiedComparisonOperator.GreaterThan,
                        quantifier = SqlSubqueryQuantifier.Any,
                        query = qualifiedNestedQuery
                    ),
                    alias = "quantified"
                ),
                SqlSelectItem.Expr(SqlExpr.Subquery(qualifiedNestedQuery), alias = "subquery"),
                SqlSelectItem.Expr(SqlExpr.ExistsPredicate(qualifiedNestedQuery), alias = "exists_query")
            ),
            query.select
        )
    }

    @Test
    fun `derived join rewriting qualifies known and unqualified expressions`() {
        initializeCoreSqlTestDefaults()
        val context = plannerContext(PlannerEntity())
        context.allFields = emptyList()
        context.derivedJoinAliasOverrides["derived_table"] = "derived_alias"
        context.projectionItems += listOf(
            KTableForSelect.ProjectionItem.SelectItemValue(SqlSelectItem.Asterisk("derived_table")),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(SqlExpr.Column("derived_table", "known"), alias = "known")
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(SqlExpr.Column(columnName = "unqualified"), alias = "unqualified")
            ),
            KTableForSelect.ProjectionItem.SelectItemValue(
                SqlSelectItem.Expr(SqlExpr.Column("other_table", "untouched"), alias = "untouched")
            )
        )

        val query = SelectFromPlanner(context)
            .plan(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper)
            .query as SqlQuery.Select

        assertEquals(
            listOf(
                SqlSelectItem.Asterisk("derived_table"),
                SqlSelectItem.Expr(SqlExpr.Column("derived_alias", "known"), alias = "known"),
                SqlSelectItem.Expr(SqlExpr.Column("derived_alias", "unqualified"), alias = "unqualified"),
                SqlSelectItem.Expr(SqlExpr.Column("other_table", "untouched"), alias = "untouched")
            ),
            query.select
        )
    }

    @Test
    fun `total count and empty projections use their canonical select items`() {
        initializeCoreSqlTestDefaults()
        val totalContext = plannerContext(PlannerEntity())
        totalContext.selectAll = true
        totalContext.pageEnabled = true
        totalContext.pageIndex = 3
        totalContext.pageSize = 7
        totalContext.orderEnabled = true
        totalContext.orderByItems = listOf(
            KTableForSort.SortItem.FieldItem(totalContext.allFields.first(), SqlOrdering.Desc)
        )

        val total = SelectFromPlanner(totalContext)
            .planTotalCount(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper)
            .query as SqlQuery.Select
        assertEquals(
            listOf(SqlSelectItem.Expr(SqlExpr.NumberLiteral("1"), alias = "count_value")),
            total.select
        )
        assertEquals(null, total.limit)
        assertEquals(emptyList(), total.orderBy)

        val emptyContext = plannerContext(PlannerEntity())
        emptyContext.allFields = emptyList()
        val empty = SelectFromPlanner(emptyContext)
            .plan(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper)
            .query as SqlQuery.Select
        assertEquals(listOf(SqlSelectItem.Asterisk()), empty.select)
    }

    private fun plannerContext(root: PlannerEntity): SelectFromContext<PlannerEntity, PlannerEntity, PlannerEntity> =
        SelectFromContext(root)

    private fun parameter(name: String): SqlExpr.Parameter =
        SqlExpr.Parameter(SqlParameter.Named(name))

    private fun parameterPredicate(tableName: String, columnName: String, name: String): SqlExpr =
        SqlExpr.Binary(
            left = SqlExpr.Column(tableName = tableName, columnName = columnName),
            operator = SqlBinaryOperator.Equal,
            right = parameter(name)
        )
}

@Table("planner_entity")
data class PlannerEntity(
    @PrimaryKey var id: Int? = null,
    var value: Int? = null
) : KPojo

@Table("planner_entity_without_primary_key")
data class PlannerEntityWithoutPrimaryKey(
    var id: Int? = null,
    var value: Int? = null
) : KPojo

@Table("planner_joined_entity")
data class PlannerJoinedEntity(
    @PrimaryKey var id: Int? = null,
    var plannerId: Int? = null
) : KPojo
