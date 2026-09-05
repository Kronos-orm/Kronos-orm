/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.orm.pagination

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.database.renderPreparedForCore
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.utils.DataSourceUtil.orDefault

data class PageResult<T>(
    val total: Int,
    val records: List<T>,
    val totalPages: Int,
    val pageIndex: Int,
    val pageSize: Int
)

data class CursorResult<T>(
    val hasNext: Boolean,
    val nextCursor: Cursor?,
    val records: List<T>
)

data class TotalPageTasks(
    val countTask: KronosQueryTask,
    val recordsTask: KronosQueryTask
)

class OffsetPageQuery<Selected : KPojo> internal constructor(
    private val query: KSelectable<Selected>,
    @PublishedApi
    internal val pageIndex: Int,
    @PublishedApi
    internal val pageSize: Int
) : KSelectable<Selected>(query.pojo) {
    @PublishedApi
    internal override val selectedType = query.selectedType

    override val nullableSelectedType = query.nullableSelectedType

    override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask =
        query.build(wrapper.orDefault())

    internal override fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper?): SqlQueryPlan =
        query.toSqlQueryPlan(wrapper.orDefault())

    internal override fun buildTotalCountTask(wrapper: KronosDataSourceWrapper?): KronosQueryTask =
        query.buildTotalCountTask(wrapper.orDefault())

    internal override fun outputStableKeyCandidates(): List<List<String>> =
        query.outputStableKeyCandidates()

    fun withTotal(): TotalPageQuery<Selected> = TotalPageQuery(this)

    fun toMapList(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any?>> {
        val dataSource = wrapper.orDefault()
        return build(dataSource).toMapList(dataSource)
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.LowPriorityInOverloadResolution
    inline fun <reified T> toList(wrapper: KronosDataSourceWrapper? = null): List<T> {
        val dataSource = wrapper.orDefault()
        return build(dataSource).toList(dataSource)
    }

    @JvmName("toProjectionList")
    @Suppress("UNCHECKED_CAST")
    fun toList(wrapper: KronosDataSourceWrapper? = null): List<Selected> {
        val dataSource = wrapper.orDefault()
        return build(dataSource).toList(dataSource, selectedType) as List<Selected>
    }
}

class TotalPageQuery<Selected : KPojo> internal constructor(
    @PublishedApi
    internal val pageQuery: OffsetPageQuery<Selected>
) {
    fun build(wrapper: KronosDataSourceWrapper? = null): TotalPageTasks {
        val dataSource = wrapper.orDefault()
        val recordsTask = pageQuery.build(dataSource)
        val countTask = pageQuery.buildTotalCountTask(dataSource)
        val innerQuery = requireNotNull(countTask.atomicTask.statement) {
            "Total-page count tasks must retain their SQL query AST."
        }
        val countQuery = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(SqlExpr.CountAsteriskFunc())),
            from = listOf(
                SqlTable.Subquery(
                    query = innerQuery,
                    alias = SqlTableAlias("total_count")
                )
            )
        )
        val rendered = countQuery.renderPreparedForCore(dataSource, countTask.atomicTask.paramMap)
        val finalCountTask = KronosQueryTask(
            countTask.atomicTask.copy(
                sql = rendered.sql,
                paramMap = rendered.parameters,
                statement = countQuery,
                listParameterOccurrences = rendered.listParameterOccurrences
            )
        )
        return TotalPageTasks(finalCountTask, recordsTask)
    }

    fun toMapList(wrapper: KronosDataSourceWrapper? = null): PageResult<Map<String, Any?>> {
        val dataSource = wrapper.orDefault()
        val tasks = build(dataSource)
        val total = tasks.countTask.first<Int>(dataSource)
        return PageResult(
            total = total,
            records = tasks.recordsTask.toMapList(dataSource),
            totalPages = totalPages(total, pageQuery.pageSize),
            pageIndex = pageQuery.pageIndex,
            pageSize = pageQuery.pageSize
        )
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.LowPriorityInOverloadResolution
    inline fun <reified T> toList(wrapper: KronosDataSourceWrapper? = null): PageResult<T> {
        val dataSource = wrapper.orDefault()
        val tasks = build(dataSource)
        val total = tasks.countTask.first<Int>(dataSource)
        return PageResult(
            total = total,
            records = tasks.recordsTask.toList<T>(dataSource),
            totalPages = totalPages(total, pageQuery.pageSize),
            pageIndex = pageQuery.pageIndex,
            pageSize = pageQuery.pageSize
        )
    }

    @JvmName("toProjectionList")
    @Suppress("UNCHECKED_CAST")
    fun toList(wrapper: KronosDataSourceWrapper? = null): PageResult<Selected> {
        val dataSource = wrapper.orDefault()
        val tasks = build(dataSource)
        val total = tasks.countTask.first<Int>(dataSource)
        val records = tasks.recordsTask.toList(dataSource, pageQuery.selectedType) as List<Selected>
        return PageResult(
            total = total,
            records = records,
            totalPages = totalPages(total, pageQuery.pageSize),
            pageIndex = pageQuery.pageIndex,
            pageSize = pageQuery.pageSize
        )
    }

    companion object {
        @PublishedApi
        internal fun totalPages(total: Int, pageSize: Int): Int =
            if (total <= 0) 0 else ((total.toLong() + pageSize - 1L) / pageSize).toInt()
    }
}

