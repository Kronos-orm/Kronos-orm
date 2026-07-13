package com.kotlinorm.orm.pagination

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.SelectClause
import com.kotlinorm.orm.select.SelectOrderItem

class CursorClause<Source : KPojo, Selected : KPojo, Context : KPojo>(
    @PublishedApi
    internal val selectClause: SelectClause<Source, Selected, Context>
) {
    private var spec: CursorSpec = CursorSpec(cursor = null, offset = 0)

    fun cursor(offset: Int): CursorClause<Source, Selected, Context> = cursor(null, offset)

    fun cursor(cursor: Cursor?, offset: Int): CursorClause<Source, Selected, Context> {
        spec = CursorSpec(cursor, offset)
        selectClause.context.cursorSpec = spec
        selectClause.applyCursorPage(offset)
        return this
    }

    fun toMapList(wrapper: KronosDataSourceWrapper? = null): Triple<Boolean, Cursor?, List<Map<String, Any?>>> {
        val rows = selectClause.build(wrapper).toMapList(wrapper)
        return pageResult(rows) { row, fieldName ->
            row[fieldName]
                ?: row.entries.firstOrNull { it.key.equals(fieldName, ignoreCase = true) }?.value
        }
    }

    inline fun <reified T> toList(wrapper: KronosDataSourceWrapper? = null): Triple<Boolean, Cursor?, List<T>> {
        val rows = selectClause.build(wrapper).toList<T>(wrapper)
        return pageResult(rows) { row, fieldName ->
            when (row) {
                is KPojo -> row[fieldName]
                is Map<*, *> -> row[fieldName]
                else -> null
            }
        }
    }

    @JvmName("toProjectionList")
    @Suppress("UNCHECKED_CAST")
    fun toList(wrapper: KronosDataSourceWrapper? = null): Triple<Boolean, Cursor?, List<Selected>> {
        val rows = selectClause.build(wrapper).toList(wrapper, selectClause.selectedType) as List<Selected>
        return pageResult(rows) { row, fieldName -> row[fieldName] }
    }

    @PublishedApi
    internal fun <T> pageResult(rows: List<T>, valueOf: (T, String) -> Any?): Triple<Boolean, Cursor?, List<T>> {
        val offset = spec.offset.coerceAtLeast(0)
        val hasNext = offset > 0 && rows.size > offset
        val records = if (hasNext) rows.take(offset) else rows
        val nextCursor = if (hasNext && records.isNotEmpty()) {
            cursorValues(records.last(), valueOf).toCursor()
        } else {
            null
        }
        return Triple(hasNext, nextCursor, records)
    }

    private fun <T> cursorValues(row: T, valueOf: (T, String) -> Any?): Map<String, Any?> {
        val fields = selectClause.context.orderByItems.mapNotNull { item ->
            (item as? SelectOrderItem.FieldItem)?.field
        }
        require(fields.size == selectClause.context.orderByItems.size && fields.isNotEmpty()) {
            "Cursor pagination requires field-based orderBy items."
        }
        return fields.associate { field ->
            val value = valueOf(row, field.name)
            require(value != null) { "Cursor pagination requires selected orderBy field '${field.name}' in result rows." }
            field.name to value
        }
    }
}
