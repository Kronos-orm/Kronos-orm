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

package com.kotlinorm.beans.dsl

import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.insert.InsertClause
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.types.ToInsertSelect
import com.kotlinorm.utils.createInstance
import kotlin.reflect.KClass

abstract class KSelectable<Selected : KPojo>(
    internal open val pojo: KPojo,
    internal open val selectedKClass: KClass<Selected>
) {
    abstract fun build(wrapper: KronosDataSourceWrapper? = null): KronosQueryTask

    @PublishedApi
    internal abstract fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper? = null): SqlQueryPlan

    internal open fun buildTotalCountTask(wrapper: KronosDataSourceWrapper? = null): KronosQueryTask =
        error("Total count is not supported for ${this::class.simpleName}.")

    open fun toSqlQuery(wrapper: KronosDataSourceWrapper? = null): SqlQuery = toSqlQueryPlan(wrapper).query

    @Suppress("UNUSED")
    fun alias(@Suppress("UNUSED_PARAMETER") alias: String): KSelectable<Selected> = this

    /**
     * Builds an INSERT SELECT clause using this query's Selected row type as the value-mapping receiver.
     */
    inline fun <reified Target : KPojo> insert(
        noinline values: ToInsertSelect<Selected, Any?> = null
    ): InsertClause<Target> {
        return InsertClause(Target::class.createInstance()).fromSource(this, values)
    }
}
