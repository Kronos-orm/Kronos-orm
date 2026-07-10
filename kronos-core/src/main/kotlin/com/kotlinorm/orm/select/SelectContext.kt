/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.select

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.cache.kPojoAllColumnsCache
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.cache.kPojoLogicDeleteCache
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.sql.renameNamedParameters
import com.kotlinorm.orm.sql.toSqlExpr
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.expr.SqlWindow
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.statement.SqlLock
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.utils.LinkedHashSet
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KType

internal class SelectContext<Source : KPojo, Selected : KPojo, Context : KPojo>(
    val pojo: Source,
    val receiverPojo: Context,
    val projectionType: KType
) {
    val kClass = pojo.kClass()
    val tableName = pojo.__tableName
    val allFields = kPojoAllFieldsCache[kClass]!!
    val allColumns = kPojoAllColumnsCache[kClass]!!
    val sourceValues: MutableMap<String, Any?> = pojo.toDataMap()
    val patchValues: MutableMap<String, Any?> = linkedMapOf()
    val parameterValues: MutableMap<String, Any?> = linkedMapOf()
    private val parameterNameCounter: MutableMap<String, Int> = mutableMapOf()

    var databaseName: String? = null
    var sourceQuery: KSelectable<*>? = null
    var sourceTableAlias: String? = null
    var logicDeleteStrategy = kPojoLogicDeleteCache[kClass]
    var cascadeEnabled = true
    var cascadeAllowed: Set<Field>? = null
    var cascadeSelectedProps: Set<Field>? = null
    var operationType = KOperationType.SELECT

    var selectedFields: LinkedHashSet<Field> = []
    var cascadeFields: LinkedHashSet<Field> = []
    var projectionItems: List<KTableForSelect.ProjectionItem> = emptyList()
    var selectAll = true
    var distinct = false
    var where: SqlExpr? = null
    var having: SqlExpr? = null
    var groupByItems: List<SqlExpr> = emptyList()
    var orderByItems: List<SelectOrderItem> = emptyList()
    var limit: SqlLimit? = null
    var lock: SqlLock? = null

    fun setSelectedFields(fields: Iterable<Field>) {
        selectedFields = fields.filter { it.isColumn }.toLinkedSet()
        cascadeFields = fields.filter { !it.isColumn }.toLinkedSet()
        projectionItems = selectedFields.map { KTableForSelect.ProjectionItem.FieldItem(it) }
        selectAll = selectedFields.isEmpty()
        distinct = false
    }

    fun setProjectionItems(projections: List<KTableForSelect.ProjectionItem>, fields: Iterable<Field>) {
        val fieldsSet = fields.toList().toLinkedSet()
        projectionItems = projections.qualifyProjectionItems(sourceTableAlias)
        selectedFields = fieldsSet.filter { it.isColumn }.toLinkedSet()
        cascadeFields = fieldsSet.filter { !it.isColumn }.toLinkedSet()
        selectAll = selectedFields.isEmpty() && projections.none {
            it is KTableForSelect.ProjectionItem.SelectItemValue || it is KTableForSelect.ProjectionItem.ScalarSubqueryValue
        }
    }

    fun selectExpr(field: Field): SqlExpr =
        if (sourceTableAlias == null) {
            field.toSqlExpr(false)
        } else {
            SqlExpr.Column(tableName = sourceTableAlias, columnName = field.columnName)
        }

    fun addFieldConditions(fields: Iterable<Field>, values: Map<String, Any?>) {
        val expressions = fields.map { field ->
            val parameterName = bindParameter(field.name, values[field.name])
            SqlExpr.Binary(
                selectExpr(field),
                SqlBinaryOperator.Equal,
                SqlExpr.Parameter(SqlParameter.Named(parameterName))
            )
        }.toList()
        andWhere(expressions.drop(1).fold(expressions.firstOrNull() ?: return) { left, right ->
            SqlExpr.Binary(left, SqlBinaryOperator.And, right)
        })
    }

    fun addSourceValueConditions() {
        val logicDeleteFieldName = logicDeleteStrategy?.field?.name
        val expressions = allColumns.mapNotNull { field ->
            val value = sourceValues[field.name] ?: return@mapNotNull null
            if (field.name == logicDeleteFieldName) return@mapNotNull null
            val parameterName = bindParameter(field.name, value)
            SqlExpr.Binary(
                selectExpr(field),
                SqlBinaryOperator.Equal,
                SqlExpr.Parameter(SqlParameter.Named(parameterName))
            )
        }
        andWhere(expressions.drop(1).fold(expressions.firstOrNull() ?: return) { left, right ->
            SqlExpr.Binary(left, SqlBinaryOperator.And, right)
        })
    }

    fun andWhere(expr: SqlExpr?, parameters: Map<String, Any?> = emptyMap()) {
        where = and(where, mergeParameters(expr, parameters))
    }

    fun andHaving(expr: SqlExpr?, parameters: Map<String, Any?> = emptyMap()) {
        having = and(having, mergeParameters(expr, parameters))
    }

    fun qualifySource(alias: String) {
        projectionItems = projectionItems.qualifyProjectionItems(alias)
        groupByItems = groupByItems.map { it.qualifySourceExpr(alias) }
        orderByItems = orderByItems.map { it.qualifySource(alias) }
        where = where?.qualifySourceExpr(alias)
        having = having?.qualifySourceExpr(alias)
    }

    private fun and(left: SqlExpr?, right: SqlExpr?): SqlExpr? =
        when {
            left == null -> right
            right == null -> left
            else -> SqlExpr.Binary(left, SqlBinaryOperator.And, right)
        }

    private fun mergeParameters(expr: SqlExpr?, parameters: Map<String, Any?>): SqlExpr? {
        if (expr == null) return null
        if (parameters.isEmpty()) return expr
        val renames = linkedMapOf<String, String>()
        parameters.forEach { (name, value) ->
            val uniqueName = bindParameter(name, value)
            if (uniqueName != name) {
                renames[name] = uniqueName
            }
        }
        return expr.renameNamedParameters(renames)
    }

    private fun bindParameter(name: String, value: Any?): String {
        val uniqueName = uniqueParameterName(name)
        parameterValues[uniqueName] = value
        return uniqueName
    }

    private fun uniqueParameterName(name: String): String {
        val (baseName, existingSuffix) = splitParameterSuffix(name)
        if (name !in parameterValues) {
            parameterNameCounter[baseName] = maxOf(parameterNameCounter.getOrDefault(baseName, 0), existingSuffix ?: 0)
            return name
        }

        var count = maxOf(parameterNameCounter.getOrDefault(baseName, 0), existingSuffix ?: 0)
        var candidate: String
        do {
            count++
            candidate = "$baseName@$count"
        } while (candidate in parameterValues)
        parameterNameCounter[baseName] = count
        return candidate
    }

    private fun splitParameterSuffix(name: String): Pair<String, Int?> {
        val match = parameterSuffixRegex.matchEntire(name) ?: return name to null
        return match.groupValues[1] to match.groupValues[2].toInt()
    }

    private companion object {
        val parameterSuffixRegex = Regex("""^(.+)@(\d+)$""")
    }
}

