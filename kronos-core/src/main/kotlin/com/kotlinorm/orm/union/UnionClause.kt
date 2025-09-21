/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *     http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kotlinorm.orm.union

import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.SelectClause
import com.kotlinorm.utils.KeyCounter

open class UnionClause(tasks: List<SelectClause<out KPojo>>) {
    private val tasks = tasks.toMutableList()
    private var builtTasks: List<KronosQueryTask> = listOf()
    private var safeKeyList: List<String> = listOf()
    private val fieldNameMapping = mutableMapOf<String, MutableMap<String, String>>()

    fun query(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any?>> {
        return executeQuery { it.query(wrapper) }
    }

    fun queryMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?> {
        return executeQuery { listOf(it.queryMap(wrapper)) }.first()
    }

    fun queryMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?>? {
        return executeQuery { listOfNotNull(it.queryMapOrNull(wrapper)) }.firstOrNull()
    }

    private fun executeQuery(
            queryFunction: (KronosQueryTask) -> List<Map<String, Any?>>
    ): List<Map<String, Any?>> {
        prepareTasks()
        val safeKeyList = getUniqueTask(KeyCounter())

        val resultMap =
                safeKeyList
                        .mapIndexed { index, safeKey ->
                            val result = queryFunction(builtTasks[index])
                            safeKey to result
                        }
                        .toMap()

        return if (resultMap.isEmpty()) emptyList() else union(resultMap)
    }

    private fun prepareTasks() {
        builtTasks = tasks.map { it.build() }
    }

    private fun getUniqueTask(keyCounters: KeyCounter): List<String> {
        val fieldCountMap = mutableMapOf<String, Int>()
        safeKeyList =
                tasks.map { task ->
                    val safeKey =
                            "${task.pojo::class.simpleName}_${keyCounters.getNext(task.pojo::class.simpleName.toString())}"
                    val fieldsToUse =
                            if (task.selectFields.isEmpty()) {
                                // 如果selectFields为空，使用所有字段
                                kPojoAllFieldsCache[task.pojo.kClass()]!!
                            } else {
                                task.selectFields
                            }
                    fieldsToUse.forEach { field ->
                        fieldCountMap[field.name] = fieldCountMap.getOrDefault(field.name, 0) + 1
                    }
                    safeKey
                }

        fieldNameMapping.clear()
        tasks.forEachIndexed { index, task ->
            val safeKey = safeKeyList[index]
            val className = task.pojo::class.simpleName
            // 计算suffix：同一个类的第二个实例及以后才有suffix
            val sameClassCount = tasks.take(index).count { it.pojo::class.simpleName == className }
            val suffix = if (sameClassCount > 0) "@$sameClassCount" else ""
            val fieldsToUse =
                    if (task.selectFields.isEmpty()) {
                        // 如果selectFields为空，使用所有字段
                        kPojoAllFieldsCache[task.pojo.kClass()]!!
                    } else {
                        task.selectFields
                    }
            val mapping =
                    fieldsToUse
                            .associate { field ->
                                val fieldName = field.name
                                // 只有当字段名冲突时才加前缀
                                if (fieldCountMap[fieldName]!! > 1) {
                                    fieldName to
                                            "$className$suffix${capitalizeFirstLetter(fieldName)}"
                                } else {
                                    fieldName to fieldName
                                }
                            }
                            .toMutableMap()
            fieldNameMapping[safeKey] = mapping
        }
        return safeKeyList
    }

    private fun union(resultMap: Map<String, List<Map<String, Any?>>>): List<Map<String, Any?>> {
        val maxRowNum = resultMap.values.maxOfOrNull { it.size } ?: 0

        val allFields =
                resultMap
                        .flatMap { (safeKey, list) ->
                            list.flatMap {
                                it.keys.map { key -> fieldNameMapping[safeKey]?.get(key) ?: key }
                            }
                        }
                        .toSet()

        return List(maxRowNum) { index ->
            mutableMapOf<String, Any?>().apply {
                allFields.forEach { this[it] = null }

                resultMap.forEach { (safeKey, list) ->
                    val fieldNameMap = fieldNameMapping[safeKey]
                    list.getOrNull(index)?.forEach { (key, value) ->
                        val fieldName = fieldNameMap?.get(key) ?: key
                        this[fieldName] = value
                    }
                }
            }
        }
    }

    private fun capitalizeFirstLetter(s: String): String {
        return s.replaceFirstChar { it.uppercaseChar() }
    }
}
