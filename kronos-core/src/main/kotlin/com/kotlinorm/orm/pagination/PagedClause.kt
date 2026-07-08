/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.orm.pagination

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.enums.QueryType.QueryList
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.logAndReturn

class PagedClause<Source : KPojo, Selected : KPojo, Clause : KSelectable<Selected>>(
    private val selectClause: Clause
) {
    fun query(wrapper: KronosDataSourceWrapper? = null): Pair<Int, List<Map<String, Any>>> {
        val tasks = this.build(wrapper)
        val total = tasks.first.queryOne<Int>(wrapper)
        val records = tasks.second.query(wrapper)
        return total to records
    }

    inline fun <reified E : KPojo> queryList(wrapper: KronosDataSourceWrapper? = null): Pair<Int, List<E>> {
        val tasks = this.build(wrapper)
        val total = tasks.first.queryOne<Int>(wrapper)
        val records = tasks.second.queryList<E>(wrapper)
        return total to records
    }

    @JvmName("queryForList")
    @Suppress("UNCHECKED_CAST")
    fun queryList(wrapper: KronosDataSourceWrapper? = null): Pair<Int, List<Selected>> {
        val tasks = this.build(wrapper)
        val total = tasks.first.queryOne<Int>(wrapper)
        with(tasks.second) {
            beforeQuery?.invoke(this)
            val records =
                atomicTask.logAndReturn(
                    wrapper.orDefault().forList(atomicTask, selectClause.selectedKClass, true, []), QueryList
                )

            afterQuery?.invoke(records, QueryList, wrapper.orDefault())
            return total to records as List<Selected>
        }
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): Pair<KronosQueryTask, KronosQueryTask> {
        val recordsTask = selectClause.build(wrapper)
        val cntTask = selectClause.buildTotalCountTask(wrapper)
        cntTask.atomicTask.sql = "SELECT COUNT(*) FROM (${cntTask.atomicTask.sql}) AS total_count"
        cntTask.beforeQuery = null
        cntTask.afterQuery = null
        return cntTask to recordsTask
    }
}
