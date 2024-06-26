/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.pagination

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.query
import com.kotlinorm.utils.queryList
import com.kotlinorm.utils.queryOne

class PagedClause<K : KPojo, T : KSelectable<K>>(
    private val selectClause: T
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
        selectClause.selectFields = linkedSetOf(Field("1", type = "string"))
        val cntTask = selectClause.build(wrapper)
        cntTask.sql = "SELECT COUNT(1) FROM (${cntTask.sql}) AS t"
        return cntTask to recordsTask
    }
}