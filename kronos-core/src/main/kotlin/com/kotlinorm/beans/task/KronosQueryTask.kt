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
import com.kotlinorm.enums.QueryType.Query
import com.kotlinorm.enums.QueryType.QueryList
import com.kotlinorm.enums.QueryType.QueryMap
import com.kotlinorm.enums.QueryType.QueryMapOrNull
import com.kotlinorm.enums.QueryType.QueryOne
import com.kotlinorm.enums.QueryType.QueryOneOrNull
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.logAndReturn

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
        crossinline queryAction: (KronosDataSourceWrapper) -> T
    ): T {
        val dataSource = wrapper.orDefault()
        beforeQuery?.invoke(this)
        QueryEvent.beforeQueryEvents.forEach { it(atomicTask, dataSource) }

        val result = atomicTask.logAndReturn(queryAction(dataSource), queryType)

        QueryEvent.afterQueryEvents.forEach { it(atomicTask, dataSource) }
        afterQuery?.invoke(result, queryType, dataSource)
        return result
    }

    fun query(wrapper: KronosDataSourceWrapper? = null) = executeQuery(wrapper, Query) {
        it.forList(atomicTask)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> queryList(
        wrapper: KronosDataSourceWrapper? = null,
        isKPojo: Boolean = false,
        superTypes: List<String> = emptyList()
    ) = executeQuery(wrapper, QueryList) {
        it.forList(atomicTask, T::class, isKPojo, superTypes) as List<T>
    }

    fun queryMap(
        wrapper: KronosDataSourceWrapper? = null
    ) = executeQuery(wrapper, QueryMap) {
        it.forMap(atomicTask) ?: throw NoSuchElementException("No result found for query: ${atomicTask.sql}")
    }

    fun queryMapOrNull(
        wrapper: KronosDataSourceWrapper? = null
    ) = executeQuery(wrapper, QueryMapOrNull) {
        it.forMap(atomicTask)
    }

    inline fun <reified T> queryOne(
        wrapper: KronosDataSourceWrapper? = null,
        isKPojo: Boolean = false,
        superTypes: List<String> = listOf()
    ) = executeQuery(wrapper, QueryOne) {
        it.forObject(atomicTask, T::class, isKPojo, superTypes) as T?
            ?: throw NoSuchElementException("No result found for query: ${atomicTask.sql}")
    }

    inline fun <reified T> queryOneOrNull(
        wrapper: KronosDataSourceWrapper? = null,
        isKPojo: Boolean = false,
        superTypes: List<String> = listOf()
    ) = executeQuery(wrapper, QueryOneOrNull) {
        it.forObject(atomicTask, T::class, isKPojo, superTypes) as T?
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