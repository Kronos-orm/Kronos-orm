package com.kotlinorm.pagination

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.orm.select.SelectClause
import com.kotlinorm.utils.query
import com.kotlinorm.utils.queryOne

class PagedClause<T : SelectClause<*>>(
    private val selectClause: T
) {
    fun query(): Pair<Int, List<Map<String, Any>>> {
        val tasks = this.build()
        val total = tasks.first.queryOne<Int>()
        val records = tasks.second.query()
        return total to records
    }

    fun build(): Pair<KronosAtomicQueryTask , KronosAtomicQueryTask> {
        val recordsTask = selectClause.build()
        selectClause.selectFields = linkedSetOf(Field("1" , type = "string"))

        val cntTask = selectClause.build()
        cntTask.sql = "SELECT COUNT(1) FROM (${cntTask.sql}) AS t"
        return cntTask to recordsTask
    }
}