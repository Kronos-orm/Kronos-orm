/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.select

import com.kotlinorm.Kronos.tableNamingStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.SourceBinding
import com.kotlinorm.beans.dsl.SourceIdentityScope
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.withUniqueOutputNames
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.pagination.CursorSpec
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
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.utils.LinkedHashSet
import com.kotlinorm.utils.resolveRuntimeMetadata
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KType

internal class SelectContext<Source : KPojo, Selected : KPojo, Context : KPojo>(
    val pojo: Source,
    val receiverPojo: Context,
    val projectionType: KType
) {
    private val metadata = pojo.resolveRuntimeMetadata()
    val kClass = metadata.kClass
    val tableName = metadata.tableName
    val allFields = metadata.allFields
    val allColumns = metadata.allColumns
    val fieldMap = metadata.fieldMap
    val primaryKey = metadata.primaryKey
    private val tableIndexes = metadata.tableIndexes
    /**
     * Fields generated for a statically annotated KPojo keep the declared
     * table name, while callers may override __tableName for tenant/shard
     * routing. Keep every known declaration as an input alias so the same
     * binding works for both dynamic objects and ordinary KPojo instances.
     */
    private val dynamicSourceTableNames = buildSet {
        if (metadata.dynamic) {
            pojo::class.simpleName?.let(tableNamingStrategy::k2db)?.let(::add)
            add(tableNamingStrategy.k2db("<no name provided>"))
        }
        metadata.allFields.mapTo(this) { it.tableName }
    } - tableName - ""
    private val sourceColumnNames = allColumns.map { it.columnName }.toSet()
    private val derivedSourceOutputNamesByColumn = allColumns.associate { field ->
        field.columnName to field.name.ifBlank { field.columnName }
    }
    private val sourceIdentityFrame = SourceIdentityScope.frame(listOf(pojo))
    val sourceBinding: SourceBinding
        get() = SourceBinding(
            tableName = tableName,
            alias = sourceTableAlias ?: sourceIdentityAlias(),
            dynamicTableNames = dynamicSourceTableNames,
            sourceColumnNames = sourceColumnNames,
            derived = sourceQuery != null,
            derivedOutputNamesByColumn = derivedSourceOutputNamesByColumn
        )
    val sourceValues: MutableMap<String, Any?> = pojo.toDataMap()
    val patchValues: MutableMap<String, Any?> = linkedMapOf()
    val parameterValues: MutableMap<String, Any?> = linkedMapOf()
    private val parameterNameCounter: MutableMap<String, Int> = mutableMapOf()

    var databaseName: String? = null
    var sourceQuery: KSelectable<*>? = null
    var sourceTableAlias: String? = null
    var logicDeleteStrategy = metadata.logicDeleteStrategy
    var cascadeEnabled = true
    var cascadeAllowed: Set<Field>? = null
    var cascadeSelectedProps: Set<Field>? = null
    var operationType = KOperationType.SELECT

    var selectedFields: LinkedHashSet<Field> = []
    var selectedFieldsByOutputName: Map<String, Field> = emptyMap()
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
    var cursorSpec: CursorSpec? = null

    fun <T> withSourceScope(block: () -> T): T =
        SourceIdentityScope.withFrame(sourceIdentityFrame, block)

    fun sourceIdentityAlias(): String? =
        sourceIdentityFrame.existingAliasFor(pojo)

    fun effectiveSourceAlias(): String? =
        sourceTableAlias ?: sourceIdentityAlias()

    var cursorOnlySelectFields: List<Pair<Field, String>> = emptyList()
        private set

    fun copyStateFrom(source: SelectContext<Source, Selected, Context>) {
        sourceValues.clear()
        sourceValues.putAll(source.sourceValues)
        patchValues.clear()
        patchValues.putAll(source.patchValues)
        parameterValues.clear()
        parameterValues.putAll(source.parameterValues)
        parameterNameCounter.clear()
        parameterNameCounter.putAll(source.parameterNameCounter)
        databaseName = source.databaseName
        sourceQuery = source.sourceQuery
        sourceTableAlias = source.sourceTableAlias
        logicDeleteStrategy = source.logicDeleteStrategy
        cascadeEnabled = source.cascadeEnabled
        cascadeAllowed = source.cascadeAllowed?.toSet()
        cascadeSelectedProps = source.cascadeSelectedProps?.toSet()
        operationType = source.operationType
        selectedFields = source.selectedFields.toLinkedSet()
        selectedFieldsByOutputName = source.selectedFieldsByOutputName.toMap(linkedMapOf())
        cascadeFields = source.cascadeFields.toLinkedSet()
        projectionItems = source.projectionItems.toList()
        selectAll = source.selectAll
        distinct = source.distinct
        where = source.where
        having = source.having
        groupByItems = source.groupByItems.toList()
        orderByItems = source.orderByItems.toList()
        limit = source.limit
        lock = source.lock
        cursorSpec = source.cursorSpec?.copy(values = source.cursorSpec?.values.orEmpty().toMap())
        cursorOnlySelectFields = source.cursorOnlySelectFields.toList()
    }

    fun setSelectedFields(fields: Iterable<Field>) {
        val normalizedFields = fields.map { sourceBinding.bindField(it) }
        selectedFields = normalizedFields.filter { it.isColumn }.toLinkedSet()
        cascadeFields = normalizedFields.filter { !it.isColumn }.toLinkedSet()
        projectionItems = selectedFields
            .map { KTableForSelect.ProjectionItem.FieldItem(it) }
            .withUniqueOutputNames()
        selectedFieldsByOutputName = projectionItems.fieldItemsByOutputName()
        selectAll = selectedFields.isEmpty()
        distinct = false
    }

    fun setProjectionItems(projections: List<KTableForSelect.ProjectionItem>, fields: Iterable<Field>) {
        val binding = sourceBinding
        val fieldsSet = fields.map { binding.bindField(it) }.toList().toLinkedSet()
        projectionItems = projections
            .qualifyProjectionItems(sourceTableAlias)
            .map { binding.bindProjectionItem(it) }
            .withUniqueOutputNames()
        selectedFields = fieldsSet.filter { it.isColumn }.toLinkedSet()
        cascadeFields = fieldsSet.filter { !it.isColumn }.toLinkedSet()
        selectedFieldsByOutputName = projectionItems.fieldItemsByOutputName()
        selectAll = selectedFields.isEmpty() && projections.none {
            it is KTableForSelect.ProjectionItem.SelectItemValue || it is KTableForSelect.ProjectionItem.ScalarSubqueryValue
        }
    }

    fun selectExpr(field: Field): SqlExpr =
        if (effectiveSourceAlias() == null) {
            field.toSqlExpr(false)
        } else {
            SqlExpr.Column(tableName = effectiveSourceAlias(), columnName = sourceColumnName(field))
        }

    fun sourceColumnName(field: Field): String =
        sourceBinding.columnName(field)

    fun bindExpr(expr: SqlExpr): SqlExpr =
        sourceBinding.bindExpr(expr)

    fun bindSelectItem(item: SqlSelectItem): SqlSelectItem =
        sourceBinding.bindSelectItem(item)

    fun prepareCursorOrder() {
        require(orderByItems.isNotEmpty()) { "Cursor pagination requires orderBy()." }
        requireCursorShape()

        val orderedFieldItems = orderByItems.map { item ->
            require(item is SelectOrderItem.FieldItem) { "Cursor pagination requires field-based orderBy items." }
            item
        }
        val keyCandidates = stableSourceKeyCandidates()
        val orderedKeys = orderedFieldItems.map { it.field.cursorColumnKey() }.toSet()
        val stableKey = keyCandidates.firstOrNull { key ->
            key.all { it.cursorColumnKey() in orderedKeys }
        }

        if (stableKey == null) {
            val tieBreaker = requireNotNull(keyCandidates.firstOrNull()) {
                "Cursor pagination requires a primary key or unique key tie-breaker."
            }
            val missingTieBreakers = tieBreaker.filterNot { it.cursorColumnKey() in orderedKeys }
            if (missingTieBreakers.isNotEmpty()) {
                orderByItems = orderByItems + missingTieBreakers.map { field ->
                    SelectOrderItem.FieldItem(field, SqlOrdering.Asc)
                }
            }
        }

        val usedOutputNames = projectionOutputNames().toMutableSet()
        cursorOnlySelectFields = orderByItems
            .filterIsInstance<SelectOrderItem.FieldItem>()
            .map { it.field }
            .distinctBy { it.cursorColumnKey() }
            .filterNot(::isCursorFieldReadableFromRows)
            .map { field -> field to allocateCursorOnlyAlias(field, usedOutputNames) }
    }

    fun cursorValueLabel(field: Field): String =
        cursorOnlySelectFields.firstOrNull { (cursorField, _) ->
            cursorField.cursorColumnKey() == field.cursorColumnKey()
        }?.second
            ?: selectedFieldsByOutputName.entries.firstOrNull { (_, selected) ->
                selected.cursorColumnKey() == field.cursorColumnKey()
            }?.key
            ?: field.name

    fun outputStableKeyCandidates(): List<List<String>> =
        stableSourceKeyCandidates().mapNotNull { key ->
            key.map { field -> outputLabelForStableField(field) }
                .takeIf { labels -> labels.all { it != null } }
                ?.filterNotNull()
                ?.takeIf { labels -> labels.isNotEmpty() }
        }.distinct()

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
        where = and(where, mergeParameters(bindNullableExpr(expr), parameters))
    }

    fun andHaving(expr: SqlExpr?, parameters: Map<String, Any?> = emptyMap()) {
        having = and(having, mergeParameters(bindNullableExpr(expr), parameters))
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

    private fun bindNullableExpr(expr: SqlExpr?): SqlExpr? =
        expr?.let(::bindExpr)

    private fun stableSourceKeyCandidates(): List<List<Field>> {
        sourceQuery?.let { query ->
            return query.outputStableKeyCandidates().mapNotNull { key ->
                key.map { outputName -> derivedSourceField(outputName) }
                    .takeIf { fields -> fields.all { it != null } }
                    ?.filterNotNull()
                    ?.takeIf { fields -> fields.isNotEmpty() }
            }.distinctBy { key -> key.joinToString("|") { it.cursorColumnKey() } }
        }

        val candidates = buildList {
            primaryKey?.let { add(listOf(sourceBinding.bindField(it))) }
            tableIndexes
                .filter { it.type.equals("UNIQUE", ignoreCase = true) || it.method.equals("UNIQUE", ignoreCase = true) }
                .mapNotNull { index ->
                    index.columns
                        .map { column -> allColumns.fieldByNameOrColumn(column) }
                        .takeIf { fields -> fields.all { it != null } }
                        ?.filterNotNull()
                        ?.map(sourceBinding::bindField)
                        ?.takeIf { fields -> fields.isNotEmpty() && fields.all { !it.nullable } }
                }
                .forEach(::add)
        }
        return candidates.distinctBy { key -> key.joinToString("|") { it.cursorColumnKey() } }
    }

    private fun derivedSourceField(outputName: String): Field? =
        allColumns.firstOrNull { field -> sourceColumnName(field) == outputName }

    private fun outputLabelForStableField(field: Field): String? {
        if (selectAll) return field.name.ifBlank { field.columnName }
        return selectedFieldsByOutputName.entries.firstOrNull { (_, selected) ->
            selected.cursorColumnKey() == field.cursorColumnKey()
        }?.key
    }

    private fun List<Field>.fieldByNameOrColumn(name: String): Field? =
        firstOrNull { it.name == name || it.columnName == name }

    private fun isCursorFieldReadableFromRows(field: Field): Boolean =
        selectAll || selectedFieldsByOutputName.values.any { selected ->
            selected.cursorColumnKey() == field.cursorColumnKey()
        }

    private fun requireCursorShape() {
        require(!distinct) { "Cursor pagination does not support DISTINCT queries." }
        require(groupByItems.isEmpty() && having == null) {
            "Cursor pagination does not support GROUP BY or HAVING queries."
        }
        require(projectionItems.none { it.isAggregateProjection() }) {
            "Cursor pagination does not support aggregate projections."
        }
    }

    private fun projectionOutputNames(): Set<String> {
        if (selectAll) return allColumns.mapTo(linkedSetOf()) { it.name.ifBlank { it.columnName } }
        return projectionItems.mapNotNullTo(linkedSetOf()) { item ->
            when (item) {
                is KTableForSelect.ProjectionItem.FieldItem ->
                    item.outputName ?: item.field.name.ifBlank { item.field.columnName }
                is KTableForSelect.ProjectionItem.ScalarSubqueryValue -> item.alias
                is KTableForSelect.ProjectionItem.SelectItemValue ->
                    (item.item as? SqlSelectItem.Expr)?.let { selectItem ->
                        selectItem.alias ?: selectItem.metadata?.outputName
                    }
            }
        }
    }

    private fun allocateCursorOnlyAlias(field: Field, usedOutputNames: MutableSet<String>): String {
        val base = "__kronos_cursor_${field.name.ifBlank { field.columnName }}"
        var label = base
        var suffix = 0
        while (!usedOutputNames.reserveIgnoreCase(label)) {
            label = "${base}_${++suffix}"
        }
        return label
    }

    private fun MutableSet<String>.reserveIgnoreCase(name: String): Boolean {
        if (any { existing -> existing.equals(name, ignoreCase = true) }) return false
        return add(name)
    }

    private fun KTableForSelect.ProjectionItem.isAggregateProjection(): Boolean =
        this is KTableForSelect.ProjectionItem.SelectItemValue &&
            (item as? SqlSelectItem.Expr)?.metadata?.scope == SqlSelectItemSourceScope.Aggregate

    private fun Field.cursorColumnKey(): String =
        if (sourceQuery != null) {
            "${sourceTableAlias ?: "q"}.${sourceColumnName(this)}"
        } else {
            "${tableName.ifBlank { this@SelectContext.tableName }}.$columnName"
        }

    private companion object {
        val parameterSuffixRegex = Regex("""^(.+)@(\d+)$""")
    }
}

private fun List<KTableForSelect.ProjectionItem>.fieldItemsByOutputName(): Map<String, Field> =
    mapNotNull { item ->
        val fieldItem = item as? KTableForSelect.ProjectionItem.FieldItem ?: return@mapNotNull null
        val outputName = fieldItem.outputName ?: fieldItem.field.name.ifBlank { fieldItem.field.columnName }
        outputName to fieldItem.field
    }.toMap(linkedMapOf())

internal sealed class SelectOrderItem {
    abstract val ordering: SqlOrdering

    data class FieldItem(
        val field: Field,
        override val ordering: SqlOrdering,
        val expr: SqlExpr.Column? = null
    ) : SelectOrderItem()

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
