/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KJoinable
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.orm.sql.materializeSqlQuery
import com.kotlinorm.orm.sql.toSqlParameterEq
import com.kotlinorm.orm.sql.totalCountSelectItem
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.inspect.SqlNodeRewriter
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlSelectItemSource
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.utils.databaseBooleanLiteral
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.resolveRuntimeMetadata

internal class SelectFromPlanner(
    private val context: SelectFromContext<*, *, *>
) {
    fun plan(dataSource: KronosDataSourceWrapper): SqlQueryPlan =
        plan(dataSource, totalCount = false)

    fun planTotalCount(dataSource: KronosDataSourceWrapper): SqlQueryPlan =
        plan(dataSource, totalCount = true)

    private fun plan(dataSource: KronosDataSourceWrapper, totalCount: Boolean): SqlQueryPlan {
        val parameters = linkedMapOf<String, Any?>()
        val parameterCounter = mutableMapOf<String, Int>()

        val fromTable = fromTable(dataSource, parameters, parameterCounter)
        val query = SqlQuery.Select(
            quantifier = if (context.distinctEnabled) SqlQuantifier.Distinct else null,
            select = selectItems(dataSource, parameters, parameterCounter, totalCount),
            from = listOf(fromTable),
            where = where(dataSource),
            groupBy = if (context.groupEnabled && context.groupByFields.isNotEmpty()) {
                SqlGroup(items = context.groupByFields.map {
                    SqlGroupingItem.Expr(it.toSyntaxExpr().rewriteDerivedJoinAliases())
                })
            } else null,
            having = context.having?.takeIf { context.havingEnabled }?.rewriteDerivedJoinAliases(),
            orderBy = if (totalCount) emptyList() else orderItems(dataSource, parameters, parameterCounter),
            limit = if (totalCount) {
                null
            } else {
                when {
                    context.pageEnabled -> SqlLimit.limit(
                        context.pageSize,
                        if (context.pageIndex > 0) (context.pageIndex - 1) * context.pageSize else 0
                    )
                    context.limitCapacity > 0 -> SqlLimit.limit(context.limitCapacity)
                    else -> null
                }
            }
        )
        context.paramMap.forEach { (name, value) ->
            if (value != null || name !in parameters) {
                parameters[name] = value
            }
        }
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
        val selected = mutableListOf<SqlSelectItem>()
        context.projectionItems.forEach { projection ->
            when (projection) {
                is KTableForSelect.ProjectionItem.FieldItem -> Unit
                is KTableForSelect.ProjectionItem.SelectItemValue ->
                    selected += projection.item.rewriteDatabaseQualifiedTables().rewriteDerivedJoinAliases()
                is KTableForSelect.ProjectionItem.ScalarSubqueryValue -> {
                    val query = projection.query.materializeSqlQuery(parameters, parameterCounter, dataSource)
                    val expr = SqlExpr.Subquery(query)
                    selected += SqlSelectItem.Expr(
                        expr = expr,
                        alias = projection.alias,
                        metadata = SqlSelectItemAliasMetadata(
                            outputName = projection.alias,
                            expression = expr,
                            scope = SqlSelectItemSourceScope.Selected
                        )
                    )
                }
            }
        }
        if (context.selectedFieldsByAlias.isNotEmpty()) {
            context.selectedFieldsByAlias.forEach { (alias, field) -> selected += field.toSyntaxSelectItem(alias) }
        } else {
            selectedFieldsForPlan().forEach { field -> selected += field.toSyntaxSelectItem() }
        }
        return selected.ifEmpty { listOf(SqlSelectItem.Asterisk()) }
    }

    private fun selectedFieldsForPlan(): Iterable<Field> =
        if (context.selectedFields.isEmpty() && context.projectionItems.isEmpty()) {
            context.allFields
        } else {
            context.selectedFields
        }

    private fun fromTable(
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterCounter: MutableMap<String, Int>
    ): SqlTable {
        var from: SqlTable = tableIdent(context.tableName)
        context.joinables.forEach { joinable ->
            val derived = context.derivedJoinQueries[joinable.tableName]
            val right = if (derived == null) {
                tableIdent(joinable.tableName)
            } else {
                val (query, alias) = derived
                SqlTable.Subquery(
                    query = query.materializeSqlQuery(parameters, parameterCounter, dataSource),
                    alias = SqlTableAlias(alias)
                )
            }
            from = SqlTable.Join(
                left = from,
                joinType = joinable.joinType,
                right = right,
                condition = joinCondition(joinable, dataSource)?.let { SqlJoinCondition.On(it) }
            )
        }
        return from
    }

    private fun tableIdent(tableName: String): SqlTable.Ident {
        val parts = listOfNotNull(context.databaseOfTable[tableName], tableName)
        return SqlTable.Ident(
            name = parts.joinToString("."),
            identifier = SqlIdentifier.of(parts)
        )
    }

    private fun joinCondition(joinable: KJoinable, dataSource: KronosDataSourceWrapper): SqlExpr? {
        var condition = joinable.condition
        if (joinable.tableName !in context.derivedJoinQueries) {
            joinable.kPojo.resolveRuntimeMetadata().logicDeleteStrategy
                ?.execute(defaultValue = false) { field, _ ->
                    condition = context.and(
                        condition,
                        SqlExpr.Binary(
                            SqlExpr.Column(
                                tableName = field.tableName.takeIf { it.isNotBlank() },
                                columnName = field.columnName
                            ),
                            SqlBinaryOperator.Equal,
                            databaseBooleanLiteral(dataSource, field, false)
                        )
                    )
                }
        }
        return condition
            ?.rewriteDatabaseQualifiedTables()
            ?.rewriteDerivedJoinAliases(joinable.tableAliasOverrides)
    }

    private fun where(dataSource: KronosDataSourceWrapper): SqlExpr? {
        var condition = context.where
        if (condition == null) {
            val mainTableData = context.root.toDataMap()
            condition = context.andAll(mainTableData.keys.filter {
                mainTableData[it] != null
            }.mapNotNull { propName ->
                context.allFields.firstOrNull { it.name == propName }?.let { field ->
                    context.paramMap[propName] = mainTableData[propName]
                    field.toSqlParameterEq(propName, useTableAlias = true)
                }
            })
        }
        context.logicDeleteStrategy?.execute(defaultValue = false) { field, _ ->
            condition = context.and(
                condition,
                SqlExpr.Binary(
                    SqlExpr.Column(tableName = field.tableName.takeIf { it.isNotBlank() }, columnName = field.columnName),
                    SqlBinaryOperator.Equal,
                    databaseBooleanLiteral(dataSource, field, false)
                )
            )
        }
        return condition?.rewriteDatabaseQualifiedTables()?.rewriteDerivedJoinAliases()
    }

    private fun orderItems(
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterCounter: MutableMap<String, Int>
    ): List<SqlOrderingItem> {
        if (!context.orderEnabled || context.orderByItems.isEmpty()) return emptyList()
        return context.orderByItems.map { item ->
            SqlOrderingItem(
                expr = when (item) {
                    is KTableForSort.SortItem.FieldItem -> item.field.toSyntaxExpr()
                    is KTableForSort.SortItem.ExpressionItem -> item.expression.rewriteDatabaseQualifiedTables()
                    is KTableForSort.SortItem.SelectableItem ->
                        SqlExpr.Subquery(item.query.materializeSqlQuery(parameters, parameterCounter, dataSource))
                }.rewriteDerivedJoinAliases(),
                ordering = item.ordering
            )
        }
    }

    private fun Field.toSyntaxSelectItem(alias: String? = null): SqlSelectItem.Expr {
        val expr = toSyntaxExpr().rewriteDerivedJoinAliases()
        val outputName = alias ?: name
        return SqlSelectItem.Expr(
            expr = expr,
            alias = alias,
            metadata = SqlSelectItemAliasMetadata(
                outputName = outputName,
                expression = expr,
                scope = if (alias == null) SqlSelectItemSourceScope.Source else SqlSelectItemSourceScope.Selected,
                source = SqlSelectItemSource(
                    tableName = tableName.takeIf { it.isNotBlank() },
                    columnName = columnName,
                    qualifier = tableName.takeIf { it.isNotBlank() }?.let { tableQualifier(it) }
                ),
                userReferenceable = true
            )
        )
    }

    private fun Field.toSyntaxExpr(): SqlExpr =
        SqlExpr.Column(
            tableName = tableName.takeIf { it.isNotBlank() },
            columnName = columnName,
            qualifier = tableName.takeIf { it.isNotBlank() }?.let { tableQualifier(it) }
        )

    private fun tableQualifier(tableName: String): SqlIdentifier =
        context.databaseOfTable[tableName]?.let { databaseName ->
            SqlIdentifier.of(databaseName, tableName)
        } ?: SqlIdentifier.of(tableName)

    private fun SqlSelectItem.rewriteDatabaseQualifiedTables(): SqlSelectItem =
        when (this) {
            is SqlSelectItem.Asterisk -> tableName?.let {
                copy(qualifier = tableQualifier(it))
            } ?: this
            is SqlSelectItem.Expr -> copy(
                expr = expr.rewriteDatabaseQualifiedTables(),
                metadata = metadata?.let {
                    it.copy(
                        expression = it.expression.rewriteDatabaseQualifiedTables(),
                        source = it.source?.let { source ->
                            source.tableName?.let { tableName ->
                                source.copy(qualifier = tableQualifier(tableName))
                            } ?: source
                        }
                    )
                }
            )
        }

    private fun SqlExpr.rewriteDatabaseQualifiedTables(): SqlExpr {
        if (context.databaseOfTable.isEmpty()) return this
        return object : SqlNodeRewriter {
            override fun rewriteExpr(expr: SqlExpr): SqlExpr {
                if (expr is SqlExpr.Column) {
                    val tableName = expr.tableName
                    if (tableName != null && tableName in context.databaseOfTable) {
                        return expr.copy(qualifier = tableQualifier(tableName))
                    }
                }
                return super.rewriteExpr(expr)
            }
        }.rewriteExpr(this)
    }

    private fun SqlSelectItem.rewriteDerivedJoinAliases(): SqlSelectItem =
        when (this) {
            is SqlSelectItem.Asterisk -> this
            is SqlSelectItem.Expr -> copy(
                expr = expr.rewriteDerivedJoinAliases(),
                metadata = metadata?.let { it.copy(expression = it.expression.rewriteDerivedJoinAliases()) }
            )
        }

    private fun SqlExpr.rewriteDerivedJoinAliases(
        replacements: Map<String, String> = context.derivedJoinAliasOverrides
    ): SqlExpr {
        if (replacements.isEmpty()) return this
        return object : SqlNodeRewriter {
            override fun rewriteExpr(expr: SqlExpr): SqlExpr {
                if (expr is SqlExpr.Column) {
                    val replacement = expr.tableName?.let { replacements[it] }
                    if (replacement != null) {
                        return expr.copy(tableName = replacement, qualifier = SqlIdentifier.of(replacement))
                    }
                    if (expr.tableName == null && replacements.size == 1) {
                        val alias = replacements.values.single()
                        return expr.copy(tableName = alias, qualifier = SqlIdentifier.of(alias))
                    }
                }
                return super.rewriteExpr(expr)
            }
        }.rewriteExpr(this)
    }
}
