/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.select

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.orm.sql.materializeSqlQuery
import com.kotlinorm.orm.sql.totalCountSelectItem
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlSelectItemSource
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.utils.databaseBooleanLiteral
import com.kotlinorm.utils.execute

internal class SelectPlanner(
    private val context: SelectContext<*, *, *>
) {
    fun plan(dataSource: KronosDataSourceWrapper): SqlQueryPlan =
        plan(dataSource, totalCount = false)

    fun planTotalCount(dataSource: KronosDataSourceWrapper): SqlQueryPlan =
        plan(dataSource, totalCount = true)

    private fun plan(dataSource: KronosDataSourceWrapper, totalCount: Boolean): SqlQueryPlan {
        val parameters = linkedMapOf<String, Any?>()
        parameters.putAll(context.parameterValues)
        parameters.putAll(context.patchValues)

        val parameterCounter = mutableMapOf<String, Int>()
        val selectItems = selectItems(dataSource, parameters, parameterCounter, totalCount)
        val fromTable = table(dataSource, parameters, parameterCounter)
        val query = SqlQuery.Select(
            quantifier = if (context.distinct) SqlQuantifier.Distinct else null,
            select = selectItems,
            from = listOf(fromTable),
            where = where(dataSource, parameters),
            groupBy = context.groupByItems.takeIf { it.isNotEmpty() }?.let {
                SqlGroup(items = it.map(SqlGroupingItem::Expr))
            },
            having = context.having,
            orderBy = if (totalCount) emptyList() else context.orderByItems.map {
                it.toOrderingItem(selectItems, dataSource, parameters, parameterCounter)
            },
            limit = context.limit.takeUnless { totalCount },
            lock = context.lock.takeUnless { totalCount }
        )

        return SqlQueryPlan(query, parameters)
    }

    private fun selectItems(
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterCounter: MutableMap<String, Int>,
        totalCount: Boolean
    ): List<SqlSelectItem> {
        if (totalCount && (context.selectAll || context.projectionItems.all { it is KTableForSelect.ProjectionItem.FieldItem })) {
            return listOf(totalCountSelectItem())
        }
        if (context.selectAll) {
            return context.allColumns.map { it.toPlannerSelectItem() }
        }
        if (context.projectionItems.isNotEmpty()) {
            return context.projectionItems.mapNotNull { projection ->
                when (projection) {
                    is KTableForSelect.ProjectionItem.FieldItem ->
                        projection.field.takeIf { it in context.selectedFields }?.toPlannerSelectItem(projection.expr)
                    is KTableForSelect.ProjectionItem.SelectItemValue -> projection.item
                    is KTableForSelect.ProjectionItem.ScalarSubqueryValue -> {
                        val query = projection.query.materializeSqlQuery(parameters, parameterCounter, dataSource)
                        scalarSubqueryItem(query, projection.alias)
                    }
                }
            }
        }
        return context.selectedFields.map { it.toPlannerSelectItem() }
    }

    private fun scalarSubqueryItem(query: SqlQuery, alias: String): SqlSelectItem.Expr {
        val expr = SqlExpr.Subquery(query)
        return SqlSelectItem.Expr(
            expr = expr,
            alias = alias,
            metadata = SqlSelectItemAliasMetadata(
                outputName = alias,
                expression = expr,
                scope = SqlSelectItemSourceScope.Selected
            )
        )
    }

    private fun Field.toPlannerSelectItem(exprOverride: SqlExpr? = null): SqlSelectItem.Expr {
        val expr = exprOverride ?: toPlannerExpr()
        val outputName = name.ifBlank { context.sourceColumnName(this) }
        val alias = when {
            context.sourceQuery != null -> null
            name != columnName -> name
            else -> null
        }
        return SqlSelectItem.Expr(
            expr = expr,
            alias = alias,
            metadata = SqlSelectItemAliasMetadata(
                outputName = alias ?: outputName,
                expression = expr,
                scope = if (alias == null) SqlSelectItemSourceScope.Source else SqlSelectItemSourceScope.Selected,
                source = SqlSelectItemSource(
                    tableName = context.sourceTableAlias,
                    columnName = context.sourceColumnName(this)
                ),
                userReferenceable = true
            )
        )
    }

    private fun Field.toPlannerExpr(): SqlExpr =
        SqlExpr.Column(
            tableName = context.sourceTableAlias,
            columnName = context.sourceColumnName(this)
        )

    private fun table(
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterCounter: MutableMap<String, Int>
    ): SqlTable {
        context.sourceQuery?.let { query ->
            return SqlTable.Subquery(
                query = query.materializeSqlQuery(parameters, parameterCounter, dataSource),
                alias = SqlTableAlias(context.sourceTableAlias ?: "q")
            )
        }
        val parts = listOfNotNull(context.databaseName, context.tableName)
        return SqlTable.Ident(
            name = parts.joinToString("."),
            alias = context.sourceTableAlias?.let { SqlTableAlias(it) },
            identifier = SqlIdentifier.of(parts)
        )
    }

    private fun where(dataSource: KronosDataSourceWrapper, parameters: MutableMap<String, Any?>): SqlExpr? {
        var where = context.where
        context.logicDeleteStrategy?.execute(defaultValue = false) { field, _ ->
            val logicDeleteExpression = SqlExpr.Binary(
                SqlExpr.Column(
                    tableName = context.sourceTableAlias,
                    columnName = context.sourceColumnName(field)
                ),
                SqlBinaryOperator.Equal,
                databaseBooleanLiteral(dataSource, field, false)
            )
            where = and(where, logicDeleteExpression)
        }
        return and(where, cursorCondition(parameters))
    }

    private fun cursorCondition(parameters: MutableMap<String, Any?>): SqlExpr? {
        val spec = context.cursorSpec ?: return null
        if (spec.cursor == null) return null
        require(context.orderByItems.isNotEmpty()) { "Cursor pagination requires orderBy()." }
        val fields = context.orderByItems.map { item ->
            require(item is SelectOrderItem.FieldItem) { "Cursor pagination requires field-based orderBy items." }
            item
        }
        val parts = fields.mapIndexedNotNull { index, item ->
            require(spec.values.containsKey(item.field.name)) {
                "Cursor token is missing orderBy field '${item.field.name}'."
            }
            val value = spec.values[item.field.name]
                ?: error("Cursor token contains null for orderBy field '${item.field.name}', which is not supported.")
            val parameterName = "cursor_${item.field.name}"
            parameters[parameterName] = value
            val comparison = SqlExpr.Binary(
                item.expr ?: item.field.toPlannerExpr(),
                if (item.ordering == SqlOrdering.Desc) SqlBinaryOperator.LessThan else SqlBinaryOperator.GreaterThan,
                SqlExpr.Parameter(SqlParameter.Named(parameterName))
            )
            fields.take(index).fold(comparison) { acc, previous ->
                val previousValue = spec.values[previous.field.name]
                    ?: error("Cursor token contains null for orderBy field '${previous.field.name}', which is not supported.")
                val previousParameterName = "cursor_${previous.field.name}"
                parameters.putIfAbsent(previousParameterName, previousValue)
                SqlExpr.Binary(
                    SqlExpr.Binary(
                        previous.expr ?: previous.field.toPlannerExpr(),
                        SqlBinaryOperator.Equal,
                        SqlExpr.Parameter(SqlParameter.Named(previousParameterName))
                    ),
                    SqlBinaryOperator.And,
                    acc
                )
            }
        }
        return parts.drop(1).fold(parts.firstOrNull()) { left, right ->
            if (left == null) right else SqlExpr.Binary(left, SqlBinaryOperator.Or, right)
        }
    }

    private fun and(left: SqlExpr?, right: SqlExpr?): SqlExpr? =
        when {
            left == null -> right
            right == null -> left
            else -> SqlExpr.Binary(left, SqlBinaryOperator.And, right)
        }

    private fun SelectOrderItem.toOrderingItem(
        selectItems: List<SqlSelectItem>,
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterCounter: MutableMap<String, Int>
    ): SqlOrderingItem =
        SqlOrderingItem(
            expr = when (this) {
                is SelectOrderItem.FieldItem -> selectItems.orderAliasExpr(field.name) ?: expr ?: field.toPlannerExpr()
                is SelectOrderItem.ExprItem -> expr
                is SelectOrderItem.SelectableItem ->
                    SqlExpr.Subquery(query.materializeSqlQuery(parameters, parameterCounter, dataSource))
            },
            ordering = ordering
        )

    private fun List<SqlSelectItem>.orderAliasExpr(outputName: String): SqlExpr? =
        asSequence()
            .mapNotNull { (it as? SqlSelectItem.Expr)?.metadata }
            .firstOrNull { it.outputName == outputName && it.userReferenceable }
            ?.let { metadata ->
                when (metadata.scope) {
                    SqlSelectItemSourceScope.Aggregate,
                    SqlSelectItemSourceScope.Window -> SqlExpr.Column(columnName = outputName)
                    SqlSelectItemSourceScope.Selected -> SqlExpr.Column(columnName = outputName)
                    else -> null
                }
            }
}