internal sealed class SelectOrderItem {
    abstract val ordering: SqlOrdering

    data class FieldItem(val field: Field, override val ordering: SqlOrdering) : SelectOrderItem()

    data class ExprItem(val expr: SqlExpr, override val ordering: SqlOrdering) : SelectOrderItem()

    data class SelectableItem(val query: KSelectable<*>, override val ordering: SqlOrdering) : SelectOrderItem()
}

internal fun List<KTableForSelect.ProjectionItem>.qualifyProjectionItems(alias: String?): List<KTableForSelect.ProjectionItem> {
    if (alias == null) return this
    return map { item ->
        when (item) {
            is KTableForSelect.ProjectionItem.FieldItem -> item
            is KTableForSelect.ProjectionItem.SelectItemValue ->
                item.copy(item = item.item.qualifySourceSelectItem(alias))
            is KTableForSelect.ProjectionItem.ScalarSubqueryValue -> item
        }
    }
}

internal fun SelectOrderItem.qualifySource(alias: String): SelectOrderItem =
    when (this) {
        is SelectOrderItem.FieldItem -> this
        is SelectOrderItem.ExprItem -> copy(expr = expr.qualifySourceExpr(alias))
        is SelectOrderItem.SelectableItem -> this
    }

internal fun SqlSelectItem.qualifySourceSelectItem(alias: String): SqlSelectItem =
    when (this) {
        is SqlSelectItem.Asterisk -> this
        is SqlSelectItem.Expr -> copy(
            expr = expr.qualifySourceExpr(alias),
            metadata = metadata?.let { it.copy(expression = it.expression.qualifySourceExpr(alias)) }
        )
    }

