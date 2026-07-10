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
 * KronosQueryTask class represents a collection of Kronos atomic query tasks used to execute SELECT operations.
 * It provides functionality to manage and execute a batch of read-only SQL queries.
 */
class KronosQueryTask(val atomicTask: KronosAtomicQueryTask) { //原子任务
    var beforeQuery: (KronosQueryTask.() -> Any?)? = null //在执行之前执行的操作
    var afterQuery: (Any?.(QueryType, KronosDataSourceWrapper) -> Any?)? = null //在执行之后执行的操作

    fun doBeforeQuery(beforeQuery: KronosQueryTask.() -> Any?): KronosQueryTask { //设置在执行之前执行的操作
        this.beforeQuery = beforeQuery
        return this
    }

    fun doAfterQuery(afterQuery: (Any?.(QueryType, KronosDataSourceWrapper) -> Any?)): KronosQueryTask { // 设置在执行之后执行的操作(返回一个新的KronosQueryTask)
        this.afterQuery = afterQuery
        return this
    }

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

    @Suppress("UNCHECKED_CAST")
    fun toMapList(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any?>> {
        return toList(wrapper, typeOf<Map<String, Any?>>(), ToMapList) as List<Map<String, Any?>>
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> toList(wrapper: KronosDataSourceWrapper? = null): List<T> {
        return toList(wrapper, typeOf<T>()) as List<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun toMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?> {
        return first(wrapper, typeOf<Map<String, Any?>>(), ToMap) as Map<String, Any?>
    }

    @Suppress("UNCHECKED_CAST")
    fun toMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?>? {
        return first(wrapper, typeOf<Map<String, Any?>?>(), ToMap) as Map<String, Any?>?
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> first(wrapper: KronosDataSourceWrapper? = null): T {
        return first(wrapper, typeOf<T>()) as T
    }

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
