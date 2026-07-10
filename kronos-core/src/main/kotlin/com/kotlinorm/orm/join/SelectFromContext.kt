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
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.utils.LinkedHashSet
import kotlin.reflect.KClass
import kotlin.reflect.KType

internal class SelectFromContext<T1 : KPojo, Selected : KPojo, Context : KPojo>(
    val root: T1
) {
    private val rootKClass = root.kClass()

    lateinit var projectionType: KType
    lateinit var nullableProjectionType: KType
    var receiverPojo: KPojo = root

    var tableName: String = root.__tableName
    var paramMap: MutableMap<String, Any?> = root.toDataMap().toMutableMap()
    var logicDeleteStrategy = rootLogicDeleteStrategy()
    var allFields = rootColumns()
    var listOfPojo: MutableList<Pair<KClass<KPojo>, KPojo>> = mutableListOf(rootKClass to root)

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
    var limitCapacity: Int = 0
    var cascadeEnabled: Boolean = true
    var cascadeAllowed: Set<Field>? = null
    var cascadeSelectedProps: Set<Field>? = null
    var pageIndex: Int = 0
    var pageSize: Int = 0
    val databaseOfTable: MutableMap<String, String> = mutableMapOf()
    var operationType: KOperationType = KOperationType.SELECT

    private val keyCounter = KeyCounter()

    fun setSources(vararg sources: KPojo) {
        tableName = root.__tableName
        paramMap = linkedMapOf<String, Any?>().also { values ->
            sources.forEach { values.putAll(it.toDataMap()) }
        }
        listOfPojo = sources.map { it.kClass() to it }.toMutableList()
        logicDeleteStrategy = rootLogicDeleteStrategy()
        allFields = rootColumns()
    }

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
        val fields = listOfPojo.flatMap { it.second.kronosColumns() }
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

    private fun rootColumns(): List<Field> =
        root.kronosColumns().filter { it.isColumn }

    private fun rootLogicDeleteStrategy() =
        root.kronosLogicDelete().takeIf { strategy -> strategy.enabled }?.bind(root.__tableName)?.takeIf {
            strategy -> rootColumns().any { it.name == strategy.field.name }
        }

    private data class KeyCounter(
        val metaOfMap: MutableMap<String, MutableMap<Int, Any?>> = mutableMapOf()
    )
}
