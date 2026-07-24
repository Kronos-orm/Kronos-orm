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

package com.kotlinorm.database

import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.interfaces.KronosRow
import com.kotlinorm.interfaces.KronosRowFirstResult
import com.kotlinorm.interfaces.requireKronosRowMapping
import com.kotlinorm.utils.prepareRawSqlParameters
import kotlin.reflect.KType
import kotlin.reflect.typeOf

object SqlExecutor {
    fun KronosDataSourceWrapper.query(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap()
    ): List<Map<String, Any?>> {
        return toList(sql, paramMap)
    }

    fun KronosDataSourceWrapper.queryMap(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap()
    ): Map<String, Any?>? {
        return toMap(sql, paramMap)
    }

    fun KronosDataSourceWrapper.queryMapOrNull(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap()
    ): Map<String, Any?>? {
        return toMapOrNull(sql, paramMap)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> KronosDataSourceWrapper.queryList(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap(),
        targetType: KType = typeOf<T>()
    ): List<T> {
        return toList(sql, paramMap, targetType)
    }

    inline fun <reified T> KronosDataSourceWrapper.queryOne(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap(),
        targetType: KType = typeOf<T>()
    ): T {
        return first(sql, paramMap, targetType)
    }

    inline fun <reified T> KronosDataSourceWrapper.queryOneOrNull(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap(),
        targetType: KType = typeOf<T?>()
    ): T? {
        return firstOrNull(sql, paramMap, targetType)
    }

    @Suppress("UNCHECKED_CAST")
    fun KronosDataSourceWrapper.toMapList(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap()
    ): List<Map<String, Any?>> {
        return this.toList(
            KronosAtomicQueryTask(sql, prepareRawSqlParameters(paramMap), targetType = typeOf<Map<String, Any?>>())
        ) as List<Map<String, Any?>>
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(NoSuchElementException::class)
    fun KronosDataSourceWrapper.toMap(sql: String, paramMap: Map<String, Any?> = emptyMap()): Map<String, Any?> {
        return this.first(
            KronosAtomicQueryTask(sql, prepareRawSqlParameters(paramMap), targetType = typeOf<Map<String, Any?>>())
        ) as Map<String, Any?>?
            ?: throw NoSuchElementException("No result found")
    }

    @Suppress("UNCHECKED_CAST")
    fun KronosDataSourceWrapper.toMapOrNull(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap()
    ): Map<String, Any?>? {
        return this.first(
            KronosAtomicQueryTask(sql, prepareRawSqlParameters(paramMap), targetType = typeOf<Map<String, Any?>?>())
        ) as Map<String, Any?>?
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> KronosDataSourceWrapper.toList(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap(),
        targetType: KType = typeOf<T>()
    ): List<T> {
        return this.toList(
            KronosAtomicQueryTask(sql, prepareRawSqlParameters(paramMap), targetType = targetType)
        ) as List<T>
    }

    /** Maps each raw SQL result row directly through the wrapper's [KronosRow] capability. */
    fun <T> KronosDataSourceWrapper.toList(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap(),
        mapper: (KronosRow) -> T
    ): List<T> {
        return requireKronosRowMapping().toList(rawRowMappingTask(sql, paramMap), mapper)
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(NoSuchElementException::class)
    inline fun <reified T> KronosDataSourceWrapper.first(
        sql: String, paramMap: Map<String, Any?> = emptyMap(),
        targetType: KType = typeOf<T>()
    ): T {
        val result = this.first(
            KronosAtomicQueryTask(sql, prepareRawSqlParameters(paramMap), targetType = targetType)
        )
        if (result == null && !targetType.isMarkedNullable) {
            throw NoSuchElementException("No result found")
        }
        return result as T
    }

    /** Maps the first raw SQL result row directly through the wrapper's [KronosRow] capability. */
    fun <T> KronosDataSourceWrapper.first(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap(),
        mapper: (KronosRow) -> T
    ): T {
        return when (val result = requireKronosRowMapping().first(rawRowMappingTask(sql, paramMap), mapper)) {
            KronosRowFirstResult.Empty -> throw NoSuchElementException("No result found")
            is KronosRowFirstResult.Present -> result.value
        }
    }

    inline fun <reified T> KronosDataSourceWrapper.firstOrNull(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap(),
        targetType: KType = typeOf<T?>()
    ): T? {
        return first(sql, paramMap, targetType)
    }

    /** Maps the first raw SQL result row directly through the wrapper's [KronosRow] capability. */
    fun <T> KronosDataSourceWrapper.firstOrNull(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap(),
        mapper: (KronosRow) -> T
    ): T? {
        return when (val result = requireKronosRowMapping().first(rawRowMappingTask(sql, paramMap), mapper)) {
            KronosRowFirstResult.Empty -> null
            is KronosRowFirstResult.Present -> result.value
        }
    }

    fun KronosDataSourceWrapper.execute(sql: String, paramMap: Map<String, Any?> = emptyMap()): Int {
        return this.update(KronosAtomicActionTask(sql, prepareRawSqlParameters(paramMap)))
    }

    fun KronosDataSourceWrapper.batchExecute(sql: String, paramMapList: Array<Map<String, Any?>>): IntArray {
        return this.batchUpdate(
            KronosAtomicBatchTask(
                sql,
                paramMapList.mapIndexed { index, parameters ->
                    prepareRawSqlParameters(parameters, batchIndex = index)
                }.toTypedArray()
            )
        )
    }

    private fun rawRowMappingTask(sql: String, paramMap: Map<String, Any?>): KronosAtomicQueryTask =
        KronosAtomicQueryTask(
            sql,
            prepareRawSqlParameters(paramMap),
            targetType = typeOf<Any?>()
        )
}
