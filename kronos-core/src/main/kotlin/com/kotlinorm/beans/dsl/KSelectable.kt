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

import com.kotlinorm.annotations.InternalKronosApi
import com.kotlinorm.beans.task.ResultColumnMetadata
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.interfaces.KronosRow
import com.kotlinorm.orm.insert.InsertClause
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.types.ToInsertSelect
import com.kotlinorm.utils.KTypeKey
import com.kotlinorm.utils.createKPojo
import kotlin.reflect.KType
import kotlin.reflect.typeOf

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

    internal open fun outputStableKeyCandidates(): List<List<String>> = emptyList()

    @PublishedApi
    internal open fun prepareFirstResult() = Unit

    /** Maps each result row directly through a wrapper-provided [KronosRow] cursor. */
    fun <R> toList(
        wrapper: KronosDataSourceWrapper? = null,
        mapper: (KronosRow) -> R
    ): List<R> = build(wrapper).toList(wrapper, mapper)

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.LowPriorityInOverloadResolution
    inline fun <reified T> first(wrapper: KronosDataSourceWrapper? = null): T {
        prepareFirstResult()
        return build(wrapper).first(wrapper)
    }

    @JvmName("firstProjection")
    @Suppress("UNCHECKED_CAST")
    fun first(wrapper: KronosDataSourceWrapper? = null): Selected {
        prepareFirstResult()
        return build(wrapper).first(wrapper, selectedType) as Selected
    }

    /** Maps the first result row directly through a wrapper-provided [KronosRow] cursor. */
    fun <R> first(
        wrapper: KronosDataSourceWrapper? = null,
        mapper: (KronosRow) -> R
    ): R {
        prepareFirstResult()
        return build(wrapper).first(wrapper, mapper)
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.LowPriorityInOverloadResolution
    inline fun <reified T> firstOrNull(wrapper: KronosDataSourceWrapper? = null): T? {
        prepareFirstResult()
        return build(wrapper).firstOrNull(wrapper)
    }

    @JvmName("firstProjectionOrNull")
    @Suppress("UNCHECKED_CAST")
    fun firstOrNull(wrapper: KronosDataSourceWrapper? = null): Selected? {
        prepareFirstResult()
        return build(wrapper).first(wrapper, nullableSelectedType, required = false) as Selected?
    }

    /** Maps the first result row directly through a wrapper-provided [KronosRow] cursor. */
    fun <R> firstOrNull(
        wrapper: KronosDataSourceWrapper? = null,
        mapper: (KronosRow) -> R
    ): R? {
        prepareFirstResult()
        return build(wrapper).firstOrNull(wrapper, mapper)
    }

    @OptIn(InternalKronosApi::class)
    internal fun resultColumns(fieldsByLabel: Map<String, Field> = emptyMap()): Map<String, ResultColumnMetadata> {
        val projectionColumns = if (
            KTypeKey.from(selectedType, ignoreTopLevelNullability = true) == KTypeKey.from(typeOf<KPojo>())
        ) {
            emptyMap()
        } else {
            kPojoAllFieldsCache[selectedType]
                ?.mapNotNull { field ->
                    val label = field.name.ifBlank { field.columnName }
                    field.kType?.let { type ->
                        label to ResultColumnMetadata(type, field, columnLabel = label)
                    }
                }
                ?.toMap()
                .orEmpty()
        }
        return buildMap {
            putAll(projectionColumns)
            fieldsByLabel.forEach { (label, field) ->
                val targetType = field.kType ?: return@forEach
                val metadata = ResultColumnMetadata(targetType, field, columnLabel = label)
                put(label, metadata)
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
        return InsertClause(createKPojo<Target>()).fromSource(this, values)
    }
}
