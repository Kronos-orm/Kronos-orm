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
import com.kotlinorm.beans.parser.NoneDataSourceWrapper
import com.kotlinorm.enums.DBType
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
        val parameterFields = linkedMapOf<String, Field>()
        parameterFields.putAll(context.parameterFields)

        val parameterCounter = mutableMapOf<String, Int>()
        val selectItems = selectItems(dataSource, parameters, parameterCounter, parameterFields, totalCount)
        val fromTable = table(dataSource, parameters, parameterCounter, parameterFields)
        val orderByItems = if (totalCount) {
            emptyList()
        } else {
            sqlServerPageOrderItems(dataSource) ?: context.orderByItems.map {
                it.toOrderingItem(selectItems, dataSource, parameters, parameterCounter, parameterFields)
            }
        }
        val query = SqlQuery.Select(
            quantifier = if (context.distinct) SqlQuantifier.Distinct else null,
            select = selectItems,
            from = listOf(fromTable),
            where = where(dataSource, parameters, parameterFields),
            groupBy = context.groupByItems.takeIf { it.isNotEmpty() }?.let {
                SqlGroup(items = it.map { expr -> SqlGroupingItem.Expr(context.bindExpr(expr)) })
            },
            having = context.having?.let(context::bindExpr),
            orderBy = orderByItems,
            limit = context.limit.takeUnless { totalCount },
            lock = context.lock.takeUnless { totalCount }
        )

        return SqlQueryPlan(query, parameters, parameterFields)
    }

    private fun selectItems(
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterCounter: MutableMap<String, Int>,
        parameterFields: MutableMap<String, Field>,
        totalCount: Boolean
    ): List<SqlSelectItem> {
        if (
            totalCount && !context.distinct &&
            (context.selectAll || context.projectionItems.all { it is KTableForSelect.ProjectionItem.FieldItem })
        ) {
            return listOf(totalCountSelectItem())
        }
        val visibleItems = if (context.selectAll) {
            context.allColumns.map { it.toPlannerSelectItem() }
        } else if (context.projectionItems.isNotEmpty()) {
            context.projectionItems.mapNotNull { projection ->
                when (projection) {
                    is KTableForSelect.ProjectionItem.FieldItem ->
                        projection.field.takeIf { it in context.selectedFields }?.toPlannerSelectItem(
                            projection.expr?.let(context::bindExpr),
                            projection.outputName
                        )
                    is KTableForSelect.ProjectionItem.SelectItemValue -> context.bindSelectItem(projection.item)
                    is KTableForSelect.ProjectionItem.ScalarSubqueryValue -> {
                        val query = projection.query.materializeSqlQuery(
                            parameters,
                            parameterCounter,
                            dataSource,
                            parameterFields
                        )
                        scalarSubqueryItem(query, projection.alias)
                    }
                }
            }
        } else {
            context.selectedFields.map { it.toPlannerSelectItem() }
        }
        if (totalCount) return visibleItems
        return visibleItems + context.cursorOnlySelectFields.map { (field, alias) ->
            field.toCursorOnlySelectItem(alias)
        }
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

    private fun Field.toPlannerSelectItem(
        exprOverride: SqlExpr? = null,
        resolvedOutputName: String? = null
    ): SqlSelectItem.Expr {
        val expr = exprOverride ?: toPlannerExpr()
        val sourceOutputName = if (context.sourceQuery != null) {
            context.sourceColumnName(this)
        } else {
            name.ifBlank { context.sourceColumnName(this) }
        }
        val outputName = resolvedOutputName ?: sourceOutputName
        val alias = when {
            outputName != sourceOutputName -> outputName
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
                    tableName = context.effectiveSourceAlias(),
                    columnName = context.sourceColumnName(this)
                ),
                userReferenceable = true
            )
        )
    }

    private fun Field.toCursorOnlySelectItem(alias: String): SqlSelectItem.Expr {
        val expr = toPlannerExpr()
        return SqlSelectItem.Expr(
            expr = expr,
            alias = alias,
            metadata = SqlSelectItemAliasMetadata(
                outputName = alias,
                expression = expr,
                scope = SqlSelectItemSourceScope.Selected,
                source = SqlSelectItemSource(
                    tableName = context.effectiveSourceAlias(),
                    columnName = context.sourceColumnName(this)
                ),
                userReferenceable = false
            )
        )
    }

    private fun Field.toPlannerExpr(): SqlExpr =
        SqlExpr.Column(
            tableName = context.effectiveSourceAlias(),
            columnName = context.sourceColumnName(this)
        )

    private fun table(
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterCounter: MutableMap<String, Int>,
        parameterFields: MutableMap<String, Field>
    ): SqlTable {
        context.sourceQuery?.let { query ->
            return SqlTable.Subquery(
                query = query.materializeSqlQuery(parameters, parameterCounter, dataSource, parameterFields),
                alias = SqlTableAlias(context.sourceTableAlias ?: "q")
            )
        }
        val parts = listOfNotNull(context.databaseName, context.tableName)
        return SqlTable.Ident(
            name = parts.joinToString("."),
            alias = context.effectiveSourceAlias()?.let { SqlTableAlias(it) },
            identifier = SqlIdentifier.of(parts)
        )
    }

    private fun where(
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterFields: MutableMap<String, Field>
    ): SqlExpr? {
        var where = context.where?.let(context::bindExpr)
        context.logicDeleteStrategy?.execute(defaultValue = false) { field, _ ->
            val logicDeleteExpression = SqlExpr.Binary(
                SqlExpr.Column(
                    tableName = context.effectiveSourceAlias(),
                    columnName = context.sourceColumnName(field)
                ),
                SqlBinaryOperator.Equal,
                databaseBooleanLiteral(dataSource, field, false)
            )
            where = and(where, logicDeleteExpression)
        }
        return and(where, cursorCondition(parameters, parameterFields))
    }

    private fun cursorCondition(
        parameters: MutableMap<String, Any?>,
        parameterFields: MutableMap<String, Field>
    ): SqlExpr? {
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
            parameterFields[parameterName] = item.field
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
                parameterFields[previousParameterName] = previous.field
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

    private fun sqlServerPageOrderItems(dataSource: KronosDataSourceWrapper): List<SqlOrderingItem>? {
        if (dataSource === NoneDataSourceWrapper) return null
        if (dataSource.dbType != DBType.Mssql) return null
        val limit = context.limit ?: return null
        if (limit.offset == null || context.orderByItems.isNotEmpty()) return null
        val primaryKey = context.primaryKey
            ?: error("SQL Server page() requires orderBy() when no primary key is available for deterministic ordering.")
        return listOf(
            SqlOrderingItem(
                expr = SqlExpr.Column(
                    tableName = context.effectiveSourceAlias(),
                    columnName = context.sourceColumnName(primaryKey)
                ),
                ordering = SqlOrdering.Asc
            )
        )
    }

    private fun SelectOrderItem.toOrderingItem(
        selectItems: List<SqlSelectItem>,
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterCounter: MutableMap<String, Int>,
        parameterFields: MutableMap<String, Field>
    ): SqlOrderingItem =
        SqlOrderingItem(
            expr = when (this) {
                is SelectOrderItem.FieldItem -> selectItems.orderAliasExpr(field.name) ?: expr?.let(context::bindExpr) ?: field.toPlannerExpr()
                is SelectOrderItem.ExprItem -> context.bindExpr(expr)
                is SelectOrderItem.SelectableItem ->
                    SqlExpr.Subquery(
                        query.materializeSqlQuery(parameters, parameterCounter, dataSource, parameterFields)
                    )
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