internal data class CursorPageField(
    val name: String,
    val resultLabel: String,
    val hidden: Boolean,
    val key: String = name
)

class CursorPageQuery<Selected : KPojo> internal constructor(
    private val query: KSelectable<Selected>,
    private val pageSize: Int,
    private val fields: List<CursorPageField>
) {
    @PublishedApi
    internal val selectedType = query.selectedType

    fun build(wrapper: KronosDataSourceWrapper? = null): KronosQueryTask =
        query.build(wrapper.orDefault())

    fun toSqlQuery(wrapper: KronosDataSourceWrapper? = null): SqlQuery =
        query.toSqlQuery(wrapper.orDefault())

    fun toMapList(wrapper: KronosDataSourceWrapper? = null): CursorResult<Map<String, Any?>> {
        val dataSource = wrapper.orDefault()
        val rows = build(dataSource).toMapList(dataSource)
        val result = pageResult(rows) { row, fieldName -> row.valueCaseInsensitive(fieldName) }
        return result.copy(records = result.records.map(::stripCursorOnlyValues))
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.LowPriorityInOverloadResolution
    inline fun <reified T> toList(wrapper: KronosDataSourceWrapper? = null): CursorResult<T> {
        val dataSource = wrapper.orDefault()
        requireCursorFieldsVisibleForTypedRows()
        val rows = build(dataSource).toList<T>(dataSource)
        return pageResult(rows) { row, fieldName ->
            when (row) {
                is KPojo -> row[fieldName]
                is Map<*, *> -> row.valueCaseInsensitive(fieldName)
                else -> null
            }
        }
    }

    @JvmName("toProjectionList")
    @Suppress("UNCHECKED_CAST")
    fun toList(wrapper: KronosDataSourceWrapper? = null): CursorResult<Selected> {
        val dataSource = wrapper.orDefault()
        requireCursorFieldsVisibleForTypedRows()
        val rows = build(dataSource).toList(dataSource, selectedType) as List<Selected>
        return pageResult(rows) { row, fieldName -> row[fieldName] }
    }

    @PublishedApi
    internal fun <T> pageResult(
        rows: List<T>,
        valueOf: (T, String) -> Any?
    ): CursorResult<T> {
        val hasNext = rows.size > pageSize
        val records = if (hasNext) rows.take(pageSize) else rows
        val nextCursor = if (hasNext && records.isNotEmpty()) {
            fields.associate { field ->
                val value = valueOf(records.last(), field.resultLabel)
                require(value != null) {
                    "Cursor pagination requires selected orderBy field '${field.name}' in result rows."
                }
                field.key to value
            }.toCursor()
        } else {
            null
        }
        return CursorResult(hasNext, nextCursor, records)
    }

    @PublishedApi
    internal fun requireCursorFieldsVisibleForTypedRows() {
        val hiddenField = fields.firstOrNull { it.hidden } ?: return
        require(false) {
            "Cursor pagination requires selected orderBy field '${hiddenField.name}' in result rows."
        }
    }

    @PublishedApi
    internal fun Map<*, *>.valueCaseInsensitive(fieldName: String): Any? =
        this[fieldName] ?: entries.firstOrNull { (key, _) ->
            key is String && key.equals(fieldName, ignoreCase = true)
        }?.value

    private fun stripCursorOnlyValues(row: Map<String, Any?>): Map<String, Any?> {
        val hiddenLabels = fields.filter { it.hidden }.map { it.resultLabel }
        if (hiddenLabels.isEmpty()) return row
        return row.filterKeys { key -> hiddenLabels.none { it.equals(key, ignoreCase = true) } }
    }
}

internal fun checkedPageOffset(pageIndex: Int, pageSize: Int): Int {
    val offset = (pageIndex.toLong() - 1L) * pageSize
    require(offset <= Int.MAX_VALUE) { "Page offset exceeds the supported Int range." }
    return offset.toInt()
}

internal fun checkedCursorFetchSize(pageSize: Int): Int {
    require(pageSize < Int.MAX_VALUE) { "Cursor page size exceeds the supported Int range." }
    return pageSize + 1
}
