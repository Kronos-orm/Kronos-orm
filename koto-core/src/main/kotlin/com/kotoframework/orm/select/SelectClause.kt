package com.kotoframework.orm.select

import com.kotoframework.interfaces.KPojo
import com.kotoframework.pagination.PagedClause
import com.kotoframework.types.KTableConditionalField
import com.kotoframework.types.KTableField
import com.kotoframework.types.KTableSortableField

class SelectClause<T : KPojo>(kPojo: T, fields: KTableField<T, Unit> = null) {
    fun orderBy(lambda: KTableSortableField<T, Unit>): SelectClause<T> {
        TODO()
    }

    fun groupBy(lambda: KTableField<T, Unit>): SelectClause<T> {
        TODO()
    }

    fun distinct(): SelectClause<T> {
        TODO()
    }

    fun limit(num: Int): SelectClause<T> {
        TODO()
    }

    fun offset(num: Int): SelectClause<T> {
        TODO()
    }

    fun page(pi: Int, ps: Int): SelectClause<T> {
        TODO()
    }

    fun by(lambda: KTableField<T, Unit>): SelectClause<T> {
        TODO()
    }

    fun where(lambda: KTableConditionalField<T, Boolean?> = null): SelectClause<T> {
        TODO()
    }

    fun having(lambda: KTableConditionalField<T, Boolean?> = null): SelectClause<T> {
        TODO()
    }

    fun withTotal(): PagedClause<SelectClause<T>> {
        TODO()
    }

    fun query(): List<Map<String, Any>> {
        TODO()
    }

    fun count(): List<Map<String, Any>> {
        TODO()
    }
}