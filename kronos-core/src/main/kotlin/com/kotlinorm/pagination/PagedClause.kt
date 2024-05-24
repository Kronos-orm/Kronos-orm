package com.kotlinorm.pagination

import com.kotlinorm.orm.select.SelectClause

class PagedClause<T : SelectClause<*>>(
    val selectClause: T
) {
    fun query(): Pair<Int, List<Map<String, Any>>> {
        TODO()
    }

    fun build() {
        TODO()
    }
}