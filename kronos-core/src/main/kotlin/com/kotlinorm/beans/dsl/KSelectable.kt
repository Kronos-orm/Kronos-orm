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
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.insert.InsertClause
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.types.ToInsertSelect
import com.kotlinorm.utils.createInstance
import kotlin.reflect.KClass
import kotlin.reflect.KType

abstract class KSelectable<Selected : KPojo>(
    internal open val pojo: KPojo
) {
    @PublishedApi
    internal abstract val selectedType: KType

    @PublishedApi
    internal open val nullableSelectedType: KType
        get() = selectedType

    abstract fun build(wrapper: KronosDataSourceWrapper? = null): KronosQueryTask

    @PublishedApi
    internal abstract fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper? = null): SqlQueryPlan

    internal open fun buildTotalCountTask(wrapper: KronosDataSourceWrapper? = null): KronosQueryTask =
        error("Total count is not supported for ${this::class.simpleName}.")

    @Suppress("UNCHECKED_CAST")
    internal fun resultColumnTypes(fieldsByLabel: Map<String, Field> = emptyMap()): Map<String, KType> {
        val selectedClass = selectedType.classifier as? KClass<*> ?: return emptyMap()
        val projectionTypes = if (selectedClass == KPojo::class) {
            emptyMap()
        } else {
            fieldsMapCache[selectedClass as KClass<KPojo>]
                ?.mapNotNull { (label, field) -> field.kType?.let { label to it } }
                ?.toMap()
                .orEmpty()
        }
        return buildMap {
            putAll(projectionTypes)
            fieldsByLabel.forEach { (label, field) ->
                val targetType = field.kType ?: return@forEach
                listOf(label, field.name, field.columnName).forEach { name ->
                    put(name, targetType)
                    put(name.uppercase(), targetType)
                    put(name.lowercase(), targetType)
                }
            }
        }
    }

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
