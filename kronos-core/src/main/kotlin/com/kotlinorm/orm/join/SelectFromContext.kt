/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KJoinable
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.SourceIdentityScope
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.orm.sql.renameNamedParameters
import com.kotlinorm.utils.LinkedHashSet
import com.kotlinorm.utils.KPojoRuntimeMetadata
import com.kotlinorm.utils.resolveRuntimeMetadata
import kotlin.reflect.KClass
import kotlin.reflect.KType

internal class SelectFromContext<T1 : KPojo, Selected : KPojo, Context : KPojo>(
    val root: T1
) {
    private var rootMetadata = root.resolveRuntimeMetadata()

    lateinit var projectionType: KType
    lateinit var nullableProjectionType: KType
    var receiverPojo: KPojo = root

    var tableName: String = root.__tableName
    var paramMap: MutableMap<String, Any?> = root.toDataMap().toMutableMap()
    var logicDeleteStrategy = rootLogicDeleteStrategy()
    var allFields = rootColumns()
    var listOfPojo: MutableList<Pair<KClass<out KPojo>, KPojo>> = mutableListOf(rootMetadata.kClass to root)
    private var metadataByPojo: MutableMap<KPojo, KPojoRuntimeMetadata> = mutableMapOf(root to rootMetadata)
    private var sourceIdentityFrame = SourceIdentityScope.frame(listOf(root))

    var where: SqlExpr? = null
    var having: SqlExpr? = null
    var selectedFields: LinkedHashSet<Field> = []
    var selectAll: Boolean = false
    val selectedFieldsByAlias: MutableMap<String, Field> = mutableMapOf()
    val projectionItems: MutableList<KTableForSelect.ProjectionItem> = mutableListOf()
    val joinables: MutableList<KJoinable> = mutableListOf()
    val derivedJoinQueries: MutableMap<String, Pair<KSelectable<*>, String>> = mutableMapOf()
    val derivedJoinAliasOverrides: MutableMap<String, String> = mutableMapOf()
    var groupByFields: LinkedHashSet<Field> = []
    var orderByItems: List<KTableForSort.SortItem> = emptyList()
    var distinctEnabled: Boolean = false
    var groupEnabled: Boolean = false
    var havingEnabled: Boolean = false
    var orderEnabled: Boolean = false
    var pageEnabled: Boolean = false
    var limitCapacity: Int? = null
    var cascadeEnabled: Boolean = true
    var cascadeAllowed: Set<Field>? = null
    var cascadeSelectedProps: Set<Field>? = null
    var pageIndex: Int = 0
    var pageSize: Int = 0
    val databaseOfTable: MutableMap<String, String> = mutableMapOf()
    var operationType: KOperationType = KOperationType.SELECT

    private val keyCounter = KeyCounter()
    private val boundParameterNames = mutableSetOf<String>()
    private val parameterNameCounter = mutableMapOf<String, Int>()

    fun setSources(vararg sources: KPojo) {
        rootMetadata = root.resolveRuntimeMetadata()
        tableName = rootMetadata.tableName
        paramMap = linkedMapOf<String, Any?>().also { values ->
            sources.forEach { values.putAll(it.toDataMap()) }
        }
        metadataByPojo = sources.associateWith { it.resolveRuntimeMetadata() }.toMutableMap()
        listOfPojo = sources.map { metadataByPojo.getValue(it).kClass to it }.toMutableList()
        logicDeleteStrategy = rootLogicDeleteStrategy()
        allFields = rootColumns()
        sourceIdentityFrame = SourceIdentityScope.frame(sources.toList())
        boundParameterNames.clear()
        parameterNameCounter.clear()
    }

    fun <T> withSourceScope(block: () -> T): T =
        SourceIdentityScope.withFrame(sourceIdentityFrame, block)

    fun sourceAliasFor(tableName: String): String? =
        sourceIdentityFrame.aliasesByTableName()[tableName]

    fun sourceAliasFor(source: KPojo): String? =
        sourceIdentityFrame.aliasForSource(source)

    fun sourceAliasReplacements(): Map<String, String> =
        sourceIdentityFrame.aliasesByTableName()

    fun registerSelectedFields(fields: Iterable<Field>) {
        selectedFields += fields
        fields.forEach { field ->
            selectedFieldsByAlias[safeKey(field.name, field)] = field
        }
    }

    fun andWhere(expr: SqlExpr?) {
        where = and(where, expr)
    }

    fun andHaving(expr: SqlExpr?) {
        having = and(having, expr)
    }

    fun mergeConditionParameters(expr: SqlExpr?, parameters: Map<String, Any?>): SqlExpr? {
        if (expr == null || parameters.isEmpty()) return expr
        val renames = linkedMapOf<String, String>()
        parameters.forEach { (name, value) ->
            val uniqueName = uniqueParameterName(name)
            paramMap[uniqueName] = value
            boundParameterNames += uniqueName
            if (uniqueName != name) renames[name] = uniqueName
        }
        return expr.renameNamedParameters(renames)
    }

    fun and(left: SqlExpr?, right: SqlExpr?): SqlExpr? =
        when {
            left == null -> right
            right == null -> left
            else -> SqlExpr.Binary(left, SqlBinaryOperator.And, right)
        }

    fun andAll(expressions: Iterable<SqlExpr>): SqlExpr? {
        val items = expressions.toList()
        if (items.isEmpty()) return null
        return items.drop(1).fold(items.first()) { left, right ->
            SqlExpr.Binary(left, SqlBinaryOperator.And, right)
        }
    }

    fun fieldsMap(): Map<String, Field> {
        val fields = listOfPojo.flatMap { metadataByPojo.getValue(it.second).allFields }
        return fields.associateBy { it.name } + fields.associateBy { it.columnName }
    }

    private fun safeKey(keyName: String, data: Any?): String {
        val existing = keyCounter.metaOfMap[keyName]?.entries?.firstOrNull { it.value == data }
        if (existing != null) {
            return if (existing.key == 0) keyName else "$keyName@${existing.key}"
        }
        val counter = (keyCounter.metaOfMap[keyName]?.keys?.maxOrNull() ?: -1) + 1
        keyCounter.metaOfMap.getOrPut(keyName) { mutableMapOf() }[counter] = data
        return if (counter == 0) keyName else "$keyName@$counter"
    }

    private fun uniqueParameterName(name: String): String {
        val (baseName, existingSuffix) = splitParameterSuffix(name)
        if (name !in boundParameterNames) {
            parameterNameCounter[baseName] = maxOf(
                parameterNameCounter.getOrDefault(baseName, 0),
                existingSuffix ?: 0
            )
            return name
        }

        var count = maxOf(parameterNameCounter.getOrDefault(baseName, 0), existingSuffix ?: 0)
        var candidate: String
        do {
            count++
            candidate = "$baseName@$count"
        } while (candidate in boundParameterNames)
        parameterNameCounter[baseName] = count
        return candidate
    }

    private fun splitParameterSuffix(name: String): Pair<String, Int?> {
        val match = parameterSuffixRegex.matchEntire(name) ?: return name to null
        return match.groupValues[1] to match.groupValues[2].toInt()
    }

    private fun rootColumns(): List<Field> =
        rootMetadata.allColumns

    private fun rootLogicDeleteStrategy() =
        rootMetadata.logicDeleteStrategy

    private data class KeyCounter(
        val metaOfMap: MutableMap<String, MutableMap<Int, Any?>> = mutableMapOf()
    )

    private companion object {
        val parameterSuffixRegex = Regex("""^(.+)@(\d+)$""")
    }
}
