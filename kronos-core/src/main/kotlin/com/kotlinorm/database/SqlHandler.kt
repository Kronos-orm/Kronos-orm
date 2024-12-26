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

package com.kotlinorm.database

import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper

object SqlHandler {
    fun KronosDataSourceWrapper.query(sql: String, paramMap: Map<String, Any?> = emptyMap()): List<Map<String, Any>> {
        return this.forList(KronosAtomicQueryTask(sql, paramMap))
    }

    fun KronosDataSourceWrapper.queryMap(sql: String, paramMap: Map<String, Any?> = emptyMap()): Map<String, Any>? {
        return this.forMap(KronosAtomicQueryTask(sql, paramMap))
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> KronosDataSourceWrapper.queryList(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap(),
        isKPojo: Boolean = false,
        superTypes: List<String> = emptyList()
    ): List<T> {
        return this.forList(KronosAtomicQueryTask(sql, paramMap), T::class, isKPojo, superTypes) as List<T>
    }

    inline fun <reified T> KronosDataSourceWrapper.queryOne(
        sql: String, paramMap: Map<String, Any?> = emptyMap(),
        isKPojo: Boolean = false,
        superTypes: List<String> = emptyList()
    ): T {
        return this.forObject(KronosAtomicQueryTask(sql, paramMap), T::class, isKPojo, superTypes) as T?
            ?: throw Exception("No result found")
    }

    inline fun <reified T> KronosDataSourceWrapper.queryOneOrNull(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap(),
        isKPojo: Boolean = false,
        superTypes: List<String> = emptyList()
    ): T? {
        return this.forObject(KronosAtomicQueryTask(sql, paramMap), T::class, isKPojo, superTypes) as T?
    }

    fun KronosDataSourceWrapper.execute(sql: String, paramMap: Map<String, Any?> = emptyMap()): Int {
        return this.update(KronosAtomicActionTask(sql, paramMap))
    }

    fun KronosDataSourceWrapper.batchExecute(sql: String, paramMapList: Array<Map<String, Any?>>): IntArray {
        return this.batchUpdate(
            KronosAtomicBatchTask(
                sql, paramMapList
            )
        )
    }
}