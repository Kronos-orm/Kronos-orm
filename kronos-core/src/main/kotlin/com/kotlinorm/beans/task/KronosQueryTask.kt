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

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.beans.task

import com.kotlinorm.enums.QueryType
import com.kotlinorm.enums.QueryType.First
import com.kotlinorm.enums.QueryType.ToList
import com.kotlinorm.enums.QueryType.ToMap
import com.kotlinorm.enums.QueryType.ToMapList
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.logAndReturn
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Executable facade around one [KronosAtomicQueryTask].
 *
 * Each terminal operation copies the atomic task with the requested logical [KType], invokes
 * hooks and global query events in a deterministic order, and delegates execution to one
 * [KronosDataSourceWrapper]. Result decoding is performed by the wrapper from that complete type
 * and the task's result-column metadata.
 *
 * @property atomicTask planned SQL, parameters, target type, and result metadata
 */
class KronosQueryTask(val atomicTask: KronosAtomicQueryTask) {
    /** Hook invoked immediately before global before-query events. Its return value is ignored. */
    var beforeQuery: (KronosQueryTask.() -> Any?)? = null

    /** Hook invoked after global after-query events with the decoded result. Its return value is ignored. */
    var afterQuery: (Any?.(QueryType, KronosDataSourceWrapper) -> Any?)? = null

    /**
     * Replaces the operation-local before-query hook.
     *
     * @return this task for fluent configuration
     */
    fun doBeforeQuery(beforeQuery: KronosQueryTask.() -> Any?): KronosQueryTask {
        this.beforeQuery = beforeQuery
        return this
    }

    /**
     * Replaces the operation-local after-query hook.
     *
     * @return this task for fluent configuration
     */
    fun doAfterQuery(afterQuery: (Any?.(QueryType, KronosDataSourceWrapper) -> Any?)): KronosQueryTask {
        this.afterQuery = afterQuery
        return this
    }

    /**
     * Executes [queryAction] with the explicit wrapper or Kronos default wrapper.
     *
     * Hook order is local-before, global-before, execution/logging, global-after, local-after.
     * Exceptions propagate immediately and therefore skip the remaining later stages.
     *
     * @param wrapper wrapper to use, or `null` to resolve the configured default
     * @param queryType terminal operation classification passed to logging and hooks
     * @param task atomic task passed to events and execution
     * @param queryAction wrapper-specific execution operation
     * @return the exact result returned by [queryAction]
     */
    inline fun <T> executeQuery(
        wrapper: KronosDataSourceWrapper?,
        queryType: QueryType,
        task: KronosAtomicQueryTask = atomicTask,
        crossinline queryAction: (KronosDataSourceWrapper, KronosAtomicQueryTask) -> T
    ): T {
        val dataSource = wrapper.orDefault()
        beforeQuery?.invoke(this)
        QueryEvent.beforeQueryEvents.forEach { it(task, dataSource) }

        val result = task.logAndReturn(queryAction(dataSource, task), queryType)

        QueryEvent.afterQueryEvents.forEach { it(task, dataSource) }
        afterQuery?.invoke(result, queryType, dataSource)
        return result
    }

    /**
     * Executes the query and decodes every row as [targetType].
     *
     * @param wrapper wrapper to use, or `null` to resolve the configured default
     * @param targetType complete row type, including generic arguments and nullability
     * @param queryType terminal operation classification used by hooks and logging
     * @return decoded rows in result-set order
     */
    fun toList(
        wrapper: KronosDataSourceWrapper?,
        targetType: KType,
        queryType: QueryType = ToList
    ): List<Any?> {
        val task = atomicTask.copy(targetType = targetType)
        return executeQuery(wrapper, queryType, task) { dataSource, queryTask ->
            dataSource.toList(queryTask)
        }
    }

    /**
     * Executes the query and decodes the first row as [targetType].
     *
     * @param wrapper wrapper to use, or `null` to resolve the configured default
     * @param targetType complete logical result type
     * @param queryType terminal operation classification used by hooks and logging
     * @param required whether an empty result must throw; defaults from target nullability
     * @return decoded first row, or `null` when no row exists and [required] is false
     * @throws NoSuchElementException when no row exists and [required] is true
     */
    fun first(
        wrapper: KronosDataSourceWrapper?,
        targetType: KType,
        queryType: QueryType = First,
        required: Boolean = !targetType.isMarkedNullable
    ): Any? {
        val task = atomicTask.copy(targetType = targetType)
        val result = executeQuery(wrapper, queryType, task) { dataSource, queryTask ->
            dataSource.first(queryTask)
        }
        if (result == null && required) {
            throw NoSuchElementException("No result found for query: ${atomicTask.sql}")
        }
        return result
    }

    /** Returns every row as a raw, string-keyed map without a declared value type. */
    @Suppress("UNCHECKED_CAST")
    fun toMapList(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any?>> {
        return toList(wrapper, typeOf<Map<String, Any?>>(), ToMapList) as List<Map<String, Any?>>
    }

    /** Executes and decodes every row using the complete reified type [T]. */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> toList(wrapper: KronosDataSourceWrapper? = null): List<T> {
        return toList(wrapper, typeOf<T>()) as List<T>
    }

    /**
     * Returns the first row as a raw map.
     *
     * @throws NoSuchElementException when the query returns no rows
     */
    @Suppress("UNCHECKED_CAST")
    fun toMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?> {
        return first(wrapper, typeOf<Map<String, Any?>>(), ToMap) as Map<String, Any?>
    }

    /** Returns the first row as a raw map, or `null` when the query returns no rows. */
    @Suppress("UNCHECKED_CAST")
    fun toMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?>? {
        return first(wrapper, typeOf<Map<String, Any?>?>(), ToMap) as Map<String, Any?>?
    }

    /**
     * Decodes the first row using the complete reified type [T].
     *
     * @throws NoSuchElementException when the query returns no rows
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> first(wrapper: KronosDataSourceWrapper? = null): T {
        return first(wrapper, typeOf<T>()) as T
    }

    /** Decodes the first row as nullable [T], returning `null` for an empty result. */
    inline fun <reified T> firstOrNull(wrapper: KronosDataSourceWrapper? = null): T? {
        return first(wrapper, typeOf<T?>()) as T?
    }

    companion object {
        fun KronosAtomicQueryTask.toKronosQueryTask(): KronosQueryTask {
            return KronosQueryTask(this)
        }
    }

    operator fun component1(): String {
        return atomicTask.sql
    }

    operator fun component2(): Map<String, Any?> {
        return atomicTask.paramMap
    }

    operator fun component3() = this
}
