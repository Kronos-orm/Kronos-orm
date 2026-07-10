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
import com.kotlinorm.database.SqlManager.renderStatement
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.utils.DataSourceUtil.orDefault

class PagedClause<Source : KPojo, Selected : KPojo, Clause : KSelectable<Selected>>(
    private val selectClause: Clause
) {
    fun toMapList(wrapper: KronosDataSourceWrapper? = null): Pair<Int, List<Map<String, Any?>>> {
        val tasks = this.build(wrapper)
        val total = tasks.first.first<Int>(wrapper)
        val records = tasks.second.toMapList(wrapper)
        return total to records
    }

    inline fun <reified E : KPojo> toList(wrapper: KronosDataSourceWrapper? = null): Pair<Int, List<E>> {
        val tasks = this.build(wrapper)
        val total = tasks.first.first<Int>(wrapper)
        val records = tasks.second.toList<E>(wrapper)
        return total to records
    }

    @JvmName("toProjectionList")
    @Suppress("UNCHECKED_CAST")
    fun toList(wrapper: KronosDataSourceWrapper? = null): Pair<Int, List<Selected>> {
        val tasks = this.build(wrapper)
        val total = tasks.first.first<Int>(wrapper)
        val records = tasks.second.toList(wrapper, selectClause.selectedType) as List<Selected>
        return total to records
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): Pair<KronosQueryTask, KronosQueryTask> {
        val dataSource = wrapper.orDefault()
        val recordsTask = selectClause.build(dataSource)
        val countTask = selectClause.buildTotalCountTask(dataSource)
        val innerQuery = requireNotNull(countTask.atomicTask.statement) {
            "Paged total-count tasks must retain their SQL query AST."
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
        val rendered = renderStatement(dataSource, countQuery, countTask.atomicTask.paramMap)
        val finalCountTask = KronosQueryTask(
            countTask.atomicTask.copy(
                sql = rendered.sql,
                paramMap = rendered.parameters,
                statement = countQuery
            )
        )
        return finalCountTask to recordsTask
    }
}
