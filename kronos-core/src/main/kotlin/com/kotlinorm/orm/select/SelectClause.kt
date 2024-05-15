package com.kotlinorm.orm.select

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.pagination.PagedClause
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.types.KTableSortableField

class SelectClause<T : KPojo>(kPojo: T, fields: KTableField<T, Unit> = null) {

    internal lateinit var tableName: String
    internal lateinit var logicDeleteStrategy: KronosCommonStrategy
    internal var allFields: MutableList<Field> = mutableListOf()

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

    operator fun component1(): String {
        TODO()
    }

    operator fun component2(): Map<String, Any?> {
        TODO()
    }
}