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
import com.kotlinorm.beans.dsl.withUniqueOutputNames
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.pagination.CursorSpec
import com.kotlinorm.orm.sql.renameNamedParameters
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.statement.SqlLock
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.utils.KPojoRuntimeMetadata
import com.kotlinorm.utils.KTypeKey
import com.kotlinorm.utils.LinkedHashSet
import com.kotlinorm.utils.createKPojo
import com.kotlinorm.utils.resolveRuntimeMetadata
import com.kotlinorm.utils.toLinkedSet
import java.util.IdentityHashMap
import kotlin.reflect.KType

internal class JoinedSelectContext<Source : KPojo, Selected : KPojo, Context : KPojo>(
    val state: JoinSourceState<Source>,
    val receiverPojo: Context,
    val projectionType: KType,
    val nullableProjectionType: KType
) {
    val root: Source = state.root
    private val rootMetadata = root.resolveRuntimeMetadata()
    private val metadataBySource = IdentityHashMap<KPojo, KPojoRuntimeMetadata>().apply {
        state.sources.forEach { source -> put(source, source.resolveRuntimeMetadata()) }
    }
    private val derivedAliases = IdentityHashMap<KPojo, String>().apply {
        state.leaves.filter { it.query != null }.forEachIndexed { index, leaf ->
            put(leaf.pojo, if (index == 0) "q" else "q_${index + 1}")
        }
    }
    /**
     * Qualifiers emitted while this query's DSL blocks are captured. A nested
     * correlated query can allocate an alias for an outer source later, so the
     * qualifier in an already captured projection/condition/order expression
     * must remain available as the replacement key.
     */
    private val capturedQualifiers = IdentityHashMap<KPojo, String>().apply {
        state.leaves.forEach { leaf -> put(leaf.pojo, state.qualifierFor(leaf.pojo)) }
    }

    val tableName: String = rootMetadata.tableName
    val listOfPojo: List<Pair<KType, KPojo>> = state.sources.map { source ->
        metadataBySource.getValue(source).kType to source
    }
    val allFields: List<Field> = rootMetadata.allColumns
    val allSourceFields: List<Field> = state.sources.flatMap { source ->
        metadataBySource.getValue(source).allFields
    }
    val rootLogicDeleteStrategy = rootMetadata.logicDeleteStrategy.takeIf {
        state.current.rootLeaf().query == null
    }

    val sourceValues: MutableMap<String, Any?> = linkedMapOf<String, Any?>().also { values ->
        state.sources.forEach { values.putAll(it.toDataMap()) }
    }
    val patchValues: MutableMap<String, Any?> = linkedMapOf()
    val parameterValues: MutableMap<String, Any?> = linkedMapOf()
    val parameterFields: MutableMap<String, Field> = linkedMapOf()
    private val parameterNameCounter = mutableMapOf<String, Int>()

    var where: SqlExpr? = null
    var having: SqlExpr? = null
    var selectedFields: LinkedHashSet<Field> = []
    var selectAll: Boolean = true
    var selectedFieldsByOutputName: Map<String, Field> = emptyMap()
    var projectionItems: List<KTableForSelect.ProjectionItem> = emptyList()
    var groupByFields: LinkedHashSet<Field> = []
    var orderByItems: List<KTableForSort.SortItem> = emptyList()
    var distinct: Boolean = false
    var limit: SqlLimit? = null
    var lock: SqlLock? = null
    var cascadeEnabled: Boolean = true
    var cascadeAllowed: Set<Field>? = null
    var cascadeSelectedProps: Set<Field>? = null
    var cursorSpec: CursorSpec? = null
    var cursorOnlySelectFields: List<Pair<Field, String>> = emptyList()
        private set
    var operationType: KOperationType = KOperationType.SELECT

    private val databaseBySource = IdentityHashMap<KPojo, String>()

    fun <T> withSourceScope(block: () -> T): T = state.withSourceScope(block)

    fun setDatabase(source: KPojo, databaseName: String) {
        databaseBySource[source] = databaseName
    }

    fun databaseFor(source: KPojo): String? = databaseBySource[source]

    fun metadataFor(source: KPojo): KPojoRuntimeMetadata = metadataBySource.getValue(source)

    fun derivedAliasFor(source: KPojo): String? = derivedAliases[source]

    fun derivedAliasForUnqualifiedColumn(columnName: String): String? =
        state.sources.singleOrNull { source ->
            metadataFor(source).allFields.any { candidate ->
                candidate.name.ifBlank { candidate.columnName } == columnName
            }
        }?.let(::derivedAliasFor)

    fun sourceAliasFor(source: KPojo): String? =
        derivedAliasFor(source) ?: state.qualifierFor(source).takeUnless {
            it == metadataFor(source).tableName
        }

    fun qualifierFor(source: KPojo): String {
        derivedAliasFor(source)?.let { return it }
        val qualifier = state.qualifierFor(source)
        val tableName = metadataFor(source).tableName
        val database = databaseFor(source)
        return if (database != null && qualifier == tableName) "$database.$tableName" else qualifier
    }

    fun qualifierFor(tableNameOrAlias: String): String {
        state.sources.firstOrNull { qualifierFor(it) == tableNameOrAlias }?.let { return tableNameOrAlias }
        val matches = sourcesMatchingQualifier(tableNameOrAlias)
        return matches.singleOrNull()?.let(::qualifierFor) ?: tableNameOrAlias
    }

    fun qualifierIdentifierFor(tableNameOrAlias: String): SqlIdentifier {
        val source = state.sources.firstOrNull { qualifierFor(it) == tableNameOrAlias }
            ?: sourcesMatchingQualifier(tableNameOrAlias).singleOrNull()
            ?: return SqlIdentifier.of(tableNameOrAlias)
        val database = databaseFor(source)
        val tableName = metadataFor(source).tableName
        return if (database != null && derivedAliasFor(source) == null && state.qualifierFor(source) == tableName) {
            SqlIdentifier.of(database, tableName)
        } else {
            SqlIdentifier.of(qualifierFor(source))
        }
    }

    fun conditionExpression(snapshot: JoinConditionSnapshot): SqlExpr {
        val replacements = snapshot.qualifiers
            .map { captured -> captured.qualifier to qualifierFor(captured.source) }
            .filter { (captured, resolved) -> captured != resolved }
            .groupBy({ it.first }, { it.second })
            .mapNotNull { (captured, resolved) ->
                resolved.distinct().singleOrNull()?.let { captured to it }
            }
            .toMap()
        return snapshot.expression.rewriteJoinQualifiers(replacements, ::qualifierIdentifierFor)
    }

    fun runtimeQualifierReplacements(): Map<String, String> =
        (state.leaves.mapNotNull { leaf ->
            val captured = capturedQualifiers[leaf.pojo] ?: return@mapNotNull null
            val resolved = qualifierFor(leaf.pojo)
            (captured to resolved).takeIf { captured != resolved }
        } + declaredQualifierReplacements()).groupBy({ it.first }, { it.second })
            .mapNotNull { (captured, resolved) ->
                resolved.distinct().singleOrNull()?.let { captured to it }
            }
            .toMap()

    private fun sourcesMatchingQualifier(tableNameOrAlias: String): List<KPojo> =
        state.sources.filter { source ->
            val metadata = metadataFor(source)
            metadata.tableName == tableNameOrAlias ||
                source.__tableName == tableNameOrAlias ||
                metadata.allFields.any { field -> field.tableName == tableNameOrAlias }
        }

    /**
     * Rewrite qualifiers emitted from generated Fields (which use the
     * declaration table name) to the qualifier of the actual source leaf.
     * Ambiguous self-joins are deliberately omitted; their identity aliases
     * are captured by SourceIdentityScope and remain authoritative.
     */
    private fun declaredQualifierReplacements(): List<Pair<String, String>> =
        state.sources.flatMap { source ->
            val resolved = qualifierFor(source)
            metadataFor(source).allFields
                .asSequence()
                .map { it.tableName }
                .filter { it.isNotBlank() && it != resolved }
                .distinct()
                .map { it to resolved }
                .toList()
        }

    fun registerProjectionItems(
        items: List<KTableForSelect.ProjectionItem>,
        fields: Iterable<Field>
    ) {
        val projectedFields = fields.toList().toLinkedSet()
        val cascadeLocalFields = projectedFields
            .asSequence()
            .filterNot(Field::isColumn)
            .flatMap(::cascadeLocalFieldsFor)
            .filterNot { local -> projectedFields.any { it.name == local.name && it.tableName == local.tableName } }
            .toList()
        selectedFields = (projectedFields + cascadeLocalFields).toLinkedSet()
        val hiddenLocalItems = cascadeLocalFields.map { field ->
            KTableForSelect.ProjectionItem.FieldItem(field)
        }
        projectionItems = (items + hiddenLocalItems).withUniqueOutputNames()
        selectedFieldsByOutputName = projectionItems.mapNotNull { item ->
            val fieldItem = item as? KTableForSelect.ProjectionItem.FieldItem ?: return@mapNotNull null
            val outputName = fieldItem.outputName ?: fieldItem.field.name.ifBlank { fieldItem.field.columnName }
            outputName to fieldItem.field
        }.toMap(linkedMapOf())
        selectAll = false
    }

    /**
     * Returns the source-table columns required to execute a projected cascade relation.
     * The relation itself is a result property, never a physical SQL column.
     */
    private fun cascadeLocalFieldsFor(field: Field): List<Field> {
        val source = state.sources.firstOrNull { source ->
            metadataFor(source).tableName == field.tableName || qualifierFor(source) == field.tableName
        } ?: return emptyList()
        val metadata = metadataFor(source)
        val direct = field.cascade?.properties.orEmpty().asSequence()
        val reverse = (field.elementKType ?: field.kType)
            ?.let { relationType ->
                val relationFields = relationType.kPojoInstanceOrNull()?.resolveRuntimeMetadata()?.allFields.orEmpty()
                relationFields.asSequence()
                    .filter { relation ->
                        relation.cascade != null &&
                            relation.cascade.targetProperties.all { target ->
                                metadata.allFields.any { sourceField -> sourceField.name == target }
                            } &&
                            (relation.elementKType ?: relation.kType)?.sameDeclaredTypeAs(source.__kType) == true
                    }
                    .flatMap { relation -> relation.cascade?.targetProperties.orEmpty().asSequence() }
            }
            .orEmpty()
        return (direct + reverse)
            .distinct()
            .mapNotNull { name -> metadata.allFields.firstOrNull { it.name == name } }
            .toList()
    }

    private fun KType.kPojoInstanceOrNull(): KPojo? =
        runCatching { createKPojo(this) }.getOrNull()

    private fun KType.sameDeclaredTypeAs(other: KType): Boolean =
        KTypeKey.from(this, ignoreTopLevelNullability = true) ==
            KTypeKey.from(other, ignoreTopLevelNullability = true)

    fun fieldsMap(): Map<String, Field> =
        allSourceFields.associateBy { it.name } + allSourceFields.associateBy { it.columnName }

    fun sourceValue(field: Field): Any? {
        val source = state.sources.firstOrNull { source -> qualifierFor(source) == field.tableName }
            ?: sourcesMatchingQualifier(field.tableName).singleOrNull()
            ?: root
        return source.toDataMap()[field.name]
    }

    fun andWhere(
        expr: SqlExpr?,
        parameters: Map<String, Any?> = emptyMap(),
        fields: Map<String, Field> = emptyMap()
    ) {
        where = and(where, mergeParameters(expr, parameters, fields))
    }

    fun andHaving(
        expr: SqlExpr?,
        parameters: Map<String, Any?> = emptyMap(),
        fields: Map<String, Field> = emptyMap()
    ) {
        having = and(having, mergeParameters(expr, parameters, fields))
    }

    fun bindParameter(name: String, value: Any?, field: Field? = null): String {
        val uniqueName = uniqueParameterName(name)
        parameterValues[uniqueName] = value
        field?.let { parameterFields[uniqueName] = it }
        return uniqueName
    }

    fun and(left: SqlExpr?, right: SqlExpr?): SqlExpr? =
        when {
            left == null -> right
            right == null -> left
            else -> SqlExpr.Binary(left, SqlBinaryOperator.And, right)
        }

    fun andAll(expressions: Iterable<SqlExpr>): SqlExpr? {
        val items = expressions.toList()
        return items.drop(1).fold(items.firstOrNull()) { left, right -> and(left, right) }
    }

    fun prepareCursorOrder() {
        require(orderByItems.isNotEmpty()) { "Cursor pagination requires orderBy()." }
        requireCursorShape()
        state.current.requireCursorSafeSourceTree()

        val cursorOrderItems = orderByItems.map { item ->
            require(item is KTableForSort.SortItem.FieldItem) {
                "Cursor pagination requires field-based orderBy items."
            }
            item.normalizeCursorSelectedField()
        }
        orderByItems = cursorOrderItems
        val orderedFields = cursorOrderItems.mapTo(mutableListOf()) { it.field }
        val orderedKeys = orderedFields.mapTo(mutableSetOf()) { it.resolvedCursorKey() }
        state.leaves.forEach { leaf ->
            val candidates = stableCursorKeyCandidates(leaf.pojo)
            val tieBreaker = candidates.firstOrNull { candidate ->
                candidate.all { it.resolvedCursorKey() in orderedKeys }
            } ?: candidates.firstOrNull() ?: throw IllegalArgumentException(
                "Cursor pagination requires every JOIN base source to define a primary key or non-null unique key; " +
                    "source '${metadataFor(leaf.pojo).tableName}' has no stable key."
            )
            tieBreaker.filterNot { it.resolvedCursorKey() in orderedKeys }.forEach { field ->
                orderByItems = orderByItems + KTableForSort.SortItem.FieldItem(
                    field = field,
                    ordering = SqlOrdering.Asc,
                    expr = field.cursorOrderExpr()
                )
                orderedFields += field
                orderedKeys += field.resolvedCursorKey()
            }
        }
        val usedOutputNames = projectionOutputNames().toMutableSet()
        cursorOnlySelectFields = orderByItems
            .filterIsInstance<KTableForSort.SortItem.FieldItem>()
            .map { it.field }
            .distinctBy { field -> cursorKey(field) }
            .filterNot(::isCursorFieldVisible)
            .map { field ->
                val base = "__kronos_cursor_${field.name.ifBlank { field.columnName }}"
                var label = base
                var suffix = 0
                while (!usedOutputNames.reserveIgnoreCase(label)) {
                    label = "${base}_${++suffix}"
                }
                field to label
            }
    }

    fun cursorValueLabel(field: Field): String =
        cursorOnlySelectFields.firstOrNull { (cursorField, _) ->
            cursorField.resolvedCursorKey() == field.resolvedCursorKey()
        }
            ?.second
            ?: selectedFieldsByOutputName.entries.firstOrNull { (_, selected) ->
                selected.resolvedCursorKey() == field.resolvedCursorKey()
            }?.key
            ?: field.name

    fun copyStateFrom(source: JoinedSelectContext<Source, Selected, Context>) {
        capturedQualifiers.clear()
        capturedQualifiers.putAll(source.capturedQualifiers)
        sourceValues.clear()
        sourceValues.putAll(source.sourceValues)
        patchValues.clear()
        patchValues.putAll(source.patchValues)
        parameterValues.clear()
        parameterValues.putAll(source.parameterValues)
        parameterFields.clear()
        parameterFields.putAll(source.parameterFields)
        parameterNameCounter.clear()
        parameterNameCounter.putAll(source.parameterNameCounter)
        source.databaseBySource.forEach { (key, value) -> databaseBySource[key] = value }
        where = source.where
        having = source.having
        selectedFields = source.selectedFields.toLinkedSet()
        selectAll = source.selectAll
        selectedFieldsByOutputName = source.selectedFieldsByOutputName.toMap(linkedMapOf())
        projectionItems = source.projectionItems.toList()
        groupByFields = source.groupByFields.toLinkedSet()
        orderByItems = source.orderByItems.toList()
        distinct = source.distinct
        limit = source.limit
        lock = source.lock
        cascadeEnabled = source.cascadeEnabled
        cascadeAllowed = source.cascadeAllowed?.toSet()
        cascadeSelectedProps = source.cascadeSelectedProps?.toSet()
        cursorSpec = source.cursorSpec?.copy(values = source.cursorSpec?.values.orEmpty().toMap())
        cursorOnlySelectFields = source.cursorOnlySelectFields.toList()
        operationType = source.operationType
    }

    private fun isCursorFieldVisible(field: Field): Boolean =
        selectAll || selectedFieldsByOutputName.values.any { selected ->
            selected.resolvedCursorKey() == field.resolvedCursorKey()
        }

    fun cursorKey(field: Field): String = field.resolvedCursorKey()

    private fun projectionOutputNames(): Set<String> {
        if (selectAll) return allFields.mapTo(linkedSetOf()) { it.name.ifBlank { it.columnName } }
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

    private fun requireCursorShape() {
        require(!distinct) { "Cursor pagination does not support DISTINCT queries." }
        require(groupByFields.isEmpty() && having == null) {
            "Cursor pagination does not support GROUP BY or HAVING queries."
        }
        require(projectionItems.none { it.isAggregateProjection() }) {
            "Cursor pagination does not support aggregate projections."
        }
    }

    private fun FromSourceNode.requireCursorSafeSourceTree() {
        when (this) {
            is FromSourceNode.Leaf -> require(source.query == null) {
                "Cursor pagination does not support JOIN derived or union sources because row uniqueness cannot be proven."
            }
            is FromSourceNode.Join -> {
                when (joinType) {
                    SqlJoinType.Inner,
                    SqlJoinType.Cross -> Unit
                    SqlJoinType.Left,
                    SqlJoinType.Right,
                    SqlJoinType.Full -> throw IllegalArgumentException(
                        "Cursor pagination does not support outer JOIN sources because row uniqueness cannot be proven."
                    )
                    is SqlJoinType.UnsafeCustom -> throw IllegalArgumentException(
                        "Cursor pagination does not support custom JOIN sources because row uniqueness cannot be proven."
                    )
                }
                left.requireCursorSafeSourceTree()
                right.requireCursorSafeSourceTree()
            }
        }
    }

    private fun stableCursorKeyCandidates(source: KPojo): List<List<Field>> {
        val metadata = metadataFor(source)
        val candidates = buildList {
            metadata.primaryKey?.let { add(listOf(it.bindCursorSource(source))) }
            metadata.tableIndexes
                .filter { it.type.equals("UNIQUE", ignoreCase = true) || it.method.equals("UNIQUE", ignoreCase = true) }
                .mapNotNull { index ->
                    index.columns
                        .map { column -> metadata.allColumns.fieldByNameOrColumn(column) }
                        .takeIf { fields -> fields.all { it != null } }
                        ?.filterNotNull()
                        ?.takeIf { fields -> fields.isNotEmpty() && fields.all { !it.nullable } }
                        ?.map { it.bindCursorSource(source) }
                }
                .forEach(::add)
        }
        return candidates.distinctBy { key -> key.joinToString("|") { it.resolvedCursorKey() } }
    }

    private fun Field.bindCursorSource(source: KPojo): Field =
        copy(tableName = qualifierFor(source))

    private fun List<Field>.fieldByNameOrColumn(name: String): Field? =
        firstOrNull { it.name == name || it.columnName == name }

    private fun KTableForSelect.ProjectionItem.isAggregateProjection(): Boolean =
        this is KTableForSelect.ProjectionItem.SelectItemValue &&
            (item as? SqlSelectItem.Expr)?.metadata?.scope == SqlSelectItemSourceScope.Aggregate

    private fun KTableForSort.SortItem.FieldItem.normalizeCursorSelectedField(): KTableForSort.SortItem.FieldItem {
        if (field.tableName.isNotBlank()) return this
        val outputName = field.name.ifBlank { field.columnName }
        val selectedField = selectedFieldsByOutputName[outputName] ?: return this
        return copy(field = selectedField, expr = null)
    }

    private fun Field.cursorOrderExpr(): SqlExpr.Column {
        val table = qualifierFor(tableName)
        return SqlExpr.Column(
            tableName = table,
            columnName = columnName,
            qualifier = qualifierIdentifierFor(table)
        )
    }

    private fun MutableSet<String>.reserveIgnoreCase(name: String): Boolean {
        if (any { existing -> existing.equals(name, ignoreCase = true) }) return false
        return add(name)
    }

    private fun Field.resolvedCursorKey(): String = "${qualifierFor(tableName)}.$columnName"

    private fun mergeParameters(
        expr: SqlExpr?,
        parameters: Map<String, Any?>,
        fields: Map<String, Field>
    ): SqlExpr? {
        if (expr == null || parameters.isEmpty()) return expr
        val renames = linkedMapOf<String, String>()
        parameters.forEach { (name, value) ->
            val uniqueName = bindParameter(name, value, fields[name])
            if (uniqueName != name) renames[name] = uniqueName
        }
        return expr.renameNamedParameters(renames)
    }

    private fun uniqueParameterName(name: String): String {
        val match = parameterSuffixRegex.matchEntire(name)
        val baseName = match?.groupValues?.get(1) ?: name
        val existingSuffix = match?.groupValues?.get(2)?.toIntOrNull()
        if (name !in parameterValues) {
            parameterNameCounter[baseName] = maxOf(parameterNameCounter.getOrDefault(baseName, 0), existingSuffix ?: 0)
            return name
        }
        var count = maxOf(parameterNameCounter.getOrDefault(baseName, 0), existingSuffix ?: 0)
        var candidate: String
        do {
            candidate = "$baseName@${++count}"
        } while (candidate in parameterValues)
        parameterNameCounter[baseName] = count
        return candidate
    }

    private companion object {
        val parameterSuffixRegex = Regex("""^(.+)@(\d+)$""")
    }
}
