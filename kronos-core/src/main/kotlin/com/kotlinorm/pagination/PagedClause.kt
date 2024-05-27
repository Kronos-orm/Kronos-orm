package com.kotlinorm.pagination

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.SelectClause
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.query
import com.kotlinorm.utils.queryList
import com.kotlinorm.utils.queryOne

class PagedClause<T : KPojo>(
    private val selectClause: SelectClause<T>
) {
    fun query(wrapper: KronosDataSourceWrapper? = null): Pair<Int, List<Map<String, Any>>> {
        val tasks = this.build(wrapper)
        val total = tasks.first.queryOne<Int>()
        val records = tasks.second.query()
        return total to records
    }

    inline fun <reified T : KPojo> queryList(wrapper: KronosDataSourceWrapper? = null): Pair<Int, List<T>> {
        val tasks = this.build(wrapper)
        val total = tasks.first.queryOne<Int>()
        val records = tasks.second.queryList<T>()
        return total to records
    }

    @JvmName("queryForList")
    @Suppress("UNCHECKED_CAST")
    fun queryList(wrapper: KronosDataSourceWrapper? = null): Pair<Int, List<T>> {
        val tasks = this.build()
        val total = tasks.first.queryOne<Int>()
        val records = tasks.second.let {
            wrapper.orDefault().forList(it, selectClause.pojo::class)
        }
        return total to records as List<T>
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): Pair<KronosAtomicQueryTask, KronosAtomicQueryTask> {
        val recordsTask = selectClause.build(wrapper)
        selectClause.selectFields = linkedSetOf(Field("1" , type = "string"))
        val cntTask = selectClause.build(wrapper)
        cntTask.sql = "SELECT COUNT(1) FROM (${cntTask.sql}) AS t"
        return cntTask to recordsTask
    }
}