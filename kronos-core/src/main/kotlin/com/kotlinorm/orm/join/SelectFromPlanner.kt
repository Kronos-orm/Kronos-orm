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
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.orm.sql.materializeSqlQuery
import com.kotlinorm.orm.sql.toSqlParameterEq
import com.kotlinorm.orm.sql.totalCountSelectItem
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.inspect.SqlNodeRewriter
import com.kotlinorm.syntax.inspect.SqlParameterCollector
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
        val where = where(dataSource)
        val parameters = contextParameters(where)
        val parameterCounter = mutableMapOf<String, Int>()

        val fromTable = fromTable(dataSource, parameters, parameterCounter)
        val limit = if (totalCount) null else limit()
        val orderBy = if (totalCount) {
            emptyList()
        } else {
            sqlServerPageOrderItems(dataSource, limit) ?: orderItems(dataSource, parameters, parameterCounter)
        }
        val query = SqlQuery.Select(
            quantifier = if (context.distinctEnabled) SqlQuantifier.Distinct else null,
            select = selectItems(dataSource, parameters, parameterCounter, totalCount),
            from = listOf(fromTable),
            where = where,
            groupBy = if (context.groupEnabled && context.groupByFields.isNotEmpty()) {
                SqlGroup(items = context.groupByFields.map {
                    SqlGroupingItem.Expr(it.toSyntaxExpr().rewriteSourceAliases().rewriteDerivedJoinAliases())
                })
            } else null,
            having = context.having?.takeIf { context.havingEnabled }?.rewriteSourceAliases()?.rewriteDerivedJoinAliases(),
            orderBy = orderBy,
            limit = limit
        )
        return SqlQueryPlan(query, parameters)
    }

    private fun contextParameters(where: SqlExpr?): LinkedHashMap<String, Any?> {
        val names = buildList<SqlNode> {
            context.joinables.mapNotNullTo(this) { it.condition }
            where?.let(::add)
            context.having?.let(::add)
            context.projectionItems.filterIsInstance<KTableForSelect.ProjectionItem.SelectItemValue>()
                .mapTo(this) { it.item }
            context.orderByItems.filterIsInstance<KTableForSort.SortItem.ExpressionItem>()
                .mapTo(this) { it.expression }
        }.flatMap(SqlParameterCollector::collectNamedParameters)

        return linkedMapOf<String, Any?>().apply {
            names.distinct().forEach { name ->
                if (context.paramMap.containsKey(name)) put(name, context.paramMap[name])
            }
        }
    }

    private fun limit(): SqlLimit? =
        when {
            context.pageEnabled -> SqlLimit.limit(
                context.pageSize,
                if (context.pageIndex > 0) (context.pageIndex - 1) * context.pageSize else 0
            )
            context.limitCapacity != null && context.limitCapacity!! >= 0 -> SqlLimit.limit(context.limitCapacity!!)
            else -> null
        }

    private fun sqlServerPageOrderItems(dataSource: KronosDataSourceWrapper, limit: SqlLimit?): List<SqlOrderingItem>? {
        if (dataSource.dbType != DBType.Mssql) return null
        if (limit?.offset == null || context.orderEnabled && context.orderByItems.isNotEmpty()) return null
        val primaryKey = context.allFields.firstOrNull { it.primaryKey != PrimaryKeyType.NOT }
            ?: error("SQL Server page() requires orderBy() when no primary key is available for deterministic ordering.")
        return listOf(SqlOrderingItem(primaryKey.toSyntaxExpr().rewriteSourceAliases().rewriteDerivedJoinAliases()))
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
                    selected += projection.item
                        .rewriteDatabaseQualifiedTables()
                        .rewriteSourceAliases()
                        .rewriteDerivedJoinAliases()
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
        var from: SqlTable = tableIdent(context.tableName, context.root)
        context.joinables.forEach { joinable ->
            val derived = context.derivedJoinQueries[joinable.tableName]
            val right = if (derived == null) {
                tableIdent(joinable.tableName, joinable.kPojo)
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

    private fun tableIdent(tableName: String, source: KPojo? = null): SqlTable.Ident {
        val parts = listOfNotNull(context.databaseOfTable[tableName], tableName)
        return SqlTable.Ident(
            name = parts.joinToString("."),
            alias = (source?.let(context::sourceAliasFor) ?: context.sourceAliasFor(tableName))
                ?.let { SqlTableAlias(it) },
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
                                tableName = (
                                    context.sourceAliasFor(joinable.kPojo)
                                        ?: context.sourceAliasFor(field.tableName)
                                        ?: field.tableName
                                    ).takeIf { it.isNotBlank() },
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
            ?.rewriteSourceAliases()
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
            val sourceName = context.sourceAliasFor(context.root)
                ?: context.sourceAliasFor(field.tableName)
                ?: field.tableName
            condition = context.and(
                condition,
                SqlExpr.Binary(
                    SqlExpr.Column(
                        tableName = sourceName.takeIf { it.isNotBlank() },
                        columnName = field.columnName
                    ),
                    SqlBinaryOperator.Equal,
                    databaseBooleanLiteral(dataSource, field, false)
                )
            )
        }
        return condition?.rewriteDatabaseQualifiedTables()?.rewriteSourceAliases()?.rewriteDerivedJoinAliases()
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
                }.rewriteSourceAliases().rewriteDerivedJoinAliases(),
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
                    tableName = sourceName().takeIf { it.isNotBlank() },
                    columnName = columnName,
                    qualifier = sourceQualifier()
                ),
                userReferenceable = true
            )
        )
    }

    private fun Field.toSyntaxExpr(): SqlExpr =
        SqlExpr.Column(
            tableName = sourceName().takeIf { it.isNotBlank() },
            columnName = columnName,
            qualifier = sourceQualifier()
        )

    private fun Field.sourceName(): String =
        context.sourceAliasFor(tableName)
            ?: context.sourceAliasFor(context.root).takeIf { tableName == context.tableName }
            ?: tableName

    private fun Field.sourceQualifier(): SqlIdentifier? {
        if (tableName.isBlank()) return null
        val sourceName = sourceName()
        return if (sourceName != tableName) SqlIdentifier.of(sourceName) else tableQualifier(tableName)
    }

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

    private fun SqlSelectItem.rewriteSourceAliases(): SqlSelectItem {
        val replacements = context.sourceAliasReplacements()
        if (replacements.isEmpty()) return this
        return when (this) {
            is SqlSelectItem.Asterisk -> tableName?.let { table ->
                replacements[table]?.let { alias -> copy(tableName = alias, qualifier = SqlIdentifier.of(alias)) }
            } ?: this
            is SqlSelectItem.Expr -> copy(
                expr = expr.rewriteSourceAliases(replacements),
                metadata = metadata?.let {
                    it.copy(
                        expression = it.expression.rewriteSourceAliases(replacements),
                        source = it.source?.let { source ->
                            source.tableName?.let { tableName ->
                                replacements[tableName]?.let { alias ->
                                    source.copy(tableName = alias, qualifier = SqlIdentifier.of(alias))
                                }
                            } ?: source
                        }
                    )
                }
            )
        }
    }

    private fun SqlExpr.rewriteSourceAliases(
        replacements: Map<String, String> = context.sourceAliasReplacements()
    ): SqlExpr {
        if (replacements.isEmpty()) return this
        return object : SqlNodeRewriter {
            override fun rewriteExpr(expr: SqlExpr): SqlExpr =
                when (expr) {
                    is SqlExpr.Column -> {
                        val replacement = expr.tableName?.let { replacements[it] }
                        if (replacement == null) {
                            expr
                        } else {
                            expr.copy(tableName = replacement, qualifier = SqlIdentifier.of(replacement))
                        }
                    }
                    is SqlExpr.Subquery -> expr
                    is SqlExpr.ExistsPredicate -> expr
                    is SqlExpr.QuantifiedComparisonPredicate -> expr.copy(expr = rewriteExpr(expr.expr))
                    is SqlExpr.In -> expr.copy(
                        expr = rewriteExpr(expr.expr),
                        `in` = when (val operand = expr.`in`) {
                            is SqlInRightOperand.Values -> operand.copy(items = operand.items.map(::rewriteExpr))
                            is SqlInRightOperand.Subquery -> operand
                        }
                    )
                    else -> super.rewriteExpr(expr)
                }
        }.rewriteExpr(this)
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