internal fun SqlExpr.qualifySourceAliasIfPresent(alias: String?): SqlExpr =
    alias?.let { qualifySourceExpr(it) } ?: this

internal fun SqlExpr.qualifySourceExpr(alias: String): SqlExpr =
    when (this) {
        is SqlExpr.Column -> if (tableName != alias || qualifier != SqlIdentifier.of(alias)) {
            copy(tableName = alias, qualifier = SqlIdentifier.of(alias))
        } else {
            this
        }
        is SqlExpr.Unary -> copy(expr = expr.qualifySourceExpr(alias))
        is SqlExpr.Binary -> copy(left = left.qualifySourceExpr(alias), right = right.qualifySourceExpr(alias))
        is SqlExpr.Tuple -> copy(items = items.map { it.qualifySourceExpr(alias) })
        is SqlExpr.Array -> copy(items = items.map { it.qualifySourceExpr(alias) })
        is SqlExpr.In -> copy(
            expr = expr.qualifySourceExpr(alias),
            `in` = when (val operand = `in`) {
                is SqlInRightOperand.Values -> operand.copy(items = operand.items.map { it.qualifySourceExpr(alias) })
                is SqlInRightOperand.Subquery -> operand
            }
        )
        is SqlExpr.Between -> copy(
            expr = expr.qualifySourceExpr(alias),
            start = start.qualifySourceExpr(alias),
            end = end.qualifySourceExpr(alias)
        )
        is SqlExpr.Like -> copy(
            expr = expr.qualifySourceExpr(alias),
            pattern = pattern.qualifySourceExpr(alias),
            escape = escape?.qualifySourceExpr(alias)
        )
        is SqlExpr.Function -> copy(
            args = args.map { it.qualifySourceExpr(alias) },
            orderBy = orderBy.map { it.copy(expr = it.expr.qualifySourceExpr(alias)) },
            withinGroup = withinGroup.map { it.copy(expr = it.expr.qualifySourceExpr(alias)) },
            filter = filter?.qualifySourceExpr(alias)
        )
        is SqlExpr.Window -> copy(expr = expr.qualifySourceExpr(alias), window = window.qualifySource(alias))
        is SqlExpr.Case -> copy(
            branches = branches.map {
                it.copy(`when` = it.`when`.qualifySourceExpr(alias), then = it.then.qualifySourceExpr(alias))
            },
            default = default?.qualifySourceExpr(alias)
        )
        is SqlExpr.SimpleCase -> copy(
            expr = expr.qualifySourceExpr(alias),
            branches = branches.map {
                it.copy(`when` = it.`when`.qualifySourceExpr(alias), then = it.then.qualifySourceExpr(alias))
            },
            default = default?.qualifySourceExpr(alias)
        )
        is SqlExpr.Coalesce -> copy(items = items.map { it.qualifySourceExpr(alias) })
        is SqlExpr.NullIf -> copy(expr = expr.qualifySourceExpr(alias), test = test.qualifySourceExpr(alias))
        is SqlExpr.Cast -> copy(expr = expr.qualifySourceExpr(alias))
        is SqlExpr.QuantifiedComparisonPredicate -> copy(expr = expr.qualifySourceExpr(alias))
        else -> this
    }

internal fun SqlWindow.qualifySource(alias: String): SqlWindow =
    copy(
        partitionBy = partitionBy.map { it.qualifySourceExpr(alias) },
        orderBy = orderBy.map { it.copy(expr = it.expr.qualifySourceExpr(alias)) }
    )

internal fun SqlExpr.numberLiteralInt(): Int? = (this as? SqlExpr.NumberLiteral)?.number?.toIntOrNull()
