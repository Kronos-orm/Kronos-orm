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
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.orm.sql.materializeSqlQuery
import com.kotlinorm.orm.sql.renameNamedParameters
import com.kotlinorm.orm.sql.toSqlParameterEq
import com.kotlinorm.orm.sql.totalCountSelectItem
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.group.SqlGroup
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.inspect.SqlNodeRewriter
import com.kotlinorm.syntax.inspect.SqlParameterCollector
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlSelectItemSource
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.utils.databaseBooleanLiteral
import com.kotlinorm.utils.execute

internal class JoinedSelectPlanner(
    private val context: JoinedSelectContext<*, *, *>
) {
    fun plan(dataSource: KronosDataSourceWrapper): SqlQueryPlan = plan(dataSource, totalCount = false)

    fun planTotalCount(dataSource: KronosDataSourceWrapper): SqlQueryPlan = plan(dataSource, totalCount = true)

    private fun plan(dataSource: KronosDataSourceWrapper, totalCount: Boolean): SqlQueryPlan {
        val parameters = linkedMapOf<String, Any?>()
        parameters.putAll(context.parameterValues)
        parameters.putAll(context.patchValues)
        val parameterFields = linkedMapOf<String, Field>()
        parameterFields.putAll(context.parameterFields)
        val parameterCounter = mutableMapOf<String, Int>()
        val crossJoinFilters = mutableListOf<SqlExpr>()
        val selectItems = selectItems(dataSource, parameters, parameterCounter, parameterFields, totalCount)
        val from = fromSource(
            context.state.current,
            dataSource,
            parameters,
            parameterCounter,
            parameterFields,
            crossJoinFilters
        )
        val where = where(dataSource, parameters, parameterFields, crossJoinFilters)
        val having = context.having
            ?.rewriteRuntimeQualifiers()
        val limit = context.limit.takeUnless { totalCount }
        val orderBy = if (totalCount) {
            emptyList()
        } else {
            sqlServerPageOrderItems(dataSource, limit)
                ?: orderItems(selectItems, dataSource, parameters, parameterCounter, parameterFields)
        }
        val query = SqlQuery.Select(
            quantifier = if (context.distinct) SqlQuantifier.Distinct else null,
            select = selectItems,
            from = listOf(from),
            where = where,
            groupBy = context.groupByFields.takeIf { it.isNotEmpty() }?.let { fields ->
                SqlGroup(items = fields.map { field -> SqlGroupingItem.Expr(field.toSyntaxExpr()) })
            },
            having = having,
            orderBy = orderBy,
            limit = limit,
            lock = context.lock.takeUnless { totalCount }
        )
        val orderedParameters = linkedMapOf<String, Any?>()
        val orderedParameterFields = linkedMapOf<String, Field>()
        SqlParameterCollector.collectNamedParameters(query).forEach { name ->
            if (name in parameters) {
                orderedParameters.putIfAbsent(name, parameters.getValue(name))
                parameterFields[name]?.let { orderedParameterFields.putIfAbsent(name, it) }
            }
        }
        return SqlQueryPlan(query, orderedParameters, orderedParameterFields)
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
        val visible = if (context.selectAll) {
            context.allFields.map { field ->
                field.toSyntaxSelectItem(exprOverride = field.toSyntaxExpr(context.root))
            }
        } else {
            context.projectionItems.mapNotNull { projection ->
                when (projection) {
                    is KTableForSelect.ProjectionItem.FieldItem ->
                        projection.field
                            .takeIf(Field::isColumn)
                            ?.toSyntaxSelectItem(
                                projection.outputName,
                                projection.expr
                                    ?.rewriteRuntimeQualifiers()
                                    ?.qualifyUnqualifiedDerivedColumns()
                            )
                    is KTableForSelect.ProjectionItem.SelectItemValue ->
                        projection.item.rewriteRuntimeQualifiers()
                    is KTableForSelect.ProjectionItem.ScalarSubqueryValue -> {
                        val query = projection.query.materializeSqlQuery(
                            parameters,
                            parameterCounter,
                            dataSource,
                            parameterFields
                        )
                        val expr = SqlExpr.Subquery(query)
                        SqlSelectItem.Expr(
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
        }
        if (totalCount) return visible
        return visible + context.cursorOnlySelectFields.map { (field, alias) ->
            field.toCursorSelectItem(alias)
        }
    }

    private fun fromSource(
        node: FromSourceNode,
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterCounter: MutableMap<String, Int>,
        parameterFields: MutableMap<String, Field>,
        crossJoinFilters: MutableList<SqlExpr>
    ): SqlTable = when (node) {
        is FromSourceNode.Leaf -> leafTable(node.source, dataSource, parameters, parameterCounter, parameterFields)
        is FromSourceNode.Join -> {
            val rightRoot = node.right.rootLeaf()
            val rightLogicDelete = logicDeleteExpression(rightRoot, dataSource)
            val condition = node.condition?.let { bindJoinCondition(it, parameters, parameterFields) }
            if (node.joinType == SqlJoinType.Cross && rightLogicDelete != null) {
                crossJoinFilters += rightLogicDelete
            }
            SqlTable.Join(
                left = fromSource(
                    node.left,
                    dataSource,
                    parameters,
                    parameterCounter,
                    parameterFields,
                    crossJoinFilters
                ),
                joinType = node.joinType,
                right = fromSource(
                    node.right,
                    dataSource,
                    parameters,
                    parameterCounter,
                    parameterFields,
                    crossJoinFilters
                ),
                condition = if (node.joinType == SqlJoinType.Cross) {
                    null
                } else {
                    context.and(condition, rightLogicDelete)?.let(SqlJoinCondition::On)
                }
            )
        }
    }

    private fun leafTable(
        leaf: FromSourceLeaf,
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterCounter: MutableMap<String, Int>,
        parameterFields: MutableMap<String, Field>
    ): SqlTable {
        leaf.query?.let { query ->
            return SqlTable.Subquery(
                query = query.materializeSqlQuery(parameters, parameterCounter, dataSource, parameterFields),
                alias = SqlTableAlias(requireNotNull(context.derivedAliasFor(leaf.pojo)))
            )
        }
        val metadata = context.metadataFor(leaf.pojo)
        val parts = listOfNotNull(context.databaseFor(leaf.pojo), metadata.tableName)
        return SqlTable.Ident(
            name = parts.joinToString("."),
            alias = context.sourceAliasFor(leaf.pojo)?.let(::SqlTableAlias),
            identifier = SqlIdentifier.of(parts)
        )
    }

    private fun where(
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterFields: MutableMap<String, Field>,
        crossJoinFilters: List<SqlExpr>
    ): SqlExpr? {
        var result = context.where
            ?.rewriteRuntimeQualifiers()
        if (result == null) {
            val rootValues = context.root.toDataMap()
            val expressions = context.allFields.mapNotNull { field ->
                val value = rootValues[field.name] ?: return@mapNotNull null
                val parameterName = uniqueParameterName(field.name, parameters)
                parameters[parameterName] = value
                parameterFields[parameterName] = field
                field.toSqlParameterEq(parameterName, useTableAlias = true).rewriteRuntimeQualifiers()
            }
            result = context.andAll(expressions)
        }
        context.rootLogicDeleteStrategy?.execute(defaultValue = false) { field, _ ->
            result = context.and(result, logicDeleteExpression(context.state.current.rootLeaf(), dataSource, field))
        }
        crossJoinFilters.forEach { filter -> result = context.and(result, filter) }
        return context.and(result, cursorCondition(parameters, parameterFields))
    }

    private fun logicDeleteExpression(
        leaf: FromSourceLeaf,
        dataSource: KronosDataSourceWrapper,
        explicitField: Field? = null
    ): SqlExpr? {
        if (leaf.query != null) return null
        var expression: SqlExpr? = null
        val strategy = context.metadataFor(leaf.pojo).logicDeleteStrategy ?: return null
        strategy.execute(defaultValue = false) { field, _ ->
            val target = explicitField ?: field
            expression = SqlExpr.Binary(
                SqlExpr.Column(
                    tableName = context.qualifierFor(leaf.pojo),
                    columnName = target.columnName,
                    qualifier = SqlIdentifier.of(context.qualifierFor(leaf.pojo))
                ),
                SqlBinaryOperator.Equal,
                databaseBooleanLiteral(dataSource, target, false)
            )
        }
        return expression
    }

    private fun bindJoinCondition(
        snapshot: JoinConditionSnapshot,
        parameters: MutableMap<String, Any?>,
        parameterFields: MutableMap<String, Field>
    ): SqlExpr {
        val renames = linkedMapOf<String, String>()
        snapshot.parameters.forEach { (name, value) ->
            val uniqueName = uniqueParameterName(name, parameters)
            parameters[uniqueName] = value
            snapshot.parameterFields[name]?.let { parameterFields[uniqueName] = it }
            if (uniqueName != name) renames[name] = uniqueName
        }
        return context.conditionExpression(snapshot)
            .renameNamedParameters(renames)
            .qualifyUnqualifiedDerivedColumns()
    }

    private fun orderItems(
        selectItems: List<SqlSelectItem>,
        dataSource: KronosDataSourceWrapper,
        parameters: MutableMap<String, Any?>,
        parameterCounter: MutableMap<String, Int>,
        parameterFields: MutableMap<String, Field>
    ): List<SqlOrderingItem> = context.orderByItems.map { item ->
        SqlOrderingItem(
            expr = when (item) {
                is KTableForSort.SortItem.FieldItem ->
                    item.expr?.rewriteRuntimeQualifiers()
                        ?: selectItems.orderAlias(item.field.name)
                        ?: item.field.toSyntaxExpr()
                is KTableForSort.SortItem.ExpressionItem -> item.expression.rewriteRuntimeQualifiers()
                is KTableForSort.SortItem.SelectableItem ->
                    SqlExpr.Subquery(
                        item.query.materializeSqlQuery(parameters, parameterCounter, dataSource, parameterFields)
                    )
            },
            ordering = item.ordering
        )
    }

    private fun cursorCondition(
        parameters: MutableMap<String, Any?>,
        parameterFields: MutableMap<String, Field>
    ): SqlExpr? {
        val spec = context.cursorSpec ?: return null
        if (spec.cursor == null) return null
        val fields = context.orderByItems.map { item ->
            require(item is KTableForSort.SortItem.FieldItem) {
                "Cursor pagination requires field-based orderBy items."
            }
            item
        }
        val parts = fields.mapIndexed { index, item ->
            val key = context.cursorKey(item.field)
            require(spec.values.containsKey(key)) {
                "Cursor token is missing orderBy field '${item.field.name}'."
            }
            val value = spec.values[key]
                ?: error("Cursor token contains null for orderBy field '${item.field.name}', which is not supported.")
            val parameterName = uniqueParameterName(cursorParameterName(key), parameters)
            parameters[parameterName] = value
            parameterFields[parameterName] = item.field
            val comparison = SqlExpr.Binary(
                item.expr?.rewriteRuntimeQualifiers() ?: item.field.toSyntaxExpr(),
                if (item.ordering == SqlOrdering.Desc) SqlBinaryOperator.LessThan else SqlBinaryOperator.GreaterThan,
                SqlExpr.Parameter(SqlParameter.Named(parameterName))
            )
            fields.take(index).fold(comparison) { expression, previous ->
                val previousKey = context.cursorKey(previous.field)
                val previousValue = spec.values[previousKey]
                    ?: error("Cursor token contains null for orderBy field '${previous.field.name}', which is not supported.")
                val previousParameter = uniqueParameterName(cursorParameterName(previousKey), parameters)
                parameters[previousParameter] = previousValue
                parameterFields[previousParameter] = previous.field
                SqlExpr.Binary(
                    SqlExpr.Binary(
                        previous.expr?.rewriteRuntimeQualifiers() ?: previous.field.toSyntaxExpr(),
                        SqlBinaryOperator.Equal,
                        SqlExpr.Parameter(SqlParameter.Named(previousParameter))
                    ),
                    SqlBinaryOperator.And,
                    expression
                )
            }
        }
        return parts.drop(1).fold(parts.firstOrNull()) { left, right ->
            if (left == null) right else SqlExpr.Binary(left, SqlBinaryOperator.Or, right)
        }
    }

    private fun sqlServerPageOrderItems(
        dataSource: KronosDataSourceWrapper,
        limit: SqlLimit?
    ): List<SqlOrderingItem>? {
        if (dataSource === NoneDataSourceWrapper) return null
        if (dataSource.dbType != DBType.Mssql || limit?.offset == null || context.orderByItems.isNotEmpty()) return null
        val primaryKey = context.allFields.firstOrNull { it.primaryKey != PrimaryKeyType.NOT }
            ?: error("SQL Server page() requires orderBy() when no primary key is available for deterministic ordering.")
        return listOf(SqlOrderingItem(primaryKey.toSyntaxExpr()))
    }

    private fun Field.toSyntaxSelectItem(
        outputName: String? = null,
        exprOverride: SqlExpr? = null
    ): SqlSelectItem.Expr {
        val expr = exprOverride ?: toSyntaxExpr()
        val sourceExpr = expr as? SqlExpr.Column
        val sourceOutputName = name.ifBlank { columnName }
        val resolvedOutputName = outputName ?: sourceOutputName
        val alias = when {
            outputName != null -> outputName
            name != columnName -> name
            else -> null
        }
        return SqlSelectItem.Expr(
            expr = expr,
            alias = alias,
            metadata = SqlSelectItemAliasMetadata(
                outputName = resolvedOutputName,
                expression = expr,
                scope = if (alias == null) SqlSelectItemSourceScope.Source else SqlSelectItemSourceScope.Selected,
                source = SqlSelectItemSource(
                    tableName = sourceExpr?.tableName,
                    columnName = sourceExpr?.columnName ?: columnName,
                    qualifier = sourceExpr?.qualifier
                ),
                userReferenceable = true
            )
        )
    }

    private fun Field.toCursorSelectItem(alias: String): SqlSelectItem.Expr {
        val expr = toSyntaxExpr()
        return SqlSelectItem.Expr(
            expr = expr,
            alias = alias,
            metadata = SqlSelectItemAliasMetadata(
                outputName = alias,
                expression = expr,
                scope = SqlSelectItemSourceScope.Selected,
                userReferenceable = false
            )
        )
    }

    private fun Field.toSyntaxExpr(): SqlExpr.Column {
        val table = context.qualifierFor(tableName.ifBlank { context.tableName })
        return SqlExpr.Column(
            tableName = table.takeIf { it.isNotBlank() },
            columnName = columnName,
            qualifier = table.takeIf { it.isNotBlank() }?.let(context::qualifierIdentifierFor)
        )
    }

    private fun Field.toSyntaxExpr(source: KPojo): SqlExpr.Column {
        val table = context.qualifierFor(source)
        return SqlExpr.Column(
            tableName = table.takeIf { it.isNotBlank() },
            columnName = columnName,
            qualifier = table.takeIf { it.isNotBlank() }?.let(context::qualifierIdentifierFor)
        )
    }

    private fun SqlSelectItem.rewriteRuntimeQualifiers(): SqlSelectItem = when (this) {
        is SqlSelectItem.Asterisk -> {
            val replacement = tableName?.let(context.runtimeQualifierReplacements()::get)
            if (replacement == null) {
                this
            } else {
                copy(tableName = replacement, qualifier = context.qualifierIdentifierFor(replacement))
            }
        }
        is SqlSelectItem.Expr -> {
            val currentMetadata = metadata
            val rewrittenExpression = expr
                .rewriteRuntimeQualifiers()
                .let { expression ->
                    if (currentMetadata?.scope == SqlSelectItemSourceScope.Selected) {
                        expression
                    } else {
                        expression.qualifyUnqualifiedDerivedColumns()
                    }
                }
            copy(
                expr = rewrittenExpression,
                metadata = currentMetadata?.copy(
                    expression = rewrittenExpression,
                    source = currentMetadata.source?.let { source ->
                        val replacement = source.tableName?.let(context.runtimeQualifierReplacements()::get)
                        if (replacement == null) {
                            source
                        } else {
                            source.copy(
                                tableName = replacement,
                                qualifier = context.qualifierIdentifierFor(replacement)
                            )
                        }
                    }
                )
            )
        }
    }

    private fun SqlExpr.rewriteRuntimeQualifiers(): SqlExpr =
        rewriteJoinQualifiers(context.runtimeQualifierReplacements(), context::qualifierIdentifierFor)

    private fun SqlExpr.qualifyUnqualifiedDerivedColumns(): SqlExpr {
        return object : SqlNodeRewriter {
            override fun rewriteExpr(expr: SqlExpr): SqlExpr = when (expr) {
                is SqlExpr.Column -> if (expr.tableName == null) {
                    val alias = context.derivedAliasForUnqualifiedColumn(expr.columnName)
                    if (alias == null) {
                        expr
                    } else {
                        expr.copy(tableName = alias, qualifier = context.qualifierIdentifierFor(alias))
                    }
                } else {
                    expr
                }
                is SqlExpr.Subquery,
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

    private fun List<SqlSelectItem>.orderAlias(outputName: String): SqlExpr? =
        asSequence()
            .mapNotNull { (it as? SqlSelectItem.Expr)?.metadata }
            .firstOrNull { it.outputName == outputName && it.userReferenceable && it.scope != SqlSelectItemSourceScope.Source }
            ?.let { SqlExpr.Column(columnName = outputName) }

    private fun uniqueParameterName(name: String, parameters: Map<String, Any?>): String {
        if (name !in parameters) return name
        val match = parameterSuffixRegex.matchEntire(name)
        val base = match?.groupValues?.get(1) ?: name
        var index = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
        var candidate: String
        do {
            candidate = "$base@${++index}"
        } while (candidate in parameters)
        return candidate
    }

    private fun cursorParameterName(key: String): String =
        "cursor_${key.replace(Regex("[^A-Za-z0-9_]"), "_")}"

    private companion object {
        val parameterSuffixRegex = Regex("""^(.+)@(\d+)$""")
    }
}

internal fun SqlExpr.rewriteJoinQualifiers(
    replacements: Map<String, String>,
    qualifierIdentifier: (String) -> SqlIdentifier = { SqlIdentifier.of(it) }
): SqlExpr {
    if (replacements.isEmpty()) return this
    return object : SqlNodeRewriter {
        override fun rewriteExpr(expr: SqlExpr): SqlExpr = when (expr) {
            is SqlExpr.Column -> expr.tableName?.let(replacements::get)?.let { replacement ->
                expr.copy(tableName = replacement, qualifier = qualifierIdentifier(replacement))
            } ?: expr
            is SqlExpr.Subquery,
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
